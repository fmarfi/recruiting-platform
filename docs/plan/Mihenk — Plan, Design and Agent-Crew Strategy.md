# Mihenk — Plan, Design and Agent-Crew Strategy

Oct 29, 2026 · @asim

## Executive summary

Mihenk's compliance spine (consent gate, off-limits with override, exposure history, KVKK texts in TR/EN) is already ahead of the incumbents a Turkish firm would compare it with. The pilot will fail for three other reasons: it cannot hold the firm's real data yet, it cannot produce the shortlist report a client sees, and it is not yet faster than Outlook plus Excel. Every recommendation below is aimed at those three gaps. Numbers in brackets (R1, R2 …) point to the full recommendation further down.

**The ten decisions**

1. **The Search is the product's home.** Cut the navigation from seven areas to four (Home, Searches, Candidates, Clients). Business development and Insights become views inside Clients and Home until month 4. (R1, R2)
2. **A ranked longlist table, not a Kanban.** The search workspace opens on a table with stage, rank, rating and consent per row; the board becomes an optional view. (R3)
3. **A "real data" gate before any feature work counts.** Hosting region, data-processing agreement, security statement, backups and the import of the firm's pool must be done in weeks 1-2, or the pilot runs on demo data and proves nothing. (R10, R11)
4. **The shortlist report is the deliverable.** Generate an editable Word file from the firm's own Word template, a PDF from it, and a blind version guarded by an automated leak test, live by week 5. (R14, R40)
5. **Email in the pilot through a forwarding mailbox in the firm's own Microsoft 365 or Google tenant**, not a US unified-mail API. Full mailbox sync comes in month 3, direct to Microsoft Graph or Gmail. (R26)
6. **Companies become records.** One organisation table for clients, target companies and employers, with a parent group, so off-limits can cover a whole group and the blind report can say "a listed industrial group" instead of a name. (R63)
7. **Fees as data, invoices elsewhere.** Model the engagement terms and instalment triggers; never issue invoices; export invoice-ready items to the firm's accounting tool, which already handles e-Fatura. (R29, R67)
8. **No more big-bang stages.** Stage Structure and Stage UX stop being stages; their checks move into every slice's definition of done. (R6, R8)
9. **Re-crew Claude Code around vertical slices.** One full-stack builder per slice in its own worktree, a contract steward, a read-only reviewer and a browser critic who times the persona task. "Done" means a person-task passed in a browser, with evidence. (R80-R92)
10. **Pilot success is measured, not felt.** Median call log ≤ 10 s, shortlist of 5 ≤ 3 min, client onboarded with complete terms ≤ 6 min, first client report sent from Mihenk by week 5, and at least 80 % of pilot-search activity logged in Mihenk by week 8. (R9)

**Cuts:** volume staffing, the client login portal, dashboards beyond the partner home, warm-path and relationship-strength scoring, assessment integrations, the separate Proposals module, dark mode, phone layouts beyond four screens, and most of ADR 0012 (Feature-Sliced layout, jMolecules, C4 in CI, the `talent` module extraction). (R7)

**Limits of this review:** I read the repository on your computer (state as of the 29 October 03:48 merge), not a running app, so screen behaviour is taken from the two critiques. Market claims rest mostly on review sites and vendor pages, flagged as such in Sources. Every legal or tax point is "verify with counsel".

## 1. Critique of the current plan and design

The current documents are thorough on what to build and weak on what the pilot needs first. The seven-area map, the entity-by-entity build plan and the two big-bang stages all optimise for completeness; a pilot with 2-3 consultants is won or lost on real data, one deliverable and speed. Ranked by impact on a pilot with real searches:

| # | Finding | Evidence | Why it hurts the pilot | Fix |
| --- | --- | --- | --- | --- |
| C1 | No path to real data. Hosting region, data-processing agreement, backups and the security statement sit in "open questions", not in the plan | `work-plan.md` §4 (hosting "decide before pilot"); consultant critique: unauthenticated `curl` returned people; dev auto-login in `local` profile | Without them the firm cannot legally load candidates, so the pilot runs on demo data and proves nothing | R10, R11, R32, R33 |
| C2 | The client deliverable does not exist and is planned last (Wave 2d, task D6) | Consultant critique task 9 scored 1/1/1; deal-ender #1 | For a retained firm the report is the product; a pilot without it ends at week 3 | R14, R40 |
| C3 | The Search is not the centre of gravity. Seven areas give BD and Insights the same rank as Searches | `app-architecture.md` (b) | Consultants live inside 2-4 searches; extra areas dilute the build and the navigation | R1, R2 |
| C4 | The search opens on a drag-and-drop board | Consultant critique "a Kanban for what is really a ranked list with narrative"; Placed column cut off | Ranking, rating and rationale have no home on a board | R3, R65 |
| C5 | The build plan is ordered by CRUD completeness ("turn every A cell into W"), not by the three flows | `app-architecture.md` (g) E1-E7 | Weeks go into proposal edit drawers and DSR screens while the shortlist flow stays broken | R5 |
| C6 | Email and calendar wait for Wave 2e | `work-plan.md` 2e; Product Plan calls mail logging table stakes | Activity stays in Outlook, so Home, alerts and client health stay empty and the app looks dead | R26, R27 |
| C7 | Stage Structure (ADR 0012) and Stage UX run as whole stages before Wave 2c | `work-plan.md` §2 | Two to three weeks with no user-visible progress inside an 8-week window; refactors before the domain is stable get redone | R6, R8 |
| C8 | Companies are strings. Employers, targets and off-limits rules match by name | Consultant critique task 5 ("Vesta" vs "Vesta Industries"); `schema.md` `person.company varchar` | Off-limits leaks on name drift, group-level rules (PB-017) are impossible, blind reports cannot describe an employer safely | R63 |
| C9 | Researcher persona has no role, no call sheet, no target list | Roles are consultant, manager, admin only; SX-003, SX-005 | Research is half of a search's hours; the researcher keeps working in Excel | R16, R72, R74 |
| C10 | Partner home leads with fee-weighted pipeline | `app-architecture.md` (a) partner item 2 | No fee data exists until terms are entered, so the one brass tile is empty for weeks | R18 |
| C11 | Confidentiality of searches is "Should" or later | CO-011 Must, PB-029 Must, but not in any wave | It is the partner's first demo question ("show me as a junior") | R17, R64 |
| C12 | "Done" meant tests pass; agents were split by layer | handout §4; `CLAUDE.md` merge order db → backend → frontend | Interfaces clash at merge, and nobody checks that a person can finish the task | R80-R92 |
| C13 | Brand is right in spirit, heavy in execution | `brand-direction.md` | Midnight rail plus midnight header darkens a third of the screen; "mastermind of the search" contradicts "discreet"; the product mark on client reports is risky while the name is unchecked | R52-R54 |
| C14 | Phone and WCAG ambitions cover every screen from day one | `app-architecture.md` (d) A12 on every route | Right goal, wrong timing: it multiplies the work per screen during the pilot | R19 |

**Assumptions I would challenge**

- *"Phase 1 has no AI."* Keep it, but log every evaluation, shortlist decision and client outcome from week 1 as clean structured data. That data is the training and evaluation set for Phase 2 matching; without it Phase 2 starts from zero.
- *"The firm needs a CRM like Salesforce."* The firm referenced Salesforce for the account model, not for its workflow. The pilot should feel like a search desk, not a sales CRM.
- *"Proposals are a client tab."* In retained search the proposal becomes the engagement terms. Model the terms (R67) and generate the proposal from them; drop Proposals as a separate object.
- *"Consent is collected after upload."* Correct, but the bulk notice to an imported pool of thousands needs a staged campaign (R11), or the firm's domain will be flagged as spam on day one.

