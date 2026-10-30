-- Contract 1.3.0: person archive and pool columns, account_merge, tag_definition, import rollback, widened
-- enum CHECKs (EntityType, TimelineKind, ImportStatus). Owner: accounts.

ALTER TABLE person
    ADD COLUMN archived_at    timestamptz,
    ADD COLUMN archived_by    uuid REFERENCES app_user (id),
    ADD COLUMN archive_reason varchar(40)
        CONSTRAINT ck_person_archive_reason CHECK (archive_reason IN
            ('DUPLICATE', 'CREATED_BY_MISTAKE', 'NO_LONGER_RELEVANT', 'MERGED', 'IMPORT_ROLLBACK', 'OTHER')),
    ADD COLUMN archive_note   varchar(500),
    ADD COLUMN skills         varchar(50)[] NOT NULL DEFAULT '{}'
        CONSTRAINT ck_person_skills_max CHECK (cardinality(skills) <= 30),
    ADD COLUMN function_area  varchar(40)
        CONSTRAINT ck_person_function_area CHECK (function_area IN
            ('GENERAL_MANAGEMENT', 'FINANCE', 'HR', 'SALES', 'MARKETING', 'OPERATIONS', 'SUPPLY_CHAIN', 'PROCUREMENT',
             'ENGINEERING', 'IT', 'RND', 'LEGAL', 'OTHER')),
    ADD COLUMN seniority      varchar(40)
        CONSTRAINT ck_person_seniority CHECK (seniority IN
            ('BOARD', 'C_LEVEL', 'VP_DIRECTOR', 'HEAD', 'MANAGER', 'SPECIALIST')),
    ADD CONSTRAINT ck_person_archive_pair CHECK ((archived_at IS NULL) = (archive_reason IS NULL)),
    ADD CONSTRAINT ck_person_archive_by CHECK ((archived_at IS NULL) = (archived_by IS NULL));
CREATE INDEX idx_person_archived ON person (archived_at) WHERE archived_at IS NOT NULL;
CREATE INDEX idx_person_archived_by ON person (archived_by) WHERE archived_by IS NOT NULL;
CREATE INDEX idx_person_skills ON person USING gin (skills);
CREATE INDEX idx_person_function_area ON person (function_area);
CREATE INDEX idx_person_seniority ON person (seniority);
CREATE INDEX idx_person_languages ON person USING gin (languages jsonb_path_ops);

ALTER TABLE person_aud
    ADD COLUMN archived_at    timestamptz,
    ADD COLUMN archived_by    uuid,
    ADD COLUMN archive_reason varchar(40),
    ADD COLUMN archive_note   varchar(500),
    ADD COLUMN skills         varchar(50)[],
    ADD COLUMN function_area  varchar(40),
    ADD COLUMN seniority      varchar(40);

CREATE TABLE account_merge (
    id          uuid PRIMARY KEY,
    survivor_id uuid        NOT NULL REFERENCES account (id),
    merged_id   uuid        NOT NULL UNIQUE REFERENCES account (id),
    moved       jsonb       NOT NULL,
    merged_at   timestamptz NOT NULL,
    merged_by   uuid        NOT NULL REFERENCES app_user (id)
);
CREATE INDEX idx_account_merge_survivor ON account_merge (survivor_id);
CREATE INDEX idx_account_merge_merged_by ON account_merge (merged_by);

