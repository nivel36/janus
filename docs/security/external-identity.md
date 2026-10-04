# External identity and AppUser provisioning

Janus authenticates users by the validated OpenID Connect `sub` claim. The issuer
is fixed by `spring.security.oauth2.resourceserver.jwt.issuer-uri`, so the local
`keycloakSubject` stores the subject within that issuer. Subjects are opaque,
nonblank strings of at most 255 characters; they need not be UUIDs. Keycloak
normally emits account UUIDs. Username and email never identify or authorize a
local profile. The verified email is mutable contact information and is not unique.

## Identity and profile lifecycle

Keycloak owns credentials, account status and client roles. Janus owns the local
AppUser preferences and optional one-to-one Employee association. Neither record
is created or deleted automatically when the other is managed administratively.

The first `GET /api/v1/app-users/me` finds or creates the profile by `sub`. It
requires a validated JWT with a recognized Janus client role, the configured
issuer and audience, and `email_verified=true`. A missing or blank email claim
returns 400; the email is trimmed, lowercased and limited to 255 characters.
Initial locale, time format and timezone come from `janus.user-provisioning.defaults`;
the initial theme is DARK. `preferred_username` is not stored or used to link users.

The optional `employeeNumber` claim is trimmed and must match
`^[A-Za-z0-9_-]{1,50}$`. A malformed claim returns 400, including when the profile
already exists. During creation, a known, unlinked employee is associated with the
profile. Missing or unknown numbers create an unlinked profile; unknown numbers
are logged. If an employee already belongs to another subject, Janus preserves
that association, creates an unlinked profile and logs the conflict.

Later `/me` requests refresh the contact email while preserving preferences and
the employee association, even when the employeeNumber claim changes. Concurrent
creation uses an independent insert transaction and reconciles the unique subject
constraint to return a single profile. Competing subjects can share an email but
only one can claim an employee; the other profile remains unlinked. The creator
resolves the employee inside its own transaction so a failed insert cannot change
the employee relationship held by the outer transaction.

The development realm includes the example Keycloak account with subject
`9a60b9f4-7436-4d93-9c25-08e08f3dfc58`. Its local profile is absent at startup and
created on first authorized `/me` access. Standard `iss` and `sub` claims must
remain unchanged; no mapper may replace `sub` with email.

## API and permissions

All endpoints use `/api/v1/app-users`. Operations other than `/me` require an
already provisioned local profile, including operations by administrators.

| Operation | Permission | Effect |
| --- | --- | --- |
| `GET /me` | Valid JWT with JANUS_ADMIN, JANUS_USER or JANUS_EMPLOYEE | Retrieve or provision own profile and refresh email |
| `GET /app-users` (collection) | JANUS_ADMIN | Search local profiles |
| `PUT /{id}` | JANUS_ADMIN for any profile; JANUS_USER or JANUS_EMPLOYEE for own UUID | Update locale, timeFormat, defaultTimezone and theme |
| `DELETE /{id}` | JANUS_ADMIN | Delete local profile; preserve Employee and Keycloak account |

There is no POST endpoint, individual GET by UUID, PUT `/me`, or API for changing
subject or employee associations. Preference updates require every preference and
preserve contact email, identity and employee association. Missing update/delete
targets return 404 after authorization; denials return a generic 403. Anonymous
requests receive 401. Deleting a local profile does not revoke Keycloak tokens:
a later authorized `/me` request can create a fresh profile with initial preferences.

Search accepts optional `email` and `employeeNumber` query parameters. Email is a
trimmed, case-insensitive partial match of at most 255 characters of single-line,
nonblank text. `%`, `_` and `!` are literal characters. Employee number matches
exactly, case-sensitively, after trimming, and follows the claim syntax above.
Filters combine with AND. Without filters, the search includes all local profiles,
including unlinked ones. Emails can match multiple profiles.

Pagination starts at `page=0`, defaults to `size=20` and caps size at
`spring.data.rest.max-page-size` (100 by default). Public
sort fields are `id`, `email` and `employeeNumber`; the default is `email,asc` with
an ascending UUID tie-breaker. Explicit id sorting supplies its own tie-breaker.
Unsupported sort fields or invalid filters return 400. The response uses the
existing `{content, page}` shape and AppUserResponse; it does not expose the
provider subject. The OpenAPI source is `apps/backend/src/main/resources/janus.yaml`.

## Recreated accounts and existing installations

A recreated Keycloak account has a new subject. Janus does not attach it to an old
profile by matching username or email. First access creates a separate profile,
even with the same email; an employee held by the old profile remains with it.

Changing a subject is an exceptional database-administration procedure, not an
application API. Disable the old Keycloak account, verify ownership using
authoritative identity records, confirm that the new subject is unused, then
replace `APP_USER.KEYCLOAK_SUBJECT` in a controlled, audited transaction. Preserve
preferences and employee association. Retry `/api/v1/app-users/me` with the new
token. The unique subject constraint prevents assigning an already-owned subject.

For existing rows without subjects, identify the corresponding provider accounts
administratively, verify unique subjects, backfill KEYCLOAK_SUBJECT, then enforce
NOT NULL and uniqueness. Never backfill by matching email or username. The checked-in
schemas rebuild databases and declare these constraints immediately; retained
installations require a staged backfill before applying them. Employee uniqueness
is enforced by `UK_APP_USER_EMPLOYEE`, and subject uniqueness by
`UK_APP_USER_KEYCLOAK_SUBJECT`. Administrative employee linking requires a controlled
database procedure; no creation or update payload accepts employeeId.

## Employee authorization and claim rollout

JANUS_EMPLOYEE operations resolve the persisted employee association from the
validated subject and compare internal employee identity. Token email, username
and changes to employeeNumber never replace that association. A provisioned
profile without an employee association receives a generic 403 for operations
requiring an employee.

Configure the identity provider to emit employeeNumber from the authoritative
personnel identifier, matching `EMPLOYEE.EMPLOYEE_NUMBER`. Deploy and verify that
mapper before the application when automatic linking is required. Missing or
unknown claims remain usable and create unlinked profiles; malformed values are
rejected. Existing profiles are not backfilled or relinked. Test matching, missing,
unknown and conflicting numbers, and monitor unlinked profiles and conflict logs
for administrative review.
