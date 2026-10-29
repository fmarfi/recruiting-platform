# Hand-off: architect · contract 1.3 (D1-D8, D11 prerequisites) · 2026-10-29

**Branch / commits:** `contract-1-3` · 8 commits, one per area (archive/merge/profile, tags, lists/evaluation, brief, commercial, pool filters, import/export/reports, ADR and purge)

**Done**
- `contracts/openapi.yaml` 1.3.0 (additive): archive/restore for accounts, job orders, proposals, people (`archived` filter, 409 `ARCHIVED`); `POST /accounts/{id}/merge`; account profile fields; `/tags` rename/merge/delete/colour/talent pool; application `list` (derived), `rank`, `rating`, `rationale`, `outreachStatus`, `PUT /job-orders/{id}/ranking`, client feedback; evaluations, interview notes, references; brief, team, target companies; agreements + fee schedule items, placements, conflict checks; pool filters (skill, functionArea, seniority, language, noticeMaxDays, mobility, salary*, hasCv); import mapping + rollback; `/exports` with approval; client report PDFs.
- `contracts/enums`: 37 new enums, `PipelineStage.candidateList()`, new values in ErrorCode, EntityType, TimelineKind, PipelineWarningCode, ImportStatus.
- `contracts/events`: `ArchiveStateChanged`, `AccountMerged`, `TagChanged`, `PlacementCreated`; `contracts/spi`: `AccountMergeParticipant`, `ExportSource`.
- `contracts/schema.md`: all tables/columns/FKs, enum CHECK list for MigrationIT, purge additions; `events.md`; ADR 0013 (decisions, deferred items, build order); ADR 0009 amendment for 1.3.0.

**Not done / deferred (ADR 0013)**
- CONFIDENTIAL/RESTRICTED visibility rule, ACTIVE activation gate, tax-id validation, new roles, proposal versions/opportunities (D11 beyond conflict check), invoice issuing.
- `BulkActionType.ARCHIVE` and `ImportKind.JOB_ORDERS/ACTIVITIES`: adding them breaks existing exhaustive switches, so they are pre-approved CRs (below), not in the enums yet.

**Others must know**
- Behaviour change: longlist → client-facing stage now needs `rating` and `rationale` ≥ 20 chars (409 `TRANSITION_GUARD_FAILED` + `errors`); 1.2 seed/ITs moving to PRESENTED must send them.
- Salary filter is in-memory after decryption (≤ 5,000 rows, own/readable rows only); never add a plaintext salary column.
- Tag rewrites are set-based with `version + 1` and no per-record Envers revision.
- Merged accounts are archived (`MERGED`, `merged_into_id`), never deleted.

**Change requests**
- db-engineer · Flyway (accounts, pipeline, security) · every 1.3.0 table/column in `schema.md`, CHECKs and re-created CHECKs listed under "Enum CHECKs added in 1.3.0", rank backfill, `languages` jsonb_path_ops GIN; MigrationIT `ENUM_COLUMNS` + array CHECKs + `_aud` list; `PersonDeleteIT` new person FKs.
- backend-accounts · `crm-accounts` · archive/restore (account, person), merge orchestration + `account_merge`, profile fields + `missingForActive`, `/tags` + `TagChanged`, pool filters, import mapping/rollback + `import_job_record`, `ExportSource` PEOPLE/ACCOUNTS, `PlacementCreated` → GUARANTEE_PERIOD rule, `ArchiveStateChanged` → `account_open_job`. Add `ImportKind.JOB_ORDERS/ACTIVITIES` with their analyzer branches (pre-approved).
- backend-pipeline · `crm-pipeline` · archive (job order, proposal), `AccountMergeParticipant`, `TagChanged` listener, list/rank/rating/rationale + shortlist guard + ranking, client feedback (DECLINE → REJECTED), evaluations/notes/references, brief/criteria/team/targets (OFF_LIMITS on APPROACHED), agreements, fee triggers + nightly jobs, placements, conflict checks, client reports + REPORT exposure, `ExportSource` JOB_ORDERS/PROPOSALS, timeline ARCHIVE/MERGE/PLACEMENT; update ITs and seed for the shortlist guard. Add `BulkActionType.ARCHIVE` with its handler (pre-approved).
- security-privacy · `security`, `privacy` · export jobs (threshold 25, approval, watermark, 24 h expiry, EXPORT/DOWNLOAD access log), placement and report access-log rows, anonymisation listener and `_aud` purge additions (schema.md step 3/4), person merge moves the 1.3 person rows and `client_report.person_ids`, share guard on report generate and sent.
- frontend-shell · archive menu + Archived views, merge dialog, Admin › Tags, export flow, profile sections.
- frontend-pipeline · Longlist/Shortlist tabs (Alt+↑/↓ → ranking), feedback, Evaluations tab, Brief and Research tabs, Commercial terms, placements, reports.
- orchestrator · `docs/plan/TASKS.md` · D1-D8 now unblocked in the ADR 0013 build order.

**Verified with**
- `npx @redocly/cli@1 lint contracts/openapi.yaml` → valid; strict YAML 1.1 parse (PyYAML, as swagger-parser) → ok
- `mvn -q -B verify` → green
- `frontend: npm ci && npm run gen:api && npx tsc --noEmit` → exit 0 (generated `schema.d.ts` not committed)
