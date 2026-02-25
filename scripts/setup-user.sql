-- Setup test user + preferences
-- Run: psql -h localhost -p 5440 -U jobhunter -d jobhunter -f scripts/setup-user.sql

BEGIN;

INSERT INTO users (id, auth0_sub, email, name)
VALUES ('a0000000-0000-0000-0000-000000000001', 'local-dev-user', 'dev@jobhunter.local', 'Dev User')
ON CONFLICT (auth0_sub) DO NOTHING;

INSERT INTO user_preferences (id, user_id, raw_input, categories, seniority_levels, keywords,
                              excluded_keywords, remote_only, disabled_sources,
                              min_score, notifications_enabled)
VALUES ('b0000000-0000-0000-0000-000000000001',
        'a0000000-0000-0000-0000-000000000001',
        'Senior Kotlin/Java backend developer, remote',
        ARRAY ['kotlin', 'java'],
        ARRAY ['senior', 'lead'],
        ARRAY ['spring', 'microservices', 'postgresql', 'kubernetes'],
        ARRAY ['php', 'wordpress', 'drupal'],
        true,
        ARRAY[]::text[],
        50,
        true)
ON CONFLICT (user_id) DO NOTHING;

COMMIT;

SELECT 'users' AS entity, count(*) FROM users
UNION ALL SELECT 'user_preferences', count(*) FROM user_preferences;
