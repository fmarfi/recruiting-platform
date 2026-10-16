# Screen: My day (home)

Requirements: UX-01, UX-02, A-10, A-09 (summary), UX-08. Route: `/`. Data: `GET /home` (`MyDay`). The one question this screen answers: **what should I do next?**

## Wireframe, desktop (laptop >= 960 px)

```
+--------+---------------------------------------------------------------------+
| LOGO   | [ Search accounts, people, jobs ... (/) ]            [+ New v] EN|TR |
| My day |---------------------------------------------------------------------|
| Accts  | Good morning, Ayşe                              Fri 16 Oct 2026      |
| Jobs   |                                                                     |
| Network| [Active clients 12] [Open jobs 9] [In process 34] [Placed 2026: 6]  |
|        |                                                                     |
| ------ | NEXT ACTIONS  (7)                         | OPEN JOBS (4)           |
| Settings| +---------------------------------------+ | +---------------------+ |
| Ayşe K |  OVERDUE                                 | | CFO  - Acme Retail  | |
|        |  [ ] Call back CFO candidate  Acme  -2d  | |  (Sourcing) 14 days | |
|        |  TODAY                                   | |  S3 Sc2 P1 CI0      | |
|        |  [ ] Send proposal draft      Nova  today| | HR Dir - Nova Ltd   | |
|        |  UPCOMING                                | |  (Interviewing)     | |
|        |  [ ] Lunch with referrer     Can T.  Mon | +---------------------+ |
|        |  [+ Add action]                          | RECENT ACCOUNTS         |
|        +---------------------------------------+ | Acme Retail  (Active)   |
|        |                                          | Nova Ltd (Proposal sent)|
+--------+---------------------------------------------------------------------+
```

## Wireframe, phone (< 720 px)

```
+--------------------------------+
| [logo]  [ Search...      ] [EN]|
|--------------------------------|
| Good morning, Ayşe             |
| 2 overdue - 1 today            |
|                                |
| NEXT ACTIONS                   |
| ( ) Call back CFO candidate    |
|     Acme Retail  OVERDUE 2d    |
| ( ) Send proposal draft        |
|     Nova Ltd   TODAY           |
| [ + Add action ]               |
|                                |
| OPEN JOBS (4)        see all > |
| CFO - Acme Retail              |
|  (Sourcing)  14 days  34 cand. |
| ...                            |
| RECENT ACCOUNTS (swipe row) >  |
|--------------------------------|
| My day | Accounts | Network | + |
+--------------------------------+
```

Stats strip becomes a 2 x 2 grid below the greeting on phone, collapsed behind "Numbers" to keep actions first (optional, default expanded on tablet).

## Data fields

| Block | Fields (names from `contracts/openapi.yaml`) |
| --- | --- |
| Header | `user`, `today`. Status labels and tones for badges come from `GET /reference-data` |
| Stats | `stats.activeAccounts`, `stats.openJobs`, `stats.candidatesInProcess`, `stats.placedThisYear` (each links to a filtered list) |
| Next actions | NextAction: `id`, `title`, `dueOn`, `owner`, `account`, `person`, `jobOrder`, `overdue`, `done`. Grouped Overdue / Today / Upcoming (today from `today`). `overdueCount` drives the greeting line |
| Open jobs | `openJobOrders[]` (JobOrder): `id`, `title`, `account.name`, `status`, `daysOpen`, `activeCandidates`, `stageCounts` (as mini chips) |
| Recent accounts | `recentAccounts[]` (Account): `id`, `name`, `status`, `lastActivityAt`, `openJobCount` |

## Actions

| Action | Result | Req |
| --- | --- | --- |
| Tick checkbox | `PATCH /next-actions/{id}` with `{ done: true, version }`, row moves to Done with Undo (`done: false`) | A-10 |
| Add action | Dialog: title, due (default tomorrow), link to account/person; `POST /next-actions` (`title`, `dueOn`, `accountId`, `personId`, `jobOrderId`) | A-10 |
| Click action title | Opens linked account or person | UX-02 |
| Click job row | Opens pipeline board | UX-02, UX-04 |
| Click recent account | Opens Account page | UX-02 |
| Click a stat | Opens the matching filtered list (Active clients -> Accounts filtered; Open jobs -> Job orders list) | UX-02 |
| Global search, + New | Available in the top bar | UX-02, UX-06 |
| Snooze (menu on an action) | Move due date +1 day / next Monday / pick date (`PATCH /next-actions/{id}` with `dueOn`) | A-10 |

## States

- **Loading:** skeleton blocks for stats, the actions list (3 rows) and jobs list; no spinner over the whole page.
- **Empty, new user (no accounts yet, UX-08):** one large card: "Welcome. Add your first client to get started." with primary button **Add your first client** and secondary **Import from CSV** (if role allows). Stats strip hidden.
- **Empty actions (has data):** "You are all caught up. Log a visit or set a follow-up after your next call." with **Add action**.
- **Empty open jobs:** "No open jobs. Create a job order inside a client." linking to Accounts.
- **Error:** inline banner at top: "Could not load your day. [Retry]" Blocks that loaded stay visible; failed block shows its own retry.
- **Offline / slow:** banner "Showing saved data from 09:12" only if caching is implemented; otherwise error banner.

## Labels

| Key | EN | TR |
| --- | --- | --- |
| nav.home | My day | Günüm |
| greeting | Good morning, {name} | Günaydın, {name} |
| block.actions | Next actions | Sıradaki işler |
| group.overdue / today / upcoming | Overdue / Today / Upcoming | Gecikmiş / Bugün / Yaklaşan |
| block.jobs | Open jobs | Açık pozisyonlar |
| block.recent | Recent clients | Son müşteriler |
| stat.activeAccounts | Active clients | Aktif müşteriler |
| stat.openJobs | Open jobs | Açık pozisyonlar |
| stat.inProcess | Candidates in process | Süreçteki adaylar |
| stat.placed | Placed this year | Bu yıl yerleşen |
| btn.addAction | Add action | İş ekle |
| empty.title | Add your first client | İlk müşterinizi ekleyin |
| empty.caughtUp | You are all caught up | Her şey güncel |
| err.load | Could not load. Retry | Yüklenemedi. Tekrar dene |