**What I would drop now:** the separate BD and Insights areas, the notification centre (a daily digest e-mail and the Home page cover it), the client login portal, org charts for clients, dashboards v1, warm-path and relationship-strength scoring, assessment integrations, the Proposals module, dark mode, and from ADR 0012 the `talent` extraction, Feature-Sliced layout, jMolecules and C4 output in CI.

## 2. A better plan

The 8-week pilot runs in three phases, each ending with a gate the firm can see: real data in (week 2), first client report out (week 5), the firm run from Mihenk (week 8). Week 1 starts the day the data-processing agreement is signed; until then the team builds on synthetic data. Six months follow in three two-month phases.

&#91;embedded content: roadmap · 6 phases, 6 gates\]

Each gate is a check the firm can watch happen; a missed gate moves the next phase, never its criteria. Phases are drawn at equal width, not to scale.

### 2.1 Phases, scope and exit criteria

| Phase | Weeks | Goal | Scope | Exit criteria | Main risk → fallback |
| --- | --- | --- | --- | --- | --- |
| P0 Ready for real data | 1-2 | The firm's own pool lives in Mihenk, safely | Hosting, DPA, security statement, backups, auth hardening (R10); pool import (R11); glossary (R4); edit everywhere for Client, Candidate, Search; quick log v2 (R43); longlist table and bulk add (R3); city, function, seniority filters; shadow search (R12) | DPA signed; staging on the chosen host with TLS and one tested restore; pool imported with consent NOT\_SENT; each consultant logs a call in ≤ 10 s median and adds 10 people to a search in ≤ 20 s, on their own data; zero axe violations on the 5 pilot screens | Hosting decision stalls → run weeks 1-2 on synthetic data plus one anonymised past search; real data waits, the gate moves |
| P1 First shortlist out | 3-5 | A client receives a shortlist made in Mihenk | Brief with criteria (R73); evaluation form (R66); shortlist with rank, rating, rationale and compare (R65, R62); report studio with Word, PDF and blind (R14, R40); client feedback rounds (R68); exposure by client; confidential searches (R17); e-mail forwarding mailbox (R26) | At least one real shortlist sent from Mihenk; blind leak test green; shortlist of 5 in ≤ 3 min median; a junior cannot see a confidential search in lists, search, Home or exports (automated and in the browser) | The firm's report template is complex → match their sample in our default Word template; the consultant finishes it in Word |
| P2 Run the firm | 6-8 | Partners and operations work in Mihenk, not beside it | Client onboarding with engagement terms (R60, R67); fee schedule and triggers; placement and guarantee; invoice-ready export (R29); partner Home (R18); researcher role, target companies, call sheet (R16, R74); organisation groups and group off-limits (R63); privacy desk (R61); daily digest e-mail | One client onboarded with complete terms in ≤ 6 min; placing a candidate computes the fee and the guarantee end; the partner answers 8 of the 12 demo questions in `partner-bd-and-firm-management.md` §H inside the app; go/no-go review held | Fee models differ per client → instalment lines with a trigger enum and free amounts; computed fees stay optional |
| M3-4 Connect | months 3-4 | Logging becomes automatic | Direct Microsoft Graph or Gmail mail and calendar sync; interview scheduling; opportunities view under Clients; e-signature decision (R30); SSO; saved searches with weekly alerts; references; org charts on target companies | Paid contract signed; ≥ 90 % of consultant e-mail with candidates and contacts logged without manual action | Firm's IT blocks app consent in Entra ID → keep the forwarding mailbox, ask IT for admin consent |
| M5-6 Phase 2 AI | months 5-6 | The pool starts to work for the consultant | CV parsing into the structured profile; shortlist suggestions ranked against brief criteria, evaluated on the pilot's own decisions; client share link with feedback; five partner numbers as charts | Parsing accuracy ≥ 95 % on the 10 fields that matter, measured on a held-out set; consultants accept ≥ 30 % of suggested longlist names | Model quality on Turkish CVs → keep humans in the loop; suggestions never auto-add |
| M7-8 Second firm | months 7-8 | Mihenk is a product, not a project | Tenant isolation, onboarding and import tooling for a new firm, Salesforce and Excel migration, billing for Mihenk itself, performance at 100k people | A second firm onboarded in ≤ 5 working days; p95 list load ≤ 300 ms at 100k people | Custom work for firm 1 leaks into the product → every request must fit the glossary and data model, or it waits |

### 2.2 Minimum lovable product per persona

| Persona | The moment they should love | Minimum needed | Not yet |
| --- | --- | --- | --- |
| Consultant | "I logged three calls in thirty seconds between meetings, and my shortlist report was ready before the client call." | Quick log, longlist table, pool filters, shortlist with rank, report studio | Mail sync, calendar, AI suggestions |
| Managing partner | "I see which searches are late and why, and the report we send looks like us." | Search-health Home, report studio with the firm's template, exposure by client, confidential searches, engagement terms | Fee-weighted pipeline chart, forecasts, dashboards |
| Researcher | "My call sheet tells me who to call next, and Enter logs the outcome." | Researcher role, target companies per search, call sheet, bulk add with consent state | Org charts, market maps |
| Data protection and operations lead | "I can prove in two minutes that this candidate consented before the client saw the CV." | Consent evidence view, DSR register with 30-day deadline, export approvals, audit search, invoice-ready export | Incident register, VERBIS inventory export |

### 2.3 Sequencing by dependency

1. **Real-data gate first** (R10): hosting, DPA, auth hardening. Nothing with real names starts before it.
2. **Glossary and organisation identity before import** (R4, R63): companies are matched once, at import, not cleaned up later.
3. **Import, then the shadow search, then filters** (R11, R12): filters are tuned on the firm's real distribution of cities, titles and sectors.
4. **Application extension → shortlist → evaluation → report → feedback** (R65, R66, R14, R68): each step reads the previous one's data.
5. **Organisation groups → group off-limits → conflict check at onboarding** (R63, R79).
6. **Engagement terms → fee schedule → placement → invoice-ready export → money tiles on the partner Home** (R67, R29, R18).
7. **Mail capture → activity outcomes → Home alerts and client health** (R26, R70).

### 2.4 Recommendations for the plan

