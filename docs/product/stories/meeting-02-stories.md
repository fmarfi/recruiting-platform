# Stories after client meeting 02 (15 Nov 2026)

Source: `docs/product/meetings/2026-11-15-meeting-02.md` (first real user input). Roles are the client's: **researcher**, **consultant**, **sales**. Roles are recorded on the user only; no permission gating until after the pilot, so no criterion below may expect a 403 by role. Team size is 3 to 5 users. Everything is table-first (Excel habit). Story IDs M2-01 to M2-08 map to the report's stories 1 to 8. Items depending on an open question are marked ASSUMPTION and name the question (Q1 ... in the last section).

## 1. Verdict on the 28 existing stories

| Story | Verdict | Reason |
| --- | --- | --- |
| A-01 Accounts and overview | keep | Client company list is still needed; shown as a table. |
| A-02 Account status lifecycle | keep | Not discussed; keep but raise next time (Q10). |
| A-03 Contacts per account | change | A contact is now the named target of a search (M2-05) and a node in the people map (M2-08). |
| A-04 Activity log | keep | Not discussed; keep as is, now also reachable from the candidate page (M2-03). Low priority for a team of 3 to 5. |
| A-05 Proposals | change | Proposals stay, but agreements are split out with their own history (M2-04). |
| A-06 Job orders | change | Add named contact (M2-05) and number of openings plus remaining openings. |
| A-07 Candidate pipeline | change | Becomes a longlist table with a stage column (M2-01); add move between positions (M2-02) and candidate-driven actions (M2-03). |
| A-08 Account timeline | keep | Not discussed; still the history of a client. |
| A-09 Account dashboard | keep | Not discussed; the people map (M2-08) is added next to it, not instead. |
| A-10 Next actions and reminders | keep | Source of "today's tasks" (M2-07). Raise next time (Q10). |
| A-11 CSV/Excel import | keep | Not discussed; Excel habit makes it more relevant. Raise next time (Q10). |
| A-12 Network of people | keep | People still exist as records; map moves into the client (M2-08). |
| A-13 Network links | change | Links are shown as a map inside the client, not only on the person page. |
| A-14 Global search | keep | No input against it; tables need find. |
| S-01 KVKK notice and consent e-mail | keep | Legal duty, not discussed; raise next time. |
| S-02 Website form consent | keep | Phase 2, unchanged. |
| S-03 Retention and deletion | keep | Legal duty; retention start still open. |
| S-04 Data subject requests | keep | Legal duty, unchanged. |
| UX-01 Home: My day | change | Add period filter and personal numbers (M2-06); tasks first (M2-07). |
| UX-02 Three clicks and global search | keep | Still valid; candidate page is the hub (M2-03). |
| UX-03 Status badges and one-click change | change | Keep badges and menu change; drop the drag-and-drop criterion (no board). |
| UX-04 Pipeline board | drop | Client does not want a kanban board; code stays hidden until the pilot confirms. |
| UX-05 Account page in one view | change | Add people map and a separate agreements section. |
| UX-06 Quick add | keep | Not discussed; raise next time. |
| UX-07 Consistent layout and languages | keep | Working language of app and data is open (Q9). |
| UX-08 Guided empty states | keep | Unchanged. |
| UX-09 Responsive layout | keep | Not discussed; desktop tables first, low priority. |
| UX-10 Usability test with pilot users | keep | Now has 3 to 5 real users, three roles. |

Counts: keep 19, change 8, drop 1. Flag "raise next time" (not discussed): A-02, A-10, A-11, S-01 to S-04, UX-06, plus consent and client statuses.

## 2. New stories

### M2-01 Longlist table for a search (replaces A-07 board view; UX-04 dropped)
As a **researcher**, I want to build and work a longlist for a search in one table, so that I need no board or spreadsheet beside it.

