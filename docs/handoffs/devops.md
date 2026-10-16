# Hand-off: devops (Wave 0, tasks W0-09, W0-10)

## What changed

- Root `pom.xml`: parent `spring-boot-starter-parent:4.1.1`, `com.recruiting:recruiting-platform:0.1.0-SNAPSHOT`, Java 21, Spring Modulith BOM `2.1.1` (latest GA on Maven Central at the time; verify it still pairs with Boot 4.1.x when upgrading). Internal modules are version-managed in `dependencyManagement`.
- Modules (build order): `contracts`, `security`, `privacy`, `crm-accounts`, `crm-pipeline`, `ai`, `platform-app`.
  - `contracts`: `sourceDirectory` = `contracts/enums`. It is empty today; the build passes with no sources (empty jar). Architect's enums (package `com.recruiting.platform.contracts`) compile as-is once added. No `package-info.java` was needed.
  - Feature modules: depend on `contracts` and `spring-modulith-api`; each has only a `package-info.java` with `@ApplicationModule`. Packages: `com.recruiting.platform.{security,privacy,accounts,pipeline,ai}`. The `.gitkeep` files were removed.
  - `platform-app`: `PlatformApplication`, starters for webmvc, data-jpa, flyway (+ flyway-database-postgresql), postgresql driver, validation, security, mail, actuator, Envers, Modulith core; test deps Modulith test, Testcontainers (`testcontainers-junit-jupiter`, `testcontainers-postgresql`, `spring-boot-testcontainers`).
- Tests in `platform-app`: `ModularityTests` (`ApplicationModules.of(PlatformApplication.class).verify()`), `PlatformApplicationTests` (context loads).
- `docker-compose.yml`: `pgvector/pgvector:0.8.7-pg17` and `axllent/mailpit:v1.31.6`, named volumes, healthchecks. Image tags confirmed to exist on Docker Hub.
- `.github/workflows/ci.yml`: jobs `build` (JDK 21 Temurin, Maven cache, `mvn -B verify`), `openapi-lint` (`npx -y @redocly/cli@1 lint contracts/openapi.yaml` if the file exists), `testcontainers` (`mvn -B verify -Pintegration`, runs `*IT` classes via failsafe on a runner with Docker).
- Config: `platform-app/src/main/resources/application.yml`, `application-local.yml`, `application-pilot.yml`; `.env.example`; `.gitignore` extended (`.env.*`, keeps `.env.example`).
- `docs/hosting.md` (W0-10): options, recommendation (Türkiye hosting for real data), open questions; unverified items marked.

## How to run

- Build and test (no Docker needed): `mvn -B verify` from the repo root.
- Local stack: `cp .env.example .env`, `docker compose up -d`, then
  `mvn -pl platform-app -am spring-boot:run -Dspring-boot.run.profiles=local` (needs `DB_PASSWORD` matching `POSTGRES_PASSWORD`; local profile defaults it to `recruiting`, so set `POSTGRES_PASSWORD=recruiting` or export `DB_PASSWORD`). Mailpit UI at http://localhost:8025.
- Integration tests with Testcontainers: name classes `*IT` and run `mvn -B verify -Pintegration` (needs Docker).

## Test profile without a database

`platform-app/src/test/resources/application-test.yml` excludes DataSource, transaction manager, Hibernate JPA, Spring Data JPA repositories and Flyway auto-configurations (by their Boot 4 class names). `PlatformApplicationTests` uses `@ActiveProfiles("test")`. Rule for builders: tests that need a database must NOT use the `test` profile; use Testcontainers (`@ServiceConnection` PostgreSQL with the pgvector image) and name them `*IT`. Once repositories/entities exist, plain `@SpringBootTest` tests without the profile need Docker.

## Result

`mvn -B verify` from root: BUILD SUCCESS, 2 tests (ModularityTests, PlatformApplicationTests) pass; also green with `-Pintegration` (no ITs yet). Not run: `docker compose` (Docker not installed; only YAML syntax validated with yaml-lint) and the GitHub workflow (cannot run locally; YAML syntax validated only). The local run used JDK 25; CI uses JDK 21 (`java.version` 21 is enforced as the release target, but the compile on 21 is untested here).

## What others must know

- Architect: put enums in `contracts/enums/*.java`, package `com.recruiting.platform.contracts`. `contracts` has only Spring-free sources; do not import Spring there unless a dependency is added to `contracts/pom.xml` (ask devops). Feature modules must reference other modules only through their public packages or events (Modulith verify will fail otherwise). Module folders map to packages as in the task brief; sub-packages like `...accounts.internal` are internal by Modulith convention.
- Builders: each module's pom has only `contracts` and `spring-modulith-api`. Add dependencies (e.g. spring-data-jpa, security) in your module pom when you need them; the app module already has the starters at runtime. Dependencies between modules are declared in the pom AND checked by Modulith.
- `platform-app` has Spring Security on the classpath: with no config the app generates a password and locks endpoints (the security agent owns the real config).
- Hibernate `ddl-auto: validate` is set: Flyway migrations (db-engineer) must match entities.
- Mockito/ByteBuddy print dynamic-agent warnings on JDK 25; harmless.

## Change requests / left to do

- TASKS.md: W0-09 and W0-10 can be marked done by the orchestrator after merge. Gate 0 item "CI green on the skeleton" can only be confirmed once the workflow runs on GitHub.
- Not done: Dockerfile / image build for the app (not requested; add before the pilot), frontend build in CI (frontend folder owner), mock server job (architect), Checkstyle/Spotless (not agreed), Dependabot.
- Pin GitHub Actions by SHA before the pilot if the firm requires it.
