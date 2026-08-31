/*
 * Copyright 2026 Abel Ferrer Jiménez
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
package es.nivel36.janus.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

class ProductionConfigurationTest {

	private static final Map<String, String> REQUIRED_CONFIGURATION = Map.of(
			"JANUS_DATASOURCE_USERNAME", "janus", "JANUS_DATASOURCE_PASSWORD", "secret", "JWT_ISSUER_URL",
			"https://identity.example/realms/janus", "JANUS_SECURITY_CLIENT_ID", "janus-api");

	private static final Map<String, String> CRITICAL_PROPERTIES = Map.of(
			"JANUS_DATASOURCE_USERNAME", "spring.datasource.username", "JANUS_DATASOURCE_PASSWORD",
			"spring.datasource.password", "JWT_ISSUER_URL",
			"spring.security.oauth2.resourceserver.jwt.issuer-uri", "JANUS_SECURITY_CLIENT_ID",
			"janus.security.client-id");

	@Test
	void prodProfileRejectsEachMissingCriticalProperty() throws IOException {
		for (final Map.Entry<String, String> criticalProperty : CRITICAL_PROPERTIES.entrySet()) {
			final Map<String, Object> supplied = new LinkedHashMap<>(REQUIRED_CONFIGURATION);
			supplied.remove(criticalProperty.getKey());
			final PropertySourcesPropertyResolver resolver = loadProdConfiguration(supplied);

			assertThatThrownBy(() -> resolver.getRequiredProperty(criticalProperty.getValue()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining(criticalProperty.getKey());
		}
	}

	@Test
	void prodProfileUsesOnlyIssuerForJwtDiscovery() throws IOException {
		final Map<String, Object> supplied = new LinkedHashMap<>(REQUIRED_CONFIGURATION);
		supplied.put("SPRING_JWT_ISSUER_URI", "https://wrong.example/issuer");
		supplied.put("JWT_JWK_SET_URI", "https://wrong.example/jwks");
		final PropertySourcesPropertyResolver resolver = loadProdConfiguration(supplied);

		assertThat(resolver.getRequiredProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"))
			.isEqualTo("https://identity.example/realms/janus");
		assertThat(resolver.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri")).isNull();
	}

	private PropertySourcesPropertyResolver loadProdConfiguration(final Map<String, Object> supplied)
			throws IOException {
		final MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MapPropertySource("production environment", supplied));
		new YamlPropertySourceLoader().load("application-prod", new ClassPathResource("application-prod.yml"))
			.forEach(propertySources::addLast);
		return new PropertySourcesPropertyResolver(propertySources);
	}
}
