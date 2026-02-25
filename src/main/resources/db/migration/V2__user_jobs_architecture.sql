-- Remove status from jobs (status belongs to user-job relationship)
ALTER TABLE jobs DROP COLUMN status;
DROP INDEX IF EXISTS idx_jobs_status;

-- Fix description: should be NOT NULL (entity contract)
ALTER TABLE jobs ALTER COLUMN description SET NOT NULL;

-- Make company nullable (n8n may not extract company from some listings)
ALTER TABLE jobs ALTER COLUMN company DROP NOT NULL;

-- Add matched_at for job matching pipeline (@Scheduled picks up NULL)
ALTER TABLE jobs ADD COLUMN matched_at TIMESTAMPTZ;
CREATE INDEX idx_jobs_unmatched ON jobs (matched_at) WHERE matched_at IS NULL;

-- Users (Auth0 identity + Telegram integration)
CREATE TABLE users
(
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth0_sub        VARCHAR(255) UNIQUE NOT NULL,
    email            VARCHAR(255),
    name             VARCHAR(255),
    telegram_chat_id VARCHAR(100),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_auth0_sub ON users (auth0_sub);

-- Recreate user_preferences with user_id FK (no production data yet)
DROP TABLE user_preferences;

CREATE TABLE user_preferences
(
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID UNIQUE NOT NULL REFERENCES users (id),
    raw_input             TEXT,
    categories            TEXT[]  NOT NULL DEFAULT '{}',
    seniority_levels      TEXT[]  NOT NULL DEFAULT '{}',
    keywords              TEXT[]  NOT NULL DEFAULT '{}',
    excluded_keywords     TEXT[]  NOT NULL DEFAULT '{}',
    min_salary            INTEGER,
    remote_only           BOOLEAN NOT NULL DEFAULT FALSE,
    enabled_sources       TEXT[]  NOT NULL DEFAULT '{}',
    notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- User-job relationship (per-user status tracking)
CREATE TABLE user_jobs
(
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID        NOT NULL REFERENCES users (id),
    job_id             UUID        NOT NULL REFERENCES jobs (id),
    status             VARCHAR(50) NOT NULL DEFAULT 'NEW',
    ai_relevance_score INTEGER,
    ai_reasoning       TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, job_id)
);

CREATE INDEX idx_user_jobs_user_status ON user_jobs (user_id, status);
CREATE INDEX idx_user_jobs_job_id ON user_jobs (job_id);
