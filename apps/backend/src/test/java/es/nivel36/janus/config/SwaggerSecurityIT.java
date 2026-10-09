/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package es.nivel36.janus.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import es.nivel36.janus.api.v1.SecurityTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
class SwaggerSecurityIT {

	private static final String CSP_HEADER = "Content-Security-Policy";
	private static final String API_POLICY = "default-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'none'";

	@Autowired
	private MockMvc mvc;

	@Test
	void swaggerUiAllowsItsResourcesAndSameOriginApiRequests() throws Exception {
		final MvcResult result = mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk()).andReturn();
		final String policy = result.getResponse().getHeader(CSP_HEADER);
		assertThat(policy).contains(
				"script-src 'self'",
				"style-src 'self' 'unsafe-inline'",
				"img-src 'self' data:",
				"connect-src 'self'",
				"frame-ancestors 'none'");
		assertThat(result.getResponse().getHeaders(CSP_HEADER)).hasSize(1);
		for (final String resource : new String[] { "swagger-ui-bundle.js", "swagger-ui-standalone-preset.js",
				"swagger-initializer.js", "swagger-ui.css", "index.css" }) {
			mvc.perform(get("/swagger-ui/" + resource)).andExpect(status().isOk())
					.andExpect(header().string(CSP_HEADER, policy));
		}
		mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection())
				.andExpect(header().string(CSP_HEADER, policy));
	}

	@Test
	void openApiDefinitionIsPublicAndKeepsStrictPolicy() throws Exception {
		mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk())
				.andExpect(header().string(CSP_HEADER, API_POLICY));
		mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(header().string(CSP_HEADER, API_POLICY));
	}

	@Test
	void unauthenticatedApiRequestsRemainProtected() throws Exception {
		mvc.perform(get("/api/v1/worksites")).andExpect(status().isUnauthorized())
				.andExpect(header().string(CSP_HEADER, API_POLICY));
		mvc.perform(get("/swagger-ui-evil/index.html")).andExpect(header().string(CSP_HEADER, API_POLICY));
	}
}
