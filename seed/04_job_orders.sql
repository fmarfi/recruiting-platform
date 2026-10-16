-- 04: 10 job orders (every status), 38 pipeline applications over every stage (rejections with reasons),
-- and the account_open_job projection with account.open_job_count.
-- Ids: job order uid(5, n), application uid(6, n). Person numbers in app_seed refer to 03_people.sql.
CREATE TEMP TABLE j_seed (n int, acct int, title text, location text, seniority text, status text, owner int,
                          opened_days int, status_days int, openings int);
INSERT INTO j_seed VALUES
 (1,  11, 'Plant Manager',                      'Bursa',    'Director',   'OPEN',           3,   6,   6, 1),
 (2,  20, 'Head of R&D (Ar-Ge Direktörü)',      'Ankara',   'Director',   'OPEN',           3,  25,  25, 1),
 (3,  11, 'Supply Chain Director',              'Bursa',    'Director',   'SOURCING',       3,  40,  28, 1),
 (4,  13, 'Chief Financial Officer',            'Ankara',   'C-level',    'SOURCING',       2,  48,  30, 1),
 (5,  14, 'Head of Risk',                       'İstanbul', 'VP',         'SHORTLIST_SENT', 2,  70,  12, 1),
 (6,  15, 'Wind Operations Director',           'İzmir',    'Director',   'INTERVIEWING',   3,  85,   9, 1),
 (7,  16, 'VP Engineering',                     'İstanbul', 'VP',         'INTERVIEWING',   3,  75,   7, 1),
 (8,  17, 'General Manager',                    'Konya',    'C-level',    'OFFER',          2, 110,   3, 1),
 (9,  23, 'Commercial Director',                'İstanbul', 'Director',   'FILLED',         3, 200, 160, 1),
 (10, 19, 'Head of E-Commerce',                 'İstanbul', 'VP',         'CANCELLED',      2,  90,  45, 1);

INSERT INTO job_order (id, account_id, title, location, seniority, description, fee_terms, openings, status,
                       status_changed_at, owner_id, opened_on, closed_on, created_at, created_by, updated_at, updated_by)
SELECT pg_temp.uid(5, n), pg_temp.uid(2, acct), title, location, seniority,
       'Synthetic demo role: ' || title || ' reporting to the CEO. Scope, targets and team size agreed in the kick-off meeting.',
       'Retained search: one third at kick-off, one third at shortlist, one third at placement (25% of first-year cash compensation).',
       openings, status, now() - make_interval(days => status_days), pg_temp.uid(1, owner),
       current_date - opened_days, CASE WHEN status IN ('FILLED', 'CANCELLED') THEN current_date - status_days END,
       now() - make_interval(days => opened_days), pg_temp.uid(1, owner),
       now() - make_interval(days => status_days), pg_temp.uid(1, owner)
FROM j_seed;

CREATE TEMP TABLE app_seed (n int, job int, person int, stage text, reason text);
INSERT INTO app_seed VALUES
 (1, 2, 40, 'SOURCED', NULL),
 (2, 3, 31, 'SOURCED', NULL), (3, 3, 32, 'SOURCED', NULL), (4, 3, 33, 'SOURCED', NULL), (5, 3, 34, 'SCREENED', NULL),
 (6, 3, 35, 'REJECTED', 'Salary expectation above the budget range'),
 (7, 4, 36, 'SOURCED', NULL), (8, 4, 37, 'SOURCED', NULL), (9, 4, 38, 'SCREENED', NULL),
 (10, 4, 65, 'REJECTED', 'Lacks listed-company experience'),
 (11, 5, 39, 'SOURCED', NULL), (12, 5, 41, 'SCREENED', NULL), (13, 5, 42, 'PRESENTED', NULL), (14, 5, 43, 'PRESENTED', NULL),
 (15, 5, 44, 'PRESENTED', NULL), (16, 5, 45, 'REJECTED', 'Not open to relocating to Istanbul'),
 (17, 6, 46, 'PRESENTED', NULL), (18, 6, 47, 'CLIENT_INTERVIEW', NULL), (19, 6, 48, 'CLIENT_INTERVIEW', NULL),
 (20, 6, 49, 'CLIENT_INTERVIEW', NULL), (21, 6, 50, 'REJECTED', 'Withdrew: accepted another offer'),
 (22, 6, 66, 'REJECTED', 'Client felt the profile was too junior'),
 (23, 7, 51, 'CLIENT_INTERVIEW', NULL), (24, 7, 52, 'CLIENT_INTERVIEW', NULL), (25, 7, 53, 'PRESENTED', NULL),
 (26, 7, 54, 'PRESENTED', NULL), (27, 7, 55, 'REJECTED', 'Failed the technical case'),
 (28, 8, 56, 'OFFER', NULL), (29, 8, 57, 'CLIENT_INTERVIEW', NULL),
 (30, 8, 58, 'REJECTED', 'Client chose another finalist'), (31, 8, 59, 'REJECTED', 'Notice period too long'),
 (32, 8, 67, 'PRESENTED', NULL),
 (33, 9, 60, 'PLACED', NULL), (34, 9, 61, 'REJECTED', 'Client chose another finalist'),
 (35, 9, 62, 'REJECTED', 'Compensation mismatch'), (36, 9, 63, 'REJECTED', 'Withdrew: counter-offer from current employer'),
 (37, 10, 64, 'SCREENED', NULL), (38, 10, 31, 'SOURCED', NULL);

INSERT INTO application (id, job_order_id, person_id, stage, rejection_reason, stage_changed_at,
                         created_at, created_by, updated_at, updated_by)
SELECT pg_temp.uid(6, s.n), pg_temp.uid(5, s.job), pg_temp.uid(3, s.person), s.stage, s.reason,
       CASE WHEN s.stage = 'SOURCED' THEN c.created_at
            ELSE least(now() - interval '1 hour', c.created_at + make_interval(days => 2 + s.n % 9)) END,
       c.created_at, pg_temp.uid(1, j.owner), c.created_at, pg_temp.uid(1, j.owner)
FROM app_seed s
JOIN j_seed j ON j.n = s.job
CROSS JOIN LATERAL (SELECT now() - make_interval(days => j.opened_days - 3 - s.n % 10) AS created_at) c;
UPDATE application SET updated_at = stage_changed_at;

-- account_open_job mirrors the job orders; open = any status except FILLED and CANCELLED
INSERT INTO account_open_job (job_order_id, account_id, title, open, updated_at)
SELECT id, account_id, title, status NOT IN ('FILLED', 'CANCELLED'), status_changed_at FROM job_order;
UPDATE account a SET open_job_count = (SELECT count(*) FROM account_open_job o WHERE o.account_id = a.id AND o.open);
