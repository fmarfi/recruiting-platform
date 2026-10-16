# AI Recruiting System — Agent Build Plan

Oct 16, 2026 · @asim

## How the build is split

The build runs as 15 specialist agents in 4 waves, coordinated by one orchestrator: Wave 0 freezes the shared contracts, Wave 1 builds Phase 1 (the CRM app) with 8 builders in parallel, Wave 2 integrates and hardens, and Wave 3 adds the AI tool (Phase 2). It follows the companion requirements doc; requirement IDs (A-, B-, S-, NFR-, UX-) are used throughout.

Three rules make the parallel work safe and fast:

- **Contract first.** API, database schema, status enums, events and design tokens are fixed before feature work starts, so agents build against the same interfaces without waiting for each other.
- **One owner per area.** Every package, folder and file has exactly one owning agent; others request changes instead of editing.
- **Isolated workspaces.** Each agent works on its own branch in its own git worktree and hands back a small, tested pull request that the orchestrator merges in a fixed order.

## Build waves

The critical path is short: Wave 0 is the only stretch where agents wait on each other, so it is kept small (contracts only, no features).

&#91;embedded content: build waves · 4 waves, 3 gates\]

Wave 1 is the widest point: all eight builders start together against the frozen contracts and the mock server. In Wave 2 the frontends switch from the mock to the real API, QA runs end-to-end tests, and the CRM goes to the pilot. Wave 3 starts the AI tool on top of live CRM data. The orchestrator and code-reviewer run through every wave; gate checklists are under Coordination rules.

## Agent roster

15 specialist agents plus the orchestrator: 4 contract agents, 9 builders and reviewers for Phase 1, and 2 AI agents for Phase 2. Model suggestions are a starting point: the strongest model where judgement matters most (architecture, review, AI), a faster one for well-specified build work.

| # | Agent | Profession | Owns | Wave | Suggested model |
| --- | --- | --- | --- | --- | --- |
| 0 | `orchestrator` | Tech lead | Plan, task board, merges, conflict resolution | All | Opus |
| 1 | `product-analyst` | Business analyst | User stories, acceptance criteria, status lifecycles | 0 | Sonnet |
| 2 | `architect` | Software architect | Module layout, OpenAPI contract, events, ADRs | 0 | Opus |
| 3 | `ux-designer` | UX/UI designer | Wireframes, user flows, design tokens, screen specs | 0 | Sonnet |
| 4 | `devops` | DevOps engineer | Repo skeleton, Docker, CI pipeline, environments | 0 | Sonnet |
| 5 | `db-engineer` | Database engineer | Schema, Flyway migrations, JPA entities, Envers audit | 1 | Sonnet |
| 6 | `backend-accounts` | Backend developer | Accounts, contacts, network, links, search (A-01, A-03, A-11 to A-14) | 1 | Sonnet |
| 7 | `backend-pipeline` | Backend developer | Job orders, proposals, pipeline, status rules, timeline, next actions (A-02, A-04 to A-10) | 1 | Sonnet |
| 8 | `security-privacy` | Security and privacy engineer | Auth, roles, audit log, KVKK consent, retention jobs (S-01 to S-04, NFR-01 to NFR-05) | 1 | Sonnet |
| 9 | `frontend-shell` | Frontend developer | App shell, My day, accounts list and page, search (UX-01, UX-02, UX-05) | 1 | Sonnet |
| 10 | `frontend-pipeline` | Frontend developer | Pipeline board, network list and person page (UX-03, UX-04, UX-06) | 1 | Sonnet |
| 11 | `synthetic-data` | Test data engineer | Synthetic accounts, people, job orders and CVs for the demo | 1 | Haiku |
| 12 | `qa-engineer` | QA/test engineer | Integration and end-to-end tests from the acceptance criteria | 1–2 | Sonnet |
| 13 | `code-reviewer` | Senior reviewer | Fresh-eyes review of every pull request; never writes features | 1–3 | Opus |
| 14 | `ai-engineer` | AI/ML engineer | CV parsing, embeddings, scoring and screening with Spring AI (B-01, B-02, B-06, B-07) | 3 | Opus |
| 15 | `ai-evaluator` | AI quality and fairness analyst | Evaluation set, score accuracy, bias checks, blind mode (B-08, NFR-07) | 3 | Sonnet |

## Agent briefs

Each brief is written so it can be pasted into the agent's file as its system prompt: mission, inputs, outputs, what it must not touch, and when it is done.

