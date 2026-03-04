-- Move telegram columns from users to user_preferences
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS telegram_chat_id VARCHAR(100);
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS telegram_username VARCHAR(100);

-- Copy existing data
UPDATE user_preferences up
SET telegram_chat_id = u.telegram_chat_id,
    telegram_username = u.telegram_username
FROM users u
WHERE up.user_id = u.id;

-- Drop from users
ALTER TABLE users DROP COLUMN IF EXISTS telegram_chat_id;
ALTER TABLE users DROP COLUMN IF EXISTS telegram_username;
