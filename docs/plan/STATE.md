# State of the work (2026-10-29)

Resume in a NEW Claude Code session started inside this folder (`cd recruiting-platform`, then `claude`) so the named agents in `.claude/agents/` load. First message: "Read CLAUDE.md, docs/plan/STATE.md and docs/plan/work-plan.md, then continue." Run commands: root `README.md`.

## Where we are
Branch `feature/network-customer-tools` (local; nothing on origin yet) holds everything merged. Last verified: frontend `tsc` clean, 236 tests, build ok; backend `mvn -B verify -Pintegration` 151 tests (run before the UI merges; no backend code changed since).

Merged: Wave 2a + 2b (candidate pool, consent engine, off-limits, duplicates and merge, files, bulk-action API, exposure), UX polish, readability pass, Mihenk brand shell (navy rail, brass accent, Source Serif 4 and Inter, tokens v2 copied into `frontend/src/theme`), UX screens pass (page headers, sectioned forms, empty states, undo), F0 shared components in `frontend/src/shell/{edit,bulk,history}` (EditDrawer, InlineEditField, useUndoToast, BulkBar, TagInput, ArchiveMenuItem, HistoryPanel; not yet used by any screen), believable demo seed (CFO search for "Lale Yapi Holding", off-limits, refused consent, exposure).

## In flight (not merged)
- Branch `contract-1-3` in `.worktrees/architect` (head 256255e): OpenAPI 1.3.0 + schema + ADR 0013 (archive/restore, client merge, tag management, longlist/shortlist rank-rating-rationale, evaluation, search brief, fee agreement, placement, conflict check, people search filters). A read-only review is writing `docs/process/handoffs/review-contract-1-3.md`. Do NOT merge before fixing its blockers and before the db migrations exist (MigrationIT compares DB CHECKs with contract enums). Behaviour change: a longlist candidate moving to a client-facing stage needs rating + rationale (>= 20 chars); pipeline tests and seed must follow.
- Leftover partial worktrees `.worktrees/client-record`, `.worktrees/search-assignment` (old tasks, uncommitted): discard and rebuild as E1/E4 on top of F0. `.worktrees/ux2` is an empty leftover folder: delete by hand.

## Next, in order
1. Fix review blockers, then db-engineer migrations on `contract-1-3`, then backends (archive, tags, lists, brief, commercial), merge in order db, backends, security-privacy, frontends.
2. Entity agents, one entity end to end with its screens (plan: `docs/product/brand/app-architecture.md` section g): E1 client, E2 contact, E3 candidate, E4 search, E5 activity/task, E6 proposal, E7 privacy and user admin, QA1 Playwright keyboard flows. Then navigation routes (`/clients`, `/searches`, `/candidates`, `/admin`), command centre, side panel, reports/PDF, BD, insights.
3. Adopt tokens v2 as a contract change (architect).
4. Stage Structure (ADR 0012; decision pending on extracting a `talent` module).

## Requirements and research (read before planning)
`docs/product/requirements-v2/` (88 requirements: PB partner, SX consultant, CO compliance), `docs/product/research/` (market, domain workflows, two critiques), `docs/product/brand/` (direction "Quiet Authority", name Mihenk, no trademark check), handout for an outside planner: `docs/process/handout-plan-and-design.md`. Legal points are all "verify with counsel"; Turkish text in the compliance statement has no diacritics.

## Open decisions for the user
- Push: remote `origin` is `https://github.com/fmarfi/recruiting-platform` (confirmed by the user); this machine has no GitHub login, so the user must run `git push -u origin feature/network-customer-tools` themselves (`! ` prefix in the prompt). History was already rewritten once (no attribution lines, author Asim Furkan Ilhan); new commits since then follow the same rule; `CLAUDE.md` and `.claude/` are git-ignored and removed from pushed history (backup copy on Google Drive; local-only branch `backup-before-rewrite` still has the old history: delete when satisfied). Commits made after the rewrite are not yet rewritten: all use the right author and no trailers.
- Extract `talent` from `accounts`? Fee model, client portal vs share link, retention clock: see `docs/plan/work-plan.md` section 4.

## Local development
`docker compose up -d`; the `local` profile signs in as the demo admin (`app.security.dev-auto-login-as` in `application-local.yml`; delete that line to get the login back; never outside local). Dev UI needs `API_TARGET=http://localhost:8080 npm run dev` (frontend/) to use the real backend. Demo data: `seed/load.ps1`. Code graph: `graphify-out/` (local, git-ignored; rebuild with `C:\Users\SID-DRW\tools\graphify-venv\Scripts\graphify.exe update .`; query with `graphify query "<q>" --graph graphify-out/graph.json --budget 1500`).

## Process rules
Agents work in worktrees (`.worktrees/` now, `.claude/worktrees/` automatic when started inside the repo); remove worktree and branch after merge. Every branch: verify yourself (tsc, vitest, vite build; `mvn -B verify -Pintegration` for backend), code review before merge, then merge. Reports from agents are claims. No attribution lines in commits. `frontend/package-lock.json` is never committed. Stray files not made by agents: `skeletree.md`, `.skeletree-cache.json`, `.gitignore` line `.docs/` (the user's).
