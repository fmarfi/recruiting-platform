-- accounts: account, person, person_employment, contact, network_link, import_job, account_open_job
CREATE TABLE account (
    id                uuid PRIMARY KEY,
    name              varchar(200) NOT NULL,
    sector            varchar(100) NOT NULL,
    city              varchar(100),
    website           varchar(300),
    notes             text,
    owner_id          uuid         NOT NULL REFERENCES app_user (id),
    status            varchar(40)  NOT NULL DEFAULT 'LEAD'
        CHECK (status IN ('LEAD', 'PROPOSAL_SENT', 'ACTIVE', 'ON_HOLD', 'CLOSED')),
    status_changed_at timestamptz  NOT NULL DEFAULT now(),
    opened_on         date         NOT NULL DEFAULT current_date,
    open_job_count    int          NOT NULL DEFAULT 0,
    last_activity_at  timestamptz,
    created_at        timestamptz  NOT NULL DEFAULT now(),
    created_by        uuid REFERENCES app_user (id),
    updated_at        timestamptz  NOT NULL DEFAULT now(),
    updated_by        uuid REFERENCES app_user (id),
    version           bigint       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_account_name ON account (lower(name));
CREATE INDEX idx_account_name_trgm ON account USING gin (immutable_unaccent(lower(name)) gin_trgm_ops);
CREATE INDEX idx_account_sector ON account (sector);
CREATE INDEX idx_account_owner ON account (owner_id);
CREATE INDEX idx_account_status ON account (status);
CREATE INDEX idx_account_last_activity ON account (last_activity_at DESC);

CREATE TABLE person (
    id               uuid PRIMARY KEY,
    name             varchar(200) NOT NULL,
    type             varchar(40)  NOT NULL CHECK (type IN ('EXECUTIVE', 'CLIENT_CONTACT', 'REFERRER', 'CANDIDATE')),
    title            varchar(200),
    company          varchar(200),
    email            varchar(254),
    phone            varchar(50),
    city             varchar(100),
    linkedin_url     varchar(300),
    how_we_know      text,
    notes            text,
    owner_id         uuid         NOT NULL REFERENCES app_user (id),
    last_contact_at  timestamptz,
    consent_status   varchar(40)  NOT NULL DEFAULT 'NOT_SENT'
        CHECK (consent_status IN ('NOT_SENT', 'SENT', 'GIVEN', 'REFUSED', 'WITHDRAWN')),
    retention_status varchar(40)  NOT NULL DEFAULT 'ACTIVE'
        CHECK (retention_status IN ('ACTIVE', 'FLAGGED', 'ANONYMISED')),
    anonymised_at    timestamptz,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    created_by       uuid REFERENCES app_user (id),
    updated_at       timestamptz  NOT NULL DEFAULT now(),
    updated_by       uuid REFERENCES app_user (id),
    version          bigint       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_person_email ON person (lower(email)) WHERE email IS NOT NULL;
CREATE INDEX idx_person_name_trgm ON person USING gin (immutable_unaccent(lower(name)) gin_trgm_ops);
CREATE INDEX idx_person_company_trgm ON person USING gin (immutable_unaccent(lower(company)) gin_trgm_ops);
CREATE INDEX idx_person_email_trgm ON person USING gin (immutable_unaccent(lower(email)) gin_trgm_ops);
CREATE INDEX idx_person_type ON person (type);
CREATE INDEX idx_person_owner ON person (owner_id);
CREATE INDEX idx_person_last_contact ON person (last_contact_at);
CREATE INDEX idx_person_consent_status ON person (consent_status);
CREATE INDEX idx_person_retention_status ON person (retention_status);

CREATE TABLE person_employment (
    id         uuid PRIMARY KEY,
    person_id  uuid         NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    company    varchar(200) NOT NULL,
    title      varchar(200),
    start_date date,
    end_date   date,
    sort_order int          NOT NULL
);
CREATE INDEX idx_person_employment_person ON person_employment (person_id);
CREATE INDEX idx_person_employment_company_trgm
    ON person_employment USING gin (immutable_unaccent(lower(company)) gin_trgm_ops);

CREATE TABLE contact (
    id         uuid PRIMARY KEY,
    account_id uuid        NOT NULL REFERENCES account (id),
    person_id  uuid        NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    role       varchar(200),
    is_primary boolean     NOT NULL DEFAULT false,
    ended_on   date,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid REFERENCES app_user (id),
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid REFERENCES app_user (id),
    version    bigint      NOT NULL DEFAULT 0
);
CREATE INDEX idx_contact_account ON contact (account_id);
CREATE INDEX idx_contact_person ON contact (person_id);
CREATE UNIQUE INDEX uq_contact_primary ON contact (account_id) WHERE is_primary AND ended_on IS NULL;
CREATE UNIQUE INDEX uq_contact_active ON contact (account_id, person_id) WHERE ended_on IS NULL;

CREATE TABLE network_link (
    id             uuid PRIMARY KEY,
    from_person_id uuid        NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    to_person_id   uuid        NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    type           varchar(40) NOT NULL CHECK (type IN ('KNOWS', 'WORKED_WITH', 'INTRODUCED', 'REFERRED', 'REPORTS_TO')),
    note           text,
    created_at     timestamptz NOT NULL DEFAULT now(),
    created_by     uuid REFERENCES app_user (id),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    updated_by     uuid REFERENCES app_user (id),
    version        bigint      NOT NULL DEFAULT 0,
    CONSTRAINT ck_network_link_distinct CHECK (from_person_id <> to_person_id),
    CONSTRAINT uq_network_link UNIQUE (from_person_id, to_person_id, type)
);
CREATE INDEX idx_network_link_from ON network_link (from_person_id);
CREATE INDEX idx_network_link_to ON network_link (to_person_id);

CREATE TABLE import_job (
    id             uuid PRIMARY KEY,
    kind           varchar(40)  NOT NULL CHECK (kind IN ('ACCOUNTS', 'CONTACTS', 'PEOPLE')),
    status         varchar(40)  NOT NULL
        CHECK (status IN ('VALIDATED', 'IMPORTING', 'COMPLETED', 'FAILED', 'DISCARDED')),
    file_name      varchar(255),
    file_content   bytea,
    total_rows     int,
    valid_rows     int,
    duplicate_rows int,
    imported_rows  int,
    errors         jsonb,
    failure_reason text,
    completed_at   timestamptz,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    created_by     uuid REFERENCES app_user (id),
    updated_at     timestamptz  NOT NULL DEFAULT now(),
    updated_by     uuid REFERENCES app_user (id),
    version        bigint       NOT NULL DEFAULT 0
);
CREATE INDEX idx_import_job_status ON import_job (status);
CREATE INDEX idx_import_job_created ON import_job (created_at);

-- Projection fed by JobOrderCreated / JobOrderStatusChanged; job_order_id has no FK (pipeline owns the job order).
CREATE TABLE account_open_job (
    job_order_id uuid PRIMARY KEY,
    account_id   uuid         NOT NULL REFERENCES account (id),
    title        varchar(200),
    open         boolean      NOT NULL,
    updated_at   timestamptz  NOT NULL
);
CREATE INDEX idx_account_open_job_account ON account_open_job (account_id);

-- Envers history tables
CREATE TABLE account_aud (
    id                uuid   NOT NULL,
    rev               bigint NOT NULL REFERENCES revinfo (rev),
    revtype           smallint,
    revend            bigint REFERENCES revinfo (rev),
    name              varchar(200),
    sector            varchar(100),
    city              varchar(100),
    website           varchar(300),
    notes             text,
    owner_id          uuid,
    status            varchar(40),
    status_changed_at timestamptz,
    opened_on         date,
    created_at        timestamptz,
    created_by        uuid,
    updated_at        timestamptz,
    updated_by        uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_account_aud_rev ON account_aud (rev);
CREATE INDEX idx_account_aud_revend ON account_aud (revend);

CREATE TABLE person_aud (
    id            uuid   NOT NULL,
    rev           bigint NOT NULL REFERENCES revinfo (rev),
    revtype       smallint,
    revend        bigint REFERENCES revinfo (rev),
    name          varchar(200),
    type          varchar(40),
    title         varchar(200),
    company       varchar(200),
    email         varchar(254),
    phone         varchar(50),
    city          varchar(100),
    linkedin_url  varchar(300),
    how_we_know   text,
    notes         text,
    owner_id      uuid,
    anonymised_at timestamptz,
    created_at    timestamptz,
    created_by    uuid,
    updated_at    timestamptz,
    updated_by    uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_person_aud_rev ON person_aud (rev);
CREATE INDEX idx_person_aud_revend ON person_aud (revend);

CREATE TABLE person_employment_aud (
    id         uuid   NOT NULL,
    rev        bigint NOT NULL REFERENCES revinfo (rev),
    revtype    smallint,
    revend     bigint REFERENCES revinfo (rev),
    person_id  uuid,
    company    varchar(200),
    title      varchar(200),
    start_date date,
    end_date   date,
    sort_order int,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_person_employment_aud_person ON person_employment_aud (person_id);
CREATE INDEX idx_person_employment_aud_rev ON person_employment_aud (rev);
CREATE INDEX idx_person_employment_aud_revend ON person_employment_aud (revend);

CREATE TABLE contact_aud (
    id         uuid   NOT NULL,
    rev        bigint NOT NULL REFERENCES revinfo (rev),
    revtype    smallint,
    revend     bigint REFERENCES revinfo (rev),
    account_id uuid,
    person_id  uuid,
    role       varchar(200),
    is_primary boolean,
    ended_on   date,
    created_at timestamptz,
    created_by uuid,
    updated_at timestamptz,
    updated_by uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_contact_aud_person ON contact_aud (person_id);
CREATE INDEX idx_contact_aud_account ON contact_aud (account_id);
CREATE INDEX idx_contact_aud_rev ON contact_aud (rev);
CREATE INDEX idx_contact_aud_revend ON contact_aud (revend);

CREATE TABLE network_link_aud (
    id             uuid   NOT NULL,
    rev            bigint NOT NULL REFERENCES revinfo (rev),
    revtype        smallint,
    revend         bigint REFERENCES revinfo (rev),
    from_person_id uuid,
    to_person_id   uuid,
    type           varchar(40),
    note           text,
    created_at     timestamptz,
    created_by     uuid,
    updated_at     timestamptz,
    updated_by     uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_network_link_aud_from ON network_link_aud (from_person_id);
CREATE INDEX idx_network_link_aud_to ON network_link_aud (to_person_id);
CREATE INDEX idx_network_link_aud_rev ON network_link_aud (rev);
CREATE INDEX idx_network_link_aud_revend ON network_link_aud (revend);
