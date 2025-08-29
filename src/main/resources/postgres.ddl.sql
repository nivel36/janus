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

-- ====================================================================
-- Schema: Janus
-- Engine: PostgreSQL
-- Notes:
--  - DayOfWeek stored as VARCHAR(16) with CHECK constraint
--  - TimeRange embedded in DayOfWeekTimeRange: start_time / end_time
--  - ScheduleRule ↔ DayOfWeekTimeRange is 1→N (no join table)
--  - UNIQUE constraints inline; redundant indexes removed
-- ====================================================================

BEGIN;

-- ============================================================
-- schedule
-- ============================================================
CREATE TABLE schedule (
  id    BIGSERIAL PRIMARY KEY,
  name  VARCHAR(255) NOT NULL UNIQUE
);

-- ============================================================
-- schedule_rule
-- ============================================================
CREATE TABLE schedule_rule (
  id           BIGSERIAL PRIMARY KEY,
  name         VARCHAR(255) NOT NULL UNIQUE,
  schedule_id  BIGINT NOT NULL REFERENCES schedule(id)
                ON UPDATE CASCADE ON DELETE RESTRICT,
  start_date   DATE NULL,
  end_date     DATE NULL
);

-- Useful index for date-range lookups by schedule
CREATE INDEX idx_schedule_rule_sched_dates
  ON schedule_rule (schedule_id, start_date, end_date);

-- Prevent overlapping rules within the same schedule
-- (requires btree_gist; run with sufficient privileges)
CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE schedule_rule
  ADD CONSTRAINT ex_schedule_rule_no_overlap
  EXCLUDE USING gist (
    schedule_id WITH =,
    daterange(start_date, end_date, '[]') WITH &&
  );

-- ============================================================
-- day_of_week_time_range  (child of schedule_rule)
-- ============================================================
CREATE TABLE day_of_week_time_range (
  id               BIGSERIAL PRIMARY KEY,
  name             VARCHAR(128) NOT NULL UNIQUE,
  schedule_rule_id BIGINT NOT NULL REFERENCES schedule_rule(id)
                     ON UPDATE CASCADE ON DELETE CASCADE,
  day_of_week      VARCHAR(16)  NOT NULL,
  start_time       TIME NOT NULL,
  end_time         TIME NOT NULL,
  -- one shift starting per day-of-week within a given rule
  CONSTRAINT uk_dowtr_rule_day UNIQUE (schedule_rule_id, day_of_week),
  -- enum check (java.time.DayOfWeek uppercase)
  CONSTRAINT ck_dowtr_day_of_week CHECK (day_of_week IN
    ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
  -- allow daytime (start<end) and overnight (start>end), but not equal
  CONSTRAINT ck_dowtr_time_range_non_equal CHECK (start_time <> end_time)
);

-- The UNIQUE(schedule_rule_id, day_of_week) already creates an index usable
-- to filter by schedule_rule_id; no extra index is strictly necessary.

-- ============================================================
-- employee
-- ============================================================
CREATE TABLE employee (
  id           BIGSERIAL PRIMARY KEY,
  name         VARCHAR(255),
  surname      VARCHAR(255),
  email        VARCHAR(254) NOT NULL UNIQUE,
  schedule_id  BIGINT NOT NULL REFERENCES schedule(id)
                ON UPDATE CASCADE ON DELETE RESTRICT
);

-- Helpful for joins from employee to schedule
CREATE INDEX idx_employee_schedule ON employee (schedule_id);

COMMIT;
