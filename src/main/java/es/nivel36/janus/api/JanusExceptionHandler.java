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

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import es.nivel36.janus.service.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Exception handler that translates common exceptions into RFC 7807 problem
 * details.
 *
 * <p>
 * It centralizes error responses for controllers, providing consistent status
 * codes and payloads.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JanusExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(JanusExceptionHandler.class);

	/**
	 * Handles missing entity
	 */
	@ExceptionHandler(EntityNotFoundException.class)
	ProblemDetail handleEntityNotFound(final EntityNotFoundException ex, final HttpServletRequest request) {
		logger.warn("Entity not found: {}", ex.getMessage());
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		pd.setTitle("Resource not found");
		pd.setDetail(ex.getMessage());
		addCommonProps(pd, request);
		return pd;
	}

	/**
	 * Handles {@link ResourceNotFoundException} thrown by services when a requested
	 * resource does not exist.
	 *
	 * @param ex      the exception raised by the service layer; must not be
	 *                {@code null}
	 * @param request the HTTP request that triggered the exception; may be
	 *                {@code null} when invoked outside of servlet context
	 * @return a {@link ProblemDetail} with {@link HttpStatus#NOT_FOUND}
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex, final HttpServletRequest request) {
		logger.warn("Resource not found: {}", ex.getMessage());
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		pd.setTitle("Resource not found");
		pd.setDetail(ex.getMessage());
		addCommonProps(pd, request);
		return pd;
	}

	/**
	 * Handles invalid arguments from client (business or precondition violations).
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail handleIllegalArgument(final IllegalArgumentException ex, final HttpServletRequest request) {
		logger.debug("Illegal argument: {}", ex.getMessage());
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setTitle("Invalid argument");
		pd.setDetail(ex.getMessage());
		addCommonProps(pd, request);
		return pd;
	}

	/**
	 * Handles illegal state
	 */
	@ExceptionHandler(IllegalStateException.class)
	ProblemDetail handleIllegalState(final IllegalStateException ex, final HttpServletRequest request) {
		logger.debug("Illegal state: {}", ex.getMessage());
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
		pd.setTitle("Operation conflict");
		pd.setDetail(ex.getMessage());
		addCommonProps(pd, request);
		return pd;
	}

	/**
	 * Handles format errors when parsing date/time from query parameters or manual
	 * parsing.
	 */
	@ExceptionHandler(DateTimeParseException.class)
	ProblemDetail handleDateTimeParse(final DateTimeParseException ex, final HttpServletRequest request) {
		logger.debug("Date/time parse error: {}", ex.getParsedString());
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setTitle("Invalid date/time format");
		pd.setDetail("Failed to parse date/time: " + ex.getParsedString());
		addCommonProps(pd, request);
		return pd;
	}

	/**
	 * Handles unreadable request payloads (e.g., malformed JSON, invalid types in
	 * body). Often wraps DateTimeParseException for body fields.
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleNotReadable(final HttpMessageNotReadableException ex, final HttpServletRequest request) {
		logger.debug("Message not readable: {}", ex.getMostSpecificCause().getMessage());
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setTitle("Malformed request");
		pd.setDetail(ex.getMostSpecificCause().getMessage());
		addCommonProps(pd, request);
		return pd;
	}

	/**
	 * Handles missing required query parameters.
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	ProblemDetail handleMissingParam(final MissingServletRequestParameterException ex,
			final HttpServletRequest request) {
		logger.debug("Missing request parameter: {}", ex.getParameterName());
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setTitle("Missing parameter");
		pd.setDetail("Required parameter '" + ex.getParameterName() + "' is missing");
		addCommonProps(pd, request);
		return pd;
	}

	/**
	 * Handles type mismatches in path variables or query parameters (e.g., id not a
	 * number).
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail handleTypeMismatch(final MethodArgumentTypeMismatchException ex, final HttpServletRequest request) {
		logger.debug("Type mismatch for '{}': {}", ex.getName(), ex.getMessage());
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setTitle("Type mismatch");
		pd.setDetail("Parameter '" + ex.getName() + "' has invalid value");
		addCommonProps(pd, request);
		return pd;
	}

	/**
	 * Handles validation errors for @Valid/@Validated on request bodies and
	 * parameters.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleMethodArgumentNotValid(final MethodArgumentNotValidException ex,
			final HttpServletRequest request) {
		logger.debug("Validation failed: {} errors", ex.getBindingResult().getErrorCount());
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setTitle("Validation failed");
		pd.setDetail("Request contains invalid fields");
		// Attach field errors as an extension
		pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
				.map(err -> "%s: %s".formatted(err.getField(), err.getDefaultMessage())).toList());
		addCommonProps(pd, request);
		return pd;
	}

	/**
	 * Handles constraint violations on @RequestParam/@PathVariable when
	 * using @Validated at controller level.
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	ProblemDetail handleConstraintViolation(final ConstraintViolationException ex, final HttpServletRequest request) {
		logger.debug("Constraint violation: {}", ex.getMessage());
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setTitle("Constraint violation");
		pd.setDetail("One or more constraints were violated");
		pd.setProperty("violations", ex.getConstraintViolations().stream()
				.map(v -> "%s: %s".formatted(v.getPropertyPath(), v.getMessage())).toList());
		addCommonProps(pd, request);
		return pd;
	}

	/**
	 * Catch-all handler for unexpected exceptions.
	 */
	@ExceptionHandler(Exception.class)
	ProblemDetail handleGeneric(final Exception ex, final HttpServletRequest request) {
		logger.error("Unexpected error", ex);
		final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		pd.setTitle("Internal server error");
		pd.setDetail("An unexpected error occurred");
		addCommonProps(pd, request);
		return pd;
	}

	/**
	 * Adds common properties to Problem Details: timestamp and request path.
	 */
	private static void addCommonProps(final ProblemDetail pd, final HttpServletRequest request) {
		pd.setProperty("timestamp", LocalDateTime.now().toString());
		if (request != null) {
			pd.setProperty("path", request.getRequestURI());
		}
	}
}
