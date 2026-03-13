ALTER TABLE job_groups ADD COLUMN categories JSONB NOT NULL DEFAULT '["java", "kotlin"]';

UPDATE job_groups jg
SET categories = COALESCE(
    (SELECT jsonb_agg(DISTINCT elem)
     FROM jobs j, jsonb_array_elements(j.categories) AS elem
     WHERE j.group_id = jg.id),
    '["java", "kotlin"]'
);

ALTER TABLE jobs DROP COLUMN categories;
