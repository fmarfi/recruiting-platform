# Demo dataset (seed/)

Fully fictional data: no real people or companies, and never the pilot client. Load it into a database the app has already migrated (Flyway runs when the app starts).

## Load
```
seed/load.sh [database]          # Git Bash / Linux / macOS
seed/load.ps1 [-Database name]   # PowerShell
```
Default database: `recruiting`. The scripts copy `seed/` into the compose `postgres` container and run `psql` there, so `docker compose up -d` must be running. If you call them from a git worktree, set `COMPOSE_PROJECT_NAME` to the project of the running stack (for example `recruiting-platform`).

The load is idempotent: it clears the data tables and re-inserts in one transaction (a failure rolls everything back). It keeps `flyway_schema_history`, `revinfo` and the `retention_settings` row (reset to the defaults). Audit (`*_aud`), access log and event tables are cleared too. All dates are relative to `now()`, so overdue and due-today next actions are fresh at every load.

`seed/counts.sql` prints row counts per table and per status.

## Demo login
Password for all three: `Demo-Password-2026` (stored as `{bcrypt}`).
`admin@demo.example` (ADMIN), `manager@demo.example` (MANAGER), `consultant@demo.example` (CONSULTANT).

## Files (loaded in this order by `run.sql`)
| File | Seeds |
| --- | --- |
| `00_clear.sql` | helper functions `uid()` and `slug()`, clears the data tables |
| `01_users.sql` | 3 demo users |
| `02_accounts.sql` | 25 accounts, every status |
| `03_people.sql` | 81 people, employment, 30 contacts (1 ended), 41 network links |
| `04_job_orders.sql` | 10 job orders (every status), 38 applications (every stage), `account_open_job` |
| `05_proposals.sql` | 18 proposals, every status |
| `06_activities_actions.sql` | 70 activities, 24 next actions (6 overdue, 6 today, 8 upcoming, 4 done) |
| `07_timeline.sql` | 311 timeline entries derived from the rows above (placed candidates get an OFFER step first) |
| `08_privacy.sql` | notices, per-person privacy rows, consent requests/records, 4 data-subject requests, retention |

## Fixed ids
`uid(kind, n)` = `01930000-<kind as 4 digits>-7000-8000-<n as 12 digits>`, e.g. admin = `01930000-0001-7000-8000-000000000001`.
Kinds: 1 user, 2 account, 3 person, 4 contact, 5 job order, 6 application, 7 proposal, 8 activity, 9 next action, 11 network link, 12 employment, 13 privacy notice, 14 consent request, 15 consent record, 16 data-subject request, 17 retention run, 19 timeline entry, 18 timeline source event.
Numbers: accounts 1-25 (11-19 are ACTIVE), persons 1-30 client contacts (person n is the primary contact of account n), 31-64 candidates, 65-74 executives, 75-79 referrers, 80 anonymised, 81 candidate (surname Öztürk). Job orders 1-10 (1 OPEN ... 8 OFFER, 9 FILLED, 10 CANCELLED).

## Editing
Each file has its data in a small `VALUES` table at the top (`acct_seed`, `j_seed`, `app_seed`, `prop_seed`) and an `INSERT ... SELECT` below. Later files read the temp tables of earlier ones, so keep the numeric order.
