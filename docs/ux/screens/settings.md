# Screen: Settings (users, roles, status lists)

Requirements: NFR-05 (roles, audit log), A-02/A-05/A-06/A-07 (status lists), A-11 (import), S-03/NFR-03 (retention setting), UX-07. Route: `/settings/{section}`. Not in the demo. Visible to Manager (read-only except where noted) and Admin; hidden from Consultants (no nav item; direct URL shows "You do not have access").

Question answered: **who can do what, and what do the status words mean here?**

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

Status lists section:

```
| Status lists:  [Clients] [Proposals] [Job orders] [Candidate stages]         |
| Clients                                                          [Reset]     |
| Order  Key            Label EN        Label TR          Tone       Used by   |
| 1      LEAD           [Lead        ]  [Aday müşteri  ]  [neutral v]  8        |
| 2      PROPOSAL_SENT  [Proposal sent] [Teklif gönd. ]  [info v]     5        |
| ...                                                                          |
| Allowed moves (read-only):  Lead -> Proposal sent, Closed  ...               |
| Live preview:  (Lead) (Proposal sent) (Active) (On hold) (Closed)            |
```

## Wireframe, phone

Section list is the first screen (iOS-style); tapping a section opens it full-width with a back arrow. Tables become cards: user card shows name, role dropdown, state badge, and a kebab menu. Status list rows expand to edit label and tone.

## Sections and fields

| Section | Fields | Actions |
| --- | --- | --- |
| Users | name, e-mail, role (Consultant / Manager / Admin), state (Active / Disabled), last login | Invite by e-mail (sends set-password link), change role, disable/enable. Never hard-delete (history keeps author names). An admin cannot demote or disable themselves; at least one Admin must remain |
| Roles | read-only matrix: role x capability (see below) | View only in Phase 1 (fixed roles per NFR-05) |
| Status lists | per kind (account, proposal, job, stage): `key`, label EN, label TR, tone, order, active flag, usage count, allowed next statuses (read-only) | Edit labels and tone, reorder, hide an unused status. Keys and transitions are fixed by code in Phase 1 (change request to architect if client wants editable transitions) |
| Import | file, column mapping, preview, result | Upload CSV/XLSX for accounts and contacts (A-11, Could) |
| Retention | retention period in months (default 24), counted from (upload or last contact: open question), reminder days before (60) | Admin only; every change is logged |
| Audit log | time, user, action (view CV, export, status change, role change, login), record | Filter by user, action, date; export CSV (NFR-05, NFR-11) |

Roles matrix (default, to confirm with client): Consultant = read/write own and team records, no Settings; Manager = all records, reassign owners, view users and audit log, import; Admin = everything plus users, roles, status lists, retention.

## States

- **Loading:** skeleton table. **Empty users:** impossible (the current user exists); invited-but-pending users show "Invitation pending" with Resend.
- **Status list guard:** hiding a status that is in use is blocked: "{n} records use this status. Move them first." Edits to labels show a live preview before saving.
- **Validation errors:** TR and EN labels both required; duplicate label blocked.
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
| hint.keysFixed | Keys and allowed moves are fixed. You can rename and recolour. | Anahtarlar ve izinli geçişler sabittir. Adı ve rengi değiştirebilirsiniz. |
| err.noAccess | You do not have access to Settings | Ayarlara erişiminiz yok |
| retention.months | Keep candidate data for (months) | Aday verisini saklama süresi (ay) |
