package es.nivel36.janus.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import es.nivel36.janus.policy.schedule.SearchSchedulePolicy;
import es.nivel36.janus.policy.timelog.SearchTimeLogPolicy;
import es.nivel36.janus.policy.worksite.SearchWorksitePolicy;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;

class EmployeeSearchPolicyTest {
	@ParameterizedTest
	@MethodSource("searchCases")
	void allSearchPoliciesRespectRolesAssociationAndExplicitFilters(final Set<Role> roles,
			final Long employeeId, final boolean filterPresent, final boolean ownsEmployee,
			final boolean expected) {
		final Actor actor = new Actor(UUID.randomUUID(), roles, employeeId);
		final EmployeeSearchPolicy.Context context = new EmployeeSearchPolicy.Context(filterPresent, ownsEmployee);
		for (final Policy<EmployeeSearchPolicy.Context> policy : java.util.List.of(
				new EmployeeSearchPolicy(), new SearchTimeLogPolicy(), new SearchSchedulePolicy(),
				new SearchWorksitePolicy())) {
			assertThat(policy.allows(actor, context)).as(policy.getClass().getSimpleName()).isEqualTo(expected);
		}
	}

	static Stream<Arguments> searchCases() {
		final Stream.Builder<Arguments> cases = Stream.builder();
		for (int mask = 0; mask < 8; mask++) {
			final Set<Role> roles = new HashSet<>();
			if ((mask & 1) != 0) roles.add(Role.JANUS_EMPLOYEE);
			if ((mask & 2) != 0) roles.add(Role.JANUS_USER);
			if ((mask & 4) != 0) roles.add(Role.JANUS_ADMIN);
			for (final Long id : new Long[] { null, -1L, 0L, 84L }) {
				for (final boolean filter : new boolean[] { false, true }) {
					for (final boolean owns : new boolean[] { false, true }) {
						final boolean elevated = (mask & 6) != 0;
						cases.add(Arguments.of(roles, id, filter, owns,
								elevated || mask == 1 && id != null && id > 0 && (!filter || owns)));
					}
				}
			}
		}
		return cases.build();
	}
}
