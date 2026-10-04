# Global application settings

`GET /api/v1/application-settings` reads the configuration and
`PUT /api/v1/application-settings` replaces all five values. These operations
require a validated JWT and an existing provisioned application account.

| Operation | Accepted roles |
| --- | --- |
| GET | `JANUS_EMPLOYEE`, `JANUS_USER`, `JANUS_ADMIN` |
| PUT | `JANUS_ADMIN` |

Any combination containing an accepted role is sufficient. Reading settings
does not require a linked employee. Anonymous requests return 401; provisioned
accounts without an accepted role and unprovisioned identities are denied with
403. Permission checks remain at the HTTP boundary so internal services and
background jobs can read the configuration without a request identity.

## JSON contract and consumers

Both responses and requests use the following names:

| Property | Validation and current use |
| --- | --- |
| `daysUntilLocked` | Required non-null integer >= 0. Defines the time log modification window, work shift lookup cutoff and precomputation anchor. |
| `employeeWorksiteCreationAllowed` | Required non-null boolean. Controls creation and editing of personal worksites by employees through worksite authorization. |
| `worksiteChangeDuringShiftAllowed` | Required non-null boolean. Allows clock-out at a different worksite from clock-in. |
| `employeeManualTimeLogEntryAllowed` | Required non-null boolean. Controls employee operations supplying explicit time log timestamps; ownership and role checks still apply. |
| `defaultTimezone` | Required non-blank string accepted by Java `ZoneId`, including `UTC` and offsets. Input is trimmed and the stored zone ID is returned. |

For example:

```json
{
  "daysUntilLocked": 7,
  "employeeWorksiteCreationAllowed": true,
  "worksiteChangeDuringShiftAllowed": false,
  "employeeManualTimeLogEntryAllowed": false,
  "defaultTimezone": "Europe/Madrid"
}
```

PUT is a complete replacement. Missing, null or invalid values return 400 and
leave the stored settings unchanged. `employeeWorkplaceCreationAllowed` and
`employeeManualTimelogEntryAllowed` are not input aliases and do not satisfy
the required canonical fields. Responses emit only the canonical names.

The global timezone is currently stored and exposed but is not used as a
fallback by business operations. Work shifts use the worksite timezone;
account provisioning uses `janus.user-provisioning.defaults.default-timezone`.
Changing this property does not change those zones.

Zero is a supported modification window. Existing time log and work shift
boundary rules remain in effect; accepting zero does not grant an exemption
from those rules or from authorization.

## Persistence and encapsulation

The single row has ID=1. The primary key and `CHECK (ID = 1)` permit at most
one row; initial data provides that row. They do not prevent direct SQL
deletion. If the row is absent, every service accessor and update fails with
`MissingApplicationSettingsException` (an `IllegalStateException`); HTTP
operations return the standard 500 internal
error response. No read or update recreates the row or substitutes defaults.

`ApplicationSettingsService` is the entry point for consumers. The repository,
fixed identifier and entity update operation are confined to its package.
Public entity getters support response mapping; there are no public mutators.
The constructor and update validate the complete input before assigning any
value. Updates use transactional JPA dirty checking; reads are read-only
transactions. Existing SQL column names are retained through explicit mappings.

## Validation

After Java changes, run `./mvnw spotless:apply` followed by `./mvnw verify`
from `apps/backend`. This includes settings unit tests, HTTP integration tests,
H2 singleton constraints and OpenAPI contract checks.
