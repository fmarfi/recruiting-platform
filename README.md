# Executive search platform

Spring Boot / Modulith / PostgreSQL backend and React + Mantine frontend. Orchestrator guide: `CLAUDE.md`. Where the work stands: `docs/plan/STATE.md`.

## Run it
| What | Command |
| --- | --- |
| Database and mail | `docker compose up -d` (needs `.env`, see `.env.example`; local Postgres is on 5433) |
| Demo data | `seed/load.ps1` (logins `admin@`, `manager@`, `consultant@demo.example`, password `Demo-Password-2026`) |
| Backend (http://localhost:8080) | `mvn -q install -DskipTests`, then `mvn -pl platform-app spring-boot:run -Dspring-boot.run.profiles=local` |
| **UI (http://localhost:5173)** | in `frontend/`: `npm run dev` (add `npm run mock` to use the mock API instead) |
| Build and test | `mvn -B verify -Pintegration` (Docker running); in `frontend/`: `npx tsc --noEmit`, `npx vitest run` |

## Docs
| Folder | What is in it |
| --- | --- |
| `docs/product/` | requirements, Product Plan, user stories, UX specs and screenshots |
| `docs/architecture/` | ADRs, code structure, hosting |
| `docs/plan/` | `STATE.md` (resume here), `work-plan.md`, `TASKS.md`, `status-lifecycles.md` |
| `docs/process/` | agent hand-offs, briefings, test plans, token budget |
| `docs/archive/` | superseded plans and reviews |

Contracts (API, schema, enums, events) live in `contracts/`.

## Rules
Fictional data only; never real people or the pilot client's name. New Claude session per wave: "Read CLAUDE.md and docs/plan/STATE.md, then continue."
