# Hand-off: backend-pipeline · W1-11 · 2026-10-19

**Branch / commits:** `worktree-wave1b-backend-pipeline` · 3 commits on top of main (merged main first)

**Done** (all under `crm-pipeline/.../pipeline/<feature>/{web,app}`, one controller per resource)
- `joborder/`: job orders list/create/get/patch + `PATCH /job-orders/{id}/status` (table, reason, manager-only reopen, FILLED needs a PLACED application, no job on CLOSED account).
- `application/`: pipeline per job order, `PATCH /applications/{id}/stage`, `DELETE` only in SOURCED (409 otherwise), candidate suggestions.
- `proposal/`: CRUD and status moves; terms editable in DRAFT only.
- `activity/`, `nextaction/`: full CRUD; activity edit/delete by author, manager or admin; `ActivityLogged` published.
- `timeline/`: listeners for all events in events.md (accounts + own events) write `timeline_entry` idempotently by `source_event_id`; `GET /accounts/{id}/timeline`. Also `PersonAnonymised` scrub and `RetentionFlagged` follow-up.
- `overview/`: `GET /accounts/{id}/dashboard`, `GET /home`.
- Every status change publishes its `*StatusChanged` event with the reason.

**Not done / left**
- Real `AccountDirectory`/`PersonDirectory`/`UserDirectory` adapters: until accounts/security publish their APIs, `shared/ref/IdOnlyDirectory` (ids as names, every id exists, 0 active accounts) is used. Real adapters must be `@Primary` beans.
- `Actor` reads the user id from `Authentication.getName()` (UUID); switch to security's `CurrentUser` when published (one place: `shared/Actor.java`).
- `_aud` purge is privacy's job (schema.md); I only scrub live rows.

**Others must know**
- security-privacy: controllers use `@PreAuthorize("hasRole('CONSULTANT')")` (needs `@EnableMethodSecurity` + RoleHierarchy) and throw `ApiException`; a handler mapping it to the ADR 0006 error body must exist. `PipelineApiIT` has stand-ins for both (`TestWeb`); drop them when the real ones merge.
- accounts: the account-close guard relies on your `account_open_job` projection from my `JobOrderCreated`/`JobOrderStatusChanged`.
- Pipeline listeners use `@ApplicationModuleListener` (async after commit); tests await.

**Change requests** (for files you don't own)
- devops/orchestrator · `platform-app/pom.xml`: added test deps `spring-boot-starter-webmvc-test`, `spring-security-test` (needed for MockMvc ITs).
- devops/orchestrator · `PlatformApplicationTests` removed, replaced by `persistence/ContextLoadsIT` on Testcontainers: feature beans need repositories, the DB-less "test" profile can no longer start. Other agents will hit the same; keep one fix.
- architect · openapi: reason shorter than 3 chars answers `VALIDATION_FAILED`, missing reason `REASON_REQUIRED` (per Transitions); confirm.

**Verified with**
- `mvn -q -B -Pintegration verify` → exit 0 (PipelineApiIT 9 tests, TimelineWriterTest 2, ModularityTests, ContextLoadsIT).