Acceptance criteria
1. Given a search with no candidates, when I open it, then I see an empty table with an "add candidate" action and no board or columns view.
2. Given a search, when I add a person (existing or new), then a row appears with name, current stage, last activity date and owner.
3. Given the table, when I change a row's stage from the stage cell, then the new stage is saved and shown without leaving the page; a rejection requires a reason (see `docs/plan/status-lifecycles.md`, A-07).
4. Given a person already in the longlist, when I add them again, then I get a duplicate message and no second row.
5. Given the table, when I sort or filter by stage or owner, then only matching rows are shown and the choice persists on reload. ASSUMPTION: persistence per user; Q2.
6. Given a closed search, when I open it, then the table is read-only.
7. Given the table, when I use "export", then a file with the visible rows and columns downloads (Excel habit). ASSUMPTION: export format; Q2.

### M2-02 Move a candidate between positions (A-07)
As a **researcher**, I want to move a candidate from one position to another, so that a good candidate is not lost when the first role does not fit.

Acceptance criteria
1. Given a candidate in search A, when I choose "move to another search" and pick search B of any client, then the candidate has a row in B and the row in A is marked as moved, not deleted.
2. Given the move, when I open the history of the candidate, then I see one entry with source search, target search, who and when.
3. Given the target already contains the candidate, when I try to move, then I get a duplicate message and nothing changes.
4. Given the target search is closed, when I try to move, then the move is refused with a message.
5. Given a move, when it completes, then the stage in B starts at the first stage and the stage in A keeps its last value. ASSUMPTION: starting stage and whether "copy" (stay in both) is also wanted; Q3.
6. Given a candidate with a reason-required stage (rejected), when I move, then no reason is needed for the move itself.

### M2-03 Run the process from the candidate's page (A-07, A-04)
As a **researcher**, I want to run a candidate's process from the candidate's own page, so that I follow the person, not the search.

Acceptance criteria
1. Given a candidate page, when I open it, then I see every search the person is in, each with its stage.
2. Given the page, when I click "add to a search" and pick a search, then the person appears in that longlist (M2-01 criterion 2).
3. Given a search row on the page, when I change its stage, then the longlist of that search shows the same stage; reason rule as in M2-01.
4. Given the page, when I log an activity (call, meeting, note), then it appears in the candidate's history and in the account timeline of the related client where one exists.
5. Given the page, when I use "move" on a row, then M2-02 applies.
6. Given a person active in another search, when I add them to a new one, then I see the existing `ACTIVE_IN_OTHER_PROCESS` warning and can confirm or cancel.

### M2-04 Agreements apart from proposals (A-05)
As a **consultant**, I want to record an agreement for a position, separate from the business proposal, so that I see proposal history and signed agreements apart.

Acceptance criteria
1. Given a client, when I open it, then proposals and agreements appear as two separate lists, each with its own history.
2. Given a position, when I record an agreement, then it is linked to that position (one or more agreements per position) with date, terms summary and status. ASSUMPTION: fee fields and agreement statuses; Q4, Q5.
3. Given an agreement, when I change it, then a history entry (who, when, what) is added and earlier entries stay unchanged.
4. Given a proposal, when I open its history, then no agreement entries appear there, and the reverse.
5. Given a position without an agreement, when I open the client, then the position shows "no agreement" so the gap is visible.
6. Given an agreement, when the position is cancelled, then the agreement stays visible and read-only history is kept.
7. Given a user of any role (researcher, consultant, sales), when they record an agreement, then it is allowed (no role gating before the pilot); the entering user is stored.

### M2-05 Search on a named contact, with openings (A-06, A-03)
As a **consultant**, I want to create a search on a named contact of the client company, so that everyone knows who the search is for.

Acceptance criteria
1. Given a client with contacts, when I create a search, then I must pick a contact of that client; without one, saving is refused with a field message.
2. Given a client with no contacts, when I start a search, then I can add a contact in the same flow.
3. Given a search, when I open it, then client and contact are shown in the header, and the contact links to the contact record.
4. Given the form, when I enter number of openings (whole number, at least 1), then remaining openings equals that number at creation.
5. Given a search with 3 openings and 1 placement recorded, when I open it, then remaining openings shows 2.
6. Given remaining openings reaches 0, when the last placement is recorded, then the search is offered for closing as filled; it is not closed automatically. ASSUMPTION: what counts as a fill (placement stage) and whether remaining can be edited by hand; Q6.
7. Given a contact is removed, when it has searches, then removal is refused and the user is told which searches use it.

