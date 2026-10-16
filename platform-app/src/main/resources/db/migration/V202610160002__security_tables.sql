-- security: app_user, access_log, Envers revinfo
CREATE TABLE app_user (
    id                   uuid PRIMARY KEY,
    email                varchar(254) NOT NULL,
    name                 varchar(120) NOT NULL,
    role                 varchar(40)  NOT NULL CHECK (role IN ('CONSULTANT', 'MANAGER', 'ADMIN')),
    active               boolean      NOT NULL DEFAULT true,
    language             varchar(2)   NOT NULL DEFAULT 'TR' CHECK (language IN ('TR', 'EN')),
    password_hash        varchar(100),
    must_change_password boolean      NOT NULL DEFAULT true,
    last_login_at        timestamptz,
    created_at           timestamptz  NOT NULL DEFAULT now(),
    created_by           uuid REFERENCES app_user (id),
    updated_at           timestamptz  NOT NULL DEFAULT now(),
    updated_by           uuid REFERENCES app_user (id),
    version              bigint       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_app_user_email ON app_user (lower(email));

CREATE SEQUENCE revinfo_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE revinfo (
    rev      bigint PRIMARY KEY,
    revtstmp bigint,
    user_id  uuid REFERENCES app_user (id)
);
CREATE INDEX idx_revinfo_user ON revinfo (user_id);

CREATE TABLE app_user_aud (
    id                   uuid     NOT NULL,
    rev                  bigint   NOT NULL REFERENCES revinfo (rev),
    revtype              smallint,
    revend               bigint REFERENCES revinfo (rev),
    email                varchar(254),
    name                 varchar(120),
    role                 varchar(40),
    active               boolean,
    language             varchar(2),
    must_change_password boolean,
    created_at           timestamptz,
    created_by           uuid,
    updated_at           timestamptz,
    updated_by           uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_app_user_aud_rev ON app_user_aud (rev);
CREATE INDEX idx_app_user_aud_revend ON app_user_aud (revend);

CREATE TABLE access_log (
    id          uuid PRIMARY KEY,
    user_id     uuid        NOT NULL REFERENCES app_user (id),
    action      varchar(40) NOT NULL CHECK (action IN ('VIEW', 'EXPORT', 'DOWNLOAD')),
    entity_type varchar(40) NOT NULL CHECK (entity_type IN ('ACCOUNT', 'PERSON', 'CONTACT', 'NETWORK_LINK', 'JOB_ORDER',
        'APPLICATION', 'PROPOSAL', 'ACTIVITY', 'NEXT_ACTION', 'USER', 'CONSENT_RECORD', 'CONSENT_REQUEST',
        'PRIVACY_NOTICE', 'DATA_SUBJECT_REQUEST', 'IMPORT_JOB')),
    entity_id   uuid        NOT NULL,
    at          timestamptz NOT NULL DEFAULT now(),
    ip_address  varchar(45)
);
CREATE INDEX idx_access_log_entity ON access_log (entity_type, entity_id, at DESC);
CREATE INDEX idx_access_log_user ON access_log (user_id, at DESC);
