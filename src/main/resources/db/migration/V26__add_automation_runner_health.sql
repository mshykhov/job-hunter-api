CREATE TABLE automation_delegations
(
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                  UUID         NOT NULL REFERENCES users (id),
    owner_issuer             VARCHAR(512) NOT NULL,
    owner_subject            VARCHAR(512) NOT NULL,
    runner_issuer            VARCHAR(512) NOT NULL,
    health_reporting_enabled BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    revoked_at               TIMESTAMPTZ,
    CONSTRAINT uk_automation_delegation_owner UNIQUE (owner_issuer, owner_subject)
);

CREATE TABLE automation_runners
(
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delegation_id             UUID        NOT NULL UNIQUE REFERENCES automation_delegations (id),
    runner_key                VARCHAR(64) NOT NULL UNIQUE,
    generation                BIGINT      NOT NULL DEFAULT 0,
    sequence                  BIGINT      NOT NULL DEFAULT 0,
    last_idempotency_key      UUID,
    launcher_version          VARCHAR(64),
    overall_state             VARCHAR(32) NOT NULL DEFAULT 'UNAVAILABLE',
    overall_reason            VARCHAR(64) NOT NULL DEFAULT 'INVALID_REPORT',
    components                JSONB       NOT NULL DEFAULT '{}'::jsonb,
    probes                    JSONB       NOT NULL DEFAULT '{}'::jsonb,
    last_heartbeat_at         TIMESTAMPTZ,
    last_preflight_success_at TIMESTAMPTZ,
    last_codex_success_at     TIMESTAMPTZ,
    codex_input_tokens        BIGINT      NOT NULL DEFAULT 0,
    codex_output_tokens       BIGINT      NOT NULL DEFAULT 0,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE automation_runner_transitions
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    runner_id   UUID        NOT NULL REFERENCES automation_runners (id),
    component   VARCHAR(32) NOT NULL,
    from_state  VARCHAR(32) NOT NULL,
    to_state    VARCHAR(32) NOT NULL,
    reason      VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    generation  BIGINT      NOT NULL,
    sequence    BIGINT      NOT NULL
);

CREATE INDEX idx_automation_runner_transitions_runner_time
    ON automation_runner_transitions (runner_id, occurred_at DESC);
