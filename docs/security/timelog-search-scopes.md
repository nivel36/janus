# Time log search authorization

`GET /api/v1/time-logs` searches visible time logs with optional `employeeNumber`,
`start` and `end` filters. Both instants must be supplied together;
the lower bound is inclusive and the upper bound is exclusive. Standard `page`,
`size` and `sort` parameters control pagination. The default order is descending
`entryTime`.

`canSearch(authentication, employeeNumber)` delegates execution permission to
`SearchTimeLogPolicy`, which uses the shared `EmployeeSearchPolicy`. Separately,
`SearchTimeLogPolicy.scope` determines visible records using `ViewTimeLogPolicy`:

| Actor | Execution permission | Scope |
| --- | --- | --- |
| `JANUS_USER` or `JANUS_ADMIN`, including combinations with other roles | Allowed with any optional employee filter | All employees |
| Only `JANUS_EMPLOYEE` with an associated employee | Allowed without a filter or with their own employee number | That persistent employee ID |
| Only `JANUS_EMPLOYEE` without an associated employee | Denied (403) | None |
| No recognized role | Denied (403) | None |

The employee association comes from the provisioned actor, not an email claim
or a client filter. An employee explicitly filtering for another or a nonexistent
employee receives 403 before the search service is called. Explicit filters are
never replaced. Individual time log retrieval uses `canView` and denies access to
another employee's record.

`TimeLogSearchScope` is a separate contract from the boolean `Policy<C>`.
Application callers must supply a non-null scope to `TimeLogService.searchTimeLogs`.
The HTTP controller obtains it from the authorization adapter; it is never a
request parameter. Internal work shift composition supplies the scope of the
persistent employee being processed.

The database specification combines the scope and client criteria with `AND`.
Spring Data uses that specification for both row retrieval and counting before
pagination. `None` produces a false predicate, and logically deleted time logs
remain excluded by the entity restriction.

## Shared authorization structure

Spring Security adapters resolve `Authentication` into `Actor` and load facts.
Pure policies decide permissions and contain no Spring or repository dependencies.
`EmployeeAccessPolicy` defines elevated access and employee ownership rules;
`EmployeeSearchPolicy.Context` distinguishes an absent filter from ownership of
an explicit filter. `EmployeeNumberResolver` loads persistent IDs and provides
filter facts, without deciding roles or permissions. Missing employee references
can be represented as absence; technical failures propagate.

Schedule and worksite searches use the same execution rules. Without a filter,
a restricted employee's linked number is supplied to their existing queries.
Explicit filters are preserved after authorization. Schedule assignments and
global/assigned worksite visibility retain their existing semantics. Elevated
actors retain optional client filters, including their absence.

Reading and writing employee resources, time logs, worksites and accounts retain
their existing permissions. Resource-not-found responses for elevated actors
remain specific to each operation; restricted employee filters do not disclose
whether another employee exists. No schema migration is required.
