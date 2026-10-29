# Personal-data classification (contract)

Owner: architect. Plan rule (section 4): every column that can hold personal data is classified here, in the same pull request that adds it, for **erasure** (ADR 0009, `schema.md` "Anonymisation and deletion"), **audit** (Envers history) and **blind-report redaction** (client reports with `blind = true`, SX-022). `PersonalDataClassificationIT` (db-engineer, `platform-app`) fails when a column in scope has no row here.

## Codes

| Field | Code | Meaning |
| --- | --- | --- |
| Class | `ID` | Key, FK or opaque id. Personal only through what it points at |
| | `PD` | Personal data of the subject (identifying or descriptive) |
| | `SPD` | Pay, assessments, private notes: personal data with extra access rules or encryption |
| | `TP` | Data about a third person (referee, attendee, client contact named in text) |
| | `EVID` | Kept on purpose as legal evidence (consent, DSR, deletion record) |
| | `BIZ` | Business data or codes; no personal data |
| Erasure | `KEEP` | Unchanged on anonymisation (deletion: row cascades or stays, see table) |
| | `NULL`, `EMPTY`, `PH` | Set to null, to `'{}'`/`'[]'`, or to the placeholder (`[removed]`, `Anonymised person`) |
| | `DEL` | The row is deleted on anonymisation (JPA, so Envers records it) |
| | `CASC`, `SETNULL` | On person DELETE the FK cascades (row gone) or sets null (row stays) |
| | `DSR` | Also erased on a data-subject request from the third person (matched by e-mail) |
| Audit | `PURGE` | Envers history: rows `revend IS NOT NULL` deleted, current row gets the same placeholder (step 4) |
| | `CIPHER` | Audited as ciphertext; purged like `PURGE` |
| | `NA` | Table or column not audited |
| | `HIST` | Audited, no person data, history kept |
| Blind | `SHOW` | Printed as is |
| | `REDACT` | Replaced by a neutral label (`Candidate A`, initials never) |
| | `GEN` | Generalised (employer → sector and size band; city → country) |
| | `NEVER` | Never printed in any report, blind or not |
| | `—` | Not a report field |

Erasure is written `anonymise / delete`. A row may list several columns with the same classification.

## Scope of the test (`PersonalDataClassificationIT`)

1. **Tables in scope** (from `pg_constraint` and `information_schema`, live schema after all migrations): every table with an FK to `person`; every table with an FK to `contact`, `application`, `activity` or `person_file`; and the fixed list `person`, `account`, `access_log`, `timeline_entry`, `import_job`, `cv_intake_item`, `person_merge`, `client_report`, `export_job`. `_aud` tables are out of scope (the Audit field covers them).
2. **Every column** of a table in scope must match a row below by `table` and column name, or the `*` row (shared audit columns). Unmatched columns → the test fails and lists `table.column`.
3. **No stale rows:** every `table.column` named here must exist (catches renames).
4. **Consistency:** a `PD`, `SPD` or `TP` column may not have Audit `HIST`; a `PD`, `SPD` or `TP` row whose Erasure starts with `KEEP / KEEP` must give the reason after a `;` in the same cell (retention rule, DSR path or UI rule).
5. The parser reads the tables under "Columns" only: first cell table, second cell comma-separated columns, cells 3-6 the codes. Rows are kept sorted by table.

## Columns

