# External identity and AppUser provisioning

Janus authenticates an application user by the OpenID Connect `sub` claim. The
accepted issuer is fixed by `spring.security.oauth2.resourceserver.jwt.issuer-uri`,
so `AppUser` stores only the stable Keycloak account UUID as `keycloakSubject`.
`username` is a functional/display name and `email` is a contact attribute; neither
is the persistent identity key or used to authorize an `AppUser`. The dedicated
`employeeNumber` claim is the only claim used to suggest an employee link.

## Identity and profile lifecycle

A Keycloak account and a local `AppUser` are different records with different
responsibilities:

* Keycloak owns credentials, login, account status, and client-role assignments.
* Janus owns the local profile (`AppUser`), including preferences and the optional
  employee association. It does not store credentials or use this profile to
  authenticate the request.

Creating an account in Keycloak does not immediately insert an `AppUser`. On the
first authenticated `GET /api/v1/appusers/me`, Janus accepts only a validated JWT
with at least one supported Janus client role. It looks up the profile by `sub` and,
when none exists, creates it with `preferred_username` as its initial display name
and the configured `janus.user-provisioning.defaults`. The response returns that
new profile. Janus normalizes the optional `employeeNumber` claim by trimming it and validates
it with the OpenAPI rule (`^[A-Za-z0-9_-]{1,50}$`). A malformed claim rejects the
request with `400 Bad Request`. A valid claim links the matching employee when it is
not already linked. The explicit product policy for a missing or unknown claim is to
create an unassociated account; administrative linking can be performed later. If the employee belongs to another
identity, Janus preserves that association, creates the new profile without an
employee, and logs the conflict. Later requests find the same profile by `sub`; they update its contact email from
the verified `email` claim but never relink it when `employeeNumber` changes. Concurrent first requests converge
on the single profile protected by the unique subject constraint.

The development realm still contains the example Keycloak account with the stable
UUID `9a60b9f4-7436-4d93-9c25-08e08f3dfc58` and its `janus-api` client roles. Its
local `AppUser` is deliberately absent at startup and is created by the first
authorized request. Keycloak's standard `iss` and `sub` claims must remain
unchanged; in particular, no mapper may replace `sub` with email.

`POST /api/v1/appusers` remains restricted to `JANUS_ADMIN` for explicit profile
management, for example associating an employee before that person first accesses
Janus. It is not a required bootstrap step. An administrator must obtain the
account UUID from Keycloak and send it as `keycloakSubject`; Janus validates it as
a UUID and permits each subject to be linked only once. Personal `GET` and `PUT`
operations use `/api/v1/appusers/me` and never accept an identity selector.

Disabling or deleting either record does not automatically modify the other:
Keycloak controls whether future tokens can be issued, while retention or deletion
of the `AppUser` follows Janus's application-data policy.

## Recovering a recreated Keycloak account

`username` remains unique. Consequently, if Keycloak deletes and recreates an
account, its new `sub` is **not** linked automatically even when
`preferred_username` or verified email matches the old profile. First access with
the new token returns `409 Conflict` with problem type
`urn:problem:external-identity-conflict`; the old profile and employee link remain
unchanged.

Recovery is an exceptional database-administration procedure; Janus deliberately
does not expose an API or application service for changing a linked subject:

1. Disable the old Keycloak account and verify, outside Janus, that the owner of
   the new account is the same person (using the organization's authoritative
   identity records, not username or email alone).
2. Verify that the new Keycloak UUID is not assigned to any other Janus profile.
3. Replace `APP_USER.KEYCLOAK_SUBJECT` directly in the database inside a controlled
   transaction.
4. Ask the user to retry `GET /api/v1/appusers/me` with a token issued for the new
   account, and audit the administrative change according to local policy.

The database unique constraint rejects a subject already owned by another profile.
The operation must change only `KEYCLOAK_SUBJECT`, preserving preferences and the
employee association, and must be recorded according to local audit policy.

## Identity-provider claim rollout

Before deploying this version, configure the identity provider/client scope to emit
an `employeeNumber` string claim in access tokens. Its value must be copied from the
authoritative immutable personnel identifier and match the value stored in
`EMPLOYEE.EMPLOYEE_NUMBER`; do not derive it from email or username. Roll out and
verify the mapper before the application deployment when automatic linking is
required. During a staged rollout, tokens without the claim remain usable but create
unassociated accounts. Unknown numbers do the same and are logged for operations;
malformed values are rejected. Existing `AppUser` employee links are preserved and
must not be backfilled by email. After rollout, test one matching, one missing, and
one unknown identifier, and monitor unassociated accounts for administrative review.

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
