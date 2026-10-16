# CRM demo API (frozen contract)

Base path `/api`, JSON only. Status values are enum names (e.g. `PROPOSAL_SENT`). Dates are ISO `2026-10-16`, date-times ISO `2026-10-16T09:30:00`. Fields may be `null` where noted.

Errors are Spring `ProblemDetail` JSON: `{ "status": 409, "title": "Illegal status change", "detail": "Cannot move a job order from Filled to Sourcing." }`. Codes: 404 not found, 409 illegal transition, 400 validation (e.g. rejection without reason, missing name).

## Shared shapes

**AccountSummary** `{ id, name, sector, city, owner, status, since, openJobs: int, lastActivity: datetime|null }`

**JobSummary** `{ id, title, accountId, accountName, location, seniority, status, opened, daysOpen: int, owner, fee, candidates: int (active, i.e. not rejected), stageCounts: { SOURCED: int, ... all 7 stages } }`

**PersonSummary** `{ id, name, title, company, type, city, owner, lastContact: date|null, consent }`

**Action** `{ id, title, due, owner, accountId|null, accountName|null, personId|null, personName|null, overdue: bool, done: bool }`

**ApplicationView** `{ id, jobId, personId, name, title, company, stage, rejectionReason|null, updated, consent }`

**Proposal** `{ id, accountId, title, terms, status, date }`

**TimelineEvent** `{ accountId, at, kind: "status"|"activity"|"proposal"|"candidate"|"job", text, user }`

## Endpoints

| Method | Path | Body | Returns |
| --- | --- | --- | --- |
| GET | `/meta` | | `{ currentUser, users: [string], today, lifecycles: { account, job, stage, proposal }, stageOrder: [stage], activityTypes: [..], personTypes: [..], consentStatuses: [..], sectors: [..] }` where each lifecycle is `{ STATUS: { en, tr, tone, next: [STATUS] } }` and `tone` ∈ neutral, info, progress, good, warn, bad, muted |
| GET | `/home` | | `{ user, today, actions: [Action] (current user, not done, by due date), openJobs: [JobSummary] (current user's open jobs), recentAccounts: [AccountSummary] (5 with latest activity), stats: { activeAccounts, openJobs, candidatesInProcess, placedThisYear } }` |
| GET | `/accounts?q=&status=&sector=&owner=` | | `[AccountSummary]` (all params optional) |
| POST | `/accounts` | `{ name, sector, city, owner? }` | `AccountSummary` (201, status LEAD) |
| GET | `/accounts/{id}` | | `{ account: AccountSummary, contacts: [{ personId, name, title, role, primary, email, consent, lastContact }], jobs: [JobSummary], proposals: [Proposal], timeline: [TimelineEvent] (newest first), actions: [Action] (open), dashboard: { openJobs, candidatesPerStage: { STAGE: int }, avgDaysOpen, nextAction: Action|null } }` |
| PATCH | `/accounts/{id}/status` | `{ status }` | `AccountSummary` |
| GET | `/jobs/{id}` | | `{ job: JobSummary, account: { id, name, status }, applications: [ApplicationView] }` |
| PATCH | `/jobs/{id}/status` | `{ status }` | `JobSummary` |
| GET | `/jobs/{id}/candidates-to-add` | | `[PersonSummary]` (candidates and executives not yet on this job) |
| POST | `/jobs/{id}/applications` | `{ personId }` | `ApplicationView` (201, stage SOURCED) |
| PATCH | `/applications/{id}/stage` | `{ stage, reason? }` | `ApplicationView` (reason required for REJECTED) |
| PATCH | `/proposals/{id}/status` | `{ status }` | `Proposal` |
| GET | `/people?q=&type=` | | `[PersonSummary]` |
| GET | `/people/{id}` | | `{ person: { id, name, title, company, type, email, city, howWeKnow, owner, lastContact, consent, career: [string] }, connections: [{ personId, name, title, company, kind }], accounts: [{ accountId, name, role }], applications: [{ id, jobId, jobTitle, accountId, accountName, stage, updated }], activities: [{ id, type, note, at, user, accountId|null, accountName|null }] (newest first), actions: [Action] }` |
| POST | `/activities` | `{ accountId?, personId?, type, note }` (at least one id) | activity `{ id, accountId, personId, type, note, at, user }` (201) |
| POST | `/actions` | `{ title, due, accountId?, personId? }` | `Action` (201, owner = current user) |
| POST | `/actions/{id}/done` | | `Action` |
| GET | `/search?q=` | | `{ accounts: [AccountSummary], people: [PersonSummary], jobs: [JobSummary] }` |
