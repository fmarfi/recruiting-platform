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

**Not done / left**
- Proposal edit (title, terms, fee) still only via create; no edit drawer was asked for.
- Not clicked through against Prism or the real backend (backend agreements branch not merged); verified by tests and build only.

**Others must know**
- This worktree directory was shared with a backend agent (uncommitted Java, seed and contracts/enums changes). I committed only frontend paths and the hand-off note.
- `frontend/src/shell/home/StatsStrip.tsx` is not valid UTF-8 (cp1252 byte) in commit ede80cd; `npm run build` fails on it (Vite). I verified the build with a temporary iconv fix and reverted it.

**Change requests**
- architect · `/reference-data` lifecycles · add an `agreement` lifecycle (labels, tones) so the badge can use the shared StatusBadge; the tone map is local in `AgreementsPanel.tsx` for now.
- architect · `Agreement.signatory` is a PersonRef but `signatoryContactId` is a contact id; the drawer preselects by `signatory.id` and assumes it is the contact id. Confirm, or expose `signatoryContactId` on `Agreement`.
- frontend-shell · `shell/home/StatsStrip.tsx` · re-save as UTF-8 (build blocker).

**Verified with**
- `npm run typecheck` → clean; `npm test` → 45 files, 298 tests pass; `npm run build` → passes with the StatsStrip encoding fixed locally (fails on the base commit without it).
- Click path: `/accounts/<id>` tabs Proposals and Agreements; `/job-orders/<id>` Agreements panel; row click opens the drawer.
