CREATE TABLE user_ai_settings
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID         NOT NULL UNIQUE REFERENCES users (id),
    api_key_encrypted TEXT         NOT NULL,
    model_id          VARCHAR(100) NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_ai_settings_user_id ON user_ai_settings (user_id);
