# ADR 0005: Audit — Hibernate Envers for changes, an access log for reads, a timeline read model for users

- Status: accepted (Wave 0)
- Date: 2026-10-16

## Context

- A-02: every status change logged with date and user. A-08: an account timeline of status changes, activities, proposals and candidate moves.
- NFR-05: audit log of who viewed or exported a CV. NFR-03: deletion and anonymisation are logged. S-03/S-04: personal data must really disappear when anonymised.
- The stack names Hibernate Envers for status history and audit.

## Decision

Three mechanisms, each with one job:

1. **Envers — authoritative change history.** All business entities are `@Audited` (see "audited" in `contracts/schema.md`); cached counters and copies (`open_job_count`, `last_activity_at`, `last_contact_at`, `consent_status` on person, `password_hash`) are `@NotAudited`. A custom revision entity `revinfo(rev, revtstmp, user_id)` with a `RevisionListener` in `security` records the current user (null for system jobs). Strategy: `ValidityAuditStrategy` (faster history queries). Exposed read-only via `GET /audit/revisions?entityType=&entityId=` (manager, admin); `security` resolves the entity class from the JPA metamodel by name, so it needs no compile dependency on other modules.
2. **Access log — reads.** `access_log` rows for `VIEW` (person page), `EXPORT` (person export, data exports) and `DOWNLOAD` (CV files, Phase 2), written through `security.AccessLog`. Append-only, admin-readable via `GET /audit/access-log`. Envers cannot see reads, hence a separate table.
3. **Timeline — read model for users.** `timeline_entry` is written by `pipeline` listeners from domain events (`contracts/events.md`) with structured fields (kind, from/to status, reason, actor). It is what the account page shows; it is not the legal audit trail and may be rebuilt from events.

Retention and anonymisation runs are recorded in `retention_run`, and each anonymisation also produces an Envers revision on `person`. During anonymisation the person's earlier `_aud` rows are purged (or their personal columns nulled), otherwise the history would keep the data that KVKK requires us to erase; the purge itself is recorded in `retention_run` and the Envers revision of the anonymisation.

## Consequences

- No hand-written audit code in services; builders only add annotations.
- `_aud` tables roughly double write volume — fine at CRM scale.
- Erasure has to reach `_aud` tables; the db-engineer and security-privacy must test that an anonymised person leaves no personal data in `person_aud`, `person_employment_aud`, `contact_aud`, `network_link_aud`.
- Timeline and Envers can disagree only if a listener fails permanently; the Modulith event registry retries and incomplete publications are monitored.

## Alternatives considered

- **Only a timeline table**: misses field-level changes and edits outside status moves.
- **Database triggers / pgaudit**: no application user, harder to test, invisible to builders.
- **Spring Data `AbstractAuditable` only**: last modifier only, no history.
