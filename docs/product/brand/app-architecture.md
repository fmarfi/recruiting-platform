# App architecture: the command centre of an executive-search firm

Owner: brand and product design and information architecture · Date: 2026-10-29 · Status: PROPOSAL · Inputs: `docs/plan/work-plan.md`, `docs/product/research/critique-consultant-review.md`, `critique-functional-gaps.md`, `docs/product/ux/lightning-style.md`, `frontend/src/routes.tsx`, `contracts/openapi.yaml`. Brand: `brand-direction.md` (product name **Mihenk**, direction "Quiet Authority"); tokens: `design-tokens-v2.json`.

**Principle.** One place for everything a partner or consultant does. The app has seven areas, one record pattern, one side panel, one command bar and one way to edit, undo, tag, bulk-edit, import and export, the same everywhere. The app speaks search-firm language (Client, Search, Longlist, Shortlist, Candidates, Mandate) and leads with money, time and risk.

Status legend used below: **Exists** (on screen today), **Partly** (on screen but thin, or API only), **Planned** (wave in brackets: 2b, 2c, 2d, 2e, Later).

---

## (a) Command centre (Home)

Route `/`. It replaces "My day". The layout is a 12-column grid: a priority column (8) and a risk-and-money column (4). It loads in under 300 ms using skeletons, one aggregate call (`GET /home`, extended **[BE]**) and no blocking spinner. Role decides the default view; a toggle ("My view / Firm view") lets a partner see a consultant's view and back.

### Partner sees first
1. **Greeting line** (serif display): "Good morning, Deniz. 3 things need you today."
2. **Fee-weighted pipeline** (the only brass element on the page): the sum of the estimated fee × stage probability across open opportunities and live searches, versus the quarter target, with this quarter's billed and outstanding retainer instalments. A click opens Insights › Pipeline. *Planned 2c/2d (needs opportunity and fee agreement).*
3. **Searches needing action:** a ranked list of live searches with a reason chip: "Shortlist due in 2 days", "No activity for 14 days", "Client feedback pending 5 days", "Offer stage". Columns: Search, Client, Lead consultant, Stage, Days open, Next milestone. *Partly (open job orders with days open exist).*
4. **Client health:** clients with no contact in 60+ days, clients with open proposals, and clients whose search is stalled. Each has a "Log call" action inline. *Planned 2c.*
5. **Risk alerts:** consent refused or withdrawn on a candidate in a live pipeline, consent requests unanswered for 7+ days, retention ending in 14 days, off-limits override requests waiting for approval, and DSR deadlines. Each alert has one action button. *Partly (retention flagged and consent exist; aggregation is planned 2b).*
6. **Team load:** live searches and overdue tasks per consultant. *Planned 2d.*

