# Time log search authorization

`GET /api/v1/timelogs` searches visible time logs with optional `employeeEmail`,
`fromInstant` and `toInstant` filters. Both instants must be supplied together;
the lower bound is inclusive and the upper bound is exclusive. Standard `page`,
`size` and `sort` parameters control pagination. The default order is descending
`entryTime`.

Permission to execute search is checked by `canSearch`: the actor must
have `JANUS_EMPLOYEE`, `JANUS_USER` or `JANUS_ADMIN`. Independently,
`SearchTimeLogPolicy` determines the visible records using `ViewTimeLogPolicy`:

| Actor | Scope |
| --- | --- |
| `JANUS_USER` or `JANUS_ADMIN`, including combinations with other roles | All employees |
| `JANUS_EMPLOYEE` with an associated employee | That persistent employee ID |
| `JANUS_EMPLOYEE` without an associated employee | None |
| No recognized role | None; execution is denied |

The employee association comes from the provisioned actor, not an email claim
or a client filter. An employee filtering for another employee receives an
empty page with total zero. Individual time log retrieval still uses `canView`
and denies access to another employee's record.

`TimeLogSearchScope` is a separate contract from the boolean `Policy<C>`.
Application callers must supply a non-null scope to `TimeLogService.searchTimeLogs`.
The HTTP controllers obtain it from the authorization adapter; it is never a
request parameter. Internal work shift composition supplies the scope of the
persistent employee being processed.

The database specification combines the scope and client criteria with `AND`.
Spring Data uses that specification for both row retrieval and counting before
pagination. `None` produces a false predicate, and logically deleted time logs
remain excluded by the entity restriction.
