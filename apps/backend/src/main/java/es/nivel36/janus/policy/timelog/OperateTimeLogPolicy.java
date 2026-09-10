package es.nivel36.janus.policy.timelog;

import java.util.Objects;
import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;

public final class OperateTimeLogPolicy implements Policy<OperateTimeLogPolicy.Context> {
 @Override public boolean allows(final Actor actor, final OperateTimeLogPolicy.Context context) {
  Objects.requireNonNull(actor, "actor can't be null");
  Objects.requireNonNull(context, "context can't be null");
  return actor.hasRole(Role.JANUS_EMPLOYEE) && context.ownsEmployee() && (!context.manualEntry() || context.manualEntryAllowed());
 }
 public record Context(boolean ownsEmployee, boolean manualEntry, boolean manualEntryAllowed) {
 }
}
