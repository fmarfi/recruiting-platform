# ADR 0003: UI framework — React SPA on the REST API (not Vaadin)

- Status: accepted (Wave 0), to be confirmed by the orchestrator before Wave 1
- Date: 2026-10-16

## Context

The requirements leave the choice open: Vaadin (all Java, ready-made grids and forms) or React on a REST API (more freedom for custom UX like the pipeline board). Facts that decide it here:

- **The build plan relies on a mock server.** Two frontend agents start in Wave 1 against a mock generated from `openapi.yaml`, before any backend exists. Vaadin Flow views call Java services in the same JVM; there is no HTTP contract to mock, so frontend work would wait for backend services.
- **The pipeline board** (UX-03, UX-04): drag-and-drop with illegal moves blocked and a reason dialog on REJECTED. The demo already implements it in plain JS against REST; Vaadin would need a custom component or an add-on.
- **The demo** (`demo/`) is a REST API plus a static HTML/JS client; its `API.md` is the seed of our contract, and pilot feedback is collected on its look and flow. Porting it to React components keeps the same request/response shapes.
- **Ownership**: two frontend agents own separate folders; a separate frontend project gives clean paths (`frontend/src/shell`, `…/pipeline`) and its own CI job, instead of Vaadin views mixed into the Java modules owned by backend agents.
- **NFR-11 portability** wants a REST API anyway.
- **UX-09 responsive, UX-06 quick add on a phone**: both options can do it; React gives finer control.

## Decision

A single-page app in `frontend/`:

- **React 19 + TypeScript (strict) + Vite.**
- **One component library: Mantine** (current major at Wave 1 start) — tables, forms, modals, date inputs, responsive layout and theming from `contracts/design-tokens.json` (UX-07).
- **API client generated from the contract**: `openapi-typescript` produces `frontend/src/api/schema.d.ts` from `contracts/openapi.yaml`; calls go through `openapi-fetch`. Generated at build time, never edited; a contract change breaks the frontend build instead of production.
- **Data**: TanStack Query (caching, optimistic updates on the board with rollback on 409).
- **Routing**: React Router. **Board**: dnd-kit. **i18n**: i18next with `tr` and `en` resources; status labels come from `GET /reference-data`.
- **Tests**: Vitest + Testing Library; Playwright end to end (qa-engineer).
- **Folders and owners**: `frontend/src/shell/`, `frontend/src/accounts/` (frontend-shell), `frontend/src/pipeline/`, `frontend/src/network/` (frontend-pipeline), `frontend/src/api/` and build config (devops). Shared UI pieces such as the status badge go in `frontend/src/shell/ui/`; frontend-pipeline requests changes there.
- **Serving**: production build copied into `platform-app` static resources (same origin, session cookie and CSRF work without CORS). Development: Vite dev server proxies `/api/v1/*` to the Prism mock (`rewrite` strips `/api/v1`, see `contracts/README.md`) or to the real backend.

## Consequences

- Frontends start on day one of Wave 1 against the mock; backend and frontend meet at the contract.
- The team needs TypeScript skills alongside Java; mitigated by generated types and one component library.
- Two builds (Maven, npm) — devops wires the npm build into CI and into the jar (e.g. `frontend-maven-plugin` or a CI step).

## Alternatives considered

- **Vaadin Flow**: fastest for pure CRUD in Java, but blocks mock-first parallel work, needs a custom board component, and puts UI code into backend-owned modules. Rejected for this build.
- **Vaadin Hilla (React + Java endpoints)**: keeps React, but its endpoints are not our OpenAPI contract and tie the client to Hilla's generator.
- **Server-rendered Thymeleaf + htmx**: simple, but weak for the board and offline-ish mobile quick add; no mock-first story.
