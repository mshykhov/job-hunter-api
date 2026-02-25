-- Clean test jobs and user-job relationships (keeps users + preferences)
-- Run: psql -h localhost -p 5440 -U jobhunter -d jobhunter -f scripts/clean-test-data.sql

BEGIN;

DELETE FROM user_jobs;
DELETE FROM jobs;

COMMIT;

SELECT 'jobs' AS entity, count(*) FROM jobs
UNION ALL SELECT 'user_jobs', count(*) FROM user_jobs;
