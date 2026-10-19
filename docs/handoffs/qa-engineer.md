# Hand-off: qa-engineer · W1-16 · 2026-10-19

**Branch / commits:** `worktree-wave1b-qa-engineer` · 4 commits (incl. this note)

**Done**
- `platform-app/src/test/.../contract/ContractAssertions.java`: `matchesContract()` ResultMatcher validating MockMvc responses against `contracts/openapi.yaml` (swagger-request-validator-mockmvc 2.46.0, test scope in platform-app/pom.xml).
- `ContractHarnessTest`: proves the harness on a stub (conforming passes, non-conforming fails).
- `tests/e2e/`: Playwright 1.64.1 setup (starts Vite on 5176 unless E2E_NO_SERVER=1), `support/auth.ts` login helper, `navigation.spec.ts` (6 passing smoke tests against the scaffold UI, TR default language, href locators).
- `login.spec.ts`, `must-stories.spec.ts`: happy paths for login, UX-01, A-01, A-02, A-07, A-12, A-14, UX-06 written as `test.fixme` with the blocker named in each.
- `docs/test-plans/must-stories.md`: criteria, layers and E2E status for each Must story.

**Not done / left**
- Contract assertions on real controllers: none exist yet. Backend agents add `.andExpect(ContractAssertions.matchesContract())` in their MockMvc tests.
- fixme specs: remove `fixme` as login, screens and seed land; selectors are guesses from UX specs and need adjusting.
- Smoke against the Prism mock: the mock has no auth, so only scaffold navigation is covered; full-stack run needs Postgres + Spring (port 8084) + seed.
- CI wiring of Playwright (ci.yml is devops-owned).

**Others must know**
- backend agents: use `ContractAssertions.matchesContract()`; the spec's `servers` prefix `/api/v1` is required in test paths.
- frontend agents: keep link hrefs and `<h1>` per screen, `html[lang]` updates on language switch; tests rely on them. Give board columns `region` roles with the column name, cards `listitem`.

**Change requests**
- devops · .github/workflows/ci.yml · add a job: `cd tests/e2e && npm ci && npx playwright install --with-deps chromium && npx playwright test`.
- synthetic-data · seed · include a person named Öztürk and at least one account in LEAD with a job order that has candidates in Sourced.

**Verified with**
- `mvn -B -q -pl platform-app test -Dtest=ContractHarnessTest` → pass
- `cd tests/e2e && npx playwright test specs/navigation.spec.ts` → 6 passed
