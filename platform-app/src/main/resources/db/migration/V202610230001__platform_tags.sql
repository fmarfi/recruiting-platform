-- Wave 2a (ADR 0010): tags on person, account, job_order, proposal (and their _aud tables).
ALTER TABLE person    ADD COLUMN tags varchar(50)[] NOT NULL DEFAULT '{}' CONSTRAINT ck_person_tags_max CHECK (cardinality(tags) <= 20);
ALTER TABLE account   ADD COLUMN tags varchar(50)[] NOT NULL DEFAULT '{}' CONSTRAINT ck_account_tags_max CHECK (cardinality(tags) <= 20);
ALTER TABLE job_order ADD COLUMN tags varchar(50)[] NOT NULL DEFAULT '{}' CONSTRAINT ck_job_order_tags_max CHECK (cardinality(tags) <= 20);
ALTER TABLE proposal  ADD COLUMN tags varchar(50)[] NOT NULL DEFAULT '{}' CONSTRAINT ck_proposal_tags_max CHECK (cardinality(tags) <= 20);

CREATE INDEX idx_person_tags    ON person    USING gin (tags);
CREATE INDEX idx_account_tags   ON account   USING gin (tags);
CREATE INDEX idx_job_order_tags ON job_order USING gin (tags);
CREATE INDEX idx_proposal_tags  ON proposal  USING gin (tags);

ALTER TABLE person_aud    ADD COLUMN tags varchar(50)[];
ALTER TABLE account_aud   ADD COLUMN tags varchar(50)[];
ALTER TABLE job_order_aud ADD COLUMN tags varchar(50)[];
ALTER TABLE proposal_aud  ADD COLUMN tags varchar(50)[];
