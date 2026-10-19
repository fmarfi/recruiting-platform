-- 07: account timeline, derived from the rows seeded in 02-06 so it always matches the history:
-- account created, status changes, contacts, activities, proposals, job orders, candidate moves.
-- Id uid(19, k); source_event_id uid(18, k). k ranges: 1xxx account, 2xxx status, 3xxx contact,
-- 4xxx activity, 5xxx proposal, 6xxx job order, 7xxx candidate.
CREATE TEMP TABLE acct_path (status text, j int, s int, frm text, tto text, reason text);
INSERT INTO acct_path VALUES
 ('PROPOSAL_SENT', 1, 1, 'LEAD', 'PROPOSAL_SENT', NULL),
 ('ACTIVE', 1, 2, 'LEAD', 'PROPOSAL_SENT', NULL), ('ACTIVE', 2, 2, 'PROPOSAL_SENT', 'ACTIVE', NULL),
 ('ON_HOLD', 1, 3, 'LEAD', 'PROPOSAL_SENT', NULL), ('ON_HOLD', 2, 3, 'PROPOSAL_SENT', 'ACTIVE', NULL),
 ('ON_HOLD', 3, 3, 'ACTIVE', 'ON_HOLD', 'Client paused hiring while the budget is reviewed'),
 ('CLOSED', 1, 3, 'LEAD', 'PROPOSAL_SENT', NULL), ('CLOSED', 2, 3, 'PROPOSAL_SENT', 'ACTIVE', NULL),
 ('CLOSED', 3, 3, 'ACTIVE', 'CLOSED', 'Relationship closed after the last search was completed');
CREATE TEMP TABLE job_path (status text, j int, s int, frm text, tto text, reason text);
INSERT INTO job_path VALUES
 ('SOURCING', 1, 1, 'OPEN', 'SOURCING', NULL),
 ('SHORTLIST_SENT', 1, 2, 'OPEN', 'SOURCING', NULL), ('SHORTLIST_SENT', 2, 2, 'SOURCING', 'SHORTLIST_SENT', NULL),
 ('INTERVIEWING', 1, 3, 'OPEN', 'SOURCING', NULL), ('INTERVIEWING', 2, 3, 'SOURCING', 'SHORTLIST_SENT', NULL),
 ('INTERVIEWING', 3, 3, 'SHORTLIST_SENT', 'INTERVIEWING', NULL),
 ('OFFER', 1, 4, 'OPEN', 'SOURCING', NULL), ('OFFER', 2, 4, 'SOURCING', 'SHORTLIST_SENT', NULL),
 ('OFFER', 3, 4, 'SHORTLIST_SENT', 'INTERVIEWING', NULL), ('OFFER', 4, 4, 'INTERVIEWING', 'OFFER', NULL),
 ('FILLED', 1, 5, 'OPEN', 'SOURCING', NULL), ('FILLED', 2, 5, 'SOURCING', 'SHORTLIST_SENT', NULL),
 ('FILLED', 3, 5, 'SHORTLIST_SENT', 'INTERVIEWING', NULL), ('FILLED', 4, 5, 'INTERVIEWING', 'OFFER', NULL),
 ('FILLED', 5, 5, 'OFFER', 'FILLED', NULL),
 ('CANCELLED', 1, 2, 'OPEN', 'SOURCING', NULL),
 ('CANCELLED', 2, 2, 'SOURCING', 'CANCELLED', 'Client cancelled the search after a reorganisation');

-- account created
INSERT INTO timeline_entry (id, account_id, occurred_at, kind, entity_type, entity_id, to_status, summary, actor_id, source_event_id)
SELECT pg_temp.uid(19, 1000 + n), pg_temp.uid(2, n), now() - make_interval(days => opened_days), 'ACCOUNT', 'ACCOUNT',
       pg_temp.uid(2, n), 'LEAD', 'Account created: ' || name, pg_temp.uid(1, owner), pg_temp.uid(18, 1000 + n)
FROM acct_seed;

