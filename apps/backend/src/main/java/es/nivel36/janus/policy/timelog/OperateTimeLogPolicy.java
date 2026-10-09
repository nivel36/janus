package es.nivel36.janus.policy.timelog;

import java.util.Objects;

import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;

/**
 * Authorizes time-log writes for employees owning the target employee record.
 * <p>
 * Manual timestamps additionally require manual entry to be enabled. Elevated
 * roles alone do not grant these operations. Actor and context must be
 * non-null.
 */
public final class OperateTimeLogPolicy implements Policy<OperateTimeLogPolicy.Context> {
	@Override
	public boolean allows(final Actor actor, final OperateTimeLogPolicy.Context context) {
		Objects.requireNonNull(actor, "actor can't be null");
		Objects.requireNonNull(context, "context can't be null");
		return actor.hasRole(Role.JANUS_EMPLOYEE) && context.ownsEmployee()
				&& (!context.manualEntry() || context.manualEntryAllowed());
	}

	/**
	 * Ownership and manual-entry facts for a time-log write.
	 *
	 * @param ownsEmployee       whether the target is the actor's employee
	 * @param manualEntry        whether timestamps are explicitly supplied
	 * @param manualEntryAllowed whether the application permits manual entry
	 */
	public record Context(boolean ownsEmployee, boolean manualEntry, boolean manualEntryAllowed) {
	}
}
