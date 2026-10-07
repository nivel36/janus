# Text storage and request limits

All textual PostgreSQL columns use `TEXT`; H2 test schemas and JPA mappings mirror
that storage policy. Length CHECK constraints apply only to stable codes and
employee numbers (1–50), Keycloak subject (1–255) and emails (at most 254).
Enumerations are restricted by value. Free text, locale and zone identifiers do
not have database length limits. Existing uniqueness, canonical email and index
constraints remain in place.

HTTP inputs preserve the existing maxima: employee names 255; worksite, schedule
and rule names 250; description and address 500; event reason 255; search query
100. Locale and every zone input are capped at 64 as a defensive application
policy, alongside semantic validation. The literal user email filter is capped
at 254 and empty input still disables the filter. Email claims and employee
requests are capped at 254, with another check after normalization. Descriptions,
addresses and reasons reject NUL, which PostgreSQL cannot store; multiline and
Unicode text remain allowed.

Nginx limits request bodies for `/api/` to 1,048,576 bytes (1 MiB) using
`client_max_body_size 1m` in Docker and Helm. The limit applies before backend
authentication, including requests without Content-Length; unknown properties
and trailing whitespace count toward it. Oversized bodies receive nginx's native
HTTP 413 response. Direct backend access, including development without nginx,
does not enforce this global body limit. Field validation failures return 400.
API limits are documented in OpenAPI and mirrored by the existing forms.

These changes update creation schemas only. No migration is added or executed;
existing databases are recreated through the project's current initialization
workflow. Historical migration scripts are unchanged.
