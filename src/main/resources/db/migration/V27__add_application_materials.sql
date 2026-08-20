CREATE TABLE application_material_artifacts
(
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID         NOT NULL REFERENCES users (id),
    kind                 VARCHAR(32)  NOT NULL,
    media_type           VARCHAR(128) NOT NULL,
    encrypted_content    BYTEA        NOT NULL,
    plaintext_sha256     VARCHAR(64)  NOT NULL,
    extraction_sha256    VARCHAR(64),
    byte_size            BIGINT       NOT NULL CHECK (byte_size >= 0),
    renderer_fingerprint VARCHAR(128),
    retention_state      VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_material_artifact_content UNIQUE (user_id, kind, plaintext_sha256)
);

CREATE TABLE fact_catalog_versions
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID        NOT NULL REFERENCES users (id),
    schema_version    VARCHAR(64) NOT NULL,
    content_sha256    VARCHAR(64) NOT NULL,
    encrypted_content BYTEA       NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_fact_catalog_version UNIQUE (user_id, content_sha256)
);

CREATE TABLE candidate_profile_versions
(
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID        NOT NULL REFERENCES users (id),
    schema_version        VARCHAR(64) NOT NULL,
    profile_version       VARCHAR(64) NOT NULL,
    content_sha256        VARCHAR(64) NOT NULL,
    encrypted_content     BYTEA       NOT NULL,
    fact_catalog_version_id UUID      NOT NULL REFERENCES fact_catalog_versions (id),
    base_docx_artifact_id UUID        NOT NULL REFERENCES application_material_artifacts (id),
    base_pdf_artifact_id  UUID        NOT NULL REFERENCES application_material_artifacts (id),
    validation_metadata   JSONB       NOT NULL DEFAULT '{}'::jsonb,
    source_commit         VARCHAR(64) NOT NULL,
    active                BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_candidate_profile_version UNIQUE (user_id, profile_version)
);

CREATE UNIQUE INDEX uk_candidate_profile_active
    ON candidate_profile_versions (user_id)
    WHERE active;

CREATE TABLE writing_style_versions
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID        NOT NULL REFERENCES users (id),
    content_sha256    VARCHAR(64) NOT NULL,
    encrypted_content BYTEA       NOT NULL,
    active            BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_writing_style_version UNIQUE (user_id, content_sha256)
);

CREATE UNIQUE INDEX uk_writing_style_active
    ON writing_style_versions (user_id)
    WHERE active;

CREATE TABLE job_description_versions
(
    id                           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                      UUID        NOT NULL REFERENCES users (id),
    job_id                       UUID        NOT NULL REFERENCES jobs (id),
    content_sha256               VARCHAR(64) NOT NULL,
    encrypted_raw_content        BYTEA       NOT NULL,
    encrypted_normalized_content BYTEA       NOT NULL,
    parser_version               VARCHAR(64) NOT NULL,
    captured_at                  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_job_description_version UNIQUE (user_id, job_id, content_sha256)
);

CREATE TABLE application_material_packages
(
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID        NOT NULL REFERENCES users (id),
    job_id               UUID        NOT NULL REFERENCES jobs (id),
    selected_revision_id UUID,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_application_material_package UNIQUE (user_id, job_id)
);

CREATE TABLE application_material_requests
(
    id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id                 UUID         NOT NULL REFERENCES application_material_packages (id),
    job_description_version_id UUID         NOT NULL REFERENCES job_description_versions (id),
    profile_version_id         UUID         NOT NULL REFERENCES candidate_profile_versions (id),
    fact_catalog_version_id    UUID         NOT NULL REFERENCES fact_catalog_versions (id),
    writing_style_version_id   UUID         NOT NULL REFERENCES writing_style_versions (id),
    status                     VARCHAR(32)  NOT NULL,
    request_mode               VARCHAR(32)  NOT NULL,
    requested_kinds            JSONB        NOT NULL,
    cover_letter_policy        VARCHAR(32)  NOT NULL,
    encrypted_owner_edits      BYTEA,
    generation_policy_version  VARCHAR(64)  NOT NULL,
    schema_version             VARCHAR(64)  NOT NULL,
    renderer_version           VARCHAR(128) NOT NULL,
    model_route                VARCHAR(32)  NOT NULL,
    input_sha256               VARCHAR(64)  NOT NULL,
    lease_owner                VARCHAR(128),
    lease_token                UUID,
    lease_expires_at           TIMESTAMPTZ,
    attempt_count              INTEGER      NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    idempotency_key            VARCHAR(128) NOT NULL,
    parent_revision_id         UUID,
    created_at                 TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_material_request_idempotency UNIQUE (package_id, idempotency_key)
);

CREATE INDEX idx_material_request_claim
    ON application_material_requests (status, lease_expires_at, created_at);

