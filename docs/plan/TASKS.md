# Task board

Status: todo · in progress · review · done · blocked. Change requests to another owner's files go under "Change requests".

## Wave 0 — contracts (no features)

| ID | Task | Owner | Status | Blocked by |
| --- | --- | --- | --- | --- |
| W0-01 | User stories with Given/When/Then criteria for all Phase 1 Must/Should requirements (A-01–A-14, S-01–S-04, UX-01–UX-10) | product-analyst | done | – |
| W0-02 | `docs/plan/status-lifecycles.md`: closed status sets and transitions for accounts, proposals, job orders, pipeline stages | product-analyst | done | – |
| W0-03 | Module/package layout and ADRs (UI framework, status-machine approach) | architect | in progress | – |
| W0-04 | `contracts/schema.md` from the core data model | architect | in progress | – |
| W0-05 | `contracts/openapi.yaml` for all Phase 1 endpoints + mock server command | architect | in progress | W0-02, W0-04 |
| W0-06 | `contracts/enums/` and `contracts/events.md` | architect | in progress | W0-02 |
| W0-07 | User flows, wireframes and screen specs for the 6 core screens | ux-designer | done | W0-01 (can start from requirements) |
| W0-08 | `contracts/design-tokens.json` incl. a badge per status | ux-designer | done | W0-02 |
| W0-09 | Maven multi-module skeleton, docker-compose (Postgres+pgvector, Mailpit), CI, env config | devops | done | W0-03 |
| W0-10 | `docs/architecture/hosting.md` with KVKK data-location options | devops | done | – |

**Gate 0 → 1**
- [x] Contracts merged and frozen (2026-10-16, after review fixes)
- [x] Mock server runs from `openapi.yaml` (Prism 5, 200 with `JSESSIONID=mock`)
- [x] `mvn -B verify` green locally; Modulith verification passes. CI on GitHub not run yet (no remote)
- [x] Stories done for all Phase 1 Musts (28 stories)

## Wave 1 — Phase 1 build (8 builders in parallel)

Runs in two parts so builders never guess shared foundations. Endpoints are listed by OpenAPI tag (`contracts/openapi.yaml`).

### Wave 1a: foundations (parallel)

| ID | Task | Owner | Status | Blocked by |
| --- | --- | --- | --- | --- |
| W1-01 | Flyway migrations for every table in `contracts/schema.md` (extensions unaccent, pg_trgm; vector deferred to Phase 2), `immutable_unaccent` function, CHECK constraints from enums, indexes incl. trigram search, Spring Modulith `event_publication` table, `account_open_job` projection | db-engineer | done | Gate 0 |
| W1-02 | JPA entities + Envers + repositories per module in `<module>/.../<module>/domain/`; repository tests as Testcontainers `*IT` (`mvn -B verify -Pintegration`) | db-engineer | done | Gate 0 |
| W1-03 | `frontend/` scaffold: Vite + React + TS strict + Mantine theme from design tokens, `openapi-typescript` client, React Router with placeholder routes per screen, i18next TR/EN, Vitest, `npm run mock` (Prism) | devops | done | Gate 0 |
| W1-04 | CI: frontend job, Prism mock-server smoke job; app Dockerfile; add `spring-modulith-starter-jpa` (and exclude its auto-config in the test profile); contracts sources via build-helper `add-source` instead of `sourceDirectory=${project.basedir}`; update devops hand-off | devops | done | Gate 0 |

**Wave 1a result (2026-10-16):** merged after review (no blockers; review fixes applied). On main: `mvn -B verify -Pintegration` green (Docker), frontend typecheck/test/build green, Docker image builds. Repo line endings normalised to LF (`.gitattributes`).

### Wave 1b: features (parallel, after 1a merges)

Shared briefing: `docs/process/context/wave-1b.md` (ports, shared files, auth, demo login).

