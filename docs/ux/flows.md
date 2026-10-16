# User flows

Core tasks of a consultant, with click counts (UX-02: any record in 3 clicks or fewer from home). Field and endpoint names follow `contracts/openapi.yaml`; anything the contract lacks is listed under "Contract gaps" in `docs/ux/README.md`.

Conventions used in all flows:

- Global search box and the **+ New** menu are on every screen (top bar on laptop, search icon plus floating **+** on phone).
- Every mutation shows a toast with **Undo** where reversible (status changes, stage moves, done actions) and updates the record's timeline at once (A-08).
- Status changes offer only the legal next statuses (`lifecycles.<kind>.statuses[].next` from `GET /reference-data`; `managerOnly` moves are shown disabled for consultants with the tooltip "Only managers can ..."). Illegal moves are not shown; a server 409 is shown as an inline message with the reason. When closing an account is blocked by open job orders, the 409 `blockingItems` are listed as links in that message.

## F1. Add a client (A-01, UX-08)

Goal: a new account exists with owner and status Lead in under 1 minute.

1. From My day empty state, Accounts list header, or **+ New > Client** (1 click).
2. A dialog (bottom sheet on phone) asks for: Name (required), Sector (list), City, Owner (defaults to me). One primary button **Create client**.
3. `POST /accounts` returns the account (status `LEAD`). The user lands on the Account page with the Contacts tab open and a prompt "Add the first contact" (UX-08).
4. Adding a contact: pick an existing person from the network (type-ahead) or **Create new person** inline (name, title, e-mail), then choose a role and tick Primary. `POST /accounts/{id}/contacts` links the account and the network record (A-03). To stop a contact, **End contact** sets an end date; ended contacts hide behind a **Show ended** toggle and stay in the person's company history.

Failure paths: duplicate name warning (soft: "A client with a similar name exists - open it?"); name missing shows an inline field error; offline shows the error banner with Retry and keeps the typed values.

```
Accounts list --[New client]--> dialog --[Create]--> Account page (Lead) --> Add first contact
```

## F2. Change a status (A-02, A-05, A-06, UX-03)

Goal: change account, proposal or job order status in one click plus at most one confirmation.

1. Click the status badge in the record header (or in a list row). The badge opens a menu listing only legal next statuses, each with its coloured dot.
2. Pick the new status. Statuses that need no input apply immediately (toast with Undo for 8 s).
3. Statuses that need input open a small dialog: Rejected (candidate) needs a reason; moves flagged `reasonRequired` in `GET /reference-data` (Closed, On hold, Cancelled, Rejected) require a reason. The reason is stored in the status log (`PATCH .../status` with `reason`).
4. The header badge, list rows and the timeline update (`kind: "status"`, text, user, time).

Phone: the badge menu is a bottom sheet with large rows (44 px).

```
badge click -> [Legal next statuses] -> pick -> (reason dialog?) -> toast "Account is now Active  [Undo]"
```

## F3. Move a candidate through the pipeline (A-07, UX-04)

Goal: change a candidate's stage on the board with one gesture.

Desktop:
1. Open the job (Account page > Job orders > job, or My day > Open jobs > job; 2-3 clicks).
2. Drag the card to another column. Only legal target columns highlight; others dim and refuse the drop. Moving forward may skip stages; back is one stage (lifecycle rule).
3. Dropping on **Rejected** opens the reason dialog (required, with quick-pick chips: Salary expectation, Not interested, Skills mismatch, Client declined, Other). Dropping elsewhere applies immediately with Undo.

Keyboard and phone (no drag): each card has a **Move** button (or tap the stage chip) opening a menu/bottom sheet of legal stages. On phone the board shows one column at a time with a stage switcher strip at the top (swipe or tap), because 7 columns do not fit.

Add a candidate: **Add candidate** on the board opens a searchable list from `GET /job-orders/{id}/candidate-suggestions` (people not yet on this job; consent badge visible); selecting one calls `POST /job-orders/{id}/applications` and the card appears in Sourced. A **Create new person** link at the bottom of the list handles people not yet in the network.

## F4. Quick-add an activity (A-04, UX-06)

Goal: log a call, visit, meeting, e-mail or note in under 30 seconds, including on a phone after a visit.

1. From anywhere: **+ New > Log activity**, or the **Log activity** button on an Account/Person page (context is pre-filled).
2. Sheet shows: type chips (Call, Visit, Meeting, E-mail, Note; last used preselected), a text field focused for the note, a context chip (Account and/or Person; changeable via type-ahead), date/time defaulting to now. Optional toggle **Add a follow-up** reveals title + due date (creates an Action, A-10).
3. **Save** (Ctrl/Cmd+Enter on laptop) calls `POST /activities` (and `POST /next-actions` if a follow-up was added). Sheet closes, toast "Logged", timeline shows the entry and the record's Last activity updates.

Minimum input: type chip (preselected) + one line of text. No other required fields. Voice dictation works through the phone keyboard in the note field.

## F5. Start the day (A-10, UX-01)

1. Login lands on **My day**.
2. Overdue and today's actions sit at the top. Tick the checkbox to complete (`PATCH /next-actions/{id}` with `done: true`, Undo). Tap the action title to open its account/person.
3. **Open jobs** lists my jobs with days open and stage counts; click opens the pipeline board (2 clicks to any candidate).
4. **Recent accounts** shows the five most recently touched.

## F6. Find anything (A-14, UX-02)

1. Type in the global search (focus with `/`). After 2 characters a dropdown lists the top 3 per group (Accounts, People, Jobs) and a final row "See all results for ...". Enter or that row opens Search results.
2. Search results page groups results and offers filters: Type, Sector, Status, Owner, Last contact (see `screens/search.md`).
3. Click a result to open the record (total: type, 1 click = 2 actions).

## F7. Add a person and connect people (A-12, A-13)

1. **+ New > Person** or Network list header. Dialog: Name (required), Type, Title, Company, City, E-mail, How we know them, Relationship owner (defaults to me).
2. On the Person page, **Add connection**: pick another person, choose link type (Knows, Worked with, Introduced, Referred, Reports to), optional note. Connections are shown both ways.
3. **Send consent request** (S-01) appears whenever consent is Not sent; see `screens/network.md`.

## F8. Create a job order and a proposal (A-05, A-06)

1. Account page > Job orders tab > **New job order**: Title, Location, Seniority, Fee terms, Owner (`POST /job-orders`); status starts at Open.
2. Account page > Proposals tab > **New proposal**: Title, Terms (text), fee amount and currency (`POST /proposals`); status starts at Draft; badge menu moves it to Sent, Accepted, Rejected.
3. When a proposal moves to Accepted and the account is still Proposal sent, the app suggests (does not force): "Mark the client as Active?" with one click.

## F9. Admin: manage users and status lists

See `screens/settings.md`. Invite user (e-mail, role) -> user appears as Active; change role from a dropdown on the user row; disable instead of delete (`PATCH /users/{id}`). Status lists are read-only (ADR 0002): labels, tones and transitions are shown for reference, with no edit, reorder or hide.

## F10. Import accounts and contacts (A-11, Could)

Settings > Import > upload CSV/XLSX > map columns (auto-guessed) > preview first 20 rows with row-level errors > Import > summary (created, skipped, errors with downloadable error CSV).
