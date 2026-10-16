# UX index

Owner: ux-designer. Contracts: `contracts/design-tokens.json`. Review of the demo: `demo-review.md`. User flows: `flows.md` (F1-F10).

Screens: [My day](screens/my-day.md) - [Accounts and Account page](screens/accounts.md) - [Job order and pipeline](screens/job-pipeline.md) - [Network and Person](screens/network.md) - [Search](screens/search.md) - [Settings](screens/settings.md).

Global shell (all screens): left nav on laptop (My day, Accounts, Jobs, Network, Settings for Manager/Admin), bottom tab bar plus floating **+** on phone, top bar with global search, **+ New** menu and EN/TR switch.

## Requirement to screen and action

| Req | Screen | Action / element | Flow |
| --- | --- | --- | --- |
| A-01 | Accounts list, Account page | List with status filter; New client; edit account | F1 |
| A-02 | Account page header, Accounts list | Status badge menu; log shows in timeline | F2 |
| A-03 | Account page > Contacts; Person page | Add contact (from network), role, primary | F1 |
| A-04 | Account page, Person page, global + | Log activity | F4 |
| A-05 | Account page > Proposals | New proposal; status badge menu | F8, F2 |
| A-06 | Account page > Job orders; Job page; Jobs list | New job order; job status badge | F8, F2 |
| A-07 | Job page board | Add candidate; drag/move; reject with reason | F3 |
| A-08 | Account page timeline | Filterable timeline, events link to records | F2 |
| A-09 | Account page dashboard strip | Open jobs, candidates per stage, days open, next action | - |
| A-10 | My day; Account/Person pages | Next actions list, add, done, snooze | F5, F4 |
| A-11 | Settings > Import; Accounts list | CSV/XLSX import with mapping and preview | F10 |
| A-12 | Network list, Person page | New person, how we know, owner, last contact, career | F7 |
| A-13 | Person page > Connections | Add connection, kinds, both-way list | F7 |
| A-14 | Global search, Search results | Type-ahead, filters sector/status/owner/last contact | F6 |
| S-01 | Person page > Privacy; Network list row menu; job cards | Consent badge; send consent request; record consent | F7 |
| S-02 | (Website form, Phase 2 B-13) | Not a Module A screen; specified with B-13 | - |
| S-03 | Person page > Privacy; Settings > Retention | Retention date and warning; configure period | - |
| S-04 | Person page > Privacy | Log data request (access, correction, deletion) | - |
| NFR-05 | Settings > Users, Roles, Audit log | Roles matrix; audit viewer | F9 |
| UX-01 | My day | Actions, overdue, recent accounts, open jobs | F5 |
| UX-02 | Shell, My day, Search | Global search on every screen; jobs/accounts reachable in 2 clicks (My day > Jobs > job; Accounts > account > job) | F5, F6 |
| UX-03 | All lists and headers | Status badge = menu (one click); board drag-and-drop | F2, F3 |
| UX-04 | Job page | Kanban per job order | F3 |
| UX-05 | Account page | Header + tabs (Jobs, Contacts, Proposals) + timeline side | F1 |
| UX-06 | Global + New, Account/Person pages, phone bottom bar | Quick-add activity < 30 s | F4 |
| UX-07 | Cross-cutting | One list/filter/detail pattern; TR and EN labels on every screen spec; tokens | all |
| UX-08 | Every screen "States" section | Guided empty states with one next step | F1 |
| UX-09 | Every screen has a phone wireframe | Responsive layouts, breakpoints in tokens | all |
| UX-10 | Not a screen (process) | Usability test script: tasks F1, F2, F3 with 3-5 consultants; owned by qa-engineer/orchestrator with these flows as the script | F1, F2, F3 |

## Status badge coverage

Every status in the demo enums has a tone and TR/EN label in `contracts/design-tokens.json` under `statusTone`: account (5), job (7), stage (7), proposal (4), consent (5), person type (4), plus user role, user state and action due states. Cross-check against `docs/status-lifecycles.md` after merge; add or rename one entry per change.

## Shared UI rules

- Badge = coloured dot + text, tone from the map; never colour alone.
- Status badge is a button where changes are allowed and a plain label elsewhere.
- Every mutation: optimistic update, toast, Undo where reversible, timeline updated.
- Destructive actions use a confirm dialog naming the record; type-to-confirm only for irreversible data deletion.
- Dates: EN `16 Oct 2026`, TR `16.10.2026`; relative dates ("2d ago") with the absolute date in a tooltip.
- Minimum touch target 44 px on phone; contrast AA in both themes.
