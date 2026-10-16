-- privacy: person_privacy, privacy_notice, consent_record, consent_request, retention_settings, retention_run,
-- data_subject_request
CREATE TABLE person_privacy (
    person_id        uuid PRIMARY KEY REFERENCES person (id) ON DELETE CASCADE,
    consent_status   varchar(40) NOT NULL DEFAULT 'NOT_SENT'
        CHECK (consent_status IN ('NOT_SENT', 'SENT', 'GIVEN', 'REFUSED', 'WITHDRAWN')),
    notice_sent_at   timestamptz,
    basis_date       date        NOT NULL DEFAULT current_date,
    retain_until     date        NOT NULL,
    retention_status varchar(40) NOT NULL DEFAULT 'ACTIVE'
        CHECK (retention_status IN ('ACTIVE', 'FLAGGED', 'ANONYMISED')),
    flagged_at       timestamptz,
    import_id        uuid,
    version          bigint      NOT NULL DEFAULT 0
);
CREATE INDEX idx_person_privacy_retain_until ON person_privacy (retain_until);
CREATE INDEX idx_person_privacy_import ON person_privacy (import_id) WHERE import_id IS NOT NULL;

CREATE TABLE privacy_notice (
    id             uuid PRIMARY KEY,
    version        varchar(40) NOT NULL,
    language       varchar(2)  NOT NULL CHECK (language IN ('TR', 'EN')),
    notice_text    text        NOT NULL,
    consent_text   text        NOT NULL,
    effective_from date        NOT NULL,
    created_at     timestamptz NOT NULL DEFAULT now(),
    created_by     uuid REFERENCES app_user (id),
    CONSTRAINT uq_privacy_notice_version UNIQUE (version, language)
);
CREATE INDEX idx_privacy_notice_effective ON privacy_notice (language, effective_from DESC);

CREATE TABLE consent_request (
    id              uuid PRIMARY KEY,
    person_id       uuid        NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    notice_id       uuid        NOT NULL REFERENCES privacy_notice (id),
    language        varchar(2)  NOT NULL CHECK (language IN ('TR', 'EN')),
    token_hash      char(64)    NOT NULL,
    delivery_status varchar(40) NOT NULL CHECK (delivery_status IN ('QUEUED', 'SENT', 'FAILED')),
    queued_at       timestamptz NOT NULL DEFAULT now(),
    sent_at         timestamptz,
    failure_reason  text,
    expires_at      timestamptz NOT NULL,
    responded_at    timestamptz,
    response        varchar(40) CHECK (response IN ('GIVEN', 'REFUSED', 'WITHDRAWN')),
    requested_by    uuid REFERENCES app_user (id),
    CONSTRAINT uq_consent_request_token UNIQUE (token_hash)
);
CREATE INDEX idx_consent_request_person ON consent_request (person_id);
CREATE INDEX idx_consent_request_notice ON consent_request (notice_id);
CREATE INDEX idx_consent_request_delivery ON consent_request (delivery_status);

CREATE TABLE consent_record (
    id                  uuid PRIMARY KEY,
    person_id           uuid        NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    decision            varchar(40) NOT NULL CHECK (decision IN ('GIVEN', 'REFUSED', 'WITHDRAWN')),
    channel             varchar(40) NOT NULL
        CHECK (channel IN ('EMAIL_LINK', 'WEBSITE_FORM', 'IN_PERSON', 'PHONE', 'IMPORT')),
    notice_id           uuid        NOT NULL REFERENCES privacy_notice (id),
    consent_request_id  uuid REFERENCES consent_request (id),
    recorded_at         timestamptz NOT NULL DEFAULT now(),
    recorded_by         uuid REFERENCES app_user (id),
    evidence            text,
    ip_address          varchar(45)
);
CREATE INDEX idx_consent_record_person ON consent_record (person_id, recorded_at DESC);
CREATE INDEX idx_consent_record_notice ON consent_record (notice_id);
CREATE INDEX idx_consent_record_request ON consent_record (consent_request_id) WHERE consent_request_id IS NOT NULL;

CREATE TABLE retention_settings (
    id                       smallint PRIMARY KEY CHECK (id = 1),
    retention_months         int          NOT NULL DEFAULT 24,
    basis                    varchar(40)  NOT NULL DEFAULT 'FROM_CREATED'
        CHECK (basis IN ('FROM_CREATED', 'FROM_LAST_CONTACT')),
    action                   varchar(40)  NOT NULL DEFAULT 'ANONYMISE' CHECK (action IN ('ANONYMISE', 'DELETE')),
    warning_days             int          NOT NULL DEFAULT 30,
    auto_send_notice         boolean      NOT NULL DEFAULT true,
    auto_send_person_types   varchar(200) NOT NULL DEFAULT 'CANDIDATE,EXECUTIVE',
    renew_on_consent         boolean      NOT NULL DEFAULT true,
    created_at               timestamptz  NOT NULL DEFAULT now(),
    created_by               uuid REFERENCES app_user (id),
    updated_at               timestamptz  NOT NULL DEFAULT now(),
    updated_by               uuid REFERENCES app_user (id),
    version                  bigint       NOT NULL DEFAULT 0
);
-- The single settings row; the application only ever updates it.
INSERT INTO retention_settings (id) VALUES (1);

