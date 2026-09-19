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

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.yaml.snakeyaml.Yaml;

import es.nivel36.janus.api.v1.appuser.AppUserResource;

class OpenApiContractTest {

	private static final String API_PREFIX = "/api/v1";

	@Test
	void appUserEndpointsArePresentInTheVersionedOpenApiContract() throws IOException {
		final Map<String, Object> contract;
		try (var contractStream = OpenApiContractTest.class.getResourceAsStream("/janus.yaml");
				var reader = new InputStreamReader(contractStream)) {
			contract = new Yaml().load(reader);
		}

		@SuppressWarnings("unchecked")
		final var paths = (Map<String, Map<String, Object>>) contract.get("paths");
		final var documentedOperations = new LinkedHashSet<String>();
		paths.forEach((path, operations) -> operations.keySet().stream()
				.filter(OpenApiContractTest::isHttpMethod)
				.map(method -> method.toUpperCase(Locale.ROOT) + " " + path)
				.forEach(documentedOperations::add));

		assertThat(documentedOperations).containsAll(implementedOperations(AppUserResource.class));
	}

	@Test
	void appUsersHasOneCanonicalPublicPath() {
		final var mapping = AppUserResource.class.getAnnotation(RequestMapping.class);

		assertThat(mapping.value()).containsExactly(API_PREFIX + "/appusers");
	}

	private static Set<String> implementedOperations(final Class<?> resource) {
		final var baseMapping = resource.getAnnotation(RequestMapping.class);
		assertThat(baseMapping).as("class-level mapping for %s", resource.getSimpleName()).isNotNull();
		assertThat(baseMapping.value()).as("one canonical mapping for %s", resource.getSimpleName()).hasSize(1);
		final var basePath = baseMapping.value()[0].substring(API_PREFIX.length());
		final var operations = new LinkedHashSet<String>();

		for (final var method : resource.getDeclaredMethods()) {
			final var mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
			if (mapping == null) {
				continue;
			}
			final var methodPaths = mapping.value().length == 0 ? new String[] { "" } : mapping.value();
			for (final var httpMethod : mapping.method()) {
				for (final var methodPath : methodPaths) {
					operations.add(httpMethod.name() + " " + basePath + methodPath);
				}
			}
		}
		return operations;
	}

	private static boolean isHttpMethod(final String value) {
		return Set.of("get", "put", "post", "delete", "patch", "head", "options", "trace").contains(value);
	}
}
