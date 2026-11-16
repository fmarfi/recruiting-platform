# Hand-off: architect · M2-C15 contract 1.5.0 (meeting 02) · 2026-11-16

**Branch / commits:** `contract-1-5` from `feature/network-customer-tools` at ede80cd · 1 commit. Previous hand-off: `git show ede80cd:docs/process/handoffs/architect.md`.

**Done**
- `contracts/openapi.yaml` 1.5.0 (additive): `POST /applications/{id}/move`, `POST /application-moves`, `CHANGE_STATUS` on `APPLICATIONS`, job order `contactId`/`contact`/`contactPersonId`, `filledOpenings`/`remainingOpenings`, activity `cvPush`, `GET /home?period=` + `MyDay.myNumbers`. Fixed 2 pre-existing lint errors (unquoted `MySearchHealth` descriptions).
- Enums: new `HomePeriod`; values added to `CloseReason` (MOVED_TO_OTHER_SEARCH), `ListEntity` (APPLICATIONS), `BulkResultCode` (DUPLICATE, OFF_LIMITS, CONSENT_REQUIRED, ARCHIVED), `PipelineWarningCode` (ALL_OPENINGS_FILLED, OPENINGS_EXCEEDED), `ExposureChannel` (CV_PUSH), `TimelineKind` (CANDIDATE_MOVED). Event `CandidateMoved`.
- `schema.md` (1.5.0 migration note, columns, Home number queries and indexes), `events.md`, `personal-data.md` (job_order now in scope via `contact_id`), `README.md` changelog with IDs C15-DB1..5, C15-P1..7, C15-A1, C15-F1..3 and owners.
- ADR 0015: move semantics, contact, openings, number definitions, assumptions.

**Not done / left**
- No `BulkActionType` value for move: adding one breaks the exhaustive switches in `BulkRequestParser` (1.4 code). Batch move is its own path, no `bulk_action` audit row in 1.5.
- No drill-down lists behind the Home tiles; no custom period.

**Others must know**
- backend-pipeline: a move publishes only `CandidateMoved` (not StageChanged/Created). Bulk stage move has no overrides. `MOVED_TO_OTHER_SEARCH` is rejected on `PATCH .../stage`.
- db-engineer: re-create CHECKs listed in schema.md "Contract 1.5.0"; `job_order` column list in personal-data.md must match the live table.
- frontend: `ExposureEntry.jobOrder` can now be null (CV_PUSH). Unknown enum values must render generically.

**Change requests** (for files you don't own)
- backend-pipeline · `BulkRequestParser` · add `default ->` to the three `switch (action)` expressions so new `BulkActionType` values do not break the build (then the batch move can fold into `/bulk-actions`).
- backend-accounts · accounts API · contact lookup for pipeline (C15-A1).

**Open questions for the client:** Q3 does a moved candidate stay on the old search, and is "copy" wanted; Q4 meaning of CV sent vs CV pushed; Q6 when an opening is filled and whether openings change later; Q8 "last week" = calendar week or last 7 days; should ending a contact used by a search be blocked.

**Verified with**
- `npx -y @redocly/cli@1 lint contracts/openapi.yaml` → valid, no warnings
- Prism 5 mock: `/home?period=LAST_WEEK` 200, bad period 422, move 201, batch move 200, bulk APPLICATIONS 200
- `mvn -q -B -o -Djava.version=21 verify` → green (only JDK 21 on this machine, `jdk-25` path missing; `javac -Xlint:all -Werror` on contracts green). DB ITs such as MigrationIT will fail on the new enum values until C15-DB4 lands; merge with db-engineer.
