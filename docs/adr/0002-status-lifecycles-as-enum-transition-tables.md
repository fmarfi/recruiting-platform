# ADR 0002: Status lifecycles as enum transition tables, not Spring Statemachine

- Status: accepted (Wave 0)
- Date: 2026-10-16

## Context

Accounts (A-02), proposals (A-05), job orders (A-06), pipeline stages (A-07), consent (S-01) and data-subject requests (S-04) each have a closed set of statuses with allowed moves (`docs/status-lifecycles.md`). Some moves need a reason, some are manager-only, a few depend on data (OFFER → FILLED needs a PLACED candidate; an account with open job orders cannot close). The UI must show the allowed next statuses on every badge and board, and the same rules must hold in the backend, the mock and the tests. The requirements list "enums with explicit transition rules, or Spring Statemachine; a plain transition table is enough for the pilot".

## Decision

Each lifecycle is a Java enum in `contracts/enums/` implementing `Lifecycle<S>`:

- `next()` — the transition table (`EnumSet` per status);
- `requiresReason(target)` — moves that need a written reason;
- `requiresElevatedRole(target)` — moves limited to MANAGER and ADMIN;
- `labelEn()`, `labelTr()`, `tone()` — badge text and tone (colours live in design tokens);
- `canMoveTo`, `isTerminal`.

Services call `IllegalTransitionException.check(from, to, reason)` (409 `ILLEGAL_TRANSITION` / 400 `REASON_REQUIRED`), then check `requiresElevatedRole` against the current user (403), then the data guards (409 `TRANSITION_GUARD_FAILED`), then save and publish the `…StatusChanged` event with `reason`. `GET /reference-data` serialises the same enums, so the UI never hard-codes transitions; the OpenAPI example for that endpoint is generated from the enums.

Status lists are closed sets in Phase 1: adding a status is a code change (enum + migration CHECK + design token), reviewed by the architect. The Settings screen shows them read-only.

## Consequences

- One source of truth, compile-checked, trivially unit-testable (a table test per enum), no framework to learn.
- Status is a plain column; history comes from Envers and the timeline (ADR 0005).
- Data guards live in services, not in the enum — documented next to each enum in Javadoc and in the OpenAPI descriptions.
- Client-editable status lists (open question to the client) would need a different model (statuses as data). Revisit if the pilot asks for it; the API already treats statuses as strings with metadata, so the frontend would not change.

## Alternatives considered

- **Spring Statemachine**: suited to long-running machines with actions, timers and persisted machine state. Our lifecycles are simple, stateless checks on one column; the framework adds configuration, a persistence model per entity and a learning curve for every builder, and has seen little development recently. Rejected.
- **Statuses as database rows (configurable)**: flexible but loses compile-time safety and makes the contract (enums, badges, tests) unstable. Deferred until the client confirms they need it.
