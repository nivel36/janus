# Keycloak role mapping

Janus accepts only client roles in `resource_access[clientId].roles`, where
`clientId` is the required `janus.security.client-id` setting. Roles assigned to
another resource client cannot grant permissions in Janus.

## Decision on realm roles

Roles in `realm_access.roles` are **not accepted**. Realm roles are global to a
Keycloak realm rather than scoped to the Janus resource, so accepting them would
allow an unrelated client or realm-wide assignment to grant Janus privileges.
Deployments that currently use realm roles must map them to client roles for the
configured Janus client instead.
