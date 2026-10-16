-- 08: privacy. Two notice versions (TR, EN), a person_privacy row and an information notice for everyone (NFR-01),
-- consent requests and records with a status mix (NOT_SENT, SENT, GIVEN, REFUSED, WITHDRAWN), 4 flagged people and
-- 1 anonymised person, 4 data-subject requests (every status), retention settings and one past retention run.
-- Ids: notice uid(13,n), consent request uid(14, person n), consent record uid(15, person n*10+k), DSR uid(16,n).
INSERT INTO privacy_notice (id, version, language, notice_text, consent_text, effective_from, created_at, created_by)
VALUES
 (pg_temp.uid(13, 1), 'v1.0', 'TR',
  'Aydınlatma metni (demo): Kişisel verileriniz işe alım ve danışmanlık hizmetleri amacıyla işlenir. Verileriniz iki yıl saklanır; erişim, düzeltme ve silme haklarınız saklıdır.',
  'Demo açık rıza metni: Kişisel verilerimin aday havuzunda saklanmasına ve uygun pozisyonlar için işlenmesine açık rıza veriyorum.',
  DATE '2024-01-01', now() - interval '500 days', pg_temp.uid(1, 1)),
 (pg_temp.uid(13, 2), 'v1.0', 'EN',
  'Information notice (demo): Your personal data is processed for recruitment and advisory services. It is kept for two years; you have the right to access, correct and delete it.',
  'Demo consent text: I consent to my personal data being kept in the candidate pool and used to consider me for suitable positions.',
  DATE '2024-01-01', now() - interval '500 days', pg_temp.uid(1, 1));

-- everyone got the information notice one day after being added; retention is 24 months from creation
INSERT INTO person_privacy (person_id, consent_status, notice_sent_at, basis_date, retain_until, retention_status, flagged_at)
SELECT pg_temp.uid(3, n), consent_status, created_at + interval '1 day', created_at::date,
       (created_at::date + interval '24 months')::date, retention_status,
       CASE WHEN retention_status = 'FLAGGED' THEN now() - interval '5 days' END
FROM p_full;

-- consent requests for everyone whose consent e-mail went out; a failed delivery for persons with n % 8 = 7
INSERT INTO consent_request (id, person_id, notice_id, language, token_hash, delivery_status, queued_at, sent_at,
                             failure_reason, expires_at, responded_at, response, requested_by)
SELECT pg_temp.uid(14, n), pg_temp.uid(3, n), pg_temp.uid(13, CASE WHEN n % 2 = 1 THEN 1 ELSE 2 END),
       CASE WHEN n % 2 = 1 THEN 'TR' ELSE 'EN' END, md5(n || 'a') || md5(n || 'b'),
       CASE WHEN consent_status = 'NOT_SENT' THEN 'FAILED' ELSE 'SENT' END,
       created_at + interval '2 days',
       CASE WHEN consent_status <> 'NOT_SENT' THEN created_at + interval '2 days' END,
       CASE WHEN consent_status = 'NOT_SENT' THEN 'Mailbox unavailable' END,
       created_at + interval '32 days',
       CASE WHEN consent_status IN ('GIVEN', 'REFUSED', 'WITHDRAWN') THEN created_at + interval '5 days' END,
       CASE consent_status WHEN 'GIVEN' THEN 'GIVEN' WHEN 'WITHDRAWN' THEN 'GIVEN' WHEN 'REFUSED' THEN 'REFUSED' END,
       pg_temp.uid(1, owner)
FROM p_full WHERE n % 8 <> 0 AND (consent_status <> 'NOT_SENT' OR n % 8 = 7);

