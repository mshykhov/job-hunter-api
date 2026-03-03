CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_jobs_title_trgm ON jobs USING gin (lower(title) gin_trgm_ops);
CREATE INDEX idx_jobs_company_trgm ON jobs USING gin (lower(company) gin_trgm_ops);
CREATE INDEX idx_user_jobs_user_created ON user_jobs (user_id, created_at DESC);
