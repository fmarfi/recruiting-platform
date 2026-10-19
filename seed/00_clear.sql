-- 00: helper functions and clearing of all data tables (idempotent reload).
-- Keeps flyway_schema_history and the retention_settings row (reset in 08_privacy.sql).
--   pg_temp.uid(kind, n) -> fixed v7-shaped UUID 01930000-<kind:4>-7000-8000-<n:12>
--   pg_temp.slug(text)   -> lower-case ASCII slug (Turkish letters folded)
CREATE FUNCTION pg_temp.uid(k int, n int) RETURNS uuid LANGUAGE sql IMMUTABLE AS
$$ SELECT format('01930000-%s-7000-8000-%s', lpad(k::text, 4, '0'), lpad(n::text, 12, '0'))::uuid $$;
CREATE FUNCTION pg_temp.slug(t text) RETURNS text LANGUAGE sql IMMUTABLE AS
$$ SELECT regexp_replace(lower(translate(t, 'çğıöşüÇĞİÖŞÜâÂ', 'cgiosucgiosuaa')), '[^a-z0-9]+', '', 'g') $$;

DELETE FROM timeline_entry;
DELETE FROM next_action;
DELETE FROM activity;
DELETE FROM application;
DELETE FROM proposal;
DELETE FROM job_order;
DELETE FROM account_open_job;
DELETE FROM contact;
DELETE FROM network_link;
DELETE FROM person_employment;
DELETE FROM consent_record;
DELETE FROM consent_request;
DELETE FROM data_subject_request;
DELETE FROM person_privacy;
DELETE FROM retention_run;
DELETE FROM import_job;
DELETE FROM person;
DELETE FROM account;
DELETE FROM privacy_notice;
DELETE FROM access_log;
DELETE FROM event_publication;
DELETE FROM event_publication_archive;
DELETE FROM app_user_aud;
DELETE FROM account_aud;
DELETE FROM person_aud;
DELETE FROM person_employment_aud;
DELETE FROM contact_aud;
DELETE FROM network_link_aud;
DELETE FROM job_order_aud;
DELETE FROM application_aud;
DELETE FROM proposal_aud;
DELETE FROM activity_aud;
DELETE FROM next_action_aud;
DELETE FROM person_privacy_aud;
DELETE FROM data_subject_request_aud;
DELETE FROM retention_settings_aud;
DELETE FROM revinfo;
UPDATE retention_settings SET created_by = NULL, updated_by = NULL;
DELETE FROM app_user;
