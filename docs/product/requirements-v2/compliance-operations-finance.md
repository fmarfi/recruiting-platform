# Mihenk: compliance, operations and finance requirements (CO-001..CO-024)

Date: 2026-10-29. Author role: data protection and operations lead. Not legal advice. Legend: **VWC** = verify with counsel. Fact = cited source; Opinion = ours. Pilot priority: Must / Should / Could. Existing decisions (ADR 0011, `docs/product/research/domain-workflows.md`) are reused, not repeated.

## Research notes (secondary sources; no primary legal text read)

- Fact: data-subject requests are answered by the controller within 30 days at most; applying to the controller precedes any complaint to the Board ([KVKK announcement](https://kvkk.gov.tr/Icerik/6938/Kurumumuza-Yapilan-Sikayetlerin-Usul-Sartlarina-Iliskin-Kamuoyu-Duyurusu), [Mihci](https://mihci.av.tr/kvkk-sikayeti/)). VWC.
- Fact: breach notice to the Board is read as "as soon as possible, no later than 72 hours" (Decision 2019/10) ([Koksal](https://koksal.av.tr/en/services/compliance/data-breach-cyber-incident-response/), [CMS](https://cms.law/en/tur/legal-updates/kvkk-limits-publication-period-for-data-breach-notices-to-60-days)). Log records are listed among Art. 12 technical measures in commentary only ([Mondaq](https://www.mondaq.com/turkey/data-protection/1577742/cyber-attacks-and-data-breaches-compliance-strategies-for-companies)). VWC.
- Fact: Law 7499 (12.03.2024) and the transfer-abroad regulation (10.07.2024) reworked transfers; standard contracts must be notified to the Authority within 5 business days; explicit consent alone is no longer the general route ([Cott Group](https://www.cottgroup.com/en/blog/kvkk-gdpr/item/standard-contract-notification-obligation-and-penalties), [Prighter art. 14](https://prighter.com/resources/laws/turkish-kvkk/by-laws/the-procedures-and-principles-for-the-transfer-of-personal-data-abroad/articles/article-14)). Sources disagree on consent fallback details. VWC.
- Fact: VERBIS thresholds: 50 employees or TRY 100m balance sheet; stricter for special-category main activity; 2025 deadline moved to 5 June 2026 ([Erdem & Erdem](https://www.erdem-erdem.av.tr/en/insights/corporate-tax-returns-for-the-2025-fiscal-year-submitted-the-verbis-registration-process-may-have-started-for-your-company), [Mondaq](https://www.mondaq.com/turkey/data-protection/1813258/extension-of-the-registration-deadline-for-the-data-controllers-registry-information-system)). Product does not register; it exports the inventory. VWC.
- Fact: a deletion regulation (28.10.2017) requires a retention and destruction policy and periodic destruction; the interval (believed 6 months at most) was NOT confirmed in sources ([GSD policy](https://www.gsdbank.com.tr/assets/kvkk/Kişisel Veri Saklama ve İmha Politikası.pdf)). No statutory period for unhired candidates found; 2 years is a company choice ([TP Vision](https://www.tpvision.com/policy/TPV-Calisan-Adayi-Aydinlatma-Metni-v2.0.pdf)). VWC.
- Practice (vendor): Invenias tracks consent and lawful basis per person with an auditable history, a right-of-access report and erasure; advises against "consent" as database-wide default basis ([Invenias GDPR](https://kb.bullhorn.com/invenias/Content/Invenias/Topics/applyingDataPrivacyPoliciesInvenias.html)). Bullhorn erase anonymises linked entities ([KB](https://kb.bullhorn.com/ats/Content/BHATS/Topics/aboutGdprSupport.htm)).
- Invoicing/e-fatura, VKN, MERSIS, bureau-licence: see domain-workflows section 1.8 (all unverified, VWC / GIB).

---

## A. Consent, retention, data subject requests

**CO-001 Consent lifecycle.** Must. Every candidate has a consent state (NOT_SENT, SENT, GIVEN, REFUSED, WITHDRAWN, EXPIRED) driven by immutable versioned notice text; decisions are append-only. Rationale: proof of what was agreed, when, to which text. VWC (Law 6698 art. 5/6/10; lawful basis per source).
- GWT: Given a CV uploaded for a person with e-mail, when the upload completes, then a notice+consent mail in the person's language is queued unless one was sent within 7 days or the state is REFUSED/WITHDRAWN.
- GWT: Given GIVEN, when the person withdraws via link, then state WITHDRAWN, sharing blocked at once, withdrawal time stored.
- Fields: person_id, state, notice_version, channel, requested_at, decided_at, ip_hash, user_agent, evidence_ref, withdrawn_at.
- Edge: link opened by a mail scanner is not consent; no e-mail address (phone-only: manual record with consultant name and evidence note); notice text changes while SENT.

**CO-002 Sharing guard.** Must. Any move to a client-facing stage, report, PDF or share link requires consent GIVEN and not anonymised; no override. Rationale: sharing a CV with a client is a transfer (domain-workflows 1.8). VWC.
- GWT: Given consent SENT, when a consultant moves the person to PRESENTED, then 409 CONSENT_REQUIRED with a one-click resend.
- GWT: Given consent withdrawn one minute ago, when a report is generated, then the person is excluded and the exclusion is listed.
- Fields: share_check_result, block_reason, actor, at.
- Edge: report already sent before withdrawal (log shows it; offer "notify client to delete" mail template); bulk moves checked per record; blind report still needs consent (consultant may justify otherwise, counsel decides, VWC).

**CO-003 Consent evidence view.** Must. Per person: timeline of notices, versions, decisions, retention end with a one-line explanation ("24 months from last consent, 5 Mar 2026 to 5 Mar 2028"). Rationale: critique found retention end unexplained. 
- GWT: Given a person with GIVEN consent, when opened, then retention end and its basis are displayed in plain TR/EN.
- Fields: retain_until, retention_basis, last_consent_at.
- Edge: clock restarts on renewal; basis switched in settings (migration shown).

**CO-004 Retention and anonymisation.** Must. Configurable retention (default 24 months from last consent); reminders at 60 and 14 days; renewal link; auto-anonymise on expiry; destruction log. Rationale: storage limitation; periodic destruction duty. VWC (period, interval, anonymise vs delete).
- GWT: Given retain_until passed without renewal, when the nightly job runs, then profile, files and bytes are anonymised, a destruction record (id, date, method) is written, and exposure ids remain.
- GWT: Given an active search placement or open dispute, when expiry hits, then a manager sees a "legal hold" prompt instead of silent deletion.
- Fields: retention_settings(period, basis, reminder_days), destruction_log(person_id, at, method, actor), legal_hold(reason, set_by, until).
- Edge: person is also a client contact or placed candidate (keep minimal placement facts per counsel); merged persons; backups (CO-021); anonymised row must not be re-identifiable via tags or notes.

**CO-005 Data subject requests.** Must. DSR register with types (ACCESS, CORRECTION, DELETION, OBJECTION, WITHDRAWAL, TRANSFER-INFO), due date at receipt + 30 days, statuses, reply template, identity-check note, export pack including pending CV-intake items and compensation. VWC (30-day rule, identity verification, fee).
- GWT: Given a request received on day 0, when day 25 arrives with status OPEN, then owner and manager get a reminder; at day 30 it shows OVERDUE.
- GWT: Given ACCESS, when the manager exports, then one file lists data, recipients (clients shown to), sources and retention; the export is audit-logged.
- Fields: request_id, person_id, type, received_at, channel, verified_by, due_on, responded_on, outcome, reason_if_refused.
- Edge: requester not in database (log and answer "none found"); request through a client or lawyer (authority document); deletion conflicts with legal hold or invoice retention.

**CO-006 Special-category data guard.** Should. Free-text fields show a warning against health, religion, politics; a flag lets a manager mark and remove such notes; no structured fields for them. Rationale: art. 6 sensitivity. VWC.
- GWT: Given a note containing a configured keyword (sağlık, hamile, siyasi), when saved, then a non-blocking banner explains the rule and the note is flagged for review.
- Fields: flagged_note_id, flagged_by, reviewed_by.
- Edge: false positives; keywords per language.

**CO-007 Cross-border and processor statement.** Must. Settings page records hosting region, sub-processors, whether any data leaves Turkey, and transfer basis per recipient (e.g. client abroad); sharing to a foreign-based client shows a flag. Rationale: transfer rules changed in 2024. VWC (basis per transfer; standard contract notification).
- GWT: Given a client account with country not TR, when sharing a shortlist, then the manager sees "transfer abroad: basis recorded?" and cannot proceed until a basis is chosen or counsel waives it.
- Fields: account.country, transfer_basis enum (EXPLICIT_CONSENT, STANDARD_CONTRACT, OTHER_NOTED), contract_ref, notified_on.
- Edge: client uses foreign webmail; candidate asks where their data went.

**CO-008 Data inventory export.** Should. One-click inventory (categories, purposes, recipients, retention, transfers, security measures) for the firm's VERBIS/ROPA work. VWC (VERBIS status).
- GWT: Given configured settings, when exported, then a TR/EN document lists each data category with purpose, recipients, period; generated date and notice versions in force.
- Fields: category, purpose, recipient_types, retention, transfer, measure.
- Edge: client-contact data included; firm below thresholds still receives it.

## B. Audit, access, confidentiality, exports

**CO-009 Audit trail.** Must. Append-only log: who, what, when, from where, for VIEW of person detail, DOWNLOAD of file, EXPORT, share, consent change, role change, login, failed login, compensation read. Not editable by admins; retained at least as long as the data plus a configured tail. Rationale: accountability and breach investigation. VWC (log retention).
- GWT: Given a consultant opens a candidate, when the page loads, then a VIEW row exists with user, person_id, timestamp, IP.
- GWT: Given an admin, when trying to delete a log row, then it is impossible via UI and API.
- Fields: log_id, actor, action, entity_type, entity_id, at, ip, session_id, outcome, details_json (no raw personal data).
- Edge: logs of anonymised persons keep ids only; log volume (partition by month); log contains personal data itself, so access limited to ADMIN/auditor.

**CO-010 Compensation visibility.** Must. Compensation and notice-period details are field-encrypted; visible only to relationship owner, search team members with flag, and managers; others see "restricted". Rationale: ADR 0011 point 2; partners' top worry.
- GWT: Given a consultant not on the search, when opening the person, then compensation is null with restricted notice and no value in network responses.
- GWT: Given an authorised read, then a COMPENSATION_READ audit row is written.
- Fields: compensation_enc, key_version, can_view_comp (team member flag).
- Edge: key rotation; compensation inside CV text (cannot be hidden, document it); export excludes unless approved.

**CO-011 Search-level confidentiality.** Must. Searches and accounts have NORMAL / RESTRICTED / CONFIDENTIAL; CONFIDENTIAL is invisible (404) to non-team users in lists, search, timeline, dashboard counts, exports and notifications.
- GWT: Given a CONFIDENTIAL search, when a non-team consultant uses global search for the client name, then no result or count reveals it.
- GWT: Given a team member removed, then access ends immediately and the change is logged.
- Fields: confidentiality_level, team[], visibility_reason.
- Edge: partner override ("break glass") with reason and alert to sponsor partner; aggregated revenue reports still include the fee without the name.

**CO-012 Restricted candidates.** Should. A person can be flagged RESTRICTED (senior executive, sitting employee of a client): contact data and notes visible only to named owners; others see name initials and "ask owner".
- GWT: Given RESTRICTED, when a non-owner opens the profile, then e-mail, phone, notes, files are hidden and an "request access" action pings the owner.
- Fields: restricted_flag, owners[], reason.
- Edge: duplicate detection must not leak the hidden person (match says "possible duplicate, ask owner").

**CO-013 Export controls.** Must. Exports of people or accounts above N rows (default 25) need manager approval; every export audit-logged with filter, row count, columns, purpose; files expire in 24 hours; compensation excluded by default; watermark with user and date.
- GWT: Given a consultant requests "export all", when submitted, then status PENDING_APPROVAL and the manager is notified; no file exists until approved.
- GWT: Given approval, then the file link expires after 24 h and download is logged.
- Fields: export_id, requester, approver, filter_json, row_count, columns, purpose, approved_at, expires_at.
- Edge: approver cannot approve own export; partners' self-approval needs second admin (Could); scheduled exports forbidden in pilot.

## C. Import, users, backups, hosting

**CO-014 Import and migration.** Must. Spreadsheet (xlsx/csv) and Salesforce export import with column mapping, dry run, duplicate report, rollback by import batch for 30 days; imported people start with consent NOT_SENT and source = IMPORT with original source recorded; no client sharing until consent. Rationale: legacy data was collected under unknown terms. VWC (notice obligation for existing data).
- GWT: Given a 5,000-row file, when dry run finishes, then counts of new, duplicates, rejected (with reasons) show and nothing is stored.
- GWT: Given a bad import, when rolled back within 30 days, then all rows of the batch are removed and logged.
- Fields: import_job(file_hash, mapping, counts, user, at), per-row status, source_system, original_collected_at.
- Edge: Turkish encodings (windows-1254), i/İ, phone formats +90/0 5xx; spreadsheet with compensation or health columns (reject mapping to unsupported fields); consultants' private copies.

**CO-015 Users and roles.** Must. Roles ADMIN, MANAGER, CONSULTANT, RESEARCHER, FINANCE_VIEW, AUDITOR (read-only on logs), with a permission matrix page; invite by e-mail, deactivate (not delete) leavers, owner reassignment wizard, MFA for ADMIN/MANAGER, session timeout. Rationale: leavers are the main leak path.
- GWT: Given a leaver is deactivated, when the admin confirms, then sessions end, tokens revoke, and open records must be reassigned or marked.
- GWT: Given FINANCE_VIEW, then no candidate personal data is visible, only fee and placement figures.
- Fields: user, role, team_memberships, mfa_enrolled, last_login, deactivated_at, reassigned_to.
- Edge: last ADMIN cannot be removed; shared mailboxes forbidden; password policy per counsel/IT.

**CO-016 Backups and restore.** Must. Daily encrypted backups of database and file bytes, retention 30 days, restore test quarterly with a report; restored data re-applies erasures made since the backup (erasure replay log). VWC (backup retention versus erasure).
- GWT: Given a person was anonymised after the backup date, when a restore is performed, then the replay job re-anonymises them before the system opens.
- Fields: backup_id, at, size, checksum, region, restore_test_on, result, erasure_log.
- Edge: bytes and database backups must match in region; developer copies never contain real data.

**CO-017 Hosting and encryption statement.** Must. A settings page and a one-pager state: hosting region, provider, TLS version, encryption at rest, field-level compensation encryption, key custody, sub-processors, incident contact; versioned. Rationale: the first question of a Turkish buyer. VWC (processor agreement, transfer).
- GWT: Given an admin, when the statement is edited, then a new version is stored and the PDF regenerates with date.
- Fields: region, provider, encryption_at_rest, tls_min, key_custody, subprocessors[], dpo_contact, version.
- Edge: statement must match reality; a CI check compares settings with deployment config (Could). Decision pending in `docs/architecture/hosting.md`.

**CO-018 Incident register.** Should. Log of security incidents and breaches with detection time, 72-hour countdown, affected count, notified Board/individuals, evidence. VWC (72h).
- GWT: Given an incident is opened, then a countdown to detection+72h shows and a notification template (TR) is prepared.
- Fields: incident_id, detected_at, description, affected_people[], board_notified_at, subjects_notified_at, measures.
- Edge: partial info allowed; affected list generated from audit logs.

## D. Finance and operations

**CO-019 Billing entity data.** Must. Accounts hold legal name, VKN/TCKN/foreign tax id, tax office, MERSIS (16 digits, length only), e-fatura status and alias, billing address and e-mails, currency, VAT/withholding flags; activation gate requires them. Rationale: invoice rework. VWC/GIB (VKN check digit, e-fatura thresholds).
- GWT: Given an account missing tax office, when activating as client, then the gate lists the missing item.
- GWT: Given a duplicate VKN on another account, then a warning shows both.
- Fields: see domain-workflows 3.1 section B.
- Edge: foreign clients; sole proprietors; group billing entity differs from search entity.

**CO-020 Invoicing triggers.** Must. Fee schedule items fire from SIGNING, DATE_OFFSET, SHORTLIST_DELIVERED, OFFER_ACCEPTED, START_DATE, EXPENSES, ABORT; reaching a trigger flips NOT_DUE to DUE and lists it on "Invoice ready" for finance; finance records e-fatura/e-arşiv reference, date, amount, state; no invoice issuing in pilot. VWC (VAT, withholding, timing).
- GWT: Given retainer 2 is DATE_OFFSET +30, when day 30 passes, then item DUE and visible to FINANCE_VIEW.
- GWT: Given finance marks INVOICED without reference, then it is rejected.
- Fields: item_id, trigger_type, percent/amount, currency, due_days, state, invoice_ref, invoiced_on, paid_on, credit_note_ref.
- Edge: percentages not summing to 100; fee changes after shortlist; cancelled search; milestone reversed (flag, not auto-undo).

**CO-021 Placements and guarantee.** Must. Placement record (start date, base, bonus, first-year cash encrypted, fee, guarantee end, left-on, replacement search) with alerts 30 days before guarantee end and on departure; retention checks at 6/12 months. Never a fee field on candidates. Bureau licence question: VWC.
- GWT: Given a placement with 12-month guarantee, when 30 days remain, then owner and manager get an alert.
- GWT: Given left_on within guarantee, then a "replacement search" action creates a linked zero-fee search.
- Fields: placement fields in domain-workflows 3.3 plus guarantee_exclusions, check_6m, check_12m.
- Edge: start date slips; candidate rejects after acceptance; guarantee extended by amendment.

**CO-022 Revenue reporting.** Should. Reports for invoiced, collected, contracted, fee-weighted pipeline by consultant (with fee credit split), client, month, currency; shows only to MANAGER/ADMIN/FINANCE_VIEW; export follows CO-013; FX shown per currency, no silent conversion.
- GWT: Given fee credit 50/30/20, when a placement is invoiced, then each consultant's revenue equals the split.
- Fields: fee_credit(user, role, percent), report_period, currency.
- Edge: credit notes; confidential searches (CO-011); percent not 100.

**CO-023 E-mail templates TR/EN.** Must. Templates for notice+consent, renewal, reminders, DSR reply, client report cover, shortlist note, referee request; language by person/contact preference; placeholders validated; versioned; consent templates editable only by ADMIN and tied to notice version.
- GWT: Given a person with language TR, when the consent mail renders, then TR text with the person's name and the active notice version link.
- GWT: Given an unknown placeholder, when saving a template, then save fails with the list.
- Fields: template_id, key, language, subject, body, version, active, edited_by.
- Edge: missing translation falls back to firm default with a warning; Turkish characters in subject encoding; unsubscribe/opt-out wording for marketing mail (VWC, e-ticaret rules).

**CO-024 Mail logging.** Must. Every system mail logged (template, version, recipient hash+address, sent, delivered, bounced, link opened) and shown on the person timeline; bounced consent mails raise a task; logs follow retention.
- GWT: Given a consent mail bounces, when the provider reports it, then state stays SENT, a "bad address" task appears, no resend loop.
- Fields: mail_id, person_id, template_key, version, sent_at, status, provider_id, opened_at.
- Edge: consultants' own mailbox mail (out of scope, no sync in pilot); provider region (CO-007).

---

## Draft: security and privacy statement (for client use; counsel to approve)

**English.** Mihenk holds candidate and client data for [Firm] only. Data is hosted at [provider, region: TO BE CONFIRMED]. Connections use TLS; storage is encrypted at rest; compensation values have an additional field-level encryption. Access is by named user accounts with roles; confidential searches and restricted candidates are visible only to the assigned team. Every view, download, export and sharing action is logged and cannot be altered by administrators. Candidates receive an information notice and a separate explicit-consent request; no candidate profile is shared with a client without recorded consent, which the system enforces. Consent can be withdrawn at any time; data is retained for [24] months from last consent and then anonymised. Data subject requests are answered within the legal period. Exports above a threshold need manager approval. Backups are encrypted and restore-tested. [Firm] decides purposes and means of candidate data; sub-processors: [list]. Transfers abroad only on a recorded legal basis. Incidents are logged and reported as required by law. Contact: [DPO/contact]. Version [x], date [y].

**Turkce.** Mihenk, aday ve musteri verilerini yalnizca [Firma] adina saklar. Veriler [saglayici, bolge: DOGRULANACAK] uzerinde barindirilir. Baglantilar TLS ile korunur, depolanan veri sifrelidir; maas bilgileri ayrica alan duzeyinde sifrelenir. Erisim kisiye ozel hesaplar ve rollerle verilir; gizli aramalar ve kisitli adaylar yalnizca atanmis ekibe gorunur. Goruntuleme, indirme, disa aktarma ve paylasim islemleri kayit altindadir ve yoneticiler tarafindan degistirilemez. Adaylara aydinlatma metni ve ayrica acik riza talebi gonderilir; kaydedilmis riza olmadan hicbir aday profili musteriyle paylasilamaz, sistem bunu zorunlu kilar. Riza her zaman geri alinabilir; veriler son rizadan itibaren [24] ay saklanir, sonra anonimlestirilir. Ilgili kisi basvurulari yasal surede yanitlanir. Belirli bir esigin ustundeki disa aktarimlar yonetici onayi gerektirir. Yedekler sifreli ve geri yukleme testlidir. Aday verisinin amac ve araclarini [Firma] belirler; alt isleyiciler: [liste]. Yurt disina aktarim yalnizca kayitli hukuki dayanakla yapilir. Olaylar kaydedilir ve mevzuatin gerektirdigi sekilde bildirilir. Iletisim: [sorumlu]. Surum [x], tarih [y].

## Ten questions a Turkish firm's partner will ask

1. Where are the servers (Turkey or abroad), and who can see them?
2. Is the firm or the vendor the veri sorumlusu, and where is the data processing agreement (VERBIS status)?
3. Can an unauthorised consultant see a candidate on a confidential search, or the CEO's salary?
4. What happens when a consultant leaves: what do they take, and how fast is access cut?
5. Who can export the whole database, and is it recorded?
6. How do we prove a candidate consented before the CV went to the client?
7. What about the 80,000 old records in our spreadsheets and Salesforce with no consent?
8. How long do you keep candidates, and what exactly is deleted, including backups?
9. What if the KVKK Board or a candidate asks us for everything we hold within 30 days?
10. If there is a breach, who tells us, how fast, and what evidence do we have?

All answers need counsel review (VWC) before external use.
