# Hand-off: db-engineer · W1-01, W1-02 · 2026-10-16

**Branch / commits:** `worktree-wave1a-db` · see `git log --oneline main..`

**Done**
- `platform-app/src/main/resources/db/migration/V2026101600{01..06}__*.sql`: extensions + `immutable_unaccent`, security, accounts, pipeline, privacy tables, Modulith `event_publication` (+ archive). All enum CHECKs, FK/filter indexes, trigram GIN indexes, `*_aud` tables with `revend` (ValidityAuditStrategy), seeded `retention_settings` row id=1. File names follow schema.md (timestamp versions), not V1__.
- JPA entities + Spring Data repositories in `<module>/.../domain/` (22 entities; ids only across modules, UUID v7 via `@UuidGenerator`). Per-module `AuditableEntity` (created/updated/version, `@Audited`). `security.domain.AuditRevision` = Envers `revinfo`.
- `platform-app/.../PersistenceConfig` (profile `!test`): `@EnableJpaAuditing`, Envers `_aud` suffix and ValidityAuditStrategy.
- ITs in `platform-app/src/test/.../persistence/`: `MigrationIT` (validate on empty DB, CHECK values == contracts enums), one repository IT per module (trigram search, Envers revisions, constraints).

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
