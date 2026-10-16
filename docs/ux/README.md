# UX index

Owner: ux-designer. Contracts: `contracts/design-tokens.json`. Review of the demo: `demo-review.md`. User flows: `flows.md` (F1-F10).

Screens: [My day](screens/my-day.md) - [Accounts and Account page](screens/accounts.md) - [Job order and pipeline](screens/job-pipeline.md) - [Network and Person](screens/network.md) - [Search](screens/search.md) - [Settings](screens/settings.md).

Global shell (all screens): left nav on laptop (My day, Accounts, Job orders, Network, Settings for Manager/Admin), bottom tab bar plus floating **+** on phone, top bar with global search, **+ New** menu and EN/TR switch.

## Requirement to screen and action

| Req | Screen | Action / element | Flow |
| --- | --- | --- | --- |
| A-01 | Accounts list, Account page | List with status filter; New client; edit account | F1 |
| A-02 | Account page header, Accounts list | Status badge menu; log shows in timeline | F2 |
| A-03 | Account page > Contacts; Person page | Add contact (from network), role, primary, end contact, show ended | F1 |
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
| S-02 | (Website consent form) | **Phase 2.** Not specified here; designed with B-13. The Phase 1 consent flow is S-01 (e-mailed link, `GET/POST /public/consent/{token}`) | - |
| S-03 | Person page > Privacy; Settings > Retention | Retention date and warning; configure period | - |
| S-04 | Person page > Privacy | Log data request (access, correction, deletion) | - |
| NFR-05 | Settings > Users, Roles, Audit log | Roles matrix; access log (`/audit/access-log`, admin) and change history (`/audit/revisions`) | F9 |
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

`contracts/design-tokens.json` maps tone to colours only. Every status label (EN/TR), its tone and the allowed transitions come at runtime from `GET /reference-data` (`lifecycles.<kind>.statuses[]`: `value`, `labelEn`, `labelTr`, `tone`, `terminal`, `next[]` with `reasonRequired` and `managerOnly`). Lifecycles: account, proposal, jobOrder, pipelineStage, consent, dataSubjectRequest. Person type, user role and action due state are not lifecycles: the UI keeps their labels in its own dictionary and picks a tone itself (EXECUTIVE info, CLIENT_CONTACT good, REFERRER progress, CANDIDATE neutral; overdue BAD, today WARN, upcoming NEUTRAL, done MUTED).

## Shared UI rules

- Badge = coloured dot + text, tone from the map; never colour alone.
- Status badge is a button where changes are allowed and a plain label elsewhere.
- Every mutation: optimistic update, toast, Undo where reversible, timeline updated.
- Destructive actions use a confirm dialog naming the record; type-to-confirm only for irreversible data deletion.
- Owner rule: any consultant may change any record. Moves only a manager may make (`managerOnly` in reference data, for example Closed on a client) are shown disabled for consultants with a tooltip "Only managers can ..."; they are never hidden.
- Dates: EN `16 Oct 2026`, TR `16.10.2026`; relative dates ("2d ago") with the absolute date in a tooltip.
- Minimum touch target 44 px on phone; contrast AA in both themes.

## Contract gaps

Status after the Wave 0 review (orchestrator):

1. **End contact:** resolved. `Contact.endedOn` and `active`; `DELETE /accounts/{id}/contacts/{contactId}` ends the contact; `GET /accounts/{id}/contacts?active=false` lists ended ones; contact roles include ended ones. Screens use `active`, not `includeEnded`.
2. **Blocking items:** resolved. `ProblemDetail.blockingItems` lists the open job orders on a 409 when closing an account.
3. **Facet counts:** deferred. Chips show no counts in Phase 1; revisit after the pilot.
4. **Audit log:** accepted as two sub-tabs (access log, revisions). security-privacy also writes logins to the access log (W1-12).
5. **Invite by e-mail:** deferred. Phase 1 uses an initial password set by the admin.
6. **Remove candidate only in Sourced:** backend-pipeline enforces it on `DELETE /applications/{id}` (409 otherwise) (W1-11).
