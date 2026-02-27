-- Copyright 2026 Abel Ferrer Jiménez
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
INSERT INTO app_user (username, locale, time_format) VALUES ('aferrer@nivel36.es', 'es_ES', 'H24');

INSERT INTO schedule (code, name) VALUES ('STD-WH-AUG-VAR', 'Standard Work Hours with August Variation');

INSERT INTO schedule_rule (schedule_id, start_date, end_date, name) VALUES (1, '2024-01-01', '2024-07-31', 'Regular Work Hours');
INSERT INTO schedule_rule (schedule_id, start_date, end_date, name) VALUES (1, '2024-08-01', '2024-08-31', 'August Work Hours');
INSERT INTO schedule_rule (schedule_id, start_date, end_date, name) VALUES (1, '2024-09-01', '2024-12-31', 'Post August Regular Work Hours');

INSERT INTO day_of_week_time_range (schedule_rule_id, start_time, end_time, day_of_week, effective_work_hours) VALUES (1, '08:00:00'::TIME, '17:30:00'::TIME, 'MONDAY', 'PT8H30M');
INSERT INTO day_of_week_time_range (schedule_rule_id, start_time, end_time, day_of_week, effective_work_hours) VALUES (1, '08:00:00'::TIME, '17:30:00'::TIME, 'TUESDAY', 'PT8H30M');
INSERT INTO day_of_week_time_range (schedule_rule_id, start_time, end_time, day_of_week, effective_work_hours) VALUES (1, '08:00:00'::TIME, '17:30:00'::TIME, 'WEDNESDAY', 'PT8H30M');
INSERT INTO day_of_week_time_range (schedule_rule_id, start_time, end_time, day_of_week, effective_work_hours) VALUES (1, '08:00:00'::TIME, '17:30:00'::TIME, 'THURSDAY', 'PT8H30M');
INSERT INTO day_of_week_time_range (schedule_rule_id, start_time, end_time, day_of_week, effective_work_hours) VALUES (1, '08:00:00'::TIME, '15:00:00'::TIME, 'FRIDAY', 'PT7H');
INSERT INTO day_of_week_time_range (schedule_rule_id, start_time, end_time, day_of_week, effective_work_hours) VALUES (2, '08:00:00'::TIME, '15:00:00'::TIME, 'MONDAY', 'PT7H');
INSERT INTO day_of_week_time_range (schedule_rule_id, start_time, end_time, day_of_week, effective_work_hours) VALUES (2, '08:00:00'::TIME, '15:00:00'::TIME, 'TUESDAY', 'PT7H');
INSERT INTO day_of_week_time_range (schedule_rule_id, start_time, end_time, day_of_week, effective_work_hours) VALUES (2, '08:00:00'::TIME, '15:00:00'::TIME, 'WEDNESDAY', 'PT7H');
INSERT INTO day_of_week_time_range (schedule_rule_id, start_time, end_time, day_of_week, effective_work_hours) VALUES (2, '08:00:00'::TIME, '15:00:00'::TIME, 'THURSDAY', 'PT7H');
INSERT INTO day_of_week_time_range (schedule_rule_id, start_time, end_time, day_of_week, effective_work_hours) VALUES (2, '08:00:00'::TIME, '15:00:00'::TIME, 'FRIDAY', 'PT7H');
INSERT INTO day_of_week_time_range (schedule_rule_id, start_time, end_time, day_of_week, effective_work_hours) VALUES (3, '08:00:00'::TIME, '17:30:00'::TIME, 'MONDAY', 'PT8H30M');
INSERT INTO day_of_week_time_range (schedule_rule_id, start_time, end_time, day_of_week, effective_work_hours) VALUES (3, '08:00:00'::TIME, '17:30:00'::TIME, 'TUESDAY', 'PT8H30M');
INSERT INTO day_of_week_time_range (schedule_rule_id, start_time, end_time, day_of_week, effective_work_hours) VALUES (3, '08:00:00'::TIME, '17:30:00'::TIME, 'WEDNESDAY', 'PT8H30M');
INSERT INTO day_of_week_time_range (schedule_rule_id, start_time, end_time, day_of_week, effective_work_hours) VALUES (3, '08:00:00'::TIME, '17:30:00'::TIME, 'THURSDAY', 'PT8H30M');
INSERT INTO day_of_week_time_range (schedule_rule_id, start_time, end_time, day_of_week, effective_work_hours) VALUES (3, '08:00:00'::TIME, '15:00:00'::TIME, 'FRIDAY', 'PT7H');

INSERT INTO employee (name, surname, email, schedule_id) VALUES ('Abel', 'Ferrer Jiménez', 'aferrer@nivel36.es', 1);

INSERT INTO worksite (name, code, time_zone) VALUES ('Barcelona Headquarters', 'BCN-HQ', 'UTC+1');

INSERT INTO employee_worksite (employee_id, worksite_id) VALUES (1,1);

INSERT INTO time_log (employee_id,worksite_id,workshift_id,entry_time,exit_time, work_duration) VALUES (1,1,NULL,'2024-04-20T07:32:10Z'::timestamp,'2024-04-20T16:15:20Z'::timestamp, 31390);
INSERT INTO time_log (employee_id,worksite_id,workshift_id,entry_time,exit_time, work_duration) VALUES (1,1,NULL,'2024-04-21T07:29:20Z'::timestamp,'2024-04-21T16:31:23Z'::timestamp, 32523);
INSERT INTO time_log (employee_id,worksite_id,workshift_id,entry_time,exit_time, work_duration) VALUES (1,1,NULL,'2024-04-22T07:34:45Z'::timestamp,'2024-04-22T16:11:31Z'::timestamp, 31006);
INSERT INTO time_log (employee_id,worksite_id,workshift_id,entry_time,exit_time, work_duration) VALUES (1,1,NULL,'2024-04-23T07:33:51Z'::timestamp,'2024-04-23T16:11:42Z'::timestamp, 31071);
INSERT INTO time_log (employee_id,worksite_id,workshift_id,entry_time) VALUES (1,1,NULL,'2024-04-24T07:30:12Z'::timestamp);

