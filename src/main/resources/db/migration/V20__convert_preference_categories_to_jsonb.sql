ALTER TABLE user_preferences ALTER COLUMN categories DROP DEFAULT;

ALTER TABLE user_preferences
    ALTER COLUMN categories SET DATA TYPE JSONB
        USING COALESCE(array_to_json(categories)::jsonb, '[]'::jsonb);

ALTER TABLE user_preferences ALTER COLUMN categories SET DEFAULT '[]';
