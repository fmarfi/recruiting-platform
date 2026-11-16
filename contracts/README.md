# Contracts

The shared interfaces every agent builds against. Owner: architect (`design-tokens.json`: ux-designer). Frozen after Gate 0; changes go through the architect as a small PR that names the affected owners.

| File | What it is | Who uses it |
| --- | --- | --- |
| `openapi.yaml` | OpenAPI 3.1 contract, version 1.5.0 (see the changelog below; operation and path counts in the changelog): auth, users, reference data, accounts, contacts, people, network links, import, search, job orders, pipeline, proposals, activities, next actions, timeline, dashboard, My day, consent, privacy notices, retention, data-subject requests, audit; since 1.1.0 saved views and bulk actions, ADR 0010; since 1.2.0 candidate files, CV intake, duplicates, exposure, off-limits, sharing guard, ADR 0011; 1.3 to 1.5: ADR 0013, 0014, 0015). Conventions are in its `info.description` and ADR 0006. `x-owner` = implementing module/agent, `x-roles` = allowed roles. | backends implement it, frontends generate types and use the mock, QA writes contract tests |
| `enums/*.java` | Status lifecycles (`AccountStatus`, `ProposalStatus`, `JobOrderStatus`, `PipelineStage`, `ConsentStatus`, `DataSubjectRequestStatus`) implementing `Lifecycle` with transition table, reason rule, manager-only rule, labels and tone; plain vocabularies (`PersonType`, `ActivityType`, `LinkType`, `Role`, …); `ErrorCode`. Must match `docs/plan/status-lifecycles.md`. | all backend modules, db-engineer (CHECK constraints), `/reference-data` |
| `events/*.java` | Domain event records (`DomainEvent` header + one record per event). | publishers and listeners across modules |
| `errors/*.java` | `ApiException` and `IllegalTransitionException` (with `check(from, to, reason)`). Mapped to RFC 9457 by `platform-app`. | all backend modules |
| `spi/*.java` | Plain-JDK service interfaces shared by several modules, implemented in `platform-app`: `FileStore` (CV and document bytes; local volume now, S3-compatible later; since 1.2.0, ADR 0011). Since 1.3.0 (ADR 0013): `AccountMergeParticipant` (implemented by pipeline, called by the accounts merge) and `ExportSource` (implemented by accounts and pipeline, called by the security export job). | accounts, pipeline, security, devops |
| `events.md` | Event catalogue: publisher, payload, listeners, delivery rules, main flows. | backend agents, security-privacy |
| `schema.md` | Tables, keys, indexes, module ownership, Envers scope, anonymisation. | db-engineer, backend agents |
| `personal-data.md` | Per-column personal-data classification: erasure, audit and blind-report redaction; source of `PersonalDataClassificationIT`. | db-engineer, security-privacy, backend agents |
| `design-tokens.json` | Colours, spacing, type, one badge style per status tone (ux-designer). | frontends |

All Java files are in package `com.recruiting.platform.contracts` (one package on purpose, ADR 0001) and need only the JDK. The `contracts` Maven module compiles every `*.java` under `contracts/`.

## Validate

```bash
npx -y @redocly/cli@1 lint contracts/openapi.yaml
```

Expected: `Woohoo! Your API description is valid.` with no errors or warnings.

Compile the Java contracts without Maven (JDK 21+; on this machine `"C:\Program Files\Java\jdk-25\bin\javac"`):

```bash
javac -Xlint:all -Werror --release 21 -d /tmp/contracts-classes contracts/enums/*.java contracts/events/*.java contracts/errors/*.java contracts/spi/*.java
```

## Run the mock server

```bash
npx -y @stoplight/prism-cli@5 mock -p 4010 contracts/openapi.yaml
```

