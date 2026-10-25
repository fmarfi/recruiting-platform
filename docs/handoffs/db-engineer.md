# Hand-off: db-engineer · W1-01, W1-02 · 2026-10-16

**Branch / commits:** `worktree-wave1a-db` · see `git log --oneline main..`

**Done**
- `platform-app/src/main/resources/db/migration/V2026101600{01..06}__*.sql`: extensions + `immutable_unaccent`, security, accounts, pipeline, privacy tables, Modulith `event_publication` (+ archive). All enum CHECKs, FK/filter indexes, trigram GIN indexes, `*_aud` tables with `revend` (ValidityAuditStrategy), seeded `retention_settings` row id=1. File names follow schema.md (timestamp versions), not V1__.
- JPA entities + Spring Data repositories in `<module>/.../domain/` (22 entities; ids only across modules, UUID v7 via `@UuidGenerator`). Per-module `AuditableEntity` (created/updated/version, `@Audited`). `security.domain.AuditRevision` = Envers `revinfo`.
- `platform-app/.../PersistenceConfig` (profile `!test`): `@EnableJpaAuditing`, Envers `_aud` suffix and ValidityAuditStrategy.
- ITs in `platform-app/src/test/.../persistence/`: `MigrationIT` (validate on empty DB, CHECK values == contracts enums), one repository IT per module (trigram search, Envers revisions, constraints).

**Review fixes:** entity fields now carry the DB defaults (JPA inserts explicit NULLs). `ImportJob`: kept the blob column and replaced the finder with the bulk update `ImportJobRepository.purgeFilesCreatedBefore(cutoff)` (nulls file_content and errors). `PrivacyNotice` and `AccessLog` are `@Immutable`.

**Not done / left**
- `createdBy/updatedBy` and `revinfo.user_id` stay null until security provides an `AuditorAware<UUID>` bean and a `RevisionListener` (set it in `@RevisionEntity(...)` on `AuditRevision`).
- Phase 2 tables (pgvector) deferred as instructed.

**Others must know**
- All modules: `version` is a `Long` wrapper on purpose (new entity -> `persist`); ids are null until `save()`. Pre-assigned ids only for `PersonPrivacy(personId)` and `AccountOpenJob(jobOrderId)`.
- Search: use `PersonRepository.searchText`, `AccountRepository.searchByName`, `JobOrderRepository.searchByTitle` (escape LIKE wildcards in the input).
- Updating a `@NotAudited` counter still bumps `updated_at`, so Envers writes a revision; avoid touching `updated_at` for counters if that matters (use a JPQL/native update).
- Anonymisation purge list in schema.md misses `person_privacy_aud` (has `import_id`) and `data_subject_request_aud` (requester name/e-mail, `person_id`); both have person indexes.
- `activity.person_id ON DELETE SET NULL` conflicts with `CHECK (account_id IS NOT NULL OR person_id IS NOT NULL)` for person-only activities on DELETE retention; the DELETE path must remove those first.
- Test profile: Modulith JPA auto-configs excluded (see below); DB tests must not use it.

**Change requests** (for files you don't own)
- devops · `platform-app/pom.xml`: added `spring-modulith-starter-jpa`. `application-test.yml`: excluded `JpaEventPublicationAutoConfiguration`, `ArchivingAutoConfiguration`, `EventPublicationAutoConfiguration`. `PersistenceConfig.java` is new in platform-app. Expect a trivial merge with devops' identical lines.
- security-privacy · listener + AuditorAware, see above.

**Verified with**
- `mvn -q -B verify` -> green (2 tests).
- `mvn -q -B verify -Pintegration` -> green (19 ITs, Docker 29.8, pgvector/pgvector:0.8.7-pg17).


# Hand-off: db-engineer · Wave 2a (tags, saved_view, bulk_action) · 2026-10-25

**Branch / commits:** `worktree-wave2a-db` · 200f4e9

**Done**
- `V202610230001__platform_tags.sql`: `tags varchar(50)[]` (max 20, GIN index) on person, account, job_order, proposal and their `_aud` tables; `tags` mapped on the four entities.
- `V202610230002__accounts_saved_view.sql`: `saved_view` (PRIVATE/SHARED, jsonb definition, unique per owner+entity+lower(name)); `SavedView` + `SavedViewRepository` in accounts.
- `V202610230003__pipeline_bulk_action.sql`: `bulk_action` and `bulk_action_item` (append-only, outcome/code CHECK matching ADR 0010); entities and repositories in pipeline.
- `Wave2aRepositoryIT` (8 tests): tag limit, saved-view uniqueness, bulk item outcome/code rule, round trips.

**Not done / left**
- Backend endpoints (`/views`, `/bulk-actions`) belong to backend-accounts and backend-pipeline.

**Verified with**
- `mvn -q -B verify` green; `mvn -B verify -Pintegration` with Docker: 69 tests, 0 failures, BUILD SUCCESS (2026-10-25).

## Review fixes
- BulkActionItem implements Persistable (isNew flag, cleared on PostLoad/PostPersist); duplicate (bulk_action_id, entity_id) now fails; IT asserts it.
- setTags null-safe (null -> empty list, defensive copy) on Person/Account/JobOrder/Proposal; imports replace FQNs; V202610230001 gains ck_*_tags_lower CHECK (tags = lower(tags::text)::varchar[]), IT asserts upper-case rejected.
