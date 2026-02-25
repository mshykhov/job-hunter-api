-- Setup test jobs + user-job relationships (requires user from setup-user.sql)
-- Run: psql -h localhost -p 5440 -U jobhunter -d jobhunter -f scripts/setup-test-data.sql

BEGIN;

INSERT INTO jobs (id, title, company, url, description, source, salary, location, remote, published_at, matched_at)
VALUES
    ('c0000000-0000-0000-0000-000000000001',
     'Senior Kotlin Backend Developer',
     'TechCorp',
     'https://dou.ua/jobs/1001',
     'We are looking for a Senior Kotlin Backend Developer to join our team. You will work with Spring Boot, PostgreSQL, and Kubernetes. Requirements: 5+ years of experience with JVM languages, strong knowledge of Kotlin and Spring ecosystem, experience with microservices architecture.',
     'DOU', '$6000-8000', 'Remote', true,
     now() - interval '3 days', now() - interval '2 days'),

    ('c0000000-0000-0000-0000-000000000002',
     'Lead Java/Kotlin Engineer',
     'FinTech Solutions',
     'https://djinni.co/jobs/2001',
     'FinTech Solutions is hiring a Lead Engineer to architect and build our core banking platform. Tech stack: Kotlin, Spring Boot 3, PostgreSQL, Kafka, gRPC. You will lead a team of 5 developers and drive technical decisions.',
     'DJINNI', '$8000-10000', 'Remote', true,
     now() - interval '5 days', now() - interval '4 days'),

    ('c0000000-0000-0000-0000-000000000003',
     'Senior Backend Engineer (Java)',
     'CloudScale',
     'https://dou.ua/jobs/1002',
     'Join CloudScale as a Senior Backend Engineer. We build cloud-native applications using Java 21, Spring Boot, and AWS. Experience with distributed systems and event-driven architecture is a plus. Competitive salary and fully remote position.',
     'DOU', '$5500-7500', 'Remote', true,
     now() - interval '2 days', now() - interval '1 day'),

    ('c0000000-0000-0000-0000-000000000004',
     'Kotlin Developer',
     'StartupXYZ',
     'https://djinni.co/jobs/2002',
     'StartupXYZ is building an AI-powered recruitment platform. We need a Kotlin Developer with Spring Boot experience. Bonus: experience with OpenAI API, vector databases, and Kubernetes.',
     'DJINNI', '$4500-6000', 'Remote', true,
     now() - interval '7 days', now() - interval '6 days'),

    ('c0000000-0000-0000-0000-000000000005',
     'Senior Software Engineer - Platform',
     'DataFlow Inc',
     'https://dou.ua/jobs/1003',
     'DataFlow Inc is looking for a Senior Software Engineer to work on our data platform. Technologies: Kotlin, Spring, PostgreSQL, Apache Kafka, Flink. You will design and implement real-time data processing pipelines.',
     'DOU', '$7000-9000', 'Kyiv, Ukraine (Remote OK)', true,
     now() - interval '1 day', now() - interval '12 hours'),

    ('c0000000-0000-0000-0000-000000000006',
     'Full Stack Developer (React + Node)',
     'WebAgency',
     'https://dou.ua/jobs/1004',
     'Looking for a Full Stack Developer with React and Node.js experience. Must know TypeScript, Next.js, and MongoDB. PHP knowledge is a plus.',
     'DOU', '$3000-4500', 'Kyiv, Ukraine', false,
     now() - interval '4 days', now() - interval '3 days'),

    ('c0000000-0000-0000-0000-000000000007',
     'Junior PHP Developer',
     'WebShop',
     'https://djinni.co/jobs/2003',
     'We need a Junior PHP Developer for our WordPress-based e-commerce platform. Experience with WooCommerce and MySQL is required.',
     'DJINNI', '$800-1200', 'Lviv, Ukraine', false,
     now() - interval '6 days', now() - interval '5 days'),

    ('c0000000-0000-0000-0000-000000000008',
     'Senior Kotlin Backend Developer',
     'NeoBank',
     'https://dou.ua/jobs/1005',
     'NeoBank is hiring Senior Kotlin Backend Developers. We use Kotlin, Spring Boot, PostgreSQL, and deploy to Kubernetes. Fully remote, great benefits.',
     'DOU', '$6500-8500', 'Remote', true,
     now() - interval '6 hours', null),

    ('c0000000-0000-0000-0000-000000000009',
     'DevOps Engineer',
     'InfraCloud',
     'https://indeed.com/jobs/3001',
     'InfraCloud needs a DevOps Engineer with Kubernetes, Terraform, and AWS experience. Python scripting skills required.',
     'INDEED', '$5000-7000', 'Remote', true,
     now() - interval '3 hours', null)

ON CONFLICT (url) DO NOTHING;

INSERT INTO user_jobs (id, user_id, job_id, status, ai_relevance_score, ai_reasoning)
VALUES
    ('d0000000-0000-0000-0000-000000000001',
     'a0000000-0000-0000-0000-000000000001',
     'c0000000-0000-0000-0000-000000000001',
     'NEW', 92, 'Strong match: Senior Kotlin + Spring Boot + PostgreSQL, remote'),

    ('d0000000-0000-0000-0000-000000000002',
     'a0000000-0000-0000-0000-000000000001',
     'c0000000-0000-0000-0000-000000000003',
     'NEW', 78, 'Good match: Senior Java backend, Spring Boot, remote. Kotlin not primary but JVM ecosystem fits'),

    ('d0000000-0000-0000-0000-000000000003',
     'a0000000-0000-0000-0000-000000000001',
     'c0000000-0000-0000-0000-000000000005',
     'NEW', 88, 'Strong match: Kotlin + Spring + PostgreSQL + Kafka, senior level, remote OK'),

    ('d0000000-0000-0000-0000-000000000004',
     'a0000000-0000-0000-0000-000000000001',
     'c0000000-0000-0000-0000-000000000002',
     'APPLIED', 95, 'Excellent match: Lead Kotlin + Spring, fintech, remote'),

    ('d0000000-0000-0000-0000-000000000005',
     'a0000000-0000-0000-0000-000000000001',
     'c0000000-0000-0000-0000-000000000004',
     'APPLIED', 72, 'Decent match: Kotlin + Spring Boot, startup'),

    ('d0000000-0000-0000-0000-000000000006',
     'a0000000-0000-0000-0000-000000000001',
     'c0000000-0000-0000-0000-000000000006',
     'IRRELEVANT', 25, 'Poor match: Full Stack React/Node, not backend-focused, not remote, PHP mentioned')

ON CONFLICT (user_id, job_id) DO NOTHING;

COMMIT;

SELECT 'jobs' AS entity, count(*) FROM jobs
UNION ALL SELECT 'user_jobs', count(*) FROM user_jobs;
