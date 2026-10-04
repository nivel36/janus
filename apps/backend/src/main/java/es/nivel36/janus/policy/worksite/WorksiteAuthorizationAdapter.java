package es.nivel36.janus.policy.worksite;

import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import es.nivel36.janus.policy.EmployeeAccessPolicy;
import es.nivel36.janus.policy.EmployeeNumberResolver;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.applicationsettings.ApplicationSettingsService;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.worksite.WorksiteScope;
import es.nivel36.janus.service.worksite.WorksiteService;

@Component("worksiteAuthorization")
public class WorksiteAuthorizationAdapter {
	private final ActorResolver actors;
	private final EmployeeService employees;
	private final WorksiteService worksites;
	private final ApplicationSettingsService settings;
	private final EmployeeNumberResolver employeeNumbers;
	private final SearchWorksitePolicy search = new SearchWorksitePolicy();
	private final ViewWorksitePolicy view = new ViewWorksitePolicy();
	private final ViewWorksiteStatsPolicy stats = new ViewWorksiteStatsPolicy();
	private final CreateWorksitePolicy create = new CreateWorksitePolicy();
	private final UpdateWorksitePolicy update = new UpdateWorksitePolicy();
	private final DeleteWorksitePolicy delete = new DeleteWorksitePolicy();
	private final ManageWorksiteAssignmentsPolicy assignments = new ManageWorksiteAssignmentsPolicy();

	public WorksiteAuthorizationAdapter(final ActorResolver actors, final EmployeeService employees,
			final WorksiteService worksites, final ApplicationSettingsService settings,
			final EmployeeNumberResolver employeeNumbers) {
		this.actors = Objects.requireNonNull(actors);
		this.employees = Objects.requireNonNull(employees);
		this.worksites = Objects.requireNonNull(worksites);
		this.settings = Objects.requireNonNull(settings);
		this.employeeNumbers = Objects.requireNonNull(employeeNumbers);
	}

	public boolean canSearch(final Authentication auth, final String employeeNumber) {
		final Actor a = this.actors.resolve(auth);
		return this.search.allows(a, this.employeeNumbers.searchContext(a, employeeNumber,
				EmployeeAccessPolicy.isRestrictedToOwnEmployee(a)));
	}

	public String effectiveEmployeeNumber(final Authentication auth, final String requested) {
		final Actor a = this.actors.resolve(auth);
		return this.employeeNumbers.effectiveNumber(a, requested, EmployeeAccessPolicy.isRestrictedToOwnEmployee(a));
	}

	public boolean canView(final Authentication auth, final String code) {
		final Actor actor = this.actors.resolve(auth);
		return this.view.allows(actor, this.viewContext(actor, code));
	}

	private ViewWorksitePolicy.Context viewContext(final Actor actor, final String code) {
		final ViewWorksitePolicy.Context missing = new ViewWorksitePolicy.Context(false, null, false);
		if (!EmployeeAccessPolicy.isRestrictedToOwnEmployee(actor) || actor.employeeId() == null) {
			return missing;
		}
		try {
			final WorksiteScope scope = this.worksites.findWorksiteByCode(code).getScope();
			return new ViewWorksitePolicy.Context(true, scope,
					scope == WorksiteScope.ASSIGNED && this.assigned(actor, code));
		} catch (final ResourceNotFoundException ex) {
			return missing;
		}
	}

	public boolean canViewStats(final Authentication auth, final String code) {
		final Actor a = this.actors.resolve(auth);
		return this.stats.allows(a, new ViewWorksiteStatsPolicy.Context(this.assigned(a, code)));
	}

	public boolean canCreate(final Authentication auth, final WorksiteScope scope) {
		final Actor actor = this.actors.resolve(auth);
		final boolean needsEmployeeFacts = !EmployeeAccessPolicy.hasElevatedAccess(actor);
		return this.create.allows(actor,
				new CreateWorksitePolicy.Context(
						needsEmployeeFacts && this.settings.isEmployeeWorkplaceCreationAllowed(),
						scope == WorksiteScope.ASSIGNED));
	}

	public boolean canUpdate(final Authentication auth, final String code, final WorksiteScope scope) {
		final Actor actor = this.actors.resolve(auth);
		final boolean needsEmployeeFacts = !EmployeeAccessPolicy.hasElevatedAccess(actor);
		return this.update.allows(actor,
				new UpdateWorksitePolicy.Context(
						needsEmployeeFacts && this.settings.isEmployeeWorkplaceCreationAllowed(),
						scope == WorksiteScope.ASSIGNED, needsEmployeeFacts && this.assigned(actor, code)));
	}

	public boolean canDelete(final Authentication auth) {
		return this.delete.allows(this.actors.resolve(auth), null);
	}

	public boolean canManageAssignments(final Authentication auth) {
		return this.assignments.allows(this.actors.resolve(auth), null);
	}

	private boolean assigned(final Actor a, final String code) {
		return a.employeeId() != null && this.employees.isAssignedToWorksite(a.employeeId(), code);
	}
}
