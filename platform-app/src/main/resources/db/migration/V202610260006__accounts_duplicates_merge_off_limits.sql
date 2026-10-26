-- Contract 1.2.0 (ADR 0011): duplicate_candidate, person_merge, off_limits_rule. Owner: accounts.
CREATE TABLE duplicate_candidate (
    id          uuid PRIMARY KEY,
    person_a_id uuid          NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    person_b_id uuid          NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    reasons     varchar(40)[] NOT NULL
        CONSTRAINT ck_duplicate_reasons CHECK (cardinality(reasons) >= 1
            AND reasons <@ ARRAY['EMAIL', 'PHONE', 'LINKEDIN_URL', 'NAME_COMPANY']::varchar[]),
    status      varchar(40)   NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'MERGED', 'DISMISSED')),
    detected_at timestamptz   NOT NULL,
    resolved_at timestamptz,
    resolved_by uuid REFERENCES app_user (id),
    CONSTRAINT ck_duplicate_order CHECK (person_a_id < person_b_id),
    CONSTRAINT uq_duplicate_pair UNIQUE (person_a_id, person_b_id)
);
CREATE INDEX idx_duplicate_person_b ON duplicate_candidate (person_b_id);
CREATE INDEX idx_duplicate_status ON duplicate_candidate (status, detected_at DESC);
CREATE INDEX idx_duplicate_resolved_by ON duplicate_candidate (resolved_by) WHERE resolved_by IS NOT NULL;

-- merged_id has no FK: that person row was deleted. Ids only. The cascade from the survivor stays (FK rule).
CREATE TABLE person_merge (
    id           uuid PRIMARY KEY,
    survivor_id  uuid          NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    merged_id    uuid          NOT NULL,
    fields_taken varchar(40)[] NOT NULL,
    merged_at    timestamptz   NOT NULL,
    merged_by    uuid          NOT NULL REFERENCES app_user (id),
    CONSTRAINT uq_person_merge_merged UNIQUE (merged_id)
);
CREATE INDEX idx_person_merge_survivor ON person_merge (survivor_id);
CREATE INDEX idx_person_merge_merged_by ON person_merge (merged_by);

CREATE TABLE off_limits_rule (
    id           uuid PRIMARY KEY,
    account_id   uuid REFERENCES account (id),
    company_name varchar(200),
    reason       varchar(40)  NOT NULL
        CHECK (reason IN ('CLIENT_AGREEMENT', 'GUARANTEE_PERIOD', 'CLIENT_REQUEST', 'OTHER')),
    note         varchar(500),
    starts_on    date         NOT NULL DEFAULT current_date,
    ends_on      date,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    created_by   uuid REFERENCES app_user (id),
    updated_at   timestamptz  NOT NULL DEFAULT now(),
    updated_by   uuid REFERENCES app_user (id),
    version      bigint       NOT NULL DEFAULT 0,
    CONSTRAINT ck_off_limits_target CHECK ((account_id IS NULL) <> (company_name IS NULL)),
    CONSTRAINT ck_off_limits_dates CHECK (ends_on IS NULL OR ends_on >= starts_on)
);
CREATE INDEX idx_off_limits_account ON off_limits_rule (account_id) WHERE account_id IS NOT NULL;
CREATE INDEX idx_off_limits_company ON off_limits_rule (immutable_unaccent(lower(trim(company_name))))
    WHERE company_name IS NOT NULL;
-- One open rule per target (the service also rejects overlapping rules).
CREATE UNIQUE INDEX uq_off_limits_open_account ON off_limits_rule (account_id)
    WHERE ends_on IS NULL AND account_id IS NOT NULL;
CREATE UNIQUE INDEX uq_off_limits_open_company ON off_limits_rule (immutable_unaccent(lower(trim(company_name))))
    WHERE ends_on IS NULL AND company_name IS NOT NULL;

CREATE TABLE off_limits_rule_aud (
    id           uuid   NOT NULL,
    rev          bigint NOT NULL REFERENCES revinfo (rev),
    revtype      smallint,
    revend       bigint REFERENCES revinfo (rev),
    account_id   uuid,
    company_name varchar(200),
    reason       varchar(40),
    note         varchar(500),
    starts_on    date,
    ends_on      date,
    created_at   timestamptz,
    created_by   uuid,
    updated_at   timestamptz,
    updated_by   uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_off_limits_rule_aud_rev ON off_limits_rule_aud (rev);
CREATE INDEX idx_off_limits_rule_aud_revend ON off_limits_rule_aud (revend);
