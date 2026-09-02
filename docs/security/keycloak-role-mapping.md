# Keycloak role mapping

Janus accepts only client roles in `resource_access[clientId].roles`, where
`clientId` is the required `janus.security.client-id` setting. Roles assigned to
another resource client cannot grant permissions in Janus.

## Access-token audience

The backend also requires the access token's `aud` claim to contain that same
client ID (normally `janus-api`). The `azp` claim identifies the client that
requested the token (normally `janus-spa`); it is not the intended resource and
must not be used as a substitute for `aud`.

The bundled realm export adds `janus-api` through the
`janus-api-audience` protocol mapper on the `janus-spa` client. If a decoded
token has `azp: janus-spa` but no `aud`, the running Keycloak realm predates that
mapper or was configured independently. Add an Audience mapper to the SPA
client (Included Client Audience: `janus-api`, Add to access token: enabled),
then log out and sign in again so that Keycloak issues a new access token.

Keycloak's `--import-realm` does not overwrite a realm already stored in its
database. In a disposable local development environment, the existing
`pg_keycloak_dev` volume can therefore retain an old realm configuration even
after the realm export changes. Recreate that volume before restarting the
development stack to import the current configuration; do not delete a
production identity database.

## Decision on realm roles

Roles in `realm_access.roles` are **not accepted**. Realm roles are global to a
Keycloak realm rather than scoped to the Janus resource, so accepting them would
allow an unrelated client or realm-wide assignment to grant Janus privileges.
Deployments that currently use realm roles must map them to client roles for the
configured Janus client instead.
