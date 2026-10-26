-- Contract 1.2.0 (ADR 0011): cv_intake_item, a work queue (not audited). Owner: accounts.
CREATE TABLE cv_intake_item (
    id                 uuid PRIMARY KEY,
    batch_id           uuid        NOT NULL,
    status             varchar(40) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'ATTACHED', 'DISCARDED')),
    file_name          varchar(255),
    content_type       varchar(100),
    size_bytes         bigint
        CONSTRAINT ck_cv_intake_size CHECK (size_bytes IS NULL OR size_bytes BETWEEN 1 AND 10485760),
    sha256             char(64),
    storage_key        varchar(300),
    text_status        varchar(40) NOT NULL DEFAULT 'PENDING'
        CHECK (text_status IN ('PENDING', 'DONE', 'NO_TEXT', 'FAILED')),
    extracted_text     text,
    detected           jsonb       NOT NULL DEFAULT '{"emails":[],"phones":[],"linkedinUrls":[]}',
    source_channel     varchar(40)
        CHECK (source_channel IN
            ('LINKEDIN_AD', 'LINKEDIN_DIRECT', 'WEBSITE', 'REFERRAL', 'EXISTING_POOL', 'EMAIL', 'CV_UPLOAD')),
    attached_person_id uuid REFERENCES person (id) ON DELETE CASCADE,
    uploaded_by        uuid        NOT NULL REFERENCES app_user (id),
    uploaded_at        timestamptz NOT NULL,
    expires_at         timestamptz NOT NULL
);
CREATE INDEX idx_cv_intake_batch ON cv_intake_item (batch_id);
CREATE INDEX idx_cv_intake_status ON cv_intake_item (status, uploaded_at);
CREATE INDEX idx_cv_intake_attached_person ON cv_intake_item (attached_person_id)
    WHERE attached_person_id IS NOT NULL;
CREATE INDEX idx_cv_intake_uploaded_by ON cv_intake_item (uploaded_by);
CREATE INDEX idx_cv_intake_expires ON cv_intake_item (expires_at);
-- CvIntakeApi.findPendingByEmail (DSR): containment on detected, e.g. detected @> '{"emails":["a@b.c"]}'.
CREATE INDEX idx_cv_intake_detected ON cv_intake_item USING gin (detected);