| ID | Task | Owner | Status | Blocked by |
| --- | --- | --- | --- | --- |
| W1-05 | Settle Wave 1a schema change requests (activity FK vs CHECK; `data_subject_request_aud` purge): update `contracts/schema.md` + migration + IT | architect | done | – |
| W1-10 | Tags Reference data, Accounts (incl. account status A-02), Contacts, People, Network links, Import, Search | backend-accounts | done | W1-02 |
| W1-11 | Tags Job orders, Pipeline, Proposals, Activities, Next actions, Account overview, Home; status transition rules from `docs/plan/status-lifecycles.md`; timeline from events; `DELETE /applications/{id}` only in SOURCED (409 otherwise) | backend-pipeline | done | W1-02 |
| W1-12 | Tags Auth, Users, Audit (session + CSRF, 3 roles, access log); Consent, Public consent, Privacy notices, Retention, Data-subject requests (S-01, S-03, S-04; S-02 is Phase 2); logins written to the access log; consent token masked in logs | security-privacy | done | W1-02, W1-05 |
| W1-13 | Shell, My day, Accounts list + Account page, Search, Settings (UX-01, 02, 05, 07–09) | frontend-shell | done | W1-03 |
| W1-14 | Job orders + pipeline board, Network list + Person page, Quick add (UX-03, 04, 06) | frontend-pipeline | done | W1-03 |
| W1-15 | Demo dataset: fictional accounts in every status, people, job orders, pipelines, history; loaded with one command | synthetic-data | done | W1-01 |
| W1-16 | Contract tests (responses validated against openapi.yaml) and Playwright smoke tests against the mock; test plan per Must story | qa-engineer | done | W1-03 |

**Gate 1 → 2**
- [x] All Phase 1 Must endpoints and screens merged (2026-10-20; `mvn -Pintegration verify` and frontend tests green; screens not yet clicked through against the real backend)
- [x] Demo data loads with one command (`seed/load.sh` / `load.ps1`; includes Envers baseline rows)

## Wave 2 — integrate and harden (re-arranged 2026-10-25, see `docs/plan/work-plan.md`)

| Wave | Scope | Status |
| --- | --- | --- |
| 2a | Speed layer: DataGrid, command bar, saved views, bulk actions, composer, path component | in progress: contract 1.1.0, frontend-shell and db merged or in review; `/views` and `/bulk-actions` backends todo |
| 2b | Candidate pool and consent engine: candidate profile, CV files + Tika, duplicates, consent e-mail + retention reminders, sharing guard, talent pools, exposure history, off-limits guard | todo: architect contract 1.2.0 first |
| UX | **Stage UX (after 2b, before 2c, user request 2026-10-26):** browser audit of every screen, WCAG 2.2 AA accessibility, design-system pass, performance, usability test; see `docs/plan/work-plan.md` | todo: starts when 2b backends are merged |
| 2c | BD and assignments: opportunities, proposals with versions and PDF, search brief, longlist/shortlist stages, scorecards, org chart, account health | todo |
| 2d | Client-ready output: branded and anonymised candidate report, weekly progress report, share link, e-mail templates, calendar, dashboards v1 | todo |
| 2e | Relationship edge and integrations: strength, warm path, alumni alerts, compliance cockpit, assessments, Outlook/Gmail sync, SSO | todo |
| Parked | Volume staffing (model stays open), AI (Phase 2), LinkedIn integration (decided: none) | – |

**Gate 2 → 3**
- [ ] End-to-end tests pass on every Must story
- [ ] 2–3 consultants complete the core tasks unaided (UX-10)
- [ ] Pilot feedback triaged

## Wave 3 — Phase 2 AI tool

**Phase 2 ready**
- [ ] CRM pilot live
- [ ] AI evaluation set agreed with the client

**Wave 1b status (2026-10-19):** all builders merged on main except W1-12 follow-ups (security swaps interim adapter, then reviewed). Light enterprise UI redesign merged (design-tokens v1.1.0). Open: real-login check with seeded DB, TOO_MANY_REQUESTS error code, Playwright CI job (devops), person export sections from pipeline.

