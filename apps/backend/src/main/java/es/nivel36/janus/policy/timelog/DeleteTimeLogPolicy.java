package es.nivel36.janus.policy.timelog;
import es.nivel36.janus.policy.RolePolicy; import es.nivel36.janus.service.appuser.Role;
public final class DeleteTimeLogPolicy extends RolePolicy { public DeleteTimeLogPolicy(){super(Role.JANUS_ADMIN);} }
