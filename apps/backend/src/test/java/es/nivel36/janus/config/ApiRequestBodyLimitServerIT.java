/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License");
 */
package es.nivel36.janus.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Exercises the backend directly with an actual chunked HTTP request. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiRequestBodyLimitServerIT {
	@LocalServerPort
	private int port;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void rejectsChunkedJsonBeforeDeserialization() throws Exception {
		when(this.jwtDecoder.decode("test-token")).thenReturn(
				Jwt.withTokenValue("test-token").header("alg", "none").subject("body-limit-test")
						.issuer("https://issuer.example.test").audience(List.of("janus-api"))
						.claim("email_verified", true).build());
		final byte[] bytes = ("{\"ignored\":\"" + "a".repeat(1_048_576) + "\"}").getBytes(StandardCharsets.UTF_8);
		final var request = HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + "/api/v1/worksites"))
				.timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer test-token")
				.header("Content-Type", "application/json")
				// An unknown publisher length makes HttpClient use chunked transfer.
				.POST(HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(bytes))).build();
		try (final var client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()) {
			final var response = client.send(request, HttpResponse.BodyHandlers.ofString());
			assertThat(response.statusCode()).isEqualTo(413);
			assertThat(response.headers().firstValue("Content-Type")).hasValue("application/problem+json");
			assertThat(response.body()).contains("urn:problem:request-body-too-large", "1048576");
		}
	}

}
