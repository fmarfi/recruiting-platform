-- 03: 80 people (Turkish and English names), employment history, 30 account contacts, network links.
-- Person id uid(3, n): 1-30 CLIENT_CONTACT (n = account n for 1-25; 26-30 second contacts of accounts 11-15),
-- 31-64 CANDIDATE, 65-74 EXECUTIVE, 75-79 REFERRER, 80 CANDIDATE already anonymised.
-- Contact id uid(4, n) = contact of person n. Network link ids uid(11, n).
CREATE TEMP TABLE p_seed AS
WITH nm AS (SELECT
  ARRAY['Ayşe','Mehmet','Zeynep','Emre','Elif','Can','Selin','Burak','Deniz','Ece','Kerem','Merve','Onur','Gizem','Tolga','Naz','Cem','Defne','Serkan','Pınar',
        'James','Olivia','Daniel','Emma','Thomas','Sophie','Michael','Laura','Henry','Grace','Oliver','Clara','Samuel','Alice','Lucas','Nora','Adam','Julia','Victor','Hannah'] AS f,
  ARRAY['Yılmaz','Kaya','Demir','Şahin','Çelik','Aydın','Özdemir','Arslan','Doğan','Kılıç','Aksoy','Erdem','Güneş','Polat','Tekin','Yıldız','Aslan','Bulut','Çetin','Korkmaz',
        'Carter','Hughes','Bennett','Fischer','Morgan','Ellis','Parker','Reed','Walker','Hayes','Foster','Gray','Hunt','Lane','Marsh','Nash','Pratt','Quinn','Shaw','Wells'] AS l)
SELECT g.n,
       CASE WHEN g.n <= 30 THEN 'CLIENT_CONTACT' WHEN g.n <= 64 THEN 'CANDIDATE' WHEN g.n <= 74 THEN 'EXECUTIVE'
            WHEN g.n <= 79 THEN 'REFERRER' ELSE 'CANDIDATE' END AS type,
       nm.f[(g.n * 11) % 40 + 1] AS first_name,
       nm.l[(g.n * 9 + (g.n / 40) * 3) % 40 + 1] AS last_name
FROM generate_series(1, 80) AS g(n) CROSS JOIN nm;

-- derived columns; consent_status mix and retention flags are decided here and reused by 08_privacy.sql
CREATE TEMP TABLE p_full AS
WITH emp AS (SELECT ARRAY['Ekinoks Holding','Vesta Industries','Kıyıbaşı Teknoloji','Mercury Foods','Bozkır Yapı','Alder & Wray',
                         'Sarıçam Otomotiv','Nimbus Logistics','Delta Vadisi Enerji','Orchid Pharma','Tepeören Gıda','Grayfield Retail'] AS c)
SELECT p.n, p.type, p.first_name || ' ' || p.last_name AS name, p.first_name, p.last_name,
       a.n AS acct_n,
       CASE WHEN p.n <= 30 THEN a.name ELSE emp.c[p.n % 12 + 1] END AS company,
       CASE p.type
         WHEN 'CLIENT_CONTACT' THEN (ARRAY['HR Director','Chief Executive Officer','Head of Talent Acquisition','Chief Financial Officer','Managing Director','Chief Operating Officer','İnsan Kaynakları Müdürü','Genel Müdür'])[p.n % 8 + 1]
         WHEN 'CANDIDATE' THEN (ARRAY['Finance Director','Supply Chain Director','Head of Risk','VP Engineering','Operations Director','General Manager','Commercial Director','Plant Manager','Head of Wind Operations','E-Commerce Director'])[p.n % 10 + 1]
         WHEN 'EXECUTIVE' THEN (ARRAY['Chief Executive Officer','Chairman','Managing Partner','Board Member'])[p.n % 4 + 1]
         ELSE (ARRAY['Partner at a law firm','Industry association secretary','Retired CEO','University professor'])[p.n % 4 + 1] END AS title,
       (ARRAY['İstanbul','Ankara','İzmir','Bursa','Antalya','Kocaeli','London','Berlin'])[p.n % 8 + 1] AS city,
       CASE WHEN p.n <= 30 THEN pg_temp.slug(a.name) ELSE 'mail' END AS dom,
       CASE p.type
         WHEN 'CLIENT_CONTACT' THEN 'Main contact at the client; met during the first call.'
         WHEN 'CANDIDATE' THEN (ARRAY['Met at an industry conference','Referred by a former colleague','Approached directly by the consultant','Long-standing contact of the firm'])[p.n % 4 + 1]
         WHEN 'EXECUTIVE' THEN 'Senior network contact, advises on market and talent.'
         ELSE 'Refers candidates and clients to the firm.' END AS how_we_know,
       CASE WHEN p.n % 7 = 0 THEN 'Prefers calls after 17:00.' WHEN p.n % 11 = 0 THEN 'Open to relocation. Notice period: three months.' END AS notes,
       CASE WHEN p.n % 7 = 0 THEN 1 WHEN p.n % 2 = 0 THEN 2 ELSE 3 END AS owner,
       CASE WHEN p.n IN (12, 33, 41, 66) THEN now() - interval '23 months 10 days'
            WHEN p.n = 80 THEN now() - interval '25 months'
            ELSE now() - make_interval(days => 30 + (p.n * 17) % 400) END AS created_at,
       (ARRAY['NOT_SENT','SENT','GIVEN','GIVEN','GIVEN','REFUSED','WITHDRAWN','NOT_SENT'])[p.n % 8 + 1] AS consent_status,
       CASE WHEN p.n IN (12, 33, 41, 66) THEN 'FLAGGED' WHEN p.n = 80 THEN 'ANONYMISED' ELSE 'ACTIVE' END AS retention_status
