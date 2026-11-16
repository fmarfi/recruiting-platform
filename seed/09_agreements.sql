-- 09: 15 fee agreements: terms of business on client accounts (jobless) and engagement letters on job orders,
-- mixed statuses, with fee schedules. Ids: agreement uid(30, n), fee schedule item uid(31, n*10 + seq).
-- acct refers to acct_seed (02), job to j_seed (04). The signatory is the primary contact of the account (uid(4, acct)).
CREATE TEMP TABLE ag_seed (n int, acct int, job int, kind text, status text, title text, model text, pct numeric,
                           est numeric, flat numeric, cur text, signed_days int, valid_days int, excl text);
INSERT INTO ag_seed VALUES
 (1,  11, NULL, 'TERMS_OF_BUSINESS',  'SIGNED',     'Terms of business 2026',            'RETAINED_THIRDS',         25, NULL,   NULL,  'TRY', 200, 165, 'NON_EXCLUSIVE'),
 (2,  12, NULL, 'TERMS_OF_BUSINESS',  'SIGNED',     'Terms of business 2026',            'PERCENT_FIRST_YEAR_CASH', 22, NULL,   NULL,  'EUR', 170, 195, 'NON_EXCLUSIVE'),
 (3,  13, NULL, 'TERMS_OF_BUSINESS',  'SIGNED',     'Framework terms, finance searches', 'RETAINED_THIRDS',         28, NULL,   NULL,  'TRY', 150, 215, 'EXCLUSIVE'),
 (4,  14, NULL, 'TERMS_OF_BUSINESS',  'SIGNED',     'Terms of business 2026',            'HYBRID',                  20, NULL,   NULL,  'EUR', 300,  65, 'NON_EXCLUSIVE'),
 (5,  15, NULL, 'TERMS_OF_BUSINESS',  'SENT',       'Terms of business 2027',            'RETAINED_THIRDS',         25, NULL,   NULL,  'TRY', NULL, 365, 'NON_EXCLUSIVE'),
 (6,  7,  NULL, 'TERMS_OF_BUSINESS',  'DRAFT',      'Draft terms, logistics',            'PERCENT_FIRST_YEAR_CASH', 24, NULL,   NULL,  'TRY', NULL, 365, NULL),
 (7,  23, NULL, 'TERMS_OF_BUSINESS',  'EXPIRED',    'Terms of business 2025',            'PERCENT_FIRST_YEAR_CASH', 23, NULL,   NULL,  'TRY', 420, -20, 'NON_EXCLUSIVE'),
 (8,  24, NULL, 'TERMS_OF_BUSINESS',  'TERMINATED', 'Terms of business 2025',            'FLAT_FEE',                NULL, NULL, 150000, 'TRY', 380, 100, NULL),
 (9,  11, 3,    'ENGAGEMENT_LETTER',  'SIGNED',     'Engagement letter, supply chain',   'RETAINED_THIRDS',         25, 240000, NULL,  'TRY', 40,  325, 'EXCLUSIVE'),
 (10, 13, 4,    'ENGAGEMENT_LETTER',  'SIGNED',     'Engagement letter, CFO search',     'RETAINED_THIRDS',         28, 360000, NULL,  'TRY', 46,  319, 'EXCLUSIVE'),
 (11, 14, 5,    'ENGAGEMENT_LETTER',  'SIGNED',     'Engagement letter, head of risk',   'RETAINED_THIRDS',         20, 40000,  NULL,  'EUR', 68,  297, 'NON_EXCLUSIVE'),
 (12, 15, 6,    'ENGAGEMENT_LETTER',  'SIGNED',     'Engagement letter, wind operations','RETAINED_THIRDS',         25, 230000, NULL,  'TRY', 82,  283, 'EXCLUSIVE'),
 (13, 23, 9,    'ENGAGEMENT_LETTER',  'SIGNED',     'Engagement letter, commercial director', 'RETAINED_THIRDS',    25, 235000, NULL,  'TRY', 198, 167, 'EXCLUSIVE'),
 (14, 11, 1,    'ENGAGEMENT_LETTER',  'SENT',       'Engagement letter, plant manager',  'RETAINED_THIRDS',         25, 220000, NULL,  'TRY', NULL, 365, 'EXCLUSIVE'),
 (15, 20, 2,    'ENGAGEMENT_LETTER',  'DRAFT',      'Engagement letter, head of R&D',    'RETAINED_THIRDS',         26, 250000, NULL,  'TRY', NULL, 365, NULL);

INSERT INTO agreement (id, account_id, job_order_id, kind, status, title, signed_on, valid_from, valid_until, language,
                       governing_law, exclusivity, fee_model, fee_percent, fee_basis, flat_fee_amount, estimated_fee,
                       fee_minimum, currency, payment_terms_days, guarantee_months, guarantee_start_basis,
                       guarantee_terms, expense_policy, off_limits_months, confidentiality_terms,
                       signatory_contact_id, billing_account_id, notes, created_at, created_by, updated_at, updated_by)
