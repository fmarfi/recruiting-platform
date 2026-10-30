# ADR 0009: Person erasure — cascade foreign keys, audit purge, and data-subject requests kept as minimised evidence

- Status: accepted (Wave 1b, W1-05)
- Date: 2026-10-17

## Context

- S-03 lets retention end in ANONYMISE (default) or DELETE; S-04 logs access, correction and deletion requests. NFR-03: deletion and anonymisation are logged.
- `activity.person_id` was `ON DELETE SET NULL`, but `ck_activity_target` requires an account or a person. Deleting a person with a person-only activity failed, so the DELETE action could not work. `next_action.person_id` was also SET NULL, which left titles that may name the person.
- Envers `_aud` tables have no foreign keys. Database cascades never reach them, and they keep every old value. ADR 0005 already requires purging them, but did not say how this works with `ValidityAuditStrategy`. That strategy updates `revend` on the previous `_aud` row and fails if the row is gone.
- `data_subject_request_aud` was missing from the purge list. A data-subject request (DSR) is proof that the firm answered a KVKK request on time (Art. 11 and 13, 30 days). The firm may need that proof against a complaint to the Board, so it is a legitimate reason to keep data (Art. 5(2)(e)).

## Decision

1. **Foreign keys to `person`: always `ON DELETE CASCADE`.** This covers rows that exist only because of the person: `person_employment`, `contact`, `network_link` (both ends), `application`, `activity`, `next_action`, `person_privacy`, `consent_request` and `consent_record`. **The one exception is `data_subject_request.person_id`, which is `ON DELETE SET NULL`** and has no CHECK that needs it. Migration `V202610170001` switches `activity` and `next_action`. An activity linked to both an account and the person is also deleted. Only DELETE mode loses it; the default ANONYMISE mode keeps it with the note scrubbed.
2. **Foreign keys to `account` stay `NO ACTION`.** Accounts are closed by status and never hard-deleted, so the SET NULL vs CHECK conflict cannot occur there. Any future account delete needs its own ADR.
3. **Audit purge rule.** Each table on the purge list in `contracts/schema.md` has a match condition on person id. The purge runs after the live rows have been scrubbed or deleted, and it is idempotent:
   - Delete the matching `_aud` rows with `revend IS NOT NULL` (old revisions).
   - In the matching rows with `revend IS NULL` (the current revision), overwrite the personal columns with the same placeholders as the live row. This keeps `ValidityAuditStrategy` working when the live row is edited later. For `person` this includes `tags = '{}'` (added in contract 1.1.0, ADR 0010): `PersonDataService.anonymise` clears the live column and `AuditPurger` clears it in the current `person_aud` row.
   - DELETE mode: `accounts` must remove the person **through JPA** (`PersonRepository.delete`/`EntityManager.remove`, never a native `DELETE`). Envers then writes the deletion revision: a `person_aud` row with `revtype = 2` that holds only the id, because `org.hibernate.envers.store_data_at_delete` stays false. The database cascades the other tables. The purge deletes every matching `_aud` row except this `revtype = 2` row (the NFR-03 per-person deletion record) and the current `data_subject_request_aud` row. In that DSR row the purge also sets `person_id` to null, because the database's SET NULL is not audited.
   - `import_job.file_content` and `errors` hold raw CSV rows. They are set to null as soon as an import ends (COMPLETED, FAILED or DISCARDED), and the retention purge also nulls any that remain. Uploaded files never outlive the import.
