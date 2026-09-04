CREATE TABLE automation_workflow_runs
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delegation_id   UUID         NOT NULL REFERENCES automation_delegations (id),
    run_type         VARCHAR(64)  NOT NULL,
    status           VARCHAR(32)  NOT NULL,
    idempotency_key UUID         NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    completed_at     TIMESTAMPTZ,
    CONSTRAINT uk_automation_workflow_run_idempotency UNIQUE (delegation_id, idempotency_key),
    CONSTRAINT ck_automation_workflow_run_type CHECK (run_type IN ('SYNTHETIC_RECOVERY')),
    CONSTRAINT ck_automation_workflow_run_status CHECK (status IN ('QUEUED', 'RUNNING', 'PAUSED', 'STOPPED', 'SUCCEEDED', 'FAILED'))
);

CREATE TABLE automation_work_items
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id            UUID         NOT NULL UNIQUE REFERENCES automation_workflow_runs (id),
    status            VARCHAR(32)  NOT NULL,
    lease_owner       VARCHAR(128),
    lease_token       UUID UNIQUE,
    lease_generation  BIGINT,
    lease_expires_at  TIMESTAMPTZ,
    attempt_count     INTEGER      NOT NULL DEFAULT 0,
    max_attempts      INTEGER      NOT NULL DEFAULT 3,
    next_step_index   INTEGER      NOT NULL DEFAULT 0,
    failure_code      VARCHAR(64),
    failure_detail    VARCHAR(512),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    completed_at      TIMESTAMPTZ,
    CONSTRAINT ck_automation_work_item_status CHECK (status IN ('QUEUED', 'LEASED', 'SUCCEEDED', 'CANCELLED', 'FAILED')),
    CONSTRAINT ck_automation_work_item_attempts CHECK (attempt_count >= 0 AND max_attempts BETWEEN 1 AND 10 AND attempt_count <= max_attempts),
    CONSTRAINT ck_automation_work_item_step CHECK (next_step_index BETWEEN 0 AND 3),
    CONSTRAINT ck_automation_work_item_lease CHECK (
        (status = 'LEASED' AND lease_owner IS NOT NULL AND lease_token IS NOT NULL AND lease_generation IS NOT NULL AND lease_expires_at IS NOT NULL)
        OR
        (status <> 'LEASED' AND lease_owner IS NULL AND lease_token IS NULL AND lease_generation IS NULL AND lease_expires_at IS NULL)
    )
);

CREATE TABLE automation_work_attempts
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    work_item_id      UUID         NOT NULL REFERENCES automation_work_items (id),
    attempt_number    INTEGER      NOT NULL,
    lease_token       UUID         NOT NULL UNIQUE,
    worker_id         VARCHAR(128) NOT NULL,
    runner_generation BIGINT       NOT NULL,
    started_at        TIMESTAMPTZ  NOT NULL,
    last_heartbeat_at TIMESTAMPTZ  NOT NULL,
    finished_at       TIMESTAMPTZ,
    outcome           VARCHAR(32)  NOT NULL,
    failure_code      VARCHAR(64),
    CONSTRAINT uk_automation_work_attempt_number UNIQUE (work_item_id, attempt_number),
    CONSTRAINT ck_automation_work_attempt_outcome CHECK (outcome IN ('ACTIVE', 'SUCCEEDED', 'PAUSED', 'STOPPED', 'STALE_GENERATION', 'LEASE_EXPIRED', 'FAILED'))
);

CREATE TABLE automation_workflow_checkpoints
(
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    work_item_id       UUID        NOT NULL REFERENCES automation_work_items (id),
    attempt_id         UUID        NOT NULL REFERENCES automation_work_attempts (id),
    step               VARCHAR(32) NOT NULL,
    step_index         INTEGER     NOT NULL,
    idempotency_key    UUID        NOT NULL,
    evidence_sha256    VARCHAR(64) NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_automation_workflow_checkpoint_step UNIQUE (work_item_id, step_index),
    CONSTRAINT uk_automation_workflow_checkpoint_idempotency UNIQUE (work_item_id, idempotency_key),
    CONSTRAINT ck_automation_workflow_checkpoint_step CHECK (step IN ('PREPARE', 'EXECUTE', 'VERIFY') AND step_index BETWEEN 0 AND 2),
    CONSTRAINT ck_automation_workflow_checkpoint_sha CHECK (evidence_sha256 ~ '^[0-9a-f]{64}$')
);

CREATE TABLE automation_workflow_events
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id       UUID         NOT NULL REFERENCES automation_workflow_runs (id),
    work_item_id UUID REFERENCES automation_work_items (id),
    event_type   VARCHAR(64)  NOT NULL,
    payload      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    occurred_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_automation_workflow_runs_delegation_created
    ON automation_workflow_runs (delegation_id, created_at DESC);
CREATE INDEX idx_automation_work_items_claim
    ON automation_work_items (status, created_at) WHERE status = 'QUEUED';
CREATE INDEX idx_automation_work_items_expiry
    ON automation_work_items (lease_expires_at) WHERE status = 'LEASED';
CREATE INDEX idx_automation_work_attempts_item_started
    ON automation_work_attempts (work_item_id, started_at DESC);
CREATE INDEX idx_automation_workflow_events_run_time
    ON automation_workflow_events (run_id, occurred_at, id);
