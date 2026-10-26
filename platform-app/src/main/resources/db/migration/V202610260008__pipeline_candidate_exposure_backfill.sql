-- Backfill (1.2.0): one PIPELINE exposure per application whose current stage is client-facing (PRESENTED,
-- CLIENT_INTERVIEW, OFFER, PLACED) or that reached PRESENTED in application_aud. presented_at is the earliest
-- client-facing revision time, else stage_changed_at. Ids come from gen_random_uuid() (UUID v4): the rows are
-- history, nothing depends on the v7 ordering. Idempotent (ON CONFLICT DO NOTHING).
INSERT INTO candidate_exposure (id, person_id, account_id, job_order_id, application_id, channel, presented_at)
SELECT gen_random_uuid(), a.person_id, j.account_id, a.job_order_id, a.id, 'PIPELINE',
       coalesce((SELECT to_timestamp(min(r.revtstmp) / 1000.0)
                 FROM application_aud h JOIN revinfo r ON r.rev = h.rev
                 WHERE h.id = a.id AND h.stage IN ('PRESENTED', 'CLIENT_INTERVIEW', 'OFFER', 'PLACED')),
                a.stage_changed_at)
FROM application a
JOIN job_order j ON j.id = a.job_order_id
WHERE a.stage IN ('PRESENTED', 'CLIENT_INTERVIEW', 'OFFER', 'PLACED')
   OR EXISTS (SELECT 1 FROM application_aud h WHERE h.id = a.id AND h.stage = 'PRESENTED')
ON CONFLICT (application_id, channel) WHERE channel = 'PIPELINE' DO NOTHING;
