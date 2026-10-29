# Hand-off: frontend-pipeline · Wave 2b (candidate pool, CV files, duplicates, guards) · 2026-10-26

**Branch / commits:** `worktree-wave2b-frontend-pipeline` · 5 commits on 1a6461f

**Done**
- /network pool filters: tag (TagsInput), source channel chips, CV full-text, shareable chip. URL params `tag`, `sourceChannel` (alias `channel` read), `cv`, `shareable=true`; saved via the existing local ViewStore (pool = tag + saved view). Columns tags, channel, shareable, CV match excerpt (rendered as text + mark, never HTML).
- Person page: candidate profile block (compensation only when not `compensationRestricted`, LinkedIn plain link), CV files tab (drag/drop + button, versions, download, extraction status), exposure tab, one-click consent send in the header only for NOT_SENT/SENT; REFUSED/WITHDRAWN (do not contact) only via the privacy-tab dialog, same rule in the 409 CONSENT_REQUIRED dialog.
- New person: /duplicates/check before create, "possible duplicate" prompt (use existing / create anyway); a failed check never blocks. /network/duplicates queue and merge dialog (MANAGER/ADMIN, sends survivor version, 409 reloads it).
- /network/cv-intake: bulk drop zone (50 files, 10 MB), review list with attach to duplicate match, create person (prefilled suggestions), discard, 14-day expiry, polling while text extraction runs.
- Pipeline: 409 CONSENT_REQUIRED dialog (resend consent, not overridable), 409 OFF_LIMITS dialog (rules; manager/admin reason 3-500 chars retries with offLimitsOverrideReason); exposure warnings shown in the move toast.
- TR/EN for every label; 119 vitest tests (filters to URL, duplicate decision, 409 handling, compensation visibility, file and expiry rules).

**Not done / left**
- Adding a candidate (POST /job-orders/{id}/applications): OFF_LIMITS shows the rules and says only a manager/admin can override on the board; TODO override field (offLimitsOverrideReason on ApplicationCreateRequest) and warnings toast there.
- Saved views still localStorage (`useViewStore` not merged); no shared talent-pool view UI yet.
- Not run against Prism or a browser; multipart bodies (FormData) untested against a real server.
- No file delete button on the files tab.

**Change requests**
- frontend-shell · routes.tsx: I added /network/cv-intake and /network/duplicates (please keep on merge).
- frontend-shell · nav: optional links to CV intake and duplicates; reuse `network.intake.nav` / `network.dup.nav`.

**Verified with**
- `npx tsc --noEmit` clean; `npx vitest run` 119 passed; `npx vite build` ok (in frontend/).

## Slice S6: edit everywhere (client, candidate, search)

**Done** (frontend only, no contract change; uses F0 EditDrawer, InlineEditField, TagInput, useUndoToast, HistoryPanel)
- Client page: inline name edit with undo, status menu now shows an undo toast, `AccountEditDrawer` with all `AccountUpdateRequest` fields, History tab.
- Person page: `PersonEditDrawer` with all `PersonUpdateRequest` fields (career, languages, education row editors), tags via TagInput, compensation section hidden when `compensationRestricted`; History tab; Edit disabled when anonymised.
- Search page: `JobOrderEditDrawer` (description, fee terms, openings, opened on, owner), description in the header card, History accordion. New `NewJobForm` keeps description and fee terms; used by the Searches list and the client page "New job" (the "New search" button already existed).
- Shared in `accounts/detail/editSupport.tsx`: draft state, inline field errors from `ProblemDetail.errors`, undo, VERSION_CONFLICT compare dialog.
- Tests: one file per drawer (save, undo, validation error; compensation guard). tsc clean, vitest 243 passed, vite build ok.

**Notes**
- Tags have no PATCH field: saved through `POST /bulk-actions` ADD_TAG / REMOVE_TAG (one id); undo reverts them with the fields.
- Compensation write permission is enforced by the server (403 shows as the drawer error); the UI only hides the section when restricted.

**Change requests**
- frontend-shell: `UndoToastProvider` is not mounted anywhere in the app; I wrapped the three pages in it. Mount it once in the layout, then the page wrappers can go.
- `NewRecordDialog` kind="job" (accounts/components) is now unused for jobs.
