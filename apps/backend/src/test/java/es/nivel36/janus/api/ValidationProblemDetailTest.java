/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package es.nivel36.janus.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

class ValidationProblemDetailTest {

	private static final String TIMESTAMP = "2026-09-30T12:00:00Z";

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		this.mvc = MockMvcBuilders.standaloneSetup(new ValidationController())
				.setControllerAdvice(new JanusExceptionHandler(
						Clock.fixed(Instant.parse(TIMESTAMP), ZoneOffset.UTC)))
				.setValidator(validator)
				.build();
	}

	@Test
	void invalidJsonFieldHasTheCommonValidationProblemShape() throws Exception {
		this.mvc.perform(post("/validation/body").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"\"}"))
				.andExpectAll(commonProblem("/validation/body", "name", "NotBlank"));
	}

	@Test
	void invalidRequestParameterHasTheCommonValidationProblemShape() throws Exception {
		this.mvc.perform(get("/validation/query").param("query", "invalid value"))
				.andExpectAll(commonProblem("/validation/query", "query", "Pattern"));
	}

	@Test
	void invalidPathVariableHasTheCommonValidationProblemShape() throws Exception {
		this.mvc.perform(get("/validation/path/{code}", "invalid value"))
				.andExpectAll(commonProblem("/validation/path/invalid%20value", "code", "Pattern"));
	}

	private static org.springframework.test.web.servlet.ResultMatcher[] commonProblem(final String instance,
			final String name, final String code) {
		return new org.springframework.test.web.servlet.ResultMatcher[] { status().isBadRequest(),
				content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON),
				jsonPath("$.type").value("urn:problem:validation-failed"),
				jsonPath("$.title").value("Validation failed"),
				jsonPath("$.status").value(400), jsonPath("$.detail").value("Request contains invalid fields"),
				jsonPath("$.errors.length()").value(1), jsonPath("$.errors[0].name").value(name),
				jsonPath("$.errors[0].reason").isString(), jsonPath("$.errors[0].code").value(code),
				jsonPath("$.timestamp").value(TIMESTAMP), jsonPath("$.instance").value(instance) };
	}

	@RestController
	@RequestMapping("/validation")
	private static final class ValidationController {

		@PostMapping("/body")
		void body(@Valid @RequestBody final ValidationBody body) {
		}

		@RequestMapping("/query")
		void query(@RequestParam("query") @Pattern(regexp = "[a-z]+") final String query) {
		}

		@RequestMapping("/path/{code}")
		void path(@PathVariable("code") @Pattern(regexp = "[a-z]+") final String code) {
		}
	}

	private record ValidationBody(@NotBlank String name) {
	}
}
