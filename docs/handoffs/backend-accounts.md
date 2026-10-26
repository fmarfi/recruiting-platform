# Hand-off: backend-accounts · Wave 2a (saved views, tags erasure) · 2026-10-25

**Branch / commits:** `worktree-wave2a-backend-accounts` · see `git log` (1 feature commit)

**Done**
- `/api/v1/views` GET (entity filter, own + shared, ordered by name), POST, PATCH `/{id}`, DELETE `/{id}` per contract 1.1.0, in `accounts/view/{app,web}`.
- Permissions: other user's PRIVATE view = 404; SHARED create or switch-to-SHARED needs MANAGER/ADMIN (403 FORBIDDEN); changing or deleting a SHARED view = owner or MANAGER/ADMIN, else 403. `editable` computed per caller.
- Name unique per owner+entity, case-insensitive (409 DUPLICATE, also on rename); `version` check gives 409 VERSION_CONFLICT; empty PATCH = 400.
- `ViewDefinitionValidator`: filter keys and values per entity (enums, UUID, `me`, dates, booleans), columns/sort/groupBy against list item property names; 400 with `field = definition.filters.<key>`.
- Anonymisation clears `tags` in `PersonDataService` (ADR 0010), with `PersonDataServiceTest`.
- No audit or timeline for views (user setting). Session user via `CurrentUser`/`UserDirectory` (ids only).
- Tests: `ViewDefinitionValidatorTest`, `SavedViewServiceTest`, `PersonDataServiceTest`, `SavedViewApiIT` (6 ITs: permissions, uniqueness, shared visibility, versions, validation, 401).

**Not done / left**
- Cap check is count-then-insert (race accepted for now).
- Tags are not yet in read models or list filters (follow-up per ADR 0010).

**Others must know**
- The cap of 50 PRIVATE views per user and entity (contract ASSUMPTION) returns 400 VALIDATION_FAILED, `field = entity`, code LIMIT; the contract names no error code for it.
- `SavedViewRepository` gained `countByOwnerIdAndEntityAndVisibility`.
- Review fixes: SHARED->PRIVATE only by owner (else 403) and checks the 50 cap; sort keys per entity follow the list endpoints; one query when `entity` absent; only uq_saved_view_owner_entity_name maps to DUPLICATE. A PATCH with only `version` is 400.

**Change requests** (for files you don't own)
- (resolved) KVKK `q` ruling: ADR 0010 amendment 2 implemented, SHARED views drop filters.q; version-only or empty PATCH is 400; 50-view cap 400 LIMIT.
- security-privacy · privacy `AuditPurger` · scrub `tags` in `person_aud` (and `account_aud` if relevant) when a person is anonymised or erased; tags in old revisions otherwise survive (ADR 0010, ADR 0009).

**Verified with**
- `mvn -q -B verify` -> green.
- `mvn -B verify -Pintegration` -> Tests run: 76, Failures: 0, BUILD SUCCESS.
