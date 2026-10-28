# Critique: functional gaps (create/edit depth)

Date: 2026-10-28. Method: static audit of contracts/schema.md, contracts/openapi.yaml, crm-accounts DTOs/entity, frontend/src, seed/, and the Product Plan; live click-through at http://localhost:5173 (Chrome extension was available). One test record was created: account "ZZ-CRITIC-TEST Client" (id 01a17fd8-a83a-702c-bd42-f58e11b34ff3). Delete or rename it once an edit/delete path exists; nothing else was touched.

## Verdict

The user is right, and the cause is systematic. The backend and contract are rich (the Account has website, notes, owner and tags; the Person has about 20 fields; the JobOrder has seniority, description and fee terms). The frontend exposes a thin slice of them, and it has no edit screen for any business record. `PATCH /accounts/{id}`, `/people/{id}`, `/job-orders/{id}`, `/proposals/{id}`, `/accounts/{id}/contacts/{contactId}`, `/activities/{id}` and `DELETE /next-actions/{id}` are all implemented server-side and never called by the UI. The set of API calls the UI makes was confirmed by grepping `api.(GET|POST|PATCH|PUT|DELETE)(` across frontend/src. After create, a typo in a client name is permanent as far as a consultant can tell.

## Defects, severity-ranked

### BLOCKER

**B1. No edit for any core record (account, person, job order, proposal, contact, activity, next action).**
- Evidence: `AccountPage.tsx:60-125` and `AccountHeader.tsx:21-56` contain no edit control; the only header actions are status menu, Log activity and Add action. Live: the account page for the test client shows only those buttons. The person page (Adam Arslan, `/people/0193...0076`) has two buttons plus tabs, with no Edit. `JobOrderPage.tsx` offers only a status menu, add candidate and log. `ProposalsTab.tsx:154-177` renders title, fee and a status menu only.
- Contract: `AccountUpdateRequest`, `PersonUpdateRequest` (openapi.yaml:3558, 3695), `JobOrderUpdateRequest` (3907), `ProposalUpdateRequest` (4007) and `ContactUpdateRequest` (3747) exist with optimistic `version`. Backend: `AccountController.java:74` has `@PatchMapping("/{id}")`.
- Professional CRM: inline edit or an Edit drawer on every record header, with a version-conflict message.
- Fix: frontend-shell (shared `EditRecordDrawer` pattern, account + person), frontend-pipeline (job order, proposal). Backend: none.

**B2. "New client" captures 3 of 6 fields; website, notes and owner are impossible to enter, and are never displayed.**
- Evidence: `NewAccountDialog.tsx:36-43` has Name, Sector, City only. Live: the dialog has exactly 3 textboxes. `AccountCreateRequest.java` accepts `website`, `notes` (4000) and `ownerId`. The account page never renders `website` or `notes` (grep for `website` in frontend finds only a hidden list column `accountColumns.tsx:45`; `.notes` is never read for accounts). The seed fills them (`seed/02_accounts.sql:35`), so demo data holds website and notes that no screen shows. Owner cannot be chosen on create (it silently defaults to the creator, so a partner cannot assign a client to a colleague); the only owner change is `PATCH /accounts/{id}` (B1).
- Professional CRM: the client form captures legal name, trade name or group, sector, HQ and locations, website, size, owner, source, description or intelligence notes, and key terms (default fee, payment terms). The overview should show them.
- Fix: frontend-shell: add website, notes (textarea), owner select (`GET /users`) to the dialog and show them in an "About" card on `AccountPage`. db-engineer plus architect (see M1) for the fields beyond the contract.