## Change requests

| From | To | Request | Status |
| --- | --- | --- | --- |
| architect | orchestrator | Account status, `/reference-data`, `/search` go to backend-accounts | done (W1-10) |
| architect | devops | `contracts` module compiles all contract Java; module `allowedDependencies` per ADR 0001 | done in wave-0 merge |
| ux-designer | architect | ~15 endpoints the screens need beyond the demo API | done; remaining gaps settled in docs/product/ux/README.md |
| ux-designer | ux/frontends | Jobs item in the main navigation | W1-13 |
| db-engineer | architect | `contracts/schema.md`: `activity.person_id ON DELETE SET NULL` conflicts with `ck_activity_target`, so deleting a person with a person-only activity fails (breaks S-03 delete). Suggested: `ON DELETE CASCADE` + IT | todo, before W1-12 |
| db-engineer | architect | `contracts/schema.md`: `data_subject_request_aud` missing from the anonymisation purge list; decide keep (legal evidence) or purge | todo, before W1-12 |
| db-engineer | security-privacy | `AuditorAware` bean + Envers `RevisionListener`, so `created_by`/`updated_by`/`revinfo.user_id` get filled | W1-12 |
| devops | security-privacy | Permit `/actuator/health` (unauthenticated, no details); then devops adds Dockerfile HEALTHCHECK | W1-12 |
| devops | devops | Bundle frontend into the app jar; add ESLint + CI lint step; dark-mode tokens into the Mantine dark palette | before pilot |
| code-reviewer | backend-accounts, security-privacy | Anonymisation must clear `tags` (ADR 0010): `PersonDataService` and the `person_aud` scrub in `AuditPurger` | todo, with 2a backends |
| code-reviewer | architect | `bulk_action.params` / `bulk_action_item.message` can hold free text (SET_NEXT_ACTION title) that no purge covers: store only ids and enums, or add a purge | todo, before `/bulk-actions` |
| frontend-pipeline | architect, backend-accounts | `GET /people` needs `city` and `company` filters (command bar sends them; only filtered client-side per page today) | todo |

## Decisions log

- 2026-10-25 · Product Plan (Phase 1, non-AI) adopted as scope input: candidate pool, CV capture, consent engine, assignments and branded/blind reports move into Phase 1; volume staffing deferred; no LinkedIn API or scraping. Wave order in `docs/plan/work-plan.md`. ASSUMPTIONS until the client answers: retention from last consent, consent on every candidate, PDF + share link instead of a portal, no website form yet.

- 2026-10-16 · S-02 (website consent form) moves to Phase 2 with B-13: no website form in Phase 1.
- 2026-10-16 · Pilot: any consultant may change any record; manager-only moves stay manager-only (ASSUMPTION).
- 2026-10-16 · Status lists are read-only in Settings (ADR 0002); labels and tones come from `/reference-data`, design tokens map tone to colour only.
- 2026-10-16 · Frontend: React + TypeScript + Mantine (ADR 0003), confirmed by the orchestrator; revisit only if the client asks for Vaadin.
- 2026-10-16 · Wave 1 split into 1a (foundations) and 1b (features).
- 2026-10-16 · Docker Desktop installed: DB tests use Testcontainers (`*IT`, `-Pintegration`) instead of embedded PostgreSQL.
- 2026-10-16 · Repo line endings are LF (`.gitattributes`); agents must not rewrite files to CRLF.
- 2026-10-16 · Migrations use timestamp names (`V202610160001__...`) as schema.md specifies.

## Open questions blocking work

From `docs/product/requirements.md` → Open questions. Agents mark dependent work as ASSUMPTION until answered:
- Status lists used today (affects W0-02)
- Hosting location (affects W0-10)
- CV languages (affects Phase 2 parser)
- Retention start point: upload or last contact (affects S-03)
- 2026-10-19 · UI is a light, professional enterprise theme (navy accent, no dark mode); design-tokens.json v1.1.0 changed visually under a contract-freeze exception.

