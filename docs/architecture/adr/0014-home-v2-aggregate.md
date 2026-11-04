# ADR 0014: Contract 1.4 — Home v2 aggregate (consultant preset)

Status: ACCEPTED (2026-11-04, architect) · Extends ADR 0006 (REST), 0013 (lists, brief, confidentiality level). Contract: `contracts/openapi.yaml` 1.4.0 (`MyDay`), `contracts/enums/{TodayItemKind,HomeRiskType,HomeAction,MilestoneSource,SearchHealthFlag}.java`, index note in `contracts/schema.md` (activity).

## Context
Plan §3.2 (R46) puts three blocks on Home: TODAY (what to do), NEEDS YOU (typed risks with one action each) and MY LIVE SEARCHES (health, not counts), loaded from **one aggregate call within 300 ms** with a freshness stamp. R17 asks that search confidentiality is enforced in Home counts. 1.3 `GET /home` (`MyDay`) returns next actions, open job orders, recent accounts and stats; the frontend (`useMyDay.ts`) already uses it with Done/Snooze.

## Decision
1. **Extend `MyDay`, no new path.** All new fields are optional: `generatedAt`, `needsYouCount`, `todayItems`, `needsYou`, `searches`. One call keeps R46 (one aggregate, one loading state, one freshness stamp) and keeps 1.3 clients valid. `today` is already the date, so the list is `todayItems`. Per-card retry (R46) re-calls `GET /home`; per-block endpoints are not worth the extra surface at pilot volume. The partner, researcher and DPO presets get their own fields in later minor versions on the same path.
2. **Only existing data, no new column.** Sources: `next_action` (owner, due_on, done), `activity` (type, occurred_at, job_order_id, account_id, direction, user_id), `application` (stage, stage_changed_at, version), `client_feedback` (recorded_at), person `consent_status` copy, `job_order` (owner, opened_on, status, archive, confidentiality_level), `job_order_team_member`, `search_brief` (longlist/shortlist/target-start dates). Three read indexes are added (schema.md, activity section).
3. **Thresholds are named constants on `HomeRiskType`** (server-side only; the client shows `days`, never recomputes):

| Constant | Value | Rule |
|---|---|---|
| `FEEDBACK_LATE_DAYS` | 5 | Application in PRESENTED ≥ 5 days since `stage_changed_at`, no `client_feedback` recorded since, and no activity on the job order with `account_id` set and `direction` not OUTBOUND since (a nudge does not clear it). |
| `STUCK_IN_STAGE_DAYS` | 21 | Active application in SOURCED or SCREENED with `stage_changed_at` ≥ 21 days ago. |
| `SEARCH_STALLED_DAYS` | 7 | Open job order whose latest activity `occurred_at` and latest application `stage_changed_at` (else `opened_on`) are ≥ 7 days ago. |
| `ACTION_OVERDUE_DAYS` | 3 | Next action overdue ≥ 3 days moves from TODAY to NEEDS YOU (no row appears twice). |
| `NO_SHORTLIST_DAYS` | 30 | `daysOpen` ≥ 30, shortlist count 0, no `shortlist_due_on` (with a due date, MILESTONE_OVERDUE covers it). |
| CONSENT_ON_CLIENT_STAGE | 0 | Active application in a `sharedWithClient()` stage, person consent not GIVEN; `days` since `stage_changed_at`. |

4. **One action per risk, mapped to an existing endpoint** (`HomeAction`): CONSENT_ON_CLIENT_STAGE → REQUEST_CONSENT `POST /people/{id}/consent/requests` (NOT_SENT, SENT) or REMOVE_FROM_SHORTLIST `PATCH /applications/{id}/stage` to REJECTED (REFUSED, WITHDRAWN); FEEDBACK_LATE → LOG_CLIENT_CONTACT `POST /activities`; STUCK_IN_STAGE → MOVE_STAGE `PATCH /applications/{id}/stage`; SEARCH_STALLED → PLAN_NEXT_ACTION `POST /next-actions`; ACTION_OVERDUE → COMPLETE_ACTION `PATCH /next-actions/{id}`. `targetVersion` is sent back for optimistic locking.
5. **Search health**: rows are open, unarchived job orders the user owns or is a team member of. Counts: longlist = active SOURCED+SCREENED; shortlist = PRESENTED+CLIENT_INTERVIEW+OFFER; clientInterview = CLIENT_INTERVIEW. `nextMilestone` = earliest date among the earliest open next action on the job order, `longlist_due_on` (status OPEN), `shortlist_due_on` (OPEN, SOURCING), `target_start_on`; text is rendered client-side from `MilestoneSource` (keys only). One `SearchHealthFlag` per row, first match in STALLED, WAITING_ON_CLIENT, MILESTONE_OVERDUE, NO_SHORTLIST, else ON_TRACK.
6. **R17 confidentiality per list.** Visible set V(user) = job orders with level NORMAL or RESTRICTED, plus CONFIDENTIAL ones the user owns or is a team member of (RESTRICTED limits changes, not reads, per `ConfidentialityLevel`). `searches`, `needsYou` (application- and job-order-based types), `todayItems` EVENTs with a job order and `openJobOrders` use the user's own searches, which are in V by definition, and drop anything anchored on a job order outside V. ACTION rows and ACTION_OVERDUE are the user's own next actions; if one points at a CONFIDENTIAL job order outside V (team changed), its `jobOrder` ref is nulled and the title kept (the user wrote it). `stats` counts exclude job orders (and their applications) outside V; `recentAccounts` leaves out CONFIDENTIAL accounts unless the user owns or is on the team of one of their job orders. MANAGER and ADMIN get no bypass on Home in 1.4 (no new role rules before the pilot, ADR 0013).
7. **300 ms budget (p95, seeded volume)**: one read-only transaction in `pipeline` (`MyDayService`); fixed caps (today 30, needs you 20, searches 30); set-based queries per block (no per-row N+1): the user's job-order ids first, then grouped counts and max dates by `job_order_id`; person name and consent status in one batch through the accounts API. No cache in 1.4; if p95 exceeds 300 ms, add a 60-second per-user cache keyed on `generatedAt`, not a schema change.

## Consequences
- 1.3 clients keep working; the frontend regenerates types and reads the new fields when present.
- EVENT rows only show **logged** meetings and visits: `activity.occurred_at` may not be in the future, so an interview planned for 14:00 is not on Home until logged. A true calendar needs a new column, either `next_action.due_time time null` (preferred: a planned interview becomes a timed next action) or a planned flag on `activity`. Not added in 1.4; needs a product decision (open question).
- The sketch's "3 forwarded mails unmatched" and the "Res" (research) column have no type or count in 1.4; they can follow as `HomeRiskType` and `MySearchHealth` additions.
- Thresholds are code constants; making them settings is a later admin feature.

## Alternatives
- New `GET /home/v2` or per-block endpoints: cleaner split, but two contracts for one screen and three loading states against R46.
- Client-side computation from existing lists: many calls, no confidentiality filtering in one place, misses 300 ms.
