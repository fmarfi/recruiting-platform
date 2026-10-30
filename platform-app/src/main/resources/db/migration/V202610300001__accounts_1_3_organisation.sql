-- Contract 1.3.0 (R63, ADR 0013 item 11): account is the organisation table. Owner: accounts.
-- Archive columns, group tree, billing identity, role flags, aliases and the generated normalized_name.

-- Name normalisation (schema.md "Organisation identity"). Both functions are IMMUTABLE so they can back a
-- generated column and expression indexes.
CREATE OR REPLACE FUNCTION normalize_org_name(x text)
    RETURNS text
    LANGUAGE sql
    IMMUTABLE
    PARALLEL SAFE
    STRICT
AS $$
SELECT coalesce(
        nullif(trim(regexp_replace(
                regexp_replace(
                        regexp_replace(t.s2, '\m(sanayi|san) ve (ticaret|tic)\M', ' ', 'g'),
                        '\m(a s|as|ltd|sti|limited|sirketi|anonim|sanayi|ticaret|san|tic|inc|llc|gmbh|plc|bv|nv|ag|co|corp)\M',
                        ' ', 'g'),
                '\s+', ' ', 'g')), ''),
        trim(t.s2))
FROM (SELECT regexp_replace(lower(public.immutable_unaccent(translate(x, U&'\0131', 'i'))), '[^a-z0-9]+', ' ', 'g') AS s2) t
$$;

CREATE OR REPLACE FUNCTION normalize_org_names(x text[])
    RETURNS text[]
    LANGUAGE sql
    IMMUTABLE
    PARALLEL SAFE
    STRICT
AS $$ SELECT coalesce(array_agg(public.normalize_org_name(e)), '{}'::text[]) FROM unnest(x) AS e $$;

ALTER TABLE account
    ADD COLUMN archived_at           timestamptz,
    ADD COLUMN archived_by           uuid REFERENCES app_user (id),
    ADD COLUMN archive_reason        varchar(40)
        CONSTRAINT ck_account_archive_reason CHECK (archive_reason IN
            ('DUPLICATE', 'CREATED_BY_MISTAKE', 'NO_LONGER_RELEVANT', 'MERGED', 'IMPORT_ROLLBACK', 'OTHER')),
    ADD COLUMN archive_note          varchar(500),
    ADD COLUMN merged_into_id        uuid REFERENCES account (id),
    ADD COLUMN legal_name            varchar(300),
    ADD COLUMN tax_id                varchar(20),
    ADD COLUMN tax_office            varchar(100),
    ADD COLUMN mersis_no             varchar(16),
    ADD COLUMN einvoice_status       varchar(40)  NOT NULL DEFAULT 'UNKNOWN'
        CONSTRAINT ck_account_einvoice_status CHECK (einvoice_status IN ('UNKNOWN', 'E_FATURA', 'E_ARSIV')),
    ADD COLUMN einvoice_alias        varchar(100),
    ADD COLUMN billing_address       varchar(500),
    ADD COLUMN billing_emails        varchar(254)[] NOT NULL DEFAULT '{}'
        CONSTRAINT ck_account_billing_emails_max CHECK (cardinality(billing_emails) <= 5),
    ADD COLUMN billing_currency      char(3),
    ADD COLUMN vat_exempt            boolean      NOT NULL DEFAULT false,
    ADD COLUMN withholding_applies   boolean      NOT NULL DEFAULT false,
    ADD COLUMN parent_account_id     uuid REFERENCES account (id),
    ADD COLUMN group_name            varchar(200),
    ADD COLUMN employee_band         varchar(40)
        CONSTRAINT ck_account_employee_band CHECK (employee_band IN
            ('B1_50', 'B51_250', 'B251_1000', 'B1001_5000', 'B5000_PLUS')),
    ADD COLUMN hq_country            char(2),
    ADD COLUMN confidentiality_level varchar(40)  NOT NULL DEFAULT 'NORMAL'
        CONSTRAINT ck_account_confidentiality CHECK (confidentiality_level IN ('NORMAL', 'RESTRICTED', 'CONFIDENTIAL')),
    ADD COLUMN preferred_language    varchar(2)
        CONSTRAINT ck_account_preferred_language CHECK (preferred_language IN ('TR', 'EN')),
    ADD COLUMN is_client             boolean      NOT NULL DEFAULT false,
    ADD COLUMN is_prospect           boolean      NOT NULL DEFAULT false,
    ADD COLUMN is_target             boolean      NOT NULL DEFAULT false,
    ADD COLUMN is_employer           boolean      NOT NULL DEFAULT false,
    ADD COLUMN aliases               text[]       NOT NULL DEFAULT '{}'
        CONSTRAINT ck_account_aliases_max CHECK (cardinality(aliases) <= 20),
    ADD CONSTRAINT ck_account_archive_pair CHECK ((archived_at IS NULL) = (archive_reason IS NULL)),
    ADD CONSTRAINT ck_account_archive_by CHECK ((archived_at IS NULL) = (archived_by IS NULL)),
    ADD CONSTRAINT ck_account_merged_into CHECK ((merged_into_id IS NULL) = (archive_reason IS DISTINCT FROM 'MERGED')),
    ADD CONSTRAINT ck_account_parent_not_self CHECK (parent_account_id <> id);