## Plan v3 — vertical slices (adopted 2026-10-29)

Source: `docs/plan/Mihenk — Plan, Design and Agent-Crew Strategy.md`. Decision by the user: adopt it, but do NOT wait for the real-data gate (hosting, DPA, real pool import). Build on synthetic data; S1 hosting/DPA parts and the real import are parked.

| ID | Slice | Owner | Status | Blocked by |
| --- | --- | --- | --- | --- |
| C13-R | Review `contract-1-3` against plan v3 (R63, R65, R70 gaps) | code-reviewer | done | – |
| S3 | Glossary TR/EN (R4), four-area routes (R1), pinned live searches | frontend-shell | done | – |
| S6 | Edit everywhere for client, candidate, search with F0 primitives | frontend-pipeline | done | – |
| C13-F | Fix review blockers (role gating deferred post-pilot) + add R63 organisation, R65 longlist_status, R70 activity fields | architect | done (frozen 20f309d) | C13-R |
| C13-DB | Migrations, entities, Envers, classification IT for contract 1.3 | db-engineer | done | C13-F |
| S4 | Quick log composer (R70) | backend-pipeline | done (merged cd0d437) | C13-DB |
| S5 | Longlist table, bulk add, pool filters (R65): backend | backend-pipeline | done (merged 4be7baa) | C13-DB |
| S5-UI | Longlist table, bulk add, pool filters (R65): UI | frontend-pipeline | done (merged ca4f055) | contract 1.3 |
| V3-TABLE | Superseded by M2-UI1 (client meeting 02: no board, longlist only) | frontend-pipeline | superseded | – |
| UI-A | Polish: day plurals, one date format, client activity without inner scroll, rule 'no record counts' | ui-slice (Haiku) | done (merged 104ddb6) | – |
| UI-B1 | Contract 1.4 Home aggregate: today, needsYou, searches health, generatedAt + ADR thresholds | architect (Opus) | done (merged d8c7e8a) | – |
| UI-B2 | Home aggregate backend, < 300 ms, R17 filtering, IT per needsYou type | backend-pipeline | on hold: home gains personal numbers and a period in contract 1.5 | M2-C15 |
| UI-B3 | Home v2 UI (consultant preset): Today, Needs you, live-search health, quick log bar | frontend-shell | on hold | M2-C15 |
| UI-C | Search health on searches list, search header, cards; activity panel on search page; records without count tiles | frontend-pipeline | todo | UI-B2, V3-TABLE |
| S2 | Organisation identity + import (synthetic file) (R63) | – | todo | C13-DB |
| S1 | Dev-login guard only (TOTP, roles, hosting/DPA parked: post-pilot) | – | todo | – |

## Client meeting 02 follow-up (2026-11-16)

Source: `docs/product/meetings/2026-11-15-meeting-02.md` (first real user input; earlier stories were assumptions).

| ID | Slice | Owner | Status | Blocked by |
| --- | --- | --- | --- | --- |
| M2-R | Desk research, 9 topics, into `docs/product/research/meeting-02-research.md` | research agent (Sonnet) | in progress | – |
| M2-UI1 | Search page longlist only: board hidden, table with stage column and stage change; candidate-driven actions on the person page (add to a search, change stage) | frontend-pipeline (Sonnet) | in progress (.worktrees/m2-longlist, branch m2-longlist) | – |
| M2-SHOTS | Screenshot pack of candidate, client and position screens with field lists for the client | orchestrator | todo | app running |
| M2-C15 | Contract 1.5: move candidate between searches; contact on job order; openings and remaining openings; home personal numbers with period | architect (Opus) | todo | M2-R |
| M2-ST | Rewrite stories from the meeting report; mark the 28 old ones keep / change / drop | product-analyst | todo | M2-R |
| M2-UI2 | Agreements UI separate from proposals, each with history | frontend-pipeline | todo | M2-UI1 |
| M2-UI3 | Network map on the client page | frontend-shell | todo | M2-C15 |

