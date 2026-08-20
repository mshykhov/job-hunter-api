CREATE TABLE user_job_group_decisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES job_groups(id),
    vacancy_seen_at TIMESTAMPTZ NOT NULL,
    decided_at TIMESTAMPTZ NOT NULL,
    outcome VARCHAR(32) NOT NULL CHECK (outcome IN ('COLD_REJECTED', 'AI_REJECTED_REMOTE', 'AI_SCORED', 'COLD_ONLY', 'LEGACY_REJECTED_UNKNOWN')),
    cold_filter VARCHAR(100),
    ai_score INTEGER CHECK (ai_score BETWEEN 0 AND 100),
    inferred_remote BOOLEAN,
    sources VARCHAR[] NOT NULL DEFAULT '{}',
    categories VARCHAR[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, group_id)
);

CREATE INDEX idx_user_job_group_decisions_user_seen ON user_job_group_decisions (user_id, vacancy_seen_at);
CREATE INDEX idx_user_job_group_decisions_sources ON user_job_group_decisions USING GIN (sources);

INSERT INTO user_job_group_decisions (
    user_id, group_id, vacancy_seen_at, decided_at, outcome, ai_score, inferred_remote, sources, categories
)
SELECT
    ujg.user_id,
    ujg.group_id,
    jg.created_at,
    COALESCE(ujg.updated_at, ujg.created_at, jg.created_at),
    'AI_SCORED',
    ujg.ai_relevance_score,
    NULL,
    COALESCE(src.sources, '{}'),
    COALESCE(cat.categories, '{}')
FROM user_job_groups ujg
JOIN job_groups jg ON jg.id = ujg.group_id
LEFT JOIN LATERAL (
    SELECT array_agg(DISTINCT j.source::text ORDER BY j.source::text) AS sources FROM jobs j WHERE j.group_id = jg.id
) src ON TRUE
LEFT JOIN LATERAL (
    SELECT array_agg(DISTINCT value ORDER BY value) AS categories FROM jsonb_array_elements_text(jg.categories) value
) cat ON TRUE;

INSERT INTO user_job_group_decisions (
    user_id, group_id, vacancy_seen_at, decided_at, outcome, sources, categories
)
SELECT
    u.id,
    jg.id,
    jg.created_at,
    jg.created_at,
    'LEGACY_REJECTED_UNKNOWN',
    COALESCE(src.sources, '{}'),
    COALESCE(cat.categories, '{}')
FROM users u
CROSS JOIN user_preferences up
CROSS JOIN job_groups jg
LEFT JOIN user_job_groups ujg ON ujg.user_id = u.id AND ujg.group_id = jg.id
LEFT JOIN LATERAL (
    SELECT array_agg(DISTINCT j.source::text ORDER BY j.source::text) AS sources FROM jobs j WHERE j.group_id = jg.id
) src ON TRUE
LEFT JOIN LATERAL (
    SELECT array_agg(DISTINCT value ORDER BY value) AS categories FROM jsonb_array_elements_text(jg.categories) value
) cat ON TRUE
WHERE up.user_id = u.id
  AND ujg.id IS NULL
  AND EXISTS (SELECT 1 FROM jobs j WHERE j.group_id = jg.id AND j.matched_at IS NOT NULL);
