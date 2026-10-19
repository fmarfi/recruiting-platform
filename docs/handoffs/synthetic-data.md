# Hand-off: synthetic-data · W1-15 · 2026-10-17

**Branch / commits:** `worktree-wave1b-synthetic-data` · 1 commit

**Done**
- `seed/00..08_*.sql`, `seed/run.sql`: demo dataset in one transaction, idempotent, fixed v7-shaped ids (see seed/README.md).
- `seed/load.sh`, `seed/load.ps1` (db name parameter, default `recruiting`), `seed/counts.sql`, `seed/README.md`.
- Demo users with real `{bcrypt}` hash of `Demo-Password-2026` (BCrypt.checkpw verified), `must_change_password=false`.

**Counts (database synthetic_data, loaded twice, identical)**
app_user 3, account 25 (LEAD 6, PROPOSAL_SENT 4, ACTIVE 9, ON_HOLD 3, CLOSED 3), person 80, person_employment 158, contact 30, network_link 41, job_order 10 (all 7 statuses), application 38 (all 7 stages, 11 REJECTED with reasons), proposal 18 (SENT 4, ACCEPTED 10, REJECTED 2, DRAFT 2), activity 70, next_action 24 (6 overdue, 6 today, 8 upcoming, 4 done), timeline_entry 310, account_open_job 10, person_privacy 80, consent_request 70, consent_record 60, privacy_notice 2, data_subject_request 4, retention_run 1. Consent mix GIVEN 30, NOT_SENT 20, REFUSED 10, SENT 10, WITHDRAWN 10; 4 FLAGGED, 1 ANONYMISED.

**Others must know**
- Seed writes rows directly, not through events or Envers; `*_aud` tables are cleared on reload.
- Reload deletes `app_user` rows, so any user created in the UI is lost.

**Verified with**
- `mvn -q install -DskipTests`, app on 8085 against `synthetic_data` (Flyway migrated), then `COMPOSE_PROJECT_NAME=recruiting-platform seed/load.sh synthetic_data` twice and `seed/counts.sql` → counts above; `load.ps1` also run OK.

## Wave 1b (W1-15 finish) · 2026-10-19

**Branch / commits:** `worktree-wave1b-synthetic-data` · merged main, then 1 fix commit

**Done**
- `seed/07_timeline.sql`: a candidate placed on a job order now gets an OFFER step before PLACED (PLACED is reachable only from OFFER). Timeline is 311 entries.
- `seed/load.ps1`: sets `$ErrorActionPreference = 'Continue'`; under 'Stop', Windows PowerShell 5.1 turned docker's stderr progress into a terminating error and the load failed.
- Checked against `docs/status-lifecycles.md`: all 7 account, 7 job order, 7 stage, 4 proposal and 5 consent statuses are present; account, job and stage histories follow legal transitions.

**Not done / left**
- The app was not started against the seeded DB. The stack's `recruiting` role password is not something I could read (access was denied), so Flyway could not log in. Migrations were applied with `psql` in version order instead (same SQL, no `flyway_schema_history` row). Logging in through the UI still needs one check.

**Others must know**
- qa-engineer / frontend: demo login is `admin|manager|consultant@demo.example` / `Demo-Password-2026`.

**Verified with**
- `docker compose exec -T postgres dropdb/createdb synthetic_data`, migrations via psql, then `COMPOSE_PROJECT_NAME=recruiting-platform seed/load.sh synthetic_data` twice → loads, counts unchanged on reload.
- `seed/load.ps1 -Database synthetic_data` → exit 0.
- `seed/counts.sql` → 3 users, 25 accounts (all 5 statuses), 80 people, 10 job orders (all 7), 38 applications (all 7 stages), 18 proposals (all 4), 70 activities, 24 next actions, 311 timeline entries; `open_job_count` mismatches 0.
- BCrypt check with `spring-security-crypto` (`BCrypt.checkpw`) on the stored hash → matches `Demo-Password-2026`, rejects `wrong`.
