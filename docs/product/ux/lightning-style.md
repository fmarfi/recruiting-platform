# Lightning-style layout and components

Owner: ux-designer. Extends `README.md` and `screens/*.md`; tokens from `contracts/design-tokens.json` v1.1.0 (no new colours; any new token goes through architect CR). Goal: the interaction patterns of a modern CRM workspace (header + object tabs, list views, record pages with highlights, Path, timeline), in our own navy identity, content and flows unchanged. No third-party logos, icons, names or brand colours. Research basis: public Lightning docs/Trailhead (compact-layout highlights of about 7 fields, fewer on small screens; Related tab; Activity tab timeline). Not verified: Path details, taken from general knowledge of the pattern.

## 1. Layout system

- **Global header (48 px, navy):** logo/app name left; centre global search (existing type-ahead, `/` focuses, max 560 px); right: **+ New** quick-create menu (Client, Person, Job order, Action, Log activity), notifications placeholder, EN/TR, user menu. Replaces the current top bar; same behaviours.
- **Object tab bar (40 px, under header):** My day, Accounts, Job orders, Network, (Settings for Manager/Admin) as horizontal tabs with active underline in accent. Opened records appear as closable sub-tabs (max 5, then overflow "More"). The left nav goes away on laptop; phone keeps bottom tab bar plus **+**.
- **Page header (record and list pages):** breadcrumb line, object icon tile (our own glyph set), title, status Path or badge, action buttons right (max 3 visible, rest in overflow menu), then the highlights panel.
- **Page body grid:** 12 columns, max width 1440, gutter 16, outer padding 16 (phone) / 24 (laptop). Record page: main column 8 cols (tabs: Details, Related, Activity), right column 4 cols of stacked cards. Below 960 px the right column drops under the main column; below 720 px one column.
- **Spacing and density:** 4 px base, card padding 16, card gap 12, radius from tokens. Two densities: Comfortable (row 48) default, Compact (row 36) per-user toggle in list toolbar, stored in browser. Cards: white surface, 1 px border, no heavy shadow; header 44 px with title left, action right.
- **Feedback:** toasts bottom-left (Undo where reversible, 5 s), inline field errors, skeleton loaders on cards, no blocking spinners.
- **Utility bar (laptop only, 36 px footer):** buttons for Recent items, Quick note, Next actions; opens a 320 px docked panel. Phase 1 optional; do after P1-P3.
- **Empty states:** icon, one sentence, one primary button (UX-08), inside the same card or table area.
- Keyboard: `/` search, `g` then a/j/n/h for tab jump, `n` quick-create, Esc closes panels.

## 2. Component specs

**Path (status chevrons).** Horizontal row of chevron steps for account status (Lead, Proposal sent, Active, On hold, Closed), application stage (Sourced to Placed; Rejected shown as a separate red end step), job status. States: done (filled navy, check), current (accent fill, bold), upcoming (neutral outline), blocked/terminal (tone colour). Click a step = same transition as the status badge menu (reason dialog when `reasonRequired`, disabled with tooltip when `managerOnly`). Data from `lifecycles.<kind>`; label by locale. Never colour alone: check icon and text. Phone: collapses to current step plus "Step 3 of 6" and a select. Optional guidance strip under the Path (collapsible): key fields to fill for the stage (static copy, no backend).

**Highlights panel.** Card directly under the page header: 4 to 7 label-over-value pairs, wraps to 2 columns on phone (fewer fields, 4). Account: Sector, City, Owner, Open jobs, Last activity, Next action. Job: Client, Location, Seniority, Fee terms, Owner, Days open. Person: Type, Company/Title, City, Owner, Last contact, Consent. Values link to records; inline edit by pencil on hover for owner and text fields. Actions in the header, not the panel.

**Related-list cards.** Card with icon, title, count "(3)", **New** button, the first 5 rows (2 to 3 key columns, status badge), footer "View all" opening the full list in the Related tab; row menu (Edit, Remove). Used for Contacts, Job orders, Proposals (account); Applications, Connections (person); Candidates (job). Empty = empty-state line plus New.

**Activity timeline + composer.** Activity tab and right-column card. Composer on top with segmented actions: **Log a call**, **New task** (next action), **Note**, **Email** (log only, no sending in Phase 1). Each opens an inline form (subject, date, link to person/job, body) with Save/Cancel; target under 30 s (UX-06). Below: filter chips (All, Calls, Meetings, Notes, Status changes, Stages) and "Upcoming and overdue" group first, then "Past" grouped by month; each item = icon, title, who, relative date with tooltip, expandable detail; events link to records. Collapsed system events (status moves) in a lighter style.

