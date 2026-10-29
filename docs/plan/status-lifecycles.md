# Status lifecycles (W0-02)

Closed sets: the statuses below are the only legal values, and the transitions listed are the only legal moves. Anything else is rejected with a validation error (HTTP 409/422 is the architect's call). Enum names are `UPPER_SNAKE` and are the contract; labels are display text.

Baseline: the status lists of the demo (`demo/src/main/java/com/recruiting/demo/domain/`). Deviations are listed in "Changes from the demo baseline" at the end.

## Conventions

- **Roles:** `consultant`, `manager`, `admin` (NFR-05). `manager` and `admin` can do everything a consultant can.
- **Owner:** the account's responsible consultant (A-01). Ownership does not restrict status changes: for the pilot any consultant may change any record (ASSUMPTION until the client answers). Manager-only moves below stay manager-only.
- **Every transition is logged** (A-02, A-08): from, to, user, timestamp, reason if any. The log is append-only.
- **Reason:** free text, trimmed, 3 to 500 characters (length counted after trimming). A missing or blank reason on a transition that requires one is a validation error. The reason is stored on the status-change record and shown in the timeline.
- **No self-transitions:** moving to the current status is rejected.
- **No AI moves:** a status change is always a human action (NFR-06).
- **Role rules are ASSUMPTION** until the client answers "Which statuses do they use today ... and who may change them?" (requirements.md, open questions). They are a safe default: any consultant moves any record; managers and admins additionally close and reopen.

Tone values are semantic hints for the badge colour (ux-designer maps them to tokens): `neutral`, `info`, `progress`, `good`, `warn`, `bad`, `muted`.

## 1. Account status (A-02)

Initial status on creation: `LEAD`.

| Enum | EN | TR | Tone | Meaning |
| --- | --- | --- | --- | --- |
| `LEAD` | Lead | Aday müşteri | neutral | Prospect the firm is talking to |
| `PROPOSAL_SENT` | Proposal sent | Teklif gönderildi | info | Proposal out, awaiting an answer |
| `ACTIVE` | Active | Aktif | good | Client signed, work ongoing |
| `ON_HOLD` | On hold | Beklemede | warn | Paused by client or firm |
| `CLOSED` | Closed | Kapalı | muted | Relationship closed for now |

ASSUMPTION: this list is the demo's guess and the requirement's example ("lead, proposal sent, active, on hold, closed"). Depends on open question "Which statuses do they use today".

| From | To | Who | Reason |
| --- | --- | --- | --- |
| `LEAD` | `PROPOSAL_SENT` | consultant, manager, admin | no |
| `LEAD` | `CLOSED` | manager, admin | yes |
| `PROPOSAL_SENT` | `ACTIVE` | consultant, manager, admin | no |
| `PROPOSAL_SENT` | `ON_HOLD` | consultant, manager, admin | yes |
| `PROPOSAL_SENT` | `LEAD` | consultant, manager, admin | no |
| `PROPOSAL_SENT` | `CLOSED` | manager, admin | yes |
| `ACTIVE` | `ON_HOLD` | consultant, manager, admin | yes |
| `ACTIVE` | `CLOSED` | manager, admin | yes |
| `ON_HOLD` | `ACTIVE` | consultant, manager, admin | no |
| `ON_HOLD` | `CLOSED` | manager, admin | yes |
| `CLOSED` | `LEAD` | manager, admin | yes |

Not allowed (examples tests must cover): `LEAD` to `ACTIVE`, `LEAD` to `ON_HOLD`, `CLOSED` to anything except `LEAD`, `ON_HOLD` to `LEAD` or `PROPOSAL_SENT`.

Guards:
- Closing an account (`CLOSED`) is blocked while it has job orders in an open status (`OPEN`, `SOURCING`, `SHORTLIST_SENT`, `INTERVIEWING`, `OFFER`). The user must fill or cancel them first. ASSUMPTION (avoids orphaned pipelines).
- Putting an account `ON_HOLD` does not change its job orders; it only shows a warning on them. ASSUMPTION.
- Accepting a proposal does not change the account status automatically. The UI may suggest moving the account to `ACTIVE`. ASSUMPTION.
- Organisation roles (since contract 1.3.0, R63; `OrganisationRole`, not a lifecycle): the account status applies to organisations with the CLIENT or PROSPECT role. CLIENT is added automatically when the status becomes `ACTIVE` and when a job order is created for the organisation; removing CLIENT while it has open job orders is refused (409 `TRANSITION_GUARD_FAILED`, the job orders as blocking items), like closing the account.

## 2. Proposal status (A-05)

Initial status on creation: `DRAFT`.

| Enum | EN | TR | Tone | Meaning |
| --- | --- | --- | --- | --- |
| `DRAFT` | Draft | Taslak | neutral | Being written |
| `SENT` | Sent | Gönderildi | info | Sent to the client |
| `ACCEPTED` | Accepted | Kabul edildi | good | Accepted by the client (final) |
| `REJECTED` | Rejected | Reddedildi | bad | Rejected by the client |

| From | To | Who | Reason |
| --- | --- | --- | --- |
| `DRAFT` | `SENT` | consultant, manager, admin | no |
| `SENT` | `ACCEPTED` | consultant, manager, admin | no |
| `SENT` | `REJECTED` | consultant, manager, admin | yes (client's reason, as far as known) |
| `SENT` | `DRAFT` | consultant, manager, admin | no (recall for rework) |
| `REJECTED` | `DRAFT` | consultant, manager, admin | no (rework) |

`ACCEPTED` is final. Commercial terms (fee model, amount, currency, validity) are editable only in `DRAFT`; after `SENT` they are read-only, to keep what the client saw. ASSUMPTION; depends on open question about what is invoiced (retainer, success fee).

## 3. Job order status (A-06)

Initial status on creation: `OPEN`.

| Enum | EN | TR | Tone | Meaning |
| --- | --- | --- | --- | --- |
| `OPEN` | Open | Açık | neutral | Role agreed, sourcing not started |
| `SOURCING` | Sourcing | Aday arama | info | Researching and sourcing candidates |
| `SHORTLIST_SENT` | Shortlist sent | Kısa liste gönderildi | progress | Shortlist sent to the client |
| `INTERVIEWING` | Interviewing | Mülakatlar | progress | Client is interviewing |
| `OFFER` | Offer | Teklif | progress | Offer out to a candidate |
| `FILLED` | Filled | Kapatıldı | good | A candidate was placed (final) |
| `CANCELLED` | Cancelled | İptal | muted | Search cancelled, can be reopened |

"Open job" (dashboard, A-09, UX-01) means any status except `FILLED` and `CANCELLED`.

| From | To | Who | Reason |
| --- | --- | --- | --- |
| `OPEN` | `SOURCING` | consultant, manager, admin | no |
| `OPEN` | `CANCELLED` | consultant, manager, admin | yes |
| `SOURCING` | `SHORTLIST_SENT` | consultant, manager, admin | no |
| `SOURCING` | `CANCELLED` | consultant, manager, admin | yes |
| `SHORTLIST_SENT` | `INTERVIEWING` | consultant, manager, admin | no |
| `SHORTLIST_SENT` | `SOURCING` | consultant, manager, admin | no |
| `SHORTLIST_SENT` | `CANCELLED` | consultant, manager, admin | yes |
| `INTERVIEWING` | `OFFER` | consultant, manager, admin | no |
| `INTERVIEWING` | `SOURCING` | consultant, manager, admin | no |
| `INTERVIEWING` | `CANCELLED` | consultant, manager, admin | yes |
| `OFFER` | `FILLED` | consultant, manager, admin | no (guard below) |
| `OFFER` | `INTERVIEWING` | consultant, manager, admin | no |
| `OFFER` | `CANCELLED` | consultant, manager, admin | yes |
| `CANCELLED` | `OPEN` | manager, admin | no |

`FILLED` is final.

Guards:
- `OFFER` to `FILLED` requires at least one application on the job order in stage `PLACED`. ASSUMPTION: one job order may have several placements only if the role has more than one opening; Phase 1 treats a job order as one opening, with an optional `openings` count (staffing, B-10) left to the architect.
- Moving a candidate to `PLACED` does not change the job order automatically; the UI may suggest `FILLED`.
- When a job order becomes `FILLED` or `CANCELLED`, applications still in an active stage stay as they are, and the board becomes read-only. Moves on a closed job order are rejected. ASSUMPTION.
- Creating a job order is rejected if the account is `CLOSED`.

## 4. Pipeline stage (A-07)

A pipeline stage belongs to an application (one candidate on one job order). Initial stage on adding a candidate: `SOURCED`. In Phase 1 candidates are people of type candidate from the network (A-12).

| Enum | EN | TR | Tone | Meaning |
| --- | --- | --- | --- | --- |
| `SOURCED` | Sourced | Bulundu | neutral | Found and added to the job order |
| `SCREENED` | Screened | Ön görüşme | info | CV screened, first call done |
| `PRESENTED` | Presented | Müşteriye sunuldu | progress | Presented to the client |
| `CLIENT_INTERVIEW` | Client interview | Müşteri mülakatı | progress | Interviewing with the client |
| `OFFER` | Offer | Teklif | progress | Offer made |
| `PLACED` | Placed | Yerleşti | good | Placed with the client (final) |
| `REJECTED` | Rejected | Elendi | bad | Out of this process, with a reason |

Order of active stages: `SOURCED` < `SCREENED` < `PRESENTED` < `CLIENT_INTERVIEW` < `OFFER` < `PLACED`. Board columns follow this order, `REJECTED` last.

Rules:
1. Forward to any later active stage (skipping is allowed, because consultants often present a known candidate directly). No reason. Exception: `PLACED` is reachable only from `OFFER`.
2. Back one stage. No reason.
3. To `REJECTED` from any active stage except `PLACED`. Reason required.
4. From `REJECTED` back to `SOURCED` or `SCREENED`. No reason; the earlier rejection reason stays in the history.
5. `PLACED` is final.

Resulting move table (active stage rows):

| From | To | Reason |
| --- | --- | --- |
| `SOURCED` | `SCREENED`, `PRESENTED`, `CLIENT_INTERVIEW`, `OFFER` | no |
| `SOURCED` | `REJECTED` | yes |
| `SCREENED` | `SOURCED` (back) ; `PRESENTED`, `CLIENT_INTERVIEW`, `OFFER` (forward) | no |
| `SCREENED` | `REJECTED` | yes |
| `PRESENTED` | `SCREENED` (back) ; `CLIENT_INTERVIEW`, `OFFER` (forward) | no |
| `PRESENTED` | `REJECTED` | yes |
| `CLIENT_INTERVIEW` | `PRESENTED` (back) ; `OFFER` (forward) | no |
| `CLIENT_INTERVIEW` | `REJECTED` | yes |
| `OFFER` | `CLIENT_INTERVIEW` (back) ; `PLACED` | no |
| `OFFER` | `REJECTED` | yes |
| `REJECTED` | `SOURCED`, `SCREENED` | no |
| `PLACED` | none | – |

Who: any consultant, manager, admin for every move above, including reinstating from `REJECTED`. ASSUMPTION; depends on "who may change them".

Other rules:
- The same person cannot be added twice to the same job order (even if `REJECTED`; reinstate instead).
- A person may be in the pipelines of several job orders at once (same CV, different clients).
- The rejection reason is stored on the application and in the status-change record; on reinstatement the field is cleared on the application but kept in history.
- Adding a candidate to a `FILLED`/`CANCELLED` job order is rejected.
- Consent (S-01) does not block pipeline moves in Phase 1. ASSUMPTION: show a consent warning on the card instead; to be reviewed with the KVKK lawyer. *Superseded by contract 1.2.0 (ADR 0011): every move into a client-facing stage runs the sharing guard below.*

Guards and side effects since contracts 1.2.0 and 1.3.0 (the move table above is unchanged; these only refuse or enrich a legal move):

| # | When | Rule | Error |
| --- | --- | --- | --- |
| G1 | Every move into a client-facing stage (`PRESENTED`, `CLIENT_INTERVIEW`, `OFFER`, `PLACED`; `PipelineStage.sharedWithClient()`) | Sharing guard: valid consent (1.2.0) | 409 `CONSENT_REQUIRED` (no override) |
| G2 | First move into a client-facing stage | Off-limits guard; MANAGER/ADMIN may override with `offLimitsOverrideReason` (1.2.0) | 409 `TRANSITION_GUARD_FAILED` (`OFF_LIMITS_RULE` items) |
| G3 | From the longlist (`SOURCED`, `SCREENED`) into a client-facing stage (1.3.0, shortlist guard) | `rating` set and `rationale` at least 20 characters (in the request or already on the application) | 409 `TRANSITION_GUARD_FAILED` with `errors` |
| G4 | Any move while the job order or the person is archived (1.3.0) | Archived records are read-only | 409 `ARCHIVED` |
| S1 | `OFFER` → `PLACED` | Preferred path is `POST /applications/{id}/placement` (1.3.0): same guards, plus the placement row in the same transaction. `PATCH .../stage` to `PLACED` still works for 1.2 clients but creates no placement | – |
| S2 | Client feedback with outcome `DECLINE` (1.3.0) | Moves the application to `REJECTED` with the decline text as reason and `closeReason = CLIENT_DECLINED`; allowed from any active stage except `PLACED` | 409 `ILLEGAL_TRANSITION` from `PLACED` |
| S3 | Every move to `REJECTED` (1.3.0, R65) | Stores the coded `closeReason` (`CloseReason`, default `OTHER`) beside the free-text reason; reinstatement clears both on the application (history keeps them) | 400 when `closeReason` is sent with another target |
| S4 | Move to `SCREENED` (1.3.0, R65) | Sets `outreachStatus = SCREENED` | – |

Lists and rank (1.3.0, ADR 0013): `SOURCED`/`SCREENED` are the **longlist**, the client-facing stages the **shortlist** (`PipelineStage.candidateList()`), `REJECTED` is on no list. Only active applications are ranked: `PLACED` is shown on the shortlist without a rank, after the ranked ones. `OutreachStatus` (`TO_CALL`, `CALLED`, `INTERESTED`, `NOT_INTERESTED`, `NOT_NOW`, `SCREENED`) is a free label, not a lifecycle: any value may follow any other.

## 5. Consent status (S-01)

Per person. Initial status: `NOT_SENT`. Changes only through consent requests and recorded decisions, never by a direct edit. Only `GIVEN` allows matching and outreach.

| Enum | EN | TR | Tone | Meaning |
| --- | --- | --- | --- | --- |
| `NOT_SENT` | Not requested | İstenmedi | neutral | No consent request sent yet |
| `SENT` | Requested | İstendi | info | Request sent, no answer yet |
| `GIVEN` | Given | Onay verdi | good | Explicit consent given |
| `REFUSED` | Refused | Reddetti | bad | Consent refused |
| `WITHDRAWN` | Withdrawn | Geri çekti | warn | Given earlier, then withdrawn |

| From | To | Trigger |
| --- | --- | --- |
| `NOT_SENT` | `SENT` | Consent request e-mail sent |
| `NOT_SENT` | `GIVEN` | Consent collected in person, on paper or by phone and recorded |
| `NOT_SENT` | `REFUSED` | Refusal collected outside the e-mail and recorded |
| `SENT` | `GIVEN` | Person confirms via the link |
| `SENT` | `REFUSED` | Person refuses via the link |
| `GIVEN` | `WITHDRAWN` | Person withdraws via the link, or withdrawal recorded |
| `REFUSED` | `SENT` | New request sent |
| `REFUSED` | `GIVEN` | Person changes their mind |
| `WITHDRAWN` | `SENT` | New request sent |
| `WITHDRAWN` | `GIVEN` | Person consents again |

No self-transitions. Re-sending a request while `SENT` (reminder or resend) is a logged action, not a status change; renewing a `GIVEN` consent likewise stores a new consent record without changing the status. No reason required. Decisions arrive from the person via the link; recording a decision or sending a request manually is open to consultant, manager, admin.

## 6. Data-subject request status (S-04)

Initial status on logging: `RECEIVED`. Types (`DataSubjectRequestType`): `ACCESS`, `CORRECTION`, `DELETION`, `OTHER`. The controller has 30 days from receipt to answer (KVKK art. 13).

| Enum | EN | TR | Tone | Meaning |
| --- | --- | --- | --- | --- |
| `RECEIVED` | Received | Alındı | neutral | Logged, not yet handled |
| `IN_PROGRESS` | In progress | İşlemde | progress | Being handled |
| `COMPLETED` | Completed | Tamamlandı | good | Answered and carried out (final) |
| `REJECTED` | Rejected | Reddedildi | bad | Refused with a written reason (final) |

| From | To | Who | Reason | Trigger |
| --- | --- | --- | --- | --- |
| `RECEIVED` | `IN_PROGRESS` | manager, admin | no | Handling starts |
| `RECEIVED` | `REJECTED` | manager, admin | yes | Request refused |
| `IN_PROGRESS` | `COMPLETED` | manager, admin | no | Answer sent; export, correction or deletion done |
| `IN_PROGRESS` | `REJECTED` | manager, admin | yes | Request refused |

Any consultant may log (create) a request; only manager and admin change its status. `COMPLETED` and `REJECTED` are final.

## Machine-readable summary (for enums and tests)

```yaml
account:      {initial: LEAD, final: [], states: [LEAD, PROPOSAL_SENT, ACTIVE, ON_HOLD, CLOSED]}
proposal:     {initial: DRAFT, final: [ACCEPTED], states: [DRAFT, SENT, ACCEPTED, REJECTED]}
job_order:    {initial: OPEN, final: [FILLED], states: [OPEN, SOURCING, SHORTLIST_SENT, INTERVIEWING, OFFER, FILLED, CANCELLED]}
pipeline:     {initial: SOURCED, final: [PLACED], states: [SOURCED, SCREENED, PRESENTED, CLIENT_INTERVIEW, OFFER, PLACED, REJECTED]}
consent:      {initial: NOT_SENT, final: [], states: [NOT_SENT, SENT, GIVEN, REFUSED, WITHDRAWN]}
data_subject: {initial: RECEIVED, final: [COMPLETED, REJECTED], states: [RECEIVED, IN_PROGRESS, COMPLETED, REJECTED]}
reason_required_to:
  account: [ON_HOLD, CLOSED, "LEAD (only from CLOSED)"]
  proposal: [REJECTED]
  job_order: [CANCELLED]
  pipeline: [REJECTED]
  data_subject: [REJECTED]
close_reason_on:            # since 1.3.0 (R65), coded CloseReason, default OTHER
  pipeline: [REJECTED]
guards:                     # since 1.2.0 / 1.3.0; see section 4 table G1-G4
  pipeline:
    into_shared_with_client: [CONSENT_REQUIRED, OFF_LIMITS_first_time_only]
    longlist_to_shortlist: [rating_set, rationale_min_20]
    any_move_when_archived: ARCHIVED
manager_or_admin_only:
  account: ["* -> CLOSED", "CLOSED -> LEAD"]
  job_order: ["CANCELLED -> OPEN"]
  data_subject: ["all status changes"]
```

## Changes from the demo baseline

| # | Item | Demo | This document | Why |
| --- | --- | --- | --- | --- |
| 1 | Status sets | 5 / 4 / 7 / 7 statuses | Unchanged, same enum names, labels, tones | Matches A-02, A-05, A-06, A-07 wording |
| 2 | Account transitions | As listed in the demo | Unchanged | – |
| 3 | Reason on account `ON_HOLD`, `CLOSED`, and `CLOSED` to `LEAD` | none | required | A-02 "every change is logged"; a lost or paused client needs an explanation for the timeline (ASSUMPTION) |
| 4 | Who closes or reopens an account | not defined | manager/admin only | NFR-05 roles; avoids accidental closure (ASSUMPTION) |
| 5 | Reason on proposal `REJECTED` | none | required | Learn why proposals fail (ASSUMPTION) |
| 6 | Reason on job order `CANCELLED` | none | required | Same as above; matches the audit need |
| 7 | Job order `CANCELLED` to `OPEN` | open to anyone | manager/admin only | Reopening is rare and should be deliberate (ASSUMPTION) |
| 8 | Job order `OFFER` to `FILLED` | no guard | needs a `PLACED` application | Keeps the two funnels consistent |
| 9 | Closing an account | no guard | blocked while it has open job orders | Avoids orphaned pipelines |
| 10 | Pipeline `PLACED` | reachable from any later stage by "forward to any" rule | only from `OFFER` | Skipping to placement bypasses the offer step and corrupts reports |
| 11 | Pipeline `REJECTED` from `PLACED` | n/a (final) | explicitly not allowed | Final means final |
| 12 | Closed job orders | not defined | board read-only, no new candidates | Consistency |
| 13 | Role rules | none | per-transition role table | Required by the task; whole table is ASSUMPTION |

| 14 | Guards on client-facing moves | none | consent (1.2.0), off-limits (1.2.0), shortlist rating and rationale (1.3.0), 409 `ARCHIVED` (1.3.0) | KVKK sharing, off-limits agreements, SX-014 |
| 15 | Coded close reason | free text only | `CloseReason` on every rejection (1.3.0, R65) | Funnel reporting |

Nothing was added to or removed from the status enums, so enum names in `contracts/enums/` can follow the demo directly. `OutreachStatus` and `CloseReason` (1.3.0) are labels, not lifecycles.

## Open questions affecting this document

1. "Which statuses do they use today for accounts, proposals, job orders and candidates, and who may change them?" Affects all four lists and every role rule here. Validate with pilot consultants before Gate 2.
2. "What exactly is invoiced, and to whom?" Affects proposal terms (editable fields) and possibly a future "Placed, invoiced" state (out of scope).
3. "Is 2 years retention counted from upload or last contact?" Not a status question, but a rejected or placed candidate's retention clock (S-03) starts from the date chosen.
