-- Contract 1.3.0: agreements, fee schedule, placements, conflict checks (PB-007..021, CO-020/021, ADR 0013).
-- Owner: pipeline. FK rule (ADR 0009): signatory_contact_id, placement.person_id and placement.application_id are
-- set null (commercial evidence outlives the person); accounts are never deleted (no action).

CREATE TABLE agreement (
    id                        uuid PRIMARY KEY,
    account_id                uuid          NOT NULL REFERENCES account (id),
    job_order_id              uuid REFERENCES job_order (id),
    kind                      varchar(40)   NOT NULL CHECK (kind IN ('TERMS_OF_BUSINESS', 'ENGAGEMENT_LETTER', 'AMENDMENT')),
    status                    varchar(40)   NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'SENT', 'SIGNED', 'EXPIRED', 'TERMINATED')),
    title                     varchar(200),
    signed_on                 date,
    valid_from                date,
    valid_until               date,
    language                  varchar(2)    CHECK (language IN ('TR', 'EN')),
    governing_law             varchar(100),
    exclusivity               varchar(40)   CHECK (exclusivity IN ('EXCLUSIVE', 'NON_EXCLUSIVE')),
    fee_model                 varchar(40)   NOT NULL CHECK (fee_model IN
        ('RETAINED_THIRDS', 'RETAINED_CUSTOM', 'PERCENT_FIRST_YEAR_CASH', 'FLAT_FEE', 'HYBRID')),
    fee_percent               numeric(5, 2) CONSTRAINT ck_agreement_fee_percent CHECK (fee_percent BETWEEN 0 AND 100),
    fee_basis                 varchar(40)[] NOT NULL DEFAULT '{}'
        CONSTRAINT ck_agreement_fee_basis CHECK (fee_basis <@ ARRAY
            ['BASE', 'TARGET_BONUS', 'PAID_BONUS', 'ALLOWANCES', 'SIGN_ON', 'CAR', 'OTHER_CASH']::varchar[]),
    flat_fee_amount           numeric(14, 2),
    estimated_fee             numeric(14, 2),
    fee_minimum               numeric(14, 2),
    fee_cap                   numeric(14, 2),
    currency                  char(3)       NOT NULL,
    payment_terms_days        smallint      NOT NULL DEFAULT 30
        CONSTRAINT ck_agreement_payment_terms CHECK (payment_terms_days BETWEEN 0 AND 180),
    guarantee_months          smallint      NOT NULL DEFAULT 6
        CONSTRAINT ck_agreement_guarantee_months CHECK (guarantee_months BETWEEN 0 AND 36),
    guarantee_start_basis     varchar(40)   NOT NULL DEFAULT 'START_DATE'
        CHECK (guarantee_start_basis IN ('START_DATE', 'OFFER_ACCEPTED')),
    signed_file_id            uuid,
    signed_file_name          varchar(255),
    signed_file_size          bigint,
    signed_file_uploaded_at   timestamptz,
    signed_file_uploaded_by   uuid REFERENCES app_user (id),
    guarantee_terms           varchar(2000),
    confidentiality_terms     varchar(2000),
    expense_policy            varchar(1000),
    off_limits_months         smallint
        CONSTRAINT ck_agreement_off_limits_months CHECK (off_limits_months BETWEEN 0 AND 60),
    signatory_contact_id      uuid REFERENCES contact (id) ON DELETE SET NULL,
    billing_account_id        uuid          NOT NULL REFERENCES account (id),
    notes                     text,
    created_at                timestamptz   NOT NULL DEFAULT now(),
    created_by                uuid REFERENCES app_user (id),
    updated_at                timestamptz   NOT NULL DEFAULT now(),
    updated_by                uuid REFERENCES app_user (id),
    version                   bigint        NOT NULL DEFAULT 0,
    CONSTRAINT ck_agreement_kind_job CHECK
        (((kind = 'ENGAGEMENT_LETTER') = (job_order_id IS NOT NULL)) OR kind = 'AMENDMENT'),
    CONSTRAINT ck_agreement_signed CHECK (status <> 'SIGNED' OR signed_on IS NOT NULL),
    CONSTRAINT ck_agreement_validity CHECK (valid_until IS NULL OR valid_from IS NULL OR valid_until >= valid_from),
    CONSTRAINT ck_agreement_fee_range CHECK (fee_minimum IS NULL OR fee_cap IS NULL OR fee_minimum <= fee_cap),
    CONSTRAINT ck_agreement_signed_file_name CHECK ((signed_file_id IS NULL) = (signed_file_name IS NULL)),
    CONSTRAINT ck_agreement_signed_file_size CHECK
        (signed_file_size IS NULL OR signed_file_size BETWEEN 1 AND 20971520)
);
CREATE INDEX idx_agreement_account ON agreement (account_id);
CREATE INDEX idx_agreement_job_order ON agreement (job_order_id) WHERE job_order_id IS NOT NULL;
CREATE INDEX idx_agreement_signatory ON agreement (signatory_contact_id) WHERE signatory_contact_id IS NOT NULL;
CREATE INDEX idx_agreement_billing_account ON agreement (billing_account_id);
CREATE INDEX idx_agreement_signed_file_uploaded_by ON agreement (signed_file_uploaded_by)
    WHERE signed_file_uploaded_by IS NOT NULL;