- Serves every path **at the root**, without `/api/v1`: `http://127.0.0.1:4010/accounts`.
- Every call except `/auth/login` and `/public/consent/{token}` needs a session cookie; any value works: `curl -b "JSESSIONID=mock" http://127.0.0.1:4010/accounts`. Without it Prism answers 401 with the `UNAUTHENTICATED` problem.
- Request bodies and parameters are validated against the contract (invalid enum → 400 problem).
- Force another documented response with `Prefer: code=409` (or `404`, `403`, …): `curl -b JSESSIONID=mock -H "Prefer: code=409" -X PATCH -H "Content-Type: application/json" -d '{"status":"SOURCING"}' http://127.0.0.1:4010/job-orders/0192a5b8-6f1e-7c3a-9b2d-3f4e5a6b7c8d/status`.
- `GET /reference-data` returns the real lifecycles (generated from the enums). Other responses are static examples; add `-d` for random data instead.
- Prism reloads when `openapi.yaml` changes.
- If `npx` fails with `ECOMPROMISED` (several agents sharing the npm cache), set a private cache: `npm_config_cache=/tmp/npmcache npx …`.

Vite dev proxy for the frontends (strips the base path for the mock):

```ts
// vite.config.ts
server: {
  proxy: {
    '/api/v1': {
      target: process.env.API_TARGET ?? 'http://127.0.0.1:4010',
      changeOrigin: true,
      rewrite: (p) => (process.env.API_TARGET ? p : p.replace(/^\/api\/v1/, '')),
    },
  },
},
```

## Changing a contract

1. Open a change request in your hand-off note (field, endpoint, reason, who is affected).
2. The architect updates `openapi.yaml` / enums / events / schema in one PR, re-runs lint, compile and the mock smoke test, and lists affected owners in the PR.
3. Adding is cheap (new optional field, new endpoint); renaming or removing needs every affected owner in the same merge window.

## Changelog

| Version | Date | Change | Affected owners |
| --- | --- | --- | --- |
| 1.1.0 | 2026-10-23 | Saved views, bulk actions, `ListEntity`/`ViewVisibility`/`Bulk*` enums, tags columns (ADR 0010). | backend-accounts, backend-pipeline, security-privacy, db-engineer, frontends |
| 1.2.0 | 2026-10-25 | Wave 2b, additive (116 operations / 78 paths, was 95 / 63). Candidate profile fields on `Person` (source channel, notice period, mobility, encrypted compensation, languages, educations); CV files `/people/{id}/files` (versions, download, Tika status) and `cv` full-text filter; bulk upload `/cv-intake`; `/duplicates/check`, `/duplicates`, `/people/{id}/merge`; `/people/{id}/exposure` and `Application.warnings`; `/off-limits` and `OFF_LIMITS`; sharing guard `CONSENT_REQUIRED` and `/consent/share-check`; consent renewal (`purpose`, `openedAt`, reminders), `RetentionBasis.FROM_LAST_CONSENT`; `tag`/`sourceChannel`/`shareable` filters. 15 new enums, 3 events (`PersonFileUploaded`, `PersonMerged`, `RetentionRenewed`), `spi/FileStore.java` (ADR 0011). | db-engineer, backend-accounts, backend-pipeline, security-privacy, devops, frontend-shell, frontend-pipeline, QA |
| 1.5.0 | 2026-11-16 | Meeting 02, additive (ADR 0015): move between searches (single and batch), bulk stage move, contact on the job order, filled/remaining openings, CV push, Home personal numbers with a period. Details below. | db-engineer, backend-pipeline, backend-accounts, frontend-pipeline, frontend-shell, QA |
| 1.1.1 | 2026-10-25 | `city`, `company` filters on `GET /people` (+ PEOPLE view keys). Bulk audit holds no personal data: `BulkResultCode.message()` fixed texts, `bulk_action_item.message` dropped, `title`/`reason` not stored in `bulk_action.params` (ADR 0010 amendment 1). Tags cleared in `person_aud` by the purge (ADR 0009). | db-engineer, backend-accounts, backend-pipeline, security-privacy |

### 1.5.0 additions and owners (ADR 0015)

