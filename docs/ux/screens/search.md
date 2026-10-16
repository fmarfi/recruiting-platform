# Screen: Search results

Requirements: A-14, UX-02, UX-08. Route: `/search?q=&type=&sector=&status=&owner=&lastContact=`. Data: `GET /search?q=&types=&sector=&accountStatus=&jobOrderStatus=&ownerId=&lastContactBefore=&lastContactAfter=&limit=` returns `{ query, accounts, people, jobOrders, totals }`. "Show all" uses the list endpoints (`/accounts`, `/people`, `/job-orders` with `q`).

Question answered: **where is the thing I am looking for?**

## Global search box (every screen)

- Always visible in the top bar; `/` focuses it (laptop). On phone it is a magnifier icon that expands to a full-width field.
- Type-ahead after 2 characters (200 ms debounce): dropdown with up to 3 results per group (Clients, People, Jobs), each with its status badge; arrow keys + Enter; last row "See all results for {q}".
- Recent searches (last 5, stored per user in the browser) appear when the field is focused and empty.
- Turkish-aware matching: case-insensitive and diacritic-insensitive (searching "ozturk" finds "Öztürk", "isci" finds "İşçi", dotted/dotless i handled).

## Wireframe, desktop

```
| Results for "acme"  (11)                                                     |
| Type: (All 11)(Clients 2)(People 6)(Jobs 3)                                  |
| [Sector v] [Status v] [Owner v] [Last contact v]            [x Clear filters]|
|------------------------------------------------------------------------------|
| CLIENTS (2)                                                                  |
|  Acme Retail      Retail - Istanbul - Ayşe          (Active)        3 jobs   |
|  Acme Logistics   Logistics - Bursa - Can           (Lead)                   |
| PEOPLE (6)                                                    [Show all 6]   |
|  Zeynep Aksoy     HR Director, Acme Retail     (Client contact)  12 Oct      |
|  ...                                                                         |
| JOBS (3)                                                      [Show all 3]   |
|  CFO              Acme Retail            (Sourcing)   14 days                |
```

## Wireframe, phone

```
| [ acme            x ]  |
| (All)(Clients)(People)(Jobs)  [Filters (2)]
| CLIENTS                |
| Acme Retail  (Active)  |
| Retail - Istanbul      |
| PEOPLE                 |
| Zeynep Aksoy           |
| HR Dir., Acme (Client contact)
```

Filters on phone open in a bottom sheet with an **Apply** button showing the result count ("Show 8 results").

## Data fields

| Group | Fields |
| --- | --- |
| Clients | Account: `name`, `sector`, `city`, `owner`, `status`, `openJobCount`, `lastActivityAt` |
| People | PersonSummary: `name`, `title`, `company`, `type`, `owner`, `lastContactAt`, `consentStatus` |
| Jobs | JobOrder (`jobOrders`): `title`, `account.name`, `status`, `owner`, `daysOpen`, `activeCandidates` |

Matched text is highlighted in names, companies and titles. Filters (A-14): Sector (clients, jobs via client), Status (two params, `accountStatus` and `jobOrderStatus`; the UI shows one dropdown grouped by kind), Owner, Last contact (any / last 30 / 90 days / more than 90 days), Type (record kind). Filters that do not apply to a group hide that group with a note ("Sector filter hides People").

## Actions

Open any result (1 click); status badge is read-only here; row menu: Log activity (clients, people). Enter in the top box always lands here. Sorting: relevance (exact name match first, then prefix, then contains), then recent activity.

## States

- **Initial (no query):** "Search for a client, person or job" with the last 5 recent searches and 3 hint chips ("A client name", "A person at a company", "A job title").
- **Loading:** three skeleton groups. Results are replaced in place, not blanked, while refining.
- **No results:** "No results for 'acme'. Check the spelling, remove filters, or [Add 'acme' as a client] [Add as a person]" (UX-08). With filters active also shows **Clear filters**.
- **Too many:** each group shows 5 rows with **Show all N**; Show all expands in place with pagination.
- **Error:** inline banner with Retry; the query stays in the box.

## Labels

| Key | EN | TR |
| --- | --- | --- |
| search.placeholder | Search clients, people, jobs | Müşteri, kişi, pozisyon ara |
| search.results | Results for "{q}" | "{q}" için sonuçlar |
| group.accounts / people / jobs | Clients / People / Jobs | Müşteriler / Kişiler / Pozisyonlar |
| filter.type / sector / status / owner / lastContact | Type / Sector / Status / Owner / Last contact | Tür / Sektör / Durum / Sorumlu / Son görüşme |
| btn.clear | Clear filters | Filtreleri temizle |
| btn.showAll | Show all {n} | Tümünü göster ({n}) |
| empty.none | No results for "{q}" | "{q}" için sonuç yok |
| empty.start | Search for a client, person or job | Müşteri, kişi veya pozisyon arayın |
| recent | Recent searches | Son aramalar |
