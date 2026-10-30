-- Contract 1.3.0: archive and confidentiality on job_order, ranked longlist/shortlist and close reason on
-- application, e-mail/call fields and attendees on activity. Owner: pipeline.

ALTER TABLE job_order
    ADD COLUMN archived_at           timestamptz,
    ADD COLUMN archived_by           uuid REFERENCES app_user (id),
    ADD COLUMN archive_reason        varchar(40)
        CONSTRAINT ck_job_order_archive_reason CHECK (archive_reason IN
            ('DUPLICATE', 'CREATED_BY_MISTAKE', 'NO_LONGER_RELEVANT', 'MERGED', 'IMPORT_ROLLBACK', 'OTHER')),
    ADD COLUMN archive_note          varchar(500),
    ADD COLUMN confidentiality_level varchar(40) NOT NULL DEFAULT 'NORMAL'
        CONSTRAINT ck_job_order_confidentiality CHECK (confidentiality_level IN ('NORMAL', 'RESTRICTED', 'CONFIDENTIAL')),
    ADD CONSTRAINT ck_job_order_archive_pair CHECK ((archived_at IS NULL) = (archive_reason IS NULL)),
    ADD CONSTRAINT ck_job_order_archive_by CHECK ((archived_at IS NULL) = (archived_by IS NULL));
CREATE INDEX idx_job_order_archived ON job_order (archived_at) WHERE archived_at IS NOT NULL;
CREATE INDEX idx_job_order_archived_by ON job_order (archived_by) WHERE archived_by IS NOT NULL;
ALTER TABLE job_order_aud
    ADD COLUMN archived_at           timestamptz,
    ADD COLUMN archived_by           uuid,
    ADD COLUMN archive_reason        varchar(40),
    ADD COLUMN archive_note          varchar(500),
    ADD COLUMN confidentiality_level varchar(40);

-- application: rank, rating, rationale, outreach status, close reason (R65)
ALTER TABLE application
    ADD COLUMN rank            int
        CONSTRAINT ck_application_rank CHECK (rank >= 1),
    ADD COLUMN rating          smallint
        CONSTRAINT ck_application_rating CHECK (rating BETWEEN 1 AND 5),
    ADD COLUMN rationale       text,
    ADD COLUMN outreach_status varchar(40) NOT NULL DEFAULT 'TO_CALL'
        CONSTRAINT ck_application_outreach_status CHECK (outreach_status IN
            ('TO_CALL', 'CALLED', 'INTERESTED', 'NOT_INTERESTED', 'NOT_NOW', 'SCREENED')),
    ADD COLUMN close_reason    varchar(40)
        CONSTRAINT ck_application_close_reason_enum CHECK (close_reason IN
            ('CLIENT_DECLINED', 'CANDIDATE_WITHDREW', 'NOT_INTERESTED', 'NOT_QUALIFIED', 'COMPENSATION_MISMATCH',
             'OFF_LIMITS', 'NO_RESPONSE', 'HIRED_ELSEWHERE', 'SEARCH_CLOSED', 'OTHER'));

-- Rank backfill: per job order and list, number the active applications by stage_changed_at, created_at.
-- LONGLIST = SOURCED, SCREENED; SHORTLIST = PRESENTED, CLIENT_INTERVIEW, OFFER. REJECTED and PLACED stay null.
UPDATE application a
SET rank = r.n
FROM (SELECT id,
             row_number() OVER (
                 PARTITION BY job_order_id, (stage IN ('PRESENTED', 'CLIENT_INTERVIEW', 'OFFER'))
                 ORDER BY stage_changed_at, created_at, id) AS n
      FROM application
      WHERE stage NOT IN ('REJECTED', 'PLACED')) r
WHERE a.id = r.id;
UPDATE application SET outreach_status = 'SCREENED' WHERE stage <> 'SOURCED';
-- Close reason backfill before the CHECK that ties it to REJECTED.
UPDATE application SET close_reason = 'OTHER' WHERE stage = 'REJECTED';
ALTER TABLE application
    ADD CONSTRAINT ck_application_close_reason CHECK ((stage = 'REJECTED') = (close_reason IS NOT NULL));
CREATE INDEX idx_application_job_rank ON application (job_order_id, rank);
CREATE INDEX idx_application_job_close_reason ON application (job_order_id, close_reason);

ALTER TABLE application_aud
    ADD COLUMN rank            int,
    ADD COLUMN rating          smallint,
    ADD COLUMN rationale       text,
    ADD COLUMN outreach_status varchar(40),
    ADD COLUMN close_reason    varchar(40);

-- activity: outcome, direction, duration, source, captured e-mail id (R70)
ALTER TABLE activity
    ADD COLUMN outcome             varchar(40)
        CONSTRAINT ck_activity_outcome CHECK (outcome IN
            ('CONNECTED', 'NO_ANSWER', 'LEFT_MESSAGE', 'CALLBACK_REQUESTED', 'INTERESTED', 'NOT_INTERESTED',
             'MEETING_BOOKED', 'WRONG_CONTACT', 'COMPLETED')),
    ADD COLUMN direction           varchar(40)
        CONSTRAINT ck_activity_direction CHECK (direction IN ('INBOUND', 'OUTBOUND')),
    ADD COLUMN duration_min        smallint
        CONSTRAINT ck_activity_duration CHECK (duration_min BETWEEN 0 AND 1440),
    ADD COLUMN source              varchar(40) NOT NULL DEFAULT 'MANUAL'
        CONSTRAINT ck_activity_source CHECK (source IN ('MANUAL', 'EMAIL', 'CALL_SHEET')),
    ADD COLUMN external_message_id varchar(300),
    ADD CONSTRAINT ck_activity_note_direction CHECK (type <> 'NOTE' OR direction IS NULL),
    ADD CONSTRAINT ck_activity_message_source CHECK (external_message_id IS NULL OR source = 'EMAIL');
CREATE UNIQUE INDEX uq_activity_external_message_id ON activity (external_message_id)
    WHERE external_message_id IS NOT NULL;

ALTER TABLE activity_aud
    ADD COLUMN outcome             varchar(40),
    ADD COLUMN direction           varchar(40),
    ADD COLUMN duration_min        smallint,
    ADD COLUMN source              varchar(40),
    ADD COLUMN external_message_id varchar(300);

-- Other people present at a call or meeting. Both FKs cascade (the row is the attendee's data).
CREATE TABLE activity_attendee (
    activity_id uuid NOT NULL REFERENCES activity (id) ON DELETE CASCADE,
    person_id   uuid NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    PRIMARY KEY (activity_id, person_id)
);
CREATE INDEX idx_activity_attendee_person ON activity_attendee (person_id);

CREATE TABLE activity_attendee_aud (
    activity_id uuid   NOT NULL,
    person_id   uuid   NOT NULL,
    rev         bigint NOT NULL REFERENCES revinfo (rev),
    revtype     smallint,
    revend      bigint REFERENCES revinfo (rev),
    PRIMARY KEY (activity_id, person_id, rev)
);
CREATE INDEX idx_activity_attendee_aud_person ON activity_attendee_aud (person_id);
CREATE INDEX idx_activity_attendee_aud_rev ON activity_attendee_aud (rev);
CREATE INDEX idx_activity_attendee_aud_revend ON activity_attendee_aud (revend);
