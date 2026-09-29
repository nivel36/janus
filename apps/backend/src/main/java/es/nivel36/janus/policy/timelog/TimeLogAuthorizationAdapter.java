package es.nivel36.janus.policy.timelog;

import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import es.nivel36.janus.policy.EmployeeNumberResolver;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.security.ActorResolver;
import es.nivel36.janus.service.applicationsettings.ApplicationSettingsService;
import es.nivel36.janus.service.appuser.Role;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.timelog.TimeLogSearchScope;

@Component("timeLogAuthorization")
public class TimeLogAuthorizationAdapter {
	private final ActorResolver actors;
	private final ApplicationSettingsService settings;
	private final EmployeeNumberResolver employeeNumbers;
	private final EmployeeService employeeService;
	private final OperateTimeLogPolicy operate = new OperateTimeLogPolicy();
	private final ViewTimeLogPolicy view = new ViewTimeLogPolicy();
	private final SearchTimeLogPolicy search = new SearchTimeLogPolicy();
	private final DeleteTimeLogPolicy delete = new DeleteTimeLogPolicy();

	public TimeLogAuthorizationAdapter(final ActorResolver a, final ApplicationSettingsService s,
			final EmployeeNumberResolver employeeNumbers, final EmployeeService employeeService) {
		this.actors = Objects.requireNonNull(a);
		this.settings = Objects.requireNonNull(s);
		this.employeeNumbers = Objects.requireNonNull(employeeNumbers);
		this.employeeService = Objects.requireNonNull(employeeService);
	}

	public boolean canOperate(final Authentication auth, final String email, final boolean manual) {
		final Actor a = this.actors.resolve(auth);
		return this.operate.allows(a, new OperateTimeLogPolicy.Context(this.ownsEmployeeNumber(a, email), manual,
				this.settings.isEmployeeManualTimelogEntryAllowed()));
	}

	public boolean canView(final Authentication auth, final String employeeSelector) {
		final Actor a = this.actors.resolve(auth);
		return this.view.allows(a, !this.restricted(a) || this.ownsEmployeeNumber(a, employeeSelector));
	}

	/** Compatibility authorization for the deprecated routes whose identifier is an email. */
	public boolean canOperateByEmail(final Authentication auth, final String email, final boolean manual) {
		final Actor actor = this.actors.resolve(auth);
		return this.operate.allows(actor, new OperateTimeLogPolicy.Context(this.ownsEmployeeEmail(actor, email), manual,
				this.settings.isEmployeeManualTimelogEntryAllowed()));
	}

	/** Compatibility authorization for the deprecated routes whose identifier is an email. */
	public boolean canViewByEmail(final Authentication auth, final String email) {
		final Actor actor = this.actors.resolve(auth);
		return this.view.allows(actor, !this.restricted(actor) || this.ownsEmployeeEmail(actor, email));
	}

	/**
	 * Resolves the employee number used by a search. Employee-only users are
	 * always scoped through their immutable AppUser-to-Employee link, rather than
	 * through the (potentially stale) identifier supplied by the client.
	 */
	public String effectiveEmployeeNumber(final Authentication auth, final String requested) {
		final Actor actor = this.actors.resolve(auth);
		return this.employeeNumbers.effectiveNumber(actor, requested, this.restricted(actor));
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

	private boolean ownsEmployeeNumber(final Actor actor, final String employeeNumber) {
		if (actor.employeeId() == null) {
			return false;
		}
		try {
			return actor.employeeId()
					.equals(this.employeeService.findEmployeeByEmployeeNumber(employeeNumber).getId());
		} catch (final ResourceNotFoundException exception) {
			return false;
		}
	}

	private boolean ownsEmployeeEmail(final Actor actor, final String email) {
		if (actor.employeeId() == null) {
			return false;
		}
		return this.employeeService.findEmployeeByEmail(email)
				.map(employee -> actor.employeeId().equals(employee.getId())).orElse(false);
	}

	private boolean restricted(final Actor a) {
		return a.hasRole(Role.JANUS_EMPLOYEE) && !a.hasRole(Role.JANUS_USER) && !a.hasRole(Role.JANUS_ADMIN);
	}
}