- **R1** Four areas: Home, Searches, Candidates, Clients. Admin and the privacy desk sit under the avatar menu, visible by role.
- **R2** Opportunities are a saved view inside Clients (filter: status Lead to Negotiation); Insights is the partner Home. Promote either to an area in month 4 only if weekly use shows it.
- **R3** The search workspace opens on a ranked longlist table; the board is a view toggle.
- **R4** Freeze one glossary in TR and EN before any screen work: Client/Müşteri, Contact/Kişi, Search/Arama, Brief/Brif, Target company/Hedef şirket, Longlist/Uzun liste, Shortlist/Kısa liste, Candidate/Aday, Engagement terms/Hizmet koşulları, Placement/Yerleştirme, Guarantee/Garanti. Have a consultant at the firm approve the Turkish words.
- **R5** Re-rank the build plan by the three flows and the report; CRUD screens are built when a flow needs them.
- **R6** From Stage Structure keep two items: enums generated from one YAML into Java, TypeScript and DB checks, and the OpenAPI split per tag. Defer the rest of ADR 0012 to month 7.
- **R7** Cut list as in the summary; the Proposals module is replaced by proposals generated from engagement terms (R67).
- **R8** Stage UX dissolves into each slice's definition of done (R83); one browser audit of the five pilot screens in week 1 sets the baseline.
- **R9** Pilot metrics, measured weekly by the browser critic and the app's own event log: call log median ≤ 10 s, shortlist of 5 ≤ 3 min, client onboarding ≤ 6 min, p95 list load ≤ 300 ms, 3 of 3 consultants active each week, ≥ 80 % of pilot-search activity in Mihenk by week 8.
- **R10** Real-data gate: hosting region chosen and documented, DPA (veri işleyen sözleşmesi) signed, the `dev-auto-login-as` property rejected at startup in any profile but `local`, TOTP two-factor for managers and admins, daily encrypted backups with one tested restore, the security one-pager (CO-017) approved by the firm's counsel.
- **R11** Import the firm's pool in week 2 with a dry run, a duplicate report and rollback by batch (CO-014). Every imported person starts at consent NOT\_SENT; the notice goes out in daily batches of a few hundred from the firm's own domain, starting with people on live searches.
- **R12** Shadow search in week 2: replay one finished, anonymised search end to end with the consultants watching. It is the cheapest usability test there is.
- **R13** Release every Thursday; a 30-minute session with one consultant every Friday; one change list per week, published to the firm.
- **R14** The shortlist report is built from the firm's own Word template (placeholders filled by poi-tl), converted to PDF by a self-hosted Gotenberg container; the blind version passes an automated leak test on every build (SX-022 acceptance).
- **R15** Client feedback is captured per candidate per round, and the client page lists everyone ever presented, with date and outcome.
- **R16** A RESEARCHER role sees longlists, target companies and call sheets, never fees or compensation.
- **R17** Search confidentiality (NORMAL, RESTRICTED, CONFIDENTIAL) ships in P1, enforced in queries, search index, Home counts, timeline, exports and notifications.
- **R18** The partner Home leads with search health (late, stalled, waiting on client) and shows money tiles only once engagement terms exist; until then it says what to enter to see them.
- **R19** Phone scope for the pilot: Home, quick log, candidate record (read), call sheet. Every other screen must not break at 320 px but is not optimised.

### 2.5 Buy, integrate or build

