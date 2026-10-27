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

# Hand-off: backend-accounts · Wave 2b (contracts 1.1.1 + 1.2.0) · 2026-10-28

**Branch / commits:** `worktree-wave2b-backend-accounts` · 6 commits (one per slice)

**Done**
- People: `GET /people` filters city (accent/İ-safe), company, tag, sourceChannel, `cv` (websearch_to_tsquery on `tr_unaccent`, relevance order, `cvMatch` excerpt), shareable; profile fields, languages, educations, normalised phone and LinkedIn; compensation encrypted through a converter calling `FieldCipher`, readable and writable only by MANAGER/ADMIN/owner (`compensationRestricted`, 403 on write); VIEW log on `GET /people/{id}`.
- Files: `/people/{id}/files` (upload with Tika type check, 10 MB, SHA-256 409, versions, download as attachment + DOWNLOAD log, delete), Tika worker (`TextExtractionWorker`, no AI), `LocalFileStore` in platform-app (`app.files.*`), `/cv-intake` (bulk, list, get, content, attach, discard, 14-day expiry job), `CvIntakeApi.findPendingByEmail` + `discardPendingByEmail`.
- Duplicates: `POST /duplicates/check`, `GET /duplicates`, dismiss; pairs queued on create/update/attach; `PersonMergeApi` (`mergeProfile`, `finish`, `mergedIds` = purge step 0) with person_merge row, JPA delete (Envers DEL), `PersonMerged`.
- Off-limits: `/off-limits` CRUD (write MANAGER/ADMIN, overlap 409), `Person.offLimits`, `OffLimitsApi` (`findForPerson`, `findForCompany`, `isBlocked`).
- Anonymise/delete: profile columns, education, files (+bytes after commit), duplicate pairs, intake items minimised.
- Tests: unit (normaliser, converter, anonymise) + ITs `PersonProfileApiIT`, `PersonFilesApiIT`, `DuplicatesAndMergeApiIT`, `OffLimitsApiIT`.

**Not done / left**
- `POST /people/{id}/merge` controller: contract owner is privacy (orchestrates accounts + pipeline + privacy steps); accounts offers `PersonMergeApi` only.
- Bulk handlers (REASSIGN_OWNER, ADD_TAG, REMOVE_TAG, CHANGE_STATUS): `BulkActionHandler` does not exist and accounts may not depend on pipeline. Needs the SPI in `contracts/spi`.
- Nightly duplicate scan and weekly orphan-bytes sweep (`FileStore` has no list method).

**Others must know**
- `FieldCipher` (security) is a minimal interface made by me (`encrypt/decrypt(byte[])`); platform-app has `FallbackFieldCipherConfig` (AES-GCM, `@ConditionalOnMissingBean`, ephemeral key unless `app.security.field-encryption-key`; fails in profile pilot). Orchestrator: keep security-privacy's version at merge.
- `Person.compensationEnc` became `Person.compensation` (converter). DB `lower('İ')` is not accent-stripped: city filter and off-limits use `translate(..., 'İı', 'Ii')`.

**Change requests**
- architect · move `BulkActionHandler` to `contracts/spi`. security-privacy · call `PersonMergeApi`, `CvIntakeApi` (DSR export/deletion). backend-pipeline · use `OffLimitsApi.findForPerson(personId, jobOrderAccountId)`.

**Verified with**
- `mvn -B verify -Pintegration` → 122 ITs, 0 failures, BUILD SUCCESS; `mvn -q -B verify` exit 0.
