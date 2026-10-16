-- pipeline: job_order, application, proposal, activity, next_action, timeline_entry
CREATE TABLE job_order (
    id                uuid PRIMARY KEY,
    account_id        uuid         NOT NULL REFERENCES account (id),
    title             varchar(200) NOT NULL,
    location          varchar(200),
    seniority         varchar(100),
    description       text,
    fee_terms         text,
    openings          int          NOT NULL DEFAULT 1 CHECK (openings >= 1),
    status            varchar(40)  NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'SOURCING', 'SHORTLIST_SENT', 'INTERVIEWING', 'OFFER', 'FILLED', 'CANCELLED')),
    status_changed_at timestamptz  NOT NULL DEFAULT now(),
    owner_id          uuid         NOT NULL REFERENCES app_user (id),
    opened_on         date         NOT NULL DEFAULT current_date,
    closed_on         date,
    created_at        timestamptz  NOT NULL DEFAULT now(),
    created_by        uuid REFERENCES app_user (id),
    updated_at        timestamptz  NOT NULL DEFAULT now(),
    updated_by        uuid REFERENCES app_user (id),
    version           bigint       NOT NULL DEFAULT 0
);
CREATE INDEX idx_job_order_account ON job_order (account_id);
CREATE INDEX idx_job_order_title_trgm ON job_order USING gin (immutable_unaccent(lower(title)) gin_trgm_ops);
CREATE INDEX idx_job_order_status ON job_order (status);
CREATE INDEX idx_job_order_owner ON job_order (owner_id);

CREATE TABLE application (
    id                uuid PRIMARY KEY,
    job_order_id      uuid        NOT NULL REFERENCES job_order (id),
    person_id         uuid        NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    stage             varchar(40) NOT NULL DEFAULT 'SOURCED'
        CHECK (stage IN ('SOURCED', 'SCREENED', 'PRESENTED', 'CLIENT_INTERVIEW', 'OFFER', 'PLACED', 'REJECTED')),
    rejection_reason  text,
    stage_changed_at  timestamptz NOT NULL DEFAULT now(),
    created_at        timestamptz NOT NULL DEFAULT now(),
    created_by        uuid REFERENCES app_user (id),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    updated_by        uuid REFERENCES app_user (id),
    version           bigint      NOT NULL DEFAULT 0,
    CONSTRAINT ck_application_rejection CHECK (stage <> 'REJECTED' OR rejection_reason IS NOT NULL),
    CONSTRAINT uq_application UNIQUE (job_order_id, person_id)
);
CREATE INDEX idx_application_person ON application (person_id);
CREATE INDEX idx_application_job_stage ON application (job_order_id, stage);

CREATE TABLE proposal (
    id                uuid PRIMARY KEY,
    account_id        uuid         NOT NULL REFERENCES account (id),
    title             varchar(200) NOT NULL,
    terms             text,
    fee_amount        numeric(14, 2),
    currency          char(3),
    status            varchar(40)  NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'SENT', 'ACCEPTED', 'REJECTED')),
    status_changed_at timestamptz  NOT NULL DEFAULT now(),
    status_reason     text,
    sent_on           date,
    decided_on        date,
    owner_id          uuid         NOT NULL REFERENCES app_user (id),
    created_at        timestamptz  NOT NULL DEFAULT now(),
    created_by        uuid REFERENCES app_user (id),
    updated_at        timestamptz  NOT NULL DEFAULT now(),
    updated_by        uuid REFERENCES app_user (id),
    version           bigint       NOT NULL DEFAULT 0
);
CREATE INDEX idx_proposal_account ON proposal (account_id);
CREATE INDEX idx_proposal_owner ON proposal (owner_id);
CREATE INDEX idx_proposal_status ON proposal (status);

CREATE TABLE activity (
    id           uuid PRIMARY KEY,
    type         varchar(40)  NOT NULL CHECK (type IN ('CALL', 'VISIT', 'MEETING', 'EMAIL', 'NOTE')),
    note         text         NOT NULL,
    occurred_at  timestamptz  NOT NULL,
    account_id   uuid REFERENCES account (id),
    person_id    uuid REFERENCES person (id) ON DELETE SET NULL,
    job_order_id uuid REFERENCES job_order (id),
    user_id      uuid         NOT NULL REFERENCES app_user (id),
    created_at   timestamptz  NOT NULL DEFAULT now(),
    created_by   uuid REFERENCES app_user (id),
    updated_at   timestamptz  NOT NULL DEFAULT now(),
    updated_by   uuid REFERENCES app_user (id),
    version      bigint       NOT NULL DEFAULT 0,
    CONSTRAINT ck_activity_target CHECK (account_id IS NOT NULL OR person_id IS NOT NULL)
);
CREATE INDEX idx_activity_account ON activity (account_id, occurred_at DESC);
CREATE INDEX idx_activity_person ON activity (person_id, occurred_at DESC);
CREATE INDEX idx_activity_job_order ON activity (job_order_id);
CREATE INDEX idx_activity_user ON activity (user_id);

CREATE TABLE next_action (
    id              uuid PRIMARY KEY,
    title           varchar(300) NOT NULL,
    due_on          date         NOT NULL,
    owner_id        uuid         NOT NULL REFERENCES app_user (id),
    account_id      uuid REFERENCES account (id),
    person_id       uuid REFERENCES person (id) ON DELETE SET NULL,
    job_order_id    uuid REFERENCES job_order (id),
    done            boolean      NOT NULL DEFAULT false,
    done_at         timestamptz,
    source_event_id uuid,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    created_by      uuid REFERENCES app_user (id),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    updated_by      uuid REFERENCES app_user (id),
    version         bigint       NOT NULL DEFAULT 0
);
CREATE INDEX idx_next_action_owner ON next_action (owner_id, done, due_on);
CREATE INDEX idx_next_action_account ON next_action (account_id);
CREATE INDEX idx_next_action_person ON next_action (person_id);
CREATE INDEX idx_next_action_job_order ON next_action (job_order_id);
CREATE UNIQUE INDEX uq_next_action_source_event ON next_action (source_event_id) WHERE source_event_id IS NOT NULL;