| Table | Columns | Class | Erasure (anonymise / delete) | Audit | Blind |
| --- | --- | --- | --- | --- | --- |
| * | id, created_at, created_by, updated_at, updated_by, version | ID | KEEP / row rule | HIST | — |
| access_log | user_id, action, entity_type, entity_id, at | ID | KEEP / KEEP (no FK) | NA | — |
| access_log | ip_address | PD | KEEP / KEEP; user's own IP, 1-year log retention (NFR-05) | NA | — |
| account | name, sector, city, website, owner_id, status, status_changed_at, opened_on, open_job_count, last_activity_at, tags, archived_at, archived_by, archive_reason, merged_into_id, legal_name, tax_office, mersis_no, einvoice_status, einvoice_alias, billing_address, billing_currency, vat_exempt, withholding_applies, parent_account_id, group_name, employee_band, hq_country, confidentiality_level, preferred_language, is_client, is_prospect, is_target, is_employer, aliases, normalized_name | BIZ | KEEP / KEEP | HIST | GEN (client name only in non-blind reports) |
| account | notes, archive_note | BIZ | KEEP / KEEP; UI hint "no personal data" | HIST | — |
| account | tax_id | PD | KEEP / KEEP; a sole trader's TCKN is personal data: **DSR** of that person nulls it in `account` and every `account_aud` row (`ThirdPartyErasure`, matched by value) | PURGE (by value) | NEVER |
| account | billing_emails | TP | KEEP / KEEP; **DSR**: the address is removed from the array (`array_remove`, lower case) in `account` and every `account_aud` row | PURGE (by value) | — |
| activity | type, occurred_at, account_id, job_order_id, user_id, outcome, direction, duration_min, source | BIZ | KEEP / CASC | HIST | — |
| activity | person_id | ID | KEEP / CASC | HIST | — |
| activity | note | PD | PH / CASC | PURGE | — |
| activity | external_message_id | PD | NULL / CASC | PURGE | — |
| activity_attendee | activity_id, person_id | TP | DEL (attendee's row) / CASC | PURGE | — |
| agreement | account_id, job_order_id, kind, status, title, signed_on, valid_from, valid_until, language, governing_law, exclusivity, fee_model, fee_percent, fee_basis, flat_fee_amount, estimated_fee, fee_minimum, fee_cap, currency, payment_terms_days, guarantee_months, guarantee_start_basis, guarantee_terms, confidentiality_terms, expense_policy, off_limits_months, billing_account_id, notes, signed_file_id, signed_file_size, signed_file_uploaded_at, signed_file_uploaded_by | BIZ | KEEP / KEEP | HIST | — |
| agreement | signatory_contact_id | ID | KEEP / SETNULL | HIST | — |
| agreement | signed_file_name | BIZ | KEEP / KEEP; UI hint: no person name in file names. The PDF holds the signatory's name and signature (business contact, kept as contract evidence) | HIST | — |
| application | job_order_id, stage, stage_changed_at, rank, outreach_status, close_reason | BIZ | KEEP / CASC | HIST | — |
| application | person_id | ID | KEEP / CASC | HIST | REDACT |
| application | rejection_reason, off_limits_override_reason, rationale | SPD | PH / CASC | PURGE | NEVER |
| application | rating | SPD | NULL / CASC | PURGE | NEVER |
| candidate_evaluation | person_id, job_order_id, application_id, evaluator_id, evaluated_on, interview_type, internal_only, report_blocks | ID | DEL / CASC | PURGE | — |
| candidate_evaluation | overall_rating, recommendation, recommendation_reason, summary, key_achievements, motivation, motivation_tags, concerns, other_processes, scores | SPD | DEL / CASC | PURGE | SHOW only the `report_blocks`, with employer and school names GEN |
| candidate_evaluation | private_notes | SPD | DEL / CASC | PURGE | NEVER |
| candidate_exposure | person_id, account_id, job_order_id, application_id, channel, presented_at, presented_by | ID | KEEP / CASC (`application_id` SETNULL) | NA | — |
| candidate_reference | person_id, job_order_id, status, checked_on, checked_by, include_in_report | ID | DEL / CASC | PURGE | — |
| candidate_reference | summary | SPD | DEL / CASC | PURGE | SHOW when `include_in_report` (no referee identity) |
| candidate_reference | referee_name, referee_relationship, referee_company, referee_email, referee_phone | TP | DEL / CASC; **DSR**: a request whose e-mail equals `lower(referee_email)` sets these to `[removed]`/null and purges the `_aud` rows of those references | PURGE | NEVER |
| client_feedback | application_id, person_id, round, feedback_on, outcome, decline_reason, recorded_by, recorded_at | BIZ | KEEP / CASC | HIST | — |
| client_feedback | reason_text, candidate_feedback | SPD | PH / CASC | PURGE | NEVER |
| client_feedback | participants | TP | PH / CASC; holds **roles** of the client people present, never names (field label and UI hint), so there is nothing to match on a contact's own DSR | PURGE | NEVER |
| client_report | job_order_id, kind, version, language, blind, storage_key, size_bytes, generated_by, generated_at, sent_on, sent_by | ID | DEL (row and PDF) when `person_ids` overlaps / same | NA | — |
| client_report | person_ids | ID | DEL / DEL | NA | — |
| consent_record | person_id, decision, channel, notice_id, consent_request_id, recorded_at, recorded_by | EVID | KEEP / CASC | NA | — |
| consent_record | evidence, ip_address | PD | NULL / CASC | NA | — |
| consent_request | person_id, notice_id, language, token_hash, delivery_status, queued_at, sent_at, failure_reason, expires_at, responded_at, response, requested_by, purpose, opened_at, reminder_days_before, reminder_for_retain_until | EVID | KEEP / CASC | NA | — |
| contact | account_id, person_id, role, is_primary, ended_on | PD | KEEP (the person row is anonymised) / CASC | PURGE | — |
| cv_intake_item | batch_id, status, content_type, size_bytes, sha256, text_status, source_channel, attached_person_id, uploaded_by, uploaded_at, expires_at | ID | KEEP / CASC (attached) or 14-day purge | NA | — |
| cv_intake_item | file_name, storage_key, extracted_text, detected | PD | NULL (minimised) / CASC; DSR via `CvIntakeApi.findPendingByEmail` | NA | — |
| data_subject_request | person_id, type, status, received_at, due_on, resolved_at, handled_by | EVID | KEEP / SETNULL | PURGE (keeps current row) | — |
| data_subject_request | requester_name, requester_email, description | PD | PH / PH (step 1, HMAC e-mail) | PURGE | — |
| data_subject_request | resolution | EVID | KEEP / KEEP; must not hold personal data | PURGE | — |
| duplicate_candidate | person_a_id, person_b_id, reasons, status, detected_at, resolved_at, resolved_by | ID | DEL / CASC | NA | — |
| export_job | entity, status, filters, filter_keys, columns, sort, purpose, include_compensation, row_count, requested_by, requested_at, decided_by, decided_at, decision_reason, storage_key, ready_at, expires_at | ID | KEEP / KEEP; no `q`/`cv` text stored; the CSV bytes expire after 24 h | NA | — |
| import_job | kind, status, total_rows, valid_rows, duplicate_rows, imported_rows, failure_reason, completed_at, mapping, rolled_back_at | BIZ | KEEP / KEEP | NA | — |
| import_job | file_name, file_content, errors | PD | NULL / NULL (on end, after 30 days, and step 4) | NA | — |
| interview_note | person_id, job_order_id, application_id, interviewer_id, held_on, interview_type, include_in_report | ID | DEL / CASC | PURGE | — |
| interview_note | body | SPD | DEL / CASC | PURGE | SHOW when `include_in_report`, with names of third parties REDACT (consultant's duty, UI hint) |
| network_link | from_person_id, to_person_id, type | ID | DEL / CASC | PURGE | — |
| network_link | note | PD | DEL / CASC | PURGE | — |
| next_action | due_on, owner_id, account_id, job_order_id, done, done_at, source_event_id | BIZ | KEEP / CASC | HIST | — |
| next_action | person_id | ID | KEEP / CASC | HIST | — |
| next_action | title | PD | PH / CASC | PURGE | — |
| person | type, owner_id, consent_status, retention_status, anonymised_at, source_channel, archived_at, archived_by, archive_reason, last_contact_at | BIZ | KEEP / DEL (id-only `person_aud` DEL row kept, NFR-03) | PURGE | — |
| person | name | PD | PH / DEL | PURGE | REDACT |
| person | email, phone, linkedin_url, phone_normalized, linkedin_url_normalized | PD | NULL / DEL | PURGE | NEVER |
| person | title, function_area, seniority | PD | NULL / DEL | PURGE | SHOW |
| person | company | PD | NULL / DEL | PURGE | GEN |
| person | city | PD | NULL / DEL | PURGE | GEN |
| person | how_we_know, notes, archive_note | PD | NULL / DEL | PURGE | NEVER |
| person | tags, skills, languages | PD | EMPTY / DEL | PURGE | SHOW (skills, languages only) |
| person | notice_period_days, mobility | PD | NULL / DEL | PURGE | SHOW |
| person | compensation_enc | SPD | NULL / DEL | CIPHER | NEVER (only in non-blind reports with the `COMPENSATION` block, later contract) |
| person_education | person_id, sort_order | ID | DEL / CASC | PURGE | — |
| person_education | school, degree, field, start_year, end_year | PD | DEL / CASC | PURGE | SHOW (degree, field), GEN (school → "university") |
| person_employment | person_id, sort_order, organisation_id | ID | DEL / CASC | PURGE | — |
| person_employment | company, title, start_date, end_date | PD | DEL / CASC | PURGE | GEN (company), SHOW (title, years) |
| person_file | person_id, kind, version, is_current, content_type, size_bytes, sha256, text_status, extracted_at, uploaded_by | ID | DEL / CASC | PURGE | — |
| person_file | file_name, storage_key, extracted_text, text_tsv | PD | DEL (row and bytes) / CASC | PURGE (`extracted_text`, `text_tsv` NA) | NEVER |
| person_merge | survivor_id, merged_id, fields_taken, merged_at, merged_by | ID | KEEP / CASC (read in step 0) | NA | — |
| person_privacy | person_id, consent_status, notice_sent_at, basis_date, retain_until, retention_status, flagged_at, last_consent_at, import_id | EVID | KEEP (marked ANONYMISED) / CASC | PURGE | — |
| placement | application_id, person_id | ID | KEEP / SETNULL | PURGE (current row kept with nulls) | — |
| placement | job_order_id, account_id, agreement_id, status, offer_accepted_on, start_date, fee_amount, fee_currency, fee_overridden, guarantee_end_on, left_on, replacement_job_order_id | BIZ | KEEP / KEEP | HIST | — |
| placement | compensation_enc | SPD | NULL / NULL (listener, `person_id is null`) | CIPHER | NEVER |
| placement | status_reason | SPD | NULL / NULL | PURGE | NEVER |
| timeline_entry | account_id, occurred_at, kind, entity_type, entity_id, from_status, to_status, person_id, job_order_id, actor_id, source_event_id | ID | KEEP / KEEP (no FK) | NA | — |
| timeline_entry | reason, summary | PD | PH / PH | NA | — |

## Residual risks (accepted for the pilot)

- Free text written about a third person in a field of another record (an activity note naming an attendee, an interview note naming a former boss) cannot be found by that person's id. Mitigation: UI hints, and attendee and participant fields hold ids or roles, not names.
- `client_feedback.participants` holds roles, not names; a consultant may still type a name by mistake (scrubbed with the candidate only).
- The signed agreement PDF names the client's signatory: business contact data kept with the commercial record.