### 0. orchestrator — Tech lead

- **Mission:** turn the requirements doc into a task board, launch agents per wave, merge their work, and keep the build moving.
- **Inputs:** requirements doc, this plan, agent hand-off notes.
- **Outputs:** `docs/TASKS.md` (task, owner, status, blocked-by), merged `main` branch, wave gate decisions.
- **Must not:** write feature code itself; change a frozen contract without the architect.
- **Done when:** every task is merged with green CI and the wave gate checklist passes.

### 1. product-analyst — Business analyst

- **Mission:** make every requirement buildable and testable.
- **Inputs:** requirements doc (A-, B-, S-, UX- IDs), open questions.
- **Outputs:** `docs/stories/*.md` with one user story per requirement and Given/When/Then acceptance criteria; `docs/status-lifecycles.md` listing every status and allowed transition for accounts, proposals, job orders and pipeline stages.
- **Must not:** decide architecture or UI layout.
- **Done when:** every Must and Should requirement in Phase 1 has a story with at least one testable criterion, and every status list is a closed set.

### 2. architect — Software architect

- **Mission:** design the system so other agents can build in parallel without colliding.
- **Inputs:** requirements doc, stack choice (Spring Boot 4, Spring Modulith, PostgreSQL + pgvector), status lifecycles.
- **Outputs:** package layout per module; `contracts/openapi.yaml`; shared enums in `contracts/`; domain events list; Architecture Decision Records in `docs/adr/`.
- **Must not:** implement business logic.
- **Done when:** the OpenAPI file validates, a mock server runs from it, and the Modulith verification test passes on the empty skeleton.

### 3. ux-designer — UX/UI designer

- **Mission:** make the CRM easier than Salesforce for a consultant on day one.
- **Inputs:** UX-01 to UX-10, user stories, Salesforce as reference.
- **Outputs:** user flows and wireframes for the 6 core screens; `contracts/design-tokens.json` (colours, spacing, type, status badge colours); a screen spec per screen listing data fields, actions, empty and error states.
- **Must not:** write production frontend code.
- **Done when:** every Phase 1 story maps to a screen and action, and every status has a badge style.

### 4. devops — DevOps engineer

- **Mission:** a working pipeline from the first commit.
- **Outputs:** Maven multi-module skeleton, `docker-compose.yml` (PostgreSQL with pgvector, mail catcher), CI running build, tests and lint on every pull request, environment config, hosting notes with the KVKK data-location constraint.
- **Must not:** touch feature packages.
- **Done when:** a fresh clone runs `docker compose up` plus the app, and CI is green on the skeleton.

### 5. db-engineer — Database engineer

- **Mission:** a schema that matches the contracts and keeps full history.
- **Outputs:** Flyway migrations, JPA entities, repositories, Envers audit tables, indexes for search and filters.
- **Must not:** add endpoints or business rules.
- **Done when:** migrations run clean on an empty database and Testcontainers repository tests pass.

### 6. backend-accounts — Backend developer (accounts and network)

- **Mission:** the client and people side of the CRM.
- **Scope:** A-01, A-03, A-11 to A-14: accounts, contacts, network people and links, CSV import, global search.
- **Outputs:** services and REST controllers matching `openapi.yaml` for its endpoints, with unit and integration tests.
- **Must not:** edit pipeline, proposal or security packages.
- **Done when:** its endpoints pass the contract tests and the acceptance criteria of its stories.

### 7. backend-pipeline — Backend developer (status and pipeline)

- **Mission:** everything that has a status and moves.
- **Scope:** A-02, A-04 to A-10: account status, activities, proposals, job orders, candidate pipeline, status transition rules, timeline, next actions.
- **Outputs:** status transition service enforcing `status-lifecycles.md`; endpoints per contract; domain events (e.g. status changed) for the timeline.
- **Must not:** edit accounts or network packages.
- **Done when:** illegal status moves are rejected, every change appears in the timeline, and tests pass.

### 8. security-privacy — Security and privacy engineer

- **Mission:** safe by default and KVKK-compliant.
- **Scope:** login, roles (consultant, manager, admin), audit log, KVKK notice and consent e-mails, consent records, retention job, data-subject requests (S-01 to S-04, NFR-01 to NFR-05).
- **Must not:** change business logic in other modules; it adds security through configuration and its own package.
- **Done when:** every endpoint requires a role, consent status is stored per person, and the retention job is tested with time-shifted data.

