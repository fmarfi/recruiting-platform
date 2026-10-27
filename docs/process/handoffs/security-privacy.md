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
