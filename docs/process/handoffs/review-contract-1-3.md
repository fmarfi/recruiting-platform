# Review: contract 1.3 (head 256255e) — 2026-10-29, code-reviewer

Enums in contracts/enums, OpenAPI and schema.md CHECK lists match (hand check). Redocly not run (reviewer had no shell).

## Blockers
1. `agreement.signatory_contact_id` FK to contact, NO ACTION; contact cascades on person delete → person delete fails. Fix: on delete set null; extend PersonDeleteIT to FKs reaching person via other tables.
2. Fees visible to consultants and leak pay: getPlacement/listPlacements `feeAmount` required; getAgreement/listAgreements feePercent, flatFeeAmount, estimatedFee, schedule; listFeeScheduleItems amounts. feeAmount/feePercent = first-year cash. Fix: fee fields nullable + `feesRestricted` for non MANAGER/ADMIN (or commercial reads MANAGER/ADMIN only); same in ExportSource.
3. PUT /job-orders/{id}/team (saveSearchTeam) editable by any CONSULTANT, and membership grants placement compensation read. Fix: owner or MANAGER/ADMIN only (403); membership grants no pay access until R64 sees-compensation flag.

## Should-fix
- status-lifecycles.md not updated (shortlist guard, 409 ARCHIVED, PLACED via placeCandidate, client DECLINE → REJECTED).
- PipelineStage.candidateList() maps PLACED to SHORTLIST but rank null → rankJobOrderList "exactly the applications on list" ambiguous.
- placement.person_id cascades on delete, removing fee/guarantee records anonymise keeps → set null.
- Third-party personal data not classified for erasure: client_feedback.participants, candidate_reference.referee_*, account.billing_emails.
- Plan rule: classify every personal-data column (erasure/audit/blind redaction) + failing test — missing.
- Reading evaluations, interview notes, references (privateNotes) writes no access-log VIEW row.
- 409 ARCHIVED missing on createInterviewNote, createReference, and deletes of evaluations/notes/references/feedback/target companies.
- Shared `Gone` response says "consent link expired"; now reused for exports/reports → generalise.
- Nit: listTags own sort param; listConflictChecks and listExports lack Sort.

## Plan v3 fit
| Item | Status |
|---|---|
| R63 | Partial: parent_account_id, group_name, conflict group walk. Missing role flags, aliases[], normalised name, person_employment.organisation_id, group off-limits |
| R65 | Partial: rank/rating/rationale. OutreachStatus lacks SCREENED; no Research/Closed stage, no close_reason |
| R70 | Missing: activity unchanged |
| R64 | Partial: confidentiality_level + team roles; no sees-compensation flag or visibility rule |
| R73 | Partial: comp band not encrypted; no team size |
| R66 | Covered (scores jsonb) |
| R67 | Mostly: no signed-file ref, guarantee start basis, VAT flag per instalment |
| R79 | Covered |
| Cut (R7/R75) | proposal archive/restore, ExportSource PROPOSALS, conflict_check.proposal_id, ListEntity.PROPOSALS |
