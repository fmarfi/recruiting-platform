# Executive search domain workflows and client data model

Date: 2026-10-28. Scope: how retained executive-search firms (mid-size, Turkish and international) run the client lifecycle, what data they capture, and what Account and Contact must hold in our platform. Builds on `docs/product/product-plan-phase1.md` and `contracts/schema.md`.

How to read this: **Fact** = stated by a cited source or by our own contract files. **Practice** = common in vendor and firm material, not a standard. **Opinion/proposal** = ours. Most sources are vendor blogs; none is a survey. Turkey-specific legal and tax points come from Turkish sources and are not legal advice; each is marked "verify with counsel / GIB / KVKK".

---

## 0. What exists today and what is missing

Fact, from `contracts/schema.md`:

- `account` holds only: name, sector (free text, required), city, website, notes, owner_id, status (`AccountStatus`, default LEAD), opened_on, tags, plus derived `open_job_count`, `last_activity_at`.
- `contact` is a person in a role at an account: `role` (free text), `is_primary`, `ended_on`. No influence, decision role, reporting line, language, preferred channel or relationship strength (the plan says "relationship strength" and "reporting line" exist; the schema does not).
- `proposal`: title, `terms` (free text), single `fee_amount` + `currency`, status, sent_on, decided_on. `job_order.fee_terms` is also free text.
- `off_limits_rule` exists (account or company name, reason, dates). `candidate_exposure` exists. `activity`, `next_action`, `timeline_entry`, `import_job`, `saved_view` exist.
- Privacy is modelled for `person` (consent, retention, DSR), not for client contacts' lawful basis.

Missing for the client lifecycle (named here, specified below):

1. Legal identity: legal name vs trade name, tax office and tax number (VKN), MERSIS, e-fatura status, billing address.
2. Group/parent hierarchy and subsidiaries; offices and locations as rows.
3. Structured industry (NACE or internal taxonomy), size band, ownership type.
4. Relationship team (owner exists; no secondary consultants, researcher, sponsor partner).
5. Contact attributes: decision role, influence, sentiment, reporting line, buying-committee role.
6. Fee agreement as structured data (terms of business, engagement letter, fee model, retainer instalments, guarantee, payment terms) separate from a single proposal.
7. Off-limits / non-solicitation scope and duration on the agreement, linked to `off_limits_rule`.
8. Exclusivity, confidentiality level per account and per search, conflict-check log.
9. Billing: invoice schedule, invoice triggers, e-fatura/e-arsiv record, payment status. (Product plan: "invoicing triggers; open question".)
10. Documents on accounts (NDA, ToB, engagement letter, signed proposal).
11. BD objects: target-company map for BD, lead source, first-meeting brief, win/loss reason.
12. Placement and guarantee records (start date, base, bonus, first-year cash, guarantee end, replacement search link).
13. Client-facing report log, client contact consent/lawful-basis for marketing and portal use.
14. KPI snapshots (time to shortlist, fee-weighted pipeline).

---

## 1. End-to-end workflow and data captured at each step

