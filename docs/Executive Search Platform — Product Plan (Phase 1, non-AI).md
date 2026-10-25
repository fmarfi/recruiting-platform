# Executive Search Platform — Product Plan (Phase 1, non-AI)

Oct 25, 2026 · @asim

## Executive summary

We build one system that runs the whole firm: client accounts and business development, search assignments, a consent-safe candidate pool, and client-ready reporting. Phase 1 contains no AI; it is the CRM + ATS foundation the AI recruiting tool (Phase 2) will sit on.

The client runs two kinds of work that competitors usually split across two products:

- **Executive search**: few, confidential, mid-to-senior mandates where relationships and reporting matter most.
- **Volume staffing**: sudden bulk demand, e.g. a retail chain asking for 400 people across 200 stores at once. Not in current scope; parked as module 6.

Our edge over Invenias, Clockwork, Ezekia, Vincere and Loxo comes from four things: one workspace for search and recruitment work (volume staffing is parked for later but designed for), KVKK compliance built into every record rather than added on, the firm owning all its candidate and client data in its own app instead of in LinkedIn, and a UI that is fast and keyboard-first where reviewers call incumbents slow and clunky.

The pilot goal is narrow: the firm's consultants run their live searches in our tool for 8 weeks and stop using spreadsheets and Salesforce for it.

## What the client told us

The notes describe a firm whose real asset is its candidate pool and its client relationships, and whose current tools (LinkedIn, Salesforce, files) keep that asset scattered. Each line below maps a note to the requirement it implies.

### Business model

| Note (original) | What it means | Requirement for us |
| --- | --- | --- |
| Recruitment Services + Executive Search | Two service lines with different workflows | Assignment types: Executive Search, Recruitment, Staffing, each with its own stages |
| Orta-üst düzey işe alım danışmanlığı | Core is mid-to-senior hiring for corporates | Senior-grade candidate profiles: career history, compensation, notice period, mobility |
| Staffing: toplu işe alım/çıkarım | Bulk hiring and layoffs drive volume | Bulk mode: one request, many positions, many locations, many hires |
| Gratis: 200 mağaza, 400 personel | A single order can hold hundreds of seats | Position × location grid with headcount per cell and a fill-rate view |
| Ne faturası kesiliyor? | Billing model is unknown to us | Fee terms per proposal (retainer, stages, success fee); invoicing triggers; open question |

### Clients and business development

| Note (original) | What it means | Requirement for us |
| --- | --- | --- |
| Salesforce, "customer" kısmı | They like Salesforce's account model as a reference | Account → contacts → opportunities → assignments, Salesforce-style but simpler |
| Account açılıyor şirkete | Every client company is an account | Account page with all contacts, open and past searches, proposals, revenue |
| Contact, hangi işler aranıyor, proposal koşulları | Each lead records what roles the client needs and the offer terms | Opportunity record: roles requested, proposal terms, status, value |
| Aktiviteler: client visit, proposal, screening | Consultants log visits, proposals and screening work | Unified activity timeline on accounts, contacts, candidates and assignments |
| Müşteri teklifleri | Proposals are a tracked object | Proposal builder with versions, status (draft, sent, won, lost) and PDF output |
| Her müşteri için account, genel overview'da toplanıyor | Partners want one overview of all accounts | Firm dashboard: pipeline value, active searches, accounts needing attention |
| Funnel management | They think in funnels, both sales and candidates | Two funnels: BD (lead → proposal → won) and candidate (sourced → placed) |
| Networking önemli | Relationships are the moat | Relationship map: who knows whom, past placements, candidates who became clients |

### Candidates and the pool

| Note (original) | What it means | Requirement for us |
| --- | --- | --- |
| LinkedIn'den ilan açılıyor (düşük pozisyonlar) | Job ads on LinkedIn for junior roles | Job posting records and an applicant inbox per posting |
| LinkedIn'den direkt ulaşanlar | Inbound candidates via LinkedIn messages | Quick add from the CV or details they send; LinkedIn URL stored as a plain field |
| Havuzda CV'ler tutulur; CV'ler nasıl saklanıyor? | The pool is central but storage is ad hoc | One structured candidate record per person, CV files versioned, full-text search |
| Aynı CV'ler başka firmaların aynı pozisyonları için | One candidate is reused across clients | Candidate ↔ many assignments; history of where they were presented; client conflict rules |
| Aday araştırma, CV screening, finish | Their search stages | Default pipeline: Research → Contacted → Screened → Presented → Interview → Offer → Placed |
| Asıl amaç LinkedIn'den data kendimize gelsin | Candidate and client data must live in the firm's own app, not inside LinkedIn | Our app is the only store for candidate and client data; no LinkedIn API, no scraping; consultants use LinkedIn by hand and record the results in the app |

