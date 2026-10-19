# Salesforce gap analysis (pre-AI)

Date: 2026-10-20 · Owner: product-analyst · Scope: what Salesforce Sales Cloud (Lightning) offers that is worth adding before Phase 2 (AI). Our app stays as is.

Sources: web search was thin and partly vendor blogs ([Summer '24 sales notes](https://gettectonic.com/salesforce-summer-24-sales-release-notes/), [Sales Cloud guide](https://zeeg.me/post/salesforce-sales-cloud), [duplicate rules](https://www.datalane.com/post/salesforce-duplicate-rules), [Einstein Activity Capture](https://admin.salesforce.com/blog/2021/ai-for-admins-a-closer-look-at-einstein-activity-capture)). Activity composer, import wizard and relationship-map details come from general product knowledge; verify against Salesforce help before quoting.

"In app?" is based on requirements.md (A-01..A-14, UX, S-01..S-04) and TASKS.md (Wave 1b merged). Where the code might differ from the spec, treat as ASSUMPTION.

Legend: Value for exec search H/M/L · Effort S/M/L · Wave 2 = before AI, Wave 3 = AI phase, Later = backlog, No = skip.

| Salesforce feature | In our app? | Value | Effort | Wave |
| --- | --- | --- | --- | --- |
| Leads (separate pre-account object) | Partly: account status "lead" covers it | L | S | No |
| Accounts, hierarchy (parent/child) | Accounts yes; hierarchy no | M | M | Later |
| Contacts with roles | Yes (A-03, linked to network) | - | - | Done |
| Opportunities with stage path | Equivalent: job orders and proposals with status lifecycle | - | - | Done |
| Path component (guided stage bar with tips per stage) | Partly: badges and board; no guided bar | M | S | Wave 2 |
| Kanban view on any list | Pipeline board only | M | M | Later |
| Activity timeline on records | Account timeline yes (A-08); person page unclear | H | S | Wave 2 |
| Activity composer (log call / note / e-mail / task in one panel) | Partly: quick add (UX-06) | H | S | Wave 2 |
| Tasks with owner, due date, reminders | Yes (A-10, My day) | - | - | Done |
| Calendar view of tasks and events | No | H | M | Wave 2 |
| Events / meetings with attendees | No (activities only) | M | M | Wave 2 (with calendar) |
| Calendar sync (Outlook/Google), Einstein Activity Capture | No | H | L | Later |
| List views with filters | Filters on accounts and search (A-01, A-14) | H | M | Wave 2 |
| Saved / shared list views, pinned lists, column choice | No | H | M | Wave 2 |
| Inline edit in lists | No | M | S | Wave 2 |
| Mass actions (reassign owner, change status, add tag, delete) | No | H | M | Wave 2 |
| Mass e-mail to a list | No | M | M | Later (needs consent check) |
| Reports (tabular, summary, matrix) | No; only account dashboard (A-09) | H | L | Wave 2 (fixed set) |
| Report builder (user-defined) | No | M | L | Later |
| Dashboards (charts, KPI tiles, per-user or team) | Partly: My day and account dashboard | H | M | Wave 2 |
| Scheduled report e-mails | No | L | S | Later |
| Forecasting (weighted pipeline by period) | No | M | M | Later: fee-weighted pipeline in dashboards |
| Revenue / placement fee fields on job order | ASSUMPTION: not modelled | H | S | Wave 2 |
| Relationship map / org chart for accounts | Network links and connection view (A-13) | M | M | Later: org chart from contact reporting line |
| Account teams (several consultants per account) | Single responsible consultant | M | M | Later |
| Record ownership, sharing rules, territories | Roles only; any consultant edits any record | L | L | No for pilot |
| E-mail integration (log e-mail to record, BCC drop box) | No; Spring Mail for consent only | H | L | Wave 2 (manual log + BCC address); sync Later |
| E-mail templates with merge fields (TR/EN) | No | H | M | Wave 2 |
| Notes (rich text, linked to records) | Notes as activity type | M | S | Done |
| Files / attachments on records (CV, proposal PDF) | No (CV upload is B-01) | H | M | Wave 2 |
| File preview, versions | No | L | M | Later |
| Duplicate management (matching + duplicate rules, warn or block) | No (B-04 is Phase 2, candidates only) | H | M | Wave 2 |
| Merge records | No | H | M | Wave 2 (with duplicates) |
| Validation rules, required fields, picklist control | Status sets fixed (ADR 0002); basic validation | M | S | Later |
| Custom fields and page layouts | No | M | L | Later |
| Data import wizard with field mapping and duplicate handling | CSV import, one-off (A-11) | M | M | Wave 2 (mapping + dup check) |
| Data export / report export | NFR-11 only as a requirement; ASSUMPTION partial | M | S | Wave 2 |
| Global search with filters | Yes (A-14) | - | - | Done |
| Recently viewed, favourites | Partly (recent accounts on My day) | L | S | Later |
| Notifications (in-app bell, e-mail digest of due tasks) | No; reminders on My day only | M | M | Wave 2 (digest) |
| Chatter / @mentions / comments | No | L | M | Later |
| Approval processes | Manager-only moves exist | L | M | No |
| Flow / workflow automation (e.g. task on stage change) | No | M | L | Later |
| Audit history / field history | Yes (Envers, access log) | - | - | Done |
| Consent, retention, DSR | Yes (S-01, S-03, S-04); beyond Salesforce standard | - | - | Done |
| Mobile app | Responsive web (UX-09) | M | - | Done |
| Einstein scoring, e-mail drafting, conversation insights | Phase 2 | H | L | Wave 3 |

## Notes for executive search

- A job order is not a sales opportunity: value sits in the candidate pipeline and in the fee, so dashboards should centre on placements, time to fill and stage conversion, not win rate only.
- People are long-lived relationships (candidates become clients and referrers). Timeline, duplicates and merge matter more here than in a normal sales org.
- Every mass action and e-mail feature must respect consent state and the audit log (KVKK). Bulk e-mail must exclude people without a valid basis.
- Attachments hold personal data: they follow retention and DSR deletion rules; store in the firm's own hosting (NFR-04).

## Recommended Wave 2 scope (max 8 items, ordered)

1. **Dashboards and fixed reports** (H/M-L): firm and per-consultant views: open jobs by status, candidates per stage, days open, placements and fees, overdue actions, accounts by status; CSV export. Add fee and expected-fee fields to job orders if missing.
2. **Saved list views and filters** (H/M): column choice, multi-filter, save private or shared, inline edit, on accounts, people, job orders, proposals. Also gives the base for bulk actions.
3. **Bulk actions** (H/M): on any list selection, reassign owner, change status (through the status machine, not around it), add or remove tag, set next action; confirm step and audit entry per record.
4. **Activity timeline and composer on every record** (H/S): one panel on account, person and job order: log call, visit, note, task, and see a merged timeline with filters by type; reuse quick add.
5. **Tasks and calendar view** (H/M): month and week view of next actions and meetings, per user or team, drag to reschedule; due-task e-mail digest.
6. **E-mail templates and logging** (H/M-L): TR/EN templates with merge fields, send through Spring Mail, auto-log to the timeline; BCC address to log external mail. Mailbox sync stays Later.
7. **File attachments** (H/M): CV, proposal PDF, notes on person, account, proposal and job order; size and type limits, virus-scan hook, retention and DSR integration. Prepares Phase 2 CV upload (B-01).
8. **Duplicate detection and merge** (H/M): fuzzy match on name, e-mail, phone, company when creating, importing or on demand; warn, not block; guided merge with audit. Reused by B-04 in Phase 2. Import gets field mapping in the same item.

Deferred on purpose: calendar/mailbox sync, custom fields, report builder, forecasting, account teams, automation flows, Chatter-style comments.

## Open questions

- Are fees or expected revenue on job orders in the pilot's terms (fixed, percent of salary)? Needed for dashboard 1.
- Which e-mail system does the client use (Outlook or Google)? Decides how soon sync is worth it.
- Should shared saved views be manager-only to create?
