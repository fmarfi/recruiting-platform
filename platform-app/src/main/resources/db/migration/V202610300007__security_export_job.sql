-- Contract 1.3.0 (CO-013, ADR 0013): export_job, the export log itself. Not audited. Owner: security.
CREATE TABLE export_job (
    id                   uuid PRIMARY KEY,
    entity               varchar(40)    NOT NULL CHECK (entity IN ('PEOPLE', 'ACCOUNTS', 'JOB_ORDERS', 'PROPOSALS')),
    status               varchar(40)    NOT NULL
        CHECK (status IN ('PENDING_APPROVAL', 'READY', 'REJECTED', 'EXPIRED', 'FAILED')),
    filters              jsonb          NOT NULL
        CONSTRAINT ck_export_job_filters_no_text CHECK (NOT (filters ?| array['q', 'cv'])),
    filter_keys          varchar(50)[]  NOT NULL,
    columns              varchar(100)[] NOT NULL,
    sort                 varchar(100)[] NOT NULL DEFAULT '{}',
    purpose              varchar(300)   NOT NULL,
    include_compensation boolean        NOT NULL DEFAULT false,
    row_count            int            NOT NULL,
    requested_by         uuid           NOT NULL REFERENCES app_user (id),
    requested_at         timestamptz    NOT NULL,
    decided_by           uuid REFERENCES app_user (id),
    decided_at           timestamptz,
    decision_reason      varchar(500),
    storage_key          varchar(300),
    ready_at             timestamptz,
    expires_at           timestamptz
);
CREATE INDEX idx_export_job_status ON export_job (status, requested_at);
CREATE INDEX idx_export_job_requested_by ON export_job (requested_by, requested_at DESC);
CREATE INDEX idx_export_job_decided_by ON export_job (decided_by) WHERE decided_by IS NOT NULL;
