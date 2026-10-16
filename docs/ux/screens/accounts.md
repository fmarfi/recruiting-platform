# Screen: Accounts list and Account page

Requirements: A-01, A-02, A-03, A-04, A-05, A-06, A-08, A-09, UX-03, UX-05, UX-06, UX-08, A-11 (entry point). Routes: `/accounts`, `/accounts/{id}`.

## 1. Accounts list

Question answered: **which clients do we have and where does each stand?** Data: `GET /accounts?q=&status=&sector=&ownerId=&sort=` (`content` + `page`).

### Desktop

```
| Clients (42)                                   [Import CSV] [+ New client]  |
| [ Filter by name...  ] [Status v] [Sector v] [Owner: Me v]   [x Clear]      |
| Status chips:  All 42 | Lead 8 | Proposal sent 5 | Active 21 | On hold 3 | Closed 5
|-----------------------------------------------------------------------------|
| Name              Sector     City      Owner   Status          Jobs  Last act.|
| Acme Retail       Retail     Istanbul  Ayşe    (Active)         3    2d ago  |
| Nova Ltd          Energy     Ankara    Can     (Proposal sent)  0    1w ago  |
| ...                                                       Page 1 of 3  < >  |
```

### Phone (rows become cards)

```
| Clients (42)        [ + ]   |
| [ Filter by name...      ]  |
| (All)(Lead)(Active)(...)  > |  <- horizontal chips, scroll
| Acme Retail     (Active)    |
| Retail - Istanbul           |
| Ayşe - 3 jobs - 2d ago      |
| ----------------------------|
```

Fields: Account `id`, `name`, `sector`, `city`, `owner`, `status`, `openedOn` (client since), `openJobCount`, `lastActivityAt`. Default filter: Owner = Me (toggle to All, because agency management also needs the overview, A-01). Default sort: `lastActivityAt,desc`; header click sorts `name`, `status`, `lastActivityAt`. Status chips double as the status filter; per-status counts are a contract gap (see README), until then chips show no counts. Status labels and tones come from `GET /reference-data`.

Actions: open row; status badge click changes status inline (F2); **+ New client** (F1); **Import CSV** (A-11, manager/admin only); row menu: Log activity, Add action.

States: loading = 8 skeleton rows; empty (no clients at all) = "Add your first client" + Import CSV (UX-08); empty filter result = "No clients match these filters. [Clear filters]"; error = banner with Retry.

## 2. Account page (UX-05: one view)

Question answered: **what is the state of this client and what is next?** Data: `GET /accounts/{id}`, `GET /accounts/{id}/dashboard`, `GET /accounts/{id}/timeline`, `GET /accounts/{id}/contacts`, `GET /job-orders?accountId=`, `GET /proposals?accountId=`, `GET /next-actions?accountId=&done=false`.

### Desktop

```
| Clients / Acme Retail                                                        |
| +--------------------------------------------------------------------------+ |
| | Acme Retail  (Active v)                  [Log activity] [Add action] [...]| |
| | Retail - Istanbul - Owner: Ayşe K. - Client since 03 Mar 2025             | |
| +--------------------------------------------------------------------------+ |
| | Open jobs 3 | Candidates: Sourced 5, Screened 3, Presented 2 | Avg 21 d   | |
| | Next action: Send proposal draft - Fri 16 Oct   [Done]                    | |
| +--------------------------------------------------------------------------+ |
| [ Job orders 3 ] [ Contacts 4 ] [ Proposals 2 ]          | TIMELINE  [All v] |
|--------------------------------------------------------| 16 Oct  Status    |
| CFO               (Sourcing)  14 d  8 cand.  [S3 Sc2 ..]|   Proposal sent ->|
| HR Director       (Interviewing) 30 d  5 cand.          |   Active - Ayşe   |
| [+ New job order]                                       | 14 Oct  Visit     |
|                                                         |   "Met CFO ..."   |
|                                                         | 10 Oct  Candidate |
|                                                         |   M. Demir ->     |
|                                                         |   Presented       |
```

### Phone

```
| < Clients                    |
| Acme Retail    (Active v)    |
| Retail - Istanbul - Ayşe     |
| [Log activity] [Add action]  |
| Next: Send proposal draft ...|
| Open jobs 3 - Avg 21 d       |
| (Jobs)(Contacts)(Proposals)(Timeline)   <- tabs, Timeline is a 4th tab on phone
| CFO       (Sourcing)         |
| 14 d - 8 candidates          |
| ...                          |
```

Sticky bottom bar on phone holds **Log activity** (primary) so a visit can be logged in 2 taps (UX-06).

### Data fields