### Compliance (KVKK)

| Note (original) | What it means | Requirement for us |
| --- | --- | --- |
| KVKK izni çok önemli | Consent is a hard requirement | Consent status on every candidate; nothing is shared with a client without it |
| LinkedIn'dekilere gerek yok, website'den gelenlere gerek var | Their current view of when consent is needed | Source-aware consent rules; we flag this view for legal review (below) |
| CV yüklenince KVKK maili gidiyor | They already send a consent email after upload | Automatic aydınlatma + açık rıza email with a tracked consent link on CV upload |
| CV'ler iki yıl havuzda kalabiliyor | Retention is 2 years | Retention clock per candidate; renewal request before expiry; auto-anonymise after |

### Assessment and reporting

| Note (original) | What it means | Requirement for us |
| --- | --- | --- |
| Yetkinlik envanteri, tek sistemden olursa daha iyi | Assessments live in other tools today | Send, track and store competency inventories from inside the candidate record |
| Aday raporları | Clients receive candidate reports | Branded candidate report and shortlist PDF, generated from the record |
| Mülakat simülasyonu?? | An idea, not a commitment | Phase 2 (AI); Phase 1 only stores structured interview scorecards |
| Blindlama için AI | They want blind CVs for fair review | Phase 1: one-click anonymised report (hide name, photo, age, gender fields); AI redaction in Phase 2 |
| Vinter | They know Vinter, an assessment and video-interview platform | Benchmark for the assessment module; possible integration rather than rebuild |

### Gaps and risks in what we heard

- **KVKK and LinkedIn.** The view that LinkedIn-sourced candidates need no consent is risky. KVKK's information duty (aydınlatma) applies however the data was collected, and sharing a CV with a client is a transfer. We should build for consent on every candidate and let their lawyer relax rules, not the reverse. We are not lawyers; this needs their counsel's confirmation.
- **No LinkedIn integration (decided).** We will not use the LinkedIn API or scrape LinkedIn. Its only sanctioned route, the Recruiter System Connect partner program, is not needed for the goal, which is that the firm's data lives in its own app. Data enters by manual entry, CV upload, email forwarding and the website application form.
- **No volumes yet.** We do not know user count, candidate pool size, or searches per year. These decide architecture and pricing.
- **Billing is unknown.** "Ne faturası kesiliyor?" was left open; it decides the proposal and finance modules.

## Competitive landscape

Every serious competitor converges on the same core: CRM + ATS in one database, longlist/shortlist management, a client portal, and GDPR tooling. None of them is built for Turkish KVKK, and none handles executive search and volume staffing equally well in one workspace.

