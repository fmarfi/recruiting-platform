# Consultant review: would I run live searches in this?

Reviewer stance: ex-partner, retained search, 15 years, buying decision. Read-only review on 2026-10-28.
Method: Chrome session on http://localhost:5173 (demo data, signed in as admin) plus curl to :8080/api/v1 and reading frontend/src, contracts/ and crm-pipeline guard code. I did not write data (no Save/Add clicks), so timings are walk-throughs counted in clicks and fields, not stopwatch runs. Competitor statements come from my memory of the products and are marked **(unverified)**.

Scoring: 1 = bad, 5 = excellent. Effort (5 = very few clicks), Clarity, Trust.

## First impression

Clean, fast, calm. Plain navigation (My day, Accounts, Job orders, Network, Settings). TR/EN switch in the header is a real plus for Turkey. Then reality sets in within 60 seconds:

- The home screen of a demo-ready product shows an overdue action called "sad" for "asdas", and a client named "asdas" in sector Construction, city "adsa" (created today, visible in the API). A prospect sees this on screen one.
- The CFO job order, the one I would demo, has candidates headlined "Plant Manager, Vesta" and "Head of Wind Operations". No CFO-looking people on a CFO search.
- Four KPI tiles (Active clients, Open jobs, Candidates in process, Placed this year). A sales-CRM dashboard. No fees, billings, time-to-shortlist or pipeline value.

## Terminology: search firm or generic CRM?

Mixed, leaning generic.

Right: "Job order" (Bullhorn dialect), "Exposure / Presented", "Off-limits", fee terms on the job (retained thirds), "Placed", "Shortlist sent" as a job status, "Client interview", KVKK consent.

Generic CRM language a partner will roll their eyes at: "Lead / Active / On hold" for clients, a "Proposals" tab on a client, "Accounts" in the nav but "client" in the UI (two words for one thing), "Network" for the candidate database, "Sourced / Screened" instead of research stages, one "Type" field mixing Executive / Client contact / Referrer / Candidate in one table, "Warm" relationship badges.

Missing words entirely: longlist, shortlist (as a thing), research, mapping, call sheet, target company list, assignment, mandate, retainer instalments, guarantee.

## Scorecard per task

| # | Task | Effort | Clarity | Trust | What actually happens |
|---|------|:-:|:-:|:-:|---|
| 1 | Onboard a new client with everything I need | 2 | 3 | 2 | New client form asks name, sector (required), city and little else. Contacts, relationships, proposals are separate tabs afterward. No structured place for: fee agreement and retainer schedule at client level, guarantee period, exclusivity, decision process, org chart, NDA/terms signed, billing entity, tax number, off-limits agreed at onboarding. Fee terms exist only as free text on the job order. Off-limits lives in Settings, not on the client. |
| 2 | Log a hiring-manager call in under 10 s | 3 | 3 | 4 | Log activity dialog = Type dropdown (Call) plus mandatory free-text note plus Save. Reachable from the New menu, Ctrl+K and the client page: roughly 5 actions, 10-15 s if you type three words. Faults: no contact picker visible, no outcome, no job link, no follow-up date in the same dialog (Add action is a second dialog), note mandatory so no one-click "left voicemail". |
| 3 | Open a CFO search, longlist 10, shortlist 5 | 2 | 2 | 3 | A job order is a Kanban: Sourced / Screened / Presented / Client interview / Offer / Placed plus Rejected. Longlist = Sourced, Shortlist = Presented, but neither word exists on screen and there is no shortlist object. Add candidate is a modal with a flat list and an Add button per row: no multi-select, so 10 candidates = 10 clicks. No ranking, rating, fit score, rationale or side-by-side compare. Board scrolls horizontally so Placed is cut off at normal width. Job order has no brief fields (comp range, must-haves, spec, target companies, timeline). |
| 4 | See which candidates a client has already seen | 3 | 3 | 4 | Real feature: an Exposure tab per person lists clients they were presented to, and adding someone already presented to this client returns a PREVIOUSLY_PRESENTED_TO_ACCOUNT warning (contract and i18n). Weakness: it is per person, not per client. I cannot open the client and see everyone we ever showed them, when, and the outcome. That is the question the client asks. |
| 5 | Put an off-limits-company candidate on a longlist | 3 | 3 | 4 | Strong design: server-side guard blocks the add (409 OFF_LIMITS in ApplicationService) and again at Presented; only a manager can override, with a written reason (3-500 chars), stored. But demo data has zero rules (GET /off-limits is empty), so it cannot be shown without live setup; rules live in Settings, not on the client; matching is by company, so name drift ("Vesta" vs "Vesta Industries") may leak (not tested). I did not trigger the block in the UI. |
| 6 | Send a candidate a consent request | 4 | 4 | 5 | Candidate page, Privacy tab, "Send consent request"; modal shows recipient email, TR/EN selector, KVKK notice. Consent is a hard gate before presenting (CONSENT_REQUIRED, no override). Bulk request exists in the network list. Privacy tab shows GIVEN with channel, date, notice version and retention end. Strongest area for a Turkish firm. Minor: no preview of the email text. |
| 7 | Find a candidate by skill and city | 2 | 2 | 3 | Network list has name search, type chips, tags, "Search in CVs" (words, "phrase", or, -exclude) and source channel. No City filter on screen and no skills, function, seniority, salary, notice or language filter. The API accepts city (GET /people?city=Ankara returned 11) but the UI does not expose it, and q=Ankara returned 0, so free text does not search city. Skill search only works through CV text; cvMatch is null in demo data. |
| 8 | See my day and what to do next | 4 | 4 | 3 | My day: overdue / today / upcoming actions, open job orders with stage and days open, recent clients, KPI tiles. Easy to read. Faults: junk demo actions, no calendar or interview schedule, no prioritisation ("client waiting for my shortlist"), no stale-search alert (110 days open is only a number). |
| 9 | Produce something I can show a client | 1 | 1 | 1 | Nothing. No shortlist report, candidate profile PDF/Word, client portal, status report or branded template. Only CSV exports of tables. For a retained firm this is the product. |

