-- Contract 1.5.0 (ADR 0015): enum CHECKs re-created for the new values (C15-DB4) and the Home personal-number
-- indexes (C15-DB5).

-- CloseReason: MOVED_TO_OTHER_SEARCH
ALTER TABLE application DROP CONSTRAINT ck_application_close_reason_enum;
ALTER TABLE application ADD CONSTRAINT ck_application_close_reason_enum CHECK (close_reason IN
    ('CLIENT_DECLINED', 'CANDIDATE_WITHDREW', 'NOT_INTERESTED', 'NOT_QUALIFIED', 'COMPENSATION_MISMATCH',
     'OFF_LIMITS', 'NO_RESPONSE', 'HIRED_ELSEWHERE', 'SEARCH_CLOSED', 'MOVED_TO_OTHER_SEARCH', 'OTHER'));

-- ExposureChannel: CV_PUSH
ALTER TABLE candidate_exposure DROP CONSTRAINT candidate_exposure_channel_check;
ALTER TABLE candidate_exposure ADD CONSTRAINT candidate_exposure_channel_check
    CHECK (channel IN ('PIPELINE', 'REPORT', 'SHARE_LINK', 'CV_PUSH'));

-- TimelineKind: CANDIDATE_MOVED
ALTER TABLE timeline_entry DROP CONSTRAINT timeline_entry_kind_check;
ALTER TABLE timeline_entry ADD CONSTRAINT timeline_entry_kind_check CHECK (kind IN
    ('ACCOUNT', 'ACCOUNT_STATUS', 'CONTACT', 'ACTIVITY', 'PROPOSAL', 'JOB_ORDER', 'CANDIDATE', 'ARCHIVE', 'MERGE',
     'PLACEMENT', 'CANDIDATE_MOVED'));

-- ListEntity: APPLICATIONS (views and exports reject it in the service)
ALTER TABLE saved_view DROP CONSTRAINT saved_view_entity_check;
ALTER TABLE saved_view ADD CONSTRAINT saved_view_entity_check
    CHECK (entity IN ('PEOPLE', 'ACCOUNTS', 'JOB_ORDERS', 'PROPOSALS', 'APPLICATIONS'));
ALTER TABLE bulk_action DROP CONSTRAINT bulk_action_entity_check;
ALTER TABLE bulk_action ADD CONSTRAINT bulk_action_entity_check
    CHECK (entity IN ('PEOPLE', 'ACCOUNTS', 'JOB_ORDERS', 'PROPOSALS', 'APPLICATIONS'));
ALTER TABLE export_job DROP CONSTRAINT export_job_entity_check;
ALTER TABLE export_job ADD CONSTRAINT export_job_entity_check
    CHECK (entity IN ('PEOPLE', 'ACCOUNTS', 'JOB_ORDERS', 'PROPOSALS', 'APPLICATIONS'));

-- BulkResultCode: DUPLICATE, OFF_LIMITS, CONSENT_REQUIRED, ARCHIVED
ALTER TABLE bulk_action_item DROP CONSTRAINT bulk_action_item_code_check;
ALTER TABLE bulk_action_item ADD CONSTRAINT bulk_action_item_code_check CHECK (code IN
    ('NO_CHANGE', 'NOT_FOUND', 'FORBIDDEN', 'ILLEGAL_TRANSITION', 'TRANSITION_GUARD_FAILED', 'REASON_REQUIRED',
     'STATE_CONFLICT', 'VERSION_CONFLICT', 'NO_EMAIL', 'CONSENT_GIVEN', 'CONSENT_REFUSED', 'CONSENT_WITHDRAWN',
     'REQUEST_PENDING', 'ANONYMISED', 'NO_CONSENT_BASIS', 'TOO_MANY_TAGS', 'DUPLICATE', 'OFF_LIMITS',
     'CONSENT_REQUIRED', 'ARCHIVED', 'INTERNAL_ERROR'));

-- Home personal numbers (C15-DB5)
CREATE INDEX idx_interview_note_interviewer_held ON interview_note (interviewer_id, held_on);
CREATE INDEX idx_evaluation_evaluator_on ON candidate_evaluation (evaluator_id, evaluated_on);
CREATE INDEX idx_timeline_screened_actor ON timeline_entry (actor_id, occurred_at)
    WHERE kind = 'CANDIDATE' AND to_status = 'SCREENED';
CREATE INDEX idx_exposure_presented_by_at ON candidate_exposure (presented_by, presented_at);
