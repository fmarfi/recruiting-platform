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
- (done after merge) Directories are adapters over `AccountApi`/`PersonApi` and security's `UserDirectory`; `Actor` is gone, services use `CurrentUser`. Job orders are served in `/search` by `joborder/search/JobOrderSearchContributor`.
- Open: `AccountsApiIT.importValidatesThenCommits...` fails at its last line (GET `/import-templates/PEOPLE` as manager returns 403); accounts/security issue, not pipeline.
- `_aud` purge is privacy's job (schema.md); I only scrub live rows.

**Others must know**
- accounts: I added to your public API (minimal, appended): `AccountSummary` + city, website, notes, statusChangedAt, openedOn, openJobCount, lastActivityAt, createdAt, updatedAt, version; `AccountApi.countByStatus`, `idsBySectorAndStatus`; `PersonSummary` + city, lastContactAt, consentStatus, retentionStatus; `PersonApi.suggest` (candidate picker). Pipeline depends on crm-accounts (ADR 0001).
- accounts: the account-close guard relies on your `account_open_job` projection from my `JobOrderCreated`/`JobOrderStatusChanged`.
- Pipeline listeners use `@ApplicationModuleListener` (async after commit); tests await.

**Change requests** (for files you don't own)
- devops/orchestrator · `platform-app/pom.xml`: added test deps `spring-boot-starter-webmvc-test`, `spring-security-test` (needed for MockMvc ITs).
- devops/orchestrator · `PlatformApplicationTests` removed, replaced by `persistence/ContextLoadsIT` on Testcontainers: feature beans need repositories, the DB-less "test" profile can no longer start. Other agents will hit the same; keep one fix.
- architect · openapi: reason shorter than 3 chars answers `VALIDATION_FAILED`, missing reason `REASON_REQUIRED` (per Transitions); confirm.

**Update 2026-10-19 (export)**
- `pipeline/export/`: `ActivitiesExportContributor` and `ApplicationsExportContributor` implement accounts' `PersonExportContributor`, so GET /people/{id}/export fills `activities` and `applications` (API representation). IT: `PipelineApiIT.personExportIncludesActivitiesAndApplications`.

**Verified with**
- `mvn -q -B -Pintegration verify` → exit 0 (PipelineApiIT 9 tests, TimelineWriterTest 2, ModularityTests, ContextLoadsIT).

# Hand-off: backend-pipeline · Wave 2b · 2026-10-28

**Branch:** `worktree-wave2b-backend-pipeline` (on 3a61e53), commits per slice.

**Done**
- 1 Bulk: `POST /bulk-actions` dispatcher (`bulk/`), SPI `pipeline.BulkActionHandler` (+ `BulkCommand`, `BulkParams`, `BulkItemResult`). Handlers: JOB_ORDERS and PROPOSALS (owner, status via the normal services, tags), SET_NEXT_ACTION for PEOPLE/ACCOUNTS/JOB_ORDERS. One tx per record (REQUIRES_NEW, rollback on FAILED), audit item in its own tx, params keep ids/enums/dates/tag/language only, message = `BulkResultCode.message()`, REQUEST_CONSENT 403 for consultants, whole-request 400s.
- 2 Guards: `pipeline.ShareGuard` SPI; `ApplicationGuards`; `PATCH /applications/{id}/stage` runs consent on every move into a `sharedWithClient()` stage (409 CONSENT_REQUIRED, no override), off-limits on the first one (409 OFF_LIMITS, MANAGER/ADMIN `offLimitsOverrideReason` stored; consultant 403; 3..500), exposure written once. Add (`POST .../applications`) also checks off-limits with the override.
- 3 `GET /people/{id}/exposure` (`accountId` filter), `warnings` (PREVIOUSLY_PRESENTED_TO_ACCOUNT, ACTIVE_IN_OTHER_PROCESS) on add and client-facing moves.
- 4 `pipeline.PipelinePersonApi.mergePerson` (same job order: survivor's application kept, exposures repointed), `RetentionRenewed` closes "Renew consent" action, scrub of `application.off_limits_override_reason`.

**Not done**
- No `GET /bulk-actions` (not in contract 1.2.0). Accounts/privacy handlers (PEOPLE/ACCOUNTS, REQUEST_CONSENT) are theirs; the dispatcher answers 500 INTERNAL_ERROR if none is registered.

**Change requests / merge notes**
- backend-accounts: I added a minimal `accounts/OffLimitsApi` (`List<Match> match(personId, ownAccountId)`); replace with yours and adapt `ApplicationGuards`. Without a bean no rule matches.
- security-privacy: implement `pipeline.ShareGuard.check(personId)` returning `ShareDecision`. Without a bean the guard fails closed (CONSENT_REQUIRED). Call `PipelinePersonApi.mergePerson` in the merge tx.
- db-engineer: `bulk_action.finished_at` is NOT NULL, so the header is inserted with finished_at = started_at and updated at the end.
- Tests: ITs use `GuardTestConfig` (@Primary stubs); after merge, existing ITs that move to client stages need consent GIVEN or the stubs.

**Verified with**
- `mvn -B verify -Pintegration` → BUILD SUCCESS, 123 tests, 0 failures (BulkActionApiIT 8, StageGuardApiIT 7, PipelineMergeErasureIT 3, BulkRequestParserTest 4).
