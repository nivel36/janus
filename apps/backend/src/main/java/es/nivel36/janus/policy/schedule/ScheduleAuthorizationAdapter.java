package es.nivel36.janus.policy.schedule;

import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import es.nivel36.janus.policy.EmployeeAccessPolicy;
import es.nivel36.janus.policy.EmployeeNumberResolver;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;
import es.nivel36.janus.service.employee.EmployeeService;

/**
 * Resolves provisioned identities and employee assignments for schedule
 * policies.
 * <p>
 * Decisions read the current persisted facts without provisioning a profile.
 * Unresolvable identities raise an access-denied exception rather than
 * returning a negative policy decision.
 */
@Component("scheduleAuthorization")
public class ScheduleAuthorizationAdapter {
	private final ActorResolver actors;
	private final EmployeeService employees;
	private final EmployeeNumberResolver employeeNumbers;
	private final SearchSchedulePolicy search = new SearchSchedulePolicy();
	private final ViewSchedulePolicy view = new ViewSchedulePolicy();
	private final CreateSchedulePolicy create = new CreateSchedulePolicy();
	private final UpdateSchedulePolicy update = new UpdateSchedulePolicy();
	private final DeleteSchedulePolicy delete = new DeleteSchedulePolicy();

	public ScheduleAuthorizationAdapter(
		final ActorResolver actors,
		final EmployeeService employees,
		final EmployeeNumberResolver employeeNumbers) {
		this.actors = Objects.requireNonNull(actors);
		this.employees = Objects.requireNonNull(employees);
		this.employeeNumbers = Objects.requireNonNull(employeeNumbers);
	}

	public boolean canSearch(final Authentication auth, final String employeeNumber) {
		final Actor a = this.actors.resolve(auth);
		return this.search.allows(
				a,
				this.employeeNumbers
						.searchContext(a, employeeNumber, EmployeeAccessPolicy.isRestrictedToOwnEmployee(a)));
	}

	public String effectiveEmployeeNumber(final Authentication auth, final String requested) {
		final Actor a = this.actors.resolve(auth);
		return this.employeeNumbers.effectiveNumber(a, requested, EmployeeAccessPolicy.isRestrictedToOwnEmployee(a));
	}

	public boolean canView(final Authentication auth, final String code) {
		final Actor a = this.actors.resolve(auth);
		return this.view.allows(
				a,
				new ViewSchedulePolicy.Context(
						a.employeeId() != null && this.employees.isAssignedToSchedule(a.employeeId(), code)));
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
}