| Product | Built for | Features we adopt | Weak spot we exploit |
| --- | --- | --- | --- |
| [Invenias (Bullhorn)](https://www.getapp.com/all-software/a/invenias/) | Retained and boutique search firms | Longlist/shortlist management, competitor mapping, branded client deliverables, BD pipeline and cross-sell tracking, GDPR tools, Outlook integration | [Reviewers](https://www.capterra.com/p/132632/Invenias/reviews/) call it slow, clunky and dated |
| [Clockwork](https://www.clockworkrecruiting.com/features) | Boutique retained search, in-house teams | Client portal with online longlist review and feedback, real-time status reports, branded emails, candidate scoring vs criteria, research strategy, BCC email capture, bulk edit, deals and contracts, SSO, mobile app | [Users](https://www.getapp.com/all-software/a/clockwork-recruiting/) want better search and data accuracy |
| [Ezekia](https://www.capterra.co.uk/software/207963/ezekia) | Executive search, PE/VC talent teams | Assignments + clients + candidates + BD in one place, client portal, client-ready reporting, dashboards, advanced querying, Outlook integration | Generic templates; no local compliance |
| [Vincere](https://platform.softwareone.com/product/executive-search-software/PCP-8580-2094) | Agencies of all types, incl. exec search | Retainer tracking, branded LiveList client microsites, talent pools and saved searches, revenue forecasts, client and candidate portals, invoicing | [Reviewers](https://recruitwithatlas.com/resources/vincere-crm-review/) cite feature bloat and keyword search without context |
| [Thrive TRM](https://www.ismartrecruit.com/comparison/thrivetrm-vs-invenias) | Elite search firms, VC/PE talent teams | Relationship intelligence and network mapping, references and referrals, team dashboards | Priced for large firms; [estimated](https://www.ismartrecruit.com/comparison/thrivetrm-vs-ezekia) $30,000+ per year |
| [Loxo](https://www.loxo.co/wip-pricing-page) | Agencies and exec search | ATS + CRM + sourcing + multi-channel outreach in one platform | Value is its sourcing database, which we cannot match; we compete on owning the firm's own data |
| [Vinter](https://theaiinsider.tech/?p=29525) | Early-stage and volume hiring, Turkey + global | Competency inventories, skills and language tests, async video interviews, status updates to candidates | Assessment tool, not a CRM; candidates and clients live elsewhere |

### Table stakes (we must have these on day one)

- One database for accounts, contacts, candidates and assignments, with a full activity history.
- Assignment pipeline with longlist and shortlist, plus candidate status per stage.
- Client portal or shared longlist with client feedback.
- Branded candidate reports and progress reports.
- Email and calendar integration (Outlook and Gmail), with automatic logging.
- Boolean and filtered search across the pool, saved searches and talent pools.
- Privacy tooling: consent, retention, deletion requests.
- Roles and permissions, plus SSO.

### Where they all fall short

- **Speed and UX.** The most common complaint in exec search reviews is slowness and friction, not missing features.
- **Volume work.** Exec search tools assume one seat per assignment; staffing tools lose the relationship depth. The client needs both.
- **Local compliance.** GDPR features exist; KVKK aydınlatma, açık rıza and 2-year retention do not.
- **Assessments.** Exec search CRMs push assessments to outside tools; the client asked for one system.

## Product principles and UX strategy

We win on how it feels to use every day: a consultant should log a call, move a candidate and send a shortlist without leaving the page they are on.

### Principles

1. **Fast by default.** Every list opens under 300 ms; search returns as you type. Speed is the feature incumbents lack.
2. **Capture once, reuse everywhere.** A CV, a call note or a contact's details are entered once and appears on the candidate, the assignment and the account.
3. **Compliance is invisible until it matters.** Consent status is a coloured badge on every candidate; the system blocks a share only when consent is missing, and offers the fix in one click.
4. **One screen per job-to-be-done.** "My day", "This search", "This client". No setting-heavy admin panels for consultants.
5. **The client sees our best side.** Every client-facing output (report, portal, email) is branded and polished by default.
6. **Simple first, powerful on demand.** Clean defaults; advanced filters, custom fields and bulk actions are one click away, not in the way.

### UX patterns we use

- **Command bar (Ctrl/Cmd + K).** Jump to any candidate, client or search; run actions like "log call" or "add to shortlist" by typing.
- **Keyboard-first pipeline.** Move candidates between stages with arrow keys; J/K to step through the list; inline edit everywhere.
- **Kanban and table views** of the same pipeline, switchable, with saved views per user.
- **Side panel previews.** Clicking a candidate opens a panel over the list instead of a new page, so context is never lost.
- **Quick log.** One box at the top of any record for notes, calls, meetings; type `@` to mention a colleague, `#` to link a search.
- **"My day" home.** Today's interviews, overdue follow-ups, consent expiring this week, client feedback waiting.
- **Drag-and-drop CV upload** anywhere, with duplicate detection before a new record is created.
- **Undo instead of confirm.** Actions happen immediately with an undo toast; confirmation dialogs only for irreversible steps such as deletion.
- **Bilingual UI** (Turkish and English) with Turkish-aware search (ı/i, ş/s, ğ/g treated as matches).
- **Mobile-friendly** consultant views for calls and notes on the go; clients review shortlists on their phone.

### How we prove it

- Time to log a call: under 10 seconds.
- Time to build and send a branded shortlist of 5 candidates: under 3 minutes.
- Onboarding: a new consultant runs a search without training in their first hour.
- We usability-test each module with 2 or 3 of the firm's consultants before the pilot.

## Core modules

Ten modules cover everything in the notes; modules 1 to 5 and 9 form the pilot MVP, and module 6 (staffing) is deferred. Each lists what it does and where the idea comes from.

### 1. Accounts and contacts (client CRM)

- Account page: company profile, sector, locations, contacts, open and past assignments, proposals, revenue to date, account owner. *(Salesforce reference, notes)*
- Contact records with role, reporting line, relationship strength, last contact date, and a "was a candidate" link. *(Thrive network mapping)*
- Org chart per account, drawn from contacts. *(Invenias competitor mapping)*
- Account health: days since last touch, open proposals, active searches; red flag if a key account is cold for 60 days.
- Firm overview: every account on one screen, filterable by owner, sector, status. *(notes: genel overview)*

### 2. Business development pipeline

- Opportunity = an account + roles needed + estimated fee + stage (Lead → Meeting → Proposal sent → Negotiation → Won / Lost). *(notes: contact, hangi işler aranıyor; Clockwork deals)*
- Activities logged against it: client visit, call, proposal, follow-up. *(notes)*
- Kanban view with pipeline value per stage and a win-rate report.
- Won opportunity becomes an assignment in one click, carrying over roles, terms and contacts.

### 3. Proposals and contracts

- Proposal builder from templates: scope, roles, fee model, payment stages, guarantee period, off-limits terms. *(notes: proposal koşulları; Clockwork contracts)*
- Versions with diff, status, sent date, PDF in firm branding, e-signature hand-off.
- Accepted terms flow into the assignment and later into billing.

### 4. Assignments (searches)

- Assignment types: Executive Search and Recruitment, each with its own default stages, editable per firm (Staffing is deferred, see module 6).
- Search brief: role, must-haves, nice-to-haves, compensation band, target companies, off-limits companies. *(Clockwork research strategy)*
- Candidate pipeline: Research → Contacted → Screened → Longlist → Shortlist → Client interview → Offer → Placed, plus Rejected / Withdrew with reasons.
- Target company list with "researched / contacted / exhausted" per company. *(Invenias competitor mapping)*
- Scorecard per candidate against brief criteria, filled by consultant and client. *(Clockwork scoring)*
- Team on the search, tasks, deadlines, and a search timeline.

### 5. Candidate pool

- One record per person: contact, current role, career history, education, languages, compensation, notice period, mobility, CVs (versioned), documents.
- Duplicate detection by email, phone, LinkedIn URL and name + company; merge tool.
- Every assignment the candidate was part of, and every client they were presented to, with outcome. *(notes: aynı CV'ler farklı firmalar için)*
- Search: full text in CVs, boolean, filters, Turkish-aware; saved searches and talent pools (e.g. "CFO-ready, Istanbul"). *(Vincere talent pools)*
- Capture: drag-and-drop CV, bulk CV upload with parsing, email forwarding to a pool address, website application form. No LinkedIn import or scraping; the LinkedIn URL is a plain field. *(Clockwork bulk upload)*
- Tags, custom fields, bulk edit. *(Clockwork)*

### 6. Volume staffing mode (deferred, not in current scope)

The team decided not to build staffing (bulk hiring and layoffs) for now. It stays on record because the client raised it (the 400-staff, 200-store example) and it is a likely upsell later. To keep the door open, the data model already allows an assignment to hold many positions and locations, so adding it later needs no migration.

What it would contain when picked up:

- A staffing order as a positions × locations grid with headcount per cell.
- Bulk actions: shortlist, move stage and message a whole cohort at once.
- Fill-rate dashboard per location and role against the deadline.
- Location-aware candidate search (distance to store, city, district).
- Outplacement cohorts for layoffs (çıkarım) as their own pipeline.

### 7. Assessments

- Send a competency inventory or test from the candidate record; track sent / started / completed; store results and report PDF on the record. *(notes: yetkinlik envanteri tek sistemden; Vinter)*
- Start with integrations (the firm's current inventory providers, Vinter) rather than building tests ourselves.
- Structured interview guide and scorecard per assignment.

### 8. Reports and client portal

- Branded candidate report: summary, career, assessment results, consultant view, compensation. *(notes: aday raporları)*
- Anonymised version in one click: hides name, photo, age, gender, nationality. *(notes: blindlama)*
- Weekly progress report per assignment, generated from activity: companies researched, people contacted, longlist size, next steps. *(Invenias, Clockwork)*
- Client portal: client logs in, sees the shortlist, comments, rates, requests interviews; every view and comment is logged. *(Clockwork, Vincere LiveList)*

### 9. KVKK and compliance

- Consent record per candidate: source, aydınlatma sent date, açık rıza status, consent text version, IP and timestamp.
- Automatic email on CV upload with a link to read the aydınlatma text and give or refuse açık rıza. *(notes: KVKK maili)*
- Sharing guard: a candidate without consent cannot go into a client report or portal; the consultant can resend the request in one click.
- Retention: 2-year clock from last consent; reminder at 60 and 14 days; renewal link; automatic anonymisation at expiry. *(notes: 2 yıl)*
- Data subject requests (access, correction, deletion) with a log and response deadline.
- Audit log of every view, export and share of personal data.

### 10. Activity, email and calendar

- Outlook and Gmail sync: emails with known contacts log automatically; BCC address as fallback. *(Clockwork, Invenias)*
- Calendar sync and interview scheduling with client and candidate.
- Email templates with merge fields, in Turkish and English.
- Unified timeline on every record.

## Differentiators beyond competitors

These are the features no competitor in the table offers together; each answers something the client said, not a feature for its own sake.

| Differentiator | What it does | Why it beats the market | From the notes |
| --- | --- | --- | --- |
| KVKK-native consent engine | Consent badge on every candidate, auto aydınlatma + açık rıza email, sharing guard, 2-year retention with renewal and auto-anonymise, audit log | Competitors ship GDPR checkboxes; none models Turkish consent text versions or the 2-year rule | KVKK izni çok önemli; 2 yıl havuz |
| Candidate exposure history | For each candidate: which clients saw them, for which role, when, and the outcome; warns before re-presenting to the same client or to an off-limits client | Others show assignment history but not client-level exposure and conflicts | Aynı CV'ler başka firmalar için |
| Off-limits guard | Marks companies the firm must not poach from (current clients, guarantee periods); flags candidates employed there | Usually a manual list in exec search; we enforce it at the moment of adding to a longlist | Executive search practice |
| Relationship graph | Shows how the firm knows a person: past placements, candidates who became clients, contacts who moved companies, consultant who owns the relationship | Thrive has this at $30k+; mid-size firms get it nowhere | Networking önemli |
| Alumni-to-client tracking | Alerts when a placed candidate or known contact moves into a hiring role at a new company | Turns the pool into BD leads automatically, without AI | Networking; account growth |
| Firm-owned talent database | Every candidate, note, CV and history entry lives in the firm's app, with duplicate checks, consent and full export; nothing depends on LinkedIn | Firms that keep their network inside LinkedIn lose it when consultants leave or licences lapse; here it stays with the firm | Asıl amaç LinkedIn'den data kendimize gelsin |
| Assessments inside the record | Send inventories, track completion, store results next to the CV and in the client report | Exec search CRMs push this to outside tools | Yetkinlik envanteri tek sistemden |
| One-click blind report | The same branded report with identifying fields removed, for fair first review | Rare in exec search tools; Phase 2 adds AI redaction of free text | Blindlama |
| Auto-written progress report | Weekly client update built from logged activity, edited and sent in one screen | Saves consultants hours of manual reporting each week | Aday raporları; client trust |
| Speed and keyboard-first UI | Command bar, inline edit, side-panel previews, sub-second lists | Directly targets the top review complaint about incumbents | Easy-to-follow UI requested |
| Turkish-first, bilingual | Turkish and English UI, templates and Turkish-aware search | Global tools treat Turkish as an afterthought | Turkish client, Turkish candidates |

## Data model and architecture

The model centres on the **candidacy**: one candidate in one assignment. That lets a person sit in many searches for many clients, while consent decides what any client may see.

&#91;embedded content: core data model · 9 entities and the shared activity log\]

A won opportunity becomes an assignment; candidates join it as candidacies; reports and the portal only show candidates whose consent is valid. A staffing order (deferred) would be an assignment with many positions and locations, so the model already allows it.

### Architecture

- **Backend:** Java with Spring Boot, as a modular monolith (one deployable, clear module boundaries). Plays to the team's Java strength and splits cleanly later.
- **Frontend:** React + TypeScript, a single-page app with a shared component library and Turkish/English i18n.
- **Database:** PostgreSQL with Turkish collation; Postgres full-text search for the pilot, OpenSearch with a Turkish analyser once the pool grows.
- **Files:** S3-compatible object storage, encrypted at rest; Apache Tika extracts CV text for search (no AI).
- **Email and calendar:** Microsoft Graph and Gmail API for sync; a transactional provider for consent and portal emails.
- **Security:** SSO (Microsoft, Google), role-based access, per-assignment visibility for confidential searches, append-only audit log, field-level encryption for compensation and IDs.
- **Hosting:** single-tenant for the pilot, region to be agreed (Turkey or EU) under KVKK transfer rules.
- **Phase 2 readiness:** every change emits an event, and structured fields stay clean, so the AI layer can read them without a rewrite.

## Roadmap

The proposal is an 18-week path from kickoff to a finished pilot, with AI and staffing kept out until the core has proven itself.

&#91;embedded content: roadmap · 5 phases, 3 gates, staffing deferred\]

The week counts are our proposal, to adjust once the client answers the scope questions below. Phase 2 AI items come from the notes (sector and job-posting suitability, blind CVs, interview simulation) and need no change to the Phase 1 data model.

## Questions to ask the client before building

The notes leave these open; the first five decide the MVP scope, so we ask them in the next meeting.

**Scope and scale**

- [ ] How many consultants, researchers and admins will use the system?
- [ ] How many candidates are in the pool today, and in what form (Excel, folders, Salesforce, email)?
- [ ] How many active searches run at once, and how many per year?
- [ ] Which Salesforce objects do they use today, and must we migrate that data?
- [ ] What does a search stage list look like in their own words? Can we see one real (anonymised) search end to end?

**Billing ("Ne faturası kesiliyor?")**

- [ ] What is the fee model: retainer in stages, success fee, percentage of salary, fixed fee?
- [ ] When is an invoice issued, and do they need invoicing in our tool or only the triggers?
- [ ] Is there a guarantee or replacement period after a placement?

**KVKK and data**

- [ ] Has their lawyer confirmed the view that LinkedIn-sourced candidates need no consent? Can we get their aydınlatma and açık rıza texts?
- [ ] Is the 2-year retention counted from upload, from last contact, or from last consent?
- [ ] Where must data be hosted (Turkey, EU)? Any cross-border transfer limits?
- [ ] Besides LinkedIn, how do candidates reach them today (email, website, referrals), so we cover each entry route?

**Assessments and reports**

- [ ] Which competency inventories do they use today, and from which provider?
- [ ] Can they share a current candidate report and a client progress report as templates?
- [ ] Do clients want a login portal, or is a branded PDF plus a share link enough for the pilot?

**Pilot**

- [ ] Which consultants and which 2 or 3 live searches go into the pilot?
- [ ] What result after 8 weeks would make them sign: time saved, fewer tools, better client feedback?
- [ ] Is volume staffing something they expect from us later, and roughly when?

## Sources

- [Clockwork Recruiting features](https://www.clockworkrecruiting.com/features)
- [Clockwork Recruiting on GetApp](https://www.getapp.com/all-software/a/clockwork-recruiting/)
- [Invenias on GetApp](https://www.getapp.com/all-software/a/invenias/)
- [Invenias reviews on Capterra](https://www.capterra.com/p/132632/Invenias/reviews/)
- [Ezekia on Capterra UK](https://www.capterra.co.uk/software/207963/ezekia)
- [Vincere Executive Search Software, SoftwareOne](https://platform.softwareone.com/product/executive-search-software/PCP-8580-2094)
- [Vincere review, Atlas](https://recruitwithatlas.com/resources/vincere-crm-review/)
- [Thrive TRM vs Invenias, iSmartRecruit](https://www.ismartrecruit.com/comparison/thrivetrm-vs-invenias)
- [Thrive TRM vs Ezekia, iSmartRecruit](https://www.ismartrecruit.com/comparison/thrivetrm-vs-ezekia)
- [Loxo pricing](https://www.loxo.co/wip-pricing-page)
- [Vinter seed funding, AI Insider](https://theaiinsider.tech/?p=29525)
- [LinkedIn Recruiter System Connect overview](https://learn.microsoft.com/en-au/linkedin/talent/recruiter-system-connect)
- [Example KVKK candidate notice with 2-year retention, TP Vision](https://www.tpvision.com/policy/TPV-Calisan-Adayi-Aydinlatma-Metni-v2.0.pdf)