-- account status changes, spread between the opening date and the last status change
INSERT INTO timeline_entry (id, account_id, occurred_at, kind, entity_type, entity_id, from_status, to_status, reason, summary, actor_id, source_event_id)
SELECT pg_temp.uid(19, 2000 + a.n * 10 + p.j), pg_temp.uid(2, a.n),
       now() - make_interval(days => a.opened_days) + make_interval(secs => (a.opened_days - a.status_days) * 86400.0 * p.j / p.s),
       'ACCOUNT_STATUS', 'ACCOUNT', pg_temp.uid(2, a.n), p.frm, p.tto, p.reason,
       'Status changed from ' || p.frm || ' to ' || p.tto, pg_temp.uid(1, a.owner), pg_temp.uid(18, 2000 + a.n * 10 + p.j)
FROM acct_seed a JOIN acct_path p ON p.status = a.status WHERE a.n NOT IN (6, 24);
-- accounts 6 and 24 went through a rejected proposal without a win, so they never became ACTIVE
INSERT INTO timeline_entry (id, account_id, occurred_at, kind, entity_type, entity_id, from_status, to_status, reason, summary, actor_id, source_event_id)
SELECT pg_temp.uid(19, 2000 + a.n * 10 + s.k), pg_temp.uid(2, a.n), now() - make_interval(days => s.days),
       'ACCOUNT_STATUS', 'ACCOUNT', pg_temp.uid(2, a.n), s.frm, s.tto, s.reason,
       'Status changed from ' || s.frm || ' to ' || s.tto, pg_temp.uid(1, a.owner), pg_temp.uid(18, 2000 + a.n * 10 + s.k)
FROM acct_seed a
JOIN (VALUES (6, 1, 'LEAD', 'PROPOSAL_SENT', NULL::text, 30),
             (6, 2, 'PROPOSAL_SENT', 'LEAD', NULL::text, 20),
             (24, 1, 'LEAD', 'PROPOSAL_SENT', NULL::text, 300),
             (24, 2, 'PROPOSAL_SENT', 'CLOSED', 'Proposal rejected by the client; relationship closed', 280))
     AS s(n, k, frm, tto, reason, days) ON s.n = a.n;


-- contacts
INSERT INTO timeline_entry (id, account_id, occurred_at, kind, entity_type, entity_id, summary, person_id, actor_id, source_event_id)
SELECT pg_temp.uid(19, 3000 + p.n), pg_temp.uid(2, p.acct_n), c.created_at, 'CONTACT', 'CONTACT', c.id,
       'Contact added: ' || p.name || ' (' || coalesce(c.role, '') || ')', pg_temp.uid(3, p.n), c.created_by, pg_temp.uid(18, 3000 + p.n)
FROM p_full p JOIN contact c ON c.id = pg_temp.uid(4, p.n) WHERE p.n <= 30;

-- activities
INSERT INTO timeline_entry (id, account_id, occurred_at, kind, entity_type, entity_id, summary, person_id, job_order_id, actor_id, source_event_id)
SELECT pg_temp.uid(19, 4000 + r.rn), r.account_id, r.occurred_at, 'ACTIVITY', 'ACTIVITY', r.id,
       initcap(lower(r.type)) || ': ' || left(r.note, 100), r.person_id, r.job_order_id, r.user_id, pg_temp.uid(18, 4000 + r.rn)
FROM (SELECT a.*, (row_number() OVER (ORDER BY a.id))::int AS rn FROM activity a WHERE a.account_id IS NOT NULL) r;

-- proposals: created (k=0), sent (k=1), decided (k=2)
INSERT INTO timeline_entry (id, account_id, occurred_at, kind, entity_type, entity_id, from_status, to_status, reason, summary, actor_id, source_event_id)
SELECT pg_temp.uid(19, 5000 + p.n * 10 + s.k), pg_temp.uid(2, p.acct),
       CASE s.k WHEN 0 THEN pr.created_at WHEN 1 THEN now() - make_interval(days => p.sent_days)
                ELSE now() - make_interval(days => p.decided_days) END,
       'PROPOSAL', 'PROPOSAL', pr.id,
       CASE s.k WHEN 0 THEN NULL WHEN 1 THEN 'DRAFT' ELSE 'SENT' END,
       CASE s.k WHEN 0 THEN 'DRAFT' WHEN 1 THEN 'SENT' ELSE p.status END,
       CASE WHEN s.k = 2 THEN p.reason END,
       CASE s.k WHEN 0 THEN 'Proposal created: ' || p.title WHEN 1 THEN 'Proposal sent: ' || p.title
                ELSE 'Proposal ' || lower(p.status) || ': ' || p.title END,
       pr.owner_id, pg_temp.uid(18, 5000 + p.n * 10 + s.k)
