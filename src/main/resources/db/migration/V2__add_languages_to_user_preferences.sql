ALTER TABLE user_preferences
    ADD COLUMN languages text[] NOT NULL DEFAULT '{}';
