package es.nivel36.janus.policy.worksite;

import java.util.Objects;
import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;

public final class CreateWorksitePolicy implements Policy<CreateWorksitePolicy.Context> {
 @Override public boolean allows(final Actor actor, final CreateWorksitePolicy.Context context) {
  Objects.requireNonNull(actor, "actor can't be null");
  Objects.requireNonNull(context, "context can't be null");
  return elevated(actor) || actor.hasRole(Role.JANUS_EMPLOYEE) && context.employeeCreationAllowed() && context.assignedScope();
 }
 private boolean elevated(Actor actor) { return actor.hasRole(Role.JANUS_USER) || actor.hasRole(Role.JANUS_ADMIN);
 }
 public record Context(boolean employeeCreationAllowed, boolean assignedScope) {
 }
}
