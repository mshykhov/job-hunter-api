CREATE TABLE user_preferences
(
    id                     BIGSERIAL PRIMARY KEY,
    user_sub               VARCHAR(255) UNIQUE NOT NULL,
    notifications_enabled  BOOLEAN     NOT NULL DEFAULT TRUE,
    languages              TEXT[]      NOT NULL DEFAULT '{}',
    excluded_keywords      TEXT[]      NOT NULL DEFAULT '{}',
    remote_only            BOOLEAN     NOT NULL DEFAULT FALSE,
    enabled_sources        TEXT[]      NOT NULL DEFAULT '{}',
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_preferences_user_sub ON user_preferences (user_sub);
