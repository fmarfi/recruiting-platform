-- 06: activities (50 on accounts, 20 on candidates) and 24 next actions.
-- Next actions are relative to the load date: 6 overdue, 6 due today, 8 upcoming, 4 done.
-- Ids: activity uid(8, n), next action uid(9, n).
INSERT INTO activity (id, type, note, occurred_at, account_id, person_id, job_order_id, user_id,
                      created_at, created_by, updated_at, updated_by)
SELECT pg_temp.uid(8, g.n), t.ty,
       CASE t.ty
         WHEN 'CALL'    THEN 'Phone call: discussed hiring plans and timing for the next quarter.'
         WHEN 'MEETING' THEN 'Meeting at the client office: reviewed priorities and the decision process.'
         WHEN 'EMAIL'   THEN 'Sent an e-mail with the market overview and next steps.'
         WHEN 'VISIT'   THEN 'Site visit: met the leadership team and toured the facility.'
         ELSE 'Internal note: client is price-sensitive but values speed and discretion.' END,
       w.at, pg_temp.uid(2, a.n), CASE WHEN t.ty <> 'NOTE' THEN pg_temp.uid(3, a.n) END, NULL, pg_temp.uid(1, a.owner),
       w.at, pg_temp.uid(1, a.owner), w.at, pg_temp.uid(1, a.owner)
FROM generate_series(1, 50) AS g(n)
JOIN acct_seed a ON a.n = (g.n - 1) % 25 + 1
CROSS JOIN LATERAL (SELECT (ARRAY['CALL','MEETING','EMAIL','VISIT','NOTE'])[g.n % 5 + 1] AS ty) t
CROSS JOIN LATERAL (SELECT now() - make_interval(days => (g.n * 3) % greatest(least(a.opened_days - 1, 45), 1),
                                                 hours => g.n % 7 + 1) AS at) w;

INSERT INTO activity (id, type, note, occurred_at, account_id, person_id, job_order_id, user_id,
                      created_at, created_by, updated_at, updated_by)
SELECT pg_temp.uid(8, 100 + s.n), (ARRAY['CALL','MEETING','EMAIL'])[s.n % 3 + 1],
       (ARRAY['Screening call: motivation, compensation and availability.',
              'Interview preparation meeting before the client round.',
              'Sent the role brief and asked for an updated CV.'])[s.n % 3 + 1],
       w.at, pg_temp.uid(2, j.acct), pg_temp.uid(3, s.person), pg_temp.uid(5, j.n), pg_temp.uid(1, j.owner),
       w.at, pg_temp.uid(1, j.owner), w.at, pg_temp.uid(1, j.owner)
FROM app_seed s JOIN j_seed j ON j.n = s.job
CROSS JOIN LATERAL (SELECT now() - make_interval(days => s.n % 15 + 1, hours => 2) AS at) w
WHERE s.n BETWEEN 2 AND 21;

-- last contact dates follow the activities (notes do not count as contact)
UPDATE account a SET last_activity_at = x.last_at
FROM (SELECT account_id, max(occurred_at) AS last_at FROM activity WHERE type <> 'NOTE' GROUP BY account_id) x
WHERE a.id = x.account_id;
UPDATE person p SET last_contact_at = x.last_at
FROM (SELECT person_id, max(occurred_at) AS last_at FROM activity WHERE type <> 'NOTE' AND person_id IS NOT NULL GROUP BY person_id) x
WHERE p.id = x.person_id;

-- columns: n, title, due (days from today), owner, account, person, job order, done
INSERT INTO next_action (id, title, due_on, owner_id, account_id, person_id, job_order_id, done, done_at,
                         created_at, created_by, updated_at, updated_by)
SELECT pg_temp.uid(9, v.n), v.title, current_date + v.due, pg_temp.uid(1, v.owner),
       CASE WHEN v.acct IS NOT NULL THEN pg_temp.uid(2, v.acct) END,
       CASE WHEN v.person IS NOT NULL THEN pg_temp.uid(3, v.person) END,
       CASE WHEN v.job IS NOT NULL THEN pg_temp.uid(5, v.job) END,
       v.done, CASE WHEN v.done THEN now() - make_interval(days => greatest(-v.due - 1, 0)) END,
       now() - interval '15 days', pg_temp.uid(1, v.owner), now(), pg_temp.uid(1, v.owner)
FROM (VALUES
 (1,  'Follow up on the proposal',                       -6, 3, 7,    7,    NULL, false),
 (2,  'Call back after the budget meeting',              -3, 2, 8,    8,    NULL, false),
 (3,  'Share the market map for the CFO search',         -2, 3, 13,   13,   4,    false),
 (4,  'Collect interview feedback from the client',      -4, 3, 15,   15,   6,    false),
 (5,  'Ask the candidate for two references',            -1, 2, 14,   43,   5,    false),
 (6,  'Reply to the data-subject request',               -8, 2, NULL, 33,   NULL, false),
 (7,  'Prepare the shortlist presentation',               0, 3, 14,   NULL, 5,    false),
 (8,  'Schedule the client interviews',                   0, 3, 16,   NULL, 7,    false),
 (9,  'Call the candidate about the offer terms',         0, 2, 17,   56,   8,    false),
 (10, 'Intro call with the new lead',                     0, 3, 4,    4,    NULL, false),
 (11, 'Send consent e-mails to new candidates',           0, 1, NULL, 39,   NULL, false),
 (12, 'Prepare the kick-off meeting',                     0, 3, 11,   NULL, 1,    false),
 (13, 'Send the engagement letter',                       1, 3, 9,    9,    NULL, false),
 (14, 'Visit the plant',                                  2, 3, 12,   12,   NULL, false),
 (15, 'Review the Q4 hiring plan',                        3, 2, 19,   19,   NULL, false),
 (16, 'Check in with the on-hold client',                 4, 3, 21,   21,   NULL, false),
 (17, 'Quarterly business review',                        7, 2, 18,   18,   NULL, false),
 (18, 'Re-open the conversation about hiring',           10, 3, 22,   22,   NULL, false),
 (19, 'Annual fee review',                               14, 2, 11,   NULL, NULL, false),
 (20, 'Prepare the proposal draft',                       5, 3, 3,    3,    NULL, false),
 (21, 'Send the NDA',                                   -10, 3, 16,   16,   NULL, true),
 (22, 'Intro call',                                      -7, 3, 2,    2,    NULL, true),
 (23, 'Collect the job description',                     -3, 3, 11,   NULL, 3,    true),
 (24, 'Send the proposal',                              -12, 2, 10,   10,   NULL, true)
) AS v(n, title, due, owner, acct, person, job, done);
