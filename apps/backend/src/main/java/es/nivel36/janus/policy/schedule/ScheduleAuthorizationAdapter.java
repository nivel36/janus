package es.nivel36.janus.policy.schedule;

import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import es.nivel36.janus.policy.EmployeeEmailResolver;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;
import es.nivel36.janus.service.appuser.Role;
import es.nivel36.janus.service.employee.EmployeeService;

@Component("scheduleAuthorization")
public class ScheduleAuthorizationAdapter {
	private final ActorResolver actors;
	private final EmployeeService employees;
	private final EmployeeEmailResolver employeeEmails;
	private final SearchSchedulePolicy search = new SearchSchedulePolicy();
	private final ViewSchedulePolicy view = new ViewSchedulePolicy();
	private final CreateSchedulePolicy create = new CreateSchedulePolicy();
	private final UpdateSchedulePolicy update = new UpdateSchedulePolicy();
	private final DeleteSchedulePolicy delete = new DeleteSchedulePolicy();

	public ScheduleAuthorizationAdapter(final ActorResolver actors, final EmployeeService employees,
			final EmployeeEmailResolver employeeEmails) {
		this.actors = Objects.requireNonNull(actors);
		this.employees = Objects.requireNonNull(employees);
		this.employeeEmails = Objects.requireNonNull(employeeEmails);
	}

	public boolean canSearch(final Authentication auth, final String email) {
		final Actor a = this.actors.resolve(auth);
		return this.search.allows(a, this.restricted(a) ? a.employeeId() != null : this.owns(a, email));
	}

	public String effectiveEmployeeEmail(final Authentication auth, final String requested) {
		final Actor a = this.actors.resolve(auth);
		return this.employeeEmails.effectiveEmail(a, requested, this.restricted(a));
	}

	public boolean canView(final Authentication auth, final String code) {
		final Actor a = this.actors.resolve(auth);
		return this.view.allows(a, a.employeeId() != null && this.employees
				.isAssignedToSchedule(a.employeeId(), code));
	}

	public boolean canCreate(final Authentication a) {
		return this.create.allows(this.actors.resolve(a), null);
	}

	public boolean canUpdate(final Authentication a) {
		return this.update.allows(this.actors.resolve(a), null);
	}

	public boolean canDelete(final Authentication a) {
		return this.delete.allows(this.actors.resolve(a), null);
	}

	private boolean owns(final Actor a, final String email) {
		return this.employeeEmails.owns(a, email);
	}

	private boolean restricted(final Actor a) {
		return a.hasRole(Role.JANUS_EMPLOYEE) && !a.hasRole(Role.JANUS_USER) && !a.hasRole(Role.JANUS_ADMIN);
	}
}
