# Contracts

The shared interfaces every agent builds against. Owner: architect (`design-tokens.json`: ux-designer). Frozen after Gate 0; changes go through the architect as a small PR that names the affected owners.

| File | What it is | Who uses it |
| --- | --- | --- |
| `openapi.yaml` | OpenAPI 3.1 contract: 90 operations on 60 paths for Phase 1 (auth, users, reference data, accounts, contacts, people, network links, import, search, job orders, pipeline, proposals, activities, next actions, timeline, dashboard, My day, consent, privacy notices, retention, data-subject requests, audit). Conventions are in its `info.description` and ADR 0006. `x-owner` = implementing module/agent, `x-roles` = allowed roles. | backends implement it, frontends generate types and use the mock, QA writes contract tests |
| `enums/*.java` | Status lifecycles (`AccountStatus`, `ProposalStatus`, `JobOrderStatus`, `PipelineStage`, `ConsentStatus`, `DataSubjectRequestStatus`) implementing `Lifecycle` with transition table, reason rule, manager-only rule, labels and tone; plain vocabularies (`PersonType`, `ActivityType`, `LinkType`, `Role`, …); `ErrorCode`. Must match `docs/status-lifecycles.md`. | all backend modules, db-engineer (CHECK constraints), `/reference-data` |
| `events/*.java` | Domain event records (`DomainEvent` header + one record per event). | publishers and listeners across modules |
| `errors/*.java` | `ApiException` and `IllegalTransitionException` (with `check(from, to, reason)`). Mapped to RFC 9457 by `platform-app`. | all backend modules |
| `events.md` | Event catalogue: publisher, payload, listeners, delivery rules, main flows. | backend agents, security-privacy |
| `schema.md` | Tables, keys, indexes, module ownership, Envers scope, anonymisation. | db-engineer, backend agents |
| `design-tokens.json` | Colours, spacing, type, one badge style per status tone (ux-designer). | frontends |

All Java files are in package `com.recruiting.platform.contracts` (one package on purpose, ADR 0001) and need only the JDK. The `contracts` Maven module compiles every `*.java` under `contracts/`.

## Validate

```bash
npx -y @redocly/cli@1 lint contracts/openapi.yaml
```

Expected: `Woohoo! Your API description is valid.` with no errors or warnings.

Compile the Java contracts without Maven (JDK 21+; on this machine `"C:\Program Files\Java\jdk-25\bin\javac"`):

```bash
javac -Xlint:all -Werror --release 21 -d /tmp/contracts-classes contracts/enums/*.java contracts/events/*.java contracts/errors/*.java
```

## Run the mock server

```bash
npx -y @stoplight/prism-cli@5 mock -p 4010 contracts/openapi.yaml
```

- Serves every path **at the root**, without `/api/v1`: `http://127.0.0.1:4010/accounts`.
- Every call except `/auth/login` and `/public/consent/{token}` needs a session cookie; any value works: `curl -b "JSESSIONID=mock" http://127.0.0.1:4010/accounts`. Without it Prism answers 401 with the `UNAUTHENTICATED` problem.
- Request bodies and parameters are validated against the contract (invalid enum → 400 problem).
- Force another documented response with `Prefer: code=409` (or `404`, `403`, …): `curl -b JSESSIONID=mock -H "Prefer: code=409" -X PATCH -H "Content-Type: application/json" -d '{"status":"SOURCING"}' http://127.0.0.1:4010/job-orders/0192a5b8-6f1e-7c3a-9b2d-3f4e5a6b7c8d/status`.
- `GET /reference-data` returns the real lifecycles (generated from the enums). Other responses are static examples; add `-d` for random data instead.
- Prism reloads when `openapi.yaml` changes.
- If `npx` fails with `ECOMPROMISED` (several agents sharing the npm cache), set a private cache: `npm_config_cache=/tmp/npmcache npx …`.

Vite dev proxy for the frontends (strips the base path for the mock):

```ts
// vite.config.ts
server: {
  proxy: {
    '/api/v1': {
      target: process.env.API_TARGET ?? 'http://127.0.0.1:4010',
      changeOrigin: true,
      rewrite: (p) => (process.env.API_TARGET ? p : p.replace(/^\/api\/v1/, '')),
    },
  },
},
```

## Changing a contract

1. Open a change request in your hand-off note (field, endpoint, reason, who is affected).
2. The architect updates `openapi.yaml` / enums / events / schema in one PR, re-runs lint, compile and the mock smoke test, and lists affected owners in the PR.
3. Adding is cheap (new optional field, new endpoint); renaming or removing needs every affected owner in the same merge window.
