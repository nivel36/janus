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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Configures the identity of the initial application user for an environment. */
@Configuration(proxyBeanMethods = false)
class InitialAppUserConfiguration {

	@Bean
	@ConditionalOnProperty(name = "janus.bootstrap.initial-user.enabled", havingValue = "true")
	ApplicationRunner initialAppUserInitializer(final JdbcClient jdbcClient,
			@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") final String issuer,
			@Value("${janus.bootstrap.initial-user.username}") final String username,
			@Value("${janus.bootstrap.initial-user.subject}") final String subject,
			@Value("${janus.bootstrap.initial-user.locale}") final String locale,
			@Value("${janus.bootstrap.initial-user.time-format}") final String timeFormat,
			@Value("${janus.bootstrap.initial-user.default-timezone}") final String defaultTimezone) {
		return arguments -> {
			jdbcClient.sql("""
					INSERT INTO app_user (username, identity_issuer, identity_subject, locale, time_format, default_timezone)
					SELECT :username, :issuer, :subject, :locale, :timeFormat, :defaultTimezone
					WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = :username)
					""")
				.param("username", username)
				.param("issuer", issuer)
				.param("subject", subject)
				.param("locale", locale)
				.param("timeFormat", timeFormat)
				.param("defaultTimezone", defaultTimezone)
				.update();
		};
	}
}