CREATE TABLE timeline_entry (
    id              uuid PRIMARY KEY,
    account_id      uuid         NOT NULL REFERENCES account (id),
    occurred_at     timestamptz  NOT NULL,
    kind            varchar(40)  NOT NULL
        CHECK (kind IN ('ACCOUNT', 'ACCOUNT_STATUS', 'CONTACT', 'ACTIVITY', 'PROPOSAL', 'JOB_ORDER', 'CANDIDATE')),
    entity_type     varchar(40)  NOT NULL CHECK (entity_type IN ('ACCOUNT', 'PERSON', 'CONTACT', 'NETWORK_LINK', 'JOB_ORDER',
        'APPLICATION', 'PROPOSAL', 'ACTIVITY', 'NEXT_ACTION', 'USER', 'CONSENT_RECORD', 'CONSENT_REQUEST',
        'PRIVACY_NOTICE', 'DATA_SUBJECT_REQUEST', 'IMPORT_JOB')),
    entity_id       uuid         NOT NULL,
    from_status     varchar(40),
    to_status       varchar(40),
    reason          text,
    summary         varchar(500),
    person_id       uuid,
    job_order_id    uuid,
    actor_id        uuid REFERENCES app_user (id),
    source_event_id uuid         NOT NULL,
    CONSTRAINT uq_timeline_source_event UNIQUE (source_event_id)
);
CREATE INDEX idx_timeline_account ON timeline_entry (account_id, occurred_at DESC);
CREATE INDEX idx_timeline_person ON timeline_entry (person_id) WHERE person_id IS NOT NULL;
CREATE INDEX idx_timeline_actor ON timeline_entry (actor_id);

-- Envers history tables
CREATE TABLE job_order_aud (
    id                uuid   NOT NULL,
    rev               bigint NOT NULL REFERENCES revinfo (rev),
    revtype           smallint,
    revend            bigint REFERENCES revinfo (rev),
    account_id        uuid,
    title             varchar(200),
    location          varchar(200),
    seniority         varchar(100),
    description       text,
    fee_terms         text,
    openings          int,
    status            varchar(40),
    status_changed_at timestamptz,
    owner_id          uuid,
    opened_on         date,
    closed_on         date,
    created_at        timestamptz,
    created_by        uuid,
    updated_at        timestamptz,
    updated_by        uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_job_order_aud_rev ON job_order_aud (rev);
CREATE INDEX idx_job_order_aud_revend ON job_order_aud (revend);

CREATE TABLE application_aud (
    id               uuid   NOT NULL,
    rev              bigint NOT NULL REFERENCES revinfo (rev),
    revtype          smallint,
    revend           bigint REFERENCES revinfo (rev),
    job_order_id     uuid,
    person_id        uuid,
    stage            varchar(40),
    rejection_reason text,
    stage_changed_at timestamptz,
    created_at       timestamptz,
    created_by       uuid,
    updated_at       timestamptz,
    updated_by       uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_application_aud_person ON application_aud (person_id);
CREATE INDEX idx_application_aud_rev ON application_aud (rev);
CREATE INDEX idx_application_aud_revend ON application_aud (revend);

CREATE TABLE proposal_aud (
    id                uuid   NOT NULL,
    rev               bigint NOT NULL REFERENCES revinfo (rev),
    revtype           smallint,
    revend            bigint REFERENCES revinfo (rev),
    account_id        uuid,
    title             varchar(200),
    terms             text,
    fee_amount        numeric(14, 2),
    currency          char(3),
    status            varchar(40),
    status_changed_at timestamptz,
    status_reason     text,
    sent_on           date,
    decided_on        date,
    owner_id          uuid,
    created_at        timestamptz,
    created_by        uuid,
    updated_at        timestamptz,
    updated_by        uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_proposal_aud_rev ON proposal_aud (rev);
CREATE INDEX idx_proposal_aud_revend ON proposal_aud (revend);

CREATE TABLE activity_aud (
    id           uuid   NOT NULL,
    rev          bigint NOT NULL REFERENCES revinfo (rev),
    revtype      smallint,
    revend       bigint REFERENCES revinfo (rev),
    type         varchar(40),
    note         text,
    occurred_at  timestamptz,
    account_id   uuid,
    person_id    uuid,
    job_order_id uuid,
    user_id      uuid,
    created_at   timestamptz,
    created_by   uuid,
    updated_at   timestamptz,
    updated_by   uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_activity_aud_person ON activity_aud (person_id);
CREATE INDEX idx_activity_aud_rev ON activity_aud (rev);
CREATE INDEX idx_activity_aud_revend ON activity_aud (revend);

CREATE TABLE next_action_aud (
    id              uuid   NOT NULL,
    rev             bigint NOT NULL REFERENCES revinfo (rev),
    revtype         smallint,
    revend          bigint REFERENCES revinfo (rev),
    title           varchar(300),
    due_on          date,
    owner_id        uuid,
    account_id      uuid,
    person_id       uuid,
    job_order_id    uuid,
    done            boolean,
    done_at         timestamptz,
    source_event_id uuid,
    created_at      timestamptz,
    created_by      uuid,
    updated_at      timestamptz,
    updated_by      uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_next_action_aud_person ON next_action_aud (person_id);
CREATE INDEX idx_next_action_aud_rev ON next_action_aud (rev);
CREATE INDEX idx_next_action_aud_revend ON next_action_aud (revend);
