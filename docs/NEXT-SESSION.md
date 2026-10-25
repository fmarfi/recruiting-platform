# Next session: where we stopped (2026-10-23) and what to do

Start the next session with: "Read CLAUDE.md, docs/NEXT-SESSION.md and docs/work-plan-v2.md, then continue."

> 2026-10-25: scope and wave order changed by the Product Plan; **`docs/work-plan-v2.md` supersedes the wave order below and in `docs/mastermind-plan.md`** (the feature ideas there still hold). State on 2026-10-25: ALL Wave 2a work so far (contract 1.1.0, DataGrid, command bar, network and job orders on the grid, tags/saved_view/bulk_action tables) is merged into `feature/network-customer-tools` (767cbff) and verified (tsc, 90 frontend tests, `mvn verify -Pintegration` 70 tests). The `wt-*` worktrees and `worktree-wave2a-*` branches are removed. Docker Desktop and WSL work (`.env` is git-ignored; the local profile expects Postgres on 5433). Next: architect contract 1.2.0 for the candidate pool, then the `/views` and `/bulk-actions` backends with the review change requests in `docs/TASKS.md`.

## Setup facts (this machine)
- Repo: `C:\Users\SID-DRW\Desktop\New folder\recruiting-platform` (branch `feature/network-customer-tools`). No worktrees at the moment (create them by hand when launching agents).
- Tools installed by winget: Git, JDK 21, Node 24, Docker Desktop. Maven 3.9.9 is manual at `C:\Users\SID-DRW\tools\apache-maven-3.9.9`.
- Every fresh PowerShell needs: `$env:Path = [Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')`.
- Git has no global identity; commit with `git -c user.name="asim" -c user.email="ilhanasimfurkan@gmail.com" commit ...`.
- Docker Desktop is installed but has never been started: launch it once before `-Pintegration` tests or the local DB.
- The harness cannot create worktrees itself (session started outside the repo): create them by hand with `git worktree add -b <branch> ..\wt-<name>` and give agents the absolute path.
- `frontend/package-lock.json` shows as modified in every checkout (npm install side effect). Do not commit it.
- Nothing is pushed to GitHub.

## State of the work
| Piece | Branch / place | Status |
| --- | --- | --- |
| Network and client tools (smart lists, CSV, bulk consent, relationship map and snapshot, account Relationships tab) | `feature/network-customer-tools`, commit cc5f751 | Done and verified: tsc clean, 24 tests, vite build ok. Not clicked through in a browser. |
| Mastermind plan | `docs/mastermind-plan.md`, commit 09a0d81 | Approved by the user ("yes go", start with grid and command bar). |
| Contract 1.1.0: saved views + bulk actions, ADR 0010 | `worktree-wave2a-architect`, commits 55ecce8 and 5f706e6 | Committed by the architect; its verification report had NOT been read yet. Re-verify before merging (below). |
| DataGrid, ViewStore, command bar (Ctrl+K), accounts list on grid | `worktree-wave2a-frontend-shell`, commits f87e806 and 0f9ec8d | Verified by the orchestrator: tsc clean, 64 tests pass. Left: bulk selection off on accounts, CSV is current page only, group-by has no UI. |
| Code review of the frontend-shell branch | agent was still running when the session ended | Result not read. If lost, rerun the review (read-only, Opus) on `worktree-wave2a-frontend-shell`. |

Command-bar URL params for the network and job screens (from the frontend-shell hand-off): `/network`: `owner=me, stale=N, type=A,B, consent=X, city, company, q`; `/job-orders`: `owner=me, status, q`; `/accounts` (already reads): `owner=me|all, status, q`.

