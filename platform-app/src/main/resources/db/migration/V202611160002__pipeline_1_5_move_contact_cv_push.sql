-- Contract 1.5.0 (ADR 0015): job_order.contact_id, application move link, activity.cv_push, candidate_exposure.activity_id.
-- Owner: pipeline tables, written by db-engineer. Audit tables get the new columns of the audited tables.

-- C15-DB1: the client contact a search is for. Indirect person FK (ADR 0009): the job order stays, the link clears.
ALTER TABLE job_order ADD COLUMN contact_id uuid REFERENCES contact (id) ON DELETE SET NULL;
CREATE INDEX idx_job_order_contact ON job_order (contact_id) WHERE contact_id IS NOT NULL;
ALTER TABLE job_order_aud ADD COLUMN contact_id uuid;

-- C15-DB2: move between searches. Set on the target; on delete set null may clear the link alone.
ALTER TABLE application
    ADD COLUMN moved_from_application_id uuid REFERENCES application (id) ON DELETE SET NULL,
    ADD COLUMN moved_from_stage          varchar(40)
        CONSTRAINT ck_application_moved_from_stage_enum CHECK (moved_from_stage IN
            ('SOURCED', 'SCREENED', 'PRESENTED', 'CLIENT_INTERVIEW', 'OFFER', 'PLACED', 'REJECTED')),
    ADD COLUMN moved_at                  timestamptz,
    ADD COLUMN moved_by                  uuid REFERENCES app_user (id),
    ADD CONSTRAINT ck_application_moved_not_self CHECK (moved_from_application_id <> id),
    ADD CONSTRAINT ck_application_moved CHECK ((moved_at IS NULL) = (moved_from_stage IS NULL)
        AND (moved_from_application_id IS NULL OR moved_at IS NOT NULL));
CREATE UNIQUE INDEX uq_application_moved_from ON application (moved_from_application_id)
    WHERE moved_from_application_id IS NOT NULL;
CREATE INDEX idx_application_moved_by ON application (moved_by) WHERE moved_by IS NOT NULL;
ALTER TABLE application_aud
    ADD COLUMN moved_from_application_id uuid,
    ADD COLUMN moved_from_stage          varchar(40),
    ADD COLUMN moved_at                  timestamptz,
    ADD COLUMN moved_by                  uuid;

-- C15-DB3: CV push = activity with cv_push, plus its exposure row (no job order).
ALTER TABLE activity ADD COLUMN cv_push boolean NOT NULL DEFAULT false;
ALTER TABLE activity ADD CONSTRAINT ck_activity_cv_push CHECK (NOT cv_push OR
    (person_id IS NOT NULL AND account_id IS NOT NULL AND job_order_id IS NULL AND type <> 'NOTE'));
ALTER TABLE activity_aud ADD COLUMN cv_push boolean;

-- Existing exposure rows are never CV_PUSH, so both new CHECKs hold for them. The channel CHECK itself learns
-- CV_PUSH in the next migration.
ALTER TABLE candidate_exposure
    ADD COLUMN activity_id uuid REFERENCES activity (id) ON DELETE CASCADE,
    ALTER COLUMN job_order_id DROP NOT NULL,
    ADD CONSTRAINT ck_exposure_cv_push_activity CHECK ((channel = 'CV_PUSH') = (activity_id IS NOT NULL)),
    ADD CONSTRAINT ck_exposure_cv_push_job_order CHECK ((channel = 'CV_PUSH') = (job_order_id IS NULL));
CREATE UNIQUE INDEX uq_exposure_activity ON candidate_exposure (activity_id) WHERE activity_id IS NOT NULL;
