package es.nivel36.janus.policy.worksite;

import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;
import es.nivel36.janus.service.applicationsettings.ApplicationSettingsService;
import es.nivel36.janus.service.appuser.Role;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.worksite.WorksiteScope;

@Component("worksiteAuthorization")
public class WorksiteAuthorizationAdapter {
	private final ActorResolver actors;
	private final EmployeeService employees;
	private final ApplicationSettingsService settings;
	private final SearchWorksitePolicy search = new SearchWorksitePolicy();
	private final ViewWorksitePolicy view = new ViewWorksitePolicy();
	private final ViewWorksiteStatsPolicy stats = new ViewWorksiteStatsPolicy();
	private final CreateWorksitePolicy create = new CreateWorksitePolicy();
	private final UpdateWorksitePolicy update = new UpdateWorksitePolicy();
	private final DeleteWorksitePolicy delete = new DeleteWorksitePolicy();
	private final ManageWorksiteAssignmentsPolicy assignments = new ManageWorksiteAssignmentsPolicy();

	public WorksiteAuthorizationAdapter(final ActorResolver actors, final EmployeeService employees,
			final ApplicationSettingsService settings) {
		this.actors = Objects.requireNonNull(actors);
		this.employees = Objects.requireNonNull(employees);
		this.settings = Objects.requireNonNull(settings);
	}

	public boolean canSearch(final Authentication auth, final String email) {
		final Actor a = this.actors.resolve(auth);
		return this.search.allows(a, this.owns(a, email));
	}

	public String effectiveEmployeeEmail(final Authentication auth, final String requested) {
		final Actor a = this.actors.resolve(auth);
		return this.restricted(a) ? this.employee(a).getEmail() : requested;
	}

	public boolean canView(final Authentication auth) {
		return this.view.allows(this.actors.resolve(auth), null);
	}

	public boolean canViewStats(final Authentication auth, final String code) {
		final Actor a = this.actors.resolve(auth);
		return this.stats.allows(a, this.assigned(a, code));
	}

	public boolean canCreate(final Authentication auth, final WorksiteScope scope) {
		final Actor a = this.actors.resolve(auth);
		return this.elevated(a) || this.create.allows(a, new CreateWorksitePolicy.Context(
				this.settings.isEmployeeWorkplaceCreationAllowed(), scope == WorksiteScope.ASSIGNED));
	}

	public boolean canUpdate(final Authentication auth, final String code, final WorksiteScope scope) {
		final Actor a = this.actors.resolve(auth);
		return this.elevated(a) || this.update.allows(a,
				new UpdateWorksitePolicy.Context(this.settings.isEmployeeWorkplaceCreationAllowed(),
						scope == WorksiteScope.ASSIGNED, this.assigned(a, code)));
	}

	public boolean canDelete(final Authentication auth) {
		return this.delete.allows(this.actors.resolve(auth), null);
	}

	public boolean canManageAssignments(final Authentication auth) {
		return this.assignments.allows(this.actors.resolve(auth), null);
	}

	private boolean owns(final Actor a, final String email) {
		if (email == null || a.employeeId() == null) {
			return false;
		}
		try {
			return Objects.equals(a.employeeId(), this.employees.findEmployeeByEmail(email).getId());
		} catch (final RuntimeException ex) {
			return false;
		}
	}

	private boolean assigned(final Actor a, final String code) {
		return a.employeeId() != null && this.employees.isAssignedToWorksite(this.employee(a).getEmail(), code);
	}

	private Employee employee(final Actor a) {
		return this.employees.findEmployeeById(a.employeeId());
	}

	private boolean elevated(final Actor a) {
		return a.hasRole(Role.JANUS_USER) || a.hasRole(Role.JANUS_ADMIN);
	}

	private boolean restricted(final Actor a) {
		return a.hasRole(Role.JANUS_EMPLOYEE) && !a.hasRole(Role.JANUS_USER) && !a.hasRole(Role.JANUS_ADMIN);
	}
}
