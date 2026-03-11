-- Setup test jobs with grouping + user-job-group relationships (requires setup-user.sql)
-- One group has jobs from 3 sources: LinkedIn, DOU, Djinni (tests grouping)
-- Run: psql -h localhost -p 5440 -U jobhunter -d jobhunter -f scripts/setup-test-data.sql

BEGIN;

-- Group 1: "Senior Kotlin Developer" at "TechCorp" — 4 jobs from 3 sources (same group)
INSERT INTO job_groups (id, group_key, title, company, job_count)
VALUES ('e0000000-0000-0000-0000-000000000001',
        md5('senior kotlin developer::techcorp'),
        'Senior Kotlin Developer', 'TechCorp', 4)
ON CONFLICT (group_key) DO NOTHING;

INSERT INTO jobs (id, group_id, title, company, url, description, source, raw_data, salary, location, remote, published_at, matched_at)
VALUES
    ('c0000000-0000-0000-0000-000000000001',
     'e0000000-0000-0000-0000-000000000001',
     'Senior Kotlin Developer',
     'TechCorp',
     'https://linkedin.com/jobs/kotlin-techcorp',
     'TechCorp is hiring a Senior Kotlin Developer. Spring Boot, PostgreSQL, Kubernetes. Fully remote. 5+ years JVM experience required.',
     'LINKEDIN', '{}'::jsonb, '$7000-9000', 'Remote', true,
     now() - interval '5 days', now() - interval '3 days'),

    ('c0000000-0000-0000-0000-000000000002',
     'e0000000-0000-0000-0000-000000000001',
     'Senior Kotlin Developer',
     'TechCorp',
     'https://dou.ua/jobs/kotlin-techcorp',
     'TechCorp шукає Senior Kotlin Developer. Spring Boot, PostgreSQL, Kubernetes. Remote.',
     'DOU', '{}'::jsonb, '$6500-8500', 'Remote', true,
     now() - interval '4 days', now() - interval '3 days'),

    ('c0000000-0000-0000-0000-000000000003',
     'e0000000-0000-0000-0000-000000000001',
     'Senior Kotlin Developer',
     'TechCorp',
     'https://djinni.co/jobs/kotlin-techcorp-1',
     'Senior Kotlin Developer at TechCorp. Stack: Kotlin, Spring Boot 3, PostgreSQL 16, K8s.',
     'DJINNI', '{}'::jsonb, '$7000-8500', 'Kyiv (Remote OK)', null,
     now() - interval '3 days', now() - interval '2 days'),

    ('c0000000-0000-0000-0000-000000000004',
     'e0000000-0000-0000-0000-000000000001',
     'Senior Kotlin Developer',
     'TechCorp',
     'https://djinni.co/jobs/kotlin-techcorp-2',
     'TechCorp needs Senior Kotlin Developer. Microservices, event-driven architecture. Remote-first.',
     'DJINNI', '{}'::jsonb, null, 'Berlin (Remote OK)', true,
     now() - interval '2 days', now() - interval '1 day')
ON CONFLICT (url) DO NOTHING;

-- Group 2: "Lead Java/Kotlin Engineer" at "FinTech Solutions" — single job
INSERT INTO job_groups (id, group_key, title, company, job_count)
VALUES ('e0000000-0000-0000-0000-000000000002',
        md5('lead java/kotlin engineer::fintech solutions'),
        'Lead Java/Kotlin Engineer', 'FinTech Solutions', 1)
ON CONFLICT (group_key) DO NOTHING;

INSERT INTO jobs (id, group_id, title, company, url, description, source, raw_data, salary, location, remote, published_at, matched_at)
VALUES
    ('c0000000-0000-0000-0000-000000000005',
     'e0000000-0000-0000-0000-000000000002',
     'Lead Java/Kotlin Engineer',
     'FinTech Solutions',
     'https://djinni.co/jobs/lead-fintech',
     'FinTech Solutions is hiring a Lead Engineer. Kotlin, Spring Boot 3, PostgreSQL, Kafka, gRPC. Lead team of 5.',
     'DJINNI', '{}'::jsonb, '$8000-10000', 'Remote', true,
     now() - interval '5 days', now() - interval '4 days')
