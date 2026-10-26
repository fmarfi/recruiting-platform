-- Contract 1.2.0 (ADR 0011): non-AI candidate profile on person, plus person_education. Owner: accounts.
ALTER TABLE person
    ADD COLUMN source_channel          varchar(40)
        CONSTRAINT ck_person_source_channel CHECK (source_channel IN
            ('LINKEDIN_AD', 'LINKEDIN_DIRECT', 'WEBSITE', 'REFERRAL', 'EXISTING_POOL', 'EMAIL', 'CV_UPLOAD')),
    ADD COLUMN notice_period_days      smallint
        CONSTRAINT ck_person_notice_period CHECK (notice_period_days BETWEEN 0 AND 365),
    ADD COLUMN mobility                varchar(40)
        CONSTRAINT ck_person_mobility CHECK (mobility IN ('NONE', 'WITHIN_COUNTRY', 'INTERNATIONAL')),
    -- AES-256-GCM ciphertext (1 byte key version, 12 byte IV, ciphertext + tag) written by security.FieldCipher.
    ADD COLUMN compensation_enc        bytea,
    ADD COLUMN languages               jsonb        NOT NULL DEFAULT '[]'
        CONSTRAINT ck_person_languages_array CHECK (jsonb_typeof(languages) = 'array'),
    ADD COLUMN phone_normalized        varchar(20),
    ADD COLUMN linkedin_url_normalized varchar(300);

CREATE INDEX idx_person_source_channel ON person (source_channel);
CREATE INDEX idx_person_phone_normalized ON person (phone_normalized) WHERE phone_normalized IS NOT NULL;
CREATE INDEX idx_person_linkedin_normalized ON person (linkedin_url_normalized)
    WHERE linkedin_url_normalized IS NOT NULL;
-- Duplicate check on name + company (B-04).
CREATE INDEX idx_person_name_company_norm ON person
    (immutable_unaccent(lower(trim(name))), immutable_unaccent(lower(trim(company))));

ALTER TABLE person_aud
    ADD COLUMN source_channel          varchar(40),
    ADD COLUMN notice_period_days      smallint,
    ADD COLUMN mobility                varchar(40),
    ADD COLUMN compensation_enc        bytea,
    ADD COLUMN languages               jsonb,
    ADD COLUMN phone_normalized        varchar(20),
    ADD COLUMN linkedin_url_normalized varchar(300);

CREATE TABLE person_education (
    id         uuid PRIMARY KEY,
    person_id  uuid         NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    school     varchar(200) NOT NULL,
    degree     varchar(100),
    field      varchar(200),
    start_year smallint,
    end_year   smallint,
    sort_order int          NOT NULL,
    CONSTRAINT ck_person_education_years CHECK (end_year IS NULL OR start_year IS NULL OR end_year >= start_year)
);
CREATE INDEX idx_person_education_person ON person_education (person_id);

CREATE TABLE person_education_aud (
    id         uuid   NOT NULL,
    rev        bigint NOT NULL REFERENCES revinfo (rev),
    revtype    smallint,
    revend     bigint REFERENCES revinfo (rev),
    person_id  uuid,
    school     varchar(200),
    degree     varchar(100),
    field      varchar(200),
    start_year smallint,
    end_year   smallint,
    sort_order int,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_person_education_aud_person ON person_education_aud (person_id);
CREATE INDEX idx_person_education_aud_rev ON person_education_aud (rev);
CREATE INDEX idx_person_education_aud_revend ON person_education_aud (revend);
