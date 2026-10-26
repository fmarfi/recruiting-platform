-- Contract 1.2.0 (ADR 0011): candidate_exposure (append-only, not audited) and the off-limits override reason.
-- Owner: pipeline. The backfill is the next migration.
CREATE TABLE candidate_exposure (
    id             uuid PRIMARY KEY,
    person_id      uuid        NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    account_id     uuid        NOT NULL REFERENCES account (id),
    job_order_id   uuid        NOT NULL REFERENCES job_order (id),
    application_id uuid REFERENCES application (id) ON DELETE SET NULL,
    channel        varchar(40) NOT NULL CHECK (channel IN ('PIPELINE', 'REPORT', 'SHARE_LINK')),
    presented_at   timestamptz NOT NULL,
    presented_by   uuid REFERENCES app_user (id)
);
CREATE INDEX idx_exposure_person ON candidate_exposure (person_id, presented_at DESC);
CREATE INDEX idx_exposure_account ON candidate_exposure (account_id, person_id);
CREATE INDEX idx_exposure_job_order ON candidate_exposure (job_order_id);
CREATE INDEX idx_exposure_presented_by ON candidate_exposure (presented_by) WHERE presented_by IS NOT NULL;
-- First move into a client-facing stage only; makes the write idempotent.
CREATE UNIQUE INDEX uq_exposure_pipeline ON candidate_exposure (application_id, channel)
    WHERE channel = 'PIPELINE';

ALTER TABLE application ADD COLUMN off_limits_override_reason text;
ALTER TABLE application_aud ADD COLUMN off_limits_override_reason text;
