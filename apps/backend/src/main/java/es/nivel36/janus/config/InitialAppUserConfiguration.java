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
			@Value("${janus.bootstrap.initial-user.employee-email}") final String employeeEmail,
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

			// Provision every pre-existing application identity before secured endpoints
			// start resolving actors. The external identity remains the authoritative key.
			jdbcClient.sql("""
					INSERT INTO security_principal (type, issuer, subject, enabled, created_at, display_name)
					SELECT 'HUMAN', au.identity_issuer, au.identity_subject, true, CURRENT_TIMESTAMP, au.username
					FROM app_user au
					WHERE NOT EXISTS (
						SELECT 1 FROM security_principal sp
						WHERE sp.issuer = au.identity_issuer AND sp.subject = au.identity_subject
					)
					""").update();

			jdbcClient.sql("""
					UPDATE app_user
					SET security_principal_id = (
						SELECT sp.id FROM security_principal sp
						WHERE sp.issuer = app_user.identity_issuer
						  AND sp.subject = app_user.identity_subject
					)
					WHERE security_principal_id IS NULL
					""").update();

			// The initial employee association is explicit configuration rather than an
			// ownership inference made from a JWT claim at request time.
			jdbcClient.sql("""
					INSERT INTO employee_principal
						(employee_id, security_principal_id, relationship_type, enabled,
						 valid_from, valid_until, created_at, created_by_principal_id)
					SELECT e.id, au.security_principal_id, 'SELF', true,
						CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, au.security_principal_id
					FROM employee e
					JOIN app_user au ON au.username = :username
					WHERE e.email = :employeeEmail
					  AND NOT EXISTS (
						SELECT 1 FROM employee_principal ep
						WHERE ep.employee_id = e.id
						  AND ep.security_principal_id = au.security_principal_id
						  AND ep.relationship_type = 'SELF' AND ep.enabled = true
					  )
					""")
				.param("username", username)
				.param("employeeEmail", employeeEmail)
				.update();
		};
	}
}