FROM prop_seed p JOIN proposal pr ON pr.id = pg_temp.uid(7, p.n)
JOIN generate_series(0, 2) AS s(k) ON (s.k = 0 OR (s.k = 1 AND p.sent_days IS NOT NULL) OR (s.k = 2 AND p.decided_days IS NOT NULL));

-- job orders: created and status changes
INSERT INTO timeline_entry (id, account_id, occurred_at, kind, entity_type, entity_id, to_status, summary, job_order_id, actor_id, source_event_id)
SELECT pg_temp.uid(19, 6000 + n * 10), pg_temp.uid(2, acct), now() - make_interval(days => opened_days), 'JOB_ORDER', 'JOB_ORDER',
       pg_temp.uid(5, n), 'OPEN', 'Job order created: ' || title, pg_temp.uid(5, n), pg_temp.uid(1, owner), pg_temp.uid(18, 6000 + n * 10)
FROM j_seed;
INSERT INTO timeline_entry (id, account_id, occurred_at, kind, entity_type, entity_id, from_status, to_status, reason, summary, job_order_id, actor_id, source_event_id)
SELECT pg_temp.uid(19, 6000 + j.n * 10 + p.j), pg_temp.uid(2, j.acct),
       now() - make_interval(days => j.opened_days) + make_interval(secs => (j.opened_days - j.status_days) * 86400.0 * p.j / p.s),
       'JOB_ORDER', 'JOB_ORDER', pg_temp.uid(5, j.n), p.frm, p.tto, p.reason,
       'Job order ' || j.title || ': ' || p.frm || ' to ' || p.tto, pg_temp.uid(5, j.n), pg_temp.uid(1, j.owner),
       pg_temp.uid(18, 6000 + j.n * 10 + p.j)
FROM j_seed j JOIN job_path p ON p.status = j.status;

-- candidates: added (k=0), then moved to the current stage (k=1)
INSERT INTO timeline_entry (id, account_id, occurred_at, kind, entity_type, entity_id, from_status, to_status, reason, summary, person_id, job_order_id, actor_id, source_event_id)
SELECT pg_temp.uid(19, 7000 + s.n * 10 + k.k), pg_temp.uid(2, j.acct),
       -- PLACED is reachable only from OFFER, so a placed candidate gets an OFFER step first (k=1), then PLACED (k=2)
       CASE k.k WHEN 0 THEN ap.created_at WHEN 2 THEN ap.stage_changed_at
                ELSE CASE WHEN s.stage = 'PLACED' THEN ap.stage_changed_at - interval '2 days' ELSE ap.stage_changed_at END END,
       'CANDIDATE', 'APPLICATION', ap.id,
       CASE k.k WHEN 0 THEN NULL WHEN 1 THEN 'SOURCED' ELSE 'OFFER' END,
       CASE k.k WHEN 0 THEN 'SOURCED' WHEN 1 THEN CASE WHEN s.stage = 'PLACED' THEN 'OFFER' ELSE s.stage END ELSE 'PLACED' END,
       CASE WHEN k.k = 1 AND s.stage <> 'PLACED' THEN s.reason END,
       CASE k.k WHEN 0 THEN 'Candidate added: ' || p.name || ' to ' || j.title
                ELSE 'Candidate ' || p.name || ' moved to '
                     || CASE k.k WHEN 1 THEN CASE WHEN s.stage = 'PLACED' THEN 'OFFER' ELSE s.stage END ELSE 'PLACED' END END,
       pg_temp.uid(3, s.person), pg_temp.uid(5, j.n), pg_temp.uid(1, j.owner), pg_temp.uid(18, 7000 + s.n * 10 + k.k)
FROM app_seed s
JOIN application ap ON ap.id = pg_temp.uid(6, s.n)
JOIN j_seed j ON j.n = s.job
JOIN p_full p ON p.n = s.person
JOIN generate_series(0, 2) AS k(k) ON (k.k = 0 OR (k.k = 1 AND s.stage <> 'SOURCED') OR (k.k = 2 AND s.stage = 'PLACED'));