-- consent records: GIVEN by e-mail link (every third on the website form), REFUSED, and WITHDRAWN after a GIVEN
INSERT INTO consent_record (id, person_id, decision, channel, notice_id, consent_request_id, recorded_at, recorded_by, evidence, ip_address)
SELECT pg_temp.uid(15, n * 10 + 1), pg_temp.uid(3, n),
       CASE consent_status WHEN 'REFUSED' THEN 'REFUSED' ELSE 'GIVEN' END,
       CASE WHEN consent_status = 'GIVEN' AND n % 3 = 0 THEN 'WEBSITE_FORM' ELSE 'EMAIL_LINK' END,
       pg_temp.uid(13, CASE WHEN n % 2 = 1 THEN 1 ELSE 2 END),
       CASE WHEN consent_status = 'GIVEN' AND n % 3 = 0 THEN NULL ELSE pg_temp.uid(14, n) END,
       created_at + interval '5 days', NULL,
       CASE WHEN consent_status = 'GIVEN' AND n % 3 = 0 THEN 'Submitted through the careers page form (demo).' END,
       '203.0.113.' || (n % 250 + 1)
FROM p_full WHERE consent_status IN ('GIVEN', 'REFUSED', 'WITHDRAWN');
INSERT INTO consent_record (id, person_id, decision, channel, notice_id, consent_request_id, recorded_at, recorded_by, evidence)
SELECT pg_temp.uid(15, n * 10 + 2), pg_temp.uid(3, n), 'WITHDRAWN', 'PHONE',
       pg_temp.uid(13, CASE WHEN n % 2 = 1 THEN 1 ELSE 2 END), NULL, created_at + interval '12 days',
       pg_temp.uid(1, owner), 'Candidate called and withdrew consent (demo).'
FROM p_full WHERE consent_status = 'WITHDRAWN';

-- data-subject requests, one per status
INSERT INTO data_subject_request (id, person_id, requester_name, requester_email, type, status, description,
                                  received_at, due_on, resolved_at, resolution, handled_by, created_at, created_by, updated_at, updated_by)
SELECT pg_temp.uid(16, v.n), CASE WHEN v.person IS NOT NULL THEN pg_temp.uid(3, v.person) END,
       v.rname, v.remail, v.type, v.status, v.descr, now() - make_interval(days => v.recv),
       (now() - make_interval(days => v.recv) + interval '30 days')::date,
       CASE WHEN v.status IN ('COMPLETED', 'REJECTED') THEN now() - make_interval(days => v.recv - 6) END,
       v.res, pg_temp.uid(1, 2), now() - make_interval(days => v.recv), pg_temp.uid(1, 2),
       now() - make_interval(days => v.recv), pg_temp.uid(1, 2)
FROM (VALUES
 (1, 33,   'Data subject 33 (demo)',    'access.requester@mail.example',     'ACCESS',     'RECEIVED',    'Asks for a copy of all data held about them.', 10, NULL),
 (2, 5,    'Data subject 5 (demo)',     'correction.requester@mail.example', 'CORRECTION', 'IN_PROGRESS', 'Wants the job title and employer corrected.',  15, NULL),
 (3, NULL, 'Former candidate (demo)',   'deletion.requester@mail.example',   'DELETION',   'COMPLETED',   'Asks for deletion of all personal data.',      40, 'Data anonymised and the deletion logged.'),
 (4, 12,   'Data subject 12 (demo)',    'other.requester@mail.example',      'OTHER',      'REJECTED',    'General question about the legal basis.',      25, 'Answered by e-mail; not a data-subject right.')
) AS v(n, person, rname, remail, type, status, descr, recv, res);

-- retention: the settings row comes from the migration; reset it to the defaults and add one past run
UPDATE retention_settings SET retention_months = 24, basis = 'FROM_CREATED', action = 'ANONYMISE', warning_days = 30,
       auto_send_notice = true, auto_send_person_types = 'CANDIDATE,EXECUTIVE', renew_on_consent = true WHERE id = 1;
INSERT INTO retention_run (id, started_at, finished_at, triggered_by, dry_run, flagged_count, anonymised_count, deleted_count)
VALUES (pg_temp.uid(17, 1), now() - interval '2 days', now() - interval '2 days' + interval '3 seconds', pg_temp.uid(1, 2), false, 4, 1, 0);
