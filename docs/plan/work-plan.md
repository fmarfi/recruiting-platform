# Work plan v2: re-arranged after the Product Plan (Phase 1, non-AI)

Date: 2026-10-25 · Status: PROPOSAL, replaces the wave order in `docs/archive/mastermind-plan.md` section 5 · Source: `docs/product/product-plan-phase1.md` (the "Product Plan").

## 1. What changed

| Before | After the Product Plan |
| --- | --- |
| Phase 1 = CRM only (A-01..A-14). The candidate pool, CV upload, duplicates, reports and consent e-mail (B-01..B-05, B-09, B-13, S-01) were Phase 2. | Phase 1 (non-AI) also holds the **candidate pool, CV capture, consent engine, assignments with longlist/shortlist, branded and blind reports**. Pilot MVP = Product Plan modules 1-5 and 9. |
| Headline = relationship intelligence. | Headline = **one system the firm's consultants run live searches in for 8 weeks**: pool + consent + assignments + client-ready output, fast and keyboard-first. Relationship graph stays a differentiator, after the pool. |
| Volume staffing "Could" (B-10). | **Deferred** (module 6). Keep the model open: one assignment may hold many positions/locations. |
| LinkedIn open. | **No LinkedIn API, no scraping.** LinkedIn URL is a plain field; data enters by hand, CV upload, e-mail forward, web form. |

Requirement mapping (new Phase 1 scope): B-01 (upload + text extraction with Tika; structured parsing stays AI/Phase 2), B-02 (pool search: Postgres full text, Turkish-aware), B-03 (source channel), B-04 (duplicates), B-05 (candidate reuse + exposure history), B-08 (blind report, fields hidden, no AI), B-09 (branded report, PDF), B-13 (web application form, see open questions), S-01 (auto consent e-mail on upload), S-03 (retention reminders at 60/14 days, renewal link, auto-anonymise), S-04.

## 2. Waves (re-ordered)

Gate for every wave stays as in `docs/plan/TASKS.md`: green build and tests, reviewed branches, then 2-3 consultants complete the core tasks unaided.

### Wave 2a: finish the "speed" layer (in flight)
Already merged or nearly: DataGrid, command bar (Ctrl+K), accounts/network/job orders on the grid, saved views (local), db tables for tags, saved views, bulk actions.
Remaining, kept small: `/views` (backend-accounts), `/bulk-actions` with consent gate (backend-pipeline + security-privacy), quick-log composer and timeline on every record, path component, "My day" polish. **Drop from 2a:** dashboards v1 (moves to 2d).
Product Plan fit: UX patterns (command bar, kanban/table, side panel, quick log, undo toast).

### Wave 2b: candidate pool and consent engine (NEW, highest priority; modules 5 + 9)
Architect first (contract 1.2.0), then db, backends, security-privacy, frontends.
- Candidate profile on `person`: career history, education, languages, compensation (field-level encryption), notice period, mobility, source channel, LinkedIn URL.
- CV files: upload (drag and drop, bulk), versions, S3-compatible store (local volume for dev), Tika text extraction, full-text search; retention-aware.
- Duplicate detection (e-mail, phone, LinkedIn URL, name + company) before create; audited merge.
- Consent: aydınlatma + açık rıza e-mail on upload with tracked link, consent text versions, retention clock, reminders at 60 and 14 days, renewal link, auto-anonymise, DSR log (extends what S-01/S-03/S-04 already have).
- Sharing guard: no consent, no client report or portal; one-click resend.
- Talent pools and saved searches (extends saved views), tags and bulk edit (tags already in db).
- **Exposure history and off-limits guard**: which clients saw a candidate, outcome; off-limits companies enforced when adding to a longlist.

### Stage Structure: clean structure and modelling (after the Wave 2b backends merge, before Stage UX; requested by the user 2026-10-26)
Detail and rationale: `docs/architecture/adr/0012-target-structure-and-modelling.md` (PROPOSED). Runs with NO agent worktrees open, in this order, each step a reviewed branch that must leave `mvn verify -Pintegration`, frontend tests and Modulith verification green with no behaviour change:
1. Docs reorganisation and one entry point (`docs/product`, `architecture`, `plan`, `process`, `archive`; merge START-HERE and NEXT-SESSION into `docs/plan/STATE.md`).
2. Context map from one event-storming pass; decide the `talent` extraction (ADR 0012 item 2).
3. Backend layout: module and package names, tests moved into their modules, `api` packages, ArchUnit layer rules, jMolecules, Modulith Documenter and C4 output in CI.
4. Contracts: split OpenAPI per tag with bundling, enums from one YAML into Java/TS/DB checks, generated ER diagram.
5. Frontend: Feature-Sliced layout with eslint-plugin-boundaries.
6. Rich domain extraction, incrementally, starting with pipeline stage guards and consent/retention.

