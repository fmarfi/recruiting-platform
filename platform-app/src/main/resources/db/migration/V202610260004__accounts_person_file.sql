-- Contract 1.2.0 (ADR 0011): Turkish accent-insensitive text search and person_file. Owner: accounts.
CREATE TEXT SEARCH CONFIGURATION tr_unaccent (COPY = pg_catalog.turkish);
ALTER TEXT SEARCH CONFIGURATION tr_unaccent
    ALTER MAPPING FOR asciiword, word, hword, hword_part, asciihword, hword_asciipart
    WITH unaccent, turkish_stem;

-- "version" is the file version number (CV: max + 1 per person). The JPA @Version column is row_version, because the
-- contract gives the audit column the same name (change request in the db-engineer hand-off).
CREATE TABLE person_file (
    id           uuid PRIMARY KEY,
    person_id    uuid         NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    kind         varchar(40)  NOT NULL CHECK (kind IN ('CV', 'DOCUMENT')),
    version      int          NOT NULL,
    is_current   boolean      NOT NULL,
    file_name    varchar(255) NOT NULL,
    content_type varchar(100) NOT NULL,
    size_bytes   bigint       NOT NULL CONSTRAINT ck_person_file_size CHECK (size_bytes BETWEEN 1 AND 10485760),
    sha256       char(64)     NOT NULL,
    storage_key  varchar(300) NOT NULL,
    text_status  varchar(40)  NOT NULL DEFAULT 'PENDING'
        CHECK (text_status IN ('PENDING', 'DONE', 'NO_TEXT', 'FAILED')),
    extracted_text text
        CONSTRAINT ck_person_file_text_len CHECK (char_length(extracted_text) <= 1000000),
    text_tsv     tsvector GENERATED ALWAYS AS (to_tsvector('tr_unaccent', coalesce(extracted_text, ''))) STORED,
    extracted_at timestamptz,
    uploaded_by  uuid         NOT NULL REFERENCES app_user (id),
    created_at   timestamptz  NOT NULL DEFAULT now(),
    created_by   uuid REFERENCES app_user (id),
    updated_at   timestamptz  NOT NULL DEFAULT now(),
    updated_by   uuid REFERENCES app_user (id),
    row_version  bigint       NOT NULL DEFAULT 0,
    CONSTRAINT uq_person_file_storage_key UNIQUE (storage_key),
    CONSTRAINT uq_person_file_sha UNIQUE (person_id, sha256)
);
CREATE INDEX idx_person_file_person ON person_file (person_id, kind, version DESC);
CREATE UNIQUE INDEX uq_person_file_cv_version ON person_file (person_id, kind, version) WHERE kind = 'CV';
CREATE UNIQUE INDEX uq_person_file_cv_current ON person_file (person_id) WHERE kind = 'CV' AND is_current;
CREATE INDEX idx_person_file_text_pending ON person_file (created_at) WHERE text_status = 'PENDING';
CREATE INDEX idx_person_file_uploaded_by ON person_file (uploaded_by);
CREATE INDEX idx_person_file_tsv ON person_file USING gin (text_tsv) WHERE kind = 'CV' AND is_current;

-- extracted_text and text_tsv are not audited.
CREATE TABLE person_file_aud (
    id           uuid   NOT NULL,
    rev          bigint NOT NULL REFERENCES revinfo (rev),
    revtype      smallint,
    revend       bigint REFERENCES revinfo (rev),
    person_id    uuid,
    kind         varchar(40),
    version      int,
    is_current   boolean,
    file_name    varchar(255),
    content_type varchar(100),
    size_bytes   bigint,
    sha256       char(64),
    storage_key  varchar(300),
    text_status  varchar(40),
    extracted_at timestamptz,
    uploaded_by  uuid,
    created_at   timestamptz,
    created_by   uuid,
    updated_at   timestamptz,
    updated_by   uuid,
    PRIMARY KEY (id, rev)
);
CREATE INDEX idx_person_file_aud_person ON person_file_aud (person_id);
CREATE INDEX idx_person_file_aud_rev ON person_file_aud (rev);
CREATE INDEX idx_person_file_aud_revend ON person_file_aud (revend);
