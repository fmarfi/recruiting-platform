# Hand-off: ux-designer (Wave 0, W0-07 and W0-08)

## What changed

- `contracts/design-tokens.json`: light and dark colours and seven tone colours extracted from the demo CSS, spacing, radius, typography, breakpoints, `statusTone` map (account, job, stage, proposal, consent, person type, user role, user state, action due) with TR/EN labels.
- `docs/ux/flows.md` (F1-F10), `docs/ux/screens/{my-day,accounts,job-pipeline,network,search,settings}.md`, `docs/ux/README.md` (requirement index), `docs/ux/demo-review.md`.

## What is left

- Story-to-screen check once `docs/stories/` is merged (README maps requirements, not stories).
- Cross-check `statusTone` against `docs/status-lifecycles.md`; add or rename entries if statuses differ.
- Colour contrast check of tone pairs in a real build (taken from the demo; AA not measured).
- UX-10 usability test script (process, not a screen).

## Change requests (for the orchestrator / architect, not edited by me)

1. OpenAPI endpoints missing versus `demo/API.md`: `POST /jobs`, `PATCH /jobs/{id}`, `GET /jobs?status=&owner=&accountId=`, `POST /proposals`, `PATCH /accounts/{id}`, `POST /people`, `PATCH /people/{id}`, `POST/DELETE /people/{id}/connections`, `POST/DELETE /accounts/{id}/contacts`, `PATCH /actions/{id}` (snooze), search filters on `GET /search`, `GET /people` extra filters (`owner`, `lastContactBefore`, `consent`), consent send/record and data-request endpoints, users/roles/status-label settings, audit log list, CSV import, status change `reason` (optional) for account/job.
2. Add `userRole`, `userState` and consent history to shared enums if not covered.
3. Status lists in Settings: only labels, tone and order editable; keys and transitions stay in code. Confirm with the architect and the client.
4. Add a **Jobs** item to the nav (list `/jobs`) for the 3-click rule; the requirement's core screen list does not name it.
5. Turkish-aware, diacritic-insensitive search (backend-accounts).
6. Self-host fonts with the latin-ext subset (frontend-shell).

## Others must know

- Tokens are keyed by tone; frontends read `statusTone[kind][STATUS].tone` then `tone.<theme>.<tone>`. Unknown status falls back to `neutral`.
- Open decisions flagged in specs: block "Add to job" for REFUSED/WITHDRAWN people (needs legal confirmation); retention counted from upload or last contact (open client question).
- Not mapped to a screen: S-02 (website form, Phase 2 B-13) and UX-10 (process). All other Phase 1 requirements map in `docs/ux/README.md`.
