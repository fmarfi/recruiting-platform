-- Basis switch (1.2.0, schema.md "Basis switch migration"). Plain statements (no DO block), run in this order:
-- 1) backfill person_privacy.last_consent_at from the latest GIVEN consent_record (always);
-- 2) only while the settings row still has the old default FROM_CREATED: recompute basis_date and retain_until for
--    every person not ANONYMISED and put FLAGGED rows that are no longer inside the warning window back to ACTIVE;
-- 3) switch the settings row to FROM_LAST_CONSENT (a basis chosen by an admin is kept, and then nothing is recomputed).
-- Never anonymises: the nightly run does that with the normal rules. The person.retention_status copy follows
-- through RetentionRenewed, published by a one-off privacy job, not by SQL. Calendar days use Europe/Istanbul.
-- NOTE: these SQL updates bypass Envers, so the latest person_privacy_aud row is stale. The one-off job must
-- re-save the changed person_privacy rows through JPA so that a fresh revision is written.
UPDATE person_privacy pp
SET last_consent_at = c.recorded_at
FROM (SELECT person_id, max(recorded_at) AS recorded_at
      FROM consent_record WHERE decision = 'GIVEN' GROUP BY person_id) c
WHERE c.person_id = pp.person_id;

UPDATE person_privacy pp
SET basis_date       = nb.basis_date,
    retain_until     = (nb.basis_date + make_interval(months => s.retention_months))::date,
    retention_status = CASE WHEN pp.retention_status = 'FLAGGED'
                                 AND (nb.basis_date + make_interval(months => s.retention_months))::date
                                     - s.warning_days > current_date
                            THEN 'ACTIVE' ELSE pp.retention_status END,
    flagged_at       = CASE WHEN pp.retention_status = 'FLAGGED'
                                 AND (nb.basis_date + make_interval(months => s.retention_months))::date
                                     - s.warning_days > current_date
                            THEN NULL ELSE pp.flagged_at END
FROM retention_settings s,
     (SELECT q.person_id,
             coalesce((q.last_consent_at AT TIME ZONE 'Europe/Istanbul')::date,
                      (p.created_at AT TIME ZONE 'Europe/Istanbul')::date) AS basis_date
      FROM person_privacy q JOIN person p ON p.id = q.person_id) nb
WHERE nb.person_id = pp.person_id
  AND s.id = 1
  AND s.basis = 'FROM_CREATED'
  AND pp.retention_status <> 'ANONYMISED';

UPDATE retention_settings SET basis = 'FROM_LAST_CONSENT' WHERE id = 1 AND basis = 'FROM_CREATED';
