# Screen: Job order and pipeline board

Requirements: A-06, A-07, UX-03, UX-04, UX-06, A-08 (events feed the account timeline), S-01 (consent visible on cards). Routes: `/job-orders/{id}` (board), `/job-orders` (list, see below). Data: `GET /job-orders/{id}` and `GET /job-orders/{id}/applications`.

Question answered: **where is each candidate for this role and who needs a push?**

## Wireframe, desktop

```
| Clients / Acme Retail / CFO                                                  |
| CFO  (Sourcing v)                          [+ Add candidate] [Log activity]  |
| Client: Acme Retail (Active) - Istanbul - Senior - Fee terms 25% - Owner Ayşe      |
| Opened 02 Oct (14 days)        [ Show rejected (2) ]                         |
|------------------------------------------------------------------------------|
| Sourced 3   | Screened 2  | Presented 1 | Client int. 0 | Offer 0 | Placed 0 |
| +---------+ | +---------+ | +---------+ | .............. | ....... | ........ |
| |M. Demir | | |E. Kaya  | | |T. Arslan| | drop here      |         |          |
| |CFO, Y Co| | |VP Fin.  | | |CFO, Z   | |                                    |
| |(Given)  | | |(Sent)   | | |(Given)  | |                                    |
| |12 Oct [>]| | |14 Oct [>]| | |15 Oct [>]| |                                  |
| +---------+ | +---------+ | +---------+ |                                    |
| ...                                                                          |
|------------------------------------------------------------------------------|
| Rejected (hidden by default, expands as 7th column or list):                 |
|   S. Yılmaz - "Salary expectation too high" - 11 Oct                          |
```

Columns: one per `stageOrder` except Rejected, which is a collapsed lane below the board ("Rejected (2)") so the board stays 6 columns wide. Column header = stage badge + count. Cards sort by last updated.

## Wireframe, phone

```
| < Acme Retail                    |
| CFO         (Sourcing v)         |
| Istanbul - Senior - 14 days      |
| [+ Add candidate]                |
|----------------------------------|
| (Sourced 3)(Screened 2)(Pres. 1)>|  <- stage switcher, one column at a time
| +------------------------------+ |
| | M. Demir            [Move v] | |
| | CFO, Y Co                    | |
| | (Consent given)     12 Oct   | |
| +------------------------------+ |
| | E. Kaya ...                  | |
| Rejected (2) >                   |
```

No drag on phone: tap **Move** (or long-press) opens a bottom sheet with the legal stages.

## Card (Application)

Application: `person.name` (links to Person page), `person.title` + `person.company`, consent badge (`person.consentStatus`), `stageChangedAt` date, `rejectionReason` (on rejected), `version`. Card menu: Move to ..., Open profile, Log activity with this person, Remove from job (only in Sourced; `DELETE /applications/{id}`).

## Header (JobOrder)

`title`, `status` (editable badge, legal next only), `account.name` + `account.status`, `location`, `seniority`, `feeTerms`, `owner`, `openedOn`, `daysOpen`, `activeCandidates`, `stageCounts` (shown as board column counts). Edit job via ... menu (`PATCH /job-orders/{id}`: title, location, seniority, description, feeTerms, openedOn, owner; sends `version`). Status via `PATCH /job-orders/{id}/status`; legal moves, reason rules and manager-only moves come from `GET /reference-data` (`lifecycles.jobOrder`). Manager-only moves (for example reopening a Cancelled job) are shown disabled for consultants with the tooltip "Only managers can reopen a job order". Any consultant may edit any job order and move any candidate.

## Actions

| Action | How | Req |
| --- | --- | --- |
| Move candidate | Drag card to legal column; or Move menu; `PATCH /applications/{id}/stage` (`stage`, `reason`, `version`) | A-07, UX-04 |
| Reject with reason | Drop on Rejected lane or menu; reason required (chips plus free text) | A-07 |
| Reopen rejected candidate | Menu on a rejected row: Back to Sourced / Screened | A-07 |
| Add candidate | Button; searchable list from `GET /job-orders/{id}/candidate-suggestions?q=`; `POST /job-orders/{id}/applications` (`personId`) | A-07 |
| Change job status | Header badge | A-06, UX-03 |
| Log activity for the job | Header button, context = account (+ optional candidate) | UX-06 |
| Mark Placed | Move to Placed; prompts "Mark the job as Filled?" (suggest only) | A-06, A-07 |

Drag details: while dragging, illegal columns dim and show a "not allowed" cursor; legal columns show a dashed outline; keyboard users focus a card, press Space, arrow to a column, Space to drop (announced to screen readers). A placed candidate card is final (no drag).

## Job orders list (`/job-orders`)

A flat list of all job orders for 3-click access (My day > Jobs). Columns: Title, Client, Status, Owner, Days open, Active candidates; filters Status (default: open only), Owner (default me), Client. Rows open the board. Data: `GET /job-orders?q=&status=&open=&ownerId=&accountId=&sort=` (default `open=true`; fields `title`, `account.name`, `status`, `owner`, `daysOpen`, `activeCandidates`).

## States

- **Loading:** board skeleton with six column headers and 2 grey cards each.
- **Empty pipeline (UX-08):** columns replaced by one guided card: "No candidates yet. Add the first candidate from your network." with **Add candidate**. Phase 2 will add "Find matches (AI)".
- **Empty column:** dashed "No candidates" (muted), accepts drops.
- **Job Filled/Cancelled:** board read-only with banner; Cancelled shows **Reopen**.
- **Move failed (409 or network):** card snaps back, toast with the server detail; optimistic move is rolled back.
- **Error loading:** banner + Retry; 404 = "This job order does not exist".
- **Concurrent edit:** if the card was moved by someone else, the 409 toast says who and offers Refresh.

## Labels

| Key | EN | TR |
| --- | --- | --- |
| pipeline | Pipeline | Süreç |
| btn.addCandidate | Add candidate | Aday ekle |
| hint.board | Drag a card to change its stage | Aşamayı değiştirmek için kartı sürükleyin |
| btn.move | Move to | Taşı |
| rejected.lane | Rejected | Elenenler |
| reject.title | Reason for rejection | Elenme nedeni |
| reject.chips | Salary expectation / Not interested / Skills mismatch / Client declined / Other | Maaş beklentisi / İlgilenmiyor / Yetkinlik uyumsuz / Müşteri reddetti / Diğer |
| fields | Location / Seniority / Fee terms / Opened / Days open | Lokasyon / Kıdem / Ücret koşulları / Açılış / Açık gün |
| tip.managerOnly | Only managers can {action} | Yalnızca yöneticiler {action} yapabilir |
| empty.pipeline | No candidates yet | Henüz aday yok |
| toast.moved | {name} moved to {stage} | {name} {stage} aşamasına taşındı |
| nav.jobs | Job orders | Pozisyonlar |
