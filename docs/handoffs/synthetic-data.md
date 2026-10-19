# Hand-off: synthetic-data · W1-15 · 2026-10-17

**Branch / commits:** `worktree-wave1b-synthetic-data` · 1 commit

**Done**
- `seed/00..08_*.sql`, `seed/run.sql`: demo dataset in one transaction, idempotent, fixed v7-shaped ids (see seed/README.md).
- `seed/load.sh`, `seed/load.ps1` (db name parameter, default `recruiting`), `seed/counts.sql`, `seed/README.md`.
- Demo users with real `{bcrypt}` hash of `Demo-Password-2026`, `must_change_password=false`.

**Others must know**
- Seed writes rows directly, not through events or Envers; `*_aud` tables are cleared on reload.
- Reload deletes `app_user` rows, so any user created in the UI is lost.

## Wave 1b (W1-15 finish, review fixes) · 2026-10-19

**Branch / commits:** `worktree-wave1b-synthetic-data` · merged main, then fix commits

**Done**
- `seed/07_timeline.sql`: placed candidates get an OFFER step before PLACED.
- `seed/load.ps1`: `$ErrorActionPreference = 'Continue'` (Windows PowerShell 5.1 turned docker stderr into a terminating error).
- Review fixes: person 81 "Merve Öztürk" added (CANDIDATE, 81 people in total); phones now `+90 000 000 xx xx`; LinkedIn host `linkedin.example`.
- Account 6 (Çınaraltı Tekstil) was LEAD with a rejected proposal and no logged SENT step. Now LEAD->PROPOSAL_SENT->LEAD (back on rejection, no reason needed).
- Account 24 (Pinewood Packaging) is CLOSED, not ACTIVE as the review said. It had only a rejected proposal, so its history no longer shows an ACTIVE step: LEAD->PROPOSAL_SENT->CLOSED with reason.

**Not done / left**
- The app was not started against the seeded DB. Migrations were applied with `psql` in version order, not Flyway. Logging in through the UI still needs one check.

**Others must know**
- qa-engineer / frontend: demo login is `admin|manager|consultant@demo.example` / `Demo-Password-2026`. Person count is now 81.

**Verified with** (`COMPOSE_PROJECT_NAME=recruiting-platform`, postgres container started with `docker compose up -d postgres`)
- `dropdb/createdb synthetic_data`, migrations via psql, `seed/load.sh synthetic_data` twice → exit 0, second load identical.
- `seed/counts.sql` → person 81, person_privacy 81, timeline_entry 312, account statuses 6/4/9/3/3, open_job_count mismatches 0.
- Checked: no `+90 555` or `linkedin.com` left in the data; account 6 and 24 histories match `docs/status-lifecycles.md`.
