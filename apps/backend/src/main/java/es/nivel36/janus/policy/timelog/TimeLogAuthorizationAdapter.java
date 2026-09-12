package es.nivel36.janus.policy.timelog;

import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;
import es.nivel36.janus.service.applicationsettings.ApplicationSettingsService;
import es.nivel36.janus.service.appuser.Role;
import es.nivel36.janus.service.employee.EmployeeService;

@Component("timeLogAuthorization")
public class TimeLogAuthorizationAdapter {
	private final ActorResolver actors;
	private final EmployeeService employees;
	private final ApplicationSettingsService settings;
	private final OperateTimeLogPolicy operate = new OperateTimeLogPolicy();
	private final ViewTimeLogPolicy view = new ViewTimeLogPolicy();
	private final DeleteTimeLogPolicy delete = new DeleteTimeLogPolicy();

	public TimeLogAuthorizationAdapter(final ActorResolver a, final EmployeeService e, final ApplicationSettingsService s) {
		this.actors = Objects.requireNonNull(a);
		this.employees = Objects.requireNonNull(e);
		this.settings = Objects.requireNonNull(s);
	}

	public boolean canOperate(final Authentication auth, final String email, final boolean manual) {
		final Actor a = this.actors.resolve(auth);
		return this.operate.allows(a, new OperateTimeLogPolicy.Context(this.owns(a, email), manual,
				this.settings.isEmployeeManualTimelogEntryAllowed()));
	}

	public boolean canView(final Authentication auth, final String email) {
		final Actor a = this.actors.resolve(auth);
		return this.view.allows(a, !this.restricted(a) || this.owns(a, email));
	}

	public boolean canDelete(final Authentication auth) {
		return this.delete.allows(this.actors.resolve(auth), null);
	}

	private boolean owns(final Actor a, final String email) {
		if (a.employeeId() == null) {
			return false;
		}
		try {
			return Objects.equals(a.employeeId(), this.employees.findEmployeeByEmail(email).getId());
		} catch (final RuntimeException e) {
			return false;
		}
	}

	private boolean restricted(final Actor a) {
		return a.hasRole(Role.JANUS_EMPLOYEE) && !a.hasRole(Role.JANUS_USER) && !a.hasRole(Role.JANUS_ADMIN);
	}
}
