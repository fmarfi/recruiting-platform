-- Contract 1.3.0: search brief (comp band encrypted, R73), criteria, search team, target companies. Owner: pipeline.

CREATE TABLE search_brief (
    job_order_id     uuid PRIMARY KEY REFERENCES job_order (id),
    reporting_line   varchar(200),
    purpose          text,
    ideal_profile    text,
    notes            text,
    seniority_level  varchar(40)
        CONSTRAINT ck_search_brief_seniority CHECK (seniority_level IN
            ('BOARD', 'C_LEVEL', 'VP_DIRECTOR', 'HEAD', 'MANAGER', 'SPECIALIST')),
    function_area    varchar(40)
        CONSTRAINT ck_search_brief_function CHECK (function_area IN
            ('GENERAL_MANAGEMENT', 'FINANCE', 'HR', 'SALES', 'MARKETING', 'OPERATIONS', 'SUPPLY_CHAIN', 'PROCUREMENT',
             'ENGINEERING', 'IT', 'RND', 'LEGAL', 'OTHER')),
    -- AES-256-GCM ciphertext of the BriefCompensation JSON (security.FieldCipher), like person.compensation_enc.
    comp_band_enc    bytea,
    team_size        smallint
        CONSTRAINT ck_search_brief_team_size CHECK (team_size BETWEEN 0 AND 100000),
    languages        jsonb       NOT NULL DEFAULT '[]'
        CONSTRAINT ck_search_brief_languages CHECK (jsonb_typeof(languages) = 'array'),
    kickoff_on       date,
    longlist_due_on  date,
    shortlist_due_on date,
    target_start_on  date,
    brief_version    int         NOT NULL DEFAULT 1,
    created_at       timestamptz NOT NULL DEFAULT now(),
    created_by       uuid REFERENCES app_user (id),
    updated_at       timestamptz NOT NULL DEFAULT now(),
    updated_by       uuid REFERENCES app_user (id),
    version          bigint      NOT NULL DEFAULT 0
);
CREATE TABLE search_brief_aud (
    job_order_id     uuid   NOT NULL,
    rev              bigint NOT NULL REFERENCES revinfo (rev),
    revtype          smallint,
    revend           bigint REFERENCES revinfo (rev),
    reporting_line   varchar(200),
    purpose          text,
    ideal_profile    text,
    notes            text,
    seniority_level  varchar(40),
    function_area    varchar(40),
    comp_band_enc    bytea,
    team_size        smallint,
    languages        jsonb,
    kickoff_on       date,
    longlist_due_on  date,
    shortlist_due_on date,
    target_start_on  date,
    brief_version    int,
    created_at       timestamptz,
    created_by       uuid,
    updated_at       timestamptz,
    updated_by       uuid,
    PRIMARY KEY (job_order_id, rev)
);
CREATE INDEX idx_search_brief_aud_rev ON search_brief_aud (rev);
CREATE INDEX idx_search_brief_aud_revend ON search_brief_aud (revend);

