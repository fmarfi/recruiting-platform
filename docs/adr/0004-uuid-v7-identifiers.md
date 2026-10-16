# ADR 0004: UUID v7 identifiers created by the application

- Status: accepted (Wave 0)
- Date: 2026-10-16

## Context

IDs appear in URLs, events, the timeline, the access log and CSV exports. Several modules create rows that reference each other through events, the seed data must be loadable on any machine, and imports create many rows at once. The demo used readable strings (`acc-1`), which are fine for a prototype only.

## Decision

- Every primary key is a `uuid` (RFC 9562 **version 7**, time-ordered), generated in the application before insert: `@Id @UuidGenerator(style = UuidGenerator.Style.VERSION_7)` (Hibernate 7). If that style is unavailable in the Hibernate version Spring Boot 4 ships, the db-engineer adds a tiny generator in `platform-app` (e.g. `com.fasterxml.uuid:java-uuid-generator` `Generators.timeBasedEpochGenerator()`).
- The API exposes ids as `string` with `format: uuid`. No numeric ids, no slugs.
- Event ids (`DomainEvent.eventId`) are UUID v7 too.
- Exceptions: Envers `revinfo.rev` stays a `bigint` sequence (Envers requirement); `retention_settings` is a single row with `id = 1`.
- Seed data uses fixed UUIDs so tests and demo links are stable.

## Consequences

- IDs are known before insert, so a service can publish an event or build a `Location` header without a round trip, and imports can assign ids in memory.
- Time-ordered UUIDs keep B-tree inserts mostly sequential (no random-UUID index bloat) and make "newest first" by id roughly correct.
- IDs leak creation time (millisecond precision). Acceptable: they are only visible to logged-in staff; public consent links use a separate random token, never an id.
- 16-byte keys instead of 8; irrelevant at this scale.

## Alternatives considered

- **`bigint` identity**: smallest and fastest, but ids are guessable, collide across environments, and need an insert before the id is known.
- **Random UUID v4**: no ordering, worse index locality.
- **ULID / TSID strings**: similar properties to UUID v7 but non-standard column types.
