# Hand-off: backend-pipeline · M2-AG (agreements API) · 2026-11-16

**Branch / commits:** `m2-agreements-api` from `feature/network-customer-tools` at ede80cd

**Done** (`crm-pipeline/.../pipeline/agreement/{app,web}`)
- All `/agreements` operations: list (accountId, jobOrderId, status[], paging, sort), create, get, PATCH, delete (DRAFT only), `PUT/GET /agreements/{id}/signed-file`.
- Rules: fee model checks, minimum <= cap, per-item exactly one of percent/amount, DATE_OFFSET needs offsetDays, retained thirds sum to 100; kind vs job order (ENGAGEMENT_LETTER needs one, TERMS_OF_BUSINESS none, job order must belong to the account); billing account defaults to the account; signatory must be an active contact of the account (400); SIGNED needs signedOn; archived account or job order gives 409 ARCHIVED; stale `version` gives 409 VERSION_CONFLICT.
- Signed commercial terms or schedule: only MANAGER/ADMIN (403). Schedule replace only while every item is NOT_DUE (409). SIGNED makes SIGNING items DUE (engagement letters only; due_on = today + dueDays).
- Signed file: PDF detected from the bytes (`%PDF-`), 50 MB, else 415/413; key `agreement/<id>/<fileId>`; old bytes deleted after commit; download writes the DOWNLOAD access-log row, attachment + nosniff.
- History: `/audit/revisions` already works for AGREEMENT and PROPOSAL (generic, both entities are `@Audited`, both in the enum). Only a test was needed. No contract change.
- Seed: `seed/09_agreements.sql` (15 agreements: 8 terms of business, 7 engagement letters, all statuses, thirds schedules). `09_envers.sql` renamed `10_envers.sql` so the Envers baseline still runs last; `run.sql`, `00_clear.sql`, README updated.

**Review fixes (round 2)**
- No SQL on accounts tables: `AccountFacts` goes through `AccountDirectory` -> `AccountApi`. For backend-accounts, additions to `accounts/AccountApi.java` and `AccountApiImpl` (methods only): `billingFacts(id)` (archived, vatExempt) and `findContacts(ids)` returning `ContactRef(id, accountId, personId, role, active)`; reusable for a contact on the job order.
- `application.yml` multipart file limit 52MB (CV, import keep their own 10MB code checks). ITs: 30 MB PDF 200, 51 MB 413.
- `00_clear.sql` also clears placement, agreement_aud, fee_schedule_item_aud, placement_aud; seed loaded twice into a scratch DB.
- Schedule-only PATCH bumps version; SIGNED to DRAFT blocked (409) while an item is not NOT_DUE; `scheduleItems: null` is 400; new file bytes deleted on rollback.
- New ITs: archived account/job order 409, DOWNLOAD access log, replaced bytes deleted, person deletion nulls signatory.

- Approved deviation (user decision): signed-PDF limit is 50 MB, not the 20 MB in the contract text (openapi `uploadAgreementSignedFile`, schema.md `signed_file_size` CHECK). New migration `V202611160001__agreement_signed_file_50mb.sql` widens the CHECK (db-engineer: please adopt). The architect updates the contract in 1.5.

**Not done / left**
- No StatusChanged event for agreements (the events contract has none for them); no timeline entry.
- Items `GET /fee-schedule-items` and item invoicing (`FeeScheduleItemUpdateRequest`) are not part of this task.

**Others must know**
- Orchestrator: commit 6dd8e0d on `m2-agreements-ui` contains my staged rename `seed/09_envers.sql -> 10_envers.sql` (two agents shared one worktree index) while its `run.sql` still says 09_envers. Merge `m2-agreements-api` too, or the seed breaks on that branch.
- Frontend: responses carry `signatoryContactId` and `billingAccountId` next to the refs; contact ids come from the account contacts endpoint.

**Change requests**
- none (contract unchanged).

**Verified with**
- `mvn -q -B verify -Pintegration` with JDK 25 (IntelliJ JBR, no JDK 25 on PATH) → see final reply; `AgreementApiIT` 8 tests.

---

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

## 2026-11-04: S4 quick log (R70) and S5 longlist (R65)

**Done**
- S4, branch `s4-quick-log` (base a8734a0): merged `feature/network-customer-tools` (61268d0), resolved NewMenu.tsx and Shell.tsx keeping the v3 layout plus quick log entry (Alt L) and host (92eed5c). Backend already matched the `/activities` contract (outcome, direction, attendees, duration, source, externalMessageId, 409 DUPLICATE).
- S5, branch `s5-longlist` (base cf78ca8): the WIP already had ranking (`PUT /job-orders/{id}/ranking`), `PATCH /applications/{id}` (rating, rationale, outreach), the shortlist entry guard and the `/people` pool filters. Added `LonglistApiIT` (c7773ec).

**Tests**
- `ActivityQuickLogApiIT` passed. Frontend: tsc clean, vitest 41 files / 266 tests passed.
- `LonglistApiIT`: 1 test, 0 failures (bulk add of 10, reorder, stale reorder 409, outreach status, shortlist refused without rationale, pool filter). Data synthetic.

**Open items**
- The contract has no bulk-add endpoint; the test does 10 calls to `POST /job-orders/{id}/applications`. Change request for the architect if a single bulk endpoint is wanted.
- `frontend/package-lock.json` is out of sync with package.json (`@tabler/icons-react` missing, `npm ci` fails); needs a lock refresh by the frontend owner.
- S4 and S5 are separate branches; both touch the pipeline module and need code-reviewer before merge.
