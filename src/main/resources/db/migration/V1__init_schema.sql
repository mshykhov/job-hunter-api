CREATE TABLE jobs
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(500)  NOT NULL,
    company     VARCHAR(300)  NOT NULL,
    url         VARCHAR(2048) NOT NULL UNIQUE,
    description TEXT,
    source      VARCHAR(50)   NOT NULL,
    salary      VARCHAR(200),
    location    VARCHAR(300),
    remote      BOOLEAN       NOT NULL DEFAULT FALSE,
    status      VARCHAR(50)   NOT NULL DEFAULT 'NEW',
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_jobs_status ON jobs (status);
CREATE INDEX idx_jobs_source ON jobs (source);
CREATE INDEX idx_jobs_created_at ON jobs (created_at DESC);
