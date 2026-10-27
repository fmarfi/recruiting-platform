# Hand-off: security-privacy · W1-12 · 2026-10-19

**Branch / commits:** `worktree-wave1b-security-privacy` · 5 commits (public API, security, privacy, retention/DSR, hand-off)

**Done**
- security: SecurityFilterChain (session 8h, CSRF cookie `XSRF-TOKEN` + `X-XSRF-TOKEN`, 3 roles via RoleHierarchy, method security, `/actuator/health` open without details). Auth (login/logout/me/password), Users, access log, revisions (Envers, entity found by name). Login writes an access-log row (VIEW, USER). Lock after 10 failures for 15 min; IP rate limit on login and public consent (429 `TOO_MANY_REQUESTS`, not in ErrorCode yet).
- Public API for other modules: `CurrentUser`, `UserDirectory`, `AccessRecorder`, `ApiPage`, `ApiPageable`, `RequestRateLimiter`.
- privacy: notices (TR/EN drafts seeded as version 2026-10, need lawyer review), consent (overview, send e-mail, manual records, bulk, list), public link (`/consent/<token>` in the SPA, token hashed, masked in logs by `logback-spring.xml`), retention (settings, flagged, runs, nightly job `app.privacy.retention-cron`, dry run), data-subject requests, `POST /people/{id}/anonymise`. Erasure follows ADR 0009 (DSR minimised with HMAC, event, consent scrub, `_aud` purge).
- `ApiExceptionHandler` (platform-app) because none existed; `PlatformApplicationTests` became `PlatformApplicationIT` (the DB-free test profile cannot start repositories any more; `application-test.yml` removed).

**Not done / left**
- `GET /people/{id}/export` (PersonDataExport): needs accounts and pipeline read APIs (`PersonDataApi`), not merged.
- `JdbcPersonGateway` is an interim adapter (reads/anonymises/deletes `person` with SQL, writes the Envers delete row, publishes `PersonAnonymised`). Replace with an adapter on accounts `PersonApi`/`PersonDataApi` after merge; then accounts must publish the event, not privacy.
- `mustChangePassword` is returned but not enforced server-side.

**Others must know**
- backend-accounts: publish `PersonCreated` (consent record + auto e-mail depend on it) and listen to `ConsentStatusChanged` / `RetentionFlagged`. People without a privacy row get one on first consent call and in the nightly run.
- synthetic-data: demo users `*@demo.example` with `{bcrypt}` hashes, `must_change_password=false`. Rows inserted by SQL into audited tables need a first `_aud` row, or the first JPA update fails (`person_privacy`: let the run create them, insert only `person`).
- devops: set `PRIVACY_HMAC_KEY` (+ `PRIVACY_HMAC_KEY_VERSION`) and `SESSION_COOKIE_SECURE`; erasure is refused without the key. Added to `application.yml`: session cookie, `management.health.mail` off, `show-details: never`. `app.privacy.controller-name` names the firm.
- frontend: login = `POST /auth/login` (no CSRF), then send `X-XSRF-TOKEN` from the cookie; public page route `/consent/:token`.

