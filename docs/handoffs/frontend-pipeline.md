# Hand-off: frontend-pipeline · Wave 2a (Network and job orders on DataGrid) · 2026-10-25

**Branch / commits:** `worktree-wave2a-frontend-pipeline` · see `git log --oneline`

**Done**
- /network on shell/grid DataGrid: column chooser, sort (name, company, last contact), density, phone cards, saved views (ViewsMenu + localViewStore, grid id `network`), CSV of visible columns (grid button = current page; "Export all (CSV)" = up to 1000 rows, visible columns), selection with bulk bar via `bulkActions` (export selected, bulk consent for managers).
- Smart-list presets kept (PresetsBar). Person page, relationship map and snapshot untouched.
- Command-bar params read from the URL on mount and on every change: owner=me, stale=N, type=A,B, consent, city, company, q; plus page (1-based) and sort. State is written back with replace.
- city/company have no API filter: they narrow the loaded page client-side and show as dimmed "(this page only)" chips with a remove button.
- /job-orders on DataGrid: columns, sort (title, status, daysOpen), density, views (`job-orders`), CSV; reads owner (absent = me), status (list), q, page, sort from the URL.
- `network/list/csv.ts` re-exports shell/grid/csv; PersonRows, SavedViewsBar and savedViews.ts removed.
- Old saved views (
etwork.savedViews.v1) are migrated into grid.network.views and the old key is removed (KVKK). The grid is keyed on filters+page, so selection resets on any filter or page change.
- CSV now exports visible columns only (email is hidden by default, so no longer exported). city/company stay page-local until GET /people supports them.
- i18n TR/EN added. Saved views store filters only, no people data (KVKK).
- Tests: 87 total (was 79): URL parsing, client filter, CSV columns, job params.

**Not done / left**
- Old saved views in localStorage key `network.savedViews.v1` are not migrated (new key `grid.network.views`).
- Server-backed ViewStore and bulk-actions endpoint: none exist yet; local store kept, bulk is only what existed (consent request).
- No server-side city/company filter on /people.

**Change requests**
- backend/contracts: add `city` and `company` query filters to GET /people so the chips can become real filters.

**Verified with**
- `npx tsc --noEmit` clean; `npx vitest run` 87 passed; `npx vite build` ok (in frontend/). No browser run.