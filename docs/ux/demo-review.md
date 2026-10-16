# Demo review: what to keep and what to improve

Reviewed: `demo/src/main/resources/static/{index.html,app.css,app.js}` and `demo/API.md`. The demo is a good base; it is not edited. Priority: **P1** blocks the pilot, **P2** should do in Wave 1, **P3** later.

## Keep (works well)

- Colour system: green accent, neutral surfaces, seven tones with dot + text badges (accessible, not colour-only). Extracted unchanged to `contracts/design-tokens.json`.
- Lifecycle-driven status menus: only legal next statuses are offered (`next` from `GET /meta`), tone and labels come from the server.
- Tables turn into cards under 720 px; modals become bottom sheets; bottom tab bar on phone; skip link, focus ring, `aria-live` view region, reduced-motion support; EN/TR toggle.
- Board drag and drop restricted to legal columns with a Move menu fallback; rejection reason required.
- Hash router with breadcrumbs; one component set for lists, badges, cards.

## Improvements for the real build

| # | Pri | Problem in the demo | Change |
| --- | --- | --- | --- |
| 1 | P1 | Quick add (UX-06) only exists as a button inside Account/Person pages; logging after a visit needs navigating first | Global **+ New** menu (Log activity, Add action, Client, Person) on every screen; floating + on phone; context picker with type-ahead; last-used type preselected; Ctrl/Cmd+Enter to save; optional follow-up in the same sheet |
| 2 | P1 | Search has no filters, so A-14 (sector, status, owner, last contact) is unmet; no type-ahead; results are only grouped lists | Type-ahead dropdown, filter bar and bottom-sheet filters on phone, diacritic-insensitive Turkish matching, recent searches, "add it as a client" on no results |
| 3 | P1 | No Settings, no user/role UI, no status list management, no audit log view | Settings screen (users, fixed roles matrix, status labels/tones, import, retention, audit log), role-aware navigation |
| 4 | P1 | Seven kanban columns on a phone are one horizontal scroll with 78vw columns; hard to know where you are, card Move is a small icon | Stage switcher strip showing one column at a time with counts; Rejected as a collapsed lane (6 columns on desktop); 44 px Move targets; rejection reason chips |
| 5 | P1 | Missing create/edit flows: no create job order, proposal, person, connection; no edit of account, job; no CSV import (A-11); no jobs list; no snooze/edit action | Add flows F1, F7, F8, F10 and `/jobs` list; list needed endpoints in the handoff (`POST /jobs`, `POST /proposals`, `POST /people`, `POST /people/{id}/connections`, `PATCH` for account/job/action, `GET /jobs`) |
| 6 | P2 | Status change has no Undo; mis-click on a badge changes client status and the timeline | Toast with Undo (8 s) for status, stage and done-action changes; optional reason on Closed/Cancelled |
| 7 | P2 | Account page shows tabs plus dashboard, but the timeline is on the side only; on phone it is far down | Timeline is a fourth tab on phone; timeline filter by kind; events link to their record |
| 8 | P2 | Lists have no "mine/all" default or counts; the overview of all accounts (A-01) needs scanning | Owner defaults to Me with an All toggle; status chips with counts double as filter; sortable columns; pagination |
| 9 | P2 | Consent only appears as a badge; no way to send a request, see retention, or log a data request (S-01, S-03, S-04) | Privacy tab on Person page; send/record consent; retention date and 60-day warning; data request log |
| 10 | P2 | Empty states are generic in places (UX-08) | One guided next step per empty state (see each screen spec) |
| 11 | P2 | Connections (A-13) are listed but cannot be added; no relationship kind variety | Add connection dialog with kinds Knows / Introduced by / Introduced / Colleague / Referrer of, shown both ways |
| 12 | P2 | Network list lacks "not contacted since", the main networking trigger | Last-contact filter and a warn-coloured date after 90 days |
| 13 | P3 | Keyboard drag alternative exists only through a Move menu | Add Space/arrow keyboard drag with screen-reader announcements |
| 14 | P3 | Fonts load from Google Fonts at runtime (privacy, offline, KVKK data flow) | Self-host fonts with the latin-ext subset for Turkish characters |
| 15 | P3 | Language toggle is in the top bar, stored only in the page | Store language per user profile; default from browser; format dates and numbers with the locale (dd.MM.yyyy in TR) |

## Salesforce-style complexity to avoid

- No configurable page layouts, record types, or related-list editors; one fixed layout per record (UX-07).
- No separate Contact object UI: a "contact" is a person in the network with a role at an account (A-03/A-12).
- No Opportunity object: proposals and job orders carry the sales status (research note in requirements).
- No free-form tabs or dashboards builder; the Account dashboard strip is fixed (A-09).
- No more than four tabs per record and no nested tabs; no modal on top of a modal.
- Status editing in Settings changes labels and colours only; transitions stay fixed in code.
- No kanban for accounts or proposals in Phase 1; only the candidate pipeline is a board (UX-04).

## Phone issues in the demo

- The three-item bottom bar has no Search or New; add Search icon and + (floating).
- Table cards hide the Sector/City columns on phone (`c-hide-sm`); show them as the second line.
- Footer note spacing and safe-area insets are good; keep. Tap targets under 44 px: icon buttons are 30 px (`btn-icon`), badges menus 24 px; raise to 44 px on phone.
- Board cards are tall because of consent badge + date; drop the date on phone (use relative "2d").
- No pull-to-refresh or offline message after a site visit with poor signal; show a clear error banner with Retry and keep typed text in the quick-add sheet.