### Consultant sees first
1. **Today:** overdue, today and upcoming tasks with inline Done, Snooze and Reassign, plus the interviews scheduled today (calendar planned 2d/2e). *Exists (next actions).*
2. **My searches:** each live search as a compact card showing the stage funnel counts (Research → Longlist → Shortlist → Client interview → Offer), the next milestone and a "Continue" button that opens the last tab used. *Partly.*
3. **Candidates waiting on me:** consent pending, CVs uploaded but not reviewed, callbacks due, duplicates to resolve. *Partly (duplicates and CV intake exist).*
4. **Risk alerts** (the same component as the partner's, filtered to my records).
5. **Recent records** (last 8 opened) and a **Quick log** box: type a name, pick an outcome, and save in under 10 s. *Partly.*

Rules: every tile and row is a link to a filtered list (saved-view URL). Numbers are money or days, not counts of records. The page states its freshness ("Updated 08:42"). Empty states tell the user what to do next (see the brand microcopy).

---

## (b) Navigation map: seven areas

Desktop: a left rail (232 px, collapsible to 64 px icons) in midnight chrome, with a header bar (56 px) holding the command bar trigger, quick add, notifications, TR/EN and the user menu. Phone: a bottom bar with Home, Searches, Candidates, Clients and **+**. Settings-like screens sit under Admin, visible to managers and admins only.

| # | Area (EN / TR) | Sub-areas and screens | Key actions | Status |
| --- | --- | --- | --- | --- |
| 1 | **Home / Ana sayfa** | Command centre; Notifications; My tasks; Calendar | Done/snooze task, quick log, open alert | Command centre: Partly (My day). Notifications: Planned 2d. My tasks: Partly. Calendar: Planned 2d |
| 2 | **Clients / Müşteriler** | Client list; Client record: Overview (About, highlights, health), Contacts and org chart, Searches, Exposure (everyone we presented to this client), Off-limits agreements, Commercial terms (fee agreement, guarantee, billing entity), Proposals and mandates (link to BD), Activity, Files; Contacts list | New client, edit, add contact (inline new person), log call, start search, add off-limits, set owner, tag, archive, merge | List and record: Exists. Edit/archive/merge: API-only or missing. Exposure-by-client: Planned 2b. Off-limits on the client: Planned 2b. Commercial terms: Planned 2c. Org chart: Planned 2c. Contacts list: Planned |
| 3 | **Searches / Aramalar** | Search list (table, board); Search record: Brief (must/nice to have, compensation band, target and off-limits companies, team, timeline), Research (target-company list, mapping, call sheet), Longlist, Shortlist (ranked, rated, with rationale), Pipeline (board/table toggle), Interviews and scorecards, Client reports (shortlist PDF, blind version, weekly progress), Tasks and team, Activity, Files; Placements list | New search, edit brief, add candidates (bulk), move stage, rank, rate, send shortlist, generate report, close/place, duplicate search | List and board: Exists. Create from the list: Missing. Brief: Planned 2c. Longlist/shortlist objects: Planned 2c. Reports: Planned 2d. Placements: Planned 2c |
| 4 | **Candidates / Adaylar** | Pool (search, filters for city, function, seniority, sector, languages, salary, notice, consent; boolean CV search); Saved searches and talent pools; Person record: Overview, Career and education, Compensation and availability, Evaluations (interview notes, scorecards, references), Exposure, Searches, Relationships, Files and CVs, Privacy (consent, retention, export, anonymise); CV intake; Duplicates; Import | New candidate, upload CVs (bulk), edit, add to search (bulk), request consent (bulk), tag, merge, export profile, anonymise | Pool, person, CV intake, duplicates, import: Exists. Filters: Partly (no city or skill). Edit: API-only. Evaluations: Planned 2c. Saved searches: Partly (saved views). Profile PDF: Planned 2d |
| 5 | **Business development / İş geliştirme** | Opportunity pipeline (Lead → Meeting → Proposal sent → Negotiation → Won/Lost, value per stage); Proposals (versions, fee terms, PDF); Mandates won (converted to searches); Cold-client list; Warm-path finder and relationship map | New opportunity, send proposal, mark won (creates a search in one click), mark lost with reason | Proposals: Partly (client tab, no edit). Opportunity: Planned 2c. Relationship map: Partly (person map exists). Warm path: Planned 2e |
| 6 | **Insights / Analiz** | Dashboards: fee-weighted pipeline, billings and instalments, placements, time to longlist/shortlist/placement, stage conversion, revenue per consultant, source channels; Client progress reports library; Compliance cockpit (consent coverage, retention due, DSR status) | Filter by period, owner, client; export CSV/PDF; open the underlying list | Planned 2d (dashboards), 2e (compliance cockpit) |
| 7 | **Admin / Yönetim** | Users and roles; Teams; Status lists and stage names; Reference data (sectors, functions, currencies); Import and export centre; Templates (e-mail TR/EN, report branding with the firm's logo); Privacy (notices, consent text versions, DSR queue, retention settings); Off-limits rules (global); Audit log (access, revisions, exports); Integrations (Outlook/Gmail, SSO, assessments); Security statement (hosting, encryption, backups) | Invite, deactivate, reset password, edit lists, run retention, answer a DSR, view the audit trail | Users, roles, statuses, import, retention, off-limits, audit (access log): Exists. Deactivate/reset: API-only. DSR, notices: API-only. Templates: Planned 2d. Integrations: Planned 2e. Security statement: Planned (quick win) |

Route plan (new names, with the old routes redirected): `/` Home · `/clients`, `/clients/:id/:tab?` (was `/accounts`) · `/searches`, `/searches/:id/:tab?` (was `/job-orders`) · `/candidates`, `/candidates/:id/:tab?`, `/candidates/intake`, `/candidates/duplicates` (was `/network`, `/people/:id`) · `/bd/opportunities`, `/bd/proposals` · `/insights/:dashboard` · `/admin/:section` (was `/settings/:section`) · `/search?q=` (global results). Every tab and filter state lives in the URL, so it can be shared and survives a refresh.

---

## (c) Cross-cutting interaction patterns

**Command bar (Ctrl/Cmd + K)**: *exists, extend.* One input for three things: go to a record or screen, run a command ("log call with …", "add Ayşe to CFO search", "new client"), and ask scoped search. Results are grouped by object with icons and status badges. It runs on the keyboard only, with a live region announcing the result count. Recent records and commands appear when the input is empty.

**Keyboard shortcuts** (a help sheet on `?`; every single-key shortcut can be turned off in the user menu, WCAG 2.1.4):

| Keys | Action | Keys | Action |
| --- | --- | --- | --- |
| Ctrl/Cmd+K | Command bar | `/` | Focus global search |
| `g` `h` / `c` / `s` / `p` / `b` / `i` / `a` | Go to Home, Clients, Searches, Candidates (People), BD, Insights, Admin | `n` | Quick-add menu |
| `j` / `k` | Next / previous row | Enter | Open record |
| Space | Open or close the side panel for the focused row | `x` / Shift+`x` | Select row / select range |
| `e` | Inline edit the focused field; on a record, open the Edit drawer | Ctrl/Cmd+Enter | Save form or drawer |
| `l` | Log activity on the current record | `t` | New task on the current record |
| `[` / `]` | Previous / next record in the list | Ctrl/Cmd+Z | Undo the last action (while its toast is visible) |
| Alt+↑ / Alt+↓ | Rank up/down in the shortlist; move a board card one stage with Alt+← / Alt+→ | Esc | Close panel, menu or dialog (asks first if there are unsaved edits) |

**Right-hand side panel (any record)**: *planned.* Opening a row with Space or a click on its name (with a modifier) opens a 480 px panel (720 px wide mode) showing the record header, highlights, the next action, the last 5 activities and the primary actions. The list keeps its scroll and selection. The panel stacks at most one deep (opening another record replaces it), focus moves into it and returns to the row when it closes, and it has a "Open full page" link. On phone it becomes a full-screen sheet.

**Quick-add menu (`n`, header "+ New")**: *exists, extend.* Log activity, Task, Client, Contact, Candidate, Upload CVs, Search, Opportunity, Proposal, Off-limits rule. It presets context when opened from a record (client, search, person). **One** activity/task form everywhere (it replaces `LogActivityDialog` and `AddActionDialog`): type, outcome, when, who (contact/candidate), search, follow-up date and owner, note optional.

**Bulk actions bar**: *partly (Candidates only).* It appears in place of the list toolbar when at least one row is selected, and is announced as "3 selected". Actions: Add to search, Change owner, Change status, Add tag, Remove tag, Set next action, Request consent, Export selected, Archive. Destructive and consent-relevant actions show a summary ("2 skipped: no consent"). Everything runs through `POST /bulk-actions` and gets an undo toast. The bar is the same on every list.

**Saved views**: *exists (server-backed `/views`).* A view selector on every list (All, Mine, Recently viewed, saved, shared), a pin as default, rename, duplicate, delete, and share (managers and admins). Saved candidate searches are saved views on the pool, and talent pools are tag-based saved views.

**Inline edit with undo**: *missing.* A pencil appears on hover and focus for every editable field in the highlights and About cards. Enter saves and Esc cancels. Saving is optimistic, followed by a toast "Owner changed to Can Yıldız · Undo". A version conflict (409 or ETag) opens "Compare and save". Anything multi-field uses the **Edit drawer** (480 px, sections, sticky Save and Cancel, unsaved-changes guard). **Undo instead of confirm** applies to everything reversible; true deletes become archive with restore. Only anonymise and permanent delete ask for typed confirmation.

**Notification centre**: *planned 2d.* A bell in the header with a count badge (text plus number for screen readers). Tabs: For me (mentions, assignments, approvals), Risk (consent, retention, off-limits overrides) and System (imports finished, reports ready). Each item has an action and mark-as-read. A daily digest e-mail can be switched on.

**Global search scopes**: *exists (type-ahead and results), extend.* Scope chips: All · Clients · Contacts · Searches · Candidates · CV text · Activities · Files. Turkish-aware (İ/ı, diacritics-insensitive). Operators: quotes, OR, `-exclude`, `city:Ankara`, `owner:me`. Results are grouped cards with a "View all" link, which opens the list with the filter applied.

**Breadcrumbs**: on every non-top-level page: Area › Parent › Record (for example Searches › Example Gıda A.Ş. › Chief Financial Officer), wrapped in `nav aria-label="Breadcrumb"` with the current page marked `aria-current="page"`.

**Record header pattern (all records)**: object icon tile · serif name · status Path (chevrons) or badge · 4-7 highlight fields · tag chips · owner avatar. On the right sit **one primary action**, which is always the same per object, then at most 2 secondary actions and an overflow menu (Edit, Duplicate, Merge, Export, Archive, Audit history).

| Object | Primary action | Secondary |
| --- | --- | --- |
| Client | Log call | Start search · Add contact |
| Search | Add candidates | Send shortlist · Log activity |
| Candidate | Add to search | Log call · Request consent (shown when consent is missing) |
| Opportunity | Advance stage | Send proposal · Log activity |
| Proposal | Send | New version · Mark won |

---

## (d) Accessibility requirements (WCAG 2.2 AA, testable)

| # | Rule | Test |
| --- | --- | --- |
| A1 | Zero axe-core violations (wcag2a, wcag2aa, wcag21aa, wcag22aa) on every route, in the empty, loaded and error states | vitest-axe on components; Playwright + @axe-core on every route in CI |
| A2 | Text contrast ≥ 4.5:1 (≥ 3:1 for text ≥ 24 px or ≥ 18.66 px bold); UI boundaries, focus rings and status dots ≥ 3:1 | Unit test that recomputes every pair in `brand-direction.md` 2.2 from `design-tokens-v2.json` and fails when a pair drops below its threshold |
| A3 | Every interactive element can be reached and operated with the keyboard, in DOM order, with no traps (grid, board, command bar, side panel, dialogs, drag and drop with keyboard alternatives) | Playwright keyboard-only scripts for the 5 core tasks; Tab through each route |
| A4 | A visible focus indicator: 3 px outline, 2 px offset, ≥ 3:1 against the adjacent colour, never hidden by sticky headers or the bulk bar (2.4.11) | Visual regression on focused states; check `scrollIntoView` margins |
| A5 | Target size ≥ 24 × 24 px for every control (2.5.8); ≥ 44 px on phone and for primary actions | Playwright measures `getBoundingClientRect` of every clickable element |
| A6 | Landmarks: one `header`, `nav` (labelled), `main`, an optional `aside` for the side panel; a skip link to main as the first focusable element | axe landmark rules plus a test for the skip link |
| A7 | One `h1` per page (the record name or page title); heading levels never skip | Automated heading-order check |
| A8 | Every input has a visible label; errors are tied with `aria-describedby`, announced, and say how to fix the problem; required fields are marked in text, not only with "*" | Form tests per dialog and drawer |
| A9 | Status is never conveyed by colour alone: badges have a dot and text, alerts have an icon and text, and charts have labels | Review checklist plus a snapshot of the badge component |
| A10 | Live regions: toasts are `role="status"` (errors `role="alert"`); list result counts and bulk selection counts are announced | Screen-reader assertions in tests (testing-library `getByRole('status')`) |
| A11 | Toasts with Undo stay ≥ 6 s, pause on hover and focus, and their action can be reached with the keyboard (Ctrl/Cmd+Z) (2.2.1) | Component test with fake timers |
| A12 | Reflow: no horizontal scrolling at 320 px CSS width, except data grids, which scroll inside their own region with a sticky first column; content works at 200 % zoom | Playwright at 320 px and at 1280 px with 200 % zoom |
| A13 | `prefers-reduced-motion` removes all translation and animation | Emulated media test |
| A14 | Single-character shortcuts can be turned off or remapped (2.1.4) | Settings test plus a check that the shortcuts are disabled after the toggle |
| A15 | Drag actions (board, rank) have a single-pointer and keyboard alternative: a "Move to…" menu and Alt+arrows (2.5.7) | Playwright without drag |
| A16 | Dialogs and drawers: focus moves in on open, Tab is trapped inside while open, Esc closes, and focus returns to the trigger | Component tests |
| A17 | `lang` is set on `html` (tr or en) and switches with the locale; mixed-language content (for example English CV text) gets its own `lang` | DOM assertion on toggle |
| A18 | Data grids use `role="grid"` or a native table with sortable headers exposing `aria-sort`, and row selection exposed with `aria-selected` | Grid component tests |
| A19 | Authentication works without a cognitive test, allows paste and supports password managers (3.3.8) | Login test with paste |
| A20 | Help and the "Contact support" link are in the same place on every page (3.2.6); forms do not ask again for data already entered in the same flow (3.3.7) | Review checklist |

---

## (e) Screen inventory

Priority: **P0** = pilot core, **P1** = before pilot, **P2** = after pilot.

| Screen | Purpose | Primary action | Empty | Loading | Error | Status | Priority |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Login | Sign in | Sign in | – | Button busy | Inline "E-mail or password is wrong" | Exists | P0 |
| Command centre | Today's priorities, money, risk | Open top priority | "Nothing due today" + next deadline | Card skeletons | Per-card retry, page still usable | Partly | P0 |
| Notifications | Mentions, approvals, risk | Act on item | "You're up to date" | List skeleton | Inline retry | Planned 2d | P1 |
| Calendar | Interviews and meetings | Schedule interview | "No meetings this week" | Grid skeleton | Retry | Planned 2d/2e | P2 |
| Client list | Find and segment clients | New client | Illustration + "Add your first client" + Import | Row skeletons | Banner + retry | Exists | P0 |
| Client record | Everything about a client | Log call | Per tab | Header + card skeletons | Not found / no access page | Exists (thin) | P0 |
| Client › Exposure | All candidates shown to this client | Export | "No one presented yet" | Row skeletons | Retry | Planned 2b | P0 |
| Client › Off-limits | Agreements for this client | Add rule | "No off-limits agreement" + Add | Skeleton | Retry | Planned 2b | P0 |
| Client › Commercial terms | Fee agreement, guarantee, billing | Edit terms | "No terms recorded" + Add | Skeleton | Retry | Planned 2c | P1 |
| Client › Org chart | Reporting lines | Add contact | "Add contacts to build the chart" | Skeleton | Retry | Planned 2c | P2 |
| Contacts list | All client contacts | New contact | "No contacts" | Rows | Retry | Planned | P1 |
| Search list | All live and closed searches | New search | "No live searches" + Start | Rows | Retry | Exists | P0 |
| Search › Brief | The spec | Edit brief | "Write the brief" checklist | Skeleton | Retry | Planned 2c | P0 |
| Search › Research | Target companies, call sheet | Add target company | "Map the market" | Rows | Retry | Planned 2c | P1 |
| Search › Longlist | Candidates being worked | Add candidates | "The longlist is empty" | Rows | Retry; consent/off-limits blocks inline | Partly (board stage) | P0 |
| Search › Shortlist | Ranked, rated, with rationale | Send shortlist | "Move candidates from the longlist" | Rows | Retry | Planned 2c | P0 |
| Search › Pipeline board | Stage overview | Move stage | Empty column hint | Column skeletons | Move rolled back + toast | Exists | P0 |
| Search › Interviews and scorecards | Evaluation | Add scorecard | "No interviews yet" | Skeleton | Retry | Planned 2c | P1 |
| Search › Client reports | Shortlist PDF, blind version, weekly progress | Generate report | "No reports yet" | Progress with steps | "Could not build the PDF" + retry | Planned 2d (print view = quick win) | P0 |
| Placements | Placed candidates, guarantee, fee | Mark placed | "No placements this year" | Rows | Retry | Planned 2c | P1 |
| Candidate pool | Find people | Add candidate / Upload CVs | "Search by name, city, skill or CV text" | Rows | Retry | Exists | P0 |
| Person record | Everything about a person | Add to search | Per tab | Skeletons | Not found / anonymised notice | Exists (thin) | P0 |
| Person › Evaluations | Notes, scorecards, references | Add note | "No evaluations" | Skeleton | Retry | Planned 2c | P1 |
| Person › Privacy | Consent, retention, export, anonymise | Request consent | "Consent not requested" | Skeleton | Retry | Partly | P0 |
| CV intake | Bulk upload and review | Upload CVs | Drop zone | Per-file progress | Per-file error + retry | Exists | P0 |
| Duplicates | Resolve duplicates | Merge | "No duplicates found" | Rows | Retry | Exists | P1 |
| Opportunity pipeline | BD funnel by value | New opportunity | "No open opportunities" | Columns | Retry | Planned 2c | P1 |
| Proposals | Offers and versions | New proposal | "No proposals" | Rows | Retry | Partly | P1 |
| Dashboards | Money, time, conversion | Change period | "Not enough data yet" with explanation | Chart skeletons | Per-chart retry | Planned 2d | P1 |
| Compliance cockpit | Consent coverage, DSRs, retention | Open DSR | "All clear" | Skeleton | Retry | Planned 2e | P1 |
| Global search results | Cross-object results | Open result | "No results for …" + tips | Grouped skeletons | Retry | Exists | P0 |
| Admin › Users and roles | Manage access | Invite user | – | Rows | Retry | Exists (thin) | P0 |
| Admin › Status lists and reference data | Stage names, sectors, currencies | Add value | – | Rows | Retry | Partly | P1 |
| Admin › Import and export | CSV/Excel in and out | Start import | "No imports yet" | Progress | Row-level error report | Partly | P0 |
| Admin › Privacy (DSR, notices, retention) | KVKK operations | New DSR | "No open requests" | Rows | Retry | Partly (retention) | P0 |
| Admin › Off-limits rules | Global rules | Add rule | "No rules" | Rows | Retry | Exists | P0 |
| Admin › Audit log | Access, revisions, exports | Filter | "No events in range" | Rows | Retry | Partly (access log) | P1 |
| Admin › Templates and branding | E-mail TR/EN, report logo | Edit template | "Use the default template" | Skeleton | Retry | Planned 2d | P1 |
| Admin › Integrations and security | Mail sync, SSO, hosting statement | Connect | – | Skeleton | Retry | Planned 2e (statement: quick win) | P2 |

---

## (f) Data manipulation matrix

Starting point: the critique's layer matrix and "features the UI cannot reach" (`critique-functional-gaps.md`), checked against `contracts/openapi.yaml` paths and the `api.GET/POST/PATCH/DELETE` calls in `frontend/src` on 2026-10-29.

Legend: **W** = works today in the UI · **W~** = works today but incomplete (the note says what is missing) · **A** = the API exists, no UI · **M** = missing (no API and no UI) · **–** = not applicable.

| Entity | Create (full form) | View | Edit (inline + drawer) | Delete / archive + restore | Duplicate | Merge | Import CSV/Excel | Export CSV / PDF | Bulk edit | Bulk assign owner | Tag | Reorder / rank | Attach files | Notes | Undo | Audit history |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Client (account) | W~ (3 of 6 fields; website, notes, owner missing) | W | A (PATCH) | W~ (status Closed only; archive/restore M) | M | M | W (ACCOUNTS) | W~ CSV list / PDF profile M | A (bulk-actions) | A (bulk-actions) | A (tags via PATCH; filter only) | – | M | A (notes field) · activity W | M | A (/audit/revisions) |
| Contact | W~ (existing person only; inline new person A) | W | A (PATCH role, primary) | W~ (End with no confirm or undo; restore M) | – | W (via person merge) | A (CONTACTS kind, not offered in UI) | M | M | – | – | M (primary flag only) | – | M | M | A |
| Candidate / person | W~ (10 fields + CV intake; career, compensation, languages, education, notice M in form) | W | A (PATCH) | A (anonymise); archive M | – | W | W (PEOPLE) | W CSV list · A per-person KVKK export · PDF profile M | A | A | A (filter only) | – | W (CV upload) | A (notes field) · activity W~ | M | A |
| Search (job order) | W~ (title, location, openings; only from client page; brief M) | W | A (PATCH) | W~ (status Closed; restore by status) | M | – | M | W CSV list / PDF brief M | A (verify bulk scope) | A (verify bulk scope) | A | – | M | A (description) | M | A |
| Proposal / mandate | W~ (title, terms, fee, currency; owner and dates M) | W~ (terms never rendered) | A (PATCH) | M | M (versions M) | – | M | M (PDF M) | M | A (PATCH owner) | A | – | M | A (statusReason) | M | A |
| Longlist / shortlist entry (application) | W~ (one at a time; no bulk add from pool) | W (board) | W~ (stage drag only) | W (remove) / restore M | – | – | M | M (shortlist PDF M) | M | – | M | M (no rank) | – | W~ (stage note) | W (stage move toast) | A |
| Activity | W~ (account dialog: type and note only; QuickAdd fuller) | W (timeline) | A (PATCH) | A (DELETE) | – | – | M | M | M | – | – | – | M | W (is the note) | M | A |
| Next action (task) | W~ (title, due, account; owner and links via QuickAdd only) | W | W~ (Done only; PATCH other fields A) | A (DELETE) | M | – | M | M | A (SET_NEXT_ACTION) | A (PATCH owner) | – | – | – | M | W (Done undo) | A |
| Off-limits rule | W (Admin) | W | W (PATCH) | W (delete) / restore M | – | – | M | M | M | – | – | – | – | W (note field) | M | A |
| Consent | W (request; bulk request W) · manual record A | W~ (Privacy tab) | – | A (withdraw via record) | – | – | M | A (per-person export) | – | – | – | – | M (signed form) | M | – | A (requests list A) |
| File / CV | W (upload, CV intake) | W~ (list; preview M) | M (rename, kind) | A (DELETE file) | – | – | W~ (bulk CV intake) | W~ (download via content) | M | – | M | M (versions order) | – | M | M | A |
| Tag | A (free text via record PATCH) | W~ (filter, hidden column) | M (rename) | M | – | M | M | – | A (ADD_TAG / REMOVE_TAG) | – | – | – | – | – | M | M |
| Saved view | W | W | W~ (update; rename unverified) | W~ (delete; restore M) | M | – | – | – | – | – | – | M (order in menu) | – | – | M | M |
| User | W~ (admin types the password; invite by e-mail M) | W | W~ (role only; name A) | A (deactivate via PATCH active) | – | – | M | M | M | – | – | – | – | – | M | W~ (access log) |
| Fee agreement | M (free-text `feeTerms` on search only) | W~ (fee terms text on search) | A (feeTerms via job PATCH) | M | M | – | M | M | M | – | – | – | M | M | M | M |
| Placement | M (only the "Placed" stage) | W~ (stage) | M (salary, start date, fee, guarantee) | M | – | – | M | M | M | – | – | – | M | M | M | M |

**Reading the matrix.** Nearly every **A** cell is frontend-only work: the contract and backend are done, so these are the cheapest wins and come first in the build plan. The **M** cells in Client archive/merge, Tag management, Fee agreement, Placement, Longlist rank and PDF exports need the architect, db and backend first.

**Rules every entity must follow once built** (they are the acceptance checks in section g):
1. **Create** is a full form with all contract fields, grouped in sections, with proactive duplicate detection where the entity can be duplicated (client, person, contact).
2. **Edit** works inline for single fields and through the Edit drawer for groups, both with optimistic `version` and a conflict dialog.
3. **Archive** replaces delete for business records; an "Archived" saved view allows restore; permanent delete and anonymise are manager-only with typed confirmation.
4. **Undo** toast after every reversible change, including bulk changes.
5. **Bulk**: the shared bulk bar with owner, status, tag, next action, export and archive.
6. **Import and export**: CSV/Excel import with a mapping step and a row-level error report; CSV export of the current view; PDF export where the entity is client-facing. Every export is audit-logged.
7. **Audit history**: an overflow-menu item "History" opens a side panel with revisions (who, when, field, old → new) from `/audit/revisions`.

---

## (g) Build plan: agent-sized tasks in order

Token rules: each task names its owner, the files and areas it touches and its acceptance check. An agent reads only the files listed plus `docs/process/context/brief.md`, this section and the relevant matrix row. "Day 1" tasks are frontend-only on existing APIs and can run in parallel after F0. Everything else needs contracts, db or backend first.

### Day 1 (one day; parallel after F0)

| # | Task | Owner | Touches | Acceptance check |
| --- | --- | --- | --- | --- |
| F0 | **Shared data-manipulation primitives:** `EditDrawer` (sections, sticky save, unsaved guard, 409 compare dialog), `InlineEditField`, `useUndoToast` (6 s, pause on focus, Ctrl+Z), `BulkBar` wired to `POST /bulk-actions`, `TagInput`, `ArchiveMenuItem`, `HistoryPanel` (`/audit/revisions`) | frontend-shell | `frontend/src/shell/` (new `edit/`, `bulk/`, `history/`), `shell/grid/DataGrid.tsx` (bulk bar slot) | Unit tests for each component; axe clean; Storybook-free demo on one list; reused by E1-E6 |
| E1 | **Client end to end:** full create form (website, notes, owner select, sector select), About card, inline edit and Edit drawer, tags, bulk owner, status and tag on the client list, CSV export unchanged, history panel | frontend-shell | `frontend/src/accounts/**` (`NewAccountDialog.tsx`, `AccountPage.tsx`, `AccountHeader.tsx`, `list/AccountsList.tsx`, `accountColumns.tsx`), accounts i18n | Every Client row cell that is A becomes W; create → edit → tag → bulk-reassign → undo works by keyboard; axe clean |
| E2 | **Contact end to end:** Add contact with inline new person (contract `person` body) and duplicate check, edit role and primary, End with undo, contacts import offered in Import (CONTACTS kind) | frontend-shell | `accounts/detail/AddContactDialog.tsx`, `ContactsTab.tsx`, `settings/ImportSection.tsx` | A new client gets a brand-new contact without leaving the page; End can be undone; CONTACTS import runs on a sample file |
| E3 | **Candidate end to end:** a multi-section create and edit form (Basics, Career, Compensation and availability, Languages and education, Notes), a "New candidate" shortcut, inline edit, tags, bulk owner/tag/next action on the pool, per-person KVKK export and anonymise (manager, typed confirm), file delete, history | frontend-pipeline | `frontend/src/network/**` (`NewPersonDialog.tsx`, `person/PersonPage.tsx`, `ProfileSection.tsx`, `OverviewTab.tsx`, `PrivacyTab.tsx`, `files/`, `list/NetworkList.tsx`) | All Person A cells become W; compensation fields stay masked for roles without access; axe clean |
| E4 | **Search end to end:** "New search" on the list and in quick add with client picker, seniority, description, fee terms, owner, opened on; Edit drawer; tags; bulk owner and status on the list; bulk "Add to search" from the pool (loops over `POST /job-orders/{id}/applications` with a consent/off-limits summary) | frontend-pipeline | `frontend/src/pipeline/joborders/**` (`list/JobOrdersList.tsx`, `jobColumns.tsx`, `detail/JobOrderPage.tsx`, `NewRecordDialog.tsx`), `shell/quickadd/NewMenu.tsx`, pool bulk bar | 10 candidates added to a search in one action; blocked ones listed with a reason; search editable after create |
| E5 | **Activity and task end to end:** one QuickAdd form everywhere (delete `LogActivityDialog`, `AddActionDialog`), with outcome, date, person, search, follow-up and owner; edit and delete on timeline rows and tasks, with undo | frontend-shell | `shell/quickadd/QuickAddFields.tsx`, `accounts/detail/*Dialog.tsx`, `shell/home/NextActionsCard.tsx`, `NextActionRow.tsx`, timeline components | A call is logged in under 10 s from any record; a wrong entry can be edited or deleted and undone |
| E6 | **Proposal end to end (current model):** render terms, dates, reason and owner; Edit drawer; currency select; validation messages | frontend-pipeline | `accounts/detail/ProposalsTab.tsx`, `NewRecordDialog.tsx` | All Proposal A cells become W |
| E7 | **Privacy and users operations:** an Admin › Privacy screen (DSR queue, notices, manual consent record), user deactivate, password reset and name edit | frontend-shell | `settings/` (new `PrivacySection.tsx`), `settings/users/UsersSection.tsx` | DSR create → update → close works; a deactivated user cannot sign in (e2e) |
| Q1 | **Day-1 quick wins (no data model change):** glossary rename in i18n (Client, Search, Longlist, Shortlist, Candidates), seed cleanup and a believable CFO search with one off-limits rule and one refused consent, city/seniority/sector chips on the pool, printable shortlist view, security statement card | ux-designer (copy) + frontend-pipeline + db-engineer (seed) | `frontend/src/**/i18n/*.json`, `seed/*.sql`, `network/list/PoolFilters.tsx`, a new print route | The consultant critique's quick wins 1-4 and 9 verified in the browser |
| T1 | **Tokens v2 and brand shell:** map `design-tokens-v2.json` into `tokens.ts` and `theme.ts` (after the architect approves it as contract 1.3), load the fonts, chrome header, logo, serif h1 | frontend-shell (+ architect CR) | `contracts/design-tokens.json` (via CR), `frontend/src/theme/*`, `shell/layout/Shell.tsx`, `index.html` | The contrast unit test (A2) passes; visual check on 4 screens; no axe regressions |
| QA1 | **Regression net:** Playwright keyboard-only flows for E1-E5 and axe on all routes | qa-engineer | `frontend/e2e/` | CI green; zero axe violations |

### Next (needs contract, db and backend; in this order)

| # | Task | Owners | Touches | Acceptance check |
| --- | --- | --- | --- | --- |
| D1 | **Archive, restore and merge for clients; archive for searches and proposals** (ADR: soft delete with `archived_at`) | architect → db-engineer → backend-accounts, backend-pipeline → frontend-shell | contract 1.3, Flyway migration, `crm-accounts`, `crm-pipeline`, archive views | The archived client disappears from lists, is restorable from the "Archived" view; merge moves contacts, searches and activities, and is audited |
| D2 | **Tag management** (rename, merge, delete, colour; talent pools as tag views) | architect → backend-accounts → frontend-shell | tag endpoints, Admin › Reference data | Renaming a tag updates all records; the talent pool view lists the tagged people |
| D3 | **Longlist/shortlist as first-class objects:** rank, rating, rationale, keyboard reordering, client feedback per entry | architect → db-engineer → backend-pipeline → frontend-pipeline | `application` extension (rank, rating, rationale), Search › Longlist/Shortlist tabs | A shortlist of 5 ranked with rationale in under 3 minutes; Alt+↑/↓ reorders it |
| D4 | **Search brief and research:** must/nice-to-have, compensation band, target and off-limits companies, team | architect → db → backend-pipeline → frontend-pipeline | job order brief tables, Brief and Research tabs | A brief can be created, edited and printed |
| D5 | **Fee agreement and placement entities:** retainer instalments, success fee, guarantee, billing entity; placement with salary, start date, fee and guarantee end | architect → db → backend-accounts/pipeline → frontend-pipeline | new tables, Client › Commercial terms, Placements list | Placing a candidate creates a placement with the fee computed from the agreement; the guarantee end date shows in alerts |
| D6 | **Client-facing PDFs:** shortlist report, candidate profile, blind version, weekly progress report; export audit | backend-pipeline + security-privacy → frontend-pipeline | report service, templates, Search › Client reports | A branded PDF of a 5-person shortlist; the blind version hides name, photo and current employer; each export is audited |
| D7 | **Import and export completeness:** Excel (xlsx) import, search and activity import, mapping UI, export approval for "Export all" | backend-accounts + security-privacy → frontend-shell | `/imports` kinds, Import centre | Every list export is logged; "Export all" requires manager approval |
| D8 | **Exposure by client, off-limits on the client record, consent risk banners on pipeline cards** | backend-pipeline → frontend-shell/pipeline | exposure aggregation, client tabs, board card | The client tab lists everyone presented, with date and outcome |
| D9 | **Command centre aggregates:** `/home` v2 with searches needing action, client health, risk alerts, fee-weighted pipeline (after D5 and the opportunity) | backend-accounts → frontend-shell | `/home`, `shell/home/*` | The partner and consultant views render in under 300 ms with seed data |
| D10 | **Side panel, notification centre, IA routes and redirects** (`/clients`, `/searches`, `/candidates`, `/admin`) | frontend-shell | `routes.tsx`, `shell/layout`, new `shell/panel/`, `shell/notifications/` | Old URLs redirect; Space opens the side panel from any list; focus returns to the row |
| D11 | **BD:** opportunity pipeline, proposal versions and PDF, won → search in one click | architect → db → backend-accounts → frontend-pipeline | Wave 2c | Won opportunity creates a search with client and fee prefilled |
| D12 | **Insights dashboards v1 and compliance cockpit** | backend + frontend-shell | Wave 2d/2e | Each chart has a table alternative and opens its underlying list |

Order rationale: F0 first, because every entity task reuses it. E1-E7 turn every **A** cell into **W** using existing APIs, which closes the critique's blockers B1-B4 and M3-M12 without backend work. D1-D8 close the **M** cells that block live searches (archive/merge, rank, brief, fees, PDFs). Then the command centre (D9), which needs that data, and the IA shell (D10) bring it all together under the new navigation.
