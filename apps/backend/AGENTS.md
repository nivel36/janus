# Java formatting and validation

Use the existing Spotless plugin and Eclipse formatter configuration.

When the task modifies Java code, including tests, complete all planned edits
before running formatting and validation. Do not run the formatter after
each individual edit.

At the end of the task, run from `apps/backend`:

```bash
./mvnw spotless:apply
./mvnw verify
```

The `verify` phase includes `spotless:check`, so do not run it separately.
If validation requires further Java changes, format again after completing
those fixes, then rerun validation.
