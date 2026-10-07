/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License");
 */
package es.nivel36.janus.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ReadListener;
import java.util.Arrays;
import jakarta.servlet.http.HttpServletRequest;

class ApiRequestBodyLimitFilterTest {
	private final ApiRequestBodyLimitFilter filter = new ApiRequestBodyLimitFilter(
			new JacksonJsonHttpMessageConverter().getMapper(),
			Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

	private MockHttpServletRequest request(final int length, final boolean knownLength) {
		final var request = new MockHttpServletRequest("POST", "/api/v1/worksites") {
			@Override
			public long getContentLengthLong() {
				return knownLength ? super.getContentLengthLong() : -1;
			}
		};
		request.setContent(new byte[length]);
		return request;
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void allowsExactlyOneMiBAndReplaysTheCompleteBody(final boolean knownLength) throws Exception {
		final var request = request(ApiRequestBodyLimitFilter.MAX_BODY_BYTES, knownLength);
		final AtomicInteger received = new AtomicInteger();
		this.filter.doFilter(request, new MockHttpServletResponse(), (wrapped, _) -> {
			received.set(wrapped.getInputStream().readAllBytes().length);
			assertThat(wrapped.getInputStream().isFinished()).isTrue();
		});
		assertThat(received.get()).isEqualTo(ApiRequestBodyLimitFilter.MAX_BODY_BYTES);
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void rejectsOneByteOverTheLimitBeforeCallingMvc(final boolean knownLength) throws Exception {
		final var response = new MockHttpServletResponse();
		final var chain = mock(FilterChain.class);
		this.filter.doFilter(request(ApiRequestBodyLimitFilter.MAX_BODY_BYTES + 1, knownLength), response, chain);
		assertThat(response.getStatus()).isEqualTo(413);
		assertThat(response.getContentType()).isEqualTo("application/problem+json");
		assertThat(response.getContentAsString())
				.contains("urn:problem:request-body-too-large", "1048576", "2026-01-01T00:00:00Z");
		verifyNoInteractions(chain);
	}

	@Test
	void countsUtf8BytesRatherThanCharactersAndSupportsReaders() throws Exception {
		final var request = new MockHttpServletRequest("PUT", "/api/v1/app-users/id");
		request.setCharacterEncoding("UTF-8");
		request.setContent("Descripción 😀".getBytes(StandardCharsets.UTF_8));
		this.filter.doFilter(request, new MockHttpServletResponse(), (wrapped, _) -> {
			assertThat(((HttpServletRequest) wrapped).getReader().readLine()).isEqualTo("Descripción 😀");
		});
		request.setContent(
				"😀".repeat(ApiRequestBodyLimitFilter.MAX_BODY_BYTES / 4 + 1).getBytes(StandardCharsets.UTF_8));
		final var response = new MockHttpServletResponse();
		final var chain = mock(FilterChain.class);
		this.filter.doFilter(request, response, chain);
		assertThat(response.getStatus()).isEqualTo(413);
		verifyNoInteractions(chain);
	}

	@Test
	void boundsReadsEvenWhenTheBodyHasNoEndOrAnHonestContentLength() throws Exception {
		final AtomicInteger bytesRead = new AtomicInteger();
		final var request = new MockHttpServletRequest("POST", "/api/v1/worksites") {
			@Override
			public long getContentLengthLong() {
				return 1; // A misleading header must not bypass the streamed limit.
			}

			@Override
			public ServletInputStream getInputStream() {
				return new ServletInputStream() {
					@Override
					public int read() {
						bytesRead.incrementAndGet();
						return 'a';
					}

					@Override
					public int read(final byte[] buffer, final int offset, final int length) {
						Arrays.fill(buffer, offset, offset + length, (byte) 'a');
						bytesRead.addAndGet(length);
						return length;
					}

					@Override
					public boolean isFinished() {
						return false;
					}

					@Override
					public boolean isReady() {
						return true;
					}

					@Override
					public void setReadListener(final ReadListener listener) {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
		final var response = new MockHttpServletResponse();
		final var chain = mock(FilterChain.class);
		this.filter.doFilter(request, response, chain);
		assertThat(bytesRead.get()).isEqualTo(ApiRequestBodyLimitFilter.MAX_BODY_BYTES + 1);
		assertThat(response.getStatus()).isEqualTo(413);
		verifyNoInteractions(chain);
	}

	@Test
	void skipsPathsOutsideTheApi() throws Exception {
		final var request = request(ApiRequestBodyLimitFilter.MAX_BODY_BYTES + 1, true);
		request.setRequestURI("/assets/example.json");
		final AtomicInteger calls = new AtomicInteger();
		this.filter.doFilter(request, new MockHttpServletResponse(), (_, _) -> calls.incrementAndGet());
		assertThat(calls.get()).isEqualTo(1);
	}
}
