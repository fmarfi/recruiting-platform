# Hand-off: frontend-shell · Wave 2a (DataGrid, command bar, accounts adoption) · 2026-10-23

**Branch / commits:** `worktree-wave2a-frontend-shell` · see `git log --oneline` (earlier wave notes: git history of this file)

**Done**
- shell/grid: generic `DataGrid<T>` (typed ColumnDef, sort callback + aria-sort, row select + select-all-on-page + `bulkActions` slot, density, sticky header, skeleton/empty/error, phone cards, CSV of visible columns of the current page), column chooser (order+visibility in localStorage `grid.<id>.columns`), `ViewStore` interface + `localViewStore`, `ViewsMenu`, `groupBy` prop as extension point (renders group header rows; no UI yet). Import from `shell/grid` (index.ts).
- shell/command: Ctrl/Cmd+K palette + header button; pure `parseQuery`/`routeFor`; grouped /search results (existing endpoint), quick actions, combobox/listbox keyboard nav. Recent items (KVKK): only kind+id+time in `command.recent.<userId>`, 30-day expiry, names fetched on open.
- accounts/list: grid adoption, column chooser, saved views (local, overwrite confirm, warning if storage fails), CSV export; filters, page and sort live in the URL (replace, so Back and Ctrl+K work while open).
- Tests: 79 total (baseline 24).

**Not done / left**
- Bulk selection is off on accounts (no bulk endpoint yet); CSV exports the current page only (a full export needs a server endpoint).
- Server-backed ViewStore: implement `ViewStore<F>` over /views and swap in `localViewStore(...)` call sites.

**Others must know: URL params written by the command bar (frontend-pipeline to read)**
- `/network`: `owner=me`, `stale=<days>`, `type=<PersonType>[,<PersonType>]` (EXECUTIVE|CLIENT_CONTACT|REFERRER|CANDIDATE), `consent=<ConsentStatus>` (NOT_SENT|SENT|GIVEN|REFUSED|WITHDRAWN), `city=<text>`, `company=<text>`, `q=<free text>`.
- `/job-orders`: `owner=me`, `status=<JobOrderStatus>`, `q=<free text>`.
- `/accounts` (done here): `owner=me|all` (absent = me), `status=<AccountStatus>` (last one wins), `q`, `page` (1-based), `sort`.
- Operator syntax: `owner:me stale:90 type:exec consent:given city:izmir company:"Y Holding" status:active`; routing is in `shell/command/operators.ts` (`routeFor`). Unknown operators stay free text.
- /network has no city/company filter in the API: the bar still sends them, but shows those chips dimmed as 
ot applied yet. Repeated owner/stale/status keep the last value.

**Change requests** (for files you don't own)
- frontend-pipeline · network/list, joborders/list · read the params above on mount and on query change.
- frontend-pipeline · network/list/csv.ts · may re-export `shell/grid/csv.ts` (identical helper) to avoid duplicates.

**Verified with**
- `npx tsc --noEmit` clean; `npx vitest run` 79 passed; `npx vite build` ok (in frontend/).

