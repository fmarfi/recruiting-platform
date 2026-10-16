-- 02: 25 fictional client accounts covering every account status
-- (6 LEAD, 4 PROPOSAL_SENT, 9 ACTIVE, 3 ON_HOLD, 3 CLOSED). Id: uid(2, n).
-- opened_days / status_days are "days ago"; the timeline in 07 is derived from them.
CREATE TEMP TABLE acct_seed (n int, name text, sector text, city text, status text, owner int,
                             opened_days int, status_days int, slug text);
INSERT INTO acct_seed (n, name, sector, city, status, owner, opened_days, status_days) VALUES
 (1,  'Kuzeyışık Enerji A.Ş.',          'Energy',          'Ankara',   'LEAD',          3,  20,  20),
 (2,  'Mavi Vadi Gıda',                 'Food & Beverage', 'İzmir',    'LEAD',          3,  14,  14),
 (3,  'Aurelia Pharma',                 'Pharma',          'İstanbul', 'LEAD',          2,  35,  35),
 (4,  'Brightvale Software',            'Software',        'İstanbul', 'LEAD',          3,   9,   9),
 (5,  'Tamarind Retail Group',          'Retail',          'İstanbul', 'LEAD',          2,  50,  50),
 (6,  'Çınaraltı Tekstil',              'Textile',         'Bursa',    'LEAD',          3,  60,  60),
 (7,  'Ormanlı Lojistik',               'Logistics',       'Kocaeli',  'PROPOSAL_SENT', 3,  70,  12),
 (8,  'Silverline Telecom',             'Telecom',         'İstanbul', 'PROPOSAL_SENT', 2,  80,  18),
 (9,  'Güneybatı Sağlık Grubu',         'Healthcare',      'Antalya',  'PROPOSAL_SENT', 3,  55,   6),
 (10, 'Harbor & Finch Capital',         'Finance',         'İstanbul', 'PROPOSAL_SENT', 2,  45,  25),
 (11, 'Demirağ Yan Sanayi',             'Automotive',      'Bursa',    'ACTIVE',        3, 300, 120),
 (12, 'Northgate Manufacturing',        'Manufacturing',   'Kocaeli',  'ACTIVE',        2, 250, 100),
 (13, 'Lale Yapı Holding',              'Construction',    'Ankara',   'ACTIVE',        3, 220,  90),
 (14, 'Quartzen Finansal Hizmetler',    'Finance',         'İstanbul', 'ACTIVE',        2, 400, 200),
 (15, 'Rüzgârgülü Enerji',              'Energy',          'İzmir',    'ACTIVE',        3, 180,  75),
 (16, 'Bosphorus Bytes',                'Software',        'İstanbul', 'ACTIVE',        3, 160,  60),
 (17, 'Taze Anadolu Gıda',              'Food & Beverage', 'Konya',    'ACTIVE',        2, 140,  50),
 (18, 'Meridian Health Labs',           'Healthcare',      'Ankara',   'ACTIVE',        3, 200,  80),
 (19, 'Kervan Perakende',               'Retail',          'İstanbul', 'ACTIVE',        2, 130,  40),
 (20, 'Poyraz Elektronik',              'Manufacturing',   'Ankara',   'ON_HOLD',       3, 210,  30),
 (21, 'Cedarwood Hospitality',          'Tourism',         'Antalya',  'ON_HOLD',       2, 190,  45),
 (22, 'Işıkkent Eğitim Kurumları',      'Education',       'İzmir',    'ON_HOLD',       3, 170,  20),
 (23, 'Tuzlu Deniz Denizcilik',         'Logistics',       'İstanbul', 'CLOSED',        3, 500, 150),
 (24, 'Pinewood Packaging',             'Manufacturing',   'Bursa',    'CLOSED',        2, 420,  90),
 (25, 'Akçalı Madencilik',              'Mining',          'Ankara',   'CLOSED',        3, 380, 200);
UPDATE acct_seed SET slug = pg_temp.slug(name);

INSERT INTO account (id, name, sector, city, website, notes, owner_id, status, status_changed_at, opened_on,
                     created_at, created_by, updated_at, updated_by)
SELECT pg_temp.uid(2, n), name, sector, city, 'https://www.' || left(slug, 20) || '.example',
       CASE WHEN n % 4 = 0 THEN 'Synthetic demo account. Prefers e-mail first, phone second.'
            WHEN n % 5 = 0 THEN 'Synthetic demo account. Decision maker is the board; allow two weeks for feedback.' END,
       pg_temp.uid(1, owner), status, now() - make_interval(days => status_days),
       current_date - opened_days, now() - make_interval(days => opened_days), pg_temp.uid(1, owner),
       now() - make_interval(days => status_days), pg_temp.uid(1, owner)
FROM acct_seed;
