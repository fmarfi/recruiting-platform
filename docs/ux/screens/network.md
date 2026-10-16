# Screen: Network list and Person page

Requirements: A-12, A-13, A-03, A-04, A-10, S-01, S-02 (n/a here), S-03, S-04, NFR-01, NFR-02, UX-06, UX-08. Routes: `/network`, `/people/{id}`.

## 1. Network list

Question answered: **who do we know and who should I contact?** Data: `GET /people?q=&type=` (extend with `owner`, `lastContactBefore`, `consent` filters, new).

### Desktop

```
| Network (318)                                          [+ New person]        |
| [ Search name, company...] [Type v] [Owner: Me v] [Not contacted since v] [Consent v]|
| Type chips: All | Executives 90 | Client contacts 60 | Referrers 40 | Candidates 128
|------------------------------------------------------------------------------|
| Name            Title / Company        Type        City   Owner  Last contact Consent|
| Mehmet Demir    CFO, Y Holding         (Executive) Izmir  Ayşe   12 Oct      (Given)  |
| Zeynep Aksoy    HR Dir., Acme Retail   (Client contact) ...                  (Not sent)|
```

### Phone

```
| Network (318)       [ + ]  |
| [ Search ...           ]   |
| (All)(Exec.)(Contacts)(..)>|
| Mehmet Demir  (Executive)  |
| CFO, Y Holding             |
| Izmir - last contact 12 Oct|
| (Consent given)            |
```

Fields: PersonSummary `id`, `name`, `title`, `company`, `type`, `city`, `owner`, `lastContact`, `consent`. Sort default: last contact, oldest first when filter "Not contacted since" is on; otherwise name. Rows with last contact older than 90 days show the date in warn colour (nudge for A-12 "last contact date").

Actions: open, **+ New person** (F7), row menu (Log activity, Add action, Send consent request). States: loading skeleton rows; empty "Your network is empty. Add the first person or import contacts." (UX-08); empty filter result with Clear; error with Retry.

## 2. Person page

Question answered: **what is my relationship with this person and what is the next step?** Data: `GET /people/{id}`.

### Desktop

```
| Network / Mehmet Demir                                                       |
| +--------------------------------------------------------------------------+ |
| | (MD) Mehmet Demir (Executive)           [Log activity] [Add action] [...]| |
| | CFO, Y Holding - Izmir - m.demir@example.com - Owner: Ayşe                | |
| | How we know: Met at CFO Forum 2024, introduced by Can T.                  | |
| | Last contact: 12 Oct 2026 (4 days)       Privacy: (Consent given) [Manage]| |
| +--------------------------------------------------------------------------+ |
| [ Overview ] [ Connections 5 ] [ Clients & jobs 2 ] [ Activity 12 ] [ Privacy ]|
|-----------------------------------------------| NEXT ACTIONS                 |
| Career: CFO Y Holding 2021-; Finance Dir. ... | [ ] Call back - due Mon      |
|                                               | CONNECTIONS (preview)        |
| Activity (newest first)                       | Can T. - introduced him      |
|  12 Oct  Call  "Open to hearing ..." - Ayşe   | Ali V. - colleague           |
|  02 Sep  Meeting  ...                          | [View all] [+ Add connection]|
```

### Phone

```
| < Network                   |
| Mehmet Demir  (Executive)   |
| CFO, Y Holding              |
| [Call][E-mail][Log activity]|  <- tel: and mailto: links
| Last contact 12 Oct (4 d)   |
| (Consent given)             |
| (Overview)(Links)(Jobs)(Activity)(Privacy)
| ...                          |
```

### Data fields

| Block | Fields |
| --- | --- |
| Header | `person.name`, `type` (badge), `title`, `company`, `city`, `email`, `owner`, `lastContact`, `consent`; edit via ... > Edit |
| Overview | `howWeKnow`, `career[]` (company history, A-12), `actions` (open) |
| Connections (A-13) | `connections[]`: `personId`, `name`, `title`, `company`, `kind` |
| Clients and jobs | `accounts[]`: `accountId`, `name`, `role`; `applications[]`: `jobTitle`, `accountName`, `stage` (badge), `updated` |
| Activity (A-04) | `activities[]`: `type`, `note`, `at`, `user`, `accountName` |
| Privacy (S-01, S-03, S-04, NFR-02) | consent status badge, history of notices/consents (text version, date, channel, withdrawal date), retention date, data requests log (new endpoints) |

### Actions

| Action | Detail | Req |
| --- | --- | --- |
| Log activity | Sheet pre-filled with this person (F4) | A-04, UX-06 |
| Add action | Title, due | A-10 |
| Add connection | Pick person, choose kind; shown on both persons; also reachable as "Connections of {company}" from account | A-13 |
| Edit person / change relationship owner | Dialog | A-12 |
| Add to job | Opens job picker (jobs not yet containing the person); creates application at Sourced | A-07 |
| Send consent request | Button when `consent` is NOT_SENT/REFUSED...; confirm dialog shows the notice language (TR/EN) and recipient; sets SENT | S-01 |
| Record consent / withdrawal received | Manual set with date, channel and text version | S-01, NFR-02 |
| Log data request (access, correction, deletion) | Opens a request record with due date; Delete executes anonymisation behind a confirm requiring typing the name (admin) | S-04 |
| See retention | Shows "Retention ends 12 Oct 2028"; banner when within 60 days: "Renew consent or delete" | S-03 |

Connections view (A-13): a list, not a graph, in Phase 1 (graph visualisation is deferred; lists are faster to scan on phone). Group by kind; each row shows the connected person's badge and a link. The company's connections are on the Account page via the Contacts tab.

### States

- **Loading:** header skeleton + tab skeleton.
- **No activity yet (UX-08):** "No contact logged. Log your first call or meeting."
- **No connections:** "Who introduced you? Add the first connection."
- **Consent NOT_SENT or REFUSED/WITHDRAWN:** the header badge is shown in its tone; for REFUSED/WITHDRAWN a banner states "Do not contact for new roles; data scheduled for deletion" and disables Add to job (decision to confirm with legal, flagged in handoff).
- **Error / 404:** standard.

### Labels

| Key | EN | TR |
| --- | --- | --- |
| nav.network | Network | Ağ |
| btn.newPerson | New person | Yeni kişi |
| filter.notContacted | Not contacted since | Şu tarihten beri görüşülmedi |
| field.howWeKnow | How we know them | Nasıl tanışıyoruz |
| field.lastContact | Last contact | Son görüşme |
| field.owner | Relationship owner | İlişki sorumlusu |
| tab.overview / connections / jobs / activity / privacy | Overview / Connections / Clients and jobs / Activity / Privacy | Genel / Bağlantılar / Müşteriler ve pozisyonlar / Hareketler / Gizlilik |
| btn.addConnection | Add connection | Bağlantı ekle |
| btn.addToJob | Add to a job | Pozisyona ekle |
| btn.sendConsent | Send consent request | Onay isteği gönder |
| conn.kind | Knows / Introduced by / Introduced / Colleague / Referrer of | Tanıyor / Tanıştıran / Tanıştırdığı / Meslektaş / Yönlendiren |
| retention | Retention ends {date} | Saklama süresi {date} tarihinde bitiyor |
| empty.network | Your network is empty | Ağınız boş |
