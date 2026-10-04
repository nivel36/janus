package es.nivel36.janus.policy.timelog;

import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import es.nivel36.janus.policy.EmployeeAccessPolicy;
import es.nivel36.janus.policy.EmployeeNumberResolver;
import es.nivel36.janus.policy.EmployeeSearchPolicy.Context;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;
import es.nivel36.janus.service.applicationsettings.ApplicationSettingsService;
import es.nivel36.janus.service.timelog.TimeLogSearchScope;

/**
 * Resolves Spring authentication and external facts for pure time log policies.
 */
@Component("timeLogAuthorization")
public class TimeLogAuthorizationAdapter {

	private final ActorResolver actorResolver;
	private final ApplicationSettingsService applicationSettingsService;
	private final EmployeeNumberResolver employeeNumberResolver;
	private final OperateTimeLogPolicy operateTimeLogPolicy = new OperateTimeLogPolicy();
	private final ViewTimeLogPolicy viewTimeLogPolicy = new ViewTimeLogPolicy();
	private final SearchTimeLogPolicy searchTimeLogPolicy = new SearchTimeLogPolicy();
	private final DeleteTimeLogPolicy deleteTimeLogPolicy = new DeleteTimeLogPolicy();

	public TimeLogAuthorizationAdapter(
		final ActorResolver actorResolver,
		final ApplicationSettingsService applicationSettingsService,
		final EmployeeNumberResolver employeeNumberResolver) {
		this.actorResolver = Objects.requireNonNull(actorResolver, "actorResolver");
		this.applicationSettingsService = Objects
				.requireNonNull(applicationSettingsService, "applicationSettingsService");
		this.employeeNumberResolver = Objects.requireNonNull(employeeNumberResolver, "employeeNumberResolver");
	}

	public boolean canOperate(
			final Authentication authentication,
			final String employeeNumber,
			final boolean manualEntry) {
		final Actor actor = this.getActor(authentication);
		final boolean employeeManualTimeLogEntryAllowed = this.applicationSettingsService
				.isEmployeeManualTimeLogEntryAllowed();
		final boolean owns = this.employeeNumberResolver.owns(actor, employeeNumber);
		final OperateTimeLogPolicy.Context context = new OperateTimeLogPolicy.Context(
				owns,
				manualEntry,
				employeeManualTimeLogEntryAllowed);
		return this.operateTimeLogPolicy.allows(actor, context);
	}

	private Actor getActor(final Authentication authentication) {
		return this.actorResolver.resolve(authentication);
	}

	public boolean canView(final Authentication authentication, final String employeeNumber) {
		final Actor actor = this.getActor(authentication);
		final boolean ownsEmployee = EmployeeAccessPolicy.isRestrictedToOwnEmployee(actor)
				&& this.employeeNumberResolver.owns(actor, employeeNumber);
		return this.viewTimeLogPolicy.allows(actor, new EmployeeAccessPolicy.Context(ownsEmployee));
	}

	public boolean canDelete(final Authentication authentication) {
		return this.deleteTimeLogPolicy.allows(this.getActor(authentication), null);
	}

	public boolean canSearch(final Authentication authentication, final String employeeNumber) {
		final Actor actor = this.getActor(authentication);
		final boolean restrictedToOwnEmployee = EmployeeAccessPolicy.isRestrictedToOwnEmployee(actor);
		final Context searchContext = this.employeeNumberResolver
				.searchContext(actor, employeeNumber, restrictedToOwnEmployee);
		return this.searchTimeLogPolicy.allows(actor, searchContext);
	}

	public TimeLogSearchScope searchScope(final Authentication authentication) {
		final Actor actor = this.getActor(authentication);
		return this.searchTimeLogPolicy.scope(actor);
	}
}
