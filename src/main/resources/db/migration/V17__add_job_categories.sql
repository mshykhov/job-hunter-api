ALTER TABLE jobs ADD COLUMN categories TEXT[] NOT NULL DEFAULT '{}';

UPDATE jobs SET categories = ARRAY['java', 'kotlin'];
