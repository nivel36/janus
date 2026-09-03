/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package es.nivel36.janus.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;

class JanusExceptionHandlerTest {

	private static final String SENSITIVE_DETAIL = "sensitive-provider-detail-8e25f43a";
	private static final URI ACCESS_DENIED_TYPE = URI.create("urn:problem:access-denied");
	private static final URI INTERNAL_ERROR_TYPE = URI.create("urn:problem:internal");

	private final JanusExceptionHandler handler = new JanusExceptionHandler(Clock.systemUTC());

	@Test
	void authorizationDeniedDoesNotExposeExceptionMessage() {
		final AuthorizationDeniedException exception = new AuthorizationDeniedException(SENSITIVE_DETAIL,
				new AuthorizationDecision(false));

		final ProblemDetail problem = this.handler.handleAuthorizationDeniedException(exception, null);

		assertSafeProblem(problem, HttpStatus.FORBIDDEN, ACCESS_DENIED_TYPE,
				"You are not authorized to perform this operation");
	}

	@Test
	void accessDeniedDoesNotExposeExceptionMessage() {
		final ProblemDetail problem = this.handler.handleAccessDenied(new AccessDeniedException(SENSITIVE_DETAIL), null);

		assertSafeProblem(problem, HttpStatus.FORBIDDEN, ACCESS_DENIED_TYPE,
				"You are not authorized to perform this operation");
	}

	@Test
	void authenticationDoesNotExposeProviderMessage() {
		final ProblemDetail problem = this.handler.handleAuthentication(new BadCredentialsException(SENSITIVE_DETAIL),
				null);

		assertSafeProblem(problem, HttpStatus.UNAUTHORIZED, ACCESS_DENIED_TYPE,
				"Valid authentication credentials are required");
	}

	@Test
	void genericExceptionDoesNotExposeExceptionMessage() {
		final ProblemDetail problem = this.handler.handleGeneric(new Exception(SENSITIVE_DETAIL), null);

		assertSafeProblem(problem, HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_TYPE,
				"An unexpected internal error occurred");
		assertThat(problem.getTitle()).isEqualTo("Internal server error");
	}

	private static void assertSafeProblem(final ProblemDetail problem, final HttpStatus expectedStatus,
			final URI expectedType, final String expectedDetail) {
		assertThat(problem.getStatus()).isEqualTo(expectedStatus.value());
		assertThat(problem.getType()).isEqualTo(expectedType);
		assertThat(problem.getDetail()).isEqualTo(expectedDetail).doesNotContain(SENSITIVE_DETAIL);
	}
}