Decisions (2026-11-16, from the client): no kanban board, longlist only (board code kept, hidden); 3 to 5 users; real roles are researcher, consultant, sales (recorded, gating still deferred until after the pilot); work is table-first because the team works in Excel today.

## UI/UX polish pass (2026-11-03, Haiku slice agents, orchestrator reviews in the browser on seeded data)

| ID | Slice | Owner | Status | Blocked by |
| --- | --- | --- | --- | --- |
| UX-FIX | Account form: sector optional since contract 1.3 (tsc broke after gen:api) | orchestrator | done (354c34f) | – |
| UXS-1 | App shell: phone nav in drawer, token spacing, sentence-case nav | slice agent | done (merged) | – |
| UXS-3 | Clients list: name affordance, dimmed activity, empty state | slice agent | done (merged) | – |
| UXP-1 | Board: column headers, card hierarchy, drag feedback, own scroll | slice agent | done (merged) | – |
| UXP-4 | Candidates list: filter row, name/role hierarchy, dimmed secondary | slice agent | done (merged) | – |
| UXP-4b | Candidates filters collapsed to one row + Filters panel | slice agent | done (merged) | – |
| UXP-1b | Board fits laptop width, labelled consent badge, stale-days tone | slice agent | done (merged) | – |
| UXS-4 | Client record page: header facts, timeline by day, empty states | slice agent | done (merged) | – |
| UXP-5 | Person page: header facts, tabs, activity by day, empty states | slice agent | done (merged) | – |
| UX-Q1 | Open question: phone bottom tab bar removed in UXS-1 (nav only in drawer); revisit after phone check | orchestrator | todo | – |
| V3-SPEC | docs/product/ux/visual-system-v3.md (binding visual rules: no cards-in-cards, no pills, Tabler icons, one header/table/toolbar/badge) + @tabler/icons-react 3.31.0 | orchestrator | done (4c7e29c) | - |
| V3-F | Foundation: objectIcons, theme radii/sizes, badges, PageHeader, EmptyState, header/nav, DataGrid | general-purpose (Sonnet) in .worktrees/ux-foundation | done (merged) | - |
| V3-S1 | Home: KPIs per spec (time/progress), de-card sections | ui-slice | done (merged) | V3-F |
| V3-S2 | Clients list + client page: de-card, toolbar, icons | ui-slice | done (merged) | V3-F |
| V3-S3 | Candidates list + person page: de-card, square filter chips, consent banner | ui-slice | done (merged) | V3-F |
| V3-S4 | Search page + board: de-card header, consent icon+words on cards | ui-slice | done (merged) | V3-F |
| V3-S5 | Searches list, settings, dialogs/drawers: de-card, icons | ui-slice | done (merged) | V3-F |
| V3-FORMS | New menu flows per spec 6b: quick add, new/edit candidate, new/edit client | ui-slice x3 | done (merged) | - |
| V3-LAYOUT | Panel/DetailList/RecordLayout; person, client, search pages and home on record layout | ui-slice + orchestrator | done (d363ce8, eccdf7f, eaa5cad, 405fb74) | V3-S1..S5 |
| V3-GATE | Orchestrator browser check of every screen against spec section 8 at 1440 and 375 | orchestrator | todo | V3-S1..S5 |

Agent cost note: general-purpose subagents start near 70k tokens (all connector tools). Use the lean ui-slice agent (.claude/agents/ui-slice.md: 6 tools, Haiku, frontend-design preloaded); it loads only in a session started from the capital-D path C:\Users\SID-DRW\Desktop\New folder\recruiting-platform, which also fixes automatic worktrees.
