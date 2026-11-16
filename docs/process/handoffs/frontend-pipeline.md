# Hand-off: frontend-pipeline · M2-UI2 agreements and proposals UI · 2026-11-16

(The M2-UI1 note for branch `m2-longlist` is in that branch's history of this file.)

**Branch / commits:** `m2-agreements-ui` from ede80cd (frontend only, no contract or backend change) · 1 commit

**Done**
- Client page `/accounts/:id`: Proposals tab is now a DataGrid (title, status badge menu with reason dialog, fee, sent, decided, owner, History). New Agreements tab beside it, its own table (agreement, position, status, fee, signed on, valid until, signed PDF, History) with empty state and "New agreement".
- Search page `/job-orders/:id`: Agreements panel for that position (`GET /agreements?jobOrderId`), "New agreement" preset to the position and client (kind engagement letter).
- Drawer `pipeline/agreements/AgreementDrawer.tsx`: kind, position, status, title, dates, fee model with only its fields (`feeModel.ts`: percent, basis, flat fee, estimated fee, min/cap, schedule), currency, payment terms, guarantee, signatory (active contacts), notes. Thirds get a 33.34/33.33/33.33 default. Update sends `version`; 409 opens the shared compare dialog (`useRecordSave`). Schedule is locked once an instalment is no longer NOT_DUE.
- Signed copy (`SignedCopy.tsx`): upload or replace PDF, client check PDF and 20 MB (`fileCheck.ts`), shows name, size, who, when, download link.
- History dialog (`parts.tsx`) for proposals (PROPOSAL) and agreements (AGREEMENT) via the shared HistoryPanel, newest first.
- i18n: `agr.*` in `pipeline/i18n/{en,tr}.json` (TR mostly English text, not polished).
- Tests: `feeModel.test.ts` (fee fields, validation, file check), `AgreementDrawer.test.tsx` (hidden fields, create body, 409 compare).

**Review fixes (round 2)**
- Signatory uses `signatoryContactId` (fallback: active contact of `signatory.id`); thirds with no instalments rejected; `vatApplies` kept per instalment; undo skips `scheduleItems` (`noUndoKeys` in `accounts/detail/editSupport.tsx`); failed upload reloads the agreement and always clears busy; `dueDays` shown for every trigger; Proposals empty state uses shared EmptyState; Agreements tab count removed; seed rename undone; StatsStrip fix merged from `feature/network-customer-tools`.

**Not done / left**
- Proposal edit drawer was not asked for. Not clicked through against Prism or the backend.

**Others must know**
- Branch touches only `frontend/` and this note.

**Change requests**
- architect · `/reference-data` lifecycles · add an `agreement` lifecycle so the shared StatusBadge can be used; tone map is local in `AgreementsPanel.tsx`.
- architect · `/audit/revisions` · instalment changes are audited as FEE_SCHEDULE_ITEM and the endpoint takes one entityId, so they cannot be shown in an agreement's History. Add a `parentType/parentId` filter or write them under AGREEMENT.

**Verified with**
- `npm run typecheck` clean; `npm test` 45 files, 302 tests pass; `npm run build` passes.
- Click path: `/accounts/<id>` tabs Proposals and Agreements; `/job-orders/<id>` Agreements panel; row click opens the drawer.
