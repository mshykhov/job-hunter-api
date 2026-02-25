-- Clean test user, preferences, and their job relationships
-- Run: psql -h localhost -p 5440 -U jobhunter -d jobhunter -f scripts/clean-user.sql

BEGIN;

DELETE FROM user_jobs;
DELETE FROM user_preferences;
DELETE FROM users;

COMMIT;

SELECT 'users' AS entity, count(*) FROM users
UNION ALL SELECT 'user_preferences', count(*) FROM user_preferences
UNION ALL SELECT 'user_jobs', count(*) FROM user_jobs;
