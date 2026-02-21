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
package es.nivel36.janus.config;

import com.fasterxml.jackson.databind.Module;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring {@link Configuration} class for customizing Jackson's ObjectMapper.
 * <p>
 * Registers additional modules required for proper JSON serialization and
 * deserialization within the application.
 */
@Configuration
public class JacksonConfig {

	/**
	 * Registers the {@link JsonNullableModule} with Jackson.
	 * <p>
	 * This module adds support for
	 * {@link org.openapitools.jackson.nullable.JsonNullable}, allowing request and
	 * response payloads to explicitly distinguish between <i>undefined</i> (field
	 * absent in JSON) and <i>null</i> (field present with value {@code null}).
	 * <p>
	 * <b>Example:</b>
	 * </p>
	 * 
	 * <pre>{@code
	 * {
	 *   "name": null        // explicitly null
	 * }
	 *
	 * {
	 *   // "name" property is omitted → considered undefined
	 * }
	 * }</pre>
	 *
	 * @return the configured {@link Module} to be registered with the Jackson
	 *         context
	 */
	@Bean
	public Module jsonNullableModule() {
		return new JsonNullableModule();
	}
}
