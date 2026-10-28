# Brief: demo-day fixes (read this, not the research reports)

Source: `docs/product/research/critique-functional-gaps.md` (B1-B4) and `critique-consultant-review.md` (quick wins). Read only the sections named in your task. Frontend lives in `frontend/src`; the backend and contract already support most of this (do NOT change contracts or backend; put needs in `docs/process/handoffs/<you>.md`).

Rules: commit on your own branch in your own worktree, no attribution lines in commit messages; quiet commands (`mvn -q`, `npx vitest run`, `npx tsc --noEmit`); do not use browser tools unless a fix cannot be checked another way; final reply at most 80 words. Done = `npx tsc --noEmit`, `npx vitest run`, `npx vite build` green. TR and EN strings for every label.

## Task A: client-record (frontend-shell area)
1. "New client" dialog (`NewAccountDialog.tsx`): add website, notes (textarea), owner (select from `GET /users`). Show website, notes and owner in an "About" card on `AccountPage` (they exist in `AccountCreateRequest`/`AccountResponse`).
2. Edit drawer on the account page header and the person page header, using the existing PATCH endpoints with the optimistic `version` field and a clear message on a version conflict (409). One reusable `EditRecordDrawer` pattern.
3. `AddContactDialog.tsx`: let the user create a new person inline (contract `ContactCreateRequest.person`), reusing the `NewPersonDialog` fields with type CLIENT_CONTACT; keep linking an existing person.
4. Full "New candidate" form fields the API already accepts (career, compensation, notice period, mobility, languages, notes) if time allows.

## Task B: search-assignment (frontend-pipeline area)
1. "New job order" button with client picker on `JobOrdersList.tsx` and in `NewMenu.tsx`; extend the dialog with seniority, description, fee terms, owner, opened on (all in `JobOrderCreateRequest`). Add edit for job order and proposal via PATCH with `version`.
2. Vocabulary (translation JSON only, EN and TR): Proposals -> Mandates, stage Sourced -> Longlist, Presented -> Shortlist, "Accounts" -> "Clients" everywhere, "Network" -> "Candidates & contacts". Keep enum values and API untouched; only labels.
3. Network filter bar: City, Seniority and Sector chips (the API supports city).
4. "Printable shortlist" button on a job order: a print-friendly page of candidates at the Presented stage (name, title, company, consent status, a few career lines); no PDF library needed (browser print, `@media print`).
5. Board: make Placed and Rejected columns visible at ordinary widths (narrower or wrapping columns); red banner on a card whose candidate consent is Refused or Withdrawn.

## Task C: demo-curator (seed only, `seed/`)
1. Remove nothing by hand; fix the seed so a reload gives a believable story: one clearly named CFO search for a client with 8 candidates, 5 of them finance people (CFO, finance director, controller, treasury head) with career history, 1 with consent Refused, 1 at a company covered by an off-limits rule so the block can be shown, 1 previously presented to the same client (exposure), consent GIVEN for those at Presented. Fictional names and companies only (Turkish and English).
2. Account notes and websites filled; sectors and cities varied; no junk.
3. Seed must load with `seed/load.ps1` on a migrated database (see `seed/00_clear.sql` for tables to clear, add any new ones).

## Code graph (use it instead of grepping or reading whole files)
A code knowledge graph of the repo is in `C:\Users\SID-DRW\Desktop\New folder\recruiting-platform\graphify-out\graph.json` (built from the main checkout; same code as your branch apart from your own changes). Query it first, it is cheap and offline:
`C:\Users\SID-DRW\tools\graphify-venv\Scripts\graphify.exe query "<question>" --graph "<path to graph.json above>" --budget 1500`
`graphify.exe explain "AccountService" --graph "<same path>"` and `graphify.exe path "A" "B" --graph "<same path>"`.
Each result lists the node and its source file and line; then read only those line ranges. Do not read GRAPH_REPORT.md (47 KB).
