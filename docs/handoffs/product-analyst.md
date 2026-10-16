# Hand-off: product-analyst (Wave 0)

## What changed
- `docs/status-lifecycles.md` (W0-02): closed status sets, transition tables with roles and reason rules for accounts, proposals, job orders, pipeline stages, plus a machine-readable summary and a "Changes from the demo baseline" table.
- `docs/stories/` (W0-01): 28 stories (A-01..A-14, S-01..S-04, UX-01..UX-10) with Given/When/Then criteria, plus `README.md` index. Count: 15 Must, 12 Should, 1 Could (A-11).

## Status lists
Unchanged from the demo: same enum names, EN/TR labels, tones and the same status sets. No status added or removed, so `contracts/enums/` can copy the demo names.

## Deviations from the demo baseline (rules only, not names)
1. Reason required: account to `ON_HOLD`, `CLOSED`, and `CLOSED` to `LEAD`; proposal to `REJECTED`; job order to `CANCELLED`; pipeline to `REJECTED` (as before).
2. Role limits: account to `CLOSED`, account `CLOSED` to `LEAD`, job order `CANCELLED` to `OPEN` are manager/admin only. All other moves: owner consultant, manager, admin.
3. Guards: job order `OFFER` to `FILLED` needs an application in `PLACED`; closing an account is blocked while it has open job orders; no job orders or candidates added under `CLOSED` account or `FILLED`/`CANCELLED` job order.
4. Pipeline `PLACED` is reachable only from `OFFER` (the demo allowed any forward jump).
5. Proposal terms are read-only after `SENT`.
All role and reason rules are ASSUMPTION (open question: statuses used today and who may change them).

## What others must know
- architect: transition service needs (from, to, role, reason) rules; consent statuses (NOT_SENT, SENT, GIVEN, REFUSED, WITHDRAWN) have suggested moves in story S-01 (Notes); data-subject request statuses OPEN/DONE/REJECTED (S-04) and activity types (CALL, VISIT, MEETING, EMAIL, NOTE) and link types (KNOWS, INTRODUCED_BY) are proposals for `contracts/enums/`. Consider an `openings` count on job orders.
- ux-designer: reason dialog needed for the reason-required moves; board is read-only on closed job orders; badge menu must list only legal targets for the user's role (UX-03). UX priorities (Must UX-01..05, Should UX-06..10) are my assumption.
- backend/QA: each story criterion is written to be directly testable; the illegal-move examples are in the lifecycle doc.

## Left / open
- Client answers needed: status lists in use and who changes them; retention clock start; invoicing/proposal terms; reminder channels; whether hiring companies log in.
- No change requests for `docs/TASKS.md`. Suggest marking W0-01 and W0-02 as review.

## Review fixes
- S-02 moved to Phase 2 with B-13 (story and index); channel is `WEBSITE_FORM`.
- Owner rule: any consultant may change any record (ASSUMPTION); manager-only moves unchanged. Updated status-lifecycles.md.
- A-02 AC5 phrased with 409 `blockingItems`; reason rule 3-500 trimmed in conventions, A-02, S-04.
- A-03: "End contact" sets `endedOn`; ended contacts hidden by default with a filter.
- Added sections 5 (consent) and 6 (data-subject request) to status-lifecycles.md from the enums; no self-transitions (resend/renew are actions). Note: ConsentStatus.java still lists SENT->SENT and GIVEN->GIVEN; contracts owner must remove.
- S-01, S-04 aligned with enums and openapi roles (consultant may POST; manager/admin GET/PATCH). A-13 link types use enum names; A-04 already did.
