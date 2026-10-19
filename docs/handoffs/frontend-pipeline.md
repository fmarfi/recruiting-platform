# Hand-off: frontend-pipeline · W1-14 · 2026-10-19

**Branch / commits:** `worktree-wave1b-frontend-pipeline` · 2 commits (screens, then tests/handoff)

**Done**
- `frontend/src/pipeline/joborders/list`: job order list (open + mine default, search, status chips, paging).
- `frontend/src/pipeline/joborders/detail` + `board/`: job order page, kanban board (@dnd-kit/core drag plus "Move to" menu), optimistic moves with undo toast, reason dialog with chips for Rejected, rejected lane, status menu (manager-only reopen), add candidate.
- `frontend/src/network/list` + `person/`: network list (filters, consent badge), new person, person page with Overview / Connections / Clients and jobs / Activity / Privacy tabs, add connection, add to job, send consent.
- `frontend/src/pipeline/quickadd`: Quick add (activity or action, entity pickers, mobile-friendly modal).
- Legal moves and labels come only from /reference-data (`pipeline/shared`).
- i18n in `src/{pipeline,network}/i18n/{tr,en}.json`, registered in `src/i18n/index.ts` via deep-merge `addResourceBundle`.
- `src/routes.tsx`: four placeholder routes replaced.

**Not done / left**
- Not run against the live backend or Prism by hand (no browser session); only unit tests with stubbed fetch.
- Board drag-and-drop and Quick add flows have no component tests (only boardRules and list render).
- Timeline entry on the account page after a move depends on the accounts screens (other agent).

**Others must know**
- frontend-shell: `<QuickAddLauncher />` is mounted in the temporary `<nav>` in `routes.tsx`; mount it in the real shell header/FAB when you replace the nav.
- `common.*` keys (cancel, confirm, save, retry, refresh, undo) are in `pipeline/i18n`; deep-merge makes duplicates harmless.
- Job order list H1 uses `pipeline.list.title` ("Pozisyonlar"), not the global `nav.jobOrders`.
- Added `@dnd-kit/core` 6.3.1 to package.json (allowed by wave briefing).

**Change requests** (for files you don't own)
- None.

**Verified with**
- `npx tsc --noEmit` → clean
- `npx vitest run` → 3 files, 6 tests pass
- `npm run build` → built
