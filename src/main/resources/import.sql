INSERT INTO schedule (id, name) VALUES (1, 'Standard Work Hours with August Variation');

INSERT INTO schedule_rule (id, name, schedule_id, start_date, end_date) VALUES (1, 'Regular Work Hours', 1, '2024-01-01', '2024-07-31');

INSERT INTO schedule_rule (id, name, schedule_id, start_date, end_date)  VALUES (2, 'August Work Hours', 1, '2024-08-01', '2024-08-31');

INSERT INTO schedule_rule (id, name, schedule_id, start_date, end_date) VALUES (3, 'Post August Regular Work Hours', 1, '2024-09-01', '2024-12-31');

INSERT INTO day_of_week_time_range (id, name, day_of_week, start_time, end_time, schedule_rule_id) VALUES (1, 'Monday 8:00 - 17:30', 'MONDAY', '08:00:00'::TIME, '17:30:00'::TIME, 1);
INSERT INTO day_of_week_time_range (id, name, day_of_week, start_time, end_time, schedule_rule_id) VALUES (2, 'Tuesday 8:00 - 17:30', 'TUESDAY', '08:00:00'::TIME, '17:30:00'::TIME, 1);
INSERT INTO day_of_week_time_range (id, name, day_of_week, start_time, end_time, schedule_rule_id) VALUES (3, 'Wednesday 8:00 - 17:30', 'WEDNESDAY', '08:00:00'::TIME, '17:30:00'::TIME, 1);
INSERT INTO day_of_week_time_range (id, name, day_of_week, start_time, end_time, schedule_rule_id) VALUES (4, 'Thursday 8:00 - 17:30', 'THURSDAY', '08:00:00'::TIME, '17:30:00'::TIME, 1);
INSERT INTO day_of_week_time_range (id, name, day_of_week, start_time, end_time, schedule_rule_id) VALUES (5, 'Friday 8:00 - 15:00', 'FRIDAY', '08:00:00'::TIME, '15:00:00'::TIME, 1);
INSERT INTO day_of_week_time_range (id, name, day_of_week, start_time, end_time, schedule_rule_id) VALUES (6, 'Monday August 8:00 - 15:00', 'MONDAY', '08:00:00'::TIME, '15:00:00'::TIME, 2);
INSERT INTO day_of_week_time_range (id, name, day_of_week, start_time, end_time, schedule_rule_id) VALUES (7, 'Tuesday August 8:00 - 15:00', 'TUESDAY', '08:00:00'::TIME, '15:00:00'::TIME, 2);
INSERT INTO day_of_week_time_range (id, name, day_of_week, start_time, end_time, schedule_rule_id) VALUES (8, 'Wednesday August 8:00 - 15:00', 'WEDNESDAY', '08:00:00'::TIME, '15:00:00'::TIME, 2);
INSERT INTO day_of_week_time_range (id, name, day_of_week, start_time, end_time, schedule_rule_id) VALUES (9, 'Thursday August 8:00 - 15:00', 'THURSDAY', '08:00:00'::TIME, '15:00:00'::TIME, 2);

INSERT INTO Employee (Id, Name, Surname, Email, Schedule_Id) VALUES (1, 'Name', 'Surname', 'name@test.com', 1);

