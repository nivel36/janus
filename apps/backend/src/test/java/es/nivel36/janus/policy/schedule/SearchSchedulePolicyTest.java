package es.nivel36.janus.policy.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;

class SearchSchedulePolicyTest {
	private final SearchSchedulePolicy policy = new SearchSchedulePolicy();

	@ParameterizedTest
	@MethodSource("searchCases")
	void onlyUsersAndAdministratorsCanSearchRegardlessOfEmployeeAssociation(
			final Set<Role> roles,
			final Long employeeId,
			final boolean expected) {
		final UUID userId = UUID.randomUUID();
		final Actor actor = new Actor(userId, roles, employeeId);
		assertThat(this.policy.allows(actor, null)).isEqualTo(expected);
	}

	static Stream<Arguments> searchCases() {
		final Stream.Builder<Arguments> cases = Stream.builder();
		for (int mask = 0; mask < 8; mask++) {
			final Set<Role> roles = new HashSet<>();
			if ((mask & 1) != 0) {
				roles.add(Role.JANUS_EMPLOYEE);
			}
			if ((mask & 2) != 0) {
				roles.add(Role.JANUS_USER);
			}
			if ((mask & 4) != 0) {
				roles.add(Role.JANUS_ADMIN);
			}
			final boolean elevated = (mask & 6) != 0;
			for (final Long employeeId : new Long[] { null, -1L, 0L, 84L }) {
				final Arguments testCase = Arguments.of(roles, employeeId, elevated);
				cases.add(testCase);
			}
		}
		return cases.build();
	}
}