4. **DSRs are kept as minimised evidence**, for as long as the firm keeps other compliance records (no automatic purge in Phase 1). Before a person is anonymised or deleted, `privacy` minimises every DSR with that `person_id`, through JPA so that Envers records it:
   - `requester_name = 'Anonymised person'`
   - `requester_email = 'hmac:v<k>:' || hex(HMAC-SHA256(key_k, lower(trim(email))))`, so a later complaint can be matched to this DSR by recomputing the value from the address the complainant gives. The key comes from the env var `PRIVACY_HMAC_KEY` (at least 32 random bytes, kept outside the database and its backups). `<k>` is the key version (`PRIVACY_HMAC_KEY_VERSION`, starting at 1). Rotation: add a new version and keep old keys read-only (`PRIVACY_HMAC_KEY_V<k>`) to check old values. Existing values are never re-hashed, because the plain address is gone. A plain sha256 would be rejected: a dictionary of e-mail addresses can reverse it.
   - `description = '[removed]'`
   - Kept: `id, type, status, received_at, due_on, resolved_at, resolution, handled_by` and the audit columns. Handlers must write `resolution` without personal data.

   Older `data_subject_request_aud` revisions are then purged by rule 3.

## Consequences

- Deleting a person always succeeds. `PersonDeleteIT` checks this through JPA, including the `revtype = 2` row, and checks the FK rule against `pg_constraint`, so a new FK to `person` that breaks the rule fails the build.
- Rows removed by a database cascade get no Envers DEL revision. The purge in rule 3 removes their history. `retention_run.deleted_count` and the person's `revtype = 2` revision log the deletion (NFR-03).
- Ops must provision and back up `PRIVACY_HMAC_KEY` separately from the database. If the key is lost, DSR e-mails can no longer be matched. The records themselves stay valid.
- security-privacy implements the order (authoritative version: `contracts/schema.md`, "Anonymisation and deletion"): **step 0 (since contract 1.2.0, ADR 0011): collect the person-id set (the person plus every `person_merge.merged_id`) and the file storage keys, before anything is deleted, because the DELETE cascade removes those rows** → minimise DSRs → `PersonDataApi.anonymise` or `delete` → `PersonAnonymised(deleted)` listeners scrub → `_aud` purge over the collected id set → delete file bytes after commit.

## Amendment for contract 1.3.0 (ADR 0013, 2026-10-29)

- **Rule 1 extended.** New person FKs cascade: `client_feedback`, `candidate_evaluation`, `interview_note`, `candidate_reference`, `placement`. Rows that point at an `application` but are not person rows set null on delete (`candidate_evaluation`, `interview_note`); `client_feedback` and `placement` cascade with their application. `PersonDeleteIT` gets the new tables.
- **Rule 2 still holds.** Merged accounts are archived (`archive_reason = MERGED`), never deleted, so FKs to `account` stay `NO ACTION`.
- **Archive is not erasure.** Archiving a person only hides them from lists; consent, retention and DSR handling run as before. `archive_note` is personal data and is nulled on anonymisation.
- **Anonymise (live rows).** `person`: `skills = '{}'`, `function_area`, `seniority`, `archive_note` null. `pipeline` listener: application `rationale` → `[removed]`, `rating` null; evaluations, interview notes and references of the person are deleted through JPA (they assess the person or hold a referee's data and have no use once the person is anonymous); client feedback keeps round, date, outcome and decline code and scrubs its free text; placements keep dates, status and fee as commercial evidence (tax retention VWC) and null the encrypted pay and status reason; every client report PDF that includes the person is deleted (row and bytes), in ANONYMISE and DELETE mode.
- **Rule 3 extended.** `_aud` purge by `person_id IN :p` also on `client_feedback_aud`, `candidate_evaluation_aud`, `interview_note_aud`, `candidate_reference_aud`, `placement_aud`, with the placeholders listed in `schema.md`. Person-free tables (brief, criteria, team, target companies, agreements, fee items, tag definitions) need no purge; `conflict_check` and `export_job` are built so that they never hold person names or free-text filters.
- **Person merge** moves the new person rows to the survivor and rewrites `client_report.person_ids`, so the erasure of the survivor reaches them.

## Alternatives considered

- **Relax `ck_activity_target` and keep SET NULL.** This leaves orphan notes that hold personal data and no owner to scrub them. Rejected.
- **Purge DSRs with the person.** This removes the only proof that the request was handled. Rejected.
- **Keep DSRs unminimised.** The requester's name and free text are not needed as proof. Rejected (data minimisation).