**B3. Cannot create a contact from the client; the only path is a pre-existing person, and the dialog gives no hint.**
- Evidence: `AddContactDialog.tsx:36-62` contains only an `EntityPicker kind="person"`, Role and Primary. Live: opening "+ Add contact" on a new client shows an empty Person combobox, with no "create new person" action and no link to the Network page. The contract supports an inline `person: PersonCreateRequest` (openapi.yaml:3739, "Exactly one of `personId` and `person`"), which the UI never uses. Result: for a brand-new client the user must leave the page, go to Network, "+ New person", pick type CLIENT_CONTACT, come back, and link. Nothing tells them this.
- Professional CRM: "Add contact" opens name, title, email, phone, LinkedIn, role, primary, with duplicate detection (the machinery already exists in `NewPersonDialog`).
- Fix: frontend-shell: reuse `NewPersonDialog` fields with `type` fixed to CLIENT_CONTACT inside `AddContactDialog`, using the inline `person` body. Backend: none.

**B4. Job orders cannot be created from the Job orders screen, and the creation form drops the fields that define a search.**
- Evidence: `JobOrdersList.tsx` has no create button (only filters, views, export). `NewMenu.tsx:24-27` offers Log activity, Add action, New client, New person; no job order, proposal or contact. The only path is account page, Job orders tab, "+ New job" (`AccountPage.tsx:86-90`). `NewRecordDialog.tsx:34` sends `{accountId, title, location, openings}`. Missing from the form, all present in `JobOrderCreateRequest` (openapi.yaml:3892) and `schema.md:272`: `seniority`, `description`, `feeTerms`, `openedOn`, `ownerId`. The job order page displays seniority, owner and `feeTerms` (`JobOrderPage.tsx:106-114`), so they can be seen when seeded but never entered or changed. There is also no brief (must-haves, nice-to-haves, compensation band, target companies; Plan module 4).
- Fix: frontend-pipeline: add a "New job order" button with a client picker to `JobOrdersList` and to the New menu; extend the dialog (seniority, description, fee terms, owner, opened on). architect + db-engineer for the brief (M2).

### MAJOR

**M1. Account model is thinner than the Plan's "company profile" and lacks BD fields.**
- Plan module 1: company profile, locations, revenue to date, account owner, org chart, account health (red flag after 60 days cold). Plan module 2: opportunity = roles needed + estimated fee + stage. Schema `account` (schema.md:87-110) has no description, group or parent company, size, locations (only one `city`), source, key terms, or revenue. No opportunity entity exists (the account status LEAD to PROPOSAL_SENT stands in for it). Value forecasting and a BD funnel by fee are therefore impossible.
- Fix: architect decides (extend `account` vs a new `opportunity`); db-engineer migration; backend-accounts DTO and entity.

**M2. Job order brief is a single free-text `description`.**
- Plan module 4 lists role, must-haves, nice-to-haves, compensation band, target companies, off-limits companies, assignment type (Executive Search / Recruitment), team on the search, scorecard. `schema.md:272-290` has only `location, seniority, description, fee_terms, openings`. No compensation band, no assignment type, no search team, no target-company list.
- Fix: architect, db-engineer, backend-pipeline; frontend-pipeline tab "Brief".

**M3. Person create form omits candidate-profile data the contract and Plan require; no way to enter it anywhere.**
- Evidence: `NewPersonDialog.tsx:14,46-56` sends name, type, title, company, city, email, phone, linkedinUrl, howWeKnow, sourceChannel. Not capturable in any form: `notes`, `noticePeriodDays`, `mobility`, `compensation` (current base, variable, expected, benefits, encrypted), `languages`, `educations`, `employments` (career history), `ownerId`, tags. `ProfileSection.tsx` and `OverviewTab.tsx:42-55` display all of these when the seed provides them, which is exactly the "displayed but not editable" pattern the user describes. `CvIntake` (`CreatePersonDialog.tsx`) is a second creation path with its own subset. Plan module 5 lists "career history, education, languages, compensation, notice period, mobility" as the core of a senior candidate record. The "type" select defaults to EXECUTIVE, and the label "New person" never says "candidate" (live: New menu has "New person", no "New candidate").
- Fix: frontend-pipeline (owner of `network/`; if that belongs to frontend-shell, assign it there): multi-section create/edit form (Basics, Career, Compensation and availability, Languages and education); add a "New candidate" shortcut that presets type CANDIDATE.

