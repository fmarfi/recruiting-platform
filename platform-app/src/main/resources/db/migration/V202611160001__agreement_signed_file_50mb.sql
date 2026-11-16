-- Signed agreement PDFs may be up to 50 MB (decision after meeting 02; contract text follows in 1.5).
ALTER TABLE agreement DROP CONSTRAINT ck_agreement_signed_file_size;
ALTER TABLE agreement ADD CONSTRAINT ck_agreement_signed_file_size
    CHECK (signed_file_size IS NULL OR signed_file_size BETWEEN 1 AND 52428800);
