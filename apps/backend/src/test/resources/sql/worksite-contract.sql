-- Copyright 2026 Abel Ferrer Jiménez
-- Licensed under the Apache License, Version 2.0 (the "License").

INSERT INTO schedule(id, code, name) VALUES
    (201, 'WORKSITE-DAY', 'Day shift'), (202, 'WORKSITE-NIGHT', 'Night shift');
INSERT INTO employee(id, employee_number, name, surname, email, schedule_id) VALUES
    (201, 'EMP-0201', 'First', 'Employee', 'first@example.test', 201),
    (202, 'EMP-0202', 'Second', 'Employee', 'second@example.test', 202);
-- Insert equal names in reverse code order to exercise paging tie-breaks.
INSERT INTO worksite(id, code, name, time_zone, scope, description, address, deleted) VALUES
    (202, 'CONTRACT-B', 'Shared depot', 'UTC', 'ASSIGNED', NULL, NULL, false),
    (201, 'CONTRACT-A', 'Shared depot', 'UTC', 'ASSIGNED', 'Old description', 'Old address', false),
    (203, 'CONTRACT-G', 'Global office', 'UTC', 'GLOBAL', NULL, NULL, false),
    (204, 'CONTRACT-D', 'Deleted depot', 'UTC', 'GLOBAL', NULL, NULL, true);
INSERT INTO employee_worksite(employee_id, worksite_id) VALUES (201, 201), (202, 202);
-- Count entry times in [08:00, 12:00); ignore deleted logs and other worksites.
INSERT INTO time_log(id, employee_id, worksite_id, entry_time, exit_time, deleted) VALUES
    (201, 201, 201, TIMESTAMP WITH TIME ZONE '2026-10-01 08:00:00+00:00', TIMESTAMP WITH TIME ZONE '2026-10-01 08:30:00+00:00', false),
    (202, 201, 201, TIMESTAMP WITH TIME ZONE '2026-10-01 09:00:00+00:00', TIMESTAMP WITH TIME ZONE '2026-10-01 09:30:00+00:00', false),
    (203, 202, 201, TIMESTAMP WITH TIME ZONE '2026-10-01 10:00:00+00:00', NULL, false),
    (204, 201, 201, TIMESTAMP WITH TIME ZONE '2026-10-01 07:59:59+00:00', NULL, false),
    (205, 201, 201, TIMESTAMP WITH TIME ZONE '2026-10-01 12:00:00+00:00', NULL, false),
    (206, 202, 202, TIMESTAMP WITH TIME ZONE '2026-10-01 10:00:00+00:00', NULL, false),
    (207, 201, 201, TIMESTAMP WITH TIME ZONE '2026-10-01 11:00:00+00:00', NULL, true);
