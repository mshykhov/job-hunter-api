-- Clean test user, preferences, and all their relationships
-- Run: psql -h localhost -p 5440 -U jobhunter -d jobhunter -f scripts/clean-user.sql

BEGIN;

DELETE FROM user_job_groups;
DELETE FROM user_jobs;
DELETE FROM user_preferences;
DELETE FROM users;
DELETE FROM jobs;
DELETE FROM job_groups;

COMMIT;

SELECT 'users' AS entity, count(*) FROM users
UNION ALL SELECT 'user_preferences', count(*) FROM user_preferences
UNION ALL SELECT 'job_groups', count(*) FROM job_groups
UNION ALL SELECT 'jobs', count(*) FROM jobs
UNION ALL SELECT 'user_job_groups', count(*) FROM user_job_groups
UNION ALL SELECT 'user_jobs', count(*) FROM user_jobs;
