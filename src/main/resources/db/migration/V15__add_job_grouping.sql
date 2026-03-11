-- Job grouping: group duplicate jobs by normalized title + company

-- 1. Create job_groups table
CREATE TABLE job_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_key VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    company VARCHAR(255),
    job_count INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 2. Populate job_groups from existing jobs
INSERT INTO job_groups (id, group_key, title, company, job_count, created_at, updated_at)
SELECT gen_random_uuid(),
       md5(regexp_replace(lower(trim(title)), '\s+', ' ', 'g') || '::' || coalesce(regexp_replace(lower(trim(company)), '\s+', ' ', 'g'), '')),
       min(title),
       min(company),
       count(*),
       min(created_at),
       now()
FROM jobs
GROUP BY md5(regexp_replace(lower(trim(title)), '\s+', ' ', 'g') || '::' || coalesce(regexp_replace(lower(trim(company)), '\s+', ' ', 'g'), ''));

-- 3. Add group_id FK to jobs
ALTER TABLE jobs ADD COLUMN group_id UUID;

UPDATE jobs SET group_id = jg.id
FROM job_groups jg
WHERE jg.group_key = md5(regexp_replace(lower(trim(jobs.title)), '\s+', ' ', 'g') || '::' || coalesce(regexp_replace(lower(trim(jobs.company)), '\s+', ' ', 'g'), ''));

ALTER TABLE jobs ALTER COLUMN group_id SET NOT NULL;
ALTER TABLE jobs ADD CONSTRAINT fk_jobs_group FOREIGN KEY (group_id) REFERENCES job_groups(id);
CREATE INDEX idx_jobs_group_id ON jobs (group_id);

-- 4. Create user_job_groups table (group-level AI evaluation + status)
CREATE TABLE user_job_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    group_id UUID NOT NULL REFERENCES job_groups(id),
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    ai_relevance_score INTEGER NOT NULL DEFAULT 0,
    ai_reasoning TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id, group_id)
);

CREATE INDEX idx_user_job_groups_user_status ON user_job_groups (user_id, status);
CREATE INDEX idx_user_job_groups_group_id ON user_job_groups (group_id);

-- 5. Migrate AI data from user_jobs to user_job_groups (representative per group = highest score)
INSERT INTO user_job_groups (id, user_id, group_id, status, ai_relevance_score, ai_reasoning, created_at, updated_at)
SELECT DISTINCT ON (uj.user_id, j.group_id)
    gen_random_uuid(),
    uj.user_id,
    j.group_id,
    uj.status,
    uj.ai_relevance_score,
    uj.ai_reasoning,
    uj.created_at,
    uj.updated_at
FROM user_jobs uj
JOIN jobs j ON uj.job_id = j.id
ORDER BY uj.user_id, j.group_id, uj.ai_relevance_score DESC;

-- 6. Drop AI columns and status from user_jobs (now on user_job_groups)
ALTER TABLE user_jobs DROP COLUMN ai_relevance_score;
ALTER TABLE user_jobs DROP COLUMN ai_reasoning;
ALTER TABLE user_jobs DROP COLUMN ai_inferred_remote;
ALTER TABLE user_jobs DROP COLUMN status;