CREATE TABLE tag_definition (
    id          uuid PRIMARY KEY,
    name        varchar(50)  NOT NULL
        CONSTRAINT uq_tag_definition_name UNIQUE
        CONSTRAINT ck_tag_definition_lower CHECK (name = lower(name)),
    colour      varchar(40)  NOT NULL DEFAULT 'GREY'
        CONSTRAINT ck_tag_definition_colour CHECK (colour IN
            ('GREY', 'BLUE', 'GREEN', 'AMBER', 'RED', 'PURPLE', 'TEAL')),
    description varchar(200),
    talent_pool boolean      NOT NULL DEFAULT false,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    created_by  uuid REFERENCES app_user (id),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  uuid REFERENCES app_user (id),
    version     bigint       NOT NULL DEFAULT 0
);
CREATE TABLE tag_definition_aud (
    id          uuid   NOT NULL,
    rev         bigint NOT NULL REFERENCES revinfo (rev),
    revtype     smallint,
    revend      bigint REFERENCES revinfo (rev),
    name        varchar(50),
    colour      varchar(40),
    description varchar(200),
    talent_pool boolean,
    created_at  timestamptz,
    created_by  uuid,
    updated_at  timestamptz,
    updated_by  uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_tag_definition_aud_rev ON tag_definition_aud (rev);
CREATE INDEX idx_tag_definition_aud_revend ON tag_definition_aud (revend);

-- Import column mapping and rollback.
ALTER TABLE import_job
    ADD COLUMN mapping        jsonb,
    ADD COLUMN rolled_back_at timestamptz;
ALTER TABLE import_job DROP CONSTRAINT import_job_status_check;
ALTER TABLE import_job ADD CONSTRAINT import_job_status_check CHECK (status IN
    ('VALIDATED', 'IMPORTING', 'COMPLETED', 'FAILED', 'DISCARDED', 'ROLLED_BACK'));

CREATE TABLE import_job_record (
    import_id   uuid        NOT NULL REFERENCES import_job (id) ON DELETE CASCADE,
    entity_type varchar(40) NOT NULL CHECK (entity_type IN (
        'ACCOUNT', 'PERSON', 'CONTACT', 'NETWORK_LINK', 'JOB_ORDER', 'APPLICATION', 'PROPOSAL', 'ACTIVITY',
        'NEXT_ACTION', 'USER', 'CONSENT_RECORD', 'CONSENT_REQUEST', 'PRIVACY_NOTICE', 'DATA_SUBJECT_REQUEST',
        'IMPORT_JOB', 'PERSON_FILE', 'CV_INTAKE_ITEM', 'OFF_LIMITS_RULE', 'TAG', 'EVALUATION', 'INTERVIEW_NOTE',
        'CANDIDATE_REFERENCE', 'CLIENT_FEEDBACK', 'TARGET_COMPANY', 'AGREEMENT', 'FEE_SCHEDULE_ITEM', 'PLACEMENT',
        'CONFLICT_CHECK', 'EXPORT_JOB', 'CLIENT_REPORT')),
    entity_id   uuid        NOT NULL,
    PRIMARY KEY (import_id, entity_type, entity_id)
);
CREATE INDEX idx_import_job_record_entity ON import_job_record (entity_id);

-- EntityType and TimelineKind gained values (CHECKs re-created from contracts/enums).
ALTER TABLE access_log DROP CONSTRAINT access_log_entity_type_check;
ALTER TABLE access_log ADD CONSTRAINT access_log_entity_type_check CHECK (entity_type IN (
    'ACCOUNT', 'PERSON', 'CONTACT', 'NETWORK_LINK', 'JOB_ORDER', 'APPLICATION', 'PROPOSAL', 'ACTIVITY', 'NEXT_ACTION',
    'USER', 'CONSENT_RECORD', 'CONSENT_REQUEST', 'PRIVACY_NOTICE', 'DATA_SUBJECT_REQUEST', 'IMPORT_JOB',
    'PERSON_FILE', 'CV_INTAKE_ITEM', 'OFF_LIMITS_RULE', 'TAG', 'EVALUATION', 'INTERVIEW_NOTE', 'CANDIDATE_REFERENCE',
    'CLIENT_FEEDBACK', 'TARGET_COMPANY', 'AGREEMENT', 'FEE_SCHEDULE_ITEM', 'PLACEMENT', 'CONFLICT_CHECK',
    'EXPORT_JOB', 'CLIENT_REPORT'));
ALTER TABLE timeline_entry DROP CONSTRAINT timeline_entry_entity_type_check;
ALTER TABLE timeline_entry ADD CONSTRAINT timeline_entry_entity_type_check CHECK (entity_type IN (
    'ACCOUNT', 'PERSON', 'CONTACT', 'NETWORK_LINK', 'JOB_ORDER', 'APPLICATION', 'PROPOSAL', 'ACTIVITY', 'NEXT_ACTION',
    'USER', 'CONSENT_RECORD', 'CONSENT_REQUEST', 'PRIVACY_NOTICE', 'DATA_SUBJECT_REQUEST', 'IMPORT_JOB',
    'PERSON_FILE', 'CV_INTAKE_ITEM', 'OFF_LIMITS_RULE', 'TAG', 'EVALUATION', 'INTERVIEW_NOTE', 'CANDIDATE_REFERENCE',
    'CLIENT_FEEDBACK', 'TARGET_COMPANY', 'AGREEMENT', 'FEE_SCHEDULE_ITEM', 'PLACEMENT', 'CONFLICT_CHECK',
    'EXPORT_JOB', 'CLIENT_REPORT'));
ALTER TABLE timeline_entry DROP CONSTRAINT timeline_entry_kind_check;
ALTER TABLE timeline_entry ADD CONSTRAINT timeline_entry_kind_check CHECK (kind IN
    ('ACCOUNT', 'ACCOUNT_STATUS', 'CONTACT', 'ACTIVITY', 'PROPOSAL', 'JOB_ORDER', 'CANDIDATE', 'ARCHIVE', 'MERGE',
     'PLACEMENT'));
