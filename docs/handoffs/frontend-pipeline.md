# Hand-off: frontend-pipeline · Wave 2b (candidate pool, CV files, duplicates, guards) · 2026-10-26

**Branch / commits:** `worktree-wave2b-frontend-pipeline` · 5 commits on 1a6461f

**Done**
- /network pool filters: tag (TagsInput), source channel chips, CV full-text, shareable chip. URL params `tag`, `sourceChannel` (alias `channel` read), `cv`, `shareable=true`; saved via the existing local ViewStore (pool = tag + saved view). Columns tags, channel, shareable, CV match excerpt (rendered as text + mark, never HTML).
- Person page: candidate profile block (compensation only when not `compensationRestricted`, LinkedIn plain link), CV files tab (drag/drop + button, versions, download, extraction status), exposure tab, one-click consent resend in the header.
- New person: /duplicates/check before create, "possible duplicate" prompt (use existing / create anyway); a failed check never blocks. /network/duplicates queue and merge dialog (MANAGER/ADMIN, sends survivor version, 409 reloads it).
- /network/cv-intake: bulk drop zone (50 files, 10 MB), review list with attach to duplicate match, create person (prefilled suggestions), discard, 14-day expiry, polling while text extraction runs.
- Pipeline: 409 CONSENT_REQUIRED dialog (resend consent, not overridable), 409 OFF_LIMITS dialog (rules; manager/admin reason 3-500 chars retries with offLimitsOverrideReason); exposure warnings shown in the move toast.
- TR/EN for every label; 113 vitest tests (filters to URL, duplicate decision, 409 handling, compensation visibility, file and expiry rules).

**Not done / left**
- Adding a candidate (POST /job-orders/{id}/applications) shows the server text on OFF_LIMITS; no override UI or warnings toast there.
- Saved views still localStorage (`useViewStore` not merged); no shared talent-pool view UI yet.
- Not run against Prism or a browser; multipart bodies (FormData) untested against a real server.
- No file delete button on the files tab.

**Change requests**
- frontend-shell · routes.tsx: I added /network/cv-intake and /network/duplicates (please keep on merge).
- frontend-shell · nav: optional links to CV intake and duplicates; reuse `network.intake.nav` / `network.dup.nav`.

**Verified with**
- `npx tsc --noEmit` clean; `npx vitest run` 113 passed; `npx vite build` ok (in frontend/).
