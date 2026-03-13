-- Setup test user + preferences for local development
-- Run: psql -h localhost -p 5440 -U jobhunter -d jobhunter -f scripts/setup-user.sql

BEGIN;

INSERT INTO users (id, auth0_sub, email, name)
VALUES ('a0000000-0000-0000-0000-000000000001', 'local-dev-user', 'dev@jobhunter.local', 'Dev User')
ON CONFLICT (auth0_sub) DO NOTHING;

INSERT INTO user_preferences (id, user_id, about,
                              categories, locations, remote_only, disabled_sources,
                              excluded_keywords, excluded_title_keywords,
                              excluded_companies, match_with_ai)
VALUES ('b0000000-0000-0000-0000-000000000001',
        'a0000000-0000-0000-0000-000000000001',
        'Senior Kotlin/Java backend developer, remote',
        ARRAY ['kotlin', 'java'],
        ARRAY ['Ukraine', 'United Kingdom', 'United States', 'Europe', 'Middle East'],
        true,
        ARRAY[]::text[],
        ARRAY ['php', 'wordpress', 'drupal'],
        ARRAY[]::text[],
        ARRAY[]::text[],
        true)
ON CONFLICT (user_id) DO NOTHING;

COMMIT;

SELECT 'users' AS entity, count(*) FROM users
UNION ALL SELECT 'user_preferences', count(*) FROM user_preferences;