**M4. Proposal creation and display drop most of the commercial terms.**
- `NewRecordDialog.tsx:36` sends title, terms, fee, currency (default "TRY", free text, error shows a blank space `error={... ? ' ' : undefined}` with no message at line 60). Not capturable: owner, sent date, decided date, status reason (all in `ProposalCreateRequest` or the proposal table). Not shown anywhere: `terms` (never rendered in `ProposalsTab.tsx`), `sentOn` only if present, `decidedOn`, `statusReason`, owner, tags. Plan module 3: payment stages, guarantee period, off-limits terms, versions with diff, PDF output, e-signature. None exist (no `proposal_version`, no PDF endpoint). The fee model is the client's open question; the UI has no retainer, stages or success-fee structure, only a single amount.
- Fix: frontend-pipeline (rendering terms and dates, validation message, currency select); architect, db-engineer for the fee structure and versions.

**M5. Activity logging is crippled compared to the contract.**
- `LogActivityDialog.tsx:117` posts `{type, note, accountId}` only. The dedicated account and person dialogs cannot set `occurredAt`, link a person or a job order, or edit or delete the entry afterwards (`PATCH/DELETE /activities/{id}` unused; A-04 specifies an edit window for the author or a manager). Only the global QuickAdd (`QuickAddFields.tsx:39-49`) offers person, job order and "when", so the same action behaves differently depending on where it is opened. A call logged yesterday from the account page is stamped "now". No attendees, outcome, or duration; no logging against a contact.
- Fix: frontend-shell: delete `LogActivityDialog` and `AddActionDialog` in favour of the QuickAdd form (one component, preset context); add edit and delete on timeline rows.

**M6. Next actions cannot be assigned, edited or deleted; dead end on the account dashboard.**
- `AddActionDialog.tsx:179` sends `{title, dueOn, accountId}`. No owner (cannot delegate), no person or job link (only via QuickAdd), no edit, no delete (`DELETE /next-actions/{id}` unused; only `PATCH done` in `NextActionRow.tsx`). Live: dashboard shows "NEXT ACTION: Set next action" with a single entry slot.
- Fix: frontend-shell.

**M7. Contacts: role, primary flag and reporting data cannot be changed after creation; "End contact" has no confirmation.**
- `ContactsTab.tsx:232-241`: an "End" button fires `api.DELETE` immediately, with no confirm or undo (the Plan says undo toast for irreversible steps), and swallows errors (`.then(onChanged)` only). There is no role edit or primary toggle (`PATCH` unused). A promoted CFO who becomes the primary contact requires ending and re-adding. Plan module 1: reporting line, relationship strength, "was a candidate" link, and an org chart per account; none exist (`RelationshipsTab` shows network links only).
- Fix: frontend-shell (confirm/undo, edit role and primary); architect and db-engineer for reporting line and relationship strength.

**M8. No tags anywhere in the UI, although tags exist on account, person, job order and proposal.**
- `schema.md` has `tags` on four tables; the only UI is a read-only filter and a hidden column on the Network list (`PoolFilters.tsx:23`, `personColumns.tsx:50`). `POST /bulk-actions` (ADD_TAG, REMOVE_TAG, CHANGE_OWNER, CHANGE_STATUS, SET_NEXT_ACTION) is never called (grep). The Plan's "talent pools" are therefore unreachable: there is no way to put a tag on a person.
- Fix: frontend-shell (tag chips with add and remove on each record header; bulk bar on all lists); backend: none.

**M9. Bulk and selection exist only on the Network list.**
- `selectable` and `bulkActions` appear only at `NetworkList.tsx:176-180`; Accounts (`AccountsList.tsx`) and Job orders (`JobOrdersList.tsx`) have no checkboxes. Live: no checkboxes on the accounts list. Even on the Network list the only bulk actions are "Export selected" and "Request consent"; owner change, tags, status and next-action bulks (contract) are not offered.
- Fix: frontend-shell (DataGrid already supports it).

