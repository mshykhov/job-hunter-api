ALTER TABLE jobs ADD COLUMN match_attempts integer NOT NULL DEFAULT 0;

CREATE INDEX idx_jobs_unmatched_created_at ON jobs (created_at) WHERE matched_at IS NULL;
