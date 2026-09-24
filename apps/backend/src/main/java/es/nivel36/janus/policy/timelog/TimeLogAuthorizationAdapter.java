package es.nivel36.janus.policy.timelog;

import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;
import es.nivel36.janus.service.applicationsettings.ApplicationSettingsService;
import es.nivel36.janus.service.appuser.Role;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.timelog.TimeLogSearchScope;
import es.nivel36.janus.util.EmailAddresses;

@Component("timeLogAuthorization")
public class TimeLogAuthorizationAdapter {
	private final ActorResolver actors;
	private final EmployeeService employees;
	private final ApplicationSettingsService settings;
	private final OperateTimeLogPolicy operate = new OperateTimeLogPolicy();
	private final ViewTimeLogPolicy view = new ViewTimeLogPolicy();
	private final SearchTimeLogPolicy search = new SearchTimeLogPolicy();
	private final DeleteTimeLogPolicy delete = new DeleteTimeLogPolicy();

	public TimeLogAuthorizationAdapter(final ActorResolver a, final EmployeeService e,
			final ApplicationSettingsService s) {
		this.actors = Objects.requireNonNull(a);
		this.employees = Objects.requireNonNull(e);
		this.settings = Objects.requireNonNull(s);
	}

	public boolean canOperate(final Authentication auth, final String email, final boolean manual) {
		final Actor a = this.actors.resolve(auth);
		return this.operate.allows(a, new OperateTimeLogPolicy.Context(this.ownsOrWillBeScoped(a, email), manual,
				this.settings.isEmployeeManualTimelogEntryAllowed()));
	}

	public boolean canView(final Authentication auth, final String email) {
		final Actor a = this.actors.resolve(auth);
		return this.view.allows(a, !this.restricted(a) || this.owns(a, email));
	}

	/**
	 * Resolves the employee email used by an operation. Employee-only users are
	 * always scoped through their immutable AppUser-to-Employee link, rather than
	 * through the (potentially stale) email claim supplied by the client.
	 */
	public String effectiveEmployeeEmail(final Authentication auth, final String requested) {
		final Actor actor = this.actors.resolve(auth);
		return this.restricted(actor) ? this.employees.findEmployeeById(actor.employeeId()).getEmail() : requested;
	}

	public boolean canDelete(final Authentication auth) {
		return this.delete.allows(this.actors.resolve(auth), null);
	}

	public boolean canSearch(final Authentication auth) {
		final Actor actor = this.actors.resolve(auth);
		return actor.hasRole(Role.JANUS_EMPLOYEE) || actor.hasRole(Role.JANUS_USER) || actor.hasRole(Role.JANUS_ADMIN);
	}

	public TimeLogSearchScope searchScope(final Authentication auth) {
		return this.search.scope(this.actors.resolve(auth));
	}

	private boolean owns(final Actor a, final String email) {
		if (a.employeeId() == null) {
			return false;
		}
		return this.employees.findEmployeeByEmail(EmailAddresses.canonicalize(email))
				.map(employee -> Objects.equals(a.employeeId(), employee.getId()))
				.orElse(false);
	}

	private boolean ownsOrWillBeScoped(final Actor actor, final String email) {
		return this.restricted(actor) ? actor.employeeId() != null : this.owns(actor, email);
	}

	private boolean restricted(final Actor a) {
		return a.hasRole(Role.JANUS_EMPLOYEE) && !a.hasRole(Role.JANUS_USER) && !a.hasRole(Role.JANUS_ADMIN);
	}
}
