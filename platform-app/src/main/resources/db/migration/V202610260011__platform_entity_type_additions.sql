-- Contract 1.2.0: EntityType gained PERSON_FILE, CV_INTAKE_ITEM and OFF_LIMITS_RULE (file download and intake
-- access are logged, off-limits rule changes show on the timeline). Widen the two CHECKs that hold EntityType.
ALTER TABLE access_log DROP CONSTRAINT access_log_entity_type_check;
ALTER TABLE access_log ADD CONSTRAINT access_log_entity_type_check CHECK (entity_type IN (
    'ACCOUNT', 'PERSON', 'CONTACT', 'NETWORK_LINK', 'JOB_ORDER', 'APPLICATION', 'PROPOSAL', 'ACTIVITY', 'NEXT_ACTION',
    'USER', 'CONSENT_RECORD', 'CONSENT_REQUEST', 'PRIVACY_NOTICE', 'DATA_SUBJECT_REQUEST', 'IMPORT_JOB',
    'PERSON_FILE', 'CV_INTAKE_ITEM', 'OFF_LIMITS_RULE'));

ALTER TABLE timeline_entry DROP CONSTRAINT timeline_entry_entity_type_check;
ALTER TABLE timeline_entry ADD CONSTRAINT timeline_entry_entity_type_check CHECK (entity_type IN (
    'ACCOUNT', 'PERSON', 'CONTACT', 'NETWORK_LINK', 'JOB_ORDER', 'APPLICATION', 'PROPOSAL', 'ACTIVITY', 'NEXT_ACTION',
    'USER', 'CONSENT_RECORD', 'CONSENT_REQUEST', 'PRIVACY_NOTICE', 'DATA_SUBJECT_REQUEST', 'IMPORT_JOB',
    'PERSON_FILE', 'CV_INTAKE_ITEM', 'OFF_LIMITS_RULE'));
