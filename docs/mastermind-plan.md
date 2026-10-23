# Mastermind plan: the firm's brain, not just a CRM

Date: 2026-10-23 · Status: PROPOSAL for approval · Builds on `docs/salesforce-gap.md` (feature gaps) and `docs/requirements.md`.

## 1. Idea

Salesforce stores records. An executive search firm lives on **who knows whom, who has gone quiet, and who can open which door**. The app should answer those questions before the consultant asks them, and show the firm's whole memory (clients, people, searches, history) in one place that is fast to search and impossible to lose.

Three pillars:

1. **Salesforce parity where it earns its keep** (lists, bulk actions, reports, dashboards, calendar, e-mail, files, duplicates). Taken from the gap analysis.
2. **Relationship intelligence** (the differentiator): relationship strength, warm-path finder, alumni and target-company maps, coverage and white-space.
3. **Firm memory + KVKK-native**: every past search, shortlist, rejection reason and contact stays findable; consent and retention are a built-in filter and gate on every action, not an add-on.

"No competitor does this" is a hypothesis, not a verified claim. Salesforce and Bullhorn have no weighted introduction-path finder or consent-gated actions out of the box as far as the repo's research shows; validate against Bullhorn/Vincere/Invenias demos before using it in sales material.

## 2. Salesforce features we add (from the gap analysis, reordered)

| Feature | Our version | Wave |
| --- | --- | --- |
| List views: column chooser, multi-filter, sort, group-by, saved private/shared, inline edit | One **Data Grid** component used for accounts, people, jobs, proposals | 2a |
| Mass actions | Reassign owner, change status (through the status machine), tag, set next action, consent request; confirm step plus audit entry per record | 2a |
| Global search | **Command bar (Ctrl+K)** with operators, see 3.1 | 2a |
| Reports and dashboards | Fixed set first (3.6), user-defined later | 2b |
| Activity composer + timeline on every record | One panel on account, person, job order | 2a |
| Calendar of actions and meetings, due-task digest | Month/week view, drag to reschedule | 2b |
| E-mail templates (TR/EN, merge fields) + logging | Spring Mail; auto-logged; consent-gated | 2b |
| Files on records (CV, proposal PDF) | Retention and DSR aware; prepares B-01 | 2b |
| Duplicate detection + merge | Fuzzy name/e-mail/phone/company; warn, not block; audited merge | 2b |
| Import wizard with field mapping + duplicate check | Extends A-11 | 2b |
| Path component (stage guide) | Guided stage bar with a tip per stage on job order and account | 2a |
| Report/Data export | CSV on every grid (frontend done for network list) | 2a |
| Later / skip | Custom fields, report builder, forecasting, automation flows, account teams, mailbox sync, Chatter | Later |

## 3. Differentiating features

### 3.1 Command bar (Ctrl+K), the fastest way to anything
- Searches accounts, people, jobs, proposals; actions too ("log call Mehmet", "new job at Acme", "go to My day").
- **Operator syntax**: `owner:me stale:90 city:izmir type:exec consent:given company:"Y Holding"`; operators become filter chips and can be saved as a list.
- Turkish-aware: accent-insensitive and typo-tolerant (`unaccent` + `pg_trgm` already in the schema).
- Recent items, pinned records, keyboard only.

### 3.2 Relationship intelligence
- **Relationship strength score** per person (0-100) from recency, frequency and depth of touchpoints, link count, and meeting vs e-mail weight. Shown as warm/cooling/cold (a client-side version of this exists in the person snapshot).
- **Warm-path finder:** "How do I reach Elif Yıldırım?" shows the strongest introduction chain through our network (up to 3 hops), ranked by link strength and consent state, with a one-click "ask for intro" task. Replaces the manual expand on the relationship map.
- **Account coverage:** which client contacts nobody has spoken to in 90 days, and the warm paths into each (first version already built).
- **Org chart per account** from `REPORTS_TO` links, with gaps marked ("no contact above this level").
- **Alumni graph:** everyone in the network who ever worked at a client or at a target company (from employment history) is a door into it.

