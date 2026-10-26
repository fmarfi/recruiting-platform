-- Contract 1.2.0 (ADR 0011): consent renewal and reminders, last_consent_at, retention settings. Owner: privacy.
ALTER TABLE consent_request
    ADD COLUMN purpose                   varchar(40) NOT NULL DEFAULT 'INITIAL'
        CONSTRAINT ck_consent_request_purpose CHECK (purpose IN ('INITIAL', 'RENEWAL')),
    ADD COLUMN opened_at                 timestamptz,
    ADD COLUMN reminder_days_before      smallint,
    ADD COLUMN reminder_for_retain_until date;
-- One reminder per person, step and retention period (the nightly job is idempotent).
CREATE UNIQUE INDEX uq_consent_request_reminder
    ON consent_request (person_id, reminder_for_retain_until, reminder_days_before)
    WHERE reminder_days_before IS NOT NULL;

ALTER TABLE person_privacy ADD COLUMN last_consent_at timestamptz;
ALTER TABLE person_privacy_aud ADD COLUMN last_consent_at timestamptz;

ALTER TABLE retention_settings DROP CONSTRAINT retention_settings_basis_check;
ALTER TABLE retention_settings
    ADD CONSTRAINT retention_settings_basis_check
        CHECK (basis IN ('FROM_CREATED', 'FROM_LAST_CONTACT', 'FROM_LAST_CONSENT'));
-- The default changes here. The existing row is switched by the next migration (only if it still has the old default).
ALTER TABLE retention_settings ALTER COLUMN basis SET DEFAULT 'FROM_LAST_CONSENT';
ALTER TABLE retention_settings
    ADD COLUMN auto_send_on_cv_upload boolean    NOT NULL DEFAULT true,
    ADD COLUMN renewal_reminder_days  smallint[] NOT NULL DEFAULT '{60,14}'
        CONSTRAINT ck_retention_reminder_days CHECK (cardinality(renewal_reminder_days) <= 5
            AND 1 <= ALL (renewal_reminder_days) AND 365 >= ALL (renewal_reminder_days));
ALTER TABLE retention_settings_aud
    ADD COLUMN auto_send_on_cv_upload boolean,
    ADD COLUMN renewal_reminder_days  smallint[];