Sources for the generic process: [Curran Daly 8 steps](https://currandaly.com/executive-hiring-roadmap-8-steps-retained-search/), [Blue Signal search process](https://bluesignal.com/clients/our-search-process/), [KiTalent process and deliverables](https://kitalent.com/executive-search-process.md), [JRG target company list](https://www.jrgpartners.com/?p=39506). Typical retained cycle: 90 to 120 days to offer, longer for board roles ([Recruiterflow benchmarks](https://recruiterflow.com/blog/time-to-fill-benchmarks/), vendor claim).

### 1.1 Business development (before there is a client)

| Step | What happens | Data captured | Platform object |
| --- | --- | --- | --- |
| Target-company mapping (BD) | Partner or researcher lists companies likely to need senior hires: sector, growth events, leadership change, funding, M&A | company, trigger event, source, owner, priority, off-limits check result | `account` status LEAD/TARGET + `account_signal` (new) |
| Conflict and off-limits check | Before approaching, check current clients, guarantee periods, non-solicit terms | rules hit, who checked, date, decision | `off_limits_rule` + `conflict_check` (new) |
| Outreach and first meeting | Intro through network or cold; meeting with the hiring authority | contact, how introduced (network link), meeting date, attendees, needs heard, urgency, competitors in play | `activity` MEETING, `opportunity` |
| Needs capture | Roles, seniority, location, timing, budget band, confidentiality (replacement of an incumbent?) | roles, rough comp band, confidential flag | `opportunity` + roles |
| Proposal / pitch | Credentials, approach, team, timeline, fee, guarantee | version, fee model, team, validity date | `proposal` (extend) |
| Terms of business and engagement letter | Legal terms signed; engagement letter per search | signed date, signatory, document, fee schedule, guarantee, payment terms, exclusivity, off-limits, confidentiality, governing law | `agreement` + `fee_schedule` (new), `account_document` |
| Won/lost | Decision recorded with reason | outcome, reason, competitor, decided_on | `proposal.status_reason` (exists) + `lost_reason` enum |

### 1.2 Client onboarding

Triggered when the first agreement is signed. Output: a complete client record (Section 3.1). Steps: collect legal and tax data (verify VKN, check e-fatura status), map group and offices, name contacts and decision roles, record agreement terms, set confidentiality level and team access, upload documents, run conflict check, create the account owner and team, schedule the kickoff. Gate: an account cannot move to ACTIVE client until legal name, tax number, billing contact, agreement and owner are present (proposal, see 3.1 validation).

### 1.3 Search assignment lifecycle

| Stage | What happens | Data captured |
| --- | --- | --- |
| Brief / kickoff | Hiring manager, sponsor, HR, reporting line; why the role is open; confidentiality | brief notes, stakeholders, confidential flag, target start date, comp band (encrypted), location, language needs |
| Position profile / specification | Document agreed with client: scope, must-haves, nice-to-haves, culture, selling points | versioned profile document, client approval date |
| Research and mapping | Target companies, orgs, off-limits list, sourcing plan | target company list with state (researched/contacted/exhausted), off-limits list, research hours |
| Outreach | Approach by phone or message; first conversation | contact attempts, response, interest, source channel, consent notice sent (KVKK) |
| Interviews and assessment | Consultant interview, assessment, competency inventory | interview notes, scorecard vs profile, compensation, notice period, mobility |
| Shortlist | 3 to 6 candidates presented with reports; consent needed before sharing | shortlist date (milestone), report PDF, consent state, blind option |
| Client interviews | Scheduling, feedback loops | interview dates, feedback, ratings, rejection reasons |
| References | Before offer | referees, dates, summary, consent to contact referees |
| Offer and negotiation | Package, counter, close risk | offer components (base, bonus, benefits, sign-on), status, expiry |
| Placement | Acceptance, resignation, start date | placed candidate, start date, base, bonus, first-year cash, fee computed |
| Guarantee period | Replacement obligation | guarantee end date, trigger events, replacement search link, off-limits on placed person's employer |

Fact: milestone checkpoints before shortlist (evidence and rationale) and before offer (motivation, references, comp alignment) are described in [KiTalent](https://kitalent.com/executive-search-process.md). The plan's pipeline (Research, Contacted, Screened, Longlist, Shortlist, Client interview, Offer, Placed) already covers candidate stages; missing are search-level milestones (kickoff done, profile approved, shortlist delivered) that drive fees and KPIs.

### 1.4 Fees and invoicing triggers

Practice (vendor sources, not a standard): retained fee is a percentage of the hire's first-year cash compensation, commonly a quarter to a third; base plus target bonus, sometimes including other cash ([Recruiterflow](https://recruiterflow.com/blog/ai-in-retained-search/), [Airswift glossary](https://www.airswift.com/glossary/retained-search)). Payment in three instalments, one-third at engagement, one-third at shortlist, one-third at placement ([Axial Search](https://axialsearch.com/insights/retained-ai-search-fees)). A municipal contract example bills at execution, 30 days later, and at signing of the employment agreement ([Skagway example](https://skagway.org/sites/default/files/fileattachments/clinic_board/meeting/packets/47611/8c._coors_executive_search_placement_agreement_ed_dahl_101322.pdf)); that is a small-government example, not corporate norm. Guarantee/replacement 90 to 180 days is a vendor claim; the municipal example used 36 months. Fact: sources note guarantee triggers and exclusions vary and must be read in the letter.

Invoice triggers the platform should support (proposal): engagement signed (retainer 1), calendar date (retainer 2, e.g. +30 or +60 days), shortlist delivered (milestone), offer accepted, candidate start date, expenses, cancellation or abort fee, replacement (no fee). Each is a row of a fee schedule with condition type, percent or amount, due-days, and an invoice state. Because the plan says "Ne faturası kesiliyor?" is open, build the triggers and invoice-ready flag first; actual e-fatura issue via the firm's accounting software/integrator (see 1.7).

### 1.5 Candidate handling, confidentiality, reporting

- Candidate: one record, many assignments; exposure history (exists); off-limits check at longlist add (exists); no client sees a candidate without valid consent (plan).
- Confidentiality (practice): per-mandate access rights, off-the-record notes excluded from shared reports ([Spott](https://spott.io/resources/crm-for-executive-search-what-search-firms-actually-need), vendor view). Proposal: three levels on account and on search: NORMAL, RESTRICTED (named team only), CONFIDENTIAL (replacement of a named incumbent; hide from timeline and search results for non-team members).
- Client reporting: weekly progress report (companies researched, contacted, longlist size, next steps), shortlist report, ad hoc call notes, closing report. Bullhorn's executive search guide mentions a closing report with candidate, position, salary, start date and credited recruiter ([Bullhorn KB](https://kb.bullhorn.com/bh4sf/Content/BH4SF/Topics/executiveSearchUserGuide.htm)).

### 1.6 KPIs

Source quality is low (vendor blogs); definitions below are our proposal.

| KPI | Definition | Source note |
| --- | --- | --- |
| Time to shortlist | Days from agreement signed (or kickoff) to shortlist delivered; retained benchmark 3 to 6 weeks, claimed 7 to 10 working days on suitable mandates | [KiTalent benchmark](https://kitalent.com/executive-search-time-to-shortlist-benchmark.md), vendor |
| Time to offer / placement | kickoff to offer accepted / start | [Recruiterflow](https://recruiterflow.com/blog/time-to-fill-benchmarks/) 90 to 120 days |
| Fee-weighted pipeline | sum over open proposals and searches of expected fee x stage probability; no source defines it, ours | proposal |
| Conversion | proposal-to-won; longlist-to-shortlist; shortlist-to-client-interview; offer-to-accept; one vendor quotes 77.9% shortlist-to-interview for retained, definition unclear ([Recruiterflow](https://recruiterflow.com/blog/time-to-fill-benchmarks/)) | vendor |
| Fee realisation | invoiced and collected vs contracted | [RecruitBPM](https://recruitbpm.com/blog/data-analytics-for-executive-search-firms) lists fee realisation, placement rate |
| Retention of placements | still in role at 6, 12, 24 months; also feeds guarantee exposure | [Thrive](https://thrivetrm.com/the-top-5-executive-search-kpi-dashboards-for-2026/) / RecruitBPM suggest retention checkpoints |
| Account health | days since last touch, open proposals, active searches (plan) | plan |

### 1.7 Team collaboration

Roles on a search (proposal): sponsor partner (client owner), lead consultant, researcher, associate, finance viewer. Fee credit split (originator, executor, researcher) as percentages summing to 100 per search; used for compensation and leaderboards. Handover record when a consultant leaves: owner reassignment with log. Shared notes with visibility flag (team / partners only / private). @mentions and tasks exist in the plan.

### 1.8 Turkey-specific points

All items below need confirmation by the firm's counsel or accountant; sources are secondary unless stated.

- **KVKK.** Fact (plan, not legal advice): aydinlatma (information notice) applies however data was obtained; explicit consent (acik riza) is a separate step; sharing a CV with a client is a transfer. Sources found: HR practice articles say candidate data should be used only for recruitment and not shared with third parties without the candidate's approval, explicit consent is stressed for special-category data (health, beliefs) and psychological or drug tests when required by the role, and there is no statute fixing how long unhired candidates' data may be kept; a 2-year period appears in company notices as a company choice ([Cott Group HR](https://www.cottgroup.com/en/blog/item/insan-kaynaklari-sureclerinde-kvkk-nin-onemi), [TP Vision notice](https://www.tpvision.com/policy/TPV-Calisan-Adayi-Aydinlatma-Metni-v2.0.pdf), [academic paper](https://uludag.edu.tr/dosyalar/iibfdergi/genel-dokuman/2022_2/asl10_20222duzeltilmis.pdf)). Search firms act as data controller of their candidate pool; the firm's own lawyer decides legal basis per source. Client contacts are also personal data: keep notice and source for them too.
- **VERBIS.** Fact (secondary): registration duty applies above 50 employees or 100 million TL balance sheet (cumulative for balance-sheet bookkeepers), or when the main activity is processing special-category data; the Board decision of 25.12.2025 (2025/2393) clarified how the criteria combine; thresholds can change ([IMER VERBIS test](https://imer.av.tr/kvkk-araclari/verbis-kayit-yukumlulugu-testi), [Cott Group VERBIS](https://www.cottgroup.com/en/blog/item/verbise-kayit-yukumlulugu-istisnasina-iliskin-uygulama-esaslari-netlestirildi)). Product impact: the platform should export the data inventory (categories, purposes, recipients, retention) the firm needs for VERBIS; it does not register on the firm's behalf. Also useful: a flag on client Accounts showing their own VERBIS status is not needed.
- **MERSIS and tax number.** Secondary sources: MERSIS gives each company a 16-digit identifier; its structure is described inconsistently across blogs (one says it starts with 0 plus the tax number, another warns of temporary numbers), so validate length only, not structure ([Parasut](https://www.parasut.com/blog/mersis-numarasi-nedir), [Is Bankasi](https://www.isbank.com.tr/blog/mersis-numarasi-nedir)). Tax ID (VKN) is 10 digits for companies; TCKN is 11 digits for sole proprietors. VKN has a published check-digit algorithm; implement after verifying it against GIB documentation (not verified here).
- **E-fatura / e-arsiv.** Secondary sources conflict on thresholds. They agree: whether a client is a registered e-fatura user is checked by VKN in GIB's registered-user list; registered users must receive e-fatura, others e-arsiv ([Eleman.net](https://www.eleman.net/is-rehberi/isveren-rehberi/isletme-tipine-gore-mukellef-sorgulama-nasil-yapilir-h22500), [Parasut](https://www.parasut.com/blog/e-fatura-sorgulama)). 2026 transition thresholds (about 3 million TL of 2025 gross sales, 1 July 2026) and the removal of the amount limit for e-arsiv from 1 Jan 2026 are claimed by vendor blogs ([Musavirler Kulubu](https://musavirlerkulubu.com.tr/makale/e-fatura-zorunluluk-siniri-2026-gecis-sureci-ve-uyum-rehberi), [Parasut](https://www.parasut.com/blog/e-fatura-ve-e-arsiv-zorunlulugu)); unverified, confirm with GIB. Product impact: store `einvoice_status` (E_FATURA_USER / E_ARSIV / UNKNOWN), the e-fatura alias (posta kutusu etiketi), checked_on; the invoice itself is issued in the accounting system or an integrator.
- **Private employment bureau licence.** Turkish law (Labour Law art. 90, Turkish Employment Agency regulation) regulates private employment bureaus (placement of job-seekers) and prohibits charging job-seekers fees ([Alomaliye FAQ](https://www.alomaliye.com/2015/08/30/ozel-istihdam-burolari-sikca-sorulan-sorular/)). Whether pure retained executive search falls under it was not established. Open question for the firm's counsel; product rule regardless: never record a fee charged to a candidate, and keep fee fields only on client agreements.
- **Language and norms.** Contracts and proposals often exist in Turkish, English, or both; clients often have Turkish and international (group) decision-makers. Store preferred language per contact and per account, and per agreement a governing-language field. Turkish search-firm norms (proposal templates, off-limits wording, guarantee length) were not found in public sources; ask the firm for its own terms of business. Names with i/I, s, g variants need Turkish-aware search (plan already covers).
- **Currency and VAT.** Fees in TRY, EUR or USD; Turkish VAT (KDV) rate and withholding apply on invoices (not researched; accountant to confirm). Proposal: currency per agreement, vat_rate and withholding flag on the fee schedule, not hard-coded.

---

## 2. Gaps in the existing contract (summary)

Existing `account` is a CRM stub. Biggest structural gaps: no legal identity, no agreement/fee schedule objects, no invoice or placement object, no confidentiality level, no conflict-check log, contact attributes too thin, `sector` free text, `city` single value, no account team. New tables proposed in Section 3.3.

---

## 3. Proposed data and screen model

### 3.1 Account (client company) create/edit form

Legend: Req = required; Req* = required before status can become ACTIVE (client signed), optional while LEAD/TARGET. "Shown" = where the value appears: H = account header, O = overview tab, L = list/grid column, P = proposal/agreement, I = invoice/billing tab, R = reports. Everything editable by owner, team member (edit) and manager; fields marked (M) editable by manager/admin only. Compensation-like or contract values are visible only to the account team and managers.

**Section A. Identity**

| Field | Type | Req | Validation | Shown |
| --- | --- | --- | --- | --- |
| trade_name (display name) | text 200 | Req | unique on lower(name) per existing rule; trim; Turkish-aware duplicate warning on fuzzy match | H, L, everywhere |
| legal_name (ticari unvan) | text 300 | Req* | as registered, incl. A.S./Ltd. Sti.; used on invoice | I, P |
| account_type | enum PROSPECT, CLIENT, FORMER_CLIENT, PARTNER, COMPETITOR, TARGET_ONLY | Req | default PROSPECT | H, L |
| status | enum AccountStatus (exists) | Req | transitions guarded; ACTIVE needs Req* fields | H, L |
| status_reason | text 500 | Opt, Req on LOST/DORMANT | | O |
| parent_account_id | FK account | Opt | no cycles; same firm; max depth 5 | O, L (group filter) |
| ultimate_parent_name | text 200 | Opt | when parent is not in system | O |
| group_name | text 200 | Opt | | O, L |
| ownership_type | enum PUBLIC, PRIVATE, FAMILY, PE_BACKED, VC_BACKED, STATE_OWNED, FOREIGN_SUBSIDIARY, NGO | Opt | | O |
| website | url 300 | Opt | https URL; normalised | H |
| logo | image | Opt | png/jpg, 1 MB | H, R |
| short_description | text 500 | Opt | | O |

**Section B. Legal and tax (Turkey-aware)**

| Field | Type | Req | Validation | Shown |
| --- | --- | --- | --- | --- |
| country_of_registration | ISO 3166 code | Req* | default TR | I |
| tax_id_type | enum VKN, TCKN, FOREIGN_TAX_ID | Req* | default VKN when TR | I |
| tax_id | text 20 | Req* | TR VKN: 10 digits plus check-digit algorithm (verify algorithm from GIB before coding); TCKN: 11 digits; foreign: free, 3 to 20 chars; unique per account within firm (warn on duplicate) | I, P |
| tax_office | text 100 | Req* when TR | | I |
| mersis_no | text 16 | Opt | exactly 16 digits (do not validate structure, sources conflict) | I |
| trade_registry_no | text 40 | Opt | | I |
| trade_registry_office | text 100 | Opt | | I |
| einvoice_status | enum E_FATURA_USER, E_ARSIV, UNKNOWN | Req* | default UNKNOWN; "check on GIB list" action | I |
| einvoice_alias | text 100 | Opt, Req if E_FATURA_USER | format urn:mail:...@... (confirm with integrator) | I |
| einvoice_checked_on | date | Auto | set when status confirmed | I |
| vat_exempt / withholding_applies | boolean | Opt | | I |
| billing_address | structured (line1, line2, district, city, postal, country) | Req* | | I |
| billing_email(s) | email list | Req* | valid addresses, at most 5 | I |
| billing_language | enum TR, EN | Opt | default account language | I |
| purchase_order_required | boolean + po_instructions text | Opt | | I |

**Section C. Classification and size**

| Field | Type | Req | Validation | Shown |
| --- | --- | --- | --- | --- |
| industry_primary | taxonomy (NACE Rev.2 section/division or firm list) | Req | replaces free-text `sector`; migrate by mapping | H, L, filters |
| industry_secondary | taxonomy, up to 3 | Opt | | O |
| nace_code | text 7 | Opt | pattern NN.NN(.N) | O |
| employee_band | enum 1-50, 51-250, 251-1000, 1001-5000, 5000+ | Opt | | O, L |
| employee_count_exact | int | Opt | >= 0 | O |
| revenue_band | enum + currency | Opt | | O |
| listed_ticker | text 20 | Opt | | O |
| growth_stage | enum STARTUP, SCALE_UP, MATURE, TURNAROUND | Opt | | O |

**Section D. Locations**

| Field | Type | Req | Validation | Shown |
| --- | --- | --- | --- | --- |
| hq_city, hq_country | text / ISO | Req | replaces `city` | H, L |
| offices[] (child rows: label, type HQ/OFFICE/PLANT/STORE, address, city, country, headcount) | repeating | Opt | at most 200 rows (stores later) | O |
| time_zone | IANA | Opt | default Europe/Istanbul | O |

**Section E. Relationship and team**

| Field | Type | Req | Validation | Shown |
| --- | --- | --- | --- | --- |
| owner_id (relationship owner) | FK app_user | Req | active user | H, L |
| sponsor_partner_id | FK app_user | Opt | | O |
| team[] (user, role: LEAD, CONSULTANT, RESEARCHER, ANALYST, FINANCE_VIEW; edit/view) | repeating | Opt | owner always included | O, access control |
| origination | enum REFERRAL, NETWORK, INBOUND, EVENT, PAST_CLIENT, COLD, OTHER + originator_id + lead_source_note | Req | | O, R (fee credit) |
| relationship_strength | enum COLD, WARM, STRONG | Opt | | H, L |
| last_activity_at, health | derived | read-only | | H, L |
| next_review_on | date | Opt | | O, My day |

**Section F. Needs, culture, preferences**

| Field | Type | Req | Validation | Shown |
| --- | --- | --- | --- | --- |
| hiring_needs_summary | text 2000 | Opt | roles, seniority, volume, annual outlook | O |
| expected_searches_per_year | int | Opt | 0 to 100 | O, forecast |
| typical_roles / functions | multi-select + tags | Opt | | O |
| culture_notes | text 4000 | Opt | internal only; flagged internal | O |
| decision_process_notes | text 2000 | Opt | who signs, approvals, board involvement | O |
| communication_preferences | enum multiple (email, phone, WhatsApp, portal) + cadence enum WEEKLY, BIWEEKLY, MONTHLY | Opt | | O, R |
| preferred_language | enum TR, EN, BOTH | Req | default TR | O, templates |
| reporting_format | enum PDF, PORTAL, BOTH | Opt | | O, R |
| diversity_or_blind_review_required | boolean | Opt | drives blind report default | R |
| competitors[] (account ids / names) | multi | Opt | feeds target mapping, off-limits | O |

**Section G. Commercial terms (default for the account; each search can override)**

These are the structured facts of the current agreement; the signed agreement itself is a document plus `agreement` row (3.3).

| Field | Type | Req | Validation | Shown |
| --- | --- | --- | --- | --- |
| agreement_status | enum NONE, TOB_SENT, TOB_SIGNED, ENGAGEMENT_SIGNED, EXPIRED | Req | | H, O |
| agreement_signed_on, valid_until | date | Req* | until >= signed | O, P |
| signatory_contact_id | FK contact | Opt | | P |
| fee_model | enum RETAINER_THIRDS, RETAINER_CUSTOM, SUCCESS_ONLY, FIXED_FEE, HOURLY, HYBRID | Req* | | O, P, forecasting |
| fee_percent | decimal 5.2 | Req* when percent-based | 0 to 100 | P |
| fee_base | enum BASE_ONLY, BASE_PLUS_TARGET_BONUS, TOTAL_FIRST_YEAR_CASH | Req* | | P |
| fee_minimum, fee_cap | money + currency | Opt | min <= cap | P |
| retainer_schedule[] (trigger, percent or amount, due_days) | repeating | Req* for retainer models | percentages sum to 100 when thirds-style; triggers from enum SIGNING, DATE_OFFSET, SHORTLIST, OFFER_ACCEPTED, START_DATE | P, I |
| expenses_policy | enum INCLUDED, CAPPED_PERCENT, AT_COST + cap | Opt | | P, I |
| currency | ISO 4217 | Req* | TRY, EUR, USD, GBP | P, I |
| payment_terms_days | int | Req* | 0 to 180; default 30 | I |
| vat_rate_percent | decimal | Opt | | I |
| guarantee_months / guarantee_days | int | Req* | 0 to 36; unit explicit | O, P, guarantee tracker |
| guarantee_terms_note | text 1000 | Opt | triggers, exclusions | P |
| exclusivity | enum EXCLUSIVE, NON_EXCLUSIVE, EXCLUSIVE_PER_SEARCH + note | Req* | | O, P |
| cancellation_terms | text 1000 | Opt | abort fee | P |
| non_solicit_months | int | Req* | 0 to 60; practice 12 to 24, firm policy example 12 months after end ([Vanguarde](https://huntscanlon.com/select-guide/vanguarde/)) | O, off-limits |
| non_solicit_scope | enum GROUP, ENTITY, FUNCTION, NAMED_LIST + note | Req* when months > 0 | | O |
| off_limits_for_client (companies the client forbids us to approach) | link to `off_limits_rule` rows | Opt | | O, longlist guard |
| terms_document | file link | Req* | pdf | O |

**Section H. Confidentiality and compliance**

| Field | Type | Req | Validation | Shown |
| --- | --- | --- | --- | --- |
| confidentiality_level | enum NORMAL, RESTRICTED, CONFIDENTIAL | Req | default NORMAL; CONFIDENTIAL limits visibility to team | H badge |
| nda_status, nda_signed_on | enum / date | Opt | | O |
| client_conflict_status | enum CLEAR, REVIEW, BLOCKED + conflict_checked_on, checked_by | Req* | block adding to ACTIVE if BLOCKED without partner override | H |
| data_processing_note | text | Opt | if client sends candidate data to us (role as controller/processor, to be set by counsel) | O |
| marketing_consent_scope | for client contacts see Contact | n/a | | |

**Section I. History and documents (mostly read-only, derived)**

| Field | Type | Req | Notes | Shown |
| --- | --- | --- | --- | --- |
| searches[] and placements[] | derived lists | read-only | counts, last placement date, total fees invoiced and collected, guarantee items open | O, R |
| proposals[], opportunities[] | derived | read-only | | O |
| documents[] (type: NDA, TOB, ENGAGEMENT, PROPOSAL, ORG_CHART, OTHER; file, version, expires_on, visibility) | repeating upload | Opt | virus scan; max 10 MB each (aligns with person_file) | O |
| tags | text array (exists) | Opt | max 20 | L |
| notes | text (exists) | Opt | internal | O |
| created/updated audit | auto | | | O |

### 3.2 Contact form (person at an account)

Contact stays a `person` + `contact` role row (A-03). Fields on the account-contact link are role-specific; fields on `person` are shared (a client contact may become a candidate or vice versa; Thrive-style relationship graph).

| Field | Type | Req | Validation | Shown |
| --- | --- | --- | --- | --- |
| person (name, surname) | text 200 | Req | duplicate check by email, phone, name+company (exists) | everywhere |
| account_id | FK | Req | | H |
| job_title | text 200 | Req | | H, L |
| function | enum CEO, CFO, HR_DIRECTOR, FUNCTION_HEAD, BOARD, OTHER | Opt | | L, filters |
| contact_roles | multi enum SPONSOR, DECISION_MAKER, BUDGET_HOLDER, HIRING_MANAGER, HR_LIAISON, INFLUENCER, FINANCE_BILLING, LEGAL_SIGNATORY, GATEKEEPER | Req (at least 1) | | O, P (signatory), I (billing) |
| decision_influence | enum HIGH, MEDIUM, LOW | Opt | | L, org chart |
| sentiment / relationship_strength | enum COLD, WARM, STRONG, CHAMPION, DETRACTOR | Opt | | H |
| reports_to_contact_id | FK contact | Opt | same account; no cycles | org chart |
| is_primary | boolean (exists) | Opt | one active per account | H |
| email | email 254 | Req (one of email/phone) | format; unique (exists) | H |
| phone, mobile | E.164 | Opt | normalise +90 (exists) | H |
| assistant_name / email | text / email | Opt | | O |
| preferred_language | TR, EN | Opt | default account's | templates |
| preferred_channel | enum EMAIL, PHONE, WHATSAPP, MEETING | Opt | | O |
| city / office_id | text / FK | Opt | | O |
| linkedin_url | url 300 | Opt | normalised (exists) | O |
| how_we_know / introduced_by | text + link to person | Opt | feeds network_link | O |
| personal_notes | text 2000 | Opt | internal; no sensitive data (health, politics, beliefs); show a hint | O |
| reports_received (portal access) | boolean + invited_on | Opt | | O |
| contact_privacy: notice_sent_on, source, lawful_basis note | date / enum / text | Opt | KVKK: the firm's counsel chooses basis; record source and notice date; marketing opt-in separate | O |
| marketing_opt_in | boolean + date | Opt | default false | L |
| started_on / ended_on | date (ended_on exists) | Opt | ended >= started; ended contact keeps history and offers "moved to new company" prompt | O |
| last_contact_at | derived | read-only | | H, L |
| owner_id | FK app_user | Req | relationship owner of the person | H |

### 3.3 New objects proposed (names only; fields in 3.1 and below)

| Object | Purpose | Key fields |
| --- | --- | --- |
| `account_location` | offices as rows | account_id, label, type, address, city, country |
| `account_team_member` | named team with permission | account_id, user_id, role, can_edit |
| `agreement` | terms of business / engagement letter, versioned | account_id, kind TOB/ENGAGEMENT/AMENDMENT, signed_on, valid_until, fee_model, fee_percent, fee_base, currency, payment_terms_days, guarantee_days, exclusivity, non_solicit_months, scope, document_id, status |
| `fee_schedule_item` | instalments and triggers | agreement_id or job_order_id, trigger_type, percent or amount, due_days, vat, invoice_state NOT_DUE/DUE/INVOICED/PAID, einvoice_ref, invoiced_on |
| `placement` | outcome and guarantee | job_order_id, person_id, start_date, base, bonus, other_cash, first_year_cash (encrypted), fee_amount, guarantee_end_on, replaced_by_job_order_id, left_on |
| `conflict_check` | audit of off-limits and conflict checks | account_id or company, checked_by, at, hits, decision, override_reason |
| `account_document` | docs on account | type, version, file ref, expires_on, visibility |
| `account_signal` | BD trigger events | account_id, type (funding, leadership change, M&A), source_url, noted_on |
| `search_milestone` | kickoff, profile approved, shortlist delivered, offer, start | job_order_id, kind, reached_on; feeds KPIs and triggers |
| `fee_credit` | split of credit | job_order_id, user_id, role, percent |

Also extend: `job_order` with confidentiality_level, brief fields, target_start_date, comp_band (encrypted), client sponsor/contact id, and fee override pointing to its `agreement`; `proposal` with version, valid_until, structured fee link instead of single `fee_amount`.

### 3.4 Screens and states for the client lifecycle

Consultant screens:

1. **Accounts list (firm overview):** table and kanban by status; filters: owner, industry, status, group, health, confidentiality; saved views (exists); bulk tag/owner (exists).
2. **Account create (quick) and full edit:** quick create asks name, country, owner, origination, industry; duplicate and conflict check runs on blur; full form in the sections above with progress bar "ready to activate: 6 of 9".
3. **Account page** tabs: Overview (health, team, key contacts, next actions), Contacts and org chart, Searches and placements, Pipeline and proposals, Agreement and billing, Documents, Timeline, Compliance. Side panel previews on lists (plan principle).
4. **BD pipeline:** kanban Lead, Meeting, Proposal sent, Negotiation, Won/Lost; opportunity drawer with roles and estimated fee; fee-weighted total per column.
5. **Target-company map for BD:** list of companies with signal, owner, state, off-limits flag.
6. **Proposal builder:** template, fee schedule table, guarantee, off-limits; versions; sent and viewed states; send PDF; on Won, button "create agreement and first search".
7. **Agreement screen:** terms of business and engagement letter upload, structured terms form, expiry reminders.
8. **Activate client wizard:** checklist of missing legal, tax, billing, agreement, contacts, conflict check, confidentiality, team; blocked items shown with one-click fix.
9. **Search kickoff:** brief form, position profile upload and client approval, team, confidentiality, fee schedule confirmed, milestones created.
10. **Billing view (per account and firm-wide):** fee schedule items with state; "invoice ready" list for finance (triggers reached but not invoiced); e-fatura reference field; overdue; link to accounting system.
11. **Placement and guarantee tracker:** placed candidates, guarantee end dates, 30-day alert, replacement search action, retention check at 6 and 12 months.
12. **Client reporting:** weekly report generator, shortlist report, closing report, portal view logs.
13. **Conflict and off-limits screen:** rules list with expiry, check history, partner override with reason.
14. **My day widgets:** overdue follow-ups, proposals to chase (7 days), agreements expiring, invoices ready, guarantee ending, cold key accounts.

Account states and transitions (proposal; extends `AccountStatus`, the existing enum values must be read from `contracts/enums/` before changing):

```
TARGET -> LEAD -> PROSPECT(meeting held) -> PROPOSAL -> CLIENT_ACTIVE
CLIENT_ACTIVE -> CLIENT_DORMANT (no open search 6 months) -> FORMER_CLIENT (agreement ended; off-limits runs on)
any -> LOST (reason required) ; any -> DO_NOT_CONTACT (reason, partner approval)
```

Guards: CLIENT_ACTIVE needs the Req* fields; closing an account with open searches is blocked (exists: `blockingItems`); FORMER_CLIENT keeps non-solicit rules active until `ends_on`.

Search/agreement states: DRAFT, CONTRACT_PENDING, KICKOFF, RESEARCH, LONGLIST, SHORTLIST_DELIVERED, CLIENT_INTERVIEWS, OFFER, PLACED, GUARANTEE, CLOSED, ON_HOLD, CANCELLED (abort fee), each tied to milestones and fee triggers.

Fee schedule item states: NOT_DUE, DUE, INVOICE_REQUESTED, INVOICED, PAID, WRITTEN_OFF, CREDITED.

Empty, error and edge states each screen needs: no agreement yet (banner "cannot start billable search"), duplicate account on create, off-limits hit (blocks with override path), expired agreement, tax ID invalid or e-fatura unknown, confidential account hidden to non-team users (404-style), consent missing when sharing (exists).

---

## 4. Gaps typical in homegrown tools (practice and opinion)

Evidence quality: mostly vendor blogs; marked accordingly.

1. **Spreadsheet or file-based records break with multiple editors:** version conflicts, no audit trail, knowledge leaves with consultants ([Recruiteze](https://recruiteze.com/recruiting-without-crm-hidden-costs/), [Sorapis](https://sorapis-web.pages.dev/blog/5-signs-recruitment-firm-outgrown-spreadsheets/index.html), vendor claims).
2. **Duplicates and unclear ownership:** the same company or person under several owners; last save wins ([iMark](https://www.imarkinfotech.com/salesforce/blogs/crm-data-drift-how-record-ownership-breaks-salesforce)). Fix: unique legal identity (VKN), duplicate guard, named owner and team.
3. **No structured fee terms:** fee agreement lives as a PDF or free text, so triggers, guarantee end and forecast are manual (our schema today: `fee_terms` text).
4. **No off-limits and conflict register tied to agreements:** clients' non-solicit scope and duration kept in heads; guarantee periods not turned into off-limits dates. Practice varies: 12 months post-engagement is one firm's published default; 18 to 24 months appear elsewhere ([Vanguarde](https://huntscanlon.com/select-guide/vanguarde/), [Recruiterflow](https://recruiterflow.com/glossary/off-limits-policy/)).
5. **Confidentiality as all-or-nothing:** everyone sees every search; no off-the-record notes ([Spott](https://spott.io/resources/crm-for-executive-search-what-search-firms-actually-need), vendor view).
6. **People modelled once per role:** a person who is a client contact, then a candidate, then a referee becomes three records (same source suggests one record with multiple roles; our `person` + `contact` already does this).
7. **Billing disconnected from delivery:** shortlist and offer milestones are not linked to invoices, so retainer thirds are invoiced late.
8. **Guarantee and replacement tracking absent:** nobody is alerted that a placed candidate left within the guarantee window.
9. **Reporting by hand:** weekly client updates compiled from memory; no log of what the client saw.
10. **Turkish specifics ignored:** tax number and e-fatura status not captured (invoice rework), KVKK notice sent only for candidates not for client contacts, Turkish characters break search.
11. **KPIs not defined:** time to shortlist, fee-weighted pipeline and conversion are computed ad hoc; no consistent milestone dates.
12. **Client-record completeness not enforced:** accounts exist with name only; no activation gate.

---

## 5. Open questions for the firm (add to the plan's list)

- Fee models and exact invoice triggers, VAT/withholding handling, currencies, who issues e-fatura (accounting software or integrator).
- Their terms of business and engagement letter (Turkish and English): guarantee length, off-limits wording and duration, exclusivity, cancellation fee.
- Do they hold or need a private employment bureau permit for executive search (counsel to confirm)?
- VERBIS status of the firm itself, and lawful basis chosen for candidates and client contacts.
- Fee credit rules between originator, lead, researcher.
- Confidentiality levels they already use.
- Current account data in Salesforce: which fields exist and can be migrated.

## 6. Limits of this research

- Web search tool is US-oriented; Turkish sources were secondary blogs and not official GIB, KVKK or Resmi Gazete texts. Thresholds and algorithms (VKN check digit, MERSIS structure, e-fatura limits) must be verified before coding validation or legal wording.
- No independent survey of executive-search fee norms or KPI benchmarks was found; figures cited are vendor claims.
- Invenias and Clockwork internal field lists were not found beyond what the product plan already cites.

## 7. Sources

Executive search process, fees, terms
- [Airswift, retained search glossary](https://www.airswift.com/glossary/retained-search)
- [Recruiterflow, retained search process](https://recruiterflow.com/blog/ai-in-retained-search/)
- [Curran Daly, 8 steps of retained search](https://currandaly.com/executive-hiring-roadmap-8-steps-retained-search/)
- [Blue Signal, search process](https://bluesignal.com/clients/our-search-process/)
- [KiTalent, executive search process](https://kitalent.com/executive-search-process.md)
- [JRG Partners, target company list](https://www.jrgpartners.com/?p=39506)
- [Axial Search, retained fee structure](https://axialsearch.com/insights/retained-ai-search-fees)
- [KiTalent, procurement guide](https://kitalent.com/executive-search-procurement-guide.md)
- [Skagway municipal search agreement example](https://skagway.org/sites/default/files/fileattachments/clinic_board/meeting/packets/47611/8c._coors_executive_search_placement_agreement_ed_dahl_101322.pdf)
- [Vanguarde off-limits policy](https://huntscanlon.com/select-guide/vanguarde/)
- [Recruiterflow, off-limits policy](https://recruiterflow.com/glossary/off-limits-policy/)

CRM, KPIs, tools
- [Spott, CRM for executive search](https://spott.io/resources/crm-for-executive-search-what-search-firms-actually-need)
- [Bullhorn executive search guide](https://kb.bullhorn.com/bh4sf/Content/BH4SF/Topics/executiveSearchUserGuide.htm)
- [Recruiterflow, time-to-fill benchmarks](https://recruiterflow.com/blog/time-to-fill-benchmarks/)
- [KiTalent, time-to-shortlist benchmark](https://kitalent.com/executive-search-time-to-shortlist-benchmark.md)
- [RecruitBPM, executive search analytics](https://recruitbpm.com/blog/data-analytics-for-executive-search-firms)
- [Thrive TRM, KPI dashboards 2026](https://thrivetrm.com/the-top-5-executive-search-kpi-dashboards-for-2026/)
- [Recruiteze, hidden cost without CRM](https://recruiteze.com/recruiting-without-crm-hidden-costs/)
- [Sorapis, outgrown spreadsheets](https://sorapis-web.pages.dev/blog/5-signs-recruitment-firm-outgrown-spreadsheets/index.html)
- [iMark, CRM record ownership](https://www.imarkinfotech.com/salesforce/blogs/crm-data-drift-how-record-ownership-breaks-salesforce)

Turkey (Turkish-language sources)
- [IMER, VERBIS registration test](https://imer.av.tr/kvkk-araclari/verbis-kayit-yukumlulugu-testi)
- [Cott Group, VERBIS exception rules](https://www.cottgroup.com/en/blog/item/verbise-kayit-yukumlulugu-istisnasina-iliskin-uygulama-esaslari-netlestirildi)
- [Cott Group, KVKK in HR processes](https://www.cottgroup.com/en/blog/item/insan-kaynaklari-sureclerinde-kvkk-nin-onemi)
- [TP Vision candidate notice (2-year example)](https://www.tpvision.com/policy/TPV-Calisan-Adayi-Aydinlatma-Metni-v2.0.pdf)
- [KPMG candidate notice](https://assets.kpmg.com/content/dam/kpmg/tr/pdf/2026/01/kpmg-bagimsiz-denetim-calisan-adayi-kvkk-aydinlatma-metni.pdf)
- [Uludag University paper on candidate data](https://uludag.edu.tr/dosyalar/iibfdergi/genel-dokuman/2022_2/asl10_20222duzeltilmis.pdf)
- [Parasut, MERSIS number](https://www.parasut.com/blog/mersis-numarasi-nedir)
- [Is Bankasi, MERSIS number](https://www.isbank.com.tr/blog/mersis-numarasi-nedir)
- [Eleman.net, taxpayer lookup by business type](https://www.eleman.net/is-rehberi/isveren-rehberi/isletme-tipine-gore-mukellef-sorgulama-nasil-yapilir-h22500)
- [Parasut, e-fatura lookup](https://www.parasut.com/blog/e-fatura-sorgulama)
- [Parasut, e-fatura and e-arsiv obligation](https://www.parasut.com/blog/e-fatura-ve-e-arsiv-zorunlulugu)
- [Musavirler Kulubu, 2026 e-fatura threshold](https://musavirlerkulubu.com.tr/makale/e-fatura-zorunluluk-siniri-2026-gecis-sureci-ve-uyum-rehberi)
- [Alomaliye, private employment bureau FAQ](https://www.alomaliye.com/2015/08/30/ozel-istihdam-burolari-sikca-sorulan-sorular/)