## Update later on 2026-10-23 (supersedes the table above where they differ)
- Architect branch `worktree-wave2a-architect` (head 8d81d21): VERIFIED by the orchestrator (tsc clean, 24 tests, `mvn -q -B verify` exit 0 without -Pintegration). Ready to merge.
- Code review of frontend-shell: DONE, no blockers, 6 should-fix + nits. The frontend-shell agent was asked to fix them (same worktree). After it reports: re-run tsc, vitest, vite build there yourself, then merge. Review items are in that agent's brief; key ones: Object.hasOwn in operators.ts, repeated `status:` operator, AccountsList filters in the URL, no people names in localStorage recents (KVKK), viewStore setItem failure.
- `db-engineer` was launched on worktree `..\wt-db` (branch `worktree-wave2a-db`, based on the architect branch). Check `git -C ..\wt-db log --oneline -5` and `docs/handoffs/db-engineer.md` there; verify with `mvn -q -B verify` (and `-Pintegration` with Docker running).
- LATER UPDATE: frontend-shell fixes done and verified by the orchestrator (79 tests, tsc clean, build ok). Architect + frontend-shell are MERGED into branch `wave2a-integration` (worktree `..\wt-integration`, head e24ec0a); tsc/79 tests/vite build verified on the merge.
- `frontend-pipeline` was launched on `..\wt-frontend-pipeline` (branch `worktree-wave2a-frontend-pipeline`, based on wave2a-integration): network list and job orders list onto the DataGrid. Not yet reviewed or verified.
- The main checkout (`recruiting-platform`, branch `feature/network-customer-tools`) does NOT contain Wave 2a yet: continue from `wave2a-integration`.
- Not yet started: backend-accounts (/views), backend-pipeline + security-privacy (/bulk-actions). They follow db-engineer's merge (rebase their worktrees on wave2a-integration + db branch).

## Steps for tomorrow (in order)
1. **Check the agents.** `git -C ..\wt-architect log --oneline -3`; read `docs/handoffs/architect.md` in `wt-architect` and the review result for frontend-shell (or rerun it).
2. **Verify the architect branch** in `..\wt-architect`: `cd frontend; npm install; npm run gen:api; npm run typecheck; npm test`, then `mvn -q -B verify` at the repo root (Docker running for `-Pintegration`), and a Prism smoke on `/views` and `/bulk-actions`. Send it to code-reviewer.
3. **Merge in the repo order** into a fresh integration branch from `feature/network-customer-tools`: architect (contracts) first, then frontend-shell. Fix review blockers via the author. Resolve any overlap in `frontend/src/shell/i18n/*.json` and regenerate the API client.
4. **Wave 2a, part 2: launch in ONE message, each in its own hand-made worktree**, briefed from `docs/context/brief.md` and the architect's change requests:
   - `db-engineer` (Sonnet): Flyway migration + entity + repository + IT for `saved_view`.
   - `backend-accounts` (Sonnet): `/views` endpoints; accept the command-bar operators in people/account list queries if the contract needs it.
   - `backend-pipeline` + `security-privacy` (Sonnet): `/bulk-actions`: status changes through the status machine, consent gate with explicit reason codes, one audit entry per record.
   - `frontend-pipeline` (Sonnet): move the Network list onto the shared DataGrid (my version in `network/list/` is the reference for presets, saved views, CSV export, bulk consent), read the command-bar URL params, adopt the server-backed ViewStore and bulk-action bar once endpoints exist.
   - Merge order: db-engineer, backend agents, security-privacy, frontends. Green build and tests or no merge.
5. **Then the rest of Wave 2a:** activity composer and timeline on every record, path component (stage guide), dashboards v1 on existing data, CSV for every grid (all pages). Update `docs/TASKS.md` (orchestrator only) with a Wave 2a section and gate.
6. **Run the app end to end** (Docker up, `mvn -q install -DskipTests`, `spring-boot:run -Dspring-boot.run.profiles=local`, `npm run dev`) and click through the new screens; none of the new UI has been seen in a browser yet. Fix what looks off.
7. **Wave 2b and 2c** per `docs/mastermind-plan.md` section 5 (reports with fees, calendar, e-mail, files, duplicates, then relationship strength, warm-path finder, org chart, alumni, "we have been here before", compliance cockpit).

## Open decisions (ask the user if still unanswered)
- Who may create shared saved views: any consultant or managers only (contract assumes MANAGER/ADMIN).
- Fees on job orders: fixed, percent of salary, or both.
- Whether contract changes for Wave 2b/2c (path finder, org chart, reports, files) go through the architect now.
- "No competitor does this" claims are unverified; check Bullhorn, Vincere, Invenias before using them externally.

## Rules to keep
Synthetic data only; never the pilot client's name. Light corporate enterprise UI, TR and EN for every label. Contracts change only through the architect. Every branch reviewed before merge. Reports from agents are claims: verify in the worktree.