CREATE TABLE agreement_aud (
    id                      uuid   NOT NULL,
    rev                     bigint NOT NULL REFERENCES revinfo (rev),
    revtype                 smallint,
    revend                  bigint REFERENCES revinfo (rev),
    account_id              uuid,
    job_order_id            uuid,
    kind                    varchar(40),
    status                  varchar(40),
    title                   varchar(200),
    signed_on               date,
    valid_from              date,
    valid_until             date,
    language                varchar(2),
    governing_law           varchar(100),
    exclusivity             varchar(40),
    fee_model               varchar(40),
    fee_percent             numeric(5, 2),
    fee_basis               varchar(40)[],
    flat_fee_amount         numeric(14, 2),
    estimated_fee           numeric(14, 2),
    fee_minimum             numeric(14, 2),
    fee_cap                 numeric(14, 2),
    currency                char(3),
    payment_terms_days      smallint,
    guarantee_months        smallint,
    guarantee_start_basis   varchar(40),
    signed_file_id          uuid,
    signed_file_name        varchar(255),
    signed_file_size        bigint,
    signed_file_uploaded_at timestamptz,
    signed_file_uploaded_by uuid,
    guarantee_terms         varchar(2000),
    confidentiality_terms   varchar(2000),
    expense_policy          varchar(1000),
    off_limits_months       smallint,
    signatory_contact_id    uuid,
    billing_account_id      uuid,
    notes                   text,
    created_at              timestamptz,
    created_by              uuid,
    updated_at              timestamptz,
    updated_by              uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_agreement_aud_rev ON agreement_aud (rev);
CREATE INDEX idx_agreement_aud_revend ON agreement_aud (revend);

CREATE TABLE fee_schedule_item (
    id              uuid PRIMARY KEY,
    agreement_id    uuid          NOT NULL REFERENCES agreement (id) ON DELETE CASCADE,
    seq             smallint      NOT NULL,
    label           varchar(200)  NOT NULL,
    trigger         varchar(40)   NOT NULL CHECK (trigger IN
        ('SIGNING', 'DATE_OFFSET', 'SHORTLIST_DELIVERED', 'OFFER_ACCEPTED', 'START_DATE', 'EXPENSES', 'ABORT')),
    offset_days     smallint,
    percent         numeric(5, 2),
    amount          numeric(14, 2),
    due_days        smallint      NOT NULL DEFAULT 30,
    vat_applies     boolean       NOT NULL DEFAULT true,
    invoice_state   varchar(40)   NOT NULL DEFAULT 'NOT_DUE'
        CHECK (invoice_state IN ('NOT_DUE', 'DUE', 'INVOICED', 'PAID', 'CANCELLED')),
    triggered_at    timestamptz,
    due_on          date,
    einvoice_ref    varchar(100),
    invoiced_on     date,
    paid_on         date,
    invoiced_amount numeric(14, 2),
    state_reason    varchar(500),
    created_at      timestamptz   NOT NULL DEFAULT now(),
    created_by      uuid REFERENCES app_user (id),
    updated_at      timestamptz   NOT NULL DEFAULT now(),
    updated_by      uuid REFERENCES app_user (id),
    version         bigint        NOT NULL DEFAULT 0,
    CONSTRAINT uq_fee_schedule_item_seq UNIQUE (agreement_id, seq),
    CONSTRAINT ck_fee_schedule_item_offset CHECK (trigger <> 'DATE_OFFSET' OR offset_days IS NOT NULL),
    CONSTRAINT ck_fee_schedule_item_amount CHECK ((percent IS NULL) <> (amount IS NULL))
);
CREATE INDEX idx_fee_schedule_item_state ON fee_schedule_item (invoice_state, due_on);
CREATE TABLE fee_schedule_item_aud (
    id              uuid   NOT NULL,
    rev             bigint NOT NULL REFERENCES revinfo (rev),
    revtype         smallint,
    revend          bigint REFERENCES revinfo (rev),
    agreement_id    uuid,
    seq             smallint,
    label           varchar(200),
    trigger         varchar(40),
    offset_days     smallint,
    percent         numeric(5, 2),
    amount          numeric(14, 2),
    due_days        smallint,
    vat_applies     boolean,
    invoice_state   varchar(40),
    triggered_at    timestamptz,
    due_on          date,
    einvoice_ref    varchar(100),
    invoiced_on     date,
    paid_on         date,
    invoiced_amount numeric(14, 2),
    state_reason    varchar(500),
    created_at      timestamptz,
    created_by      uuid,
    updated_at      timestamptz,
    updated_by      uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_fee_schedule_item_aud_rev ON fee_schedule_item_aud (rev);
CREATE INDEX idx_fee_schedule_item_aud_revend ON fee_schedule_item_aud (revend);

CREATE TABLE placement (
    id                       uuid PRIMARY KEY,
    application_id           uuid REFERENCES application (id) ON DELETE SET NULL,
    job_order_id             uuid          NOT NULL REFERENCES job_order (id),
    account_id               uuid          NOT NULL REFERENCES account (id),
    person_id                uuid REFERENCES person (id) ON DELETE SET NULL,
    agreement_id             uuid REFERENCES agreement (id),
    status                   varchar(40)   NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'LEFT_IN_GUARANTEE', 'GUARANTEE_ENDED', 'REPLACED', 'CANCELLED')),
    offer_accepted_on        date,
    start_date               date          NOT NULL,
    compensation_enc         bytea,
    fee_amount               numeric(14, 2) NOT NULL,
    fee_currency             char(3)       NOT NULL,
    fee_overridden           boolean       NOT NULL DEFAULT false,
    guarantee_end_on         date          NOT NULL,
    left_on                  date,
    replacement_job_order_id uuid REFERENCES job_order (id),
    status_reason            varchar(500),
    created_at               timestamptz   NOT NULL DEFAULT now(),
    created_by               uuid REFERENCES app_user (id),
    updated_at               timestamptz   NOT NULL DEFAULT now(),
    updated_by               uuid REFERENCES app_user (id),
    version                  bigint        NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_placement_application ON placement (application_id) WHERE application_id IS NOT NULL;
CREATE INDEX idx_placement_job_order ON placement (job_order_id);
CREATE INDEX idx_placement_account ON placement (account_id);
CREATE INDEX idx_placement_person ON placement (person_id) WHERE person_id IS NOT NULL;
CREATE INDEX idx_placement_agreement ON placement (agreement_id) WHERE agreement_id IS NOT NULL;
CREATE INDEX idx_placement_replacement_job ON placement (replacement_job_order_id)
    WHERE replacement_job_order_id IS NOT NULL;
CREATE INDEX idx_placement_guarantee_end ON placement (guarantee_end_on);
CREATE TABLE placement_aud (
    id                       uuid   NOT NULL,
    rev                      bigint NOT NULL REFERENCES revinfo (rev),
    revtype                  smallint,
    revend                   bigint REFERENCES revinfo (rev),
    application_id           uuid,
    job_order_id             uuid,
    account_id               uuid,
    person_id                uuid,
    agreement_id             uuid,
    status                   varchar(40),
    offer_accepted_on        date,
    start_date               date,
    compensation_enc         bytea,
    fee_amount               numeric(14, 2),
    fee_currency             char(3),
    fee_overridden           boolean,
    guarantee_end_on         date,
    left_on                  date,
    replacement_job_order_id uuid,
    status_reason            varchar(500),
    created_at               timestamptz,
    created_by               uuid,
    updated_at               timestamptz,
    updated_by               uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_placement_aud_person ON placement_aud (person_id);
CREATE INDEX idx_placement_aud_rev ON placement_aud (rev);
CREATE INDEX idx_placement_aud_revend ON placement_aud (revend);

-- Append-only log of conflict checks; not audited, no names in hits.
CREATE TABLE conflict_check (
    id              uuid PRIMARY KEY,
    account_id      uuid REFERENCES account (id),
    company_name    varchar(200),
    job_order_id    uuid REFERENCES job_order (id),
    hits            jsonb        NOT NULL DEFAULT '[]',
    decision        varchar(40)  NOT NULL CHECK (decision IN ('CLEAR', 'PROCEED', 'BLOCKED')),
    override_reason varchar(500),
    checked_by      uuid         NOT NULL REFERENCES app_user (id),
    checked_at      timestamptz  NOT NULL,
    CONSTRAINT ck_conflict_check_target CHECK ((account_id IS NULL) <> (company_name IS NULL))
);
CREATE INDEX idx_conflict_check_account ON conflict_check (account_id, checked_at DESC)
    WHERE account_id IS NOT NULL;
CREATE INDEX idx_conflict_check_job ON conflict_check (job_order_id) WHERE job_order_id IS NOT NULL;
CREATE INDEX idx_conflict_check_checked_by ON conflict_check (checked_by);
