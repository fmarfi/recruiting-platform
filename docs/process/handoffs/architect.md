# Hand-off: architect · M2-C15 contract 1.5.0 (meeting 02) + review fixes · 2026-11-16

**Branch / commits:** `contract-1-5` from `feature/network-customer-tools` at ede80cd · 2 commits (85b0e48 contract, then review fixes). Frozen after this. Previous hand-off: `git show ede80cd:docs/process/handoffs/architect.md`.

**Done**
- `openapi.yaml` 1.5.0: move single/batch, bulk stage move (`CHANGE_STATUS` on `APPLICATIONS`), job order contact, filled/remaining openings, `cvPush`, `GET /home?period=` + `myNumbers`; agreements: 50 MB signed PDF, `/audit/revisions?includeChildren`, `lifecycles.agreement`.
- Review blockers: source-account timeline row keyed by UUID v5 of `eventId:source`; moved source cannot be reinstated or deleted, moved target cannot be deleted (409 `STATE_CONFLICT`, `status-lifecycles.md` rule 4 updated).
- Should-fix: moves into SCREENED not counted as screened (decided); PATCH cv-push to NOTE 400, `presented_at` = `occurredAt`; move CHECK relaxed; openings floor only when lowered (`BELOW_FILLED_OPENINGS`); funnel excludes MOVED_TO_OTHER_SEARCH; batch target checked once (404/409); `/home` 400; archived source search allowed.
- Q4 (CV pushed vs sent) confirmed by the user 2026-11-16, out of ASSUMPTIONS.
- Enums: `HomePeriod`, `AgreementStatus` now a `Lifecycle` (labels, tones, any-to-any `next`), new values in 7 enums; event `CandidateMoved`; schema.md, events.md, personal-data.md, README changelog (C15-DB1..6, P1..8, S1, A1..2, F1..4), ADR 0015.

**Not done / left**
- No `BulkActionType` for move (exhaustive switches in `BulkRequestParser`); batch move has no `bulk_action` row.
- No Home drill-down lists, no custom period.

**Others must know**
- backend-pipeline: C15-P3 parser changes (closeReason key, APPLICATIONS excluded from owner/tag actions). A move publishes only `CandidateMoved`.
- db-engineer: re-create CHECKs (schema.md 1.5.0 note, plus `signed_file_size` 50 MB); job_order rows in personal-data.md must match the live table.
- frontend: `ExposureEntry.jobOrder` may be null; use `lifecycles.agreement` for badge tones.

**Change requests** (for files you don't own)
- backend-pipeline · `BulkRequestParser` · `default ->` in the `switch (action)` expressions so future `BulkActionType` values do not break the build.
- product-analyst · `docs/plan/status-lifecycles.md` · add an agreement status section matching `AgreementStatus` (plain field, labels and tones).

**Open questions for the client:** Q3 does a moved candidate stay on the old search, and is copy wanted; Q6 when an opening is filled and whether openings change; Q8 "last week" = calendar week or last 7 days; block ending a contact used by a search?

**Verified with**
- `npx -y @redocly/cli@1 lint contracts/openapi.yaml` → valid, no warnings
- Prism 5: `/home?period=LAST_WEEK` 200, bad period 400, move 201, batch 200, revisions includeChildren 200, reference-data 200
- `javac -Xlint:all -Werror --release 25` on contracts → ok (JDK 25 = IntelliJ JBR 25.0.4, `C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\jbr`)
- `mvn -q -B -o verify` with that JDK → green
