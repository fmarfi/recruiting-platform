# Hand-off: devops

## Wave 0 (W0-09, W0-10), condensed

- Root `pom.xml`: Boot `4.1.1`, Java 21, Modulith BOM `2.1.1`. Modules: `contracts`, `security`, `privacy`, `crm-accounts`, `crm-pipeline`, `ai`, `platform-app`. Feature modules depend on `contracts` + `spring-modulith-api`; add other deps in your module pom (dependencies between modules are declared in the pom AND checked by Modulith).
- `docker-compose.yml`: `pgvector/pgvector:0.8.7-pg17`, `axllent/mailpit:v1.31.6`. Config: `application.yml`, `-local`, `-pilot`; `.env.example`. `docs/hosting.md` has hosting options.
- Run: `mvn -B verify` (no Docker). Local stack: `cp .env.example .env && docker compose up -d`, then `mvn -pl platform-app -am spring-boot:run -Dspring-boot.run.profiles=local` (`DB_PASSWORD` must match `POSTGRES_PASSWORD`). Mailpit UI http://localhost:8025. Integration tests: classes named `*IT`, run with `-Pintegration` (Docker needed).
- Test profile `test` (`platform-app/src/test/resources/application-test.yml`) excludes DataSource/JPA/Flyway (and Modulith JPA event publication) auto-configs so the context test needs no DB. Tests that need a database must NOT use it: use Testcontainers `*IT`.
- Hibernate `ddl-auto: validate`: Flyway migrations must match entities. Spring Security has no config yet (generated password).

## Wave 1a (W1-03, W1-04)

- `contracts/pom.xml`: `build-helper-maven-plugin` 3.6.0 adds `enums/`, `events/`, `errors/` as source roots (no more `sourceDirectory` hack). `platform-app` gets `spring-modulith-starter-jpa`; the test profile also excludes `JpaEventPublicationAutoConfiguration`, `ArchivingAutoConfiguration`, `EventPublicationAutoConfiguration` (class names verified in the 2.1.1 jars).
- `frontend/`: Vite 8, React 19, TS strict, Mantine 9, React Router 7, i18next (TR default, EN), Vitest 5. TypeScript is pinned to 5.9.3 because openapi-typescript 7 does not accept TS 7. All versions exact; `package-lock.json` committed. No ESLint yet (CI has no lint step).
- Scripts: `npm run dev` (proxy `/api/v1` to the mock on 4010 with the prefix stripped, or `API_TARGET=http://localhost:8080 npm run dev` for the real backend), `gen:api` (writes `src/api/schema.d.ts`, gitignored; also runs on `npm install`/`typecheck`/`build`), `typecheck`, `test`, `build`, `mock` (Prism 5 on :4010; needs `-b JSESSIONID=x` cookie).
- Theme: `src/theme/tokens.ts` turns `contracts/design-tokens.json` into CSS variables (`--color-*`, `--tone-<tone>-bg/fg/dot`, `--space-*`, `--radius-*`, `--badge-*`) plus `.tone-<tone>` classes, light/dark; `theme.ts` is the Mantine theme. Use `toneOf(tone)` (unknown falls back to NEUTRAL).
- API: `import { api } from '../api'` (openapi-fetch, `credentials: include`, sends `X-XSRF-TOKEN` from the `XSRF-TOKEN` cookie on non-GET). Routes: placeholders in `src/routes.tsx` for every screen path; replace the `element`, keep the path.
- Empty owned folders with `.gitkeep`: `src/{shell,accounts,settings,pipeline,network}`.
- CI: new jobs `frontend` (npm ci, typecheck, test, build), `mock-smoke` (Prism, curl `/reference-data` expects 200), `docker-image` (builds the Dockerfile).
- `Dockerfile` (repo root): Maven build stage, Temurin 21 JRE runtime, layered jar, non-root uid 10001. `.dockerignore` added.
- `docker-compose.yml`: removed fixed `container_name`s (they collided across checkouts/worktrees and with stale containers).

## Results

`mvn -q -B verify` green. In `frontend/`: `npm ci`, `typecheck`, `test` (2 tests), `build` green. `npm run mock` answered 200 on `/reference-data` and `/accounts`. Docker was up: image built, runs as uid 10001; `docker compose up -d` gave both services healthy, then `down -v`. The GitHub workflow itself has not run (YAML only).

## Change requests / left to do

- Frontend is not yet bundled into the jar (ADR 0003 serving step); do it before the pilot (copy `dist/` into platform-app static resources or add it to the Dockerfile).
- ESLint and Dependabot not set up. The orchestrator can mark W1-03 and W1-04 done after merge.