FROM p_seed p CROSS JOIN emp
LEFT JOIN acct_seed a ON a.n = CASE WHEN p.n <= 25 THEN p.n WHEN p.n <= 30 THEN p.n - 15 END;

INSERT INTO person (id, name, type, title, company, email, phone, city, linkedin_url, how_we_know, notes, owner_id,
                    last_contact_at, consent_status, retention_status, anonymised_at, created_at, created_by, updated_at, updated_by)
SELECT pg_temp.uid(3, n), name, type, title, company,
       pg_temp.slug(first_name) || '.' || pg_temp.slug(last_name) || n || '@' || dom || '.example',
       '+90 555 ' || lpad(((n * 37) % 1000)::text, 3, '0') || ' ' || lpad(((n * 91) % 100)::text, 2, '0') || ' ' || lpad(((n * 53) % 100)::text, 2, '0'),
       city,
       CASE WHEN type <> 'CLIENT_CONTACT' AND n % 3 <> 0
            THEN 'https://www.linkedin.com/in/' || pg_temp.slug(first_name) || '-' || pg_temp.slug(last_name) || '-' || n END,
       how_we_know, notes, pg_temp.uid(1, owner),
       CASE WHEN n % 6 <> 0 THEN now() - make_interval(days => (n * 5) % 90 + 3) END,
       consent_status, retention_status, CASE WHEN n = 80 THEN now() - interval '2 days' END,
       created_at, pg_temp.uid(1, owner), created_at, pg_temp.uid(1, owner)
FROM p_full;

-- person 80 was anonymised after a deletion request: personal data scrubbed
UPDATE person SET name = '[Anonymised]', title = NULL, company = NULL, email = NULL, phone = NULL, city = NULL,
                  linkedin_url = NULL, how_we_know = NULL, notes = NULL
WHERE id = pg_temp.uid(3, 80);

-- employment history: current role (sort 0) and a previous role (sort 1); none for the anonymised person
INSERT INTO person_employment (id, person_id, company, title, start_date, end_date, sort_order)
SELECT pg_temp.uid(12, n * 10 + 1), pg_temp.uid(3, n), company, title, current_date - ((n * 37) % 1500 + 200), NULL, 0
FROM p_full WHERE n <> 80;
INSERT INTO person_employment (id, person_id, company, title, start_date, end_date, sort_order)
SELECT pg_temp.uid(12, n * 10 + 2), pg_temp.uid(3, n),
       (ARRAY['Ekinoks Holding','Vesta Industries','Kıyıbaşı Teknoloji','Mercury Foods','Bozkır Yapı','Alder & Wray'])[(n * 5) % 6 + 1],
       'Senior Manager', current_date - ((n * 37) % 1500 + 200) - 1500, current_date - ((n * 37) % 1500 + 200) - 1, 1
FROM p_full WHERE n <> 80;

-- contacts: persons 1-25 are the primary contact of accounts 1-25; 26-29 extra contacts; 30 has left (ended)
INSERT INTO contact (id, account_id, person_id, role, is_primary, ended_on, created_at, created_by, updated_at, updated_by)
SELECT pg_temp.uid(4, p.n), pg_temp.uid(2, p.acct_n), pg_temp.uid(3, p.n),
       (ARRAY['HR Director','CEO / General Manager','Head of Talent','Finance lead','Hiring manager'])[p.n % 5 + 1],
       p.n <= 25, CASE WHEN p.n = 30 THEN current_date - 40 END,
       now() - make_interval(days => a.opened_days - 2), pg_temp.uid(1, a.owner),
       now() - make_interval(days => a.opened_days - 2), pg_temp.uid(1, a.owner)
FROM p_full p JOIN acct_seed a ON a.n = p.acct_n WHERE p.n <= 30;

-- network links: a chain through the candidates, plus hand-picked ones
INSERT INTO network_link (id, from_person_id, to_person_id, type, note, created_at, created_by, updated_at, updated_by)
SELECT pg_temp.uid(11, n), pg_temp.uid(3, n), pg_temp.uid(3, n + 1),
       (ARRAY['KNOWS','WORKED_WITH','INTRODUCED','REFERRED'])[n % 4 + 1],
       CASE n % 4 WHEN 1 THEN 'Worked together on a turnaround project.' WHEN 2 THEN 'Introduced at a sector dinner.' END,
       now() - make_interval(days => 20 + n), pg_temp.uid(1, 3), now() - make_interval(days => 20 + n), pg_temp.uid(1, 3)
FROM generate_series(31, 64) AS g(n);
INSERT INTO network_link (id, from_person_id, to_person_id, type, note, created_at, created_by, updated_at, updated_by)
SELECT pg_temp.uid(11, 100 + k), pg_temp.uid(3, f), pg_temp.uid(3, t), ty, nt,
       now() - make_interval(days => 10 + k), pg_temp.uid(1, 2), now() - make_interval(days => 10 + k), pg_temp.uid(1, 2)
FROM (VALUES (1, 66, 67, 'REPORTS_TO', 'Reports to the CEO since 2024.'),
             (2, 68, 67, 'REPORTS_TO', NULL),
             (3, 75, 31, 'REFERRED',   'Recommended for supply chain roles.'),
             (4, 76, 45, 'INTRODUCED', NULL),
             (5, 77, 65, 'KNOWS',      'Sit on the same industry board.'),
             (6, 78, 50, 'REFERRED',   NULL),
             (7, 1,  65, 'KNOWS',      'Client HR director knows the executive from a conference.')) AS v(k, f, t, ty, nt);