CREATE TABLE search_criterion (
    id           uuid PRIMARY KEY,
    job_order_id uuid         NOT NULL REFERENCES job_order (id),
    kind         varchar(40)  NOT NULL CHECK (kind IN ('MUST_HAVE', 'NICE_TO_HAVE')),
    text         varchar(300) NOT NULL,
    sort_order   int          NOT NULL,
    removed_at   timestamptz,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    created_by   uuid REFERENCES app_user (id),
    updated_at   timestamptz  NOT NULL DEFAULT now(),
    updated_by   uuid REFERENCES app_user (id),
    version      bigint       NOT NULL DEFAULT 0
);
CREATE INDEX idx_search_criterion_job ON search_criterion (job_order_id, kind, sort_order);
CREATE TABLE search_criterion_aud (
    id           uuid   NOT NULL,
    rev          bigint NOT NULL REFERENCES revinfo (rev),
    revtype      smallint,
    revend       bigint REFERENCES revinfo (rev),
    job_order_id uuid,
    kind         varchar(40),
    text         varchar(300),
    sort_order   int,
    removed_at   timestamptz,
    created_at   timestamptz,
    created_by   uuid,
    updated_at   timestamptz,
    updated_by   uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_search_criterion_aud_rev ON search_criterion_aud (rev);
CREATE INDEX idx_search_criterion_aud_revend ON search_criterion_aud (revend);

CREATE TABLE job_order_team_member (
    job_order_id uuid        NOT NULL REFERENCES job_order (id),
    user_id      uuid        NOT NULL REFERENCES app_user (id),
    role         varchar(40) NOT NULL CHECK (role IN ('LEAD', 'CONSULTANT', 'RESEARCHER')),
    added_at     timestamptz NOT NULL,
    PRIMARY KEY (job_order_id, user_id)
);
CREATE INDEX idx_job_order_team_member_user ON job_order_team_member (user_id);
CREATE TABLE job_order_team_member_aud (
    job_order_id uuid   NOT NULL,
    user_id      uuid   NOT NULL,
    rev          bigint NOT NULL REFERENCES revinfo (rev),
    revtype      smallint,
    revend       bigint REFERENCES revinfo (rev),
    role         varchar(40),
    added_at     timestamptz,
    PRIMARY KEY (job_order_id, user_id, rev)
);
CREATE INDEX idx_job_order_team_member_aud_rev ON job_order_team_member_aud (rev);
CREATE INDEX idx_job_order_team_member_aud_revend ON job_order_team_member_aud (revend);

CREATE TABLE target_company (
    id                         uuid PRIMARY KEY,
    job_order_id               uuid         NOT NULL REFERENCES job_order (id),
    account_id                 uuid REFERENCES account (id),
    company_name               varchar(200),
    sector                     varchar(100),
    tier                       varchar(40)  NOT NULL CHECK (tier IN ('A', 'B', 'C')),
    status                     varchar(40)  NOT NULL DEFAULT 'TO_MAP'
        CHECK (status IN ('TO_MAP', 'MAPPED', 'APPROACHED', 'CLOSED', 'EXCLUDED')),
    rationale                  varchar(1000),
    source                     varchar(200),
    notes                      varchar(2000),
    owner_id                   uuid         NOT NULL REFERENCES app_user (id),
    off_limits_override_reason varchar(500),
    created_at                 timestamptz  NOT NULL DEFAULT now(),
    created_by                 uuid REFERENCES app_user (id),
    updated_at                 timestamptz  NOT NULL DEFAULT now(),
    updated_by                 uuid REFERENCES app_user (id),
    version                    bigint       NOT NULL DEFAULT 0,
    CONSTRAINT ck_target_company_target CHECK ((account_id IS NULL) <> (company_name IS NULL))
);
CREATE INDEX idx_target_company_job ON target_company (job_order_id);
CREATE INDEX idx_target_company_owner ON target_company (owner_id);
CREATE INDEX idx_target_company_account ON target_company (account_id) WHERE account_id IS NOT NULL;
CREATE UNIQUE INDEX uq_target_company_account ON target_company (job_order_id, account_id)
    WHERE account_id IS NOT NULL;
CREATE UNIQUE INDEX uq_target_company_name ON target_company (job_order_id, immutable_unaccent(lower(trim(company_name))))
    WHERE company_name IS NOT NULL;
CREATE TABLE target_company_aud (
    id                         uuid   NOT NULL,
    rev                        bigint NOT NULL REFERENCES revinfo (rev),
    revtype                    smallint,
    revend                     bigint REFERENCES revinfo (rev),
    job_order_id               uuid,
    account_id                 uuid,
    company_name               varchar(200),
    sector                     varchar(100),
    tier                       varchar(40),
    status                     varchar(40),
    rationale                  varchar(1000),
    source                     varchar(200),
    notes                      varchar(2000),
    owner_id                   uuid,
    off_limits_override_reason varchar(500),
    created_at                 timestamptz,
    created_by                 uuid,
    updated_at                 timestamptz,
    updated_by                 uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_target_company_aud_rev ON target_company_aud (rev);
CREATE INDEX idx_target_company_aud_revend ON target_company_aud (revend);

-- R63 backfill that needed the table above: organisations that are already targets.
UPDATE account SET is_target = true WHERE id IN (SELECT account_id FROM target_company WHERE account_id IS NOT NULL);
