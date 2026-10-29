# Hand-off: architect · contract 1.3 review round (B1-B3, should-fix, R63/R65/R67/R70/R73, cuts) · 2026-10-30

**Branch / commits:** `contract-1-3-fixes` (on top of `contract-1-3` 256255e; fast-forward `contract-1-3` to it) · 8 commits. Base 1.3 hand-off: `git show 256255e:docs/process/handoffs/architect.md`.

**Done**
- B1 FK set-null; B2/B3 role gating **deferred post-pilot** (ADR 0013 section; no new role rules anywhere). Should-fix: 409 `ARCHIVED`, access-log VIEW on evaluations/notes/references, generic `Gone`, shared `Sort`, PLACED unranked, lifecycle guards G1-G4 and S1-S4 in `status-lifecycles.md`.
- `contracts/personal-data.md`: classification per column + `PersonalDataClassificationIT` spec; `ThirdPartyErasure` SPI (billing e-mails, referees).
- R63 (ADR 0013 item 11), R65, R70, R73, R67; cut proposal archive/restore, PROPOSALS exports, `conflict_check.proposal_id`.
- New enums: OrganisationRole, CloseReason, ActivityOutcome/Direction/Source, GuaranteeStartBasis; OutreachStatus + SCREENED. Event `TargetCompanyAdded`.

**Others must know**
- `ListEntity.PROPOSALS` **kept**: it is a 1.2 value (saved views, bulk actions). Only exports reject it (400).
- **frontends:** `Account.sector` is now nullable (non-client organisations), so every account view and form must handle null; `GET /accounts` without `role` = CLIENT/PROSPECT.
- Placement `person`/`applicationId` are nullable (they outlive an erased person).

**Migration checklist (db-engineer)**
- `account`: `is_client/is_prospect/is_target/is_employer bool` + backfill + CHECK at least one; `aliases text[]` CHECK ≤20 + GIN on `normalize_org_names(aliases)`; functions `normalize_org_name`, `normalize_org_names`; `normalized_name` generated stored, read-only, `@NotAudited`; drop unique `lower(name)`, add unique `(normalized_name) where archived_at is null` (pre-check duplicates); `sector` nullable + CHECK.
- `person_employment.organisation_id` FK account + index. `off_limits_rule.include_subsidiaries` + CHECK.
- `application.close_reason` (backfill OTHER, then CHECK `(stage='REJECTED') = (close_reason IS NOT NULL)`, enum CHECK, index `(job_order_id, close_reason)`); re-create `outreach_status` CHECK (SCREENED).
- `activity`: `outcome`, `direction`, `duration_min`, `source` (default MANUAL), `external_message_id` (unique partial) + CHECKs; new audited `activity_attendee` (both FKs cascade).
- `search_brief`: `comp_band_enc bytea` instead of `comp_min/max/currency/period/bonus_note` (drop their CHECKs), `team_size`.
- `agreement`: `signatory_contact_id` **on delete set null** + index; `guarantee_start_basis` (CHECK); `signed_file_id/name/size/uploaded_at/uploaded_by` + CHECKs. `fee_schedule_item.vat_applies`.
- `placement`: `person_id` and `application_id` nullable, **on delete set null**, partial unique on `application_id`.
- Drop from the 1.3 plan: `proposal` archive columns + `archive_reason` CHECK, `conflict_check.proposal_id`. Index `candidate_reference (lower(referee_email))`.
- `_aud` columns (same migration): `account_aud` 4 role flags + `aliases` (not `normalized_name`); `person_employment_aud.organisation_id`; `off_limits_rule_aud.include_subsidiaries`; `application_aud.close_reason`; `activity_aud` outcome/direction/duration_min/source/external_message_id; new `activity_attendee_aud`; `search_brief_aud` `comp_band_enc`, `team_size` (drop the clear comp columns); `agreement_aud` guarantee_start_basis + 5 signed_file columns; `fee_schedule_item_aud.vat_applies`; `placement_aud` person_id/application_id nullable.
- MigrationIT `ENUM_COLUMNS`/`_aud` list per schema.md line 19; `PersonDeleteIT` indirect-FK walk; new `PersonalDataClassificationIT`, `OrganisationNameNormalisationIT`.

**Change requests**
- backend-accounts · roles/aliases/match (`OrganisationApi.match`), CLIENT on `JobOrderCreated` + 409 on removing CLIENT with open job orders, `ActivityLogged.attendeePersonIds` → `last_contact_at`, `eraseTaxId`, employment link, off-limits subsidiary match, `TargetCompanyAdded` listener, merge unions, `ThirdPartyErasure` (billing e-mails).
- backend-pipeline · publish `ActivityLogged` with `attendeePersonIds` (new canonical constructor; the 1.2 one stays), close reason, outreach SCREENED, activity fields + attendees, encrypted band, signed-file endpoints, guarantee basis, VAT flag, placement erasure nulling, `ThirdPartyErasure` (referees), VIEW rows, 409 `ARCHIVED`.
- security-privacy · call `ThirdPartyErasure` beans in DSRs (e-mail, TCKN); retention clock for `ActivityLogged.attendeePersonIds`; purge additions (`activity_attendee_aud`, `placement_aud` keep-current rule).
- frontends · `npm run gen:api`; nullable `sector`, placement `person`.

**Verified with**
- `npx @redocly/cli@1 lint contracts/openapi.yaml` → valid
- `mvn -q -B -pl contracts -am verify` → green
- Scripted check: every `contracts/enums` value list equals its OpenAPI enum → no mismatches
