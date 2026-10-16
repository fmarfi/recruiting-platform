# Wave 1b briefing (all builders read this)

Goal of the wave: the real app runs end to end. You log in at http://localhost:5173 (Vite proxy to the backend on 8080) and click through every Phase 1 screen with the demo dataset loaded.

## Where you work
- Each agent has its own git worktree under `.claude/worktrees/wave1b-<role>` on branch `worktree-wave1b-<role>`, based on main. Work and commit only there. Never touch the main checkout or another worktree.
- Read your role file `.claude/agents/<role>.md` and follow it. Ownership: `grep -n -A 16 "Ownership map" docs/build-plan.md`.
- Line endings: LF (`.gitattributes`). Don't rewrite files you don't change.
- Never edit `docs/TASKS.md` or `contracts/`. Put requests to other owners under "Change requests" in your hand-off.

## Ports (several agents run at once, so use only your own)
| Agent | Spring app | Prism mock | Vite |
| --- | --- | --- | --- |
| backend-accounts | 8081 | – | – |
| backend-pipeline | 8082 | – | – |
| security-privacy | 8083 | – | – |
| frontend-shell | – | 4011 | 5174 |
| frontend-pipeline | – | 4012 | 5175 |
| qa-engineer | 8084 | 4013 | 5176 |
| synthetic-data | 8085 | – | – |
Start a Spring app with `-Dserver.port=<port>`, Prism with `npx prism mock -p <port> ../contracts/openapi.yaml`, and Vite with `npx vite --port <port>`. Stop every server you start before you finish. Shared Postgres for manual runs: `docker compose up -d` (port 5433, env in `.env`). Each agent gets its own database on it: `docker compose exec -T postgres createdb -U recruiting <role_with_underscores>`. Automated tests use Testcontainers (`*IT`, `mvn -B verify -Pintegration`).

## Backend rules (backend-accounts, backend-pipeline, security-privacy)
- All paths are under `/api/v1` (openapi `servers`). Implement exactly the shapes, status codes and error bodies in `contracts/openapi.yaml` (errors per `contracts/errors/` and ADR 0006), and the transitions in `docs/status-lifecycles.md`.
- Auth per ADR 0007. Session cookie, CSRF cookie `XSRF-TOKEN` and header `X-XSRF-TOKEN`, and roles CONSULTANT < MANAGER < ADMIN via `RoleHierarchy`. security-privacy owns the `SecurityFilterChain` and enables method security. Feature controllers use `@PreAuthorize` per the operation's `x-roles`. Until security merges, test controllers with `@WebMvcTest` plus `spring-security-test` (`@WithMockUser(roles=...)`, `.with(csrf())`).
- Entities and repositories already exist in `<module>/.../domain/` (db-engineer). You may add finder methods or projections to your own module's repositories. Report any entity or migration change as a change request, except trivial finders. Migrations belong to db-engineer.
- Cross-module calls only through events (`contracts/events.md`, Spring Modulith `@ApplicationModuleListener`) or public APIs allowed by ADR 0001. `ModularityTests` must stay green.
- The current user id comes from `security`'s public API (`com.recruiting.platform.security.CurrentUser` or similar). security-privacy publishes it first thing. Until then, take a `UUID actorId` parameter in your services.

## Frontend rules (frontend-shell, frontend-pipeline)
- Build against the mock first. Use the typed client in `frontend/src/api/`. Don't edit `src/api/`, `src/theme/`, `package.json` or `vite.config.ts` (devops). Ask for new dependencies via change requests, unless Mantine sub-packages (`@mantine/dates`, `@mantine/notifications`, `@mantine/form`) at version 9.7.2 or `@dnd-kit/*` are needed, which you may add.
- Shared files: in `src/routes.tsx`, replace only the placeholder elements of your own screens (frontend-shell also replaces the temporary `<nav>` with the real shell layout and owns `/login`). In `src/i18n/`, keep your strings in `src/<area>/i18n/{tr,en}.json`. Register them in `src/i18n/index.ts` with one line each. The orchestrator resolves the trivial merge conflicts.
- Specs: `docs/ux/screens/*.md`, `docs/ux/flows.md`, `docs/ux/README.md`, stories in `docs/stories/`. Status badge colours come from the theme tone tokens. Labels come from `/reference-data`.

## Demo login (security-privacy and synthetic-data agree on this)
- Passwords use `DelegatingPasswordEncoder`, so stored values look like `{bcrypt}$2a$10$...`.
- The seed creates three users with the password `Demo-Password-2026`: `admin@demo.example` (ADMIN), `manager@demo.example` (MANAGER) and `consultant@demo.example` (CONSULTANT). Set `must_change_password=false` for the demo users.

## Hand-off
`docs/handoffs/<role>.md` per TEMPLATE (under 40 lines; append a Wave 1b section if the file exists). Final reply to the orchestrator at most 150 words.
