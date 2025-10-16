-- Copyright 2025 Abel Ferrer Jiménez
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
-- 
--     http://www.apache.org/licenses/LICENSE-2.0
-- 
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
INSERT INTO schedule (code, name) VALUES ('STD-WH-AUG-VAR', 'Standard Work Hours with August Variation');

INSERT INTO schedule_rule (name, schedule_id, start_date, end_date) VALUES ('Regular Work Hours', 1, '2024-01-01', '2024-07-31');
INSERT INTO schedule_rule (name, schedule_id, start_date, end_date) VALUES ('August Work Hours', 1, '2024-08-01', '2024-08-31');
INSERT INTO schedule_rule (name, schedule_id, start_date, end_date) VALUES ('Post August Regular Work Hours', 1, '2024-09-01', '2024-12-31');

INSERT INTO day_of_week_time_range (name, day_of_week, start_time, end_time, schedule_rule_id) VALUES ('Monday 8:00 - 17:30', 'MONDAY', '08:00:00'::TIME, '17:30:00'::TIME, 1);
INSERT INTO day_of_week_time_range (name, day_of_week, start_time, end_time, schedule_rule_id) VALUES ('Tuesday 8:00 - 17:30', 'TUESDAY', '08:00:00'::TIME, '17:30:00'::TIME, 1);
INSERT INTO day_of_week_time_range (name, day_of_week, start_time, end_time, schedule_rule_id) VALUES ('Wednesday 8:00 - 17:30', 'WEDNESDAY', '08:00:00'::TIME, '17:30:00'::TIME, 1);
INSERT INTO day_of_week_time_range (name, day_of_week, start_time, end_time, schedule_rule_id) VALUES ('Thursday 8:00 - 17:30', 'THURSDAY', '08:00:00'::TIME, '17:30:00'::TIME, 1);
INSERT INTO day_of_week_time_range (name, day_of_week, start_time, end_time, schedule_rule_id) VALUES ('Friday 8:00 - 15:00', 'FRIDAY', '08:00:00'::TIME, '15:00:00'::TIME, 1);
INSERT INTO day_of_week_time_range (name, day_of_week, start_time, end_time, schedule_rule_id) VALUES ('Monday August 8:00 - 15:00', 'MONDAY', '08:00:00'::TIME, '15:00:00'::TIME, 2);
INSERT INTO day_of_week_time_range (name, day_of_week, start_time, end_time, schedule_rule_id) VALUES ('Tuesday August 8:00 - 15:00', 'TUESDAY', '08:00:00'::TIME, '15:00:00'::TIME, 2);
INSERT INTO day_of_week_time_range (name, day_of_week, start_time, end_time, schedule_rule_id) VALUES ('Wednesday August 8:00 - 15:00', 'WEDNESDAY', '08:00:00'::TIME, '15:00:00'::TIME, 2);
INSERT INTO day_of_week_time_range (name, day_of_week, start_time, end_time, schedule_rule_id) VALUES ('Thursday August 8:00 - 15:00', 'THURSDAY', '08:00:00'::TIME, '15:00:00'::TIME, 2);

INSERT INTO Employee (name, surname, email, schedule_id) VALUES ('Name', 'Surname', 'aferrer@nivel36.es', 1);

INSERT INTO Worksite (name, code, time_zone, deleted) VALUES ('Barcelona Headquarters', 'BCN-HQ', 'UTC+1', false);

INSERT INTO employee_worksite (employee_id, worksite_id) VALUES (1,1);
