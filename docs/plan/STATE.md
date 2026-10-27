# State of the work (2026-10-28)

Resume with: "Read docs/plan/STATE.md and docs/plan/work-plan.md, then continue." Run commands are in the root `README.md`.

## Where we are
Branch `feature/network-customer-tools` holds everything. Verified on the merged result: `mvn -B verify -Pintegration` 151 tests, 0 failures; frontend `tsc` clean, 154 tests, build ok.

Merged: Wave 2a (DataGrid, command bar, saved views, bulk-action tables), contracts 1.1.1 and 1.2.0, Wave 2b db migrations and entities, Wave 2b backends (accounts: people profile, encrypted compensation, files and Tika, CV intake, duplicates, off-limits; pipeline: bulk actions, ShareGuard stage guards, exposure; security-privacy: FieldCipher, consent engine, merge orchestration, purge), Wave 2b frontends, and a UX polish pass (My day, highlights, accessibility basics).

Local development: the `local` profile signs in as the demo admin (`app.security.dev-auto-login-as` in `application-local.yml`). Delete that line to get the login screen back. The dev server needs `API_TARGET=http://localhost:8080` to use the real backend.

## Known gaps and follow-ups
- `POST /people/{id}/merge` returns a reduced person record, not the contract's full `Person`; accounts needs a `survivorView` API.
- Not built yet: nightly duplicate scan, weekly orphan sweep, `GET /bulk-actions`, accounts and people bulk handlers, `hasCv` filter field.
- Phone layout of the UX pass was not checked in a browser; the Network filter card is long on phones; a Referrer page still says "Candidate profile".
- `ConsentEngineApiIT` flaked once; fixed in the test, watch it.
- Frontend: off-limits override field and exposure warning toast on add-to-job, file delete button, network list on the server-backed view store, Playwright end-to-end tests, demo people at PRESENTED need consent GIVEN.
- Open client questions: `docs/plan/work-plan.md` section 4.

## Queue
1. Stage Structure (ADR 0012, needs the decision to extract a `talent` module from `accounts`; run with no agent worktrees open).
2. Stage UX in full: browser audit of every screen, WCAG 2.2 AA, design system, performance, usability test.
3. Wave 2c (BD and assignments), 2d (client output), 2e (relationship edge and integrations).

## Housekeeping rules
Agents work in `.worktrees/` (git-excluded); remove each worktree and branch after its merge. `frontend/package-lock.json` is never committed. Commits carry no attribution lines. `CLAUDE.md` and `.claude/` are git-ignored and kept local; a copy is on Google Drive.