### 3.3 Search-assignment workbench (job order = mission control)
- **Target-company map:** pick the companies to source from; see the people we already know there, their strength, owner and consent state; gaps are the research list.
- **Internal first:** before sourcing, show pool/network people who fit, past shortlisted-and-rejected candidates for similar roles (with the old rejection reason), and referrers connected to the target companies.
- Pipeline board plus conversion and time-in-stage on the same page.

### 3.4 Firm memory
- **"We have been here before":** opening a job order, account or person surfaces prior searches, who was presented, outcome and reasons; searchable across the whole history.
- Everything versioned (Envers already in place); a "what changed" view per record.
- Unified timeline per person, account and job; nothing is lost when a consultant leaves.

### 3.5 KVKK-native by design
- Consent and retention status are filters in every grid and the command bar; mass actions and e-mail **cannot** include people without a valid basis (hard gate, with an explanation, not a silent skip).
- **Compliance cockpit** for the DPO: consent funnel, flagged for retention, open data requests, one-click evidence export per person.
- Every export and mass action is audited and visible to managers.

### 3.6 Dashboards that fit executive search
Not win rate. Centre on: placements and fees, fee-weighted pipeline, time to fill, stage conversion, candidates per job, accounts by status, dormant key relationships, consultant workload, coverage per account. Firm view for partners, "my numbers" for consultants.

### 3.7 Data quality engine
Completeness score per record, duplicate queue, stale-data queue ("no contact 12 months, owner left"), enrichment tasks generated automatically. Keeps the "mastermind" trustworthy.

### 3.8 Explore views for the same data
Table, board (any status list), calendar, **group-by and pivot** (company, sector, city, owner), and a city distribution view for the network. One data model, many lenses.

## 4. What needs the architect (contract is frozen)

| New | Why | Owner |
| --- | --- | --- |
| `GET /search` operators + `POST /views` (saved/shared lists) | 3.1, grid views | architect, backend-accounts |
| `GET /people/{id}/relationship` (strength) and `GET /network/path?from=&to=` | 3.2, needs graph query (recursive CTE on `network_link`) | architect, backend-accounts |
| `GET /accounts/{id}/org-chart`, `GET /accounts/{id}/alumni` | 3.2 | backend-accounts |
| `GET /job-orders/{id}/target-companies` (+ CRUD) and `/history-suggestions` | 3.3, 3.4 | backend-pipeline |
| `POST /bulk-actions` (typed, per-record result) | mass actions with audit | backend-pipeline + security-privacy |
| `GET /reports/*`, fee fields on job order | 3.6 | backend-pipeline, db-engineer |
| Attachments, e-mail templates, duplicates/merge | Salesforce parity | db-engineer, security-privacy |
| Calendar events | calendar | backend-pipeline |

Frontend-only now (within the contract): data grid shell, command bar over `/search`, composer, calendar of `/next-actions`, snapshot and coverage views, CSV export.

## 5. Build order

- **Wave 2a (feel the product):** Data Grid with saved views and mass actions on network and accounts, command bar, activity composer, path component, dashboards v1 on existing data. First backend work: saved views and bulk actions.
- **Wave 2b (parity and trust):** reports with fees, calendar, e-mail templates and logging, files, duplicates and merge, import mapping.
- **Wave 2c (the moat):** relationship strength, warm-path finder, org chart, alumni and target-company map, "we have been here before", compliance cockpit.
- **Wave 3:** AI on top: suitability score, shortlist, blind mode, reports. The graph and history make the AI better (referrals, prior outcomes).

Gate for each wave stays as in `docs/TASKS.md`: green build and tests, pilot consultants complete the core tasks unaided.

## 6. Decisions needed from you

1. Is the **relationship intelligence** pillar the headline (my recommendation) or should reporting come first?
2. May backend contract changes (section 4) go through the architect now, or stay frontend-only until the pilot?
3. Shared saved views: any consultant, or managers only?
4. Fees on job orders: fixed, percent of salary, or both?