Average: effort 2.7, clarity 2.7, trust 3.2.

## Information density and layout

- Lists are tidy with Compact/Comfortable, saved Views, Columns and CSV: a good grid layer, better than many CRMs.
- Person Overview is sparse: header, four tiles, two lines of Career, Next actions. No skills, compensation, notice period, languages, mobility, CV preview, interview notes or references on the page (some fields exist in i18n but are not visible on the demo person).
- Job order page: header plus Kanban only. Nothing about the client brief, no search timeline, no comparison, no fee or invoice status.
- Account page: timeline (calls, emails, meetings) and next action are good; the Relationships tab is a differentiator for Turkey.

## What a partner would laugh at

1. Junk records ("sad", "asdas", "adsa") on the first screen.
2. A CFO search whose candidates are a plant manager and a wind-operations head.
3. A drag-and-drop Kanban for what is really a ranked list with narrative.
4. Headline metrics "Active clients 9" and "Placed this year 1" instead of fees, billings and open retainer instalments.
5. "Proposals" on a client tab: that is software-sales CRM vocabulary.
6. Candidates with consent Refused or Withdrawn sitting in a live pipeline, flagged only by a colour chip.
7. English-only fake names, mail.example addresses and +90 000 numbers in a Turkish pitch.
8. A "status" of Open next to an "Open" column that holds a candidate count.

## Turkish-firm trust issues around candidate data

Positive: KVKK notice and consent with versioned notice, per-person retention end date, retention settings, data-subject requests, anonymised rows, audit section, merge audit, TR UI.

Negative or unproven:

- `curl http://localhost:8080/api/v1/people` returned data with no credentials from my shell. Presumably a dev auto-login, but I could not confirm that production enforces auth. If an IT person asks "can I read candidates without logging in?" the demo needs an answer.
- No visible statement on hosting location (Turkey/EU), backups, encryption, VERBIS registration, processor agreement or cross-border transfer. A Turkish buyer asks in the first ten minutes.
- No visible field-level or search-level access control. Email and phone show on the overview for every role; no way to restrict a confidential executive candidate to the search team.
- "Export all (CSV)" of the whole network is one button, with no approval step; unverified whether exports are audit-logged.
- Retention end (28 Feb 2028 for consent given 5 Mar 2026) is plausible but unexplained on screen.

## Comparison with incumbents (all from memory, unverified)

| Task | Bullhorn | Invenias | Vincere | This app |
|---|---|---|---|---|
| Client onboarding | Company record with custom fields, contacts, parent/child; flexible, heavy (unverified) | Company, contacts, fee agreement concepts (unverified) | Company with terms of business (unverified) | Basic form; terms are free text |
| Log a call | Note with action linked to contact and job; mail and phone integration (unverified) | Quick note from anywhere plus Outlook sync (unverified) | Activity with outcome plus calendar and email sync (unverified) | Dialog, no contact/job link, no email sync |
| Longlist / shortlist | Submissions per job; bulk "add to job" (unverified) | Assignments with longlist, shortlist, ratings and a client report: core strength (unverified) | Pipeline with stages and shortlist emailing (unverified) | Kanban, one-by-one add, no ratings |
| Client exposure | Submission history per candidate (unverified) | "Presented to" history and reports (unverified) | Submission history per job (unverified) | Per-person exposure tab plus warning on add |
| Off-limits | Usually custom fields or add-ons (unverified) | Company-level do-not-approach warnings (unverified) | Company flags and blacklists (unverified) | Hard block plus manager override with reason: arguably ahead |
| Consent / privacy | GDPR tooling and retention (unverified) | GDPR module (unverified) | Consent portal and retention (unverified) | KVKK-specific, TR/EN: ahead for Turkey |
| Skill and city search | Boolean search, saved searches, parsed CVs (unverified) | Filters across candidates and companies (unverified) | Boolean and semantic search (unverified) | No skill filter, no city filter in UI |
| Daily view | Dashboard, calendar, tasks (unverified) | Dashboard with assignments and tasks (unverified) | Dashboard and calendar (unverified) | My day: good but thin |
| Client deliverable | Profile formatting, submission email templates, portal (unverified) | Branded client reports, a major selling point (unverified) | Formatted CV and client portal (unverified) | None |