### 9. frontend-shell — Frontend developer (shell and accounts)

- **Mission:** the frame of the app and the client screens.
- **Scope:** layout, navigation, My day home, accounts list, account page with timeline, global search (UX-01, UX-02, UX-05, UX-07 to UX-09).
- **Inputs:** screen specs, design tokens, mock server from `openapi.yaml`.
- **Must not:** edit pipeline or network screens; invent API fields not in the contract.
- **Done when:** its screens work against the mock server and then the real API, on laptop and phone widths.

### 10. frontend-pipeline — Frontend developer (pipeline and network)

- **Mission:** the screens where work moves.
- **Scope:** job order page with the kanban pipeline board, status badges and changes, quick add for activities, network list and person page with connections (UX-03, UX-04, UX-06).
- **Must not:** edit shell or account screens.
- **Done when:** a candidate can be dragged across stages and the change shows in the account timeline.

### 11. synthetic-data — Test data engineer

- **Mission:** realistic, fully fictional data for the demo and tests.
- **Outputs:** seed scripts with Turkish and English company and person names, accounts in every status, job orders, pipelines and activity history; later, synthetic CVs for Phase 2.
- **Must not:** use any real person's data.
- **Done when:** one command loads the demo dataset and every screen has meaningful content.

### 12. qa-engineer — QA/test engineer

- **Mission:** prove each story works end to end.
- **Outputs:** automated tests from acceptance criteria (Testcontainers for API, Playwright for UI), a regression suite in CI, bug reports assigned to owners.
- **Must not:** fix bugs in other agents' code; it reports them.
- **Done when:** every Must story has a passing end-to-end test.

### 13. code-reviewer — Senior reviewer

- **Mission:** catch what the author cannot see in their own work.
- **Checks:** contract compliance, module boundaries, security and KVKK, test quality, readability.
- **Outputs:** review comments with severity (blocker, should fix, nit).
- **Must not:** write features or approve its own suggestions.
- **Done when:** no open blockers on the pull request.

### 14. ai-engineer — AI/ML engineer (Phase 2)

- **Mission:** the AI recruitment tool on top of the CRM data.
- **Scope:** CV text extraction (Apache Tika), structured profile extraction, embeddings in pgvector, suitability score with reasons, ranked shortlist feeding the pipeline (B-01, B-02, B-06, B-07).
- **Must not:** let the AI change a status without a human action (NFR-06).
- **Done when:** scores come back as typed objects with reasons, and the evaluator's accuracy threshold is met.

### 15. ai-evaluator — AI quality and fairness analyst (Phase 2)

- **Mission:** measure whether the AI is right and fair.
- **Outputs:** labelled evaluation set of synthetic CVs and job orders, accuracy report per prompt or model change, bias checks across protected attributes, blind-mode tests (B-08, NFR-07).
- **Must not:** tune prompts itself; it reports results to the ai-engineer.
- **Done when:** every AI change has a before/after report attached to its pull request.

## Contracts that unlock parallel work

Five files, all in `contracts/` and written in Wave 0, let nine agents start Wave 1 at the same time. After the Wave 0 gate they are frozen: changes go through the architect as a small, versioned pull request that every affected agent sees.

| Contract | Owner | Who builds against it | What it removes |
| --- | --- | --- | --- |
| `openapi.yaml` — every endpoint, request and response | architect | backend agents implement it; frontend agents code against a mock server generated from it; QA writes contract tests | Frontend waiting for backend |
| `status-lifecycles.md` + shared Java enums — every status and allowed transition | product-analyst, then architect | backend-pipeline enforces it; frontends show it as badges; QA tests every transition | Arguments about what statuses exist |
| `schema.md` — tables, keys, relationships (from the data model) | architect, then db-engineer | db-engineer writes migrations; backend agents write services in parallel against entity interfaces | Backend waiting for migrations |
| `events.md` — domain events between modules (e.g. status changed, candidate shortlisted) | architect | backend-pipeline publishes; timeline and later the AI module listen | Modules calling each other's internals |
| `design-tokens.json` + screen specs | ux-designer | both frontend agents | Two frontends that look different |

The mock server matters most: with it, the two frontend agents ship working screens in Wave 1 before a single backend endpoint exists.

## Coordination rules

Speed comes from agents never blocking or overwriting each other; these rules are what the orchestrator enforces.

