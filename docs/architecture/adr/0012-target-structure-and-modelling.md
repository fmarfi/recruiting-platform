# ADR 0012: Target structure and modelling approach

Status: PROPOSED (2026-10-26, orchestrator; needs the user's decision on item 2) · Supersedes nothing; refines ADR 0001 (modular monolith) and ADR 0008 (one person identity).

## Context
The repo grew wave by wave and shows it (survey 2026-10-26): 13 loose files in `docs/` with three overlapping plans and three entry points; a 5,150-line `openapi.yaml`; hand-written `schema.md`; enum values kept by hand in Java, OpenAPI and DB CHECKs (MigrationIT already caught one drift); `crm-accounts` is 119 files and is about to absorb files, Tika, duplicates and merge; module folders (`crm-accounts`) and packages (`accounts`) are named differently; entities live in a `domain` package apart from the feature packages; JPA entities are also the domain model, so invariant-heavy rules (consent, stage guards, retention) are spread across services.

## Decision (technical approach)

### 1. Strategic modelling: bounded contexts = Spring Modulith modules
Run one event-storming pass (written as a table in `docs/architecture/context-map.md`: commands, events, aggregates, policies per context) and keep a context map with relationship types (customer/supplier, conformist, published language). Target contexts: **accounts** (accounts, contacts, views, search), **talent** (person identity, profile, CV files, intake, duplicates, merge, off-limits), **pipeline** (job orders, applications, proposals, exposure, bulk), **privacy** (consent, retention, DSR, purge), **security** (users, roles, audit access, field cipher), **ai** (Phase 2). Cross-context calls only through a published API/SPI or domain events; verified by Modulith `verify()` and ArchUnit.

### 2. DECISION NEEDED: extract `talent` from `accounts`
ADR 0008 keeps one person identity but does not say which module owns it. Today it is `accounts`, which then owns CV files, Tika, duplicates and merge: three unrelated reasons to change. Recommended: move person, profile, files, intake, duplicates, merge and off-limits into a new `talent` module; `accounts` keeps accounts, contacts, views and search and refers to people by id. Cost: one refactor stage (package moves, SPI for search contributors, contract owner labels, tests) with no behaviour change; best done right after the Wave 2b backends merge and before the UX stage. Alternative: keep one module and enforce internal sub-packages with ArchUnit (cheaper, but `accounts` keeps growing).

### 3. Tactical modelling: tiered, not dogma
- Invariant-heavy contexts (pipeline stages and guards, consent and retention, merge, bulk): rich domain model in a framework-free `domain` package (aggregates, value objects, domain services, domain events), use cases in `application`, adapters in `adapter.in.web` and `adapter.out.persistence` (hexagonal). Entities map to JPA through persistence adapters or annotated models where the cost is not worth a second model; the rule is enforced by ArchUnit onion rules (domain depends on nothing, application on domain, adapters on both).
- CRUD-like contexts (reference data, views, settings): keep the pragmatic vertical slice (web, app, domain-entity) without extra layers.
- jMolecules annotations (`@AggregateRoot`, `@DomainEvent`, `@Repository`) make the model machine-checkable and feed the generated documentation.

### 4. One layout everywhere
Backend module folder = context name (`accounts`, `talent`, `pipeline`, `privacy`, `security`, `ai`, `contracts`, `app` instead of `platform-app`), package `com.recruiting.platform.<context>`; inside: `domain`, `application`, `adapter/in/web`, `adapter/out/persistence`, `api` (the only package other contexts may import). Tests live in the module they test (platform-app keeps only end-to-end and architecture tests). Frontend: Feature-Sliced layout (`app`, `pages`, `widgets`, `features`, `entities`, `shared`) with eslint-plugin-boundaries so layers import only downward; the shell grid, command bar and view store move to `shared`/`features`.

### 5. Contracts and single sources of truth
- Split `openapi.yaml` per tag under `contracts/api/` with `$ref`, bundled by `redocly bundle` into the generated artefact; lint and Prism run on the bundle.
- Enums declared once in `contracts/enums/*.yaml` and generated into Java enums, TypeScript unions and the DB CHECK migration helper test, removing the drift class that MigrationIT catches today.
- `schema.md` keeps decisions and rules only; the ER diagram and column listing are generated from the migrated database (SchemaSpy or Mermaid ER) in CI.

### 6. Architecture as code
C4 diagrams (context, container, component) generated from Spring Modulith `Documenter` output (PlantUML) into `docs/architecture/generated/`; arc42-style skeleton in `docs/architecture/`; ADRs stay (MADR template). Architecture fitness tests in `app`: module cycles, layer rules, "no entity leaves its module", "contracts has no dependencies".

### 7. Docs and process layout
`docs/` becomes: `product/` (requirements, product plan, stories, ux), `architecture/` (context map, arc42, generated, `adr/`), `plan/` (work-plan, TASKS, status-lifecycles), `process/` (agents, handoffs per wave, token budget), `archive/` (old drafts, superseded plans). One entry point: `README.md` (what, run, where to read next); `CLAUDE.md` stays the orchestrator guide; `README.md` and `plan/STATE.md` merge into `docs/plan/STATE.md`.

## Consequences
- Needs a restructure stage with NO parallel agent branches open (moves rewrite paths): schedule after the Wave 2b backends merge, before the UX stage, as 'Stage Structure' (see `docs/plan/work-plan.md`).
- Contract file moves and generated enums touch every owner's imports once; do them in a single reviewed branch with `mvn verify -Pintegration`, frontend tests and Modulith verification green before and after (behaviour must not change).
- Rich-domain extraction is incremental, context by context, starting with pipeline stage guards and consent/retention, each behind its existing API tests.
