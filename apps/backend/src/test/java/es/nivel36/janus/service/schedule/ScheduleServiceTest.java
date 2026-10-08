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
package es.nivel36.janus.service.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import es.nivel36.janus.service.ResourceAlreadyExistsException;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.employee.Employee;

/** Verifies orchestration, safe replacement and normalized search requests. */
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {
	private @Mock ScheduleRepository repository;
	private ScheduleService service;

	@BeforeEach
	void setUp() {
		this.service = new ScheduleService(this.repository, 100);
	}

	private Schedule schedule() {
		final Schedule schedule = new Schedule("STD", "Standard", Duration.ZERO, Duration.ZERO);
		schedule.setId(42L);
		return schedule;
	}

	private ScheduleRuleDefinition rule(final String name, final LocalTime end) {
		return new ScheduleRuleDefinition(
				name,
				null,
				null,
				List.of(
						new ScheduleRuleTimeRangeDefinition(
								DayOfWeek.MONDAY,
								Duration.ofHours(8),
								LocalTime.of(22, 0),
								end)));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "" })
	void absentQueryStillAppliesEmployeeFilterAndDefaults(final String query) {
		final Pageable normalized = PageRequest.of(3, 20, Sort.by("code"));
		final Page<Schedule> page = Page.empty(normalized);
		when(this.repository.search("", "EMP-42", normalized)).thenReturn(page);
		assertThat(this.service.searchSchedules(query, "EMP-42", PageRequest.of(3, 20))).isSameAs(page);
		verify(this.repository, never()).findAll(any(Pageable.class));
	}

	@Test
	void searchEscapesWildcardsWithoutTrimmingAndCapsPageSize() {
		final Pageable normalized = PageRequest.of(2, 100, Sort.by("code"));
		when(this.repository.search("  A!%!_!!  ", null, normalized)).thenReturn(Page.empty(normalized));
		this.service.searchSchedules("  A%_!  ", null, PageRequest.of(2, 500));
		verify(this.repository).search("  A!%!_!!  ", null, normalized);
	}

	@Test
	void sortOptionsArePreservedAndCodeBreaksTies() {
		final Sort.Order order = Sort.Order.desc("name").ignoreCase().nullsLast();
		final Pageable normalized = PageRequest.of(0, 10, Sort.by(order, Sort.Order.asc("code")));
		when(this.repository.search("", null, normalized)).thenReturn(Page.empty(normalized));
		this.service.searchSchedules(null, null, PageRequest.of(0, 10, Sort.by(order)));
		verify(this.repository).search("", null, normalized);
	}

	@Test
	void explicitCodeOrderingKeepsDirectionAndPosition() {
		final Pageable request = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("code"), Sort.Order.asc("name")));
		when(this.repository.search("", null, request)).thenReturn(Page.empty(request));
		this.service.searchSchedules(null, null, request);
		verify(this.repository).search("", null, request);
	}

	@ParameterizedTest
	@ValueSource(strings = { "id", "employees.employeeNumber", "rules.name", "entryTolerance", "unknown" })
	void unsupportedSortIsRejectedBeforeSearching(final String property) {
		assertThatThrownBy(() -> this.service.searchSchedules(null, null, PageRequest.of(0, 10, Sort.by(property))))
				.isInstanceOf(IllegalArgumentException.class);
		verifyNoInteractions(this.repository);
	}

	@Test
	void unpagedSearchIsRejectedBeforeSearching() {
		assertThatThrownBy(() -> this.service.searchSchedules(null, null, Pageable.unpaged()))
				.isInstanceOf(IllegalArgumentException.class);
		verifyNoInteractions(this.repository);
	}

	@Test
	void searchPreservesPageMetadataWithoutLoadingAssociations() {
		final Pageable normalized = PageRequest.of(2, 1, Sort.by("code"));
		final Page<Schedule> page = new PageImpl<>(List.of(schedule()), normalized, 7);
		when(this.repository.search("", null, normalized)).thenReturn(page);
		assertThat(this.service.searchSchedules(null, null, normalized)).isSameAs(page);
		verify(this.repository).search("", null, normalized);
		verifyNoMoreInteractions(this.repository);
	}

	@Test
	void creationBuildsAnOvernightRuleAndBothSidesOfItsAssociations() {
		when(this.repository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
		final Schedule created = this.service.createSchedule(
				"STD",
				"Standard",
				Duration.ofMinutes(5),
				Duration.ZERO,
				List.of(rule("Night", LocalTime.of(6, 0))));
		assertThat(created.getCode()).isEqualTo("STD");
		assertThat(created.getEntryTolerance()).isEqualTo(Duration.ofMinutes(5));
		final ScheduleRule rule = created.getRules().iterator().next();
		assertThat(rule.getSchedule()).isSameAs(created);
		assertThat(rule.getDayOfWeekRanges()).singleElement().satisfies(range -> {
			assertThat(range.getScheduleRule()).isSameAs(rule);
			assertThat(range.getTimeRange().getEndTime()).isEqualTo(LocalTime.of(6, 0));
			assertThat(range.getEffectiveWorkHours()).isEqualTo(Duration.ofHours(8));
		});
	}

	@Test
	void duplicateCodeIsRejectedWithoutSaving() {
		when(this.repository.existsByCode("STD")).thenReturn(true);
		assertThatThrownBy(
				() -> this.service.createSchedule("STD", "Standard", Duration.ZERO, Duration.ZERO, List.of()))
				.isInstanceOf(ResourceAlreadyExistsException.class);
		verify(this.repository, never()).save(any());
	}

	@Test
	void replacementChangesAllEditableFieldsAndKeepsCode() {
		final Schedule existing = schedule();
		final ScheduleRule oldRule = new ScheduleRule("Old", existing);
		existing.addRule(oldRule);
		when(this.repository.findByCode("STD")).thenReturn(existing);
		assertThat(
				this.service.updateSchedule(
						"STD",
						"Updated",
						Duration.ofMinutes(5),
						Duration.ofMinutes(10),
						List.of(rule("Night", LocalTime.of(6, 0)))))
				.isSameAs(existing);
		assertThat(existing.getCode()).isEqualTo("STD");
		assertThat(existing.getName()).isEqualTo("Updated");
		assertThat(existing.getEntryTolerance()).isEqualTo(Duration.ofMinutes(5));
		assertThat(existing.getExitTolerance()).isEqualTo(Duration.ofMinutes(10));
		assertThat(existing.getRules()).singleElement().satisfies(replacement -> {
			assertThat(replacement.getName()).isEqualTo("Night");
			assertThat(replacement.getSchedule()).isSameAs(existing);
		});
	}

	@Test
	void invalidLaterRuleLeavesAllExistingFieldsAndRulesUnchanged() {
		final Schedule existing = schedule();
		final ScheduleRule original = new ScheduleRule("Old", existing);
		existing.addRule(original);
		when(this.repository.findByCode("STD")).thenReturn(existing);
		assertThatThrownBy(
				() -> this.service.updateSchedule(
						"STD",
						"Updated",
						Duration.ofMinutes(5),
						Duration.ofMinutes(10),
						List.of(rule("Valid", LocalTime.of(6, 0)), rule("Invalid", LocalTime.of(22, 0)))))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(existing.getName()).isEqualTo("Standard");
		assertThat(existing.getEntryTolerance()).isEqualTo(Duration.ZERO);
		assertThat(existing.getExitTolerance()).isEqualTo(Duration.ZERO);
		assertThat(existing.getRules()).containsExactly(original);
	}

	@Test
	void missingScheduleReportsNotFoundForLookupAndReplacement() {
		assertThatThrownBy(() -> this.service.findScheduleByCode("MISSING"))
				.isInstanceOf(ResourceNotFoundException.class);
		assertThatThrownBy(
				() -> this.service.updateSchedule("MISSING", "Name", Duration.ZERO, Duration.ZERO, List.of()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void assignedScheduleCannotBeDeleted() {
		final Schedule schedule = schedule();
		when(this.repository.hasEmployees("STD")).thenReturn(true);
		assertThatThrownBy(() -> this.service.deleteSchedule(schedule)).isInstanceOf(IllegalStateException.class);
		verify(this.repository, never()).delete(any());
	}

	@Test
	void unassignedScheduleIsDeleted() {
		final Schedule schedule = schedule();
		this.service.deleteSchedule(schedule);
		verify(this.repository).hasEmployees("STD");
		verify(this.repository).delete(schedule);
	}

	@Test
	void dateLookupUsesEmployeeIdAndTheDayOnWhichTheShiftStarts() {
		final Employee employee = new Employee("EMP-42", "Jane", "Doe", "jane@example.test", schedule());
		ReflectionTestUtils.setField(employee, "id", 42L);
		final LocalDate date = LocalDate.of(2026, 10, 5);
		final TimeRange range = new TimeRange(LocalTime.of(22, 0), LocalTime.of(6, 0));
		when(this.repository.findTimeRangeForDate(42L, date, DayOfWeek.MONDAY)).thenReturn(Optional.of(range));
		assertThat(this.service.findTimeRangeForEmployeeByDate(employee, date)).containsSame(range);
	}
}
