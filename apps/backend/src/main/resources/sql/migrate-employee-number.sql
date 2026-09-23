-- Rollout migration for installations created before EMPLOYEE_NUMBER existed.
-- Review/replace the generated values with the authoritative HR employee numbers
-- before executing the final NOT NULL statement.
ALTER TABLE EMPLOYEE ADD COLUMN EMPLOYEE_NUMBER VARCHAR(50);

UPDATE EMPLOYEE
SET EMPLOYEE_NUMBER = 'LEGACY-' || LPAD(ID::text, 10, '0')
WHERE EMPLOYEE_NUMBER IS NULL;

-- Stop here to reconcile the generated LEGACY values with the HR system.
ALTER TABLE EMPLOYEE ADD CONSTRAINT UK_EMPLOYEE_NUMBER UNIQUE (EMPLOYEE_NUMBER);
ALTER TABLE EMPLOYEE ALTER COLUMN EMPLOYEE_NUMBER SET NOT NULL;
