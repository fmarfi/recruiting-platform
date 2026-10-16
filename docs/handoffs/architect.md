# Hand-off: architect (Wave 0, tasks W0-03 to W0-06)

Branch: `worktree-agent-a4ae1054721951d61`. Not pushed, not merged.

## What changed

| Path | Content |
| --- | --- |
| `contracts/openapi.yaml` | OpenAPI 3.1, 90 operations / 60 paths, all Phase 1 endpoints plus users/roles/auth, KVKK consent (records, requests, e-mail status, public link), privacy notices, retention, data-subject requests, person export/anonymise, audit, contacts CRUD, network links CRUD, CSV import, paging, RFC 9457 errors. Lints clean (Redocly), Prism mock runs. |
| `contracts/enums/*.java` | 6 lifecycles implementing `Lifecycle` (transitions, `requiresReason`, `requiresElevatedRole`, labels, `Tone`), 16 vocabularies incl. `ErrorCode`. Package `com.recruiting.platform.contracts`. |
| `contracts/events/*.java` | `DomainEvent` + 18 event records (1 is Phase 2: `CandidateShortlisted`). |
| `contracts/errors/*.java` | `ApiException`, `IllegalTransitionException.check(from, to, reason)`. |
| `contracts/schema.md` | Tables, keys, indexes, ownership per module, Envers scope, anonymisation procedure. |
| `contracts/events.md` | Delivery rules, catalogue with listeners, main flows. |
| `contracts/README.md` | Every contract file; lint, compile and mock commands; Vite proxy snippet. |
| `docs/adr/0001`–`0008` | Modular monolith + boundaries; enum transition tables (not Statemachine); React SPA (not Vaadin); UUID v7; Envers + access log + timeline; REST conventions; session auth + roles; one person identity. |

Status-lifecycle rules from the product-analyst (relayed by the orchestrator) are in the enums and the contract: reasons for account → ON_HOLD/CLOSED, CLOSED → LEAD, proposal → REJECTED, job order → CANCELLED, stage → REJECTED; manager-only account → CLOSED, CLOSED → LEAD, job order CANCELLED → OPEN; PLACED only from OFFER; guards (FILLED needs a PLACED application, no close with open job orders, no new job orders/candidates on closed accounts or job orders, proposal terms read-only after SENT); every `…StatusChanged` event has `reason`.

Vocabularies I decided (the analyst proposed these be my call): `ConsentStatus` NOT_SENT/SENT/GIVEN/REFUSED/WITHDRAWN (as demo, plus a transition table); `DataSubjectRequestStatus` RECEIVED/IN_PROGRESS/COMPLETED/REJECTED; `ActivityType` CALL/VISIT/MEETING/EMAIL/NOTE (NOTE is not a contact); `LinkType` KNOWS/WORKED_WITH/INTRODUCED/REFERRED/REPORTS_TO. If `docs/status-lifecycles.md` uses other labels for consent or DSR, the orchestrator should keep the enum names and copy the labels into the enums at merge (I can do it in a follow-up).

## What others must know

- **Module boundaries (ADR 0001):** allowed deps — security → contracts; accounts → contracts, security; pipeline → contracts, security, accounts; privacy → contracts, security, accounts, pipeline; ai → all. Upward flows go by events in `contracts`. Public API = the module's top-level package; `domain`, `internal`, `web` sub-packages are internal.
- **Ownership deviations from the build plan:** account status (A-02, `PATCH /accounts/{id}/status`), `/reference-data` and `/search` are implemented in `accounts` (backend-accounts). backend-pipeline keeps the timeline, job orders, pipeline, proposals, activities, next actions, dashboard, My day. Job orders join search through the `SearchContributor` SPI in accounts.
- **Mock:** paths are served at the root (no `/api/v1`), needs any `JSESSIONID` cookie; see `contracts/README.md`.
- **Frontend folders (ADR 0003):** `frontend/src/{shell,accounts}` frontend-shell, `frontend/src/{pipeline,network}` frontend-pipeline, `frontend/src/api` generated from `openapi.yaml`.
- **db-engineer:** enums are `varchar` + CHECK, not PG enums; UUID v7 app-generated; migration names `V<yyyyMMddHHmm>__<module>_<what>.sql`; extensions `unaccent`, `pg_trgm` (and `vector` later); Envers `_aud` must be purged on anonymisation.
- **security-privacy:** consent tokens are stored hashed (`token_hash`), 30-day expiry; DSR due date = received + 30 days.

