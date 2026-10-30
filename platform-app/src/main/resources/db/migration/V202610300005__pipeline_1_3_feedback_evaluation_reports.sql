-- Contract 1.3.0: client feedback, candidate evaluation, interview notes, references, client reports.
-- Owner: pipeline. FK rule (ADR 0009): person FKs cascade; indirect FKs to application are set null unless the row
-- is the person's own data.

CREATE TABLE client_feedback (
    id                 uuid PRIMARY KEY,
    application_id     uuid          NOT NULL REFERENCES application (id) ON DELETE CASCADE,
    person_id          uuid          NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    round              smallint      NOT NULL CONSTRAINT ck_client_feedback_round CHECK (round BETWEEN 1 AND 20),
    feedback_on        date          NOT NULL,
    participants       varchar(500),
    outcome            varchar(40)   NOT NULL CHECK (outcome IN ('ADVANCE', 'HOLD', 'DECLINE')),
    decline_reason     varchar(40)   CHECK (decline_reason IN
        ('EXPERIENCE', 'COMPENSATION', 'CULTURE_FIT', 'LOCATION', 'TIMING', 'OTHER_CANDIDATE', 'OTHER')),
    reason_text        varchar(1000),
    candidate_feedback varchar(2000),
    recorded_by        uuid          NOT NULL REFERENCES app_user (id),
    recorded_at        timestamptz   NOT NULL,
    created_at         timestamptz   NOT NULL DEFAULT now(),
    created_by         uuid REFERENCES app_user (id),
    updated_at         timestamptz   NOT NULL DEFAULT now(),
    updated_by         uuid REFERENCES app_user (id),
    version            bigint        NOT NULL DEFAULT 0,
    CONSTRAINT uq_client_feedback_round UNIQUE (application_id, round),
    CONSTRAINT ck_client_feedback_decline CHECK ((outcome = 'DECLINE') = (decline_reason IS NOT NULL))
);
CREATE INDEX idx_client_feedback_application ON client_feedback (application_id, round DESC);
CREATE INDEX idx_client_feedback_person ON client_feedback (person_id);
CREATE INDEX idx_client_feedback_recorded_by ON client_feedback (recorded_by);
CREATE TABLE client_feedback_aud (
    id                 uuid   NOT NULL,
    rev                bigint NOT NULL REFERENCES revinfo (rev),
    revtype            smallint,
    revend             bigint REFERENCES revinfo (rev),
    application_id     uuid,
    person_id          uuid,
    round              smallint,
    feedback_on        date,
    participants       varchar(500),
    outcome            varchar(40),
    decline_reason     varchar(40),
    reason_text        varchar(1000),
    candidate_feedback varchar(2000),
    recorded_by        uuid,
    recorded_at        timestamptz,
    created_at         timestamptz,
    created_by         uuid,
    updated_at         timestamptz,
    updated_by         uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_client_feedback_aud_person ON client_feedback_aud (person_id);
CREATE INDEX idx_client_feedback_aud_rev ON client_feedback_aud (rev);
CREATE INDEX idx_client_feedback_aud_revend ON client_feedback_aud (revend);

CREATE TABLE candidate_evaluation (
    id                    uuid PRIMARY KEY,
    person_id             uuid         NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    job_order_id          uuid REFERENCES job_order (id),
    application_id        uuid REFERENCES application (id) ON DELETE SET NULL,
    evaluator_id          uuid         NOT NULL REFERENCES app_user (id),
    evaluated_on          date         NOT NULL,
    interview_type        varchar(40)  NOT NULL CHECK (interview_type IN ('PHONE', 'VIDEO', 'IN_PERSON')),
    overall_rating        smallint     CONSTRAINT ck_candidate_evaluation_rating CHECK (overall_rating BETWEEN 1 AND 5),
    recommendation        varchar(40)  NOT NULL CHECK (recommendation IN ('SHORTLIST', 'HOLD', 'REJECT')),
    recommendation_reason varchar(500),
    summary               varchar(600),
    key_achievements      jsonb        NOT NULL DEFAULT '[]'
        CONSTRAINT ck_candidate_evaluation_achievements CHECK (jsonb_typeof(key_achievements) = 'array'),
    motivation            varchar(400),
    motivation_tags       varchar(40)[] NOT NULL DEFAULT '{}'
        CONSTRAINT ck_candidate_evaluation_motivation_tags CHECK
            (motivation_tags <@ ARRAY['GROWTH', 'PAY', 'STABILITY', 'RELOCATION', 'PUSH_FACTOR']::varchar[]),
    concerns              varchar(400),
    other_processes       varchar(1000),
    scores                jsonb        NOT NULL DEFAULT '[]'
        CONSTRAINT ck_candidate_evaluation_scores CHECK (jsonb_typeof(scores) = 'array'),
    private_notes         text,
    internal_only         boolean      NOT NULL DEFAULT false,
    report_blocks         varchar(40)[] NOT NULL DEFAULT '{SUMMARY,ACHIEVEMENTS,MOTIVATION,CONCERNS}'
        CONSTRAINT ck_candidate_evaluation_report_blocks CHECK
            (report_blocks <@ ARRAY['SUMMARY', 'ACHIEVEMENTS', 'MOTIVATION', 'CONCERNS', 'CRITERIA', 'REFERENCES']::varchar[]),
    created_at            timestamptz  NOT NULL DEFAULT now(),
    created_by            uuid REFERENCES app_user (id),
    updated_at            timestamptz  NOT NULL DEFAULT now(),
    updated_by            uuid REFERENCES app_user (id),
    version               bigint       NOT NULL DEFAULT 0
);
CREATE INDEX idx_candidate_evaluation_person ON candidate_evaluation (person_id, evaluated_on DESC);
CREATE INDEX idx_candidate_evaluation_application ON candidate_evaluation (application_id)
    WHERE application_id IS NOT NULL;
CREATE INDEX idx_candidate_evaluation_evaluator ON candidate_evaluation (evaluator_id);
CREATE UNIQUE INDEX uq_candidate_evaluation_search ON candidate_evaluation (person_id, job_order_id)
    WHERE job_order_id IS NOT NULL;
CREATE TABLE candidate_evaluation_aud (
    id                    uuid   NOT NULL,
    rev                   bigint NOT NULL REFERENCES revinfo (rev),
    revtype               smallint,
    revend                bigint REFERENCES revinfo (rev),
    person_id             uuid,
    job_order_id          uuid,
    application_id        uuid,
    evaluator_id          uuid,
    evaluated_on          date,
    interview_type        varchar(40),
    overall_rating        smallint,
    recommendation        varchar(40),
    recommendation_reason varchar(500),
    summary               varchar(600),
    key_achievements      jsonb,
    motivation            varchar(400),
    motivation_tags       varchar(40)[],
    concerns              varchar(400),
    other_processes       varchar(1000),
    scores                jsonb,
    private_notes         text,
    internal_only         boolean,
    report_blocks         varchar(40)[],
    created_at            timestamptz,
    created_by            uuid,
    updated_at            timestamptz,
    updated_by            uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_candidate_evaluation_aud_person ON candidate_evaluation_aud (person_id);
CREATE INDEX idx_candidate_evaluation_aud_rev ON candidate_evaluation_aud (rev);
CREATE INDEX idx_candidate_evaluation_aud_revend ON candidate_evaluation_aud (revend);

CREATE TABLE interview_note (
    id                uuid PRIMARY KEY,
    person_id         uuid        NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    job_order_id      uuid REFERENCES job_order (id),
    application_id    uuid REFERENCES application (id) ON DELETE SET NULL,
    interviewer_id    uuid        NOT NULL REFERENCES app_user (id),
    held_on           date        NOT NULL,
    interview_type    varchar(40) NOT NULL CHECK (interview_type IN ('PHONE', 'VIDEO', 'IN_PERSON')),
    body              text        NOT NULL,
    include_in_report boolean     NOT NULL DEFAULT false,
    created_at        timestamptz NOT NULL DEFAULT now(),
    created_by        uuid REFERENCES app_user (id),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    updated_by        uuid REFERENCES app_user (id),
    version           bigint      NOT NULL DEFAULT 0
);
CREATE INDEX idx_interview_note_person ON interview_note (person_id, held_on DESC);
CREATE INDEX idx_interview_note_job ON interview_note (job_order_id) WHERE job_order_id IS NOT NULL;
CREATE INDEX idx_interview_note_application ON interview_note (application_id) WHERE application_id IS NOT NULL;
CREATE INDEX idx_interview_note_interviewer ON interview_note (interviewer_id);
CREATE TABLE interview_note_aud (
    id                uuid   NOT NULL,
    rev               bigint NOT NULL REFERENCES revinfo (rev),
    revtype           smallint,
    revend            bigint REFERENCES revinfo (rev),
    person_id         uuid,
    job_order_id      uuid,
    application_id    uuid,
    interviewer_id    uuid,
    held_on           date,
    interview_type    varchar(40),
    body              text,
    include_in_report boolean,
    created_at        timestamptz,
    created_by        uuid,
    updated_at        timestamptz,
    updated_by        uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_interview_note_aud_person ON interview_note_aud (person_id);
CREATE INDEX idx_interview_note_aud_rev ON interview_note_aud (rev);
CREATE INDEX idx_interview_note_aud_revend ON interview_note_aud (revend);

CREATE TABLE candidate_reference (
    id                    uuid PRIMARY KEY,
    person_id             uuid         NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    job_order_id          uuid REFERENCES job_order (id),
    referee_name          varchar(200) NOT NULL,
    referee_relationship  varchar(200),
    referee_company       varchar(200),
    referee_email         varchar(254),
    referee_phone         varchar(50),
    status                varchar(40)  NOT NULL DEFAULT 'TO_REQUEST'
        CHECK (status IN ('TO_REQUEST', 'REQUESTED', 'RECEIVED', 'DECLINED')),
    summary               text,
    checked_on            date,
    checked_by            uuid REFERENCES app_user (id),
    include_in_report     boolean      NOT NULL DEFAULT false,
    created_at            timestamptz  NOT NULL DEFAULT now(),
    created_by            uuid REFERENCES app_user (id),
    updated_at            timestamptz  NOT NULL DEFAULT now(),
    updated_by            uuid REFERENCES app_user (id),
    version               bigint       NOT NULL DEFAULT 0
);
CREATE INDEX idx_candidate_reference_person ON candidate_reference (person_id);
CREATE INDEX idx_candidate_reference_job ON candidate_reference (job_order_id) WHERE job_order_id IS NOT NULL;
CREATE INDEX idx_candidate_reference_checked_by ON candidate_reference (checked_by) WHERE checked_by IS NOT NULL;
-- Third-party erasure by referee e-mail (ThirdPartyErasure, DSR).
CREATE INDEX idx_candidate_reference_referee_email ON candidate_reference (lower(referee_email))
    WHERE referee_email IS NOT NULL;
CREATE TABLE candidate_reference_aud (
    id                   uuid   NOT NULL,
    rev                  bigint NOT NULL REFERENCES revinfo (rev),
    revtype              smallint,
    revend               bigint REFERENCES revinfo (rev),
    person_id            uuid,
    job_order_id         uuid,
    referee_name         varchar(200),
    referee_relationship varchar(200),
    referee_company      varchar(200),
    referee_email        varchar(254),
    referee_phone        varchar(50),
    status               varchar(40),
    summary              text,
    checked_on           date,
    checked_by           uuid,
    include_in_report    boolean,
    created_at           timestamptz,
    created_by           uuid,
    updated_at           timestamptz,
    updated_by           uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_candidate_reference_aud_person ON candidate_reference_aud (person_id);
CREATE INDEX idx_candidate_reference_aud_email ON candidate_reference_aud (lower(referee_email))
    WHERE referee_email IS NOT NULL;
CREATE INDEX idx_candidate_reference_aud_rev ON candidate_reference_aud (rev);
CREATE INDEX idx_candidate_reference_aud_revend ON candidate_reference_aud (revend);

-- Immutable after insert except sent_on and sent_by; not audited. person_ids has no FK (like access_log).
CREATE TABLE client_report (
    id           uuid PRIMARY KEY,
    job_order_id uuid         NOT NULL REFERENCES job_order (id),
    kind         varchar(40)  NOT NULL CHECK (kind IN ('SHORTLIST', 'PROGRESS', 'CANDIDATE_PROFILE')),
    version      int          NOT NULL,
    language     varchar(2)   NOT NULL CHECK (language IN ('TR', 'EN')),
    blind        boolean      NOT NULL,
    person_ids   uuid[]       NOT NULL DEFAULT '{}',
    storage_key  varchar(300) NOT NULL UNIQUE,
    size_bytes   bigint       NOT NULL,
    generated_by uuid         NOT NULL REFERENCES app_user (id),
    generated_at timestamptz  NOT NULL,
    sent_on      date,
    sent_by      uuid REFERENCES app_user (id),
    CONSTRAINT uq_client_report_version UNIQUE (job_order_id, kind, version)
);
CREATE INDEX idx_client_report_person_ids ON client_report USING gin (person_ids);
CREATE INDEX idx_client_report_generated_by ON client_report (generated_by);