**List-view toolbar.** Row above the table: list-view selector (dropdown of "All clients", "My clients", "Recently viewed", saved views, pin icon sets default), then search-in-list, filter button (opens right filter panel with chips of active filters and Clear), column chooser (checkbox list, drag order, persists per user and list), density toggle, view toggle (Table, Kanban where the object has stages), refresh. Header shows "42 items - sorted by Name - filtered by Owner" line. Rows: checkbox column, sortable headers, sticky header, row menu. Mass actions bar replaces the toolbar when rows are selected: Change owner, Change status, Export CSV (Delete only for manager). Saved views are per user and stored in the browser in Phase 1 (server-side sharing is a later backend task).

**Dashboard tiles.** My day top strip: KPI tiles (value, label, delta or sublabel, click opens the filtered list), 4 per row on laptop, 2 on phone. Below: report cards (e.g. Pipeline by stage as horizontal bars, Jobs by age, Placements per month) each with title, small chart, "View report" link. Chart colours from tone map, labelled, values visible (no tooltip-only data). Use existing `GET /home` and `GET /accounts/{id}` summary data where present.

## 3. Per-screen changes

1. **My day:** tiles + report cards as above; Next actions as a task list card with inline Done/Snooze and a composer shortcut; Open jobs and Recent items as related-list-style cards in the right column; greeting becomes page header.
2. **Accounts (list + page):** list view with selector, filter panel, column chooser, mass owner/status change; status chips become the Path on the page. Page: header with Path, highlights panel, tabs Details / Related (Contacts, Jobs, Proposals) / Activity, right column cards (Next actions, Activity timeline short, Open jobs summary). Dashboard strip (A-09) becomes the highlights panel.
3. **Job order + pipeline:** Path in header for job status; highlights panel; Table/Kanban toggle on the Candidates card (Kanban = existing board, drag to move, Rejected lane); stage Path per candidate in a side drawer opened by clicking a card; right column: Client card, Activity.
4. **Network + Person:** list view as accounts, type chips become list views (Executives, Client contacts, Referrers, Candidates). Person page: highlights panel, tabs Details / Related (Connections, Applications, Career) / Activity / Privacy; consent shown as a Path-like 3-step strip (Not sent, Sent, Given).
5. **Search:** results page becomes grouped related-list-style cards per object with "View all"; filters in the same filter panel; type-ahead unchanged but result rows get object icon and status badge.
6. **Settings:** vertical nav kept, in a card; users and status lists use the same list toolbar (search, columns); Audit log gets filter panel; no Path.

## 4. Task list (prioritised, small, independent)

Backend flag: **[BE]** = needs a new endpoint (not designed here); otherwise uses existing contracts.

frontend-shell
- S1. Global header + object tab bar replacing left nav; quick-create menu; phone bottom bar unchanged. No BE.
- S2. Card, page header, breadcrumb, highlights panel components and 12-col grid, densities, toasts (shared primitives used by all pages). No BE.
- S3. List-view toolbar: view selector, filter panel, column chooser, density, with browser-stored saved views; apply to Accounts and Network lists. No BE. Server-side shared views would be **[BE]**, later.
- S4. Dashboard tiles + report cards on My day. Basic tiles no BE; "Placements per month" and "Pipeline by stage across all jobs" report cards **[BE]** (aggregate endpoint).
- S5. Utility bar with Recent items and Quick note (recent items kept in browser). No BE; server-side recents **[BE]**, optional.

frontend-pipeline
- P1. Path component bound to `lifecycles` reference data, used on Account and Job headers (replaces status badge menus there). No BE.
- P2. Record page template: tabs Details / Related / Activity, right-column cards, related-list card component; apply to Account page. No BE.
- P3. Activity timeline + composer (Log a call, New task, Note, Email-log, filters, upcoming group). Uses existing activity and next-action endpoints; filtering by type and month grouping server-side would be **[BE]** if the current feed lacks a `type` filter (verify).
- P4. Apply record template to Job page and Person page; Table/Kanban toggle on candidates; candidate drawer with stage Path. No BE.
- P5. Mass actions on lists (change owner, change status, export CSV). Bulk update and export endpoints **[BE]**; without them, loop over single updates for at most 25 rows.

Order: S2, S1, P1, P2, S3, P3, P4, S4, P5, S5. S2 must land before P2 and P4; everything else is independent.
