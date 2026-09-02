# External identity and AppUser provisioning

Janus authenticates an application user by the OpenID Connect pair `(iss, sub)`.
The issuer identifies the Keycloak realm and the subject is the stable account UUID
inside that realm. `username` is a functional/display name and `email` is a contact
attribute; neither is used to link or authorize an `AppUser`. A verified email only
proves current control of that address.

## Administrative provisioning

`POST /api/v1/appusers` is restricted to `JANUS_ADMIN`. The administrator obtains
the account UUID from Keycloak and sends it as `identitySubject`. Janus validates it
as a UUID and always supplies `identityIssuer` from its configured
`spring.security.oauth2.resourceserver.jwt.issuer-uri`; clients cannot choose the
issuer. The `(issuer, subject)` pair may only be linked once. Personal `GET` and
`PUT` operations use `/api/v1/appusers/me` and never accept an identity selector.

The development realm assigns the example account the known UUID
`9a60b9f4-7436-4d93-9c25-08e08f3dfc58`. Keycloak's standard `iss` and `sub` claims
must remain unchanged; in particular, no mapper may replace `sub` with email.

## Existing installations

Do not backfill identities by matching email or username. For every existing row:

1. locate and verify the corresponding Keycloak account administratively;
2. obtain its UUID and the configured canonical realm issuer;
3. verify that no `(issuer, UUID)` pair occurs more than once;
4. populate `IDENTITY_ISSUER` and `IDENTITY_SUBJECT`; and
5. add the composite unique constraint and convert both columns to `NOT NULL`.

The checked-in schemas rebuild databases and therefore declare the columns as
required immediately. Deployments that retain data must perform the staged backfill
above before applying those final constraints.

Employee authorization still resolves verified email claims in some controllers.
That is transitional technical debt, not an immutable identity mapping. A future
change should link `AppUser` explicitly to `Employee`; email canonicalization remains
only a domain consistency and case-insensitive uniqueness rule.
