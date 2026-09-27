-- Apply once to existing installations before deploying theme preferences.
-- Existing users receive the same default as newly provisioned users.
ALTER TABLE APP_USER
  ADD COLUMN THEME VARCHAR(16) DEFAULT 'DARK' NOT NULL
  CHECK (THEME IN ('DARK', 'LIGHT'));
