CREATE TABLE jobs
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(500)  NOT NULL,
    company     VARCHAR(300)  NOT NULL,
    url         VARCHAR(2048) NOT NULL UNIQUE,
    description TEXT,
    source      VARCHAR(50)   NOT NULL,
    salary      VARCHAR(200),
    location    VARCHAR(300),
    remote       BOOLEAN       NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,
    status       VARCHAR(50)   NOT NULL DEFAULT 'NEW',
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_jobs_status ON jobs (status);
CREATE INDEX idx_jobs_source ON jobs (source);
CREATE INDEX idx_jobs_created_at ON jobs (created_at DESC);

CREATE TABLE user_preferences
(
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_sub               VARCHAR(255) UNIQUE NOT NULL,
    notifications_enabled  BOOLEAN     NOT NULL DEFAULT TRUE,
    categories             TEXT[]      NOT NULL DEFAULT '{}',
    excluded_keywords      TEXT[]      NOT NULL DEFAULT '{}',
    remote_only            BOOLEAN     NOT NULL DEFAULT FALSE,
    enabled_sources        TEXT[]      NOT NULL DEFAULT '{}',
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_preferences_user_sub ON user_preferences (user_sub);