**Change requests** (for files you don't own)
- architect · `ErrorCode` · add `TOO_MANY_REQUESTS` (429) and list it in openapi for login and public consent.
- devops · `ApiExceptionHandler` · adopt or replace the minimal handler in platform-app; add the env vars above to `.env.example`.

**Verified with**
- `mvn -B verify -Pintegration` → exit 0, 44 tests (SecurityApiIT 8, ConsentApiIT 7, RetentionAndRequestsApiIT 4, ModularityTests, persistence ITs).

## Update 2026-10-19 (accounts/pipeline/seed merged)
- `JdbcPersonGateway` replaced by `AccountsPersonGateway` (PersonApi + PersonDataApi; accounts now publishes `PersonAnonymised`). Only `idsWithoutPrivacy` stays SQL (ids only); `PersonSummary` has no `createdAt`, so the retention basis falls back to today for people without a privacy row. privacy now depends on `crm-accounts`.
- `ErrorCode.TOO_MANY_REQUESTS(429)` added; `ApiExceptionHandler` uses it. Still open for architect: list it in `contracts/openapi.yaml` ErrorCode enum and on login/public consent.
- `ConsentExportContributor` adds `consent` to `GET /people/{id}/export` (IT added). backend-pipeline: add `PersonExportContributor` for `activities` and `applications`.
- `application-local.yml`: DB default port 5433, imports `.env` (`../.env`, `./.env`; not in a worktree, so export `DB_PASSWORD`/`DB_URL`).
- Local run verified (own DB `security_privacy`, port 8083, `mvn spring-boot:run -Dspring-boot.run.profiles=local`, Flyway ok, seed loaded with `COMPOSE_PROJECT_NAME=recruiting-platform seed/load.sh security_privacy`): login admin@demo.example 200, `/auth/me` 200, `/accounts` 200, `/people` 200, export 200 with consent, `/retention/flagged` 200, `/users` 200, POST without CSRF 403. Server stopped.
- BUG for synthetic-data: seeded rows have no `_aud` baseline, so PATCH /people/{id} (and anonymise/delete of seeded people) returns 500 "Cannot update previous revision". Seed must insert a revtype 0 `_aud` row plus `revinfo` per audited row (person, account, contact, job_order, application, proposal, app_user, network_link, person_employment).
- `mvn -q -B -Pintegration verify` green (60 tests).

# Hand-off: security-privacy - Wave 2b - 2026-10-27

**Branch:** `worktree-wave2b-security-privacy` (based on 3a61e53).

**Done (by slice)**
- 1 `security.FieldCipher` + `crypto.AesGcmFieldCipher` (AES-256-GCM, 1 byte version + 12 byte IV + ct/tag; `FIELD_ENCRYPTION_KEY_V<k>` base64 32 bytes, `FIELD_ENCRYPTION_KEY_VERSION`). pilot/prod profile fails fast without key; any other profile (local, test, ITs) logs a warning and uses a public dev key. `FieldCipherTest`.
- 2 Consent engine: `ShareGuardImpl` (privacy) + `POST /consent/share-check`; overview has shareable/shareBlockReason/lastConsentAt/nextReminderOn; `PersonFileUploaded` (CV) auto-sends the notice + consent mail (setting on; never REFUSED/WITHDRAWN; none while a valid unanswered request exists, row-locked); RENEWAL requests (only while GIVEN); reminders 60/14 (`RenewalReminderService`, idempotent via `reminder_for_retain_until`); `openedAt` set on first GET, never a consent signal; GIVEN sets `last_consent_at`, restarts the clock (FROM_LAST_CONSENT) and publishes `RetentionRenewed`; `RetentionBasisSync` (one-off, idempotent, runs at start and in every retention run) re-saves stale `person_privacy` rows through JPA and publishes `RetentionRenewed` for rows the migration unflagged. `ConsentEngineApiIT`.
- 3 Purge: step 0 (`ErasureScopes`: merged ids + file keys collected first), `AuditPurger` covers person_education_aud, person_file_aud, new person_aud columns, tags, application_aud.off_limits_override_reason and merged ids; file bytes deleted via FileStore after the commit; attached intake items minimised (file_name, sha256, storage_key, extracted_text, detected), pending items matched by lower-cased detected e-mail are discarded on erasure and DELETION requests; export adds `files`, `exposure`, `pendingIntake`. Merge: `POST /people/{id}/merge` (MANAGER) orchestrates accounts, pipeline, privacy in one transaction, publishes `PersonMerged`. `PurgeWave2bIT`, `MergeApiIT`.
- 4 `RequestConsentBulkHandler` (BulkActionHandler SPI), MANAGER/ADMIN only, codes per ADR 0010. `BulkConsentHandlerIT`.

**Not done / notes**
- Interfaces missing on this branch, so I created minimal copies (theirs win at merge; adapt call sites): `pipeline.ShareGuard`, `pipeline.PipelinePersonApi`, `pipeline.BulkActionHandler`, `accounts.PersonMergeApi`. Merge returns 501-like 409 when no implementation exists.
- Live-table minimisation on ANONYMISE (person_file/person_education deletion, step 2) stays in backend-accounts; my IT checks only aud, intake and bytes there. Token reuse: a link stays usable until expiry so people can withdraw (decision kept from Wave 1).
- Basis sync writes one extra person_privacy revision (shift then restore) because Envers needs a dirty field.
- Privacy reads person_merge, person_file, cv_intake_item, candidate_exposure by SQL (ids/keys/metadata only) until accounts/pipeline offer APIs (`CvIntakeApi.findPendingByEmail`).

**Change requests**
- backend-pipeline: implement ShareGuard consumers, `PipelinePersonApi.movePerson`, dispatcher calling `BulkActionHandler.handle(actor, role, params, id)`; register no REQUEST_CONSENT handler yourself.
- backend-accounts: implement `PersonMergeApi` (mergeProfile with version check, completeMerge, survivorView) and step 2 deletions; devops: provide a FileStore bean and env `FIELD_ENCRYPTION_KEY_V1`/`_VERSION` in pilot.

**Verification:** `mvn -B verify -Pintegration` (see final reply).
