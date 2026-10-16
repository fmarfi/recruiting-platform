# Task board

Status: todo · in progress · review · done · blocked. Change requests to another owner's files go under "Change requests".

## Wave 0 — contracts (no features)

| ID | Task | Owner | Status | Blocked by |
| --- | --- | --- | --- | --- |
| W0-01 | User stories with Given/When/Then criteria for all Phase 1 Must/Should requirements (A-01–A-14, S-01–S-04, UX-01–UX-10) | product-analyst | done | – |
| W0-02 | `docs/status-lifecycles.md`: closed status sets and transitions for accounts, proposals, job orders, pipeline stages | product-analyst | done | – |
| W0-03 | Module/package layout and ADRs (UI framework, status-machine approach) | architect | in progress | – |
| W0-04 | `contracts/schema.md` from the core data model | architect | in progress | – |
| W0-05 | `contracts/openapi.yaml` for all Phase 1 endpoints + mock server command | architect | in progress | W0-02, W0-04 |
| W0-06 | `contracts/enums/` and `contracts/events.md` | architect | in progress | W0-02 |
| W0-07 | User flows, wireframes and screen specs for the 6 core screens | ux-designer | done | W0-01 (can start from requirements) |
| W0-08 | `contracts/design-tokens.json` incl. a badge per status | ux-designer | done | W0-02 |
| W0-09 | Maven multi-module skeleton, docker-compose (Postgres+pgvector, Mailpit), CI, env config | devops | done | W0-03 |
| W0-10 | `docs/hosting.md` with KVKK data-location options | devops | done | – |

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

Shared briefing: `docs/context/wave-1b.md` (ports, shared files, auth, demo login).

| ID | Task | Owner | Status | Blocked by |
| --- | --- | --- | --- | --- |
| W1-05 | Settle Wave 1a schema change requests (activity FK vs CHECK; `data_subject_request_aud` purge): update `contracts/schema.md` + migration + IT | architect | in progress | – |
| W1-10 | Tags Reference data, Accounts (incl. account status A-02), Contacts, People, Network links, Import, Search | backend-accounts | in progress | W1-02 |
| W1-11 | Tags Job orders, Pipeline, Proposals, Activities, Next actions, Account overview, Home; status transition rules from `docs/status-lifecycles.md`; timeline from events; `DELETE /applications/{id}` only in SOURCED (409 otherwise) | backend-pipeline | in progress | W1-02 |
| W1-12 | Tags Auth, Users, Audit (session + CSRF, 3 roles, access log); Consent, Public consent, Privacy notices, Retention, Data-subject requests (S-01, S-03, S-04; S-02 is Phase 2); logins written to the access log; consent token masked in logs | security-privacy | todo | W1-02, W1-05 |
| W1-13 | Shell, My day, Accounts list + Account page, Search, Settings (UX-01, 02, 05, 07–09) | frontend-shell | in progress | W1-03 |
| W1-14 | Job orders + pipeline board, Network list + Person page, Quick add (UX-03, 04, 06) | frontend-pipeline | in progress | W1-03 |
| W1-15 | Demo dataset: fictional accounts in every status, people, job orders, pipelines, history; loaded with one command | synthetic-data | in progress | W1-01 |
| W1-16 | Contract tests (responses validated against openapi.yaml) and Playwright smoke tests against the mock; test plan per Must story | qa-engineer | in progress | W1-03 |

**Gate 1 → 2**
- [ ] All Phase 1 Must endpoints and screens merged
- [ ] Demo data loads with one command

## Wave 2 — integrate and harden

**Gate 2 → 3**
- [ ] End-to-end tests pass on every Must story
- [ ] 2–3 consultants complete the core tasks unaided (UX-10)
- [ ] Pilot feedback triaged

## Wave 3 — Phase 2 AI tool

**Phase 2 ready**
- [ ] CRM pilot live
- [ ] AI evaluation set agreed with the client

## Change requests

| From | To | Request | Status |
| --- | --- | --- | --- |
| architect | orchestrator | Account status, `/reference-data`, `/search` go to backend-accounts | done (W1-10) |
| architect | devops | `contracts` module compiles all contract Java; module `allowedDependencies` per ADR 0001 | done in wave-0 merge |
| ux-designer | architect | ~15 endpoints the screens need beyond the demo API | done; remaining gaps settled in docs/ux/README.md |
| ux-designer | ux/frontends | Jobs item in the main navigation | W1-13 |
| db-engineer | architect | `contracts/schema.md`: `activity.person_id ON DELETE SET NULL` conflicts with `ck_activity_target`, so deleting a person with a person-only activity fails (breaks S-03 delete). Suggested: `ON DELETE CASCADE` + IT | todo, before W1-12 |
| db-engineer | architect | `contracts/schema.md`: `data_subject_request_aud` missing from the anonymisation purge list; decide keep (legal evidence) or purge | todo, before W1-12 |
| db-engineer | security-privacy | `AuditorAware` bean + Envers `RevisionListener`, so `created_by`/`updated_by`/`revinfo.user_id` get filled | W1-12 |
| devops | security-privacy | Permit `/actuator/health` (unauthenticated, no details); then devops adds Dockerfile HEALTHCHECK | W1-12 |
| devops | devops | Bundle frontend into the app jar; add ESLint + CI lint step; dark-mode tokens into the Mantine dark palette | before pilot |

## Decisions log

- 2026-10-16 · S-02 (website consent form) moves to Phase 2 with B-13: no website form in Phase 1.
- 2026-10-16 · Pilot: any consultant may change any record; manager-only moves stay manager-only (ASSUMPTION).
- 2026-10-16 · Status lists are read-only in Settings (ADR 0002); labels and tones come from `/reference-data`, design tokens map tone to colour only.
- 2026-10-16 · Frontend: React + TypeScript + Mantine (ADR 0003), confirmed by the orchestrator; revisit only if the client asks for Vaadin.
- 2026-10-16 · Wave 1 split into 1a (foundations) and 1b (features).
- 2026-10-16 · Docker Desktop installed: DB tests use Testcontainers (`*IT`, `-Pintegration`) instead of embedded PostgreSQL.
- 2026-10-16 · Repo line endings are LF (`.gitattributes`); agents must not rewrite files to CRLF.
- 2026-10-16 · Migrations use timestamp names (`V202610160001__...`) as schema.md specifies.

## Open questions blocking work

From `docs/requirements.md` → Open questions. Agents mark dependent work as ASSUMPTION until answered:
- Status lists used today (affects W0-02)
- Hosting location (affects W0-10)
- CV languages (affects Phase 2 parser)
- Retention start point: upload or last contact (affects S-03)
