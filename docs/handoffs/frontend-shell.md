# Hand-off: frontend-shell · W1-13 · 2026-10-19

**Branch / commits:** `worktree-wave1b-frontend-shell` · 3 commits (shell+accounts, i18n+settings+routes, tests+hand-off)

**Done**
- shell/: AuthProvider, RequireAuth, /login, layout (sidebar on laptop, bottom tabs on phone, search, + New, TR/EN, user menu), reference-data provider, shared badges/states.
- shell/home: My day (stats, grouped next actions with done/snooze/undo, side cards, empty states).
- shell/search: header typeahead with recent searches, /search page with grouped results.
- accounts/: list (filters, sort, cards on phone), account page (header, status change with reason, dashboard strip, jobs/contacts/proposals tabs, timeline), new account, log activity, add action dialogs.
- settings/: Users (list, role change, enable/disable, invite; admin edits, manager read only), Roles matrix, Status lists (read only). Consultants see "no access".
- i18n: strings in `src/{shell,accounts,settings}/i18n/{tr,en}.json`, merged in `src/i18n/index.ts`. Test checks every used key exists in TR and EN.
- routes.tsx: real shell and screens; job-orders, network, people stay placeholders for frontend-pipeline.

**Not done / left**
- Settings sections Import, Retention, Audit log show "not available" (no spec'd UI built).
- Not run against the real backend; manual click-through on phone width not done.
- Account page: add/end contact and new job/proposal buttons not built (pipeline screens own jobs).

**Others must know**
- frontend-pipeline: add routes inside the `<Route element={<RequireAuth><Shell/></RequireAuth>}>` group in routes.tsx; use `useReference()` for labels and `ToneBadge`/`StatusBadge` from shell/components.
- Providers (Auth, ReferenceData) now live in routes.tsx, not App.tsx.

**Change requests**
- none

**Verified with**
- `npx tsc --noEmit` → clean; `npx vitest run` → 7 passed; `npx vite build` → ok (in frontend/).