| Capability | Pilot (weeks 1-8) | After the pilot | Why |
| --- | --- | --- | --- |
| Transactional mail (consent notices, reminders) | **R25** Send from the firm's own domain through their Microsoft 365 or Google SMTP relay, with SPF, DKIM and DMARC checked | Same; add bounce handling through the provider's reports | Consent mail from an unknown sender domain looks like phishing and hurts deliverability; the firm is the data controller |
| Mail logging | **R26** A forwarding mailbox in the firm's tenant (log@firm); Mihenk polls it, matches sender and recipients to people and searches, and files the message | Direct Microsoft Graph or Gmail API sync; a unified API such as Nylas only if the firm mixes providers, and then with a transfer assessment | Keeps data inside the firm's tenant; a US processor makes candidate e-mail a transfer abroad under KVKK article 9, with a standard contract to notify within 5 business days ([Cottgroup](https://cottgroup.com/en/blog/work-life/item/standart-sozlesme-bildirim-yukumlulugu-ve-yaptirimlari)); Nylas publishes plans but not regions on its plan page ([Nylas](https://www.nylas.com/?p=50072)) |
| Calendar | **R27** .ics invites generated by Mihenk; interviews stored as events on the search | Graph or Google free-busy and two-way events | Cheap now; sync is a month-3 project |
| Documents (Word and PDF) | **R28** poi-tl fills the firm's .docx templates (Apache-2.0, loops, tables, images, conditional blocks) ([poi-tl](https://github.com/Sayi/poi-tl)); Gotenberg converts DOCX and HTML to PDF in its own container, with PDF/A options ([Gotenberg guide](https://computingforgeeks.com/gotenberg-html-to-pdf-api/)) | Same | Consultants edit reports in Word before they send them; the firm owns its template |
| E-invoicing | **R29** Never issue invoices. Produce invoice-ready items (client legal data, VKN, amount, currency, VAT flag) and export them as Excel | A connector to the firm's accounting tool; Paraşüt, for example, links a public API from its integrations page ([Paraşüt](https://www.parasut.com/entegrasyonlar)) | E-Fatura and e-Arşiv run through the firm's accounting software and its GİB integrator; rebuilding that is out of scope (tax: verify) |
| E-signature | **R30** Upload the signed engagement letter as a PDF; record signed date and signatory | Decide in month 4 between a global e-signature service and a Turkish qualified e-signature; ask counsel which documents need written form | Under Law 5070 a simple e-signature does not satisfy a written-form requirement; only the secured (qualified) signature does ([CMS guide](https://cms.law/en/int/expert-guides/cms-expert-guide-to-e-signatures-in-commercial-contracts/turkey), dated 2020). Whether an engagement letter needs written form is a counsel question |
| Search | **R31** PostgreSQL full text with the Turkish unaccent dictionary plus trigram indexes (already in place) | OpenSearch only if p95 search exceeds 300 ms or the pool passes about 500,000 CVs | One database is cheaper to run and keeps erasure simple |
| Hosting | **R32** A Turkey-based provider or the firm's own infrastructure, chosen with the firm's counsel; document region and sub-processors | Re-evaluate when hyperscale regions open in Türkiye: Google Cloud and Turkcell announced one in November 2025 with no availability date ([Business Wire](https://www.businesswire.com/news/home/20251111239976/en/Turkcell-Announces-Strategic-Partnership-with-Google-Cloud-with-Plans-t)) | Hosting in Turkey avoids the transfer-abroad regime for candidate data (counsel to confirm) |
| Identity | **R33** Own login with TOTP two-factor for managers and admins | SSO with Microsoft Entra ID or Google in month 4 | Leavers lose access the day IT disables their account |

## 3. A better design

The design principle is a search desk, not a CRM: four areas, the Search workspace at the centre, and every other screen one keystroke away from it. Speed is designed in (tables, keyboard, side panel, undo) and compliance is shown as calm, inline rules with one action each, never as a settings page.

### 3.1 Information architecture

&#91;embedded content: information architecture · 4 areas + avatar menu\]

Business development lives as a view inside Clients and Insights as the partner preset of Home until real use justifies their own areas (R2).

- **R41** Global chrome: a 52 px midnight header (mark, command bar Ctrl+K, + New, Quick log `l`, help `?`, TR/EN, avatar). A light left rail (208 px, collapsible to 56 px) with the four areas, then **My live searches** pinned below a divider, so any live search is one click from anywhere (**R42**).
- **R45** Admin and the privacy desk live under the avatar menu: Admin (users and roles, stage and reference lists, templates and branding, import and export, integrations, security statement) and Privacy desk (DSR register, consent campaigns, retention, audit search, export approvals). Visible by role only.
- **R44** URLs carry every tab, filter, sort and selection: `/searches/:id/longlist?stage=screened&sort=rank`, `/candidates?city=Ankara&function=finance&lang=en:C1`, `/clients/:id/terms`. Old routes (`/accounts`, `/job-orders`, `/network`, `/people`, `/settings`) redirect.

### 3.2 Home command centre per persona

One Home route, four role presets, the same three building blocks: **Today** (what to do), **Needs you** (risk with one action each), **Searches** (health, not counts). Every row is a link to a filtered list; every number is days, money or people, never record counts. **R46** The page states its freshness and loads from one aggregate call within 300 ms, with per-card retry.

**Consultant: "what do I do next?"**

```
+-- Good morning, Elif.  4 things need you today.          updated 08:42 --+
| TODAY (7)                                  | NEEDS YOU                     |
| ! Call back K. Toprak · CFO search    [l]  | x Consent refused: S. Aydin   |
|   overdue 1 day           Snooze  Done     |   is on a Shortlist   [Remove]|
| o 14:00 Client interview · A. Demir        | ~ Client feedback 5 days late |
| o Send shortlist · COO search · due Fri    |   COO search          [Nudge] |
| o 3 callbacks from yesterday's call sheet  | @ 3 forwarded mails unmatched |
|                                            |                       [Match] |
+--------------------------------------------+-------------------------------+
| MY LIVE SEARCHES        Res  Long Short ClInt  Day  Next milestone          |
| CFO · Example Gida A.S.  42   18    4     2    31  shortlist due Fri        |
| COO · Ornek Holding      80   25    0     0    12  ! no activity 9 days     |
+----------------------------------------------------------------------------+
| QUICK LOG [ type a name or search...          ]  1-6 outcome · Ctrl+Enter   |
+----------------------------------------------------------------------------+
```

**Managing partner: "is the firm on track?"** Search health first (R18); the money row appears once engagement terms exist and otherwise says "Add terms to 2 live searches to see fees".

```
+-- Firm view · Q4 2026 · all consultants v                updated 08:42 --+
| SEARCHES AT RISK (3)                    | WAITING FOR YOU (2)             |
| COO · Ornek Holding  no activity 9 d    | Off-limits override: Vesta Grp  |
| CMO · Demo Enerji    no shortlist day 41|   requested by Can     [Review] |
| CFO · Example Gida   client silent 5 d  | Export of 140 people   [Review] |
+-----------------------------------------+---------------------------------+
| FEES (brass rule)   Invoiced Q4   Due next 90 d   In guarantee (at risk)   |
|                     TRY 1.9 M     TRY 2.4 M       TRY 0.8 M · ends 12 Jan  |
+----------------------------------------------------------------------------+
| CLIENTS SILENT > 60 DAYS (4)   | SHORTLISTS OUT THIS MONTH: 3 · feedback 1 |
+----------------------------------------------------------------------------+
```

**Researcher: "who do I call next?"** Today's call sheet across searches (name, company, last outcome, script), target companies still to map, and duplicates waiting. Enter logs the outcome and moves to the next row.

**Data protection and operations lead: "are we exposed?"** DSRs by days left (30-day clock), people on client-facing stages without valid consent (must be zero), exports awaiting approval, retention ending in 14 days, bounced consent mails, instalments that became due.

### 3.3 The 10 core screens

Screen 1 is Home (3.2). Screens 2-10 follow. Every screen shares the record header (icon, serif title, status path, 4-6 highlights, one primary action) and the three states: **empty** teaches with one action, **loading** shows skeletons in the final layout (no spinners), **error** keeps the rest of the page usable and offers retry with the error code in words.

**2. Searches list** (`/searches`)

```
Searches   [Live v] [Mine v] [Client v] [Consultant v]   [/ filter]  [+ New search]
+---+---------------------------+--------------+------+-----+-----+-----+--------+
|[ ]| Search                    | Client       | Lead | Day | Long|Short| Health |
|[ ]| Chief Financial Officer   | Example Gida | EK   |  31 |  18 |   4 | ok     |
|[ ]| Chief Operating Officer   | Ornek Hold.  | CY   |  12 |  25 |   0 | ! 9 d  |
|[ ]| CMO  (Confidential)       | Demo Enerji  | EK   |  41 |  30 |   2 | ! late |
+---+---------------------------+--------------+------+-----+-----+-----+--------+
```

Empty: "No live searches. Start one from a client, or press n then s." · Loading: 8 row skeletons · Error: banner with retry; filters stay.

**3. Search workspace, Longlist tab** (default tab; R3)

```
Searches > Example Gida A.S. > Chief Financial Officer            [+ Add people]
Live · day 31 · Lead EK · Researcher DA · Fee: retained thirds  [Send shortlist]
[Longlist] Shortlist  Brief  Research  Client  Activity  Files      Board | Table
+---+----+---------------------+-------------+---------+---------+-----+-------+
|[ ]|Rank| Candidate           | Now         | Stage   | Status  |Rate | Cons. |
|[x]|  1 | Ayse Kaya           | CFO, Vesta  |Screened | Interest| 4.5 | Given |
|[x]|  2 | Mert Ozturk         | Fin.Dir, Ak | Screened| Interest| 4.0 | Sent  |
|[ ]|  - | Selin Aydin         | VP Fin, Lm  | Longlist| x Consent refused: cannot  |
|   |    |                     |             |         |   be presented  [Remove] |
+---+----+---------------------+-------------+---------+---------+-----+-------+
 2 selected:  Move to Shortlist  Request consent  Tag  Next action  Export
```

Status picklist per row (To call, Called, Interested, Not interested, Not now, Screened); Space opens the person in the side panel; Alt+arrows rank. Empty: "Add people from the pool, a CV or a name" with the three buttons · Loading: table skeleton, tab bar live · Error: rows that failed a guard show the reason inline (consent, off-limits, exposure), never a modal.

**4. Shortlist and compare (R62)**

```
[Shortlist 4]   drag or Alt+Up/Down to rank          [Compare 2-5]  [Build report]
1 Ayse Kaya     4.5  "Ran IPO prep at Vesta; strong FX risk record"  ready
2 Mert Ozturk   4.0  "Turnaround CFO; notice 90 d, buy-out possible"  ! consent Sent
3 ...
+-- Compare -----------+---------------+---------------+
|                      | Ayse Kaya     | Mert Ozturk   |
| Brief: IFRS, listed  | 5  led IPO    | 3  private co |
| Brief: FX treasury   | 4             | 4             |
| Expectation (TRY)    | restricted    | restricted    |
| Notice / mobility    | 60 d / Ist.   | 90 d / any    |
| Concerns             | first CFO role| two moves 3 y |
+----------------------+---------------+---------------+
```

Entering the Shortlist needs rating, rank, a rationale of 20+ characters and consent Given (SX-014); a missing item shows as a chip with a jump link. Empty: "Move your best 3-5 from the longlist" · Loading: card skeletons · Error: rank save failed → row snaps back, toast with retry.

**5. Brief and Research tabs**

```
BRIEF                                   RESEARCH: target companies      [+ Add]
Purpose, reporting line, team size      Tier Company          Status   Owner
Must-haves (become scoring rows)        A    Vesta Group      mapped   DA
  1 Listed-company CFO experience       A    Ak Enerji        approach DA
  2 FX treasury                         B    Lima Retail  x OFF-LIMITS (client
Nice-to-haves                                 group until 3/2027) [Why?]
Comp band (encrypted, restricted)       CALL SHEET (12 due)   Enter = log outcome
Timeline: shortlist by 20 Nov           K. Toprak  Vesta  Called  next: Mon
```

Empty: a 6-item checklist ("Write the brief") · Loading: section skeletons · Error: autosaved draft is kept locally and restored.

**6. Candidate pool** (`/candidates`)

```
[ CFO city:Ankara lang:en>=C1 -"audit firm"            ]  Saved: "Finance Ankara"
City [Ankara x]  Function [Finance x]  Seniority [C-level]  Consent [any]  More v
+---+-------------------+----------------------+--------+--------+-------+------+
|[ ]| Name              | Current              | City   | Notice | Cons. | Seen |
|[ ]| Ayse Kaya         | CFO, Vesta Group     | Ankara | 60 d   | Given | 2 cl |
|[ ]| Okan Celik        | Finance Dir, Ak      | Ankara | 90 d   | -     | 0    |
+---+-------------------+----------------------+--------+--------+-------+------+
 238 people · CV text matches highlighted in the side panel     [Upload CVs]
```

Boolean errors are shown under the box ("closing quote missing after audit"). Empty (no query): recent people and saved searches · Empty (no results): the filter chips to remove, one per line · Loading: rows stream in, count shown last · Error: query kept, retry.

**7. Candidate record**

```
Candidates > Ayse Kaya                         [Add to search]  Log  Consent v
CFO, Vesta Group · Ankara · notice 60 d · EN C1 · consent Given until 5/2028
[Profile] Evaluations(2) Searches & exposure(3) Activity Files Privacy
Career               | Evaluation · CFO search (EK, 3 Nov)        [Edit e]
2021- CFO, Vesta     | 4.5 Shortlist · summary, motivation, concerns
2016-21 FinDir, Lima | Criteria: IFRS 5 · FX 4 · Team 4
Compensation: restricted (owner and managers)  [Request access]
```

Empty tabs say what fills them ("Evaluations appear when you interview Ayse for a search") · Loading: header first, tabs lazy · Error: anonymised person shows a neutral notice, never a 404.

**8. Client record**

```
Clients > Example Gida A.S.   Active client · group Ornek Holding   [Log call]
Owner EK · last contact 3 d · 1 live search · terms signed 2 Sep 2026
[Overview] Contacts Searches Exposure(14) Terms & fees Off-limits Activity Files
TERMS: retained thirds, 33% of first-year cash, guarantee 6 months, TRY
  1/3 on signing  invoiced · 1/3 on shortlist  DUE · 1/3 on start  not due
OFF-LIMITS: whole Ornek group, all functions, until 2 Mar 2027 (contract)
```

Empty: onboarding checklist with progress ("4 of 9 items: add billing entity") · Loading: header, then cards · Error: per-card retry.

**9. Report studio** (from Shortlist: Build report; R40)

```
Shortlist report · CFO · Example Gida        Template: Firm standard (TR) v
[x] Cover  [x] Search summary  [ ] Market note  [x] Comparison  [x] Profiles
Version: ( ) Named  (o) Blind  -> aliases Candidate A-D, employers as sector
Candidates: [x] Ayse Kaya  [x] Mert Ozturk (consent Sent: blocked)  ...
+------------------- live preview (page 3 of 11) -------------------------+
| Candidate A · CFO of a listed industrial group · 4.5 · notice 60 days   |
+-------------------------------------------------------------------------+
[Generate Word + PDF]   History: v2 blind, sent 4 Nov to client (logged)
```

Generating takes ≤ 15 s with a step indicator ("filling template, converting to PDF, leak check"). The leak check failing blocks download and names the field. Empty: "Shortlist at least one candidate with consent Given" · Error: "Could not convert to PDF; the Word file is ready" with download.

**10. Privacy desk** (avatar menu, DPO and managers; R61)

```
Privacy desk   DSR 2 open · Consent gaps 0 · Exports waiting 1 · Retention 14 d: 37
+-----------------------------------------------------------------------------+
| DSR-0012 ACCESS  K. Demir   received 21 Oct  due 20 Nov (22 d)  [Prepare pack]|
| DSR-0013 DELETE  M. Yilmaz  received 28 Oct  due 27 Nov (29 d)  [Review]     |
+-----------------------------------------------------------------------------+
Consent campaign: imported pool  1,840 sent · 412 given · 22 refused · 9 bounced
Audit search: [ who viewed Ayse Kaya's compensation in October ]
```

Empty: "No open requests. Requests made by e-mail can be logged here." · Loading: counters first · Error: counters show "unavailable" with retry, never zero (a wrong zero is a compliance risk).

### 3.4 The three flows to optimise

Each flow has a time budget per step, measured by the app's own event log (composer opened → saved) and by the browser critic every week. Times assume a trained consultant on a laptop.

**Flow A · Log a call in under 10 seconds (R43, one composer everywhere)**

1. Press `l` anywhere, or Enter on a call-sheet row. The composer opens with the current person, search and type Call prefilled. *(0.5 s)*
2. If no person is in context, type three letters ("ayş" and "ays" both match, Turkish-folded) and press Enter. *(2 s)*
3. Press one outcome key: 1 Reached, interested · 2 Reached, not now · 3 Voicemail · 4 No answer · 5 Not interested · 6 Wrong number. *(0.5 s)*
4. Optional follow-up chip: `t` tomorrow, `3` in 3 days, `w` next week. Voicemail and No answer propose a retry in 2 days by default. *(0.5 s)*
5. Optional note; it is never required. `@yesterday 16:00` changes the time. *(0-5 s)*
6. Ctrl+Enter saves, the composer closes, a toast offers Undo for 6 s, and the longlist row status updates (Interested, Not now …). *(0.3 s)*

Budget: 4 s without a note, 9 s with six words. Search defaults to the last search this person was logged against.

**Flow B · A shortlist of 5 in under 3 minutes (R59)**

Assumes the five candidates have evaluations, as in the Product Plan proof point.

1. Longlist tab, filter Stage = Screened, sort by rating. *(5 s)*
2. `x` on five rows, then "Move to Shortlist". *(7 s)*
3. A checklist drawer lists each candidate's missing items. Rank is prefilled from rating; the rationale is prefilled from the first sentence of the evaluation summary; consent shows Given, Sent or a blocker with "Send reminder". *(60 s to adjust five rationales)*
4. Fine-tune order with Alt+Up and Alt+Down. *(15 s)*
5. "Build report": the firm's default template and the named version are preselected; toggle Blind if needed. *(10 s)*
6. Generate Word and PDF. *(≤ 15 s)*
7. Open the Word file, check, send. Sending from Outlook with the log mailbox in CC files the report as "Shortlist sent" on the search and updates exposure. *(45 s)*

Budget: about 2 min 40 s.

**Flow C · Onboard a client with complete terms (R60)**

A four-step drawer. Each step saves; the client stays a Prospect until the checklist is green.

1. **Company.** Legal name with organisation matching ("Looks like Example Gıda A.Ş., part of Örnek Holding: use it?"), group parent, sector from the list, HQ city, website. The conflict check runs here and shows hits: current client in the group, active guarantee, off-limits term, our placed candidates working there, a competing search. *(60 s)*
2. **Billing entity.** VKN or foreign tax id, tax office, MERSIS (length check only), billing address and e-mails, e-invoice status, currency. Pasting a signature block fills the fields it recognises. *(60 s)*
3. **Contacts.** Add 1-3 people inline with decision roles (sponsor, hiring manager, HR, finance, signatory), with duplicate detection. *(90 s)*
4. **Engagement terms.** Pick a fee model (retained thirds, percent of first-year cash, flat, hybrid); the firm's default fills percent, fee basis toggles, instalment triggers, guarantee months, payment days, exclusivity and the off-limits term. Upload the signed letter, or mark it pending. *(120 s)*
5. **Activate.** All checklist items green → status Active; the off-limits rule for the group is created automatically; "Start search" opens with client, contacts and terms prefilled.

Budget: 5 min 30 s with the signed letter at hand.

### 3.5 Shared patterns, brand adjustments and design system

**Interaction patterns** (keep the keyboard map in `app-architecture.md` (c); these are the changes and additions)

- **R47** Side panel peek: Space on any row opens a 480 px panel with header, highlights, next action, last five activities and the primary action; Esc returns focus to the row. One panel deep, never stacked.
- **R48** Undo instead of confirm for everything reversible (the F0 primitives already exist). Only anonymise and permanent delete ask for typed confirmation.
- **R49** Guards live on the row, not in a modal: consent, off-limits, exposure and confidentiality show the reason and exactly one action ("Send reminder", "Why?", "Request override"). The server decides; the UI only explains.
- **R50** Bulk results are reported, never silent: "26 added, 4 skipped: no consent · View skipped".
- **R51** `l` logs, `?` shows the shortcut sheet, and every single-key shortcut can be switched off (WCAG 2.1.4).
- **R55** Turkish-proof layouts: labels budget 40 % extra length, buttons never fixed width, a pseudo-locale in tests stretches every string by 40 % and adds İ, ı, Ğ, ş; tables truncate with a tooltip, forms never truncate.
- **R56** Performance is instrumented: `Server-Timing` headers on list endpoints, keyset pagination at 50 rows, prefetch the side panel on hover, and a CI check that fails when a seeded list call exceeds 300 ms p95.
- **R57** Every error code has a TR and EN sentence that says what happened and what to do next ("This person is off-limits until 2 March 2027 because of the Örnek Holding agreement. Ask a partner for an override.").
- **R58** A daily digest e-mail at 08:00 replaces the notification centre for the pilot.

**Brand adjustments** ("Quiet Authority" stays)

- **R52** Midnight on the header only; the left rail becomes ivory (`bg`) with a brass marker on the active item. It halves the dark area and gives tables more visual room.
- **R53** Source Serif 4 for page titles, report headings and the one headline number; record names inside lists and side panels stay Inter. Compact density (36 px rows) is the default for tables, comfortable is an option.
- **R54** Drop "The mastermind of the search"; it is the opposite of discreet. Keep "Executive search, proven." The "Prepared with Mihenk" line on client reports is off by default until the trademark searches (TÜRKPATENT, EUIPO, WIPO; classes 9, 35, 42) are done.

**Design system inventory**

| Component | Status | Used by | Note |
| --- | --- | --- | --- |
| AppShell (header, rail, pinned searches) | Extend | All | Light rail (R52), pinned live searches (R42) |
| CommandBar | Extend | All | Add commands: log, add to search, move stage, new client |
| DataGrid | Extend | Lists, longlist, pool | Rank column, inline status picklist, guard row, keyset paging |
| BulkBar, EditDrawer, InlineEditField, UndoToast, TagInput, HistoryPanel | Exists (F0) | All records | Wire into every slice |
| SidePanel | New | All lists | R47 |
| Composer (quick log and task) | New | Everywhere | Replaces `LogActivityDialog` and `AddActionDialog` |
| RecordHeader and StatusPath | Extend | Records | One primary action per object |
| GuardRow and GuardChip | New | Longlist, shortlist, pool | R49 |
| RestrictedField | New | Compensation, confidential data | Masked value plus "Request access" |
| StepperDrawer and ChecklistCard | New | Client onboarding, brief, shortlist entry | Each step saves |
| OrganisationPicker | New | Clients, employments, targets | Matches aliases and group (R63) |
| MoneyField | New | Terms, compensation, placements | Locale format (₺ 1.250.000 / TRY 1,250,000), currency select |
| CriterionScore and RatingInput | New | Evaluation, compare | 1-5 with labels, number keys |
| RankHandle | New | Shortlist | Drag plus Alt+arrows (WCAG 2.5.7) |
| CompareTable | New | Shortlist | 2-5 columns, "not asked" for gaps |
| BooleanQueryInput | New | Pool | Parser errors under the box, field prefixes |
| DateChips | New | Composer, tasks | t, 3, w |
| ReportPreview | New | Report studio | Page thumbnails, blind toggle |
| TypedConfirm | New | Anonymise, permanent delete | Manager only |
| EmptyState, Skeleton, ErrorBanner | Extend | All | Empty teaches one action; skeleton in final layout |

## 4. Data model review

The person, consent and privacy side of the model is strong; the search, company and commercial side is thin. The biggest structural fix is organisation identity (R63): it must land before the pool import, because companies are matched once at import and are expensive to clean afterwards.

**Rule for every change:** each new column that holds personal data is classified in the same pull request for erasure (ADR 0009), audit and blind-report redaction; a test fails when a person-linked column has no classification.

| # | Change | Gap it closes | Priority |
| --- | --- | --- | --- |
| R63 | Generalise `account` into `organisation` with role flags (client, prospect, target, employer), `parent_id` for the group tree, `aliases[]` and a normalised name; `person_employment.organisation_id`; off-limits rules scoped to an organisation with "include subsidiaries". Needs ADR 0013 | C8; PB-017 group off-limits; SX-003 targets; SX-022 blind employer description | P0 · weeks 1-2 |
| R65 | Extend `application`: `rank`, `rating`, `rationale`, `longlist_status` (To call, Called, Interested, Not interested, Not now, Screened), stages Research → Longlist → Screened → Shortlist → Client interview → Offer → Placed → Closed with `close_reason` | SX-013, SX-014, SX-016; consultant deal-ender 2 | P0 · weeks 1-2 |
| R70 | Extend `activity`: `outcome`, `direction`, attendee people, `job_order_id`, `duration_min`, `source` (manual, e-mail, call sheet), `external_message_id` | SX-030 quick log; A-04 | P0 · weeks 1-2 |
| R73 | `search_brief` (1:1 with the search: purpose, reporting line, team size, encrypted comp band, timeline, target start) and `search_criterion` (must or nice, text, order) | SX-001; scoring rows for SX-017 | P1 · weeks 3-5 |
| R66 | `evaluation` (person, search, evaluator, type, overall rating, recommendation, summary, achievements, motivation, concerns, private notes, include-in-report flags) and `evaluation_score` (criterion, score, comment) | SX-008, SX-009, SX-017; the evaluation form in requirements §4 | P1 · weeks 3-5 |
| R68 | `client_feedback` (application, round, date, client participants, outcome advance/hold/decline, reason codes, text, relay to candidate) | SX-019; PB-023 exposure by client | P1 · weeks 3-5 |
| R69 | `generated_document` (search, kind shortlist/profile/progress/proposal, blind flag, template version, candidate ids, Word and PDF file refs, created by, sent to, sent at) and `export_request` (requester, filter, row count, columns, purpose, approver, expiry) | SX-021, SX-022; CO-009, CO-013 | P1 · weeks 3-5 |
| R64 | `job_order.confidentiality_level` (normal, restricted, confidential) and `search_team_member` (user, role lead/consultant/researcher, sees compensation) | CO-011, PB-029, SX-033 | P1 · weeks 3-5 |
| R71 | `inbound_email` (message id, from, to, subject, received at, matched people and searches, status matched/unmatched/ignored, body retention date) and `mail_log` for every system mail | R26 mail capture; CO-024 | P1 · weeks 3-5 |
| R72 | Roles RESEARCHER, FINANCE and AUDITOR added to `Role`; one permission matrix in code, rendered on an Admin page | CO-015, PB-028; C9 | P1 · weeks 3-5 |
| R74 | `target_company` (search, organisation, tier A/B/C, rationale, status, owner); the call sheet is a view over applications, targets and activities | SX-003, SX-005 | P2 · weeks 6-8 |
| R67 | `agreement` (organisation, kind, signed on, signatory, language, governing law, exclusivity, fee model, percent, fee-basis flags, currency, payment days, guarantee months and start basis, off-limits months, signed file, status), `fee_schedule_item` (trigger, percent or amount, due days, VAT flag, state not due/due/invoiced/paid, invoice number, paid on) and `placement` (start date, base, bonus, encrypted first-year cash, fee, guarantee end, left on, replacement search) | PB-007, PB-008, PB-009, PB-011; CO-020, CO-021 | P2 · weeks 6-8 |
| R78 | Billing entity fields on `organisation`: legal name, tax id, tax office, MERSIS, billing address and e-mails, e-invoice status and alias, default currency | CO-019, PB-014 | P2 · weeks 6-8 |
| R79 | `conflict_check` (organisation, checked by, at, hits, decision, override reason) | PB-018, PB-019 | P2 · weeks 6-8 |
| R75 | `opportunity` (organisation, roles, expected fee, probability, stage, decision date; won → search); `proposal` rows migrate into it and into `generated_document` | PB-004, PB-005, PB-006 | Later |
| R76 | `reference_check` (referee, relationship, status, summary, rating) | SX-010 | Later |
| R77 | `interview` (application, round, scheduled at, participants, ics uid, calendar event id) | SX-018 (pilot uses an activity of type Meeting plus .ics) | Later |

## 5. Claude Code agent-crew plan

Replace the 15 layer-based agents with five roles that work on vertical slices: one person-task, end to end, from contract to browser. A slice is done when the browser critic has watched the task succeed on the merged build within its time budget, not when tests pass.

### 5.1 The crew

| Role | Model and effort | Tools | Owns | Replaces |
| --- | --- | --- | --- | --- |
| Orchestrator (your main session) | Opus, high | All | Slice board, briefs, merges, `docs/plan/STATE.md` | Same; never writes feature code |
| Contract steward | Opus, high | Read, Edit, Write, Grep, Glob, Bash | `contracts/**`, ADRs, i18n key list | architect |
| Slice builder (1-2 at a time) | Sonnet, medium; `isolation: worktree` | Read, Edit, Write, Bash, Grep, Glob | The slice's migration, backend package, frontend feature folder, seed and tests | db-engineer, backend-accounts, backend-pipeline, frontend-shell, frontend-pipeline |
| Reviewer | Opus, high; `memory: project` | Read, Grep, Glob, Bash (git diff only) | Review verdict, recurring-findings memory | code-reviewer; security-privacy becomes a lens it applies to slices with personal data |
| Browser critic | Sonnet, medium; `memory: project` | Playwright MCP or Claude in Chrome, Read, Write | Timed persona tasks, screenshots, axe results, evidence file | qa-engineer's manual checks; ux-designer's audits |
| Seeder | Haiku, low | Read, Write, Bash | Synthetic story data per slice | synthetic-data |

- **R80** A slice is one user task end to end, at most about 800 changed lines and two days of work. If it is bigger, split it by task, never by layer.
- **R81** One builder owns every layer of its slice; two builders never touch the same files on the same day.
- **R82** Contract first, per slice: the steward opens a contract change (OpenAPI operations, schema, enums, events, i18n keys), the reviewer approves it, it is merged and frozen, and only then does the builder start. The builder requests contract changes through its hand-off note, never by editing.

### 5.2 Slices in order

| # | Slice (the person-task) | Phase | Contract | Browser check (budget) |
| --- | --- | --- | --- | --- |
| S1 | Real-data hardening: auth, two-factor, dev-login guard, backups, security page | P0 | Minor | An unauthenticated API call is refused; the DPO opens the security statement |
| S2 | Organisation identity and pool import | P0 | R63 | Import a 500-row file, read the duplicate report, roll the batch back |
| S3 | Glossary, four-area routes, pinned live searches | P0 | None | Reach the CFO search from any page in one click |
| S4 | Quick log composer | P0 | R70 | Flow A (≤ 10 s) |
| S5 | Longlist table, bulk add, pool filters | P0 | R65 | Add 10 Ankara finance people to a search (≤ 20 s) |
| S6 | Edit everywhere for client, candidate, search (F0 primitives) | P0 | None | Fix a typo in a client name, undo it |
| S7 | Brief and evaluation | P1 | R73, R66 | Fill a brief, evaluate a candidate against it (≤ 3 min) |
| S8 | Shortlist and compare | P1 | R65 | Flow B steps 1-4 (≤ 90 s) |
| S9 | Report studio with blind leak test | P1 | R69 | Flow B steps 5-7 (≤ 75 s); leak test green |
| S10 | Client feedback and exposure by client | P1 | R68 | Record feedback after an interview (≤ 30 s); list everyone shown to a client |
| S11 | Confidential searches and search teams | P1 | R64 | As a junior, find no trace of the confidential search |
| S12 | Mail capture mailbox | P1 | R71 | A forwarded mail lands on the right person and search; an unmatched one is matched from Home |
| S13 | Client onboarding, engagement terms, conflict check | P2 | R67, R78, R79 | Flow C (≤ 6 min) |
| S14 | Placement, fee schedule, invoice-ready export | P2 | R67 | Place a candidate; the due instalment appears in the export |
| S15 | Partner Home and daily digest | P2 | None | The partner answers "which searches are at risk and why" (≤ 30 s) |
| S16 | Researcher role, target companies, call sheet | P2 | R72, R74 | Log ten call-sheet outcomes with Enter (≤ 60 s) |
| S17 | Privacy desk | P2 | R69 | Prepare an access-request pack (≤ 3 min); approve an export |

### 5.3 Definition of done (R83)

1. The slice's contract change is merged and frozen before the build starts.
2. Migration, backend, frontend, seed and tests ship in one branch.
3. Integration tests cover every guard (consent, off-limits, confidentiality, roles); one Playwright test runs the persona task with the keyboard only.
4. Zero axe violations on every touched route, in the empty, loaded and error states.
5. TR and EN strings complete; the 40 % pseudo-locale run shows no clipping.
6. p95 of every touched list endpoint ≤ 300 ms on the 100k-person seed.
7. Every new personal-data column is classified for erasure, audit and redaction.
8. Reviewer verdict: no blockers.
9. Browser critic evidence on the merged build: the persona task done in a real browser, the time against the budget, screenshots at 1280 px (and 375 px for the four phone screens), saved in `docs/process/evidence/<slice>.md` (**R84**).
10. A hand-off note under 40 lines; the orchestrator updates `docs/plan/STATE.md`.

&#91;embedded content: slice pipeline · 4 gates, 3 loops back\]

Four gates per slice: the frozen contract, the builder's own stop-hook checks, the review, and the timed browser task on the merged build. Persona Friday feeds regressions back as next week's first brief.

### 5.4 Claude Code set-up

- **R85 Hooks as gates.** Subagent definitions can carry their own hooks, and exit code 2 blocks a tool call, keeps a subagent working on SubagentStop, or refuses a TaskCompleted ([hooks reference](https://code.claude.com/docs/en/hooks), [subagents](https://code.claude.com/docs/en/sub-agents)). Use three: (a) in the builder's frontmatter, a PreToolUse hook on Edit and Write that exits 2 for any path under `contracts/`; (b) a SubagentStop hook on the builder that runs `scripts/slice-check.sh` (tsc, changed vitest files, the module's `mvn -q test`) and exits 2 with the first 30 error lines; (c) a TaskCompleted hook that refuses completion unless the slice's evidence file exists and says `Result: PASS`.
- **R86 Briefs under 60 lines**, from the template below. They name exact files and operation ids, never whole documents; code context comes from `graphify query "<question>" --budget 1500`.
- **R87 Three project skills** in `.claude/skills/`: `slice-brief` (builds the brief from the slice table, the frozen contract diff and a graphify query), `browser-check` (loads the persona script, drives the browser, times the task, runs axe, writes the evidence file), `ship-slice` (runs the definition-of-done checks, then the merge and worktree clean-up).
- **R88 Commit a slim shared `CLAUDE.md`** (project facts, rules, glossary, a link to the definition of done; under 120 lines) together with `.claude/agents/` and `.claude/skills/`. Today `CLAUDE.md` and `.claude/` are git-ignored with a copy on Google Drive, so every machine can drift. Keep personal orchestration notes in a local, ignored file.
- **R89 Agent teams only for review and critique.** They are experimental, off by default, and every teammate is a separate instance with its own token bill; the docs recommend them for research and review rather than same-file implementation ([agent teams](https://code.claude.com/docs/en/agent-teams)). Use a three-lens review team (security, UX, domain) at each phase gate; build with subagents in worktrees.
- **R90** At most two builders in parallel plus the critic; merge daily; slices that share files run in sequence.
- **R91** Give the reviewer and the critic `memory: project` so recurring findings accumulate in `.claude/agent-memory/<name>/MEMORY.md`, and tell them in their prompts to read it first and update it last.
- **R92 Persona Friday.** Each Friday the critic runs the ten daily tasks from `consultant-search-execution.md` §3 against the staging build and posts the times next to their budgets; any regression becomes the first slice of the next week.

**Slice brief template**

```markdown
# Slice S5: longlist table, bulk add, pool filters
Person-task: a consultant adds 10 Ankara finance people to the CFO search in <= 20 s.
Acceptance (Given/When/Then):
- Given 10 people selected in the pool, when I press "Add to search", then 8 are added,
  2 are listed as skipped with the reason (consent refused, off-limits), and Undo works.
- Given the longlist tab, when I press Alt+Up on row 3, then it becomes rank 2 and stays after reload.
Contract (frozen, read only): openapi tags Applications, People (operationIds: addApplications,
  listPeople, updateApplication); schema.md application (rank, rating, longlist_status).
You own: crm-pipeline/src/**/application/**, frontend/src/searches/longlist/**,
  frontend/src/candidates/pool/filters/**, db/migration/V0xx__application_rank.sql, seed/05_longlist.sql
Read only: frontend/src/shell/grid/**, frontend/src/shell/edit/**
Out of scope: shortlist rules, reports, board view changes.
Context: graphify query "how are applications created and guarded" --budget 1500
Checks: scripts/slice-check.sh; Playwright e2e/longlist.spec.ts (keyboard only); axe on /searches/:id/longlist
Evidence: docs/process/evidence/S5.md written by the browser critic.
Reply in <= 150 words: branch, commits, checks, blockers.
```

## 6. Open questions for the customer

Fifteen questions, ordered by what they block. The team builds on the working assumption until the answer arrives, and records the answer in `docs/plan/work-plan.md` §4.

| # | Question | Working assumption | Blocks |
| --- | --- | --- | --- |
| Q1 | Must data be hosted in Turkey? Your own servers or a provider? | A Turkey-based provider, chosen with your counsel | P0 real data |
| Q2 | Will you sign our data-processing agreement as controller, with us as processor? What is your VERBIS status? | Yes; signed before any real record is loaded | P0 real data |
| Q3 | How large is the pool today, and where does it live (Excel, Salesforce, folders, mail)? | Under 20,000 people in Excel and a Salesforce export, imported once in week 2 | S2 import |
| Q4 | What are your search stages in your own words, and can we replay one finished search, anonymised? | Our default stages; the shadow search runs in week 2 | S5, R12 |
| Q5 | Microsoft 365 or Google Workspace? Can IT create a log mailbox now and allow app consent later? | Microsoft 365; log mailbox in week 3; Graph consent in month 3 | S12, M3-4 |
| Q6 | Can you share your current shortlist report and progress report as Word files? | Yes; our template matches them by week 4 | S9 |
| Q7 | What must a blind report hide, and is compensation shown or banded? | Hide as in SX-022; compensation banded | S9 |
| Q8 | Which fee models and instalment triggers do you use? Does the guarantee start at placement or start date, and how long is it? | Retained thirds, 33 % of first-year cash, 6-month guarantee from start date | S13, S14 |
| Q9 | Who issues invoices, in which accounting software, through which e-Fatura integrator? | An external accountant; Mihenk exports invoice-ready items to Excel | S14 |
| Q10 | Which confidentiality levels do you use, and who may see a candidate's compensation? | Normal and Confidential; compensation visible to the owner, the search lead and partners | S11 |
| Q11 | Do off-limits terms cover the client's whole group, and for how long? | Whole group, 12 months after the search closes or the guarantee ends, whichever is later | S2, S13 |
| Q12 | Has counsel approved your aydınlatma and açık rıza texts, and the retention clock (from last consent)? | Consent for every candidate, whatever the source; 24 months from last consent | S2 import campaign |
| Q13 | Who joins the pilot, and which 2-3 live searches? Is there a researcher? | Two consultants, one researcher, one partner; two live searches | P0 |
| Q14 | What result after 8 weeks makes you sign, and at what price range? | The R9 metrics plus one placement made through Mihenk; price discussed in week 6 | Go/no-go |
| Q15 | Do clients need a login, or is a Word/PDF report by e-mail enough? | E-mail for the pilot; a share link in months 5-6 | S9, M5-6 |

## Sources and what I could not verify

Pages opened for this review. Review-site and vendor pages are marketing-grade evidence; legal and tax pages are commentary, not primary law.

- [Capterra: Invenias reviews](https://www.capterra.com/p/132632/Invenias/reviews/) (review site): 4.2/5 from 15 reviews; recent reviews cite slowness, duplicates, weak reporting and many steps to add people.
- [Recruiterflow: executive search software 2026](https://recruiterflow.com/blog/executive-search-software/) (**vendor blog**, ranks itself first): list prices for Invenias, Clockwork, Talentis, Loxo; Thrive and Cluen on request.
- [Clockwork product page](https://www.clockworkrecruiting.com/product) (**vendor**): client portal with unlimited client logins and controlled visibility.
- [Capterra UK: Ezekia](https://www.capterra.co.uk/software/207963/ezekia) (review site, four reviews from 2020): branded client portal, from £120 per user.
- [Cottgroup on KVKK standard contracts](https://cottgroup.com/en/blog/work-life/item/standart-sozlesme-bildirim-yukumlulugu-ve-yaptirimlari) (consultancy commentary): 5-business-day notification, explicit consent no longer a transfer basis after 1 September 2024, 2026 fine range.
- [CMS expert guide: e-signatures in Turkey](https://cms.law/en/int/expert-guides/cms-expert-guide-to-e-signatures-in-commercial-contracts/turkey) (law-firm guide, last updated 2020).
- [Business Wire: Turkcell and Google Cloud region](https://www.businesswire.com/news/home/20251111239976/en/Turkcell-Announces-Strategic-Partnership-with-Google-Cloud-with-Plans-t) (press release, November 2025; no availability date).
- [Nylas plans](https://www.nylas.com/?p=50072) (**vendor**): four tiers; regions and per-account prices not on the page.
- [Paraşüt integrations](https://www.parasut.com/entegrasyonlar) (**vendor**): links a public API; invoice creation through it not confirmed.
- [poi-tl on GitHub](https://github.com/Sayi/poi-tl): Apache-2.0 Word template engine.
- [Gotenberg guide](https://computingforgeeks.com/gotenberg-html-to-pdf-api/) (third-party tutorial): Chromium and LibreOffice conversion in one container, PDF/A options.
- Claude Code docs: [agent teams](https://code.claude.com/docs/en/agent-teams), [subagents](https://code.claude.com/docs/en/sub-agents), [hooks](https://code.claude.com/docs/en/hooks).
- Repository: `docs/plan/STATE.md`, `work-plan.md`, `docs/product/brand/*`, `requirements-v2/*`, `research/*`, `product-plan-phase1.md`, `contracts/schema.md`, `.claude/agents/*`, git log to commit c251475.

**Not verified:** whether hosting in Turkey removes all transfer-abroad questions for this firm; whether an engagement letter needs written form; Paraşüt's invoice API scope and plans; Nylas data regions; the firm's actual mail provider; competitor feature claims beyond the pages above; how the running app behaves today (taken from the two critiques of 28 October, before the F0 and brand-shell merges).