ON CONFLICT (url) DO NOTHING;

-- Group 3: "Full Stack Developer (React + Node)" at "WebAgency" — irrelevant job
INSERT INTO job_groups (id, group_key, title, company, job_count)
VALUES ('e0000000-0000-0000-0000-000000000003',
        md5('full stack developer (react + node)::webagency'),
        'Full Stack Developer (React + Node)', 'WebAgency', 1)
ON CONFLICT (group_key) DO NOTHING;

INSERT INTO jobs (id, group_id, title, company, url, description, source, raw_data, salary, location, remote, published_at, matched_at)
VALUES
    ('c0000000-0000-0000-0000-000000000006',
     'e0000000-0000-0000-0000-000000000003',
     'Full Stack Developer (React + Node)',
     'WebAgency',
     'https://dou.ua/jobs/fullstack-webagency',
     'Looking for a Full Stack Developer with React and Node.js. Must know TypeScript, Next.js, MongoDB. PHP knowledge is a plus.',
     'DOU', '{}'::jsonb, '$3000-4500', 'Kyiv, Ukraine', false,
     now() - interval '4 days', now() - interval '3 days')
ON CONFLICT (url) DO NOTHING;

-- Group 4: Unmatched job (no matched_at — will be picked up by matching scheduler)
INSERT INTO job_groups (id, group_key, title, company, job_count)
VALUES ('e0000000-0000-0000-0000-000000000004',
        md5('senior kotlin backend developer::neobank'),
        'Senior Kotlin Backend Developer', 'NeoBank', 1)
ON CONFLICT (group_key) DO NOTHING;

INSERT INTO jobs (id, group_id, title, company, url, description, source, raw_data, salary, location, remote, published_at, matched_at)
VALUES
    ('c0000000-0000-0000-0000-000000000007',
     'e0000000-0000-0000-0000-000000000004',
     'Senior Kotlin Backend Developer',
     'NeoBank',
     'https://dou.ua/jobs/kotlin-neobank',
     'NeoBank is hiring Senior Kotlin Backend Developers. Kotlin, Spring Boot, PostgreSQL, deploy to Kubernetes. Fully remote.',
     'DOU', '{}'::jsonb, '$6500-8500', 'Remote', true,
     now() - interval '6 hours', null)
ON CONFLICT (url) DO NOTHING;

-- User-job-group relationships (for groups 1-3, group 4 is unmatched)
INSERT INTO user_job_groups (id, user_id, group_id, status, ai_relevance_score, ai_reasoning)
VALUES
    ('d0000000-0000-0000-0000-000000000001',
     'a0000000-0000-0000-0000-000000000001',
     'e0000000-0000-0000-0000-000000000001',
     'NEW', 92,
     'Strong match: Senior Kotlin + Spring Boot + PostgreSQL, remote, 4 postings from 3 sources'),

    ('d0000000-0000-0000-0000-000000000002',
     'a0000000-0000-0000-0000-000000000001',
     'e0000000-0000-0000-0000-000000000002',
     'APPLIED', 95,
     'Excellent match: Lead Kotlin + Spring, fintech, remote, leadership role'),

    ('d0000000-0000-0000-0000-000000000003',
     'a0000000-0000-0000-0000-000000000001',
     'e0000000-0000-0000-0000-000000000003',
     'IRRELEVANT', 25,
     'Poor match: Full Stack React/Node, not backend-focused, not remote, PHP mentioned')
ON CONFLICT (user_id, group_id) DO NOTHING;

COMMIT;

SELECT 'job_groups' AS entity, count(*) FROM job_groups
UNION ALL SELECT 'jobs', count(*) FROM jobs
UNION ALL SELECT 'user_job_groups', count(*) FROM user_job_groups;
