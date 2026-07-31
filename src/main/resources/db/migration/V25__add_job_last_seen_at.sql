ALTER TABLE jobs ADD COLUMN last_seen_at timestamptz NOT NULL DEFAULT now();

UPDATE jobs SET last_seen_at = updated_at;

CREATE INDEX idx_jobs_last_seen_at ON jobs (last_seen_at);
