# Hand-off: frontend-shell · Wave 2b (server-backed views, off-limits admin, retention, operators) · 2026-10-26

**Branch / commits:** `worktree-wave2b-frontend-shell` · see `git log --oneline` (earlier wave notes: git history of this file)

**Done**
- ViewStore over /views: `shell/grid/apiViewStore.ts` (list/save/rename/delete, versions kept for optimistic lock, SHARED only if `canShare`), `viewErrors.ts` (409 DUPLICATE, 400 LIMIT, 409 VERSION_CONFLICT to CONFLICT, 403, 404, 5xx/network to UNAVAILABLE), `viewStoreFactory.ts` `createViewStore()` (API first, `localViewStore` fallback when unavailable, one-time migration of local views as PRIVATE with a notice, flag `grid.<id>.views.migrated`), hook `useViewStore(options)` (adds user id and `canShare = isManager`). All exported from `shell/grid`.
- ViewsMenu: shared badge, rename, share checkbox (managers), notice line (fallback/migrated/storage), error messages in TR/EN, warning that free-text `q` is not stored in shared views. Hidden edit/delete when `editable === false`.
- Accounts list now uses it (`VIEW_OPTIONS` in `accounts/list/accountFilters.ts`: `q`, `status`, `ownerId=me`).
- Settings: new section `/settings/offlimits` (list active/all, create for account or company with reason, note, start and optional end date, End today, delete with confirm; 409 DUPLICATE message). Retention: basis select, auto-send on CV upload, renewal reminder days (max 5, 1..365); read-only for non-admins.
- Command bar: `tag:`, `source:`, `cv:` operators with tests (112 tests total, baseline 90).
- Account page shows off-limits rules in force (`OffLimitsBanner`, `GET /off-limits?accountId&active=true`).

**Not done / left**
- Mock/backends for /off-limits and retention fields were not run against a live server (Prism not started; typed against the contract).
- NetworkList and JobOrdersList still use `localViewStore`; frontend-pipeline adopts `useViewStore` (see below).

**Others must know (frontend-pipeline)**
- Adopt: `const { store, notice } = useViewStore<MyFilters>({ gridId, entity: 'PEOPLE'|'JOB_ORDERS', toFilters, fromFilters, hasFreeText })`, pass `store` and `notice` to `ViewsMenu`. Allowed server filter keys per entity are in `ViewDefinition`; `columns` must be list-item property names.
- URL params from the command bar to `/network`: `tag=<t1>,<t2>` (lower-case, OR), `sourceChannel=<SourceChannel>[,<..>]`, `cv=<text>` (a multi-word phrase arrives quoted, e.g. `"project manager"`), besides the existing owner, stale, type, consent, city, company, q.

**Change requests**
- architect: the brief asked for `cv:yes|no`, but contract `cv` on `GET /people` is a full-text query over the CV text, with no "has CV" filter. I implemented `cv:<text>` (min 2 chars) to `cv=`; `cv:yes|no` stays free text. Add a boolean `hasCv` if yes/no is wanted.
- architect: `SavedView`/`ViewDefinition` has no free-text-safe marker; fine, but `columns` validation of unknown names returns 400 so account column ids must stay equal to `Account` property names (they do today).
- frontend-pipeline: network/list read `tag`, `sourceChannel`, `cv` on mount; NetworkList to adopt `useViewStore`.

**Verified with**
- `npx tsc --noEmit` clean; `npx vitest run` 112 passed; `npx vite build` ok (in frontend/).
