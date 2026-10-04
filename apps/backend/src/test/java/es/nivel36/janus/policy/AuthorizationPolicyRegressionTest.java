package es.nivel36.janus.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import es.nivel36.janus.policy.appuser.UpdateCurrentAppUserPolicy;
import es.nivel36.janus.policy.timelog.OperateTimeLogPolicy;
import es.nivel36.janus.policy.worksite.CreateWorksitePolicy;
import es.nivel36.janus.policy.worksite.UpdateWorksitePolicy;
import es.nivel36.janus.policy.worksite.ViewWorksitePolicy;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;
import es.nivel36.janus.service.worksite.WorksiteScope;

class AuthorizationPolicyRegressionTest {
	private final Actor employee = new Actor(UUID.randomUUID(), Set.of(Role.JANUS_EMPLOYEE), 84L);

	@Test
	void operatingRequiresEmployeeRoleOwnershipAndManualEntryPermission() {
		final OperateTimeLogPolicy policy = new OperateTimeLogPolicy();
		assertThat(policy.allows(this.employee, new OperateTimeLogPolicy.Context(true, false, false))).isTrue();
		assertThat(policy.allows(this.employee, new OperateTimeLogPolicy.Context(true, true, false))).isFalse();
		assertThat(policy.allows(this.employee, new OperateTimeLogPolicy.Context(true, true, true))).isTrue();
		assertThat(policy.allows(this.employee, new OperateTimeLogPolicy.Context(false, false, true))).isFalse();
		assertThat(policy.allows(new Actor(UUID.randomUUID(), Set.of(Role.JANUS_ADMIN), 84L),
				new OperateTimeLogPolicy.Context(true, false, true))).isFalse();
	}

	@Test
	void worksiteVisibilityAndEditingRetainGlobalAndAssignmentRules() {
		final ViewWorksitePolicy view = new ViewWorksitePolicy();
		assertThat(view.allows(this.employee, new ViewWorksitePolicy.Context(true, WorksiteScope.GLOBAL, false)))
				.isTrue();
		assertThat(view.allows(this.employee, new ViewWorksitePolicy.Context(true, WorksiteScope.ASSIGNED, true)))
				.isTrue();
		assertThat(view.allows(this.employee, new ViewWorksitePolicy.Context(true, WorksiteScope.ASSIGNED, false)))
				.isFalse();
		assertThat(view.allows(this.employee, new ViewWorksitePolicy.Context(false, null, false))).isFalse();
		assertThat(view.allows(new Actor(UUID.randomUUID(), Set.of(Role.JANUS_EMPLOYEE), null),
				new ViewWorksitePolicy.Context(true, WorksiteScope.GLOBAL, false))).isFalse();
		final CreateWorksitePolicy create = new CreateWorksitePolicy();
		assertThat(create.allows(this.employee, new CreateWorksitePolicy.Context(true, true))).isTrue();
		assertThat(create.allows(this.employee, new CreateWorksitePolicy.Context(true, false))).isFalse();
		assertThat(create.allows(this.employee, new CreateWorksitePolicy.Context(false, true))).isFalse();
		final UpdateWorksitePolicy update = new UpdateWorksitePolicy();
		assertThat(update.allows(this.employee, new UpdateWorksitePolicy.Context(true, true, true))).isTrue();
		assertThat(update.allows(this.employee, new UpdateWorksitePolicy.Context(true, true, false))).isFalse();
	}

	@ParameterizedTest
	@EnumSource(Role.class)
	void editingAccountsKeepsAdminOverrideAndOwnAccountRestriction(final Role role) {
		final Actor actor = new Actor(UUID.randomUUID(), Set.of(role), null);
		final UpdateCurrentAppUserPolicy policy = new UpdateCurrentAppUserPolicy();
		assertThat(policy.allows(actor, actor.id())).isTrue();
		assertThat(policy.allows(actor, UUID.randomUUID())).isEqualTo(role == Role.JANUS_ADMIN);
		assertThat(policy.allows(new Actor(actor.id(), Set.of(), null), actor.id())).isFalse();
	}
}
