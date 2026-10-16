# Status lifecycles (W0-02)

Closed sets: the statuses below are the only legal values, and the transitions listed are the only legal moves. Anything else is rejected with a validation error (HTTP 409/422 is the architect's call). Enum names are `UPPER_SNAKE` and are the contract; labels are display text.

Baseline: the status lists of the demo (`demo/src/main/java/com/recruiting/demo/domain/`). Deviations are listed in "Changes from the demo baseline" at the end.

## Conventions

- **Roles:** `consultant`, `manager`, `admin` (NFR-05). `manager` and `admin` can do everything a consultant can.
- **Owner:** the account's responsible consultant (A-01). For job orders, proposals and applications, "owner" means the responsible consultant of the parent account.
- **Every transition is logged** (A-02, A-08): from, to, user, timestamp, reason if any. The log is append-only.
- **Reason:** free text, trimmed, 3 to 500 characters. A missing or blank reason on a transition that requires one is a validation error. The reason is stored on the status-change record and shown in the timeline.
- **No self-transitions:** moving to the current status is rejected.
- **No AI moves:** a status change is always a human action (NFR-06).
- **Role rules are ASSUMPTION** until the client answers "Which statuses do they use today ... and who may change them?" (requirements.md, open questions). They are a safe default: consultants move what they own, managers and admins move anything and handle reopening.

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
| `LEAD` | `PROPOSAL_SENT` | consultant (owner), manager, admin | no |
| `LEAD` | `CLOSED` | manager, admin | yes |
| `PROPOSAL_SENT` | `ACTIVE` | consultant (owner), manager, admin | no |
| `PROPOSAL_SENT` | `ON_HOLD` | consultant (owner), manager, admin | yes |
| `PROPOSAL_SENT` | `LEAD` | consultant (owner), manager, admin | no |
| `PROPOSAL_SENT` | `CLOSED` | manager, admin | yes |
| `ACTIVE` | `ON_HOLD` | consultant (owner), manager, admin | yes |
| `ACTIVE` | `CLOSED` | manager, admin | yes |
| `ON_HOLD` | `ACTIVE` | consultant (owner), manager, admin | no |
| `ON_HOLD` | `CLOSED` | manager, admin | yes |
| `CLOSED` | `LEAD` | manager, admin | yes |

Not allowed (examples tests must cover): `LEAD` to `ACTIVE`, `LEAD` to `ON_HOLD`, `CLOSED` to anything except `LEAD`, `ON_HOLD` to `LEAD` or `PROPOSAL_SENT`.

Guards:
- Closing an account (`CLOSED`) is blocked while it has job orders in an open status (`OPEN`, `SOURCING`, `SHORTLIST_SENT`, `INTERVIEWING`, `OFFER`). The user must fill or cancel them first. ASSUMPTION (avoids orphaned pipelines).
- Putting an account `ON_HOLD` does not change its job orders; it only shows a warning on them. ASSUMPTION.
- Accepting a proposal does not change the account status automatically. The UI may suggest moving the account to `ACTIVE`. ASSUMPTION.

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
| `DRAFT` | `SENT` | consultant (owner), manager, admin | no |
| `SENT` | `ACCEPTED` | consultant (owner), manager, admin | no |
| `SENT` | `REJECTED` | consultant (owner), manager, admin | yes (client's reason, as far as known) |
| `SENT` | `DRAFT` | consultant (owner), manager, admin | no (recall for rework) |
| `REJECTED` | `DRAFT` | consultant (owner), manager, admin | no (rework) |

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
| `OPEN` | `SOURCING` | consultant (owner), manager, admin | no |
| `OPEN` | `CANCELLED` | consultant (owner), manager, admin | yes |
| `SOURCING` | `SHORTLIST_SENT` | consultant (owner), manager, admin | no |
| `SOURCING` | `CANCELLED` | consultant (owner), manager, admin | yes |
| `SHORTLIST_SENT` | `INTERVIEWING` | consultant (owner), manager, admin | no |
| `SHORTLIST_SENT` | `SOURCING` | consultant (owner), manager, admin | no |
| `SHORTLIST_SENT` | `CANCELLED` | consultant (owner), manager, admin | yes |
| `INTERVIEWING` | `OFFER` | consultant (owner), manager, admin | no |
| `INTERVIEWING` | `SOURCING` | consultant (owner), manager, admin | no |
| `INTERVIEWING` | `CANCELLED` | consultant (owner), manager, admin | yes |
| `OFFER` | `FILLED` | consultant (owner), manager, admin | no (guard below) |
| `OFFER` | `INTERVIEWING` | consultant (owner), manager, admin | no |
| `OFFER` | `CANCELLED` | consultant (owner), manager, admin | yes |
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

Who: consultant (owner of the account), manager, admin for every move above. `REJECTED` to `SOURCED`/`SCREENED` (reinstating) is also open to the owner consultant. ASSUMPTION; depends on "who may change them".

Other rules:
- The same person cannot be added twice to the same job order (even if `REJECTED`; reinstate instead).
- A person may be in the pipelines of several job orders at once (same CV, different clients).
- The rejection reason is stored on the application and in the status-change record; on reinstatement the field is cleared on the application but kept in history.
- Adding a candidate to a `FILLED`/`CANCELLED` job order is rejected.
- Consent (S-01) does not block pipeline moves in Phase 1. ASSUMPTION: show a consent warning on the card instead; to be reviewed with the KVKK lawyer.

## Machine-readable summary (for enums and tests)

```yaml
account:      {initial: LEAD, final: [], states: [LEAD, PROPOSAL_SENT, ACTIVE, ON_HOLD, CLOSED]}
proposal:     {initial: DRAFT, final: [ACCEPTED], states: [DRAFT, SENT, ACCEPTED, REJECTED]}
job_order:    {initial: OPEN, final: [FILLED], states: [OPEN, SOURCING, SHORTLIST_SENT, INTERVIEWING, OFFER, FILLED, CANCELLED]}
pipeline:     {initial: SOURCED, final: [PLACED], states: [SOURCED, SCREENED, PRESENTED, CLIENT_INTERVIEW, OFFER, PLACED, REJECTED]}
reason_required_to:
  account: [ON_HOLD, CLOSED, "LEAD (only from CLOSED)"]
  proposal: [REJECTED]
  job_order: [CANCELLED]
  pipeline: [REJECTED]
manager_or_admin_only:
  account: ["* -> CLOSED", "CLOSED -> LEAD"]
  job_order: ["CANCELLED -> OPEN"]
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

Nothing was added to or removed from the status enums, so enum names in `contracts/enums/` can follow the demo directly.

## Open questions affecting this document

1. "Which statuses do they use today for accounts, proposals, job orders and candidates, and who may change them?" Affects all four lists and every role rule here. Validate with pilot consultants before Gate 2.
2. "What exactly is invoiced, and to whom?" Affects proposal terms (editable fields) and possibly a future "Placed, invoiced" state (out of scope).
3. "Is 2 years retention counted from upload or last contact?" Not a status question, but a rejected or placed candidate's retention clock (S-03) starts from the date chosen.