-- R63 backfill. is_target is set by the pipeline when target_company rows exist (the table is new, so none yet).
UPDATE account a
SET is_client   = (a.status IN ('ACTIVE', 'ON_HOLD', 'CLOSED')
                   OR EXISTS (SELECT 1 FROM job_order j WHERE j.account_id = a.id)),
    is_prospect = a.status IN ('LEAD', 'PROPOSAL_SENT');
UPDATE account SET is_prospect = true WHERE NOT (is_client OR is_prospect OR is_target OR is_employer);

ALTER TABLE account
    ADD CONSTRAINT ck_account_role CHECK (is_client OR is_prospect OR is_target OR is_employer),
    ALTER COLUMN sector DROP NOT NULL,
    ADD CONSTRAINT ck_account_sector CHECK (sector IS NOT NULL OR NOT (is_client OR is_prospect));

-- Generated, read-only, not audited. Uniqueness moves here from lower(name).
ALTER TABLE account
    ADD COLUMN normalized_name varchar(200) GENERATED ALWAYS AS (normalize_org_name(name)) STORED;

DO $$
DECLARE
    clash text;
BEGIN
    SELECT string_agg(ids, '; ') INTO clash FROM (
        SELECT normalized_name, string_agg(id::text, ', ') AS ids
        FROM account WHERE archived_at IS NULL
        GROUP BY normalized_name HAVING count(*) > 1) c;
    IF clash IS NOT NULL THEN
        RAISE EXCEPTION 'Organisation names clash after normalisation (rename one): %', clash;
    END IF;
END $$;

DROP INDEX uq_account_name;
CREATE UNIQUE INDEX uq_account_normalized_name ON account (normalized_name) WHERE archived_at IS NULL;
CREATE INDEX idx_account_normalized_name ON account (normalized_name);
CREATE INDEX idx_account_aliases_norm ON account USING gin (normalize_org_names(aliases));
CREATE INDEX idx_account_archived ON account (archived_at) WHERE archived_at IS NOT NULL;
CREATE INDEX idx_account_merged_into ON account (merged_into_id) WHERE merged_into_id IS NOT NULL;
CREATE INDEX idx_account_parent ON account (parent_account_id) WHERE parent_account_id IS NOT NULL;
CREATE INDEX idx_account_tax_id ON account (tax_id) WHERE tax_id IS NOT NULL;
CREATE INDEX idx_account_is_client ON account (id) WHERE is_client;
CREATE INDEX idx_account_is_target ON account (id) WHERE is_target;
CREATE INDEX idx_account_is_employer ON account (id) WHERE is_employer;
CREATE INDEX idx_account_archived_by ON account (archived_by) WHERE archived_by IS NOT NULL;

-- Audit table: every audited column, but not normalized_name.
ALTER TABLE account_aud
    ADD COLUMN archived_at           timestamptz,
    ADD COLUMN archived_by           uuid,
    ADD COLUMN archive_reason        varchar(40),
    ADD COLUMN archive_note          varchar(500),
    ADD COLUMN merged_into_id        uuid,
    ADD COLUMN legal_name            varchar(300),
    ADD COLUMN tax_id                varchar(20),
    ADD COLUMN tax_office            varchar(100),
    ADD COLUMN mersis_no             varchar(16),
    ADD COLUMN einvoice_status       varchar(40),
    ADD COLUMN einvoice_alias        varchar(100),
    ADD COLUMN billing_address       varchar(500),
    ADD COLUMN billing_emails        varchar(254)[],
    ADD COLUMN billing_currency      char(3),
    ADD COLUMN vat_exempt            boolean,
    ADD COLUMN withholding_applies   boolean,
    ADD COLUMN parent_account_id     uuid,
    ADD COLUMN group_name            varchar(200),
    ADD COLUMN employee_band         varchar(40),
    ADD COLUMN hq_country            char(2),
    ADD COLUMN confidentiality_level varchar(40),
    ADD COLUMN preferred_language    varchar(2),
    ADD COLUMN is_client             boolean,
    ADD COLUMN is_prospect           boolean,
    ADD COLUMN is_target             boolean,
    ADD COLUMN is_employer           boolean,
    ADD COLUMN aliases               text[];

-- Employment links to an organisation (null = free text only). Accounts are never deleted: no action.
ALTER TABLE person_employment ADD COLUMN organisation_id uuid REFERENCES account (id);
CREATE INDEX idx_person_employment_organisation ON person_employment (organisation_id)
    WHERE organisation_id IS NOT NULL;
ALTER TABLE person_employment_aud ADD COLUMN organisation_id uuid;

-- Off-limits rule also covers subsidiaries.
ALTER TABLE off_limits_rule
    ADD COLUMN include_subsidiaries boolean NOT NULL DEFAULT false,
    ADD CONSTRAINT ck_off_limits_subsidiaries CHECK (NOT include_subsidiaries OR account_id IS NOT NULL);
ALTER TABLE off_limits_rule_aud ADD COLUMN include_subsidiaries boolean;
