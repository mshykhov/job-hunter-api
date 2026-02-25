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

-- Jobs
CREATE TABLE jobs
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title        VARCHAR(500)  NOT NULL,
    company      VARCHAR(300),
    url          VARCHAR(2048) NOT NULL UNIQUE,
    description  TEXT          NOT NULL,
    source       VARCHAR(50)   NOT NULL,
    salary       VARCHAR(200),
    location     VARCHAR(300),
    remote       BOOLEAN       NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,
    matched_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_jobs_source ON jobs (source);
CREATE INDEX idx_jobs_created_at ON jobs (created_at DESC);
CREATE INDEX idx_jobs_unmatched ON jobs (matched_at) WHERE matched_at IS NULL;

-- User preferences
CREATE TABLE user_preferences
(
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID UNIQUE NOT NULL REFERENCES users (id),
    raw_input             TEXT,
    categories            TEXT[]  NOT NULL DEFAULT '{}',
    seniority_levels      TEXT[]  NOT NULL DEFAULT '{}',
    keywords              TEXT[]  NOT NULL DEFAULT '{}',
    excluded_keywords     TEXT[]  NOT NULL DEFAULT '{}',
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
