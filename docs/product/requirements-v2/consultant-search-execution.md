# Mihenk: consultant search execution requirements (SX)

Author view: senior consultant and lead researcher. Date 2026-10-29. Pilot: 2-3 consultants, Turkey, KVKK. Inputs: `critique-consultant-review.md` (scorecard, Top 15, 10 deal-enders), `app-architecture.md` (Searches and Candidates rows). Vocabulary: Client, Search, Longlist, Shortlist, Candidates (never Job order, Network).

## 1. Research basis (fact vs opinion)

Facts from public sources (vendor and firm marketing, so treat as practice, not standard):
- Retained flow: brief, market mapping of organisations and talent pools, longlist, assessment interviews, shortlist, client interviews, offer ([Amrop](https://www.amrop.com/lu/en/executive-search/our-methodology/), [AESC](https://www.aesc.org/insights/executive-search/executive-search-process-what-expect)).
- Sizes quoted: 150-300 names mapped, longlist 20-40 (one guide), shortlist 3-5; 6-16 weeks brief to accepted offer ([headhunters guide](https://headhunters.pages.dev/guides/executive-search-process/), [Ertler](https://ertlerexecutivesearch.odoo.com/en/services/executive-search-process)).
- AESC: the longlist is a report with current role, contact details, CV and interaction notes. Shortlist reports hold CV, assessment, interview report and fit analysis against competencies.
- Good reports separate verified facts from the consultant's interpretation and show gaps ([Recruiterflow](https://recruiterflow.com/glossary/candidate-presentation/)). Compensation is usually disclosed at shortlist ([Firth Rossmartin](https://www.firthrossmartin.co.uk/our-services/executive-search/)).
- Invenias (Bullhorn): assignments, longlist and shortlist management, branded candidate profiles, progress reports and shortlist presentations, GDPR consent ([Capterra/GetApp listings](https://www.getapp.com/hr-employee-management-software/a/invenias/)).
- Vincere LiveList: branded real-time shortlist portal, clients rate candidates and arrange interviews ([Vincere help](https://help.vincere.io/en/collections/1813461-vincere-core), [G2](https://www.g2.com/products/vincere/reviews)).
- Loxo: saved Boolean and filter searches, shareable; bulk sequences from search results; reviewers cite duplicates and consent worries ([Loxo help](https://help.loxo.co/en/articles/3168722-add-candidates-using-loxo-source), [ZoomInfo review](https://pipeline.zoominfo.com/sales/loxo-review)).
- Bullhorn: submission lists across candidate, vacancy, client; submission preview before sending ([Bullhorn KB](https://kb.bullhorn.com/ats/Content/BHATS/LP/candidateList.htm)). No evidence found of a native shortlist portal.

Opinion (mine): blind CVs are weak for the client (a boutique says the same, [KiTalent](https://kitalent.com/pay-on-shortlist-executive-search.md)); Turkish clients still want blind versions early because candidates are employed at competitors. So blind is a switch, not the default. The deliverable is the product; ranking beats a Kanban.

## 2. Requirements

Format: statement; priority (M/S/C for the pilot); why; GWT; data; edge. Field lists are minimum.

### Brief and mapping

**SX-001 Search brief and position profile (M).** A Search holds a structured brief. Why: today a job order has no brief (critique #3). Given a new Search, when I open Brief, then I can fill position title, reporting line, purpose, must-haves, nice-to-haves, compensation band (min, max, currency, bonus), location, languages, timeline (kick-off, longlist due, shortlist due, target start), confidentiality (open/confidential), team and fee terms (link). Given a required field is empty, when I set stage "Research", then I get a warning, not a block. Data: those fields plus `briefVersion`. Edge: client changes the brief mid-search: new version kept, diff visible, candidates are not re-scored automatically.

**SX-002 Search from a won mandate (S).** Given a won opportunity, when I click "Start search", then client, fee terms and contacts prefill. Edge: duplicate search for the same client and title warns.

**SX-003 Target-company list (M).** Per Search, a list of target companies with tier (A/B/C), rationale, status (to map, mapped, approached, closed), owner. Given a company on the client's off-limits list, when I add it, then it shows "Off-limits" and cannot be set to approached without manager override (reuse the existing guard). Data: company, sector, size, tier, status, notes, source. Edge: name drift ("Vesta" vs "Vesta Industries") matches through aliases.

**SX-004 Org chart per target company (S).** Given a target company, when I add people with title, reports-to and status (not approached, approached, interested, declined, hired by us), then a tree view shows them. Each box can be promoted to a candidate in one click. Edge: vacancy boxes; two people in the same role; people who left (greyed, dated).

**SX-005 Research call sheet (S).** A researcher gets a per-Search table of people to call: name, company, title, phone, status, last outcome, next call, script link. Given I log an outcome from the sheet, when I press Enter, then the row updates and the next row focuses. Edge: do-not-call and consent-withdrawn rows are locked and labelled.

### Sourcing and outreach

**SX-006 Source and add with consent state (M).** Adding a person to a longlist from the pool, a CV or a typed name shows consent state next to them. Given consent Refused or Withdrawn, when I add, then the add is allowed as "Research only" (no outreach, no presenting). Given an off-limits company, then the add is blocked (existing 409). Data: source channel, source URL (public, optional), legal basis note. Edge: public-profile-only prospects hold minimal data until first contact and the KVKK notice goes out at first contact.

**SX-007 Templated outreach with consent link (S).** Given selected longlist people with an email, when I choose a TR/EN template, then each message preview shows merge fields and a consent link, and people without consent-eligible status are skipped with a summary. Data: template, sent time, channel, reply status. Edge: bounce, unsubscribe, a person approached by a colleague in the last 90 days is flagged.

### Candidate record and evaluation

**SX-008 Candidate evaluation record (M).** Each person has an Evaluations tab holding the form in section 4, once per Search. Why: deal-ender 3. Given I save, then the person's overview shows rating, compensation, notice, mobility. Edge: edits keep revisions (Envers); an evaluation can be marked "internal only".

**SX-009 Interview notes (M).** Free text with template headings (Career, Motivation, Fit, Concerns), date, interviewer, type (phone, video, in person). Given I use Ctrl+Enter, then it saves. Edge: notes never appear in client reports unless I mark a block "Include in report".

**SX-010 References (S).** Per candidate: referee name, relationship, company, contact, date, status (requested, done, declined), summary, rating. Given the candidate has not agreed to references, when I log one, then a warning appears (checking behind a candidate's back is a deal-ender in confidential searches). Edge: referee is an employee of the client.

**SX-011 Compensation, notice and mobility (M).** Fields: current fixed, bonus target, benefits text, expectation min/max, currency, notice period (days) and buy-out possible, garden leave, non-compete note, relocation willingness (no, city list, abroad), earliest start. Given a currency differs from the brief's, then a converted figure shows with the rate date. Edge: unknown is a value ("not asked"), distinct from empty.

**SX-012 Languages and skills (S).** Language with level (CEFR) and skills as tags with years. Feeds filters. Edge: Turkish diacritics folded in search.

### Longlist, shortlist, ranking

**SX-013 Longlist object (M).** A Search has a Longlist list view (table) with status per person: To call, Called, Interested, Not interested, Not now, Screened. Given 10 selected candidates in the pool, when I press "Add to search", then all 10 are added in one action with a result summary (added, skipped with reason). Data: person, search, status, source, added by, date. Edge: already in this search (skipped); previously presented to this client (warning PREVIOUSLY_PRESENTED_TO_ACCOUNT).

**SX-014 Shortlist object with rank, rating, rationale (M).** Moving a person to Shortlist requires: rating (1-5), rank number, a rationale of at least 20 characters, and consent Given. Given I press Alt+Up, then rank changes; ranks stay unique and gapless. Edge: ties are not allowed; removing someone renumbers; shortlist of more than 7 shows a hint.

**SX-015 Side-by-side compare (S).** Select 2-5 shortlisted people, view columns: summary, rating per criterion, compensation, notice, mobility, concerns. Edge: missing data shows "not asked".

**SX-016 Stage vocabulary and pipeline view (M).** Stages: Research, Longlist, Screened, Shortlist, Client interview, Offer, Placed, Closed (with reason). Board and table both exist, with "Move to..." menu and Alt+arrows. Edge: Placed and Closed are always visible (critique #15).

**SX-017 Per-criterion scoring against the brief (S).** The must-haves of the brief become scoring rows (1-5 each) on the evaluation. Given the brief changes, then new criteria appear empty and old ones remain.

### Client interaction

**SX-018 Client interview scheduling (S).** From a shortlisted person: propose slots, pick client participants, create calendar events, send invites to both sides. Pilot may use .ics download and copy-paste text. Edge: time zones, candidate wants the interview kept confidential (neutral invite title).

**SX-019 Client feedback capture (M).** Per shortlisted person and per round: date, client participants, outcome (advance, hold, decline), reasons (picklist plus text), candidate feedback to relay. Given outcome Decline, when I save, then the reason is required and the person's status becomes Closed for this Search. Edge: the client replies by phone: I enter it on their behalf with "recorded by" shown.

**SX-020 Offer and placement (M).** Offer: date, base, bonus, benefits, start date, status (verbal, written, accepted, declined, counter-offer). Placement: start date, fee basis, guarantee end. Given Accepted, when I press "Place", then the Search moves to Placed, other candidates get a "Not selected" task, and consent/retention for them is flagged. Edge: candidate declines after accepting; counter-offer logged; fee amount editing needs manager role.

### Reports

**SX-021 Shortlist report (M).** Branded Word and PDF from the Shortlist, spec in section 5. Given 5 shortlisted people with complete evaluations, when I press "Generate report", then a file is produced in under 15 s and stored in Files with version number. Given a person lacks consent Given, then they are excluded and the exclusion is listed to me. Edge: Turkish characters, long CV text, missing photo.

**SX-022 Blind version (M).** A toggle on the same report hides identifying data (section 5). Given Blind, when I generate, then name, employer name, photo, contact, links and exact dates are replaced and an alias "Candidate A" is used. A check scans free text for the person's name and employer and warns. Edge: a unique role ("CFO of the only Turkish X") re-identifies, so the warning is advisory and the consultant confirms.

**SX-023 Progress report (S).** A one-page weekly status per Search: counts per stage, activities, next steps. Edge: counts only, no names for confidential searches.

**SX-024 Secure share link (C).** A time-limited link for the client to view the shortlist and give rating and feedback. Pilot may defer to PDF by email. Edge: link expiry, revoke, view log.

### Pools, search, bulk, duplicates

**SX-025 Skill, function, seniority, city, salary filters (M).** Pool filters: city, function, seniority, sector, languages, salary expectation range, notice, mobility, consent state, owner. Given I set city Ankara and function Finance, then results show in under 1 s for 10k people. Edge: Turkish İ/ı folding.

**SX-026 Boolean CV search (M).** Operators AND, OR, NOT/-, "phrase", parentheses, proximity optional, field prefixes `city:`, `title:`, `company:`. Given an invalid query, then an inline message says where. Results show highlighted snippets. Edge: CVs not parsed show as "not searchable" with a count.

**SX-027 Saved searches and talent pools (S).** Save filter plus Boolean as a named search (private, shared). A talent pool is a static list or a saved search with "alert me on new matches" (weekly). Edge: a pool containing people whose consent was later withdrawn removes them from outreach automatically.

**SX-028 Bulk actions (M).** On any list: add to search, status, owner, tag, next action, request consent, export. Always with a summary and Undo. Given 30 selected and 4 without consent, when I choose Outreach, then 26 proceed and 4 are listed as skipped. Edge: export of more than 50 people needs manager approval and is audit-logged.

**SX-029 Duplicate guard (M).** On add or import, a match on email, phone, LinkedIn URL or name plus company shows "Looks like X" with Merge or Keep both. Edge: shared mailbox emails; merge keeps both consents with dates and keeps the stricter retention.

### Daily work and keyboard

**SX-030 Quick log (M).** Log a call in under 10 s (task 1 below). One form: person (picker or current record), outcome (picklist, one key each), search (defaults to the last used), follow-up date (quick chips: tomorrow, +3d, +1w), note optional. Edge: unknown caller: create a stub candidate with just a name.

**SX-031 My day (M).** Consultant home as in app-architecture, plus: calls due, interviews today, shortlists due this week, client feedback overdue, stale searches (no activity 14 days). Every row has inline Done and Snooze.

**SX-032 Keyboard-first (M).** Shortcuts from app-architecture work on every list and on the longlist/shortlist: j/k, x, l (log), n, Alt+arrows (rank), Ctrl+Enter, Esc, `?` help. Given a keyboard-only user, then the 10 daily tasks complete with no mouse. Edge: shortcuts can be turned off (WCAG 2.1.4).

**SX-033 Confidential search permissions (S).** A confidential Search is visible only to its team; candidates on it are masked elsewhere ("Confidential search" instead of the search name). Edge: manager sees all, access is logged.

## 3. Ten daily tasks and click targets

A click is a mouse click or a keypress that is not typing. Targets assume the pilot UI after Wave 2c.

| # | Task | Path | Target clicks | Max seconds |
|---|---|---|---:|---:|
| 1 | Log a call with a candidate | `l`, type name, Enter, outcome key, Ctrl+Enter | 4 | 10 |
| 2 | Log a client call and set the follow-up | `l`, contact, outcome, chip "+3d", save | 5 | 15 |
| 3 | Add 10 people from the pool to a Search | filter, Shift+x range, "Add to search", confirm | 5 | 20 |
| 4 | Rank and rate a shortlisted candidate | open row, rating, Alt+Up, rationale, save | 5 | 30 |
| 5 | Find CFO candidates in Ankara with English C1 | filters or `city:Ankara`, Enter, open | 4 | 15 |
| 6 | Send consent requests to 15 selected people | select, Request consent, confirm | 3 | 15 |
| 7 | Record client feedback after an interview | shortlist row, Feedback, outcome, reason, save | 5 | 30 |
| 8 | Check if the client has seen this person | open candidate, Exposure panel (also on client page) | 2 | 5 |
| 9 | Generate the blind shortlist report | Reports, Blind toggle, Generate, open | 4 | 20 |
| 10 | Plan my day | open Home, tick/snooze 5 rows | 6 | 30 |

## 4. Candidate evaluation form (field by field)

Scope: one evaluation per person per Search; fields marked P also live on the person profile and sync.

| # | Field | Type | Req | Rule |
|---|---|---|---|---|
| 1 | Search | picker | yes | Defaults to current |
| 2 | Evaluator, date | user, date | yes | Defaults to me, today |
| 3 | Interview type | phone/video/in person | yes | |
| 4 | Overall rating | 1-5 stars with labels (1 reject, 3 maybe, 5 outstanding) | yes for Shortlist | |
| 5 | Recommendation | Shortlist / Hold / Reject | yes | Reject needs reason |
| 6 | Criterion scores | 1-5 per brief must-have, plus comment | S | Auto-filled from brief |
| 7 | Summary (3-5 lines) | text, max 600 | yes for Shortlist | Goes to report |
| 8 | Key achievements | list, up to 5 | no | Goes to report |
| 9 | Motivation to move | text, max 400, tag (growth, pay, stability, relocation, push factor) | yes | |
| 10 | Concerns and gaps | text, max 400 | yes for Shortlist | Shown openly in report |
| 11 | Current employer, title (P) | text | yes | Hidden in blind |
| 12 | Current fixed, bonus, benefits (P) | money, currency | S | Hidden or banded in blind |
| 13 | Expectation min/max (P) | money | S | |
| 14 | Notice period days, buy-out, garden leave (P) | number, bool, text | yes | |
| 15 | Mobility (P) | no / cities / abroad | yes | |
| 16 | Languages (P) | language, level | S | |
| 17 | Other processes | text | no | Competing offers |
| 18 | References | list (SX-010) | no | Status shown in report |
| 19 | Assessment results | file or score, vendor | no | Pilot: file upload |
| 20 | Private notes | text | no | Never in reports |
| 21 | Include in report | per block checkbox | yes | Default on for 7-10 |

Rules: autosave draft; Ctrl+Enter saves; unsaved guard; revision history; consent must be Given before recommendation Shortlist.

## 5. Shortlist report spec

Sections, in order:
1. Cover: client logo, Search title, firm logo, date, version, confidentiality notice, consultant name.
2. Search summary: brief digest, process followed, numbers (mapped, called, interviewed, shortlisted) with a timeline.
3. Market note (S): talent pool size, compensation observed, target companies approached (names hidden if the client wishes).
4. Comparison table: rank, name, current role, location, rating, compensation, notice, mobility, key concern.
5. One profile per candidate: photo (optional), headline, summary, career table (employer, title, dates, one-line scope), education, languages, achievements, motivation, fit against each criterion (score and comment), concerns, compensation (current, expectation), notice and mobility, reference status, consultant recommendation and rank rationale.
6. Next steps: proposed interview slots, open questions.
7. Appendix: CVs (non-blind only), assessment summaries.
8. Footer: KVKK notice (shared with consent, retention, no onward sharing).

Blind version hides: names (alias Candidate A-E), employer names (replaced by sector and size, "a listed Turkish industrial group"), photo, contact details, social links, exact dates (years only, rounded to half years), school names if unique (replace with tier), names of referees, free text matching name or employer, CV appendix and file metadata (author, title), the file name (neutral). Compensation stays but may be banded (setting). Blind keeps rank, rating, fit, motivation, concerns. Generation logs who made it, which candidates, blind flag and recipient. A candidate released from blind to named version needs a second explicit consent step recorded on their record.

Acceptance: Given Blind, when the PDF text is extracted, then no string equals any candidate name, employer, email or phone of included candidates (automated test with seed data).

## 6. Out of scope for pilot

Psychometric integrations, AI long-list ranking, native client portal (SX-024 is Could), mail merge sequences beyond SX-007, billing (separate commercial layer).