| Block | Fields |
| --- | --- |
| Header | `account.name`, `status` (editable badge), `sector`, `city`, `owner`, `openedOn` (client since); edit via **...** > Edit (name, sector, city, owner, website, notes; `PATCH /accounts/{id}`, sends `version`) |
| Dashboard strip (A-09) | `dashboard.openJobOrders`, `candidatesPerStage`, `avgDaysOpen`, `placedCount`, `openProposals`, `nextAction` (`title`, `dueOn`, `owner`) |
| Job orders tab | JobOrder: `id`, `title`, `status`, `location`, `seniority`, `owner`, `feeTerms`, `openedOn`, `daysOpen`, `activeCandidates`, `stageCounts` |
| Contacts tab | Contact: `id`, `person` (PersonSummary: `name`, `title`, `email`, `consentStatus` badge, `lastContactAt`), `role`, `primary`. Ended contacts are hidden by default; a **Show ended** toggle lists them with their end date in a muted style. Ended contacts also appear in the person's company history (Person page > Overview) |
| Proposals tab | Proposal: `id`, `title`, `terms`, `feeAmount` + `currency`, `status` (editable badge), `sentOn`, `decidedOn` |
| Timeline | TimelineEntry: `occurredAt`, `kind` (icon; `ACCOUNT_STATUS`, `ACTIVITY`, `PROPOSAL`, `JOB_ORDER`, `CONTACT`, `CANDIDATE`), `summary` (the UI builds TR/EN text from kind, `fromStatus`, `toStatus`, `reason`), `actor`; filter by `kind` |
| Open actions | `GET /next-actions?accountId=` list shown above tabs when present |

### Actions

| Action | Where | Req |
| --- | --- | --- |
| Change account status (legal next only from `GET /reference-data`; moves flagged `reasonRequired` open a reason dialog; Closed is manager-only: for consultants it shows disabled with the tooltip "Only managers can close a client") | header badge | A-02, UX-03 |
| Log activity (call, visit, meeting, e-mail, note) | header button, phone bottom bar | A-04, UX-06 |
| Add action / mark done (`POST /next-actions`; done = `PATCH /next-actions/{id}` with `done: true`) | header button; next-action strip | A-10 |
| New job order, open job | Job orders tab | A-06 |
| Add contact (pick from network or create; `POST /accounts/{id}/contacts`), set role, set primary, **End contact** (sets an end date; the person stays in the network and keeps the history) | Contacts tab | A-03 |
| New proposal, edit terms (while Draft), change status | Proposals tab | A-05 |
| Click timeline event | Opens the related job, person or proposal | A-08 |
| Edit account | ... menu | A-01 |
| Add person to network from contact dialog | Contacts tab | A-12 |

### States

- **Loading:** header skeleton, three tab skeletons; timeline skeleton on the side.
- **New account, no content (UX-08):** each tab has a guided empty state: Job orders "Create the first job order for this client"; Contacts "Add the person you talk to at Acme"; Proposals "Write a proposal when terms are agreed"; Timeline "Nothing yet. Log your first call or visit." Dashboard strip shows "-" values and the next-action slot shows "Set a next action" button.
- **Status Closed:** page shown with a muted banner "This client is closed. [Reopen as Lead]" (button is manager-only: disabled with tooltip for consultants); adding jobs disabled with hint.
- **Error 404:** "This client does not exist or you do not have access. [Back to clients]". **Error 5xx / network:** banner with Retry; cached tab content stays.
- **Illegal transition 409:** inline message under the badge with server `detail`.
- **Close blocked by open job orders (409 `TRANSITION_GUARD_FAILED`):** the error state under the badge reads "This client still has open job orders. Close or cancel them first." followed by the list from `blockingItems` as links to each job order (title, status badge); the status stays unchanged. Needs `blockingItems` on ProblemDetail (contract gap, see README).
- **Owner rule:** any consultant may edit any account, contact or proposal. Manager-only moves (for example Closed, Reopen as Lead) are shown disabled for consultants with the tooltip "Only managers can ..." instead of being hidden.

### Labels

| Key | EN | TR |
| --- | --- | --- |
| nav.accounts | Clients | Müşteriler |
| btn.newAccount | New client | Yeni müşteri |
| btn.import | Import CSV | CSV içe aktar |
| filter.status / sector / owner | Status / Sector / Owner | Durum / Sektör / Sorumlu |
| filter.mine | Mine | Benim |
| col.lastActivity | Last activity | Son hareket |
| tab.jobs / contacts / proposals / timeline | Job orders / Contacts / Proposals / Timeline | Pozisyonlar / Kişiler / Teklifler / Zaman çizelgesi |
| btn.logActivity | Log activity | Hareket kaydet |
| btn.addAction | Add action | İş ekle |
| btn.newJob | New job order | Yeni pozisyon |
| btn.newProposal | New proposal | Yeni teklif |
| btn.addContact | Add contact | Kişi ekle |
| btn.endContact | End contact | Kişiyi sonlandır |
| toggle.showEnded | Show ended | Sona erenleri göster |
| tip.managerOnly | Only managers can {action} | Yalnızca yöneticiler {action} yapabilir |
| err.openJobs | This client still has open job orders | Bu müşterinin hâlâ açık pozisyonları var |
| dash.openJobs | Open jobs | Açık pozisyonlar |
| dash.avgDays | Avg. days open | Ort. açık gün |
| dash.nextAction | Next action | Sıradaki iş |
| empty.accounts | Add your first client | İlk müşterinizi ekleyin |
| empty.filter | No clients match these filters | Filtrelere uyan müşteri yok |
