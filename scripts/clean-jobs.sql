-- Clean test jobs, groups, and user-job-group relationships (keeps users + preferences)
-- Run: psql -h localhost -p 5440 -U jobhunter -d jobhunter -f scripts/clean-jobs.sql

BEGIN;

DELETE FROM user_job_groups;
DELETE FROM user_jobs;
DELETE FROM jobs;
DELETE FROM job_groups;

COMMIT;

SELECT 'job_groups' AS entity, count(*) FROM job_groups
UNION ALL SELECT 'jobs', count(*) FROM jobs
UNION ALL SELECT 'user_job_groups', count(*) FROM user_job_groups
UNION ALL SELECT 'user_jobs', count(*) FROM user_jobs;
