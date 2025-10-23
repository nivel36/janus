/*
 * Copyright 2025 Abel Ferrer Jiménez
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

/**
 * Exception handler that translates common exceptions into RFC 7807 problem
 * details.
 *
 * <p>
 * It centralizes error responses for controllers, providing consistent status
 * codes and payloads.
 */
import java.net.URI;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import es.nivel36.janus.service.ResourceAlreadyExistsException;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.timelog.TimeLogChronologyException;
import es.nivel36.janus.service.timelog.TimeLogModificationNotAllowedException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JanusExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(JanusExceptionHandler.class);

	private static final URI TYPE_NOT_FOUND = URI.create("urn:problem:resource-not-found");
	private static final URI TYPE_INVALID_ARGUMENT = URI.create("urn:problem:invalid-argument");
	private static final URI TYPE_OPERATION_CONFLICT = URI.create("urn:problem:operation-conflict");
	private static final URI TYPE_INVALID_DATE_TIME = URI.create("urn:problem:invalid-date-time-format");
	private static final URI TYPE_MALFORMED_REQUEST = URI.create("urn:problem:malformed-request");
	private static final URI TYPE_MISSING_PARAMETER = URI.create("urn:problem:missing-parameter");
	private static final URI TYPE_TYPE_MISMATCH = URI.create("urn:problem:type-mismatch");
	private static final URI TYPE_VALIDATION_FAILED = URI.create("urn:problem:validation-failed");
	private static final URI TYPE_VALIDATION_ERROR = URI.create("urn:problem:validation-error");
	private static final URI TYPE_CONSTRAINT_VIOLATION = URI.create("urn:problem:constraint-violation");
	private static final URI TYPE_INTERNAL_ERROR = URI.create("urn:problem:internal");

	private final Clock clock;

	/**
	 * Creates an exception handler that timestamps generated {@link ProblemDetail}
	 * instances.
	 *
	 * @param clock clock used to populate the {@code timestamp} attribute; must not
	 *              be {@code null}
	 */
	public JanusExceptionHandler(final Clock clock) {
		this.clock = Objects.requireNonNull(clock);
	}

	@ExceptionHandler(EntityNotFoundException.class)
	ProblemDetail handleEntityNotFound(final EntityNotFoundException ex, final HttpServletRequest request) {
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		pd.setType(TYPE_NOT_FOUND);
		pd.setTitle("Resource not found");
		pd.setDetail(ex.getMessage());
		addCommonProps(pd, request);
		logger.warn("Error {}", pd);
		return pd;
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail handleResourceNotFound(ResourceNotFoundException ex, final HttpServletRequest request) {
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		pd.setType(TYPE_NOT_FOUND);
		pd.setTitle("Resource not found");
		pd.setDetail(ex.getMessage());
		addCommonProps(pd, request);
		logger.warn("Error {}", pd);
		return pd;
	}

	@ExceptionHandler(ResourceAlreadyExistsException.class)
	ProblemDetail handleResourceAlreadyExists(ResourceAlreadyExistsException ex, final HttpServletRequest request) {
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setType(TYPE_OPERATION_CONFLICT);
		pd.setTitle("Resource already exists");
		pd.setDetail(ex.getMessage());
		addCommonProps(pd, request);
		logger.warn("Error {}", pd);
		return pd;
	}

	@ExceptionHandler(TimeLogChronologyException.class)
	ProblemDetail handleTimeLogChronology(TimeLogChronologyException ex, final HttpServletRequest request) {
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setType(TYPE_OPERATION_CONFLICT);
		pd.setTitle("Invalid chronological order");
		pd.setDetail(ex.getMessage());
		addCommonProps(pd, request);
		logger.warn("Error {}", pd);
		return pd;
	}

	@ExceptionHandler(TimeLogModificationNotAllowedException.class)
	ProblemDetail handleTimeLogModificationNotAllowed(TimeLogModificationNotAllowedException ex,
			final HttpServletRequest request) {
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setType(TYPE_OPERATION_CONFLICT);
		pd.setTitle("Modification not allowed");
		pd.setDetail(ex.getMessage());
		addCommonProps(pd, request);
		logger.warn("Error {}", pd);
		return pd;
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail handleIllegalArgument(final IllegalArgumentException ex, final HttpServletRequest request) {
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setType(TYPE_INVALID_ARGUMENT);
		pd.setTitle("Invalid argument");
		pd.setDetail(ex.getMessage());
		addCommonProps(pd, request);
		logger.warn("Error {}", pd);
		return pd;
	}

	@ExceptionHandler(IllegalStateException.class)
	ProblemDetail handleIllegalState(final IllegalStateException ex, final HttpServletRequest request) {
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
		pd.setType(TYPE_OPERATION_CONFLICT);
		pd.setTitle("Operation conflict");
		pd.setDetail(ex.getMessage());
		addCommonProps(pd, request);
		logger.error("Error {}",pd);
		return pd;
	}

	@ExceptionHandler(DateTimeParseException.class)
	ProblemDetail handleDateTimeParse(final DateTimeParseException ex, final HttpServletRequest request) {
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setType(TYPE_INVALID_DATE_TIME);
		pd.setTitle("Invalid date/time format");
		pd.setDetail("Failed to parse date/time: " + ex.getParsedString());
		addCommonProps(pd, request);
		logger.warn("Error {}", pd);
		return pd;
	}

	@ExceptionHandler(DateTimeException.class)
	ProblemDetail handleDateTime(final DateTimeException ex, final HttpServletRequest request) {
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setType(TYPE_INVALID_DATE_TIME);
		pd.setTitle("Invalid date/time format");
		pd.setDetail("Failed to parse date/time: " + ex.getMessage());
		addCommonProps(pd, request);
		logger.warn("Error {}", pd);
		return pd;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleNotReadable(final HttpMessageNotReadableException ex, final HttpServletRequest request) {
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setType(TYPE_MALFORMED_REQUEST);
		pd.setTitle("Malformed request");
		pd.setDetail(ex.getMostSpecificCause().getMessage());
		addCommonProps(pd, request);
		logger.warn("Error {}", pd);
		return pd;
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	ProblemDetail handleMissingParam(final MissingServletRequestParameterException ex,
			final HttpServletRequest request) {
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setType(TYPE_MISSING_PARAMETER);
		pd.setTitle("Missing parameter");
		pd.setDetail("Required parameter '" + ex.getParameterName() + "' is missing");
		addCommonProps(pd, request);
		logger.warn("Error {}", pd);
		return pd;
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail handleTypeMismatch(final MethodArgumentTypeMismatchException ex, final HttpServletRequest request) {
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setType(TYPE_TYPE_MISMATCH);
		pd.setTitle("Type mismatch");
		pd.setDetail("Parameter '" + ex.getName() + "' has invalid value " + ex.getMessage());
		addCommonProps(pd, request);
		logger.warn("Error {}", pd);
		return pd;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleMethodArgumentNotValid(final MethodArgumentNotValidException ex,
			final HttpServletRequest request) {
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setType(TYPE_VALIDATION_FAILED);
		pd.setTitle("Validation failed");
		pd.setDetail("Request contains invalid fields");
		pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
				.map(err -> "%s: %s".formatted(err.getField(), err.getDefaultMessage())).toList());
		addCommonProps(pd, request);
		logger.warn("Error {}", pd);
		return pd;
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ProblemDetail handle(HandlerMethodValidationException ex) {
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setType(TYPE_VALIDATION_ERROR);
		pd.setTitle("Validation error");
		pd.setDetail("Request parameters/path are invalid");

		final List<Map<String, String>> errors = new ArrayList<>();
		for (final ParameterValidationResult pvr : ex.getParameterValidationResults()) {
			for (final MessageSourceResolvable msr : pvr.getResolvableErrors()) {
				final String fullCode = msr.getCodes()[1];
				final StringTokenizer st = new StringTokenizer(fullCode, ".");
				final String code = st.nextToken();
				final String field = st.nextToken();
				final String message = msr.getDefaultMessage();
				errors.add(Map.of("name", field, "reason", message, "code", code));
			}
		}
		pd.setProperty("errors", errors);
		logger.warn("Error {}", pd);
		return pd;
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ProblemDetail handleConstraintViolation(final ConstraintViolationException ex, final HttpServletRequest request) {
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setType(TYPE_CONSTRAINT_VIOLATION);
		pd.setTitle("Constraint violation");
		pd.setDetail("One or more constraints were violated");
		pd.setProperty("violations", ex.getConstraintViolations().stream()
				.map(v -> "%s: %s".formatted(v.getPropertyPath(), v.getMessage())).toList());
		addCommonProps(pd, request);
		logger.warn("Error {}", pd);
		return pd;
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleGeneric(final Exception ex, final HttpServletRequest request) {
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		pd.setType(TYPE_INTERNAL_ERROR);
		pd.setTitle("Internal server error");
		pd.setDetail("An unexpected error occurred");
		addCommonProps(pd, request);
		logger.error("Error {}", pd);
		return pd;
	}

	private void addCommonProps(final ProblemDetail pd, final HttpServletRequest request) {
		pd.setProperty("timestamp", clock.instant().toString());
		if (request != null) {
			pd.setProperty("instance", request.getRequestURI());
		}
	}
}
