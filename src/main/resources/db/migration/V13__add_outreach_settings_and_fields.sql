ALTER TABLE user_jobs ADD COLUMN cover_letter TEXT;
ALTER TABLE user_jobs ADD COLUMN recruiter_message TEXT;

CREATE TABLE outreach_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    cover_letter_prompt TEXT,
    recruiter_message_prompt TEXT,
    source_config JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
