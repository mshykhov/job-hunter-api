ALTER TABLE user_preferences
    DROP COLUMN weight_location;

ALTER TABLE user_preferences
    RENAME COLUMN weight_technology TO weight_keywords;

ALTER TABLE user_preferences
    RENAME COLUMN weight_skills TO weight_categories;

ALTER TABLE user_preferences
    ALTER COLUMN weight_keywords SET DEFAULT 45,
    ALTER COLUMN weight_seniority SET DEFAULT 30,
    ALTER COLUMN weight_categories SET DEFAULT 25;

UPDATE user_preferences
SET weight_keywords = 45,
    weight_seniority = 30,
    weight_categories = 25;
