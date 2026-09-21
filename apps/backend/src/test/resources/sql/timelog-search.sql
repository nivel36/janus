-- Copyright 2026 Abel Ferrer Jiménez
-- Licensed under the Apache License, Version 2.0 (the "License").

INSERT INTO schedule(id, code, name)
VALUES (101, 'SEARCH-HOURS', 'Search fixture work hours');

INSERT INTO employee(id, name, surname, email, schedule_id) VALUES
    (101, 'Alice', 'Search', 'alice@example.test', 101),
    (102, 'Bob', 'Search', 'bob@example.test', 101),
    (103, 'Carol', 'Search', 'carol@example.test', 101);

INSERT INTO app_user(email, keycloak_subject, locale, time_format, default_timezone, employee_id)
VALUES ('alice-account', '11111111-1111-4111-8111-111111111111', 'en-US', 'H24', 'UTC', 101);

INSERT INTO worksite(id, code, name, time_zone, scope)
VALUES (101, 'SEARCH-HQ', 'Search fixture worksite', 'UTC', 'GLOBAL');

-- Foreign records are interleaved with Alice's records in entry-time order.
-- Explicit UTC offsets preserve these instants when H2 converts to its local TIMESTAMP column.
INSERT INTO time_log(id, employee_id, worksite_id, entry_time) VALUES
    (101, 102, 101, TIMESTAMP WITH TIME ZONE '2025-07-01 07:00:00+00:00'),
    (102, 101, 101, TIMESTAMP WITH TIME ZONE '2025-07-01 08:00:00+00:00'),
    (103, 103, 101, TIMESTAMP WITH TIME ZONE '2025-07-01 09:00:00+00:00'),
    (104, 102, 101, TIMESTAMP WITH TIME ZONE '2025-07-02 07:00:00+00:00'),
    (105, 101, 101, TIMESTAMP WITH TIME ZONE '2025-07-02 08:00:00+00:00'),
    (106, 102, 101, TIMESTAMP WITH TIME ZONE '2025-07-03 07:00:00+00:00'),
    (107, 101, 101, TIMESTAMP WITH TIME ZONE '2025-07-03 08:00:00+00:00'),
    (108, 103, 101, TIMESTAMP WITH TIME ZONE '2025-07-03 09:00:00+00:00'),
    (109, 102, 101, TIMESTAMP WITH TIME ZONE '2025-07-04 07:00:00+00:00'),
    (110, 101, 101, TIMESTAMP WITH TIME ZONE '2025-07-04 08:00:00+00:00'),
    (111, 101, 101, TIMESTAMP WITH TIME ZONE '2025-07-05 08:00:00+00:00');

INSERT INTO time_log(id, employee_id, worksite_id, entry_time, deleted) VALUES
    (112, 101, 101, TIMESTAMP WITH TIME ZONE '2025-07-02 08:30:00+00:00', true),
    (113, 102, 101, TIMESTAMP WITH TIME ZONE '2025-07-02 07:30:00+00:00', true);
