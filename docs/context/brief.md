# Project brief (read this instead of the full docs)

One page on purpose. For a specific requirement, look up its row by ID instead of reading the whole file: `grep -n "| A-07 |" docs/requirements.md`.

## What we build
Recruitment platform for a Turkish executive search / staffing consultancy, so candidate and client data lives in the firm's own system.
- **Phase 1, Module A: CRM app.** Client accounts with status, contacts, the firm's network of people, proposals, job orders, a candidate pipeline per job order, timeline, next actions, global search. Salesforce-style concepts, but simpler.
- **Phase 2, Module B: AI tool.** CV upload and parsing, candidate pool, AI suitability score with reasons, AI shortlist feeding the pipeline, blind mode, candidate reports.
- **Across both:** KVKK consent and retention; roles (consultant, manager, admin); audit log; Turkish and English UI.

## Requirement ID ranges (rows in docs/requirements.md)
| IDs | Area |
| --- | --- |
| A-01..A-14 | CRM: accounts, status, contacts, activities, proposals, job orders, pipeline, timeline, dashboard, next actions, CSV import, network, links, search |
| B-01..B-13 | AI tool (Phase 2) |
| S-01..S-04 | KVKK consent e-mails, website consent, retention, data-subject requests |
| NFR-01..NFR-11 | Privacy, retention, data location, security, AI transparency, fairness, performance, language, usability, portability |
| UX-01..UX-10 | My day home, 3 clicks, status badges, kanban board, account page, quick add, consistency, empty states, responsive, user tests |

## Hard rules
- Synthetic data only. Never real people, never the pilot client's name.
- KVKK: every person gets an information notice (also LinkedIn-sourced); explicit consent is a separate step; default retention 2 years, configurable; deletion or anonymisation is logged.
- AI never changes a status without a human action; every AI score shows its reasons.
- No LinkedIn scraping; LinkedIn data only via candidate-provided files or official integrations.

## Stack
Java 21+ on Spring Boot 4.1, Spring Modulith, PostgreSQL + pgvector, Flyway, Hibernate Envers, Spring Security, Spring Mail. Spring AI 2.0 in Phase 2. Tests: JUnit 5, Testcontainers, Playwright.

## Module layout
Maven multi-module, base package `com.recruiting.platform`: `contracts`, `crm-accounts`, `crm-pipeline`, `security`, `privacy`, `ai`, `platform-app`. Ownership of each path: the table "Ownership map" in docs/build-plan.md (`grep -n -A 14 "Ownership map" docs/build-plan.md`).

## Where the truth lives
| Question | File |
| --- | --- |
| Which statuses and moves exist? | docs/status-lifecycles.md, contracts/enums/ |
| Which endpoints and fields? | contracts/openapi.yaml |
| Which tables? | contracts/schema.md |
| Which events between modules? | contracts/events.md |
| What does a screen show? | docs/ux/screens/ |
| What must a story satisfy? | docs/stories/<ID>.md |
| Who does what now? | docs/TASKS.md |
| What did another agent leave? | docs/handoffs/<agent>.md |

`demo/` is a throwaway prototype. Use it only when a task says so.
