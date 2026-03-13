ALTER TABLE jobs ALTER COLUMN categories DROP DEFAULT;

ALTER TABLE jobs
    ALTER COLUMN categories SET DATA TYPE JSONB
        USING array_to_json(categories)::jsonb;

ALTER TABLE jobs ALTER COLUMN categories SET DEFAULT '["java", "kotlin"]';
