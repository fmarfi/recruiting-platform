# ADR 0001: Modular monolith with Spring Modulith; module layout and boundaries

- Status: accepted (Wave 0)
- Date: 2026-10-16
- Deciders: architect, devops (layout agreed), orchestrator

## Context

Phase 1 is a CRM (accounts, network, job orders, pipeline) with KVKK consent and audit; Phase 2 adds an AI tool on the same data. Eight builder agents work in parallel in Wave 1, each owning one area. We need one deployable (small team, one database the firm owns, NFR-04) but hard boundaries so agents do not edit or call each other's internals.

## Decision

One Spring Boot 4 application, split into Maven modules that each hold exactly one Spring Modulith application module. Module boundaries are verified by `ApplicationModules.of(PlatformApplication.class).verify()` in `platform-app` (the "Modulith verification test"), which runs in CI.

### Layout (fixed with devops)

groupId `com.recruiting`, base package `com.recruiting.platform`.

| Maven module | Java package (Modulith module) | Owner (Wave 1) | Contents |
| --- | --- | --- | --- |
| `contracts` | `com.recruiting.platform.contracts` | architect | Shared kernel: enums, `Lifecycle`, event records, `ApiException`. Sources: every `*.java` under `contracts/` (enums/, events/, errors/). No Spring dependency. |
| `security` | `…platform.security` | security-privacy | Users, login, roles, access log, Envers revision listener, Spring Security config |
| `crm-accounts` | `…platform.accounts` | backend-accounts | Accounts (incl. account status), contacts, people, network links, CSV import, search, reference data |
| `crm-pipeline` | `…platform.pipeline` | backend-pipeline | Job orders, applications (pipeline), proposals, activities, next actions, timeline, account dashboard, My day |
| `privacy` | `…platform.privacy` | security-privacy | Consent records and requests, privacy notices, consent e-mails, retention job, data-subject requests, person export |
| `ai` | `…platform.ai` | ai-engineer (Phase 2) | Empty until Phase 2 |
| `platform-app` | `com.recruiting.platform` (root, no module) | devops | `PlatformApplication`, global `@RestControllerAdvice` turning `ApiException` into RFC 9457, Flyway migrations, `application.yml`, Modulith verification test |

All contract classes share one package on purpose: Modulith treats sub-packages of a module as internal, so `contracts.events` would be invisible to other modules unless the module were declared OPEN (which would need a Spring dependency in `contracts`).

### Package layout inside each module

```
com.recruiting.platform.<module>            public API: interfaces (e.g. AccountApi, PersonApi), their DTO records, SPIs
com.recruiting.platform.<module>.domain     JPA entities and repositories (db-engineer writes them, module owner uses them)
com.recruiting.platform.<module>.internal   services, transition logic, event listeners, scheduled jobs
com.recruiting.platform.<module>.web        REST controllers and request/response records matching openapi.yaml
```

Only the top-level package is visible to other modules. Entities are never shared: other modules get DTO records or ids.

### Allowed dependencies

Declared in each module's `package-info.java` with `@ApplicationModule(allowedDependencies = …)`:

| Module | May depend on |
| --- | --- |
| contracts | – |
| security | contracts |
| accounts | contracts, security |
| pipeline | contracts, security, accounts |
| privacy | contracts, security, accounts, pipeline |
| ai (Phase 2) | contracts, security, accounts, pipeline, privacy |

No cycles. Anything that has to flow "upwards" (e.g. pipeline facts into accounts, privacy facts into accounts) travels as a domain event defined in `contracts` (see `contracts/events.md`), so the listener never references the publisher's types.

What each module exposes:

- **security**: `CurrentUser` (id, role), `CurrentUserProvider`, `UserDirectory` (`UserRef` lookup by ids), `AccessLog.record(action, entityType, entityId)`.
- **accounts**: `AccountApi` (refs and existence checks), `PersonApi` (summaries, existence), `PersonDataApi` (export data, `anonymise`, `delete`), SPI `SearchContributor` (pipeline implements it for job orders, so `/search` covers job orders without an accounts → pipeline dependency).
- **pipeline**: `JobOrderApi`, `ApplicationApi`, `ActivityApi` (read-only, for privacy export and Phase 2 ai).
- **privacy**: nothing in Phase 1.

### Event flow

Publish inside the writing transaction, consume with `@ApplicationModuleListener` (after commit, async, at-least-once via the JPA event publication registry `spring-modulith-starter-jpa`). Main flows:

- accounts → pipeline: `AccountCreated/Updated/StatusChanged`, `ContactAdded/Removed` feed the timeline.
- pipeline → accounts: `JobOrderCreated/StatusChanged` keep `account.open_job_count`; `ActivityLogged` keeps `last_activity_at` and `person.last_contact_at`.
- accounts → privacy: `PersonCreated/Updated` start consent and retention; privacy → accounts: `ConsentStatusChanged`, `RetentionFlagged` keep copies on `person` for lists and badges.
- privacy → accounts (sync call): `PersonDataApi.anonymise`; accounts → pipeline/privacy: `PersonAnonymised` scrubs free text.

### Ownership decisions that differ from the build plan

- **Account status (A-02) lives in `accounts`**, not `pipeline`. The status is a column of `account`, filtered on by the accounts list (A-01) and needed by search; putting the write in `pipeline` would force pipeline to write another module's table. The rules stay in one place (`AccountStatus` in contracts). backend-pipeline still owns the timeline entries for it.
- **Reference data (`GET /reference-data`) lives in `accounts`**, since it reads only contracts enums plus the sectors in use.
- **Search (A-14) lives in `accounts`** with the `SearchContributor` SPI for job orders.

## Consequences

- Agents build in parallel against public APIs and events; Modulith fails CI on any illegal reference.
- Some data is copied (consent status, counts, last-contact date) and is eventually consistent (milliseconds after commit). Guards that must be exact are enforced on the side that owns the data (e.g. pipeline refuses new job orders on a CLOSED account).
- Splitting a module into a service later is possible: events are already the integration contract.
- Integration tests per module can use `@ApplicationModuleTest` and Modulith `Scenario` for event-driven assertions.

## Alternatives considered

- **Microservices**: rejected for the pilot — operational cost, distributed transactions, and KVKK data in several stores.
- **Plain packages without Modulith**: no enforcement; with 8 agents in parallel boundaries would erode within one wave.
- **Events owned by publisher modules** (Modulith default): creates cycles (accounts ↔ pipeline, accounts ↔ privacy) or forces chains of APIs; a small shared kernel is cheaper.
