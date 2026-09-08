# External identity and AppUser provisioning

Janus authenticates an application user by the OpenID Connect `sub` claim. The
accepted issuer is fixed by `spring.security.oauth2.resourceserver.jwt.issuer-uri`,
so `AppUser` stores only the stable Keycloak account UUID as `keycloakSubject`.
`username` is a functional/display name and `email` is a contact attribute; neither
is used to link or authorize an `AppUser`. A verified email only proves current
control of that address.

## Administrative provisioning

`POST /api/v1/appusers` is restricted to `JANUS_ADMIN`. The administrator obtains
the account UUID from Keycloak and sends it as `keycloakSubject`. Janus validates it
as a UUID. The subject may only be linked once. Personal `GET` and `PUT` operations
use `/api/v1/appusers/me` and never accept an identity selector.

The development realm assigns the example account the known UUID
`9a60b9f4-7436-4d93-9c25-08e08f3dfc58`. Keycloak's standard `iss` and `sub` claims
must remain unchanged; in particular, no mapper may replace `sub` with email.

## Existing installations

Do not backfill identities by matching email or username. For every existing row:

1. locate and verify the corresponding Keycloak account administratively;
2. obtain its UUID;
3. verify that no UUID occurs more than once;
4. populate `KEYCLOAK_SUBJECT`; and
5. add the unique constraint and convert the column to `NOT NULL`.

The checked-in schemas rebuild databases and therefore declare the columns as
required immediately. Deployments that retain data must perform the staged backfill
above before applying those final constraints.

## Employee authorization

Administrative creation and update payloads accept `employeeId`. Janus resolves
that database identifier and persists the one-to-one association with
`AppUser.setEmployee`; omitting `employeeId` on update preserves the current association. Both the
subject and employee foreign key are protected by `UK_APP_USER_KEYCLOAK_SUBJECT`
and `UK_APP_USER_EMPLOYEE`, and the service rejects an already-linked value before
the database constraint is reached.

Requests made with the restricted `JANUS_EMPLOYEE` role resolve the employee from
`Authentication.getName()` (the validated OIDC `sub`) and the persisted `AppUser`
association. Authorization compares the employee database identity; token email,
username and other mutable claims are ignored. A provisioned account without an
employee association receives `403 Forbidden` with a generic message that does not
disclose subjects or internal email addresses.
