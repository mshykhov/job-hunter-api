CREATE TABLE user_ai_providers
(
    id                uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id           uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    priority          integer     NOT NULL,
    provider          varchar(32) NOT NULL,
    api_key_encrypted text        NOT NULL DEFAULT '',
    model_id          varchar(100) NOT NULL,
    enabled           boolean     NOT NULL DEFAULT true,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_ai_providers_priority UNIQUE (user_id, priority),
    CONSTRAINT uq_user_ai_providers_provider UNIQUE (user_id, provider)
);

CREATE INDEX idx_user_ai_providers_user_id ON user_ai_providers (user_id);

INSERT INTO user_ai_providers (user_id, priority, provider, api_key_encrypted, model_id)
SELECT user_id, 1, 'OPENAI', api_key_encrypted, model_id
FROM user_ai_settings;