SELECT pg_temp.uid(30, g.n), pg_temp.uid(2, g.acct), CASE WHEN g.job IS NOT NULL THEN pg_temp.uid(5, g.job) END,
       g.kind, g.status, g.title,
       CASE WHEN g.signed_days IS NOT NULL THEN current_date - g.signed_days END,
       CASE WHEN g.signed_days IS NOT NULL THEN current_date - g.signed_days ELSE current_date END,
       current_date + g.valid_days, 'EN', 'Turkish law, Istanbul courts', g.excl, g.model, g.pct,
       CASE WHEN g.model = 'FLAT_FEE' THEN ARRAY[]::varchar[]
            ELSE ARRAY['BASE', 'TARGET_BONUS']::varchar[] END,
       g.flat, g.est, CASE WHEN g.pct IS NOT NULL THEN 60000 END, g.cur, 30, 6, 'START_DATE',
       'Replacement search once if the placed candidate leaves within the guarantee period, unless dismissed for cause.',
       'Pre-approved travel and assessment costs are invoiced at cost.', 12,
       'Both parties keep the search and the candidates confidential.',
       pg_temp.uid(4, g.acct), pg_temp.uid(2, g.acct),
       'Synthetic demo agreement.',
       now() - make_interval(days => coalesce(g.signed_days, 7) + 5), pg_temp.uid(1, a.owner),
       now() - make_interval(days => coalesce(g.signed_days, 7)), pg_temp.uid(1, a.owner)
FROM ag_seed g JOIN acct_seed a ON a.n = g.acct;

-- Fee schedule: thirds for the retained engagements (instalments fall due as the search advances), none for framework
-- terms, which are templates.
CREATE TEMP TABLE item_seed (seq int, label text, trig text, pct numeric);
INSERT INTO item_seed VALUES (1, 'Retainer 1/3 (at signing)', 'SIGNING', 33.33),
                             (2, 'Retainer 2/3 (shortlist delivered)', 'SHORTLIST_DELIVERED', 33.33),
                             (3, 'Success fee 3/3 (offer accepted)', 'OFFER_ACCEPTED', 33.34);

INSERT INTO fee_schedule_item (id, agreement_id, seq, label, trigger, percent, due_days, vat_applies, invoice_state,
                               triggered_at, due_on, invoiced_on, invoiced_amount, paid_on, created_at, created_by,
                               updated_at, updated_by)
SELECT pg_temp.uid(31, g.n * 10 + s.seq), pg_temp.uid(30, g.n), s.seq, s.label, s.trig, s.pct, 30, true, st.state,
       CASE WHEN st.state <> 'NOT_DUE' THEN now() - make_interval(days => st.age) END,
       CASE WHEN st.state <> 'NOT_DUE' THEN current_date - st.age + 30 END,
       CASE WHEN st.state IN ('INVOICED', 'PAID') THEN current_date - st.age + 2 END,
       CASE WHEN st.state IN ('INVOICED', 'PAID') THEN round(g.est * s.pct / 100, 2) END,
       CASE WHEN st.state = 'PAID' THEN current_date - st.age + 20 END,
       now() - make_interval(days => coalesce(g.signed_days, 7) + 5), pg_temp.uid(1, a.owner),
       now() - make_interval(days => 3), pg_temp.uid(1, a.owner)
FROM ag_seed g
JOIN item_seed s ON true
JOIN acct_seed a ON a.n = g.acct
JOIN j_seed j ON j.n = g.job
CROSS JOIN LATERAL (
    SELECT CASE
             WHEN g.status <> 'SIGNED' THEN 'NOT_DUE'
             WHEN s.seq = 1 THEN CASE WHEN g.signed_days > 60 THEN 'PAID' ELSE 'DUE' END
             WHEN s.seq = 2 AND j.status IN ('SHORTLIST_SENT', 'INTERVIEWING', 'OFFER', 'FILLED') THEN 'INVOICED'
             WHEN s.seq = 3 AND j.status = 'FILLED' THEN 'PAID'
             ELSE 'NOT_DUE' END AS state,
           CASE WHEN s.seq = 1 THEN g.signed_days WHEN s.seq = 2 THEN 20 ELSE 15 END AS age) st
WHERE g.job IS NOT NULL AND g.model = 'RETAINED_THIRDS';

-- Framework terms carry the same thirds as templates (never due).
INSERT INTO fee_schedule_item (id, agreement_id, seq, label, trigger, percent, due_days, vat_applies, invoice_state,
                               created_at, created_by, updated_at, updated_by)
SELECT pg_temp.uid(31, g.n * 10 + s.seq), pg_temp.uid(30, g.n), s.seq, s.label, s.trig, s.pct, 30, true, 'NOT_DUE',
       now() - make_interval(days => coalesce(g.signed_days, 7) + 5), pg_temp.uid(1, a.owner),
       now() - make_interval(days => coalesce(g.signed_days, 7)), pg_temp.uid(1, a.owner)
FROM ag_seed g JOIN item_seed s ON true JOIN acct_seed a ON a.n = g.acct
WHERE g.job IS NULL AND g.model = 'RETAINED_THIRDS';
