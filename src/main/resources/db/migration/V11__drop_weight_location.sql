ALTER TABLE user_preferences
    DROP COLUMN weight_location;

ALTER TABLE user_preferences
    ALTER COLUMN weight_technology SET DEFAULT 45,
    ALTER COLUMN weight_seniority SET DEFAULT 30,
    ALTER COLUMN weight_skills    SET DEFAULT 25;