**M10. No delete, archive or merge path for accounts, people (except privacy anonymise via API), job orders, proposals.**
- Live: the test client I created cannot be removed or renamed from the UI. Closed is the only state. A mistyped duplicate client stays forever; account merge does not exist (person merge does). Plan: "full export", "deletion requests".
- Fix: architect decision (soft delete / merge for accounts); backend-accounts; frontend-shell.

**M11. KVKK and privacy operations implemented in the API are unreachable.**
- Not called from the UI: `/people/{id}/export`, `/people/{id}/anonymise`, `/data-subject-requests` (list, create, update), `/privacy-notices`, `/people/{id}/consent/records` (manual consent), `/consent/share-check`, `GET /consent-requests`, `/audit/revisions` (grep of frontend/src for those paths returns nothing). The Plan lists "Data subject requests with a log and response deadline" and "Audit log of every view, export and share". Only retention settings, access log and resend consent are available. `PrivacyTab.tsx` is 32 lines.
- Fix: frontend-pipeline or frontend-shell (a "Privacy" screen under Settings: DSR queue, notices, per-person export and anonymise with a manager confirm).

**M12. Users: only invite and role change; no deactivate, no password reset, no name edit; no per-user profile.**
- `UsersSection.tsx` has a search, a role select and an Invite dialog (name, email, role, initial password). `PATCH /users/{id}` is used only for role, and `POST /users/{id}/password-reset` and `/auth/password` are never called. The admin must type a password for the new user (a policy smell: the admin knows it). No phone, title, or team fields.
- Fix: frontend-shell; backend: invite-by-email flow (architect).

### MINOR

- **m1. Labels:** the list calls the entity "Accounts" (nav) and "New client" (button), "Client" in filters, "New account" in the code; people are "Network", "Person", "Executive", never "Candidate". A consultant searching for "Candidates" finds nothing in the nav. Fix: architect picks the glossary; frontend-shell applies it.
- **m2. Create validation:** `NewAccountDialog.tsx:21` shows one generic "fill required" alert; there is no per-field error, no `maxLength` on the account Name or Sector (the server rejects over 200/100), and the website is not collected so it cannot be validated at all. Duplicate-name check is a server 409 only (the person dialog has proactive duplicate detection; the account dialog does not). Fix: frontend-shell.
- **m3. Sector is a free-text input with a datalist** (`NewAccountDialog.tsx:36`). Two consultants will create "Fintech" and "FinTech"; the list filter by sector then fractures. Fix: a Select with "add new" governed by reference data (backend-accounts exposes the list).
- **m4. Currency is a free 3-letter text field** (`NewRecordDialog.tsx:61`), lower-case is silently upper-cased, an invalid code reaches the server. Use a Select (TRY, EUR, USD, GBP).
- **m5. Account list has no "Owner" filter beyond "Mine/All" and no sector or city filter** (`AccountFilterBar.tsx`, 51 lines), though Plan module 1 says "filterable by owner, sector, status". Fix: frontend-shell.
- **m6. Account list columns:** no revenue, open proposals, last contact days, health flag; "Website" exists only as a hidden column (`accountColumns.tsx:45`).
- **m7. Job order list** shows no fee terms, seniority, location or openings columns (`jobColumns.tsx`, 58 lines), although these are the first things a partner scans.
- **m8. Imports:** CSV import exists for people and accounts from Settings (`ImportSection.tsx`) but there is no import for job orders, contacts or activities, so Salesforce migration (a Plan question) is partial.
- **m9. Saved views** have create, list, update and delete in the API; confirm the UI offers rename and delete (`ViewsMenu.tsx`, not exercised live).
- **m10. No Cancel confirmation** when dialogs are dismissed with typed data (all dialogs); work is lost by an Escape or an outside click.
- **m11. Seed vs UI mismatch:** seed populates `website`, `notes` (accounts), `notes`, `how_we_know` (people), `description`, `fee_terms` (job orders) that screens either hide (account website/notes) or can never be edited. The demo looks richer than the product can actually reproduce, so a prospect typing their own data will see less than the demo.

