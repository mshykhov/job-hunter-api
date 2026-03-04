ALTER TABLE user_preferences DROP COLUMN IF EXISTS languages;

ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS excluded_title_keywords TEXT[] NOT NULL DEFAULT '{}';
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS excluded_companies TEXT[] NOT NULL DEFAULT '{}';
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS match_with_ai BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS custom_prompt TEXT;
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS notification_sources TEXT[] NOT NULL DEFAULT '{}';
