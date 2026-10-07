/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License");
 */
package es.nivel36.janus.config;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Bounds API bodies before MVC deserialization, including chunked requests,
 * ignored JSON properties and trailing whitespace. Registered only inside the
 * security chain, after authentication and URL authorization.
 */
final class ApiRequestBodyLimitFilter extends OncePerRequestFilter {
	static final int MAX_BODY_BYTES = 1_048_576;

	private final ObjectMapper objectMapper;
	private final Clock clock;

	ApiRequestBodyLimitFilter(final ObjectMapper objectMapper, final Clock clock) {
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Override
	protected boolean shouldNotFilter(final HttpServletRequest request) {
		final String path = request.getRequestURI().substring(request.getContextPath().length());
		return !path.equals("/api") && !path.startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final FilterChain chain) throws IOException,
			ServletException {
		if (request.getContentLengthLong() > MAX_BODY_BYTES) {
			reject(request, response);
			return;
		}
		// Never trust Content-Length alone, and never allocate an unbounded cache.
		final byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
		if (body.length > MAX_BODY_BYTES) {
			reject(request, response);
			return;
		}
		chain.doFilter(new BufferedBodyRequest(request, body), response);
	}

	private void reject(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		final ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONTENT_TOO_LARGE);
		problem.setType(URI.create("urn:problem:request-body-too-large"));
		problem.setTitle("Request body too large");
		problem.setDetail("API request bodies must not exceed 1048576 bytes");
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("timestamp", this.clock.instant().toString());
		response.setStatus(413);
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		this.objectMapper.writeValue(response.getOutputStream(), problem);
	}

	private static final class BufferedBodyRequest extends HttpServletRequestWrapper {
		private final ServletInputStream input;

		BufferedBodyRequest(final HttpServletRequest request, final byte[] body) {
			super(request);
			final ByteArrayInputStream bytes = new ByteArrayInputStream(body);
			this.input = new ServletInputStream() {
				@Override
				public int read() {
					return bytes.read();
				}

				@Override
				public int read(final byte[] buffer, final int offset, final int length) {
					return bytes.read(buffer, offset, length);
				}

				@Override
				public boolean isFinished() {
					return bytes.available() == 0;
				}

				@Override
				public boolean isReady() {
					return true;
				}

				@Override
				public void setReadListener(final ReadListener listener) {
					try {
						if (!isFinished()) {
							listener.onDataAvailable();
						}
						if (isFinished()) {
							listener.onAllDataRead();
						}
					} catch (final IOException ex) {
						listener.onError(ex);
					}
				}
			};
		}

		@Override
		public ServletInputStream getInputStream() {
			return this.input;
		}

		@Override
		public BufferedReader getReader() {
			final String encoding = getCharacterEncoding();
			return new BufferedReader(
					new InputStreamReader(
							this.input,
							encoding == null ? StandardCharsets.UTF_8 : java.nio.charset.Charset.forName(encoding)));
		}
	}
}
