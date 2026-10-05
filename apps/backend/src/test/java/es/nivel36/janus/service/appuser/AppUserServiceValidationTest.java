/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package es.nivel36.janus.service.appuser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import es.nivel36.janus.config.UserProvisioningProperties;
import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.employee.EmployeeService;
import jakarta.validation.ConstraintViolationException;

/**
 * Verifies that Spring rejects invalid service arguments before dependency
 * access.
 */
class AppUserServiceValidationTest {
	private AnnotationConfigApplicationContext context;
	private AppUserRepository repository;
	private AppUserCreator creator;
	private EmployeeService employees;
	private AppUserService service;

	@BeforeEach
	void setUp() {
		this.repository = mock(AppUserRepository.class);
		this.creator = mock(AppUserCreator.class);
		this.employees = mock(EmployeeService.class);
		this.context = new AnnotationConfigApplicationContext();
		this.context.registerBean(
				LocalValidatorFactoryBean.class,
				definition -> definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE));
		this.context.registerBean(MethodValidationPostProcessor.class, () -> {
			final MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
			processor.setValidator(this.context.getBean(LocalValidatorFactoryBean.class));
			return processor;
		});
		this.context.registerBean(
				AppUserService.class,
				() -> new AppUserService(
						this.repository,
						this.creator,
						new UserProvisioningProperties(),
						this.employees,
						100));
		this.context.refresh();
		this.service = this.context.getBean(AppUserService.class);
	}

	@AfterEach
	void tearDown() {
		this.context.close();
	}

	static Stream<Arguments> invalidClaims() {
		return Stream.of(
				Arguments.of(null, "valid@example.test", null),
				Arguments.of("", "valid@example.test", null),
				Arguments.of(" ", "valid@example.test", null),
				Arguments.of("subject|identity", "valid@example.test", null),
				Arguments.of("x".repeat(256), "valid@example.test", null),
				Arguments.of("subject", null, null),
				Arguments.of("subject", "", null),
				Arguments.of("subject", " ", null),
				Arguments.of("subject", "not-an-email", null),
				Arguments.of("subject", " user@example.test ", null),
				Arguments.of("subject", "valid@example.test", ""),
				Arguments.of("subject", "valid@example.test", "bad number"),
				Arguments.of("subject", "valid@example.test", " EMP-42 "),
				Arguments.of("subject", "valid@example.test", "x".repeat(51)));
	}

	@ParameterizedTest
	@MethodSource("invalidClaims")
	void invalidProvisioningClaimsAreRejectedBeforeLookup(
			final String subject,
			final String email,
			final String employee) {
		assertThatThrownBy(() -> this.service.findOrCreateAppUser(subject, email, employee))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.creator, this.employees);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", " ", "subject|identity" })
	void invalidSubjectLookupIsRejectedBeforeRepositoryAccess(final String subject) {
		assertThatThrownBy(() -> this.service.findAppUserByKeycloakSubject(subject))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.creator, this.employees);
	}

	@Test
	void validBoundaryClaimsAndMissingEmployeePassValidation() {
		final String subject = "x".repeat(255);
		final AppUser profile = new AppUser("user@example.test", subject, Locale.ENGLISH, TimeFormat.H24);
		when(this.repository.findByKeycloakSubject(subject)).thenReturn(Optional.of(profile));
		assertThat(this.service.findOrCreateAppUser(subject, "USER@EXAMPLE.TEST", null)).isSameAs(profile);
		assertThat(this.service.findOrCreateAppUser(subject, "USER@EXAMPLE.TEST", "x".repeat(50))).isSameAs(profile);
		assertThat(this.service.findAppUserByKeycloakSubject(subject)).isSameAs(profile);
		verifyNoInteractions(this.creator, this.employees);
	}

	static Stream<Arguments> nullPreferences() {
		final UUID id = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
		return Stream.of(
				Arguments.of(null, Locale.FRANCE, TimeFormat.H12, ZoneId.of("UTC"), Theme.LIGHT),
				Arguments.of(id, null, TimeFormat.H12, ZoneId.of("UTC"), Theme.LIGHT),
				Arguments.of(id, Locale.FRANCE, null, ZoneId.of("UTC"), Theme.LIGHT),
				Arguments.of(id, Locale.FRANCE, TimeFormat.H12, null, Theme.LIGHT),
				Arguments.of(id, Locale.FRANCE, TimeFormat.H12, ZoneId.of("UTC"), null));
	}

	@ParameterizedTest
	@MethodSource("nullPreferences")
	void incompletePreferencesAreRejectedBeforeLoadingProfile(
			final UUID id,
			final Locale locale,
			final TimeFormat format,
			final ZoneId timezone,
			final Theme theme) {
		assertThatThrownBy(() -> this.service.updatePreferences(id, locale, format, timezone, theme))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.creator, this.employees);
	}

	@Test
	void nullDeletionIdIsRejectedBeforeLoadingProfile() {
		assertThatThrownBy(() -> this.service.deleteAppUser(null)).isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.creator, this.employees);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "bad number", " EMP-42 ", "!" })
	void invalidEmployeeSearchFilterIsRejectedBeforeSearching(final String employee) {
		assertThatThrownBy(() -> this.service.searchAppUsers(null, employee, PageRequest.of(0, 10)))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.creator, this.employees);
	}

	@Test
	void nullPageableIsRejectedBeforeSearching() {
		assertThatThrownBy(() -> this.service.searchAppUsers(null, null, null))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.creator, this.employees);
	}
}