### M2-06 My numbers for a period (UX-01, A-09)
As **any user**, I want to see my own numbers for last week or the last 30 days (interviews, CVs screened, CVs sent, CVs pushed), so that I know where I stand.

Acceptance criteria
1. Given the home page, when I open it, then I see tiles for interviews, CVs screened, CVs sent and CVs pushed, each a count for me only, with the period default "last 30 days". ASSUMPTION: default period and tile list; Q7.
2. Given the period control, when I choose "last week", then all tiles recompute for the previous seven days (definition of week: Q8); other users' activity is never counted.
3. Given a tile, when I click its "view" link, then I see the list of records behind the number and its length equals the number.
4. Given no events in the period, when I look at the tiles, then each shows 0, not blank.
5. Given the events happened on the boundary day, when the period is computed, then the start day is included and the day of the query is included, in the firm's time zone.
6. Given CV sent and CV pushed are two events, when I record one, then only its own tile changes. ASSUMPTION: exact meaning of each and who records them; Q4 of the meeting's open list (CV push vs CV sent).

### M2-07 Today's tasks (UX-01, A-10)
As **any user**, I want to see today's tasks when I sit down, so that I know what to do first.

Acceptance criteria
1. Given tasks assigned to me and due today or overdue, when I open home, then they are the first block, overdue first, then by time.
2. Given a task, when I tick it done from the list, then it leaves the block and is kept in history.
3. Given a task, when I click its title, then I land on the related client, search or candidate.
4. Given no tasks today, when I open home, then I see an empty state with an "add task" action.
5. Given tasks of other users, when I open home, then they are not listed.
6. Given a task due tomorrow, when I open home, then it is not in today's block.

### M2-08 People map inside a client (A-12, A-13)
As **sales**, I want to see the people map inside a client, so that I know who is who before I call.

Acceptance criteria
1. Given a client, when I open it, then a "people" view lists its contacts in a table with name, title, department, reports to, and last contact date. Table first; a drawn org chart is optional. ASSUMPTION: table vs chart; Q9b.
2. Given a contact, when I set "reports to" another contact of the same client, then the view shows the relation; a loop (A reports to B reports to A) is refused.
3. Given a contact, when I click it, then I open the person record with history and the firm's relationship owner.
4. Given a person who moved companies, when I open the old client, then they appear as former contact with end date, not as current.
5. Given a client with no contacts, when I open the view, then I see an empty state with "add contact".
6. Given the view, when I sort by last contact date, then the people not contacted longest are first.

## 3. Open questions for the client (stories depend on these)

| # | Question | Stories |
| --- | --- | --- |
| Q1 | Which fields do you keep, remove or miss on candidate, client and position screens (screenshots sent)? | all |
| Q2 | Which table features matter most: inline edit, paste from Excel, export, saved filters? One anonymised search in Excel please. | M2-01 |
| Q3 | When a candidate moves between positions: does the old row stay, and do you also want "copy" (in both)? Which stage does the candidate start at? | M2-02 |
| Q4 | Exact meaning of CV sent versus CV pushed, and who records each? | M2-06 |
| Q5 | What does an agreement hold (fee model, signed date, statuses) and which engagement types exist besides permanent retained? | M2-04 |
| Q6 | When is an opening filled (which stage) and can the number of openings change later? | M2-05 |
| Q7 | Which of the dashboard tiles do you really use, and which periods besides last week and last 30 days? | M2-06 |
| Q8 | Does a week start Monday, and does "last week" mean the previous calendar week or the last seven days? | M2-06 |
| Q9 | Working language of the app and the data. Q9b: is a people table enough or do you need a drawn org chart? | UX-07, M2-08 |
| Q10 | Not discussed, please confirm or drop: quick log, next actions, client statuses, consent texts, CSV import. | A-02, A-10, A-11, S-01 to S-04, UX-06 |
| Q11 | Story 9 in the report (sales: campaign history): what does it hold and who uses it? No criteria written until answered. | not written |