CREATE TABLE retention_run (
    id               uuid PRIMARY KEY,
    started_at       timestamptz NOT NULL,
    finished_at      timestamptz,
    triggered_by     uuid REFERENCES app_user (id),
    dry_run          boolean     NOT NULL DEFAULT false,
    flagged_count    int         NOT NULL DEFAULT 0,
    anonymised_count int         NOT NULL DEFAULT 0,
    deleted_count    int         NOT NULL DEFAULT 0,
    failure_reason   text
);
CREATE INDEX idx_retention_run_started ON retention_run (started_at DESC);
CREATE INDEX idx_retention_run_user ON retention_run (triggered_by) WHERE triggered_by IS NOT NULL;

CREATE TABLE data_subject_request (
    id              uuid PRIMARY KEY,
    person_id       uuid REFERENCES person (id) ON DELETE SET NULL,
    requester_name  varchar(200) NOT NULL,
    requester_email varchar(254) NOT NULL,
    type            varchar(40)  NOT NULL CHECK (type IN ('ACCESS', 'CORRECTION', 'DELETION', 'OTHER')),
    status          varchar(40)  NOT NULL DEFAULT 'RECEIVED'
        CHECK (status IN ('RECEIVED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED')),
    description     text,
    received_at     timestamptz  NOT NULL DEFAULT now(),
    due_on          date         NOT NULL,
    resolved_at     timestamptz,
    resolution      text,
    handled_by      uuid REFERENCES app_user (id),
    created_at      timestamptz  NOT NULL DEFAULT now(),
    created_by      uuid REFERENCES app_user (id),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    updated_by      uuid REFERENCES app_user (id),
    version         bigint       NOT NULL DEFAULT 0
);
CREATE INDEX idx_dsr_person ON data_subject_request (person_id);
CREATE INDEX idx_dsr_status ON data_subject_request (status);
CREATE INDEX idx_dsr_due ON data_subject_request (due_on);
CREATE INDEX idx_dsr_handled_by ON data_subject_request (handled_by) WHERE handled_by IS NOT NULL;

-- Envers history tables
CREATE TABLE person_privacy_aud (
    person_id        uuid   NOT NULL,
    rev              bigint NOT NULL REFERENCES revinfo (rev),
    revtype          smallint,
    revend           bigint REFERENCES revinfo (rev),
    consent_status   varchar(40),
    notice_sent_at   timestamptz,
    basis_date       date,
    retain_until     date,
    retention_status varchar(40),
    flagged_at       timestamptz,
    import_id        uuid,
    PRIMARY KEY (person_id, rev)
);
CREATE INDEX idx_person_privacy_aud_rev ON person_privacy_aud (rev);
CREATE INDEX idx_person_privacy_aud_revend ON person_privacy_aud (revend);

CREATE TABLE retention_settings_aud (
    id                     smallint NOT NULL,
    rev                    bigint   NOT NULL REFERENCES revinfo (rev),
    revtype                smallint,
    revend                 bigint REFERENCES revinfo (rev),
    retention_months       int,
    basis                  varchar(40),
    action                 varchar(40),
    warning_days           int,
    auto_send_notice       boolean,
    auto_send_person_types varchar(200),
    renew_on_consent       boolean,
    created_at             timestamptz,
    created_by             uuid,
    updated_at             timestamptz,
    updated_by             uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_retention_settings_aud_rev ON retention_settings_aud (rev);
CREATE INDEX idx_retention_settings_aud_revend ON retention_settings_aud (revend);

CREATE TABLE data_subject_request_aud (
    id              uuid   NOT NULL,
    rev             bigint NOT NULL REFERENCES revinfo (rev),
    revtype         smallint,
    revend          bigint REFERENCES revinfo (rev),
    person_id       uuid,
    requester_name  varchar(200),
    requester_email varchar(254),
    type            varchar(40),
    status          varchar(40),
    description     text,
    received_at     timestamptz,
    due_on          date,
    resolved_at     timestamptz,
    resolution      text,
    handled_by      uuid,
    created_at      timestamptz,
    created_by      uuid,
    updated_at      timestamptz,
    updated_by      uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_dsr_aud_person ON data_subject_request_aud (person_id);
CREATE INDEX idx_dsr_aud_rev ON data_subject_request_aud (rev);
CREATE INDEX idx_dsr_aud_revend ON data_subject_request_aud (revend);

-- Envers needs a history row for it, otherwise the first update fails with the validity audit strategy.
INSERT INTO revinfo (rev, revtstmp) VALUES (nextval('revinfo_seq'), (extract(epoch FROM now()) * 1000)::bigint);
INSERT INTO retention_settings_aud (id, rev, revtype, retention_months, basis, action, warning_days, auto_send_notice,
                                    auto_send_person_types, renew_on_consent, created_at, updated_at)
SELECT s.id, currval('revinfo_seq'), 0, s.retention_months, s.basis, s.action, s.warning_days, s.auto_send_notice,
       s.auto_send_person_types, s.renew_on_consent, s.created_at, s.updated_at
FROM retention_settings s;