CREATE TABLE application_material_revisions
(
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id           UUID         NOT NULL REFERENCES application_material_packages (id),
    request_id           UUID         NOT NULL REFERENCES application_material_requests (id),
    revision_number      INTEGER      NOT NULL CHECK (revision_number > 0),
    parent_revision_id   UUID         REFERENCES application_material_revisions (id),
    origin               VARCHAR(32)  NOT NULL,
    input_sha256         VARCHAR(64)  NOT NULL,
    generator_model      VARCHAR(64),
    renderer_version     VARCHAR(128) NOT NULL,
    eligibility_state    VARCHAR(32)  NOT NULL,
    manifest             JSONB        NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_material_package_revision UNIQUE (package_id, revision_number),
    CONSTRAINT uk_material_request_revision UNIQUE (request_id)
);

ALTER TABLE application_material_packages
    ADD CONSTRAINT fk_material_package_selected_revision
        FOREIGN KEY (selected_revision_id) REFERENCES application_material_revisions (id);

ALTER TABLE application_material_requests
    ADD CONSTRAINT fk_material_request_parent_revision
        FOREIGN KEY (parent_revision_id) REFERENCES application_material_revisions (id);

CREATE TABLE application_material_revision_artifacts
(
    revision_id UUID        NOT NULL REFERENCES application_material_revisions (id) ON DELETE CASCADE,
    artifact_id UUID        NOT NULL REFERENCES application_material_artifacts (id),
    kind        VARCHAR(32) NOT NULL,
    PRIMARY KEY (revision_id, kind),
    CONSTRAINT uk_material_revision_artifact UNIQUE (revision_id, artifact_id)
);

CREATE TABLE material_claim_usages
(
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    revision_id              UUID         NOT NULL REFERENCES application_material_revisions (id) ON DELETE CASCADE,
    artifact_kind            VARCHAR(32)  NOT NULL,
    json_path                VARCHAR(512) NOT NULL,
    fact_id                  VARCHAR(128) NOT NULL,
    variant_id               VARCHAR(128),
    profile_version          VARCHAR(64)  NOT NULL,
    fact_catalog_version     VARCHAR(64)  NOT NULL,
    claim_strength           VARCHAR(32)  NOT NULL,
    validator_outcome        VARCHAR(32)  NOT NULL
);

CREATE TABLE material_generation_attempts
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id          UUID         NOT NULL REFERENCES application_material_requests (id) ON DELETE CASCADE,
    lease_token         UUID         NOT NULL,
    route               VARCHAR(32)  NOT NULL,
    model                VARCHAR(64),
    cli_version          VARCHAR(64),
    schema_version       VARCHAR(64)  NOT NULL,
    skill_version        VARCHAR(64)  NOT NULL,
    renderer_version     VARCHAR(128) NOT NULL,
    input_sha256         VARCHAR(64)  NOT NULL,
    repair_of_attempt_id UUID         REFERENCES material_generation_attempts (id),
    started_at           TIMESTAMPTZ  NOT NULL,
    finished_at          TIMESTAMPTZ,
    usage_metadata       JSONB        NOT NULL DEFAULT '{}'::jsonb,
    outcome              VARCHAR(64)
);

CREATE TABLE material_validation_results
(
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    revision_id          UUID REFERENCES application_material_revisions (id) ON DELETE CASCADE,
    attempt_id           UUID REFERENCES material_generation_attempts (id) ON DELETE CASCADE,
    validator_version    VARCHAR(64)  NOT NULL,
    renderer_fingerprint VARCHAR(128),
    findings             JSONB        NOT NULL,
    reason_counts        JSONB        NOT NULL,
    eligibility_decision VARCHAR(32)  NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_material_validation_owner CHECK (revision_id IS NOT NULL OR attempt_id IS NOT NULL)
);

CREATE TABLE legacy_outreach_imports
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID        NOT NULL REFERENCES users (id),
    job_id            UUID        NOT NULL REFERENCES jobs (id),
    user_job_id       UUID        NOT NULL REFERENCES user_jobs (id),
    kind              VARCHAR(32) NOT NULL,
    encrypted_content BYTEA       NOT NULL,
    origin            VARCHAR(32) NOT NULL DEFAULT 'LEGACY_IMPORTED',
    imported_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_legacy_outreach_import UNIQUE (user_job_id, kind),
    CONSTRAINT ck_legacy_outreach_origin CHECK (origin = 'LEGACY_IMPORTED')
);

CREATE TABLE application_data_migrations
(
    name           VARCHAR(128) PRIMARY KEY,
    source_count   BIGINT      NOT NULL CHECK (source_count >= 0),
    imported_count BIGINT      NOT NULL CHECK (imported_count >= 0),
    completed_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_application_data_migration_counts CHECK (source_count = imported_count)
);
