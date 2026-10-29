# Hand-off: frontend-shell 路 Wave 2b (server-backed views, off-limits admin, retention, operators) 路 2026-10-26

**Branch / commits:** `worktree-wave2b-frontend-shell` 路 see `git log --oneline` (earlier wave notes: git history of this file)

**Done**
- ViewStore over /views: `shell/grid/apiViewStore.ts` (list/save/rename/delete, versions kept for optimistic lock, SHARED only if `canShare`), `viewErrors.ts` (409 DUPLICATE, 400 LIMIT, 409 VERSION_CONFLICT to CONFLICT, 403, 404, 5xx/network to UNAVAILABLE), `viewStoreFactory.ts` `createViewStore()` (API first, `localViewStore` fallback when unavailable, one-time migration of local views as PRIVATE with a notice (views the server rejects are dropped and counted in the notice, the rest is written back and retried; flag cleared on fallback; concurrent list() shares one run), flag `grid.<id>.views.migrated`), hook `useViewStore(options)` (adds user id and `canShare = isManager`). All exported from `shell/grid`.
- ViewsMenu: shared badge, rename, share checkbox (managers), notice line (fallback/migrated/storage), error messages in TR/EN, warning that free-text `q` is not stored in shared views. Hidden edit/delete when `editable === false`.
- Accounts list now uses it (`VIEW_OPTIONS` in `accounts/list/accountFilters.ts`: `q`, `status`, `ownerId=me`).
- Settings: new section `/settings/offlimits` (list active/all, create for account or company with reason, note, start and optional end date, End now (yesterday, or its start day if later; shows Ends today), delete with confirm; 409 DUPLICATE message). Retention: basis select, auto-send on CV upload, renewal reminder days (max 5, 1..365); read-only for non-admins.
- Command bar: `tag:`, `source:`, `cv:` operators with tests (125 tests total, baseline 90).
- Account page shows off-limits rules in force (`OffLimitsBanner`, `GET /off-limits?accountId&active=true`).

**Not done / left**
- Mock/backends for /off-limits and retention fields were not run against a live server (Prism not started; typed against the contract).
- NetworkList and JobOrdersList still use `localViewStore`; frontend-pipeline adopts `useViewStore` (see below).

**Others must know (frontend-pipeline)**
- Adopt: `const { store, notice } = useViewStore<MyFilters>({ gridId, entity: 'PEOPLE'|'JOB_ORDERS', toFilters, fromFilters, hasFreeText })`, pass `store` and `notice` to `ViewsMenu`. Allowed server filter keys per entity are in `ViewDefinition`; `columns` must be list-item property names.
- URL params from the command bar to `/network`: `tag=<t1>,<t2>` (lower-case, OR), `sourceChannel=<SourceChannel>[,<..>]`, `cv=<text>` (a multi-word phrase arrives quoted, e.g. `"project manager"`), besides the existing owner, stale, type, consent, city, company, q.

**Change requests**
- architect: the brief asked for `cv:yes|no`, but contract `cv` on `GET /people` is a full-text query over the CV text, with no "has CV" filter. I implemented `cv:<text>` (min 2 chars) to `cv=`; `cv:yes|no` stays free text. Repeated `cv:` join with a space (AND); user quotes are kept as typed. Add a boolean `hasCv` if yes/no is wanted.
- architect: `SavedView`/`ViewDefinition` has no free-text-safe marker; fine, but `columns` validation of unknown names returns 400 so account column ids must stay equal to `Account` property names (they do today).
- frontend-pipeline: network/list read `tag`, `sourceChannel`, `cv` on mount; NetworkList to adopt `useViewStore`.

**Verified with**
- `npx tsc --noEmit` clean; `npx vitest run` 125 passed; `npx vite build` ok (in frontend/).

---

# Hand-off: frontend-shell/ux 路 demo polish 路 2026-10-28

**Before / after**
- Accounts, Job orders lists: 400 error screen (backend splits `sort=name,desc`) -> central `sortFix` middleware in `api/client.ts`; job `daysOpen` sort mapped to `openedOn`.
- Admin demo login saw empty "Mine" lists -> admins default to all records (`owner=me` selects mine).
- My day: "Open job orders (0)" next to 8 open jobs -> card shows the firm's oldest open jobs with "View all"; KPI tiles with accent, recent clients with links.
- Job page header and account dashboard strip -> shared `Highlights` panel (label over value); empty pipeline button no longer full width.
- Network filters grouped in one labelled card; account timeline as a rail with a heading.
- Phone: command button hidden, avatar user menu, bottom bar text fits.
- A11y: skip link, `main` target, pagination buttons named (TR/EN), search combobox role (axe: aria-allowed-attr), AA-safe green/yellow/blue text shades, visible focus on nav, EmptyState heading order, language switch label translated. axe-core added as devDependency (run via the dev server in the browser; clean on My day, Accounts, Network after the fixes except what is listed below).

**Still rough:** not visually re-checked at 500 px after the last commit (browser extension disconnected); Network filter card is long on phone; Person page says "Candidate profile" for any type; no axe test in vitest.

**Change requests (backend/architect)**
- Paging.of: a single `sort=name,desc` is split by Spring into two keys, 400 "unsupported sort key: desc". Accept the comma form as one value.
- `GET /job-orders` rejects `sort=daysOpen` although the contract lists it.
- `GET /home` returns `openJobOrders: []` for users who own no jobs while `stats.openJobOrders` is 8.
- `GET /search?q=R眉zg芒rg眉l眉` returned 500 via curl (works from the browser with `R眉zg芒r`): check long accented terms.
- Search for "energy" (sector text) finds nothing; only names match.

## T1 brand shell (branch ux-brand-shell)
- Mihenk brand applied: `frontend/src/theme/design-tokens-v2.json` is a copy of docs/product/brand/design-tokens-v2.json; tokens.ts/theme.ts now read it. contracts/design-tokens.json is untouched.
- **Change request (architect):** adopt tokens v2 as the contract (replace contracts/design-tokens.json v1.1 and point vite fs.allow/tokens import back to it), then delete the frontend copy.
- Fonts: Source Serif 4 (h1, KPI) and Inter via Google Fonts in index.html with Georgia/system fallbacks.
- Shell: midnight 232px rail (collapses to 64px, remembered in localStorage), 56px midnight header, inline Logo component (accessible name "Mihenk"), brass "+ New" as the single primary header action, bottom bar on phone. Nav labels Home/Clients/Searches/Candidates/Admin (TR: Ana sayfa/M黶teriler/Aramalar/Adaylar/Y鰊etim); routes unchanged.
- src/theme/contrast.test.ts checks the token pairs (WCAG AA).
- Still off: screen titles still say "Job orders", "Network" tabs etc. (other agent's scope); Admin page title "Settings"; phone widths not re-checked.
