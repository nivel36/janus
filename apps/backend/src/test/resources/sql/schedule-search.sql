INSERT INTO schedule(id, code, name) VALUES
(101, 'SCH-A', 'Alpha shift'),
(102, 'SCH-B', 'Beta shift'),
(103, 'SCH-C', 'Gamma shift'),
(104, 'SCH-D', 'Literal % shift'),
(105, 'SCH-E', 'Literal _ shift'),
(106, 'SCH-F', 'Literal ! shift');
INSERT INTO schedule_rule(id, name, schedule_id) VALUES
(101, 'Alpha rule', 101), (102, 'Empty rule', 102);
INSERT INTO day_of_week_time_range(id, schedule_rule_id, day_of_week, start_time, end_time, effective_work_hours) VALUES
(101, 101, 'MONDAY', '09:00:00', '17:00:00', 28800000000000),
(102, 101, 'TUESDAY', '09:00:00', '17:00:00', 28800000000000);
INSERT INTO employee(id, employee_number, name, surname, email, schedule_id) VALUES
(101, 'EMP-0101', 'Alice', 'Search', 'alice@example.test', 101),
(102, 'EMP-0102', 'Bob', 'Search', 'bob@example.test', 102),
(103, 'EMP-0103', 'Carol', 'Search', 'carol@example.test', 101);