## What I need from devops (I did not touch any pom.xml)

1. `contracts` Maven module (artifact `contracts`, groupId `com.recruiting`): compile **every `*.java` under `contracts/`** (`enums/`, `events/`, `errors/`; paths do not match the package, that is fine for javac), Java release 21, **no dependencies**. Every other module depends on it.
2. `platform-app` depends on all modules, `spring-modulith-starter-core`, `spring-modulith-starter-jpa` (event publication registry), and `spring-modulith-starter-test` for the verification test:
   ```java
   class ModularityTest {
       @Test void verify() { ApplicationModules.of(PlatformApplication.class).verify(); }
   }
   ```
   `PlatformApplication` and the global `ApiExceptionHandler` sit in the root package `com.recruiting.platform`.
3. A `package-info.java` per module (in each module's top-level package; needs `spring-modulith-api` on that module):
   ```java
   @org.springframework.modulith.ApplicationModule(allowedDependencies = {"contracts", "security"}) // accounts
   package com.recruiting.platform.accounts;
   ```
   Values: security `{"contracts"}`; accounts `{"contracts","security"}`; pipeline `{"contracts","security","accounts"}`; privacy `{"contracts","security","accounts","pipeline"}`; ai `{"contracts","security","accounts","pipeline","privacy"}`. No `package-info` with `@ApplicationModule` in `contracts` (it stays dependency-free).
4. Flyway location `platform-app/src/main/resources/db/migration`; Postgres image with `pgvector`; init `CREATE EXTENSION IF NOT EXISTS unaccent; pg_trgm; vector`.
5. CI: add `npx -y @redocly/cli@1 lint contracts/openapi.yaml`, plus the frontend build with `openapi-typescript` generation once `frontend/` exists.

## What is left / open issues

- **Modulith verification test not run**: it depends on the devops skeleton; run it after merging both branches (contracts must compile as the `contracts` module and the empty modules must have their `package-info`).
- **Status lifecycles** were taken from the demo plus the relayed rules; reconcile with the analyst's file at merge (see above).
- **Open client questions** handled as ASSUMPTIONs: retention basis default `FROM_CREATED` (configurable), no record-level visibility per consultant, status lists not user-editable, Turkish default language.
- **Proposed TASKS.md change requests** (orchestrator): move A-02 account status endpoint, `/reference-data` and `/search` to backend-accounts; frontend ownership paths under `frontend/src/`; add W1 task "devops: contracts module + package-info + Modulith test".
- Website consent form (S-02) is Phase 2 (B-13); `ConsentChannel.WEBSITE_FORM` is reserved.
- The ADR 0003 React choice should be confirmed by the orchestrator/client before Wave 1 frontends start.

## Review fixes

- Contacts keep history: `contact.ended_on`, `Contact.endedOn/active`, `DELETE` ends the contact (200 + Contact), `active` filter (default true) on the list; contact roles of a person show ended ones.
- `account_open_job` projection (upsert by job_order_id) replaces the counter rule; `ProblemDetail.blockingItems` plus 409 close-account example.
- `Reason` 3..500 trimmed; enforced in `IllegalTransitionException.check` (VALIDATION_FAILED on size).
- Pilot ownership rule in openapi info, 403 description and ADR 0007 addendum; website form is Phase 2 (`WEBSITE_FORM` reserved).
- `ConsentStatus`: self-transitions removed, triggers documented; re-send is the action `POST /people/{id}/consent/requests`.
- schema.md: `event_publication` needs a Flyway migration (ddl-auto validate); anonymisation scrubs timeline reason/summary, nulls company, lists all `_aud` tables; unconfirmed imports purged after 30 days.
- `ConsentStatusChanged.reason`, `JobOrder.openings` (>=1, default 1), fictional example company names, `/reference-data` stated read-only.
- Lint 0 errors; `mvn -pl contracts compile` exit 0.
