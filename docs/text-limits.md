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

Authenticated API request bodies are limited to 1,048,576 bytes (1 MiB), before
MVC deserialization. The backend bounds the full body even without Content-Length;
unknown properties and trailing whitespace count toward the limit. Oversized
bodies return 413 with ProblemDetail. Nginx declares the same limit for `/api/` in
Docker and Helm. Field validation failures return 400. API limits are documented
in OpenAPI and mirrored by the existing forms.

These changes update creation schemas only. No migration is added or executed;
existing databases are recreated through the project's current initialization
workflow. Historical migration scripts are unchanged.
