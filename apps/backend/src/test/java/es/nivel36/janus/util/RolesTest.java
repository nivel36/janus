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
package es.nivel36.janus.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

import org.junit.jupiter.api.Test;

class RolesTest {

	@Test
	void hasOnlyEmployeeRoleShouldIgnoreNonRoleAuthorities() {
		assertThat(Roles.hasOnlyEmployeeRole(
				createAuthorityList("ROLE_JANUS_EMPLOYEE", "SCOPE_read", "SCOPE_profile"))).isTrue();
	}

	@Test
	void hasOnlyEmployeeRoleShouldRejectAnotherJanusRole() {
		assertThat(Roles.hasOnlyEmployeeRole(
				createAuthorityList("ROLE_JANUS_EMPLOYEE", "ROLE_JANUS_USER", "SCOPE_read"))).isFalse();
	}

	@Test
	void hasOnlyUserRoleShouldIgnoreNonRoleAuthorities() {
		assertThat(Roles.hasOnlyUserRole(createAuthorityList("ROLE_JANUS_USER", "SCOPE_read"))).isTrue();
	}

	@Test
	void hasOnlyUserRoleShouldRejectAnotherJanusRole() {
		assertThat(Roles.hasOnlyUserRole(
				createAuthorityList("ROLE_JANUS_USER", "ROLE_JANUS_ADMIN", "SCOPE_read"))).isFalse();
	}
}