## Layer matrix (displayed / editable / stored / planned)

| Entity | Contract+DB fields | Create form | Edit UI | Shown on detail | Plan items absent everywhere |
| --- | --- | --- | --- | --- | --- |
| Account | name, sector, city, website, notes, owner, status, tags | name, sector, city | none | name, sector, city, owner, since, status | description, locations, revenue, health flag, opportunity, key terms |
| Contact | person, role, primary, endedOn | existing person + role + primary | none (end only) | name, role, email, consent | reporting line, relationship strength, "was candidate" link, org chart |
| Person | ~20 incl. comp, notice, mobility, langs, edu, career, notes, tags, owner | 10 fields | none | most, read-only | scorecards, assessments, references |
| Job order | title, location, seniority, description, feeTerms, openings, openedOn, owner, tags | title, location, openings | none (status only) | seniority, owner, fee, highlights | brief criteria, comp band, type, team, target companies |
| Proposal | title, terms, fee, currency, owner, sent/decided, reason, tags | title, terms, fee, currency | none (status only) | title, fee, sent date | versions, PDF, payment stages, guarantee |
| Activity | type, note, occurredAt, account, person, job | type, note (account) | none | timeline | attendees, outcome |
| Next action | title, due, owner, links, done | title, due, account | done only | list | assignee, edit, delete |
| Off-limits rule | company or account, reason, note, dates | company, reason, note, dates | edit via Settings | banner | per-job off-limits list |
| User | name, email, role, active | name, email, role, password | role only | list | deactivate, reset |
| Tag | on 4 entities | none | none | filter only | talent pools |
| Saved view | name, visibility, definition | in grid | in grid | menu | n/a |

## Top 10 fixes by customer-demo impact

1. Edit drawer on account and person headers, then job order and proposal (B1). Owner: frontend-shell (accounts, person) and frontend-pipeline (job order, proposal).
2. Complete "New client": website, notes, owner; show them on the account overview (B2). Owner: frontend-shell.
3. "Add contact" with inline new-person creation (B3). Owner: frontend-shell.
4. "New job order" on the Job orders page and New menu, with seniority, description, fee terms, owner (B4). Owner: frontend-pipeline.
5. Full candidate form and "New candidate" shortcut: career, compensation, notice, mobility, languages, education, notes (M3). Owner: frontend-pipeline.
6. Tags on every record plus bulk bar (owner, tag, status) on Accounts and Job orders (M8, M9). Owner: frontend-shell.
7. One unified activity and next-action dialog with date, person, job, owner, plus edit and delete (M5, M6). Owner: frontend-shell.
8. Proposal terms, dates, status reason and owner displayed and editable; a currency select (M4). Owner: frontend-pipeline.
9. Account profile model extension and opportunity (M1, M2): decide, migrate, expose. Owners: architect, db-engineer, backend-accounts, backend-pipeline.
10. Privacy operations screen (DSR queue, per-person export and anonymise, privacy notices) (M11). Owner: frontend-pipeline with backend unchanged.

## Features in the Plan or contract that the UI cannot reach

- Edit of any business record (all PATCH endpoints except status, done, role, off-limits).
- Bulk actions API (`POST /bulk-actions`) and tag editing; talent pools by tag.
- Data subject requests, privacy notices, manual consent records, per-person export and anonymise, share-check, audit revisions.
- Password reset and user deactivation.
- Contact create-with-person (inline `person` in `ContactCreateRequest`).
- Activity edit and delete; next-action delete.
- Network link edit and delete (`PATCH/DELETE /network-links/{id}`; only add exists).
- Not built at all (Plan only): proposal versions, PDF, opportunity entity, client portal, branded reports and blind report, assessments, email and calendar sync, scorecards, weekly progress report, target-company list, org chart, account health flag, revenue to date.