**Ownership map** — one owner per path; anyone else opens a change request in `docs/TASKS.md`:

| Path | Owner |
| --- | --- |
| `contracts/` | architect (ux-designer for design tokens) |
| `crm-accounts/` (accounts incl. account status, contacts, network, import, search, reference data) | backend-accounts |
| `crm-pipeline/` (job orders, pipeline, proposals, activities, next actions, timeline, home) | backend-pipeline |
| `platform-app/src/main/resources/db/migration/`, `<module>/.../domain/` (entities, repositories) | db-engineer |
| `security/`, `privacy/` | security-privacy |
| `frontend/src/shell/`, `frontend/src/accounts/`, `frontend/src/settings/` | frontend-shell |
| `frontend/src/pipeline/`, `frontend/src/network/` | frontend-pipeline |
| `seed/` | synthetic-data |
| `tests/e2e/` | qa-engineer |
| `ai/` (Phase 2) | ai-engineer |
| CI, Docker, build files, `frontend/` scaffold and config (package.json, vite, theme, `frontend/src/api/`) | devops |

**Working rules**

1. **One worktree per agent.** Each agent works on its own branch in its own git worktree, so parallel agents never share a working directory.
2. **Small pull requests.** One story or one endpoint group per pull request; anything above a few hundred changed lines is split.
3. **Hand-off note.** Every agent ends with a short note in `docs/handoffs/<agent>.md`: what changed, what is left, what others must know.
4. **Review before merge.** Every pull request goes to code-reviewer; blockers are fixed by the author, not the reviewer.
5. **Fixed merge order within a wave.** db-engineer first, then backend agents, then security-privacy, then frontends; this keeps conflicts in one direction.
6. **Contracts change only through the architect.** Agents that need a new field file a request; the architect updates `contracts/` and notifies affected owners.
7. **Green CI or no merge.** Build, tests and contract tests must pass.

**Gates between waves**

- [ ] **Gate 0 → 1:** contracts merged and frozen, mock server runs, CI green on the skeleton, stories done for all Phase 1 Musts.
- [ ] **Gate 1 → 2:** all Phase 1 Must endpoints and screens merged; demo data loads.
- [ ] **Gate 2 → 3:** end-to-end tests pass on every Must story; 2–3 consultants complete the core tasks unaided (UX-10); pilot feedback triaged.
- [ ] **Phase 2 ready:** CRM pilot live; AI evaluation set agreed with the client.

## Claude Code setup

Each agent becomes one Markdown file in `.claude/agents/` with YAML frontmatter (name, description, tools, model) and its brief as the body; Claude Code picks the agent from its description ([Claude Code docs: subagents](https://code.claude.com/docs/en/sub-agents)). Commit the folder so the whole team shares the same agents.

```
recruiting-platform/
├── .claude/agents/          # one file per agent below
├── contracts/               # openapi.yaml, enums, events.md, design-tokens.json
├── docs/                    # TASKS.md, stories/, adr/, handoffs/
├── crm-accounts/            # Module A: accounts, contacts, network
├── crm-pipeline/            # Module A: statuses, job orders, pipeline
├── security/  privacy/
├── ai/                      # Module B (Phase 2)
├── frontend/
├── seed/                    # synthetic data
└── tests/e2e/
```

Template (`.claude/agents/backend-pipeline.md`):

```markdown
---
name: backend-pipeline
description: Backend developer for statuses and the candidate pipeline (A-02, A-04 to A-10). Use for job orders, proposals, status transitions, timeline and next actions.
tools: Read, Edit, Write, Bash, Grep, Glob
model: sonnet
isolation: worktree
---

You are the backend developer who owns crm-pipeline/.

Mission: everything in the CRM that has a status and moves.
Build against contracts/openapi.yaml and contracts/status-lifecycles.md. Never edit them; request changes in docs/TASKS.md.
Only edit files under crm-pipeline/.
Reject illegal status moves; publish a StatusChanged event for every change.
Write unit and Testcontainers tests for every endpoint.
Finish with a hand-off note in docs/handoffs/backend-pipeline.md.
Done when: contract tests and story acceptance criteria pass.
```

Starting a wave from the orchestrator session: ask for all agents of that wave in one message (e.g. "Run db-engineer, backend-accounts, backend-pipeline and security-privacy in parallel on Wave 1 tasks"), so they run at the same time instead of one after another. Read-only agents such as code-reviewer get only `Read, Grep, Glob` so they cannot edit code.
