# Screen: Settings (users, roles, status lists)

Requirements: NFR-05 (roles, audit log), A-02/A-05/A-06/A-07 (status lists), A-11 (import), S-03/NFR-03 (retention setting), UX-07. Route: `/settings/{section}`. Visible to Manager (read-only except where noted) and Admin; hidden from Consultants (no nav item; direct URL shows "You do not have access").

Question answered: **who can do what, and what do the status words mean here?** Status lists are closed sets in Phase 1 (ADR 0002): shown here read-only, never edited.

## Wireframe, desktop

```
| Settings                                                                    |
| +-------------+--------------------------------------------------------------+
| | Users       | Users (9)                                    [+ Invite user]|
| | Roles       | [ Search ]                                                  |
| | Status lists| Name           E-mail             Role           State      |
| | Import      | Ayşe Kaya      ayse@example.com   [Consultant v] (Active)   |
| | Retention   | Can Tunç       can@example.com    [Manager v]    (Active)   |
| | Audit log   | Deniz Ak       deniz@example.com  [Consultant v] (Disabled) |
| +-------------+--------------------------------------------------------------+
```

Status lists section (read-only, data from `GET /reference-data`):

```
| Status lists:  [Clients] [Proposals] [Job orders] [Candidate stages] [Consent] [Data requests] |
| Clients                                          (read-only, fixed in Phase 1)               |
| Order  Key            Label EN        Label TR          Tone       Moves to                  |
| 1      LEAD           Lead            Aday müşteri      (neutral)  Proposal sent, Closed*    |
| 2      PROPOSAL_SENT  Proposal sent   Teklif gönderildi (info)     Lead, Active, On hold*, Closed* |
| ...        * = reason required;  M = manager only                                            |
```

No edit, reorder, hide or reset controls. A hint explains why: "Status lists are fixed in this version. Contact support to request a change."

## Wireframe, phone

Section list is the first screen (iOS-style); tapping a section opens it full-width with a back arrow. Tables become cards: user card shows name, role dropdown, state badge, and a kebab menu. Status list rows are plain cards (label, tone badge, allowed moves).

## Sections and fields

| Section | Fields | Actions |
| --- | --- | --- |
| Users (`GET/POST /users`, `PATCH /users/{id}`, `POST /users/{id}/password-reset`; admin) | `name`, `email`, `role`, `active`, `language`, `lastLoginAt` | Add user (name, e-mail, role, language, initial password; see Contract gaps), change role, disable/enable (`active`), reset password. Never hard-delete (history keeps author names). An admin cannot demote or disable themselves; at least one Admin must remain |
| Roles | read-only matrix: role x capability (see below) | View only in Phase 1 (fixed roles per NFR-05) |
| Status lists | per lifecycle in `GET /reference-data` (account, proposal, jobOrder, pipelineStage, consent, dataSubjectRequest): `value`, `labelEn`, `labelTr`, `tone`, `terminal`, `next[]` (`to`, `reasonRequired`, `managerOnly`) | None. Read-only (ADR 0002) |
| Import (`/imports`, `/import-templates/{kind}`) | `kind` (ACCOUNTS / CONTACTS / PEOPLE), file, preview with `errors[]` (`row`, `column`, `message`), counts (`validRows`, `duplicateRows`), result | Upload, preview, commit (`POST /imports/{id}/commit`), discard (A-11, Could) |
| Retention (`GET/PUT /retention/settings`, `GET /retention/flagged`, `GET /retention/runs`) | `retentionMonths` (default 24), `basis` (FROM_CREATED / FROM_LAST_CONTACT), `action` (ANONYMISE / DELETE), `warningDays`, `autoSendNotice`, `autoSendPersonTypes`, `renewOnConsent`; flagged people list and past runs | Change: Admin only (managers read-only); sends `version`; every change is logged |
| Audit log | Two tabs. Access log (`GET /audit/access-log`, admin): time, user, action (VIEW / EXPORT / DOWNLOAD), entity. Change history (`GET /audit/revisions`, manager and admin): who changed what, when, per record | Filter by user, entity type, action, date range (NFR-05) |

Roles matrix (default, to confirm with client): Consultant = read/write own and team records, no Settings; Manager = all records, reassign owners, view users and audit log, import; Admin = everything plus users, retention changes and access log. Any consultant may change any record; manager-only moves elsewhere in the app are disabled for consultants with a tooltip.

## States

- **Loading:** skeleton table. **Empty users:** impossible (the current user exists); invited-but-pending users show "Invitation pending" with Resend.
- **Validation errors:** field errors from the server shown inline (for example weak initial password, duplicate e-mail 409 `DUPLICATE`).
- **No permission:** "You do not have access to Settings. Ask an administrator." with link home.
- **Error:** banner + Retry; failed save keeps the form open with the server detail.
- **Save pattern:** each section has Save/Cancel; unsaved changes prompt on leave.

## Labels

| Key | EN | TR |
| --- | --- | --- |
| nav.settings | Settings | Ayarlar |
| sec.users / roles / statuses / import / retention / audit | Users / Roles / Status lists / Import / Retention / Audit log | Kullanıcılar / Roller / Durum listeleri / İçe aktarma / Saklama / Denetim kaydı |
| btn.invite | Invite user | Kullanıcı davet et |
| role.consultant / manager / admin | Consultant / Manager / Admin | Danışman / Yönetici / Sistem yöneticisi |
| state.active / disabled | Active / Disabled | Aktif / Devre dışı |
| col.tone | Colour | Renk |
| col.moves | Moves to | Geçişler |
| flag.reason / flag.manager | Reason required / Manager only | Neden gerekli / Yalnızca yönetici |
| hint.keysFixed | Status lists are fixed in this version. They are shown for reference. | Durum listeleri bu sürümde sabittir. Yalnızca bilgi için gösterilir. |
| err.noAccess | You do not have access to Settings | Ayarlara erişiminiz yok |
| retention.months | Keep candidate data for (months) | Aday verisini saklama süresi (ay) |
