ALTER TABLE user_preferences
    ADD COLUMN weight_technology INT NOT NULL DEFAULT 40,
    ADD COLUMN weight_seniority INT NOT NULL DEFAULT 25,
    ADD COLUMN weight_skills    INT NOT NULL DEFAULT 20,
    ADD COLUMN weight_location  INT NOT NULL DEFAULT 15;