## Top 15 criticisms ranked by impact on tomorrow's sales demo

1. No client-facing output (shortlist report, candidate profile, status report). The demo's climax has nothing to show.
2. Junk data on the home screen ("sad", "asdas", "adsa" client). Clean it before the call.
3. The demo CFO search has non-CFO candidates; the story collapses. Seed believable finance profiles.
4. No longlist or shortlist concept or vocabulary; a generic Kanban stands in.
5. Cannot find candidates by skill, function, seniority, salary or city in the UI (city exists in the API, hidden).
6. No candidate evaluation: ratings, interview notes, references, compensation expectation, notice period on the profile.
7. Adding 10 candidates takes 10 modal clicks; no bulk add from the network list to a job.
8. Off-limits is the best differentiator but has no demo data and lives in Settings; it cannot be shown without live setup.
9. Client onboarding captures name, sector and city only; no commercial terms or client-level off-limits agreements.
10. Generic CRM vocabulary (Lead, Proposals, Accounts vs client, Network) signals a re-skinned sales tool.
11. KPI tiles count things instead of money; no fees, retainer schedule or invoicing view.
12. "Which candidates has this client seen" is answerable only person by person.
13. No visible security posture (hosting, encryption, role-based field access, unauthenticated API call from my shell).
14. Log activity lacks contact, job link, outcome and a follow-up date.
15. Board horizontal scroll hides Placed and Rejected at ordinary widths; consent problems on cards are chip-only with no blocking cue.

## What is genuinely good

- The consent and sharing guard is serious engineering: no presenting a candidate without recorded consent, no override, versioned notice, retention dates, anonymise, audit.
- Off-limits guard with manager override and mandatory reason, enforced server-side at add and at present.
- Exposure history and "previously presented to this client" warnings.
- Relationships and map: who knows whom is how Turkish executive search works.
- Real TR/EN bilingual UI with diacritic-aware search intent.
- Fast, uncluttered UI; Ctrl+K command palette, saved Views, column chooser, compact mode.
- Duplicate detection and audited merge; CV intake that suggests email and phone from CV text.
- My day is clear and actionable.

## Quick wins doable in one day

1. Delete the junk records and seed a believable CFO search: three finance candidates, one with Refused consent, one off-limits rule so the block can be shown live.
2. Rename UI words (translation JSON only): Proposals to Mandates, Sourced to Longlist, Presented to Shortlist, Accounts and Clients to one word, Network to Candidates and contacts.
3. Add City, Seniority and Sector chips to the Network filter bar (the API already supports city).
4. Add a "Printable shortlist" button opening a browser print view of Presented candidates (name, title, company, career lines, consent status). Even a plain page gives the demo an ending.
5. Replace KPI tiles with search-firm numbers derived from existing data: open searches, shortlists pending, days to first shortlist.
6. Show an Exposure list on the client page (group the existing per-person data by account).
7. Add a next-action date field inside Log activity.
8. Show a red banner on pipeline cards whose candidate has Refused or Withdrawn consent.
9. Add a one-line hosting and security statement in Settings, and confirm API auth is on in the demo build.
10. Make the board columns narrower or wrapping so Placed and Rejected are visible.

## The 10 features whose absence ends the deal

1. Client-ready shortlist and presentation report (branded, Word/PDF) plus candidate profile export.
2. Longlist and shortlist as first-class objects with ranking, rating and rationale.
3. Candidate evaluation: interview notes, scorecards, references, compensation expectation, notice period, languages.
4. Skill, function, seniority, city and salary search with saved searches over parsed CVs, boolean included.
5. Email and calendar integration (Outlook/Gmail): automatic activity logging, interview scheduling, templated outreach and consent mails.
6. Bulk actions: add many to a search, bulk outreach, mail merge or sequences that respect consent.
7. Commercial layer: retainer instalments, invoices, fee calculation, guarantee tracking, revenue per consultant.
8. Client portal or secure sharing link for shortlists with client feedback and rejection reasons.
9. Search-level and role-level permissions: confidential searches, restricted candidates, field masking, export approvals.
10. Mapping and research workspace: target-company lists, org charts, researcher call sheets, approach status per target.

## Verdict

As a candidate and consent database with a strong compliance spine, it is promising for Turkey. As a retained-search operating system it is not yet usable for a live search: it cannot produce the deliverable, cannot find people by skill or city, and does not speak the firm's language. I would not run a live search in it today; I would pilot it for candidate data and off-limits control. Do quick wins 1 to 4 before the demo.