| ID | Addition | Owner |
| --- | --- | --- |
| C15-DB1 | `job_order.contact_id` FK contact, set null, index | db-engineer |
| C15-DB2 | `application.moved_from_application_id` (unique partial), `moved_from_stage`, `moved_at`, `moved_by`, CHECKs | db-engineer |
| C15-DB3 | `activity.cv_push` + CHECK; `candidate_exposure.activity_id` (cascade, unique), `job_order_id` null-able for CV_PUSH, CHECKs | db-engineer |
| C15-DB4 | Re-created enum CHECKs (CloseReason, ExposureChannel, TimelineKind, ListEntity columns, BulkResultCode) and `moved_from_stage` in MigrationIT; job_order rows in `PersonalDataClassificationIT` | db-engineer |
| C15-DB6 | `agreement.signed_file_size` CHECK up to 52428800 (50 MB) | db-engineer |
| C15-DB5 | Indexes: `interview_note (interviewer_id, held_on)`, `candidate_evaluation (evaluator_id, evaluated_on)`, partial `timeline_entry (actor_id, occurred_at)` for SCREENED, `candidate_exposure (presented_by, presented_at)` | db-engineer |
| C15-P1 | `POST /applications/{id}/move` + `CandidateMoved` + timeline `CANDIDATE_MOVED` (source-account row keyed by UUID v5 of `eventId:source`); `Application.movedFrom`/`movedTo`; 409 `STATE_CONFLICT` on reinstating or deleting a moved source and on deleting a moved target | backend-pipeline |
| C15-P2 | `POST /application-moves` (batch, per-record results; target checked once: 404 / 409 for the whole request) | backend-pipeline |
| C15-P3 | `CHANGE_STATUS` on `APPLICATIONS` in `/bulk-actions` (+ `closeReason`, new bulk codes). In `BulkRequestParser`: add `closeReason` to the CHANGE_STATUS keys (line 47), exclude APPLICATIONS from REASSIGN_OWNER, ADD_TAG, REMOVE_TAG (line 71, 400) and accept `PipelineStage` except PLACED in `status()` | backend-pipeline |
| C15-P4 | `contactId` on job order create/update, `JobOrder.contact`, `contactPersonId` filter | backend-pipeline (needs C15-A1) |
| C15-P5 | `filledOpenings`, `remainingOpenings`, `openings` floor only when lowered (400 `BELOW_FILLED_OPENINGS`), warnings ALL_OPENINGS_FILLED / OPENINGS_EXCEEDED on PLACED and placement | backend-pipeline |
| C15-P6 | `cvPush` on activities (guards, CV_PUSH exposure with `presented_at` = `occurredAt`, kept in step on edits; PATCH to NOTE 400 `CV_PUSH_NOT_NOTE`), `ExposureEntry.jobOrder` null, `activityId` | backend-pipeline |
| C15-P7 | `GET /home?period=`, `MyDay.myNumbers` (4 counts, ADR 0015 §8), unblocks UI-B2 | backend-pipeline |
| C15-P8 | `PUT /agreements/{id}/signed-file` limit 50 MB, 413 above (already implemented per backend) | backend-pipeline, devops (multipart limit) |
| C15-S1 | `GET /audit/revisions?includeChildren=true` for AGREEMENT (with fee schedule item revisions), `Revision.entityType`/`entityId` | security-privacy |
| C15-A2 | `ReferenceData.lifecycles.agreement` from `AgreementStatus` (now a `Lifecycle`: labels, tones, any-to-any `next`) | backend-accounts |
| C15-A1 | Accounts API method to resolve a contact (account, person, name, role, active) for pipeline; `/views` and `/exports` reject `APPLICATIONS` (400) | backend-accounts (exports: security-privacy) |
| C15-F1 | Move dialog (single, from longlist row and person page) and bulk move; bulk stage change on the longlist | frontend-pipeline |
| C15-F2 | Contact picker on job order form and header; openings and remaining in header; FILLED prompt on ALL_OPENINGS_FILLED | frontend-pipeline |
| C15-F4 | Agreements UI: badge tones from `lifecycles.agreement` (drop the local map), History with `includeChildren=true`, 50 MB hint | frontend-pipeline |
| C15-F3 | Home period selector and four number tiles (UI-B3); CV push toggle in quick log | frontend (owner of UI-B3) |

