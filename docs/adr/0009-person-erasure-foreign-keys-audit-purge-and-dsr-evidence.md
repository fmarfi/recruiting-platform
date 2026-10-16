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
   - In the matching rows with `revend IS NULL` (the current revision), overwrite the personal columns with the same placeholders as the live row. This keeps `ValidityAuditStrategy` working when the live row is edited later.
   - DELETE mode: the live rows are gone, so all matching `_aud` rows are deleted. Two kinds of rows stay: `person_aud` rows with `revtype = 2` (the deletion revision, id only, because `store_data_at_delete` stays false) and the current `data_subject_request_aud` row (already minimised, see 4).
4. **DSRs are kept as minimised evidence**, for as long as the firm keeps other compliance records (no automatic purge in Phase 1). Before a person is anonymised or deleted, `privacy` minimises every DSR with that `person_id`, through JPA so that Envers records it:
   - `requester_name = 'Anonymised person'`
   - `requester_email = 'sha256:' || hex(sha256(lower(trim(email))))`, so a later complaint can still be matched to this DSR
   - `description = '[removed]'`
   - Kept: `id, type, status, received_at, due_on, resolved_at, resolution, handled_by` and the audit columns. Handlers must write `resolution` without personal data.

   Older `data_subject_request_aud` revisions are then purged by rule 3.

## Consequences

- `DELETE FROM person WHERE id = ?` always succeeds. `PersonDeleteIT` checks this and checks the FK rule against `pg_constraint`, so a new FK to `person` that breaks the rule fails the build.
- Rows removed by a database cascade get no Envers DEL revision. The purge in rule 3 removes their history. `retention_run.deleted_count` and the person's deletion revision log the deletion (NFR-03).
- security-privacy implements the order: minimise DSRs → `PersonDataApi.anonymise` or `delete` → `PersonAnonymised(deleted)` listeners scrub → `_aud` purge.

## Alternatives considered

- **Relax `ck_activity_target` and keep SET NULL.** This leaves orphan notes that hold personal data and no owner to scrub them. Rejected.
- **Purge DSRs with the person.** This removes the only proof that the request was handled. Rejected.
- **Keep DSRs unminimised.** The requester's name and free text are not needed as proof. Rejected (data minimisation).