### Stage UX: engineer the UX and UI in every detail (after Stage Structure, before 2c; requested by the user 2026-10-26)
Goal: perfect, professional and accessible. Nothing built so far has been looked at in a real browser by a person, so this stage starts with seeing it.
1. **Audit first:** run the full app (Docker, seed, backend, frontend) and click through every screen on desktop, tablet and phone widths with the Chrome browser tools; screenshots and a defect list per screen in `docs/product/ux/audit-<date>.md`. Compare against `docs/product/ux/screens/*.md`, `docs/product/ux/lightning-style.md` and the Product Plan UX principles (under 300 ms lists, command bar, undo instead of confirm, side panels, quick log under 10 s).
2. **Accessibility to WCAG 2.2 AA:** semantic landmarks and headings, full keyboard operation with visible focus and no traps (grid, command bar, dialogs, kanban moves with keys), screen-reader names/roles/live regions (axe-core in vitest and Playwright on every route; manual NVDA pass), contrast of every status badge and chip in `design-tokens.json`, target size 24 px minimum, reduced motion, 200 percent zoom and 320 px reflow, form labels, errors and help text tied to fields, no colour-only meaning, TR and EN `lang` attributes and no truncated translations.
3. **Design system pass:** one component inventory (buttons, inputs, tables, dialogs, toasts, empty/loading/error states, badges), spacing and type scale, density options, consistent copy (TR and EN), icon set, microcopy for every error code, empty-state guidance, skeletons instead of spinners, undo toasts.
4. **Performance and feel:** measured list and search latency against the 300 ms target, optimistic updates, no layout shift, print styles for reports later.
5. **Usability test script** for 2-3 consultants (the Product Plan proof points: log a call under 10 s, shortlist of 5 in under 3 minutes, a new consultant runs a search in the first hour).
Owners: ux-designer (audit, specs, tokens), frontend-shell and frontend-pipeline (fixes), qa-engineer (axe, Playwright, keyboard and visual regression suites), code-reviewer (accessibility review). Gate: zero axe violations on every route, every defect from the audit closed or consciously deferred in writing, keyboard-only walkthrough of the core tasks passes, consultants complete the core tasks unaided.

### Wave 2c: BD and assignments (modules 2, 3, 4)
- Opportunity stages Lead → Meeting → Proposal sent → Negotiation → Won/Lost, value per stage; won becomes an assignment in one click.
- Proposals with versions, fee terms, status, PDF (fee model still open, see section 4).
- Assignment = search brief (must/nice-to-have, band, target and off-limits companies), pipeline Research → Contacted → Screened → Longlist → Shortlist → Client interview → Offer → Placed (+ Rejected/Withdrew with reason), target-company list, scorecards, team and tasks. Extends the existing job order and pipeline; stage list needs the client's own words.
- Account health (cold for 60 days), firm overview, org chart per account.

### Wave 2d: client-ready output (module 8, part of 10)
Branded candidate report and shortlist PDF, **one-click anonymised version**, weekly progress report generated from activity, shortlist share link (portal only if the client wants a login, see open questions), e-mail templates TR/EN with merge fields, calendar view and due-task digest, dashboards v1 (placements, fee-weighted pipeline, time to fill, conversion).

### Wave 2e: relationship edge and integrations
Relationship strength, warm-path finder, alumni-to-client alerts, "we have been here before", compliance cockpit for the DPO, assessments by integration (provider/Vinter, module 7), Outlook/Gmail sync and BCC logging (table stakes in the Product Plan; heavy, so after the pilot core works), SSO.

### Later / parked
Volume staffing (module 6), AI (Phase 2: parsing, suitability, shortlist, AI redaction, interview simulation), custom fields beyond tags, report builder, forecasting, automation flows.

## 3. Immediate order of work (this week)

1. Finish and merge Wave 2a in flight: db review fixes, frontend-pipeline (verified, reviewed), then `wave2a-integration`.
2. **Architect now (in parallel with 2a backends):** contract 1.2.0 for Wave 2b (candidate profile, files, consent e-mail events, exposure, off-limits, duplicates) plus the open ADR 0010 questions (free text in `bulk_action.params`, tags on anonymisation). Contracts change only through the architect.
3. Launch 2a backends (`/views`, `/bulk-actions`) with the review change requests: clear tags on anonymisation (PersonDataService, AuditPurger), purge or avoid free text in bulk action params.
4. Wave 2b build as soon as contract 1.2.0 is reviewed and frozen.
5. Run the app end to end and click through every new screen (nothing new has been seen in a browser yet).

## 4. Decisions and client questions that gate work

| Question | Gates | Working assumption until answered |
| --- | --- | --- |
| Retention clock: from upload, last contact or last consent? | S-03, 2b | From last consent (Product Plan wording); configurable per `retention_settings`. |
| KVKK for LinkedIn-sourced candidates | 2b consent rules | **Build consent on every candidate**; their lawyer may relax, never the reverse. |
| Fee model and invoicing triggers | 2c proposals, 2d dashboards | Fixed + percent of salary + stages as optional fields; no invoicing module. |
| Client login portal or branded PDF + share link? | 2d | PDF + share link for the pilot. |
| Website application form (S-02 / B-13) | 2b | Not built; earlier decision (2026-10-16) kept until the client confirms. |
| Their own search stage list and a real anonymised search | 2c | Product Plan default stages, editable. |
| Who may create shared saved views | 2a | Managers and admins. |
| Hosting region (Turkey or EU), cross-border limits | 2b file store | Local volume now; decide before pilot (`docs/architecture/hosting.md`). |
| Volumes (users, pool size, searches per year) | search engine choice | Postgres full text for the pilot; OpenSearch later. |

## 5. Pilot proof points (from the Product Plan)
Time to log a call under 10 s; branded shortlist of 5 in under 3 minutes; a new consultant runs a search in the first hour; lists open under 300 ms; each module usability-tested with 2-3 of the firm's consultants before the pilot.
