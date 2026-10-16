# Task board

Status: todo · in progress · review · done · blocked. Change requests to another owner's files go under "Change requests".

## Wave 0 — contracts (no features)

| ID | Task | Owner | Status | Blocked by |
| --- | --- | --- | --- | --- |
| W0-01 | User stories with Given/When/Then criteria for all Phase 1 Must/Should requirements (A-01–A-14, S-01–S-04, UX-01–UX-10) | product-analyst | review | – |
| W0-02 | `docs/status-lifecycles.md`: closed status sets and transitions for accounts, proposals, job orders, pipeline stages | product-analyst | review | – |
| W0-03 | Module/package layout and ADRs (UI framework, status-machine approach) | architect | in progress | – |
| W0-04 | `contracts/schema.md` from the core data model | architect | in progress | – |
| W0-05 | `contracts/openapi.yaml` for all Phase 1 endpoints + mock server command | architect | in progress | W0-02, W0-04 |
| W0-06 | `contracts/enums/` and `contracts/events.md` | architect | in progress | W0-02 |
| W0-07 | User flows, wireframes and screen specs for the 6 core screens | ux-designer | review | W0-01 (can start from requirements) |
| W0-08 | `contracts/design-tokens.json` incl. a badge per status | ux-designer | review | W0-02 |
| W0-09 | Maven multi-module skeleton, docker-compose (Postgres+pgvector, Mailpit), CI, env config | devops | review | W0-03 |
| W0-10 | `docs/hosting.md` with KVKK data-location options | devops | review | – |

**Gate 0 → 1**
- [ ] Contracts merged and frozen
- [ ] Mock server runs from `openapi.yaml`
- [ ] CI green on the skeleton; Modulith verification test passes
- [ ] Stories done for all Phase 1 Musts

## Wave 1 — Phase 1 build (8 builders in parallel)

To be broken into tasks after Gate 0: db-engineer, backend-accounts, backend-pipeline, security-privacy, frontend-shell, frontend-pipeline, synthetic-data, qa-engineer; code-reviewer on every branch.

**Gate 1 → 2**
- [ ] All Phase 1 Must endpoints and screens merged
- [ ] Demo data loads with one command

## Wave 2 — integrate and harden

**Gate 2 → 3**
- [ ] End-to-end tests pass on every Must story
- [ ] 2–3 consultants complete the core tasks unaided (UX-10)
- [ ] Pilot feedback triaged

## Wave 3 — Phase 2 AI tool

**Phase 2 ready**
- [ ] CRM pilot live
- [ ] AI evaluation set agreed with the client

## Change requests

_None yet._

## Open questions blocking work

From `docs/requirements.md` → Open questions. Agents mark dependent work as ASSUMPTION until answered:
- Status lists used today (affects W0-02)
- Hosting location (affects W0-10)
- CV languages (affects Phase 2 parser)
- Retention start point: upload or last contact (affects S-03)
