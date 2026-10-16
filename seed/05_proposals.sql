-- 05: 18 proposals in every status (4 SENT, 10 ACCEPTED, 2 REJECTED with reasons, 2 DRAFT). Id: uid(7, n).
-- sent_days / decided_days are "days ago" and line up with the account status history in 02.
CREATE TEMP TABLE prop_seed (n int, acct int, title text, status text, fee numeric, cur text,
                             sent_days int, decided_days int, reason text);
INSERT INTO prop_seed VALUES
 (1,  7,  'Executive search framework, logistics',       'SENT',     180000, 'TRY',  12, NULL, NULL),
 (2,  8,  'Leadership hiring programme 2026',            'SENT',      42000, 'EUR',  18, NULL, NULL),
 (3,  9,  'Clinic network director search',              'SENT',     210000, 'TRY',   6, NULL, NULL),
 (4,  10, 'Senior investment hires',                     'SENT',      38000, 'EUR',  25, NULL, NULL),
 (5,  11, 'Plant leadership search',                     'ACCEPTED', 240000, 'TRY', 210, 122, NULL),
 (6,  12, 'Engineering leadership search',               'ACCEPTED',  36000, 'EUR', 175, 102, NULL),
 (7,  13, 'Finance leadership search',                   'ACCEPTED', 260000, 'TRY', 155,  92, NULL),
 (8,  14, 'Risk and compliance leadership',              'ACCEPTED',  44000, 'EUR', 300, 202, NULL),
 (9,  15, 'Operations leadership search',                'ACCEPTED', 230000, 'TRY', 128,  77, NULL),
 (10, 16, 'Engineering executives',                      'ACCEPTED',  39000, 'EUR', 110,  62, NULL),
 (11, 17, 'General management search',                   'ACCEPTED', 250000, 'TRY',  95,  52, NULL),
 (12, 18, 'Clinical operations search',                  'ACCEPTED',  41000, 'EUR', 140,  82, NULL),
 (13, 19, 'E-commerce leadership search',                'ACCEPTED', 220000, 'TRY',  85,  42, NULL),
 (14, 23, 'Commercial leadership search',                'ACCEPTED', 235000, 'TRY', 380, 270, NULL),
 (15, 24, 'Packaging division heads',                    'REJECTED', 195000, 'TRY', 300, 280, 'Client decided to hire in-house'),
 (16, 6,  'Textile executive search',                    'REJECTED', 175000, 'TRY',  30,  20, 'Fee structure not acceptable to the client'),
 (17, 3,  'Pharma regulatory affairs head',              'DRAFT',     37000, 'EUR', NULL, NULL, NULL),
 (18, 4,  'Software leadership team',                    'DRAFT',     35000, 'EUR', NULL, NULL, NULL);

INSERT INTO proposal (id, account_id, title, terms, fee_amount, currency, status, status_changed_at, status_reason,
                      sent_on, decided_on, owner_id, created_at, created_by, updated_at, updated_by)
SELECT pg_temp.uid(7, p.n), pg_temp.uid(2, p.acct), p.title,
       'Retained search. Fee paid in three instalments. Valid for 30 days after sending.', p.fee, p.cur, p.status,
       now() - make_interval(days => coalesce(p.decided_days, p.sent_days, 7)), p.reason,
       CASE WHEN p.sent_days IS NOT NULL THEN current_date - p.sent_days END,
       CASE WHEN p.decided_days IS NOT NULL THEN current_date - p.decided_days END,
       pg_temp.uid(1, a.owner),
       now() - make_interval(days => coalesce(p.sent_days + 5, 7)), pg_temp.uid(1, a.owner),
       now() - make_interval(days => coalesce(p.decided_days, p.sent_days, 7)), pg_temp.uid(1, a.owner)
FROM prop_seed p JOIN acct_seed a ON a.n = p.acct;
