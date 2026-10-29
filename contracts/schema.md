# Database schema (contract)

Owner: architect; the db-engineer turns it into Flyway migrations and JPA entities. Source: the core data model in `docs/product/requirements.md`, the demo domain, `contracts/openapi.yaml`.

PostgreSQL 16+ with pgvector (vectors only in Phase 2). One schema, `public`.

## Conventions

- **Primary keys:** `id uuid`, UUID v7 created by the application (ADR 0004). No database sequences except the Envers revision number.
- **Foreign keys:** `<table>_id uuid`, always with an FK constraint and an index. FKs never cross a module boundary *for writes*: a module may hold the id of another module's row (FK constraint kept for integrity), but only the owning module writes the row.
- **Time:** `timestamptz` for instants (`*_at`), `date` for calendar days (`*_on`, `*_date`). Store UTC; the business time zone is `Europe/Istanbul`.
- **Enums:** `varchar(40)` holding the Java enum name, with a `CHECK (col IN (...))` built from `contracts/enums/`. No PostgreSQL enum types (they make adding a status a migration pain).
- **Audit columns** on every mutable table: `created_at`, `created_by` (FK `app_user`, null for system jobs), `updated_at`, `updated_by`, `version bigint not null default 0` (JPA `@Version`).
- **Envers:** every table marked *audited* gets `<table>_aud` plus the shared `revinfo` (ADR 0005). Columns listed as *not audited* are excluded with `@NotAudited`.
- **Free-text search:** `unaccent` + `pg_trgm` extensions; the search columns get a GIN trigram index on `immutable_unaccent(lower(col))` so that `ozturk` finds `Öztürk` (A-14).
- **Naming:** snake_case, singular table names, `app_user` because `user` is reserved.
- **Migrations:** one Flyway location `platform-app/src/main/resources/db/migration`, file names `V<yyyyMMddHHmm>__<module>_<what>.sql` so parallel branches never clash on version numbers.

**Enum CHECKs added in 1.3.0 (MigrationIT `ENUM_COLUMNS`, one `EnumColumn(table, column, Enum.class)` each):** `account`: `archive_reason` ArchiveReason, `einvoice_status` EInvoiceStatus, `employee_band` EmployeeBand, `confidentiality_level` ConfidentialityLevel, `preferred_language` Language · `person`: `archive_reason`, `function_area` JobFunction, `seniority` Seniority · `job_order`: `archive_reason`, `confidentiality_level` · `tag_definition.colour` TagColour · `application.outreach_status` OutreachStatus (SCREENED added in the review round) · `application.close_reason` CloseReason · `activity`: `outcome` ActivityOutcome, `direction` ActivityDirection, `source` ActivitySource · `client_feedback`: `outcome` ClientFeedbackOutcome, `decline_reason` DeclineReason · `candidate_evaluation`: `interview_type` InterviewType, `recommendation` Recommendation · `interview_note.interview_type` · `candidate_reference.status` ReferenceStatus · `search_brief`: `seniority_level`, `function_area` (no `comp_period` CHECK: the band is encrypted, R73) · `search_criterion.kind` CriterionKind · `job_order_team_member.role` SearchTeamRole · `target_company`: `tier` TargetTier, `status` TargetCompanyStatus · `agreement`: `kind` AgreementKind, `status` AgreementStatus, `language`, `exclusivity` Exclusivity, `fee_model` FeeModel, `guarantee_start_basis` GuaranteeStartBasis · `fee_schedule_item`: `trigger` FeeTrigger, `invoice_state` InvoiceState · `placement.status` PlacementStatus · `conflict_check.decision` ConflictDecision · `export_job`: `entity` ListEntity, `status` ExportStatus · `client_report`: `kind` ReportKind, `language` · `import_job_record.entity_type` EntityType. Array CHECKs (`arrayCheckConstraintsMatchTheContractEnums`): `candidate_evaluation.motivation_tags` MotivationTag, `candidate_evaluation.report_blocks` EvaluationBlock, `agreement.fee_basis` FeeBasisComponent. **Re-created** CHECKs (new values): `access_log.entity_type` and `timeline_entry.entity_type` (EntityType), `timeline_entry.kind` (TimelineKind: ARCHIVE, MERGE, PLACEMENT), `import_job.status` (ImportStatus: ROLLED_BACK). New audited tables for the `_aud` existence check: `activity_attendee`, `tag_definition`, `client_feedback`, `candidate_evaluation`, `interview_note`, `candidate_reference`, `search_brief`, `search_criterion`, `job_order_team_member`, `target_company`, `agreement`, `fee_schedule_item`, `placement`.

## Ownership by module

| Module | Tables |
| --- | --- |
| security | `app_user`, `access_log`, `revinfo`; since 1.3.0 `export_job` |
| accounts | `account`, `person`, `person_employment`, `contact`, `network_link`, `import_job`, `account_open_job`, `saved_view`; since 1.2.0 `person_education`, `person_file`, `cv_intake_item`, `duplicate_candidate`, `person_merge`, `off_limits_rule`; since 1.3.0 `account_merge`, `tag_definition`, `import_job_record` |
| pipeline | `job_order`, `application`, `proposal`, `activity`, `next_action`, `timeline_entry`, `bulk_action`, `bulk_action_item`; since 1.2.0 `candidate_exposure`; since 1.3.0 `client_feedback`, `candidate_evaluation`, `interview_note`, `candidate_reference`, `search_brief`, `search_criterion`, `job_order_team_member`, `target_company`, `agreement`, `fee_schedule_item`, `placement`, `conflict_check`, `client_report` |
| privacy | `person_privacy`, `privacy_notice`, `consent_record`, `consent_request`, `retention_settings`, `retention_run`, `data_subject_request` |
| platform-app (Modulith) | `event_publication` (Spring Modulith JPA event registry; the db-engineer writes the Flyway migration for the table the Spring Modulith JPA starter expects, because `ddl-auto` is `validate` and nothing creates it automatically) |

## Relationships

```
app_user 1─* account.owner_id, person.owner_id, job_order.owner_id, proposal.owner_id, next_action.owner_id, activity.user_id
account  1─* contact *─1 person            (A-03: a contact is a person in a role at an account)
person   1─* person_employment             (A-12 company history)
person   *─* person via network_link       (A-13, directed from → to)
account  1─* job_order 1─* application *─1 person   (A-06, A-07; one person on many job orders)
account  1─* proposal                       (A-05)
activity *─1 account?, person?, job_order?  (A-04; at least one of account/person)
next_action *─1 account?, person?, job_order?  (A-10)
account  1─* timeline_entry                 (A-08, written by event listeners)
person   1─1 person_privacy 1─* consent_record, consent_request   (S-01, NFR-02)
privacy_notice 1─* consent_record, consent_request
person   1─* data_subject_request (nullable until identified)        (S-04)
app_user 1─* saved_view.owner_id                                     (grid views, ADR 0010)
app_user 1─* bulk_action 1─* bulk_action_item                       (mass-action audit, ADR 0010)
person   1─* person_education, person_file                          (candidate profile and CVs, 1.2.0, ADR 0011)
person   *─* person via duplicate_candidate; person 1─* person_merge (B-04)
account  0..1─* off_limits_rule                                      (off-limits guard)
person   1─* candidate_exposure *─1 account, job_order, application? (exposure history, B-05)
account  0..1─* account.parent_account_id; account 1─* account_merge   (group, merge; 1.3.0)
account  0..1─* person_employment.organisation_id; off_limits_rule.include_subsidiaries walks the parent tree (R63, 1.3.0)
application 1─* client_feedback; person 1─* candidate_evaluation, interview_note, candidate_reference (1.3.0)
job_order 1─0..1 search_brief; job_order 1─* search_criterion, job_order_team_member, target_company, client_report (1.3.0)
account  1─* agreement 1─* fee_schedule_item; job_order 0..1─* agreement (engagement)               (1.3.0)
application 1─0..1 placement *─1 person, job_order, account, agreement?                           (1.3.0)
account  0..1─* conflict_check; import_job 1─* import_job_record; app_user 1─* export_job          (1.3.0)
```

Since 1.2.0 (ADR 0011) the non-AI candidate profile lives **on `person` in `accounts`** (columns plus `person_education` and `person_file`), not in a separate `candidate_profile` table. Phase 2 (`ai`) adds only AI data: parsed structure, `cv_embedding vector(n)` keyed by `person_file.id`, an AI score on `application`. Retention and consent already work on `person`, so nothing moves (ADR 0008).

## Tables

### security

**app_user** — audited (except `password_hash`, `last_login_at`)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| email | varchar(254) | unique on `lower(email)` |
| name | varchar(120) | not null |
| role | varchar(40) | `Role` |
| active | boolean | default true |
| language | varchar(2) | `Language`, default `TR` |
| password_hash | varchar(100) | bcrypt/argon2 (Spring `DelegatingPasswordEncoder` format) |
| must_change_password | boolean | default true |
| last_login_at | timestamptz | null |
| audit columns | | |

**access_log** — append-only, not audited (NFR-05: who viewed or exported a CV)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| user_id | uuid FK app_user | not null |
| action | varchar(40) | `AccessAction` |
| entity_type | varchar(40) | `EntityType` |
| entity_id | uuid | no FK (entity may be anonymised or deleted) |
| at | timestamptz | not null; index `(entity_type, entity_id, at desc)` and `(user_id, at desc)` |
| ip_address | varchar(45) | null |

**export_job** — not audited; it is itself the export log (1.3.0, CO-013, ADR 0013)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| entity | varchar(40) | `ListEntity` |
| status | varchar(40) | `ExportStatus`; index `(status, requested_at)` |
| filters | jsonb | not null; the filters **without** `q` and `cv` (free text may name a person); `CHECK (NOT (filters ?\| array['q','cv']))` |
| filter_keys | varchar(50)[] | not null; every key used, including `q`/`cv` |
| columns | varchar(100)[] | not null |
| sort | varchar(100)[] | not null default `'{}'` |
| purpose | varchar(300) | not null; no personal data (UI hint) |
| include_compensation | boolean | not null default false |
| row_count | int | not null |
| requested_by | uuid FK app_user | not null; index `(requested_by, requested_at desc)` |
| requested_at | timestamptz | not null |
| decided_by | uuid FK app_user | null |
| decided_at | timestamptz | null |
| decision_reason | varchar(500) | null |
| storage_key | varchar(300) | null; `export/<id>` in the `FileStore`; null after expiry |
| ready_at, expires_at | timestamptz | null; `expires_at = ready_at + 24 hours` |

A nightly job in `security` deletes the bytes of READY exports past `expires_at` and sets EXPIRED. The CSV holds personal data only during those 24 hours; the row holds none.

**revinfo** — Envers revision entity: `rev bigint PK` (sequence `revinfo_seq`), `revtstmp bigint`, `user_id uuid null` (custom `RevisionListener` reads the current user).

### accounts

**account** — audited (except `open_job_count`, `last_activity_at`, `normalized_name`); the organisation table since 1.3.0 (R63)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| name | varchar(200) | not null; trigram index. Since 1.3.0 the uniqueness moves to `normalized_name` (the old unique index on `lower(name)` is dropped) |
| sector | varchar(100) | null since 1.3.0 (was not null); `CHECK (sector IS NOT NULL OR NOT (is_client OR is_prospect))`; index |
| city | varchar(100) | null |
| website | varchar(300) | null |
| notes | text | null |
| owner_id | uuid FK app_user | not null; index |
| status | varchar(40) | `AccountStatus`, default `LEAD`; index |
| status_changed_at | timestamptz | not null |
| opened_on | date | not null, default today |
| open_job_count | int | not null default 0; derived: `count(*) from account_open_job where account_id = ? and open`, refreshed after each projection upsert |
| last_activity_at | timestamptz | null; maintained from `ActivityLogged`; index desc |
| tags | varchar(50)[] | not null default `'{}'`; lower-case, at most 20 (`CHECK (cardinality(tags) <= 20)`); GIN index; written by `ADD_TAG`/`REMOVE_TAG` bulk actions (ADR 0010) |
| archived_at, archived_by, archive_reason, archive_note | timestamptz, uuid FK app_user, varchar(40), varchar(500) | all null (1.3.0, ADR 0013); see "Archive columns" below |
| merged_into_id | uuid FK account | null (1.3.0); index; `CHECK ((merged_into_id IS NULL) = (archive_reason IS DISTINCT FROM 'MERGED'))` |
| legal_name | varchar(300) | null (1.3.0); invoice name |
| tax_id | varchar(20) | null (1.3.0); plain string, no format CHECK; index on `tax_id` (duplicate warning by the service) |
| tax_office | varchar(100) | null (1.3.0) |
| mersis_no | varchar(16) | null (1.3.0); no format CHECK |
| einvoice_status | varchar(40) | `EInvoiceStatus`, not null default `UNKNOWN` (1.3.0) |
| einvoice_alias | varchar(100) | null (1.3.0) |
| billing_address | varchar(500) | null (1.3.0) |
| billing_emails | varchar(254)[] | not null default `'{}'`; `CHECK (cardinality(billing_emails) <= 5)` (1.3.0) |
| billing_currency | char(3) | null (1.3.0) |
| vat_exempt, withholding_applies | boolean | not null default false (1.3.0) |
| parent_account_id | uuid FK account | null (1.3.0); index; `CHECK (parent_account_id <> id)`; no cycles and depth at most 5 checked by the service |
| group_name | varchar(200) | null (1.3.0) |
| employee_band | varchar(40) | null; `EmployeeBand` (1.3.0) |
| hq_country | char(2) | null (1.3.0); `city` is the HQ city |
| confidentiality_level | varchar(40) | `ConfidentialityLevel`, not null default `NORMAL` (1.3.0) |
| preferred_language | varchar(2) | null; `Language` (1.3.0) |
| is_client, is_prospect, is_target, is_employer | boolean | not null default false (1.3.0, R63; `OrganisationRole`); `CHECK (is_client OR is_prospect OR is_target OR is_employer)`; partial indexes `where is_client`, `where is_target`, `where is_employer`. Backfill: `is_client = status IN ('ACTIVE','ON_HOLD','CLOSED')`, `is_prospect = status IN ('LEAD','PROPOSAL_SENT')`; `is_target` true where a `target_company.account_id` points at it; `is_employer` false |
| aliases | text[] | not null default `'{}'` (1.3.0, R63); former names, brands, abbreviations; `CHECK (cardinality(aliases) <= 20)`; each 1..200 characters (service); GIN expression index on `normalize_org_names(aliases)` |
| normalized_name | varchar(200) | 1.3.0, R63: `generated always as (normalize_org_name(name)) stored`; **unique partial index `(normalized_name) where archived_at is null`** (merged and archived rows may share a name); btree index for matching. JPA maps it read-only (`insertable = false, updatable = false`) and `@NotAudited`; `account_aud` has no such column |
| audit columns | | |

**Organisation identity (1.3.0, R63, ADR 0013):** `account` is the organisation table; it is not renamed. Roles are the four flags; `status` stays the client lifecycle. `parent_account_id` is the group tree (depth at most 5, no cycles; service). Two immutable SQL functions are created by the accounts migration (before the generated column):
- `normalize_org_name(text) returns text`: (1) `lower(immutable_unaccent(x))` with `ı` → `i`; (2) `regexp_replace(_, '[^a-z0-9]+', ' ', 'g')`; (3) remove `\m(sanayi|san) ve (ticaret|tic)\M`; (4) remove the legal-form tokens `\m(a s|as|ltd|sti|limited|sirketi|anonim|sanayi|ticaret|san|tic|inc|llc|gmbh|plc|bv|nv|ag|co|corp)\M`; (5) collapse spaces and trim; if the result is empty, return the step-2 result. Example: `Örnek Gıda Sanayi ve Ticaret A.Ş.` → `ornek gida`.
- `normalize_org_names(text[]) returns text[]`: the same per element.

Matching (`OrganisationApi.match(name)`, used by import, employment entry, target companies and off-limits): `normalize_org_name(:name)` equals a non-archived row's `normalized_name` or is in its `normalize_org_names(aliases)`; one hit = match, several = ambiguous (no link, the UI asks). An alias equal to another non-archived organisation's normalized name or alias gives 409 `DUPLICATE` (service check). Migration pre-check: the migration fails with the list of clashing ids if two non-archived rows normalise to the same value (synthetic seed: rename one). Test `OrganisationNameNormalisationIT`: the example above, `ACME Ltd. Şti.` = `Acme`, `Marmara Holding` keeps `holding`, dotless `ı`, and the unique index rejects a second active `Örnek Gıda A.Ş.`.
Account merge (1.3.0) also ORs the four flags into the survivor, adds the merged name and aliases to the survivor's aliases (de-duplicated, at most 20, longest dropped first), and moves `person_employment.organisation_id`.

**Archive columns (1.3.0, ADR 0013)** on `account`, `person`, `job_order` (proposal archive is cut from 1.3, R7/R75): `archived_at timestamptz null`, `archived_by uuid FK app_user null`, `archive_reason varchar(40) null` (`ArchiveReason`), `archive_note varchar(500) null`, with `CHECK ((archived_at IS NULL) = (archive_reason IS NULL))` and `CHECK ((archived_at IS NULL) = (archived_by IS NULL))`. Index: partial `(archived_at) where archived_at is not null` (the Archived view); list queries add `archived_at is null`. All four columns are audited (Envers gives who/when for archive and restore). Existing rows: all null, so the default list behaviour is unchanged.

**account_merge** — append-only, not audited (1.3.0): `id uuid PK`, `survivor_id uuid FK account not null` (index), `merged_id uuid FK account not null` (unique; the merged account stays as an archived row, so a normal FK works), `moved jsonb not null` (counts per kind, no names), `merged_at timestamptz not null`, `merged_by uuid FK app_user not null`. No personal data.

**tag_definition** — audited (1.3.0, ADR 0013)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| name | varchar(50) | not null; unique; lower-case, same rule as the `tags` arrays |
| colour | varchar(40) | `TagColour`, not null default `GREY` |
| description | varchar(200) | null; no personal data |
| talent_pool | boolean | not null default false; a talent pool is a tag with this flag (`GET /people?tag=`) |
| audit columns | | |

A tag exists when any record carries it, with or without a definition row; `GET /tags` unions the distinct values of the four `tags` arrays with `tag_definition`. Rename, merge and delete rewrite the arrays with one set-based `UPDATE ... SET tags = <array op>, version = version + 1` per table (accounts: `account`, `person`; pipeline: `job_order`, `proposal`, through `TagChanged`). These bulk rewrites write **no** Envers revision per record (tags are not personal data; the `tag_definition` revision and the `TagChanged` event are the audit), and bumping `version` makes open editors get `VERSION_CONFLICT` instead of writing the old tag back.

**person** — audited (except `last_contact_at`, `consent_status`, `retention_status`)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| name | varchar(200) | not null; trigram index |
| type | varchar(40) | `PersonType`; index |
| title | varchar(200) | null |
| company | varchar(200) | null; trigram index |
| email | varchar(254) | null; unique on `lower(email)` where not null |
| phone | varchar(50) | null |
| city | varchar(100) | null; btree index on `immutable_unaccent(lower(trim(city)))` for the `city` filter (1.1.1) |
| linkedin_url | varchar(300) | null |
| how_we_know | text | null |
| notes | text | null |
| owner_id | uuid FK app_user | not null; index |
| last_contact_at | timestamptz | null; from `ActivityLogged` where `type.countsAsContact()`; index |
| consent_status | varchar(40) | `ConsentStatus`, default `NOT_SENT`; read-only copy, maintained from `ConsentStatusChanged` |
| retention_status | varchar(40) | `RetentionStatus`, default `ACTIVE`; copy, maintained from `RetentionFlagged` and anonymisation |
| anonymised_at | timestamptz | null; when set, personal columns hold placeholders |
| tags | varchar(50)[] | not null default `'{}'`; lower-case, at most 20 (`CHECK (cardinality(tags) <= 20)`); GIN index; written by `ADD_TAG`/`REMOVE_TAG` bulk actions (ADR 0010) |
| source_channel | varchar(40) | null; `SourceChannel`; index (1.2.0). Not personal data: kept on anonymisation |
| notice_period_days | smallint | null; `CHECK (notice_period_days BETWEEN 0 AND 365)` (1.2.0) |
| mobility | varchar(40) | null; `Mobility` (1.2.0) |
| compensation_enc | bytea | null; the `Compensation` JSON encrypted with AES-256-GCM by the JPA converter `security.FieldCipher` (format: 1 byte key version, 12 byte IV, ciphertext + tag). Key from env `FIELD_ENCRYPTION_KEY_V<k>`, current version `FIELD_ENCRYPTION_KEY_VERSION`. Audited as ciphertext. Never indexed. Readable and writable only by MANAGER/ADMIN and the relationship owner (`owner_id`); reads ride on the VIEW access-log row of `GET /people/{id}` (1.2.0) |
| languages | jsonb | not null default `'[]'`; `[{language, level}]`, at most 20, validated by the service (1.2.0) |
| phone_normalized | varchar(20) | null; digits with country code (`+90` default), written by the service on every phone change; index (duplicate check, 1.2.0) |
| linkedin_url_normalized | varchar(300) | null; lower case, `https://www.linkedin.com/in/<slug>` form, no query, no trailing slash; index (1.2.0) |
| archived_at, archived_by, archive_reason, archive_note | | archive columns (1.3.0); `archive_note` is personal data (scrubbed on anonymisation) |
| skills | varchar(50)[] | not null default `'{}'`; lower-case; `CHECK (cardinality(skills) <= 30)`; GIN index (1.3.0, `skill` filter) |
| function_area | varchar(40) | null; `JobFunction`; index (1.3.0) |
| seniority | varchar(40) | null; `Seniority`; index (1.3.0) |
| audit columns | | |

Pool filters (1.3.0): `skill` uses `skills && :skills` (GIN), `language` uses `languages @> '[{"language":"en"}]'` (GIN `jsonb_path_ops` index on `languages`, new), `noticeMaxDays` and `mobility` use the plain columns. The salary filter cannot use SQL because `compensation_enc` is encrypted: the service applies it in memory after the SQL filters (ADR 0013, max 5,000 rows before the salary step).

Full-text support (1.2.0): a text search configuration `tr_unaccent` (`COPY = pg_catalog.turkish`, mapping `asciiword, word, hword, hword_part, asciihword, hword_asciipart` to `unaccent, turkish_stem`). Duplicate name match uses `immutable_unaccent(lower(trim(name)))` and the same on `company` (expression index on the pair).

**person_education** — audited (1.2.0); `id uuid PK`, `person_id FK person on delete cascade` (index), `school varchar(200) not null`, `degree varchar(100)`, `field varchar(200)`, `start_year smallint`, `end_year smallint` (`CHECK (end_year IS NULL OR start_year IS NULL OR end_year >= start_year)`), `sort_order int not null`. Replaced as a whole on update, like `person_employment`.

**person_file** — audited except `extracted_text` and `text_tsv` (`@NotAudited`) (1.2.0, ADR 0011)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| person_id | uuid FK person | not null; `on delete cascade`; index `(person_id, kind, version desc)` |
| kind | varchar(40) | `PersonFileKind` |
| version | int | not null; CV: max + 1 per person, renumbered on merge; DOCUMENT: 1; unique `(person_id, kind, version) where kind = 'CV'` |
| is_current | boolean | not null; partial unique `(person_id) where kind = 'CV' and is_current` |
| file_name | varchar(255) | not null; the original name (may contain the person's name: personal data) |
| content_type | varchar(100) | not null; detected by Tika |
| size_bytes | bigint | not null; `CHECK (size_bytes BETWEEN 1 AND 10485760)` |
| sha256 | char(64) | not null; unique `(person_id, sha256)` (same file twice gives 409 `DUPLICATE`) |
| storage_key | varchar(300) | not null, unique; `person/<person_id>/<id>`; the bytes are in the `FileStore`, never in the database |
| text_status | varchar(40) | `TextExtractionStatus`, default `PENDING`; index where `PENDING` (worker queue) |
| extracted_text | text | null; Tika output, at most 1,000,000 characters |
| text_tsv | tsvector | `generated always as (to_tsvector('tr_unaccent', coalesce(extracted_text, ''))) stored`; GIN index. Only the current CV is searched (`where kind = 'CV' and is_current`) |
| extracted_at | timestamptz | null |
| uploaded_by | uuid FK app_user | not null |
| audit columns | | |

**cv_intake_item** — not audited (a work queue; minimised on resolve and purged) (1.2.0)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| batch_id | uuid | not null; index; one id per `POST /cv-intake` |
| status | varchar(40) | `CvIntakeStatus`, default `PENDING`; index `(status, uploaded_at)` |
| file_name | varchar(255) | null after ATTACHED/DISCARDED |
| content_type, size_bytes, sha256 | | as in `person_file`; null after resolve |
| storage_key | varchar(300) | null after resolve (the bytes moved to `person_file` or were deleted) |
| text_status | varchar(40) | `TextExtractionStatus` |
| extracted_text | text | null after resolve |
| detected | jsonb | not null default `'{"emails":[],"phones":[],"linkedinUrls":[]}'`; reset to empty after resolve |
| source_channel | varchar(40) | null; `SourceChannel` proposed for the new person |
| attached_person_id | uuid FK person | null; `on delete cascade` |
| uploaded_by | uuid FK app_user | not null |
| uploaded_at | timestamptz | not null |
| expires_at | timestamptz | not null; `uploaded_at + 14 days`. A nightly job in `accounts` deletes expired PENDING rows and their bytes, and deletes ATTACHED/DISCARDED rows older than 30 days |

**Pending intake items and DSRs (1.2.0):** a PENDING item holds personal data of someone who may not be a `person` yet. `accounts` exposes `CvIntakeApi.findPendingByEmail(email)` (match on `detected->'emails'`, case-insensitive; GIN index on `detected`). A data-subject request runs it with the requester's e-mail: the ACCESS export lists the matching items (file name, upload date, detected values), and a DELETION request discards them (bytes, text and detected values removed) before the person steps below. If the e-mail is not in the CV text the item is not found, which is why pending items expire after 14 days.

**duplicate_candidate** — not audited (1.2.0)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| person_a_id, person_b_id | uuid FK person | not null; `on delete cascade`; `CHECK (person_a_id < person_b_id)`; unique `(person_a_id, person_b_id)`; index on each |
| reasons | varchar(40)[] | not null; `DuplicateMatchReason` values, at least one |
| status | varchar(40) | `DuplicateStatus`, default `OPEN`; index `(status, detected_at desc)` |
| detected_at | timestamptz | not null |
| resolved_at | timestamptz | null |
| resolved_by | uuid FK app_user | null |

Written on person create/update, CV attach and by a nightly scan in `accounts`. A DISMISSED pair is never re-opened.

**person_merge** — append-only, not audited (1.2.0): `id uuid PK`, `survivor_id uuid FK person on delete cascade` (index), `merged_id uuid not null` (no FK: that row was deleted; unique), `fields_taken varchar(40)[] not null`, `merged_at timestamptz not null`, `merged_by uuid FK app_user not null`. Holds ids only. The `_aud` purge uses it to reach the merged person's history: `privacy` reads the merged ids **before** step 2 (step 0 of the order below), because on DELETE the cascade removes these rows. The cascade stays (the FK rule, `PersonDeleteIT`).

**off_limits_rule** — audited (1.2.0)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| account_id | uuid FK account | null; index |
| company_name | varchar(200) | null; index on `immutable_unaccent(lower(trim(company_name)))`; `CHECK ((account_id IS NULL) <> (company_name IS NULL))` |
| reason | varchar(40) | `OffLimitsReason` |
| note | varchar(500) | null; no personal data |
| starts_on | date | not null default today |
| ends_on | date | null; `CHECK (ends_on IS NULL OR ends_on >= starts_on)` |
| include_subsidiaries | boolean | not null default false (1.3.0, R63); `CHECK (NOT include_subsidiaries OR account_id IS NOT NULL)`; the rule then also covers every organisation below `account_id` in the `parent_account_id` tree |
| audit columns | | partial unique on the target `where ends_on is null` (the service also rejects overlapping open rules) |

**Off-limits match (1.3.0, R63):** a person is covered by a rule when one of their current employments (`end_date is null`) has `organisation_id` = the rule's `account_id`, or a descendant of it (up to depth 5) when `include_subsidiaries`; otherwise, as in 1.2, when the normalised current company text (`person.company` or a current `person_employment.company` without `organisation_id`) equals `normalize_org_name` of the rule's `company_name`, or of the rule account's name or an alias.

**person_employment** — audited; `id uuid PK`, `person_id FK person on delete cascade`, `company varchar(200) not null`, `title varchar(200)`, `start_date date`, `end_date date`, `sort_order int not null`. Replaced as a whole on update. Since 1.3.0 (R63): `organisation_id uuid FK account null` (`no action`, accounts are never deleted; index). When set, the service copies the organisation's name into `company` (the free text stays the fallback and the display value as entered at that time) and sets `account.is_employer = true`. Import and the person form call `OrganisationApi.match(company)` and set `organisation_id` only on a single match. Backfill: none (null = free text only); a one-off admin job may link later.

**contact** — audited

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| account_id | uuid FK account | not null |
| person_id | uuid FK person | not null |
| role | varchar(200) | null |
| is_primary | boolean | default false; partial unique index `(account_id) where is_primary and ended_on is null` |
| ended_on | date | null; set by `DELETE .../contacts/{id}` (history is kept); `active = ended_on is null` |
| audit columns | | unique `(account_id, person_id) where ended_on is null`; an ended contact can be re-added as a new row |

**account_open_job** — projection, not audited. `job_order_id uuid PK` (no FK, pipeline owns the job order), `account_id uuid FK account not null` (index), `title varchar(200)`, `open boolean not null`, `updated_at timestamptz not null`. Fed by `JobOrderCreated` and `JobOrderStatusChanged` with an upsert by `job_order_id` (`open = status.isOpen()`), so redelivery is harmless. It feeds `open_job_count` and the `blockingItems` of the account-close guard.

**network_link** — audited

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| from_person_id | uuid FK person | not null; index |
| to_person_id | uuid FK person | not null; index; `CHECK (from_person_id <> to_person_id)` |
| type | varchar(40) | `LinkType` |
| note | text | null |
| audit columns | | unique `(from_person_id, to_person_id, type)`; for symmetric types the service stores `from < to` |

**import_job** — not audited

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| kind | varchar(40) | `ImportKind` |
| status | varchar(40) | `ImportStatus` |
| file_name | varchar(255) | |
| file_content | bytea | the uploaded file until commit or discard, then nulled |
| total_rows, valid_rows, duplicate_rows, imported_rows | int | |
| errors | jsonb | `[{row, column, message}]`, first 500 |
| failure_reason | text | null |
| completed_at | timestamptz | null |
| mapping | jsonb | null (1.3.0); `{header: templateColumn}` sent with the upload; header names only, no row data |
| rolled_back_at | timestamptz | null (1.3.0); set with status ROLLED_BACK |
| audit columns | | |

**import_job_record** — append-only, not audited (1.3.0, CO-014 rollback): `import_id uuid FK import_job on delete cascade`, `entity_type varchar(40)` (`EntityType`: ACCOUNT, PERSON or CONTACT), `entity_id uuid` (no FK, like `access_log`), PK `(import_id, entity_type, entity_id)`, index `(entity_id)`. Written in the commit transaction, one row per created record. Ids only, so no anonymisation step.

`PersonCreated.importId` tells privacy which import created a person; it keeps that in `person_privacy.import_id` so `POST /consent-requests/bulk` with `importId` can target them.

**saved_view** — not audited (a user setting, not a business record; ADR 0010)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| entity | varchar(40) | `ListEntity` |
| name | varchar(120) | not null; unique `(owner_id, entity, lower(name))` |
| visibility | varchar(40) | `ViewVisibility`, default `PRIVATE`; index `(entity, visibility)` |
| owner_id | uuid FK app_user | not null; index `(owner_id, entity)` |
| definition | jsonb | not null; `{filters, columns, sort, groupBy}` as `ViewDefinition` in openapi.yaml, validated by the service per entity |
| audit columns | | `version` gives the optimistic lock of `PATCH /views/{id}` |

Users are deactivated, never deleted, so `owner_id` stays valid. Filters hold ids, enum names and short search terms. SHARED views never hold `q` (free text that may name a person; removed by the service, ADR 0010 amendment 2); a PRIVATE view's `q` is the owner's own setting and is not anonymised (it is not a record about the person). Max 50 PRIVATE views per owner and entity (400 `VALIDATION_FAILED`, code `LIMIT`).
### pipeline

**job_order** — audited (except cached counters)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| account_id | uuid FK account | not null; index |
| title | varchar(200) | not null; trigram index |
| location | varchar(200) | null |
| seniority | varchar(100) | null |
| description | text | null |
| fee_terms | text | null |
| openings | int | not null default 1; `CHECK (openings >= 1)` |
| status | varchar(40) | `JobOrderStatus`, default `OPEN`; index |
| status_changed_at | timestamptz | not null |
| owner_id | uuid FK app_user | not null; index |
| opened_on | date | not null |
| closed_on | date | null; set on FILLED or CANCELLED |
| tags | varchar(50)[] | not null default `'{}'`; lower-case, at most 20 (`CHECK (cardinality(tags) <= 20)`); GIN index; written by `ADD_TAG`/`REMOVE_TAG` bulk actions (ADR 0010) |
| archived_at, archived_by, archive_reason, archive_note | | archive columns (1.3.0) |
| confidentiality_level | varchar(40) | `ConfidentialityLevel`, not null default `NORMAL` (1.3.0) |
| audit columns | | |

`stageCounts`, `activeCandidates` and `daysOpen` are computed by query (`group by stage`), not stored.

**search_brief** — audited (1.3.0, SX-001); one row per job order, created on the first `PUT .../brief`

| Column | Type | Notes |
| --- | --- | --- |
| job_order_id | uuid PK, FK job_order | |
| reporting_line | varchar(200) | null |
| purpose, ideal_profile, notes | text | null; at most 4000 each (service) |
| seniority_level | varchar(40) | null; `Seniority` |
| function_area | varchar(40) | null; `JobFunction` |
| comp_band_enc | bytea | null (R73, replaces the clear `comp_min`, `comp_max`, `comp_currency`, `comp_period`, `comp_bonus_note` of the first 1.3.0 draft); the `BriefCompensation` JSON `{min, max, currency, period, bonusNote}` encrypted by the JPA converter `security.FieldCipher` exactly like `person.compensation_enc` (same key versions). Audited as ciphertext; never indexed. The rules that were CHECKs move to the service: `min <= max`, currency required when min or max is set, `period` a `CompensationPeriod` (400 `VALIDATION_FAILED`) |
| team_size | smallint | null (R73); `CHECK (team_size BETWEEN 0 AND 100000)` |
| languages | jsonb | not null default `'[]'`; `[{language, level}]`, at most 10 |
| kickoff_on, longlist_due_on, shortlist_due_on, target_start_on | date | null |
| brief_version | int | not null default 1; +1 when criteria, band or timeline change |
| audit columns | | `version` is the optimistic lock of the PUT |

The band is the client's budget, but it reveals what the client pays (and, after a placement, close to what the person earns), so since the review round it is encrypted at rest like person pay (R73, ADR 0013). Who may read it is unchanged (everyone who may read the brief); no SQL filter uses it.

**search_criterion** — audited (1.3.0): `id uuid PK`, `job_order_id uuid FK job_order not null` (index `(job_order_id, kind, sort_order)`), `kind varchar(40)` (`CriterionKind`), `text varchar(300) not null`, `sort_order int not null`, `removed_at timestamptz null` (never hard-deleted: `candidate_evaluation.scores` refer to the id), audit columns.

**job_order_team_member** — audited (1.3.0): `job_order_id uuid FK job_order` + `user_id uuid FK app_user`, PK `(job_order_id, user_id)`, `role varchar(40)` (`SearchTeamRole`), `added_at timestamptz not null`. Replaced as a whole by `PUT .../team`; the owner is always a LEAD row (the service adds it). Index on `user_id` (later visibility rule).

**target_company** — audited (1.3.0, SX-003)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| job_order_id | uuid FK job_order | not null; index |
| account_id | uuid FK account | null; partial unique `(job_order_id, account_id) where account_id is not null` |
| company_name | varchar(200) | null; `CHECK ((account_id IS NULL) <> (company_name IS NULL))`; unique `(job_order_id, immutable_unaccent(lower(trim(company_name))))` where not null |
| sector | varchar(100) | null |
| tier | varchar(40) | `TargetTier` |
| status | varchar(40) | `TargetCompanyStatus`, default `TO_MAP` |
| rationale | varchar(1000) | null |
| source | varchar(200) | null |
| notes | varchar(2000) | null; no personal data (hint in the UI) |
| owner_id | uuid FK app_user | not null |
| off_limits_override_reason | varchar(500) | null; set by a manager approaching an off-limits company |
| audit columns | | at most 300 rows per job order (service) |

**application** — audited

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| job_order_id | uuid FK job_order | not null |
| person_id | uuid FK person | not null; index |
| stage | varchar(40) | `PipelineStage`, default `SOURCED` |
| rejection_reason | text | not null when stage = REJECTED (`CHECK`) |
| stage_changed_at | timestamptz | not null |
| off_limits_override_reason | text | null (1.2.0); set when a manager added the person despite an off-limits rule; scrubbed like `rejection_reason` |
| rank | int | null (1.3.0); `CHECK (rank >= 1)`; position on the list (`PipelineStage.candidateList()`), gapless per `(job_order_id, list)`; null when REJECTED or PLACED. No unique index (reorders would collide mid-statement): the service locks the `job_order` row (`PESSIMISTIC_WRITE`) for every rank change. Index `(job_order_id, rank)` |
| rating | smallint | null (1.3.0); `CHECK (rating BETWEEN 1 AND 5)` |
| rationale | text | null (1.3.0); at most 2000 characters (service); personal data, scrubbed like `rejection_reason` |
| outreach_status | varchar(40) | `OutreachStatus`, not null default `TO_CALL` (1.3.0); set to `SCREENED` by the service on a move to stage SCREENED |
| close_reason | varchar(40) | null (1.3.0, R65); `CloseReason`; `CHECK ((stage = 'REJECTED') = (close_reason IS NOT NULL))`; index `(job_order_id, close_reason)` (funnel report). Set with every move to REJECTED (`OTHER` when the request has none; `CLIENT_DECLINED` from client feedback), cleared with `rejection_reason` on reinstatement. Backfill: `OTHER` for every REJECTED row, before the CHECK is added. Not personal data (a code) |
| audit columns | | unique `(job_order_id, person_id)`; index `(job_order_id, stage)` |

Rank backfill (1.3.0 migration): for each job order and list, number the active applications by `stage_changed_at, created_at` (1..n); REJECTED and PLACED stay null.

**candidate_exposure** — append-only, not audited (1.2.0, ADR 0011; B-05 exposure history)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| person_id | uuid FK person | not null; `on delete cascade`; index `(person_id, presented_at desc)` |
| account_id | uuid FK account | not null; index `(account_id, person_id)` (re-presenting warning) |
| job_order_id | uuid FK job_order | not null |
| application_id | uuid FK application | null; `on delete set null` (not a person FK) |
| channel | varchar(40) | `ExposureChannel` |
| presented_at | timestamptz | not null |
| presented_by | uuid FK app_user | null for system |
| | | unique `(application_id, channel) where channel = 'PIPELINE'` (first move into a client-facing stage only; idempotent) |

Written by `pipeline` in the transaction of the first move into a client-facing stage (PRESENTED, CLIENT_INTERVIEW, OFFER or PLACED, `PipelineStage.sharedWithClient()`; forward moves may skip PRESENTED). Holds ids and dates only, so it is kept on anonymisation and cascades on delete. Backfill migration: one PIPELINE row per application whose current stage is PRESENTED, CLIENT_INTERVIEW, OFFER or PLACED, or that reached PRESENTED in `application_aud` (`presented_at` = the earliest such revision time, else `stage_changed_at`).

**client_feedback** — audited (1.3.0, SX-019)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| application_id | uuid FK application | not null; `on delete cascade`; index `(application_id, round desc)` |
| person_id | uuid FK person | not null; `on delete cascade` (ADR 0009 rule); copy of the application's person, for the purge match |
| round | smallint | not null; `CHECK (round BETWEEN 1 AND 20)`; unique `(application_id, round)` |
| feedback_on | date | not null |
| participants | varchar(500) | null; client contacts' names (business contacts) |
| outcome | varchar(40) | `ClientFeedbackOutcome` |
| decline_reason | varchar(40) | null; `DeclineReason`; `CHECK ((outcome = 'DECLINE') = (decline_reason IS NOT NULL))` |
| reason_text | varchar(1000) | null; personal data (scrubbed) |
| candidate_feedback | varchar(2000) | null; personal data (scrubbed) |
| recorded_by | uuid FK app_user | not null |
| recorded_at | timestamptz | not null |
| audit columns | | |

**candidate_evaluation** — audited (1.3.0, SX-008/017)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| person_id | uuid FK person | not null; `on delete cascade`; index `(person_id, evaluated_on desc)` |
| job_order_id | uuid FK job_order | null; partial unique `(person_id, job_order_id) where job_order_id is not null` (one per search) |
| application_id | uuid FK application | null; `on delete set null` (not a person FK) |
| evaluator_id | uuid FK app_user | not null |
| evaluated_on | date | not null |
| interview_type | varchar(40) | `InterviewType` |
| overall_rating | smallint | null; `CHECK (overall_rating BETWEEN 1 AND 5)` |
| recommendation | varchar(40) | `Recommendation` |
| recommendation_reason | varchar(500) | null; required for REJECT (service) |
| summary | varchar(600) | null |
| key_achievements | jsonb | not null default `'[]'`; array of at most 5 strings |
| motivation | varchar(400) | null |
| motivation_tags | varchar(40)[] | not null default `'{}'`; `MotivationTag` values (array CHECK `motivation_tags <@ ARRAY[...]`) |
| concerns | varchar(400) | null |
| other_processes | varchar(1000) | null |
| scores | jsonb | not null default `'[]'`; `[{criterionId, score 1-5, comment}]`, at most 30; criterion ids of `search_criterion` of the same job order (service check; criteria are never hard-deleted, so ids stay valid) |
| private_notes | text | null; at most 4000; never in reports |
| internal_only | boolean | not null default false |
| report_blocks | varchar(40)[] | not null default `'{SUMMARY,ACHIEVEMENTS,MOTIVATION,CONCERNS}'`; `EvaluationBlock` values (array CHECK) |
| audit columns | | |

**interview_note** — audited (1.3.0, SX-009): `id uuid PK`, `person_id uuid FK person not null on delete cascade` (index `(person_id, held_on desc)`), `job_order_id uuid FK job_order null`, `application_id uuid FK application null on delete set null`, `interviewer_id uuid FK app_user not null`, `held_on date not null`, `interview_type varchar(40)` (`InterviewType`), `body text not null` (at most 20,000), `include_in_report boolean not null default false`, audit columns.

**candidate_reference** — audited (1.3.0, SX-010)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| person_id | uuid FK person | not null; `on delete cascade`; index |
| job_order_id | uuid FK job_order | null |
| referee_name | varchar(200) | not null; a third person's data |
| referee_relationship, referee_company | varchar(200) | null |
| referee_email | varchar(254) | null |
| referee_phone | varchar(50) | null |
| status | varchar(40) | `ReferenceStatus`, default `TO_REQUEST` |
| summary | text | null; at most 4000 |
| checked_on | date | null |
| checked_by | uuid FK app_user | null |
| include_in_report | boolean | not null default false |
| audit columns | | |

**client_report** — not audited; immutable after insert except `sent_on`, `sent_by` (1.3.0, SX-021/022, PB-021)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| job_order_id | uuid FK job_order | not null; unique `(job_order_id, kind, version)` |
| kind | varchar(40) | `ReportKind` |
| version | int | not null; max + 1 per job order and kind |
| language | varchar(2) | `Language` |
| blind | boolean | not null |
| person_ids | uuid[] | not null default `'{}'`; included people (no FK, like `access_log`); GIN index (erasure lookup) |
| storage_key | varchar(300) | not null, unique; `report/<job_order_id>/<id>.pdf` in the `FileStore` |
| size_bytes | bigint | not null |
| generated_by | uuid FK app_user | not null |
| generated_at | timestamptz | not null |
| sent_on | date | null |
| sent_by | uuid FK app_user | null |

The PDF holds personal data of every included person: anonymisation and deletion remove every report whose `person_ids` overlaps the person-id set (row and bytes; see "Anonymisation and deletion" step 3).

**agreement** — audited (1.3.0, PB-007/008, ADR 0013)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| account_id | uuid FK account | not null; index |
| job_order_id | uuid FK job_order | null (framework terms) or the search (engagement); index |
| kind | varchar(40) | `AgreementKind`; `CHECK ((kind = 'ENGAGEMENT_LETTER') = (job_order_id IS NOT NULL) OR kind = 'AMENDMENT')` |
| status | varchar(40) | `AgreementStatus`, default `DRAFT`; `CHECK (status <> 'SIGNED' OR signed_on IS NOT NULL)` |
| title | varchar(200) | null |
| signed_on, valid_from, valid_until | date | null; `CHECK (valid_until IS NULL OR valid_from IS NULL OR valid_until >= valid_from)` |
| language | varchar(2) | null; `Language` |
| governing_law | varchar(100) | null |
| exclusivity | varchar(40) | null; `Exclusivity` |
| fee_model | varchar(40) | `FeeModel` |
| fee_percent | numeric(5,2) | null; `CHECK (fee_percent BETWEEN 0 AND 100)` |
| fee_basis | varchar(40)[] | not null default `'{}'`; `FeeBasisComponent` values (array CHECK) |
| flat_fee_amount, estimated_fee, fee_minimum, fee_cap | numeric(14,2) | null; `CHECK (fee_minimum IS NULL OR fee_cap IS NULL OR fee_minimum <= fee_cap)` |
| currency | char(3) | not null |
| payment_terms_days | smallint | not null default 30; 0..180 |
| guarantee_months | smallint | not null default 6; `CHECK (guarantee_months BETWEEN 0 AND 36)` |
| guarantee_start_basis | varchar(40) | `GuaranteeStartBasis`, not null default `START_DATE` (R67) |
| signed_file_id | uuid | null (R67); id of the signed PDF in the `FileStore`, key `agreement/<agreement_id>/<signed_file_id>` (no FK: there is no file table for agreements) |
| signed_file_name | varchar(255) | null; `CHECK ((signed_file_id IS NULL) = (signed_file_name IS NULL))` |
| signed_file_size | bigint | null; `CHECK (signed_file_size IS NULL OR signed_file_size BETWEEN 1 AND 20971520)` |
| signed_file_uploaded_at, signed_file_uploaded_by | timestamptz, uuid FK app_user | null; set with `signed_file_id` |
| guarantee_terms, confidentiality_terms | varchar(2000) | null |
| expense_policy | varchar(1000) | null |
| off_limits_months | smallint | null; 0..60 |
| signatory_contact_id | uuid FK contact | null; `on delete set null` (a contact cascades with its person, so `no action` would block person deletion; review 1.3 B1); index |
| billing_account_id | uuid FK account | not null (defaults to `account_id`); the billing entity |
| notes | text | null; no personal data |
| audit columns | | |

**fee_schedule_item** — audited (1.3.0, PB-009, CO-020)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| agreement_id | uuid FK agreement | not null; `on delete cascade` (only DRAFT agreements are deleted); unique `(agreement_id, seq)` |
| seq | smallint | not null; 1..n in schedule order |
| label | varchar(200) | not null |
| trigger | varchar(40) | `FeeTrigger` |
| offset_days | smallint | null; `CHECK (trigger <> 'DATE_OFFSET' OR offset_days IS NOT NULL)` |
| percent | numeric(5,2) | null |
| amount | numeric(14,2) | null; `CHECK ((percent IS NULL) <> (amount IS NULL))` |
| due_days | smallint | not null default 30 |
| vat_applies | boolean | not null default true (R67); the service defaults it to `not account.vat_exempt` of the billing account. Amounts are net |
| invoice_state | varchar(40) | `InvoiceState`, default `NOT_DUE`; index `(invoice_state, due_on)` |
| triggered_at | timestamptz | null; set on NOT_DUE → DUE (idempotent: a trigger never fires twice) |
| due_on | date | null; `triggered_at::date + due_days` |
| einvoice_ref | varchar(100) | null |
| invoiced_on, paid_on | date | null |
| invoiced_amount | numeric(14,2) | null |
| state_reason | varchar(500) | null; no personal data |
| audit columns | | |

Triggers fire in `pipeline`, in the transaction of the cause (agreement SIGNED, first client-facing stage move, placement created, job order CANCELLED) or in a nightly job (DATE_OFFSET, START_DATE). Only items of agreements with a `job_order_id` fire; framework items are templates copied into a new engagement by the UI.

**placement** — audited except `compensation_enc` is audited as ciphertext (1.3.0, CO-021, PB-011)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| application_id | uuid FK application | null; partial unique `where application_id is not null`; `on delete set null` (the application cascades with the person; the placement is commercial evidence and stays) |
| job_order_id | uuid FK job_order | not null; index |
| account_id | uuid FK account | not null; index |
| person_id | uuid FK person | null; `on delete set null` (exception to the ADR 0009 cascade rule, like `data_subject_request`: fee and guarantee records outlive the person); index. Always set on insert (service) |
| agreement_id | uuid FK agreement | null |
| status | varchar(40) | `PlacementStatus`, default `ACTIVE` |
| offer_accepted_on | date | null |
| start_date | date | not null |
| compensation_enc | bytea | null; `PlacementCompensation` JSON encrypted by `security.FieldCipher` like `person.compensation_enc`; readable by MANAGER/ADMIN and the job order's owner and team |
| fee_amount | numeric(14,2) | not null |
| fee_currency | char(3) | not null |
| fee_overridden | boolean | not null default false |
| guarantee_end_on | date | not null; index (30-day alert) |
| left_on | date | null |
| replacement_job_order_id | uuid FK job_order | null |
| status_reason | varchar(500) | null; personal data (scrubbed) |
| audit columns | | |

Nightly job in `pipeline`: a next action "Guarantee ends on <date>" for the job order owner 30 days before `guarantee_end_on` (`source_event_id` = UUID v5 of `placement.id + ':guarantee-30'`, so it is created once), status GUARANTEE_ENDED after the end date, START_DATE items DUE. Retention checks at 6 and 12 months are the same mechanism (`:retention-6`, `:retention-12`).

**conflict_check** — append-only, not audited (1.3.0, PB-018/019; it is itself the log)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| account_id | uuid FK account | null; index `(account_id, checked_at desc)` |
| company_name | varchar(200) | null; `CHECK ((account_id IS NULL) <> (company_name IS NULL))` |
| job_order_id | uuid FK job_order | null |
| hits | jsonb | not null default `'[]'`; `[{kind, refType, refId, title, until}]`; `title` is null for PLACED_CANDIDATE, so no person data is stored |
| decision | varchar(40) | `ConflictDecision` |
| override_reason | varchar(500) | null; required for PROCEED with hits (service); no personal data (UI hint) |
| checked_by | uuid FK app_user | not null |
| checked_at | timestamptz | not null |

**proposal** — audited

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| account_id | uuid FK account | not null; index |
| title | varchar(200) | not null |
| terms | text | null |
| fee_amount | numeric(14,2) | null |
| currency | char(3) | null, ISO 4217 |
| status | varchar(40) | `ProposalStatus`, default `DRAFT` |
| status_changed_at | timestamptz | not null |
| status_reason | text | null; reason of the last move |
| sent_on | date | null |
| decided_on | date | null |
| owner_id | uuid FK app_user | not null |
| tags | varchar(50)[] | not null default `'{}'`; lower-case, at most 20 (`CHECK (cardinality(tags) <= 20)`); GIN index; written by `ADD_TAG`/`REMOVE_TAG` bulk actions (ADR 0010) |
| audit columns | | |

**activity** — audited

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| type | varchar(40) | `ActivityType` |
| note | text | not null |
| occurred_at | timestamptz | not null |
| account_id | uuid FK account | null; index `(account_id, occurred_at desc)` |
| person_id | uuid FK person | null; `on delete cascade` (ADR 0009); index `(person_id, occurred_at desc)` |
| job_order_id | uuid FK job_order | null |
| user_id | uuid FK app_user | not null |
| outcome | varchar(40) | null (1.3.0, R70); `ActivityOutcome` |
| direction | varchar(40) | null (1.3.0); `ActivityDirection`; `CHECK (type <> 'NOTE' OR direction IS NULL)` |
| duration_min | smallint | null (1.3.0); `CHECK (duration_min BETWEEN 0 AND 1440)` |
| source | varchar(40) | `ActivitySource`, not null default `MANUAL` (1.3.0) |
| external_message_id | varchar(300) | null (1.3.0); RFC 5322 `Message-ID` of a captured e-mail; **unique partial index `where external_message_id is not null`** (capture is idempotent); `CHECK (external_message_id IS NULL OR source = 'EMAIL')`. Not personal data on its own (an opaque id), but it is nulled with `note` on anonymisation because some providers embed the address |
| audit columns | | `CHECK (account_id IS NOT NULL OR person_id IS NOT NULL)` |

**activity_attendee** — audited (1.3.0, R70): `activity_id uuid FK activity on delete cascade`, `person_id uuid FK person on delete cascade`, PK `(activity_id, person_id)`, index `(person_id)`. Other people present at a call or meeting (client contacts, co-interviewers); at most 20 per activity (service); never the activity's own `person_id`. Replaced as a whole by create/update. An attendee counts as contacted for `last_contact_at` like `person_id` (except NOTE). On the attendee's anonymisation the row is deleted (the activity and its note stay: the note belongs to the main person or account; residual risk listed in `personal-data.md`).

**next_action** — audited

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| title | varchar(300) | not null |
| due_on | date | not null |
| owner_id | uuid FK app_user | not null; index `(owner_id, done, due_on)` |
| account_id, person_id, job_order_id | uuid FK | all null allowed; `person_id` `on delete cascade` (ADR 0009) |
| done | boolean | default false |
| done_at | timestamptz | null |
| source_event_id | uuid | null; unique; set for actions created from `RetentionFlagged` (idempotency) |
| audit columns | | |

**timeline_entry** — append-only, not audited (A-08; Envers is the authoritative history, this is the read model)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| account_id | uuid FK account | not null; index `(account_id, occurred_at desc)` |
| occurred_at | timestamptz | from the event |
| kind | varchar(40) | `TimelineKind` |
| entity_type | varchar(40) | `EntityType` |
| entity_id | uuid | |
| from_status, to_status | varchar(40) | null |
| reason | text | null |
| summary | varchar(500) | English summary for logs |
| person_id | uuid | null (candidate moves, contacts) |
| job_order_id | uuid | null |
| actor_id | uuid FK app_user | null for system |
| source_event_id | uuid | unique (listener idempotency) |

**bulk_action** — append-only, not audited (audit evidence of one mass action, ADR 0010)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | returned as `BulkActionResult.id` |
| entity | varchar(40) | `ListEntity` |
| action | varchar(40) | `BulkActionType` |
| params | jsonb | the typed params **without free text** (1.1.1): owner id, status, tag, next-action due date and owner, language. `title` (SET_NEXT_ACTION) and `reason` (CHANGE_STATUS) are removed before insert; they live only in the changed records, which the anonymisation scrub covers. `CHECK (NOT (params ?\| array['title','reason']))` |
| actor_id | uuid FK app_user | not null; index `(actor_id, started_at desc)` |
| started_at, finished_at | timestamptz | not null |
| requested_count, ok_count, skipped_count, failed_count | int | not null |

**bulk_action_item** — append-only, not audited; one row per requested id, whatever the outcome

| Column | Type | Notes |
| --- | --- | --- |
| bulk_action_id | uuid FK bulk_action | `on delete cascade`; PK `(bulk_action_id, entity_id)` |
| entity_id | uuid | no FK (the record may be deleted or anonymised later, like `access_log`); index `(entity_id)` |
| outcome | varchar(40) | `BulkOutcome` |
| code | varchar(40) | `BulkResultCode`; null when OK, not null otherwise (`CHECK`) |

No `message` column (dropped in 1.1.1): the response message is `BulkResultCode.message()`, a fixed text per code, so nothing free-form is stored. With no free text and no names, `bulk_action` and `bulk_action_item` hold only ids and codes and need no anonymisation step; `entity_id` of an erased person points at nothing, like `access_log`.

Each OK record also gets its normal Envers revision, domain event and timeline entry, because the handlers call the same services as the single endpoints. A failed record rolls back only its own transaction; its `bulk_action_item` row is written in a separate transaction so the evidence survives.
### privacy

**person_privacy** — audited; one row per person, created on `PersonCreated`

| Column | Type | Notes |
| --- | --- | --- |
| person_id | uuid PK, FK person | |
| consent_status | varchar(40) | `ConsentStatus` — the master copy |
| notice_sent_at | timestamptz | null; first information notice (NFR-01) |
| basis_date | date | start of the retention period |
| retain_until | date | `basis_date + retention_months`; index |
| retention_status | varchar(40) | `RetentionStatus` |
| flagged_at | timestamptz | null |
| last_consent_at | timestamptz | null (1.2.0); `recorded_at` of the latest GIVEN record; the basis with `FROM_LAST_CONSENT` |
| import_id | uuid | null; import that created the person |
| version | bigint | |

**privacy_notice** — immutable after insert

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| version | varchar(40) | unique `(version, language)` |
| language | varchar(2) | `Language` |
| notice_text | text | aydınlatma metni |
| consent_text | text | açık rıza beyanı |
| effective_from | date | the current notice per language = latest `effective_from <= today` |
| created_at, created_by | | |

**consent_record** — append-only (NFR-02: text version, timestamp, channel, withdrawal)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| person_id | uuid FK person | index `(person_id, recorded_at desc)` |
| decision | varchar(40) | `ConsentDecision` |
| channel | varchar(40) | `ConsentChannel` |
| notice_id | uuid FK privacy_notice | not null |
| consent_request_id | uuid FK consent_request | null; set for EMAIL_LINK |
| recorded_at | timestamptz | |
| recorded_by | uuid FK app_user | null when the person answered the link |
| evidence | text | null |
| ip_address | varchar(45) | null; EMAIL_LINK only |

A withdrawal is a new record with decision `WITHDRAWN`; its `recorded_at` is the withdrawal date.

**consent_request** — one e-mail with a one-time link

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| person_id | uuid FK person | index |
| notice_id | uuid FK privacy_notice | |
| language | varchar(2) | |
| token_hash | char(64) | SHA-256 of the link token; unique. The token itself is never stored. |
| delivery_status | varchar(40) | `EmailDeliveryStatus`; index |
| queued_at, sent_at | timestamptz | |
| failure_reason | text | null |
| expires_at | timestamptz | queued_at + 30 days |
| responded_at | timestamptz | null |
| response | varchar(40) | `ConsentDecision`, null |
| requested_by | uuid FK app_user | null when automatic |
| purpose | varchar(40) | `ConsentRequestPurpose`, not null default `INITIAL` (1.2.0) |
| opened_at | timestamptz | null (1.2.0); first successful `GET /public/consent/{token}` |
| reminder_days_before | smallint | null (1.2.0); set on automatic reminders only (RENEWAL, or INITIAL resent while SENT) |
| reminder_for_retain_until | date | null (1.2.0); the `retain_until` the reminder was for; unique `(person_id, reminder_for_retain_until, reminder_days_before) where reminder_days_before is not null` (one reminder per step and period; nightly job idempotent) |

**retention_settings** — single row (`id smallint PK CHECK (id = 1)`), audited: `retention_months int default 24`, `basis varchar(40) default 'FROM_LAST_CONSENT'` (1.2.0; was `FROM_CREATED`; ASSUMPTION until the client answers; the migration updates the existing row), `action varchar(40) default 'ANONYMISE'`, `warning_days int default 30`, `auto_send_notice boolean default true`, `auto_send_person_types varchar(200) default 'CANDIDATE,EXECUTIVE'`, `renew_on_consent boolean default true`, `auto_send_on_cv_upload boolean not null default true` (1.2.0), `renewal_reminder_days smallint[] not null default '{60,14}'` (1.2.0; each 1..365, at most 5), `version`.

**Basis switch migration (1.2.0, db-engineer + security-privacy):** switching the default to `FROM_LAST_CONSENT` changes `basis_date` and `retain_until` of existing people. The migration (after `last_consent_at` is added) 1) backfills `person_privacy.last_consent_at` from the latest `consent_record` with decision GIVEN, 2) sets `retention_settings.basis = 'FROM_LAST_CONSENT'` only when the row still has the old default `FROM_CREATED`, 3) recomputes for every row not ANONYMISED: `basis_date = coalesce(last_consent_at::date, person.created_at::date)`, `retain_until = basis_date + retention_months`, and sets FLAGGED rows whose new `retain_until - warning_days > today` back to ACTIVE (the person copy follows through `RetentionRenewed`, published by a one-off privacy job, not by SQL). It never anonymises: the next nightly run does that with the normal rules. Test (`RetentionBasisMigrationIT`): people with no consent, an old GIVEN, a recent GIVEN, and a FLAGGED one get the expected `retain_until` and status; a custom basis set by an admin is kept.

**retention_run** — `id uuid PK`, `started_at`, `finished_at`, `triggered_by uuid FK app_user null`, `dry_run boolean`, `flagged_count`, `anonymised_count`, `deleted_count int`, `failure_reason text`.

**data_subject_request** — audited

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| person_id | uuid FK person | null until identified; `on delete set null` (the only FK to person that is not cascade, ADR 0009) |
| requester_name | varchar(200) | |
| requester_email | varchar(254) | |
| type | varchar(40) | `DataSubjectRequestType` |
| status | varchar(40) | `DataSubjectRequestStatus`; index |
| description | text | null |
| received_at | timestamptz | |
| due_on | date | `received_at + 30 days`; index |
| resolved_at | timestamptz | null |
| resolution | text | null |
| handled_by | uuid FK app_user | null |
| audit columns | | |

## Anonymisation and deletion (S-03, S-04, ADR 0009)

**Foreign-key rule:** every FK to `person` is `on delete cascade` (`person_employment`, `contact`, `network_link` both ends, `application`, `activity`, `next_action`, `person_privacy`, `consent_request`, `consent_record`; since 1.2.0 `person_education`, `person_file`, `cv_intake_item.attached_person_id`, `duplicate_candidate` both ends, `person_merge.survivor_id`, `candidate_exposure`; since 1.3.0 `client_feedback`, `candidate_evaluation`, `interview_note`, `candidate_reference`, `activity_attendee`), except `data_subject_request.person_id` and, since 1.3.0, `placement.person_id` (both `on delete set null`). **Indirect FKs** (rows that reach `person` through a table that cascades: `contact`, `application`, `activity`) are `on delete cascade` when the row is the person's data (`client_feedback`, `activity_attendee`) and `on delete set null` otherwise (`candidate_exposure.application_id`, `candidate_evaluation.application_id`, `interview_note.application_id`, `placement.application_id`, `agreement.signatory_contact_id`); never `no action`. FKs to `account` stay `no action`: accounts are never hard-deleted. `PersonDeleteIT` enforces all three rules: it walks `pg_constraint` from `person` transitively (person → contact/application/activity → …) and fails on any `no action`/`restrict` FK on the way, and it deletes a person who is an agreement signatory, has a placement and is a meeting attendee, then asserts the agreement and placement rows remain with null references.

**Classification:** every person-linked column is classified for erasure, audit and blind-report redaction in `contracts/personal-data.md`; `PersonalDataClassificationIT` fails on an unclassified column (plan rule, review 1.3).

**Third-party data (1.3.0, review):** a completed DELETION request carrying the requester's e-mail also runs, before step 1 and whether or not the requester is a `person`: every `ThirdPartyErasure` bean (SPI in `contracts/spi`) runs `eraseEmail(email)`: `accounts` removes the address (lower case) from `account.billing_emails` and from every `account_aud` row (`array_remove`); `pipeline` sets `referee_name = '[removed]'`, nulls `referee_relationship, referee_company, referee_email, referee_phone` of every `candidate_reference` with `lower(referee_email) = lower(:email)` (through JPA) and purges those references' `_aud` rows like step 4 ANONYMISE. Index: `candidate_reference (lower(referee_email))`. ACCESS requests list the same matches.

**Order (one retention or DSR action, run by `privacy`):**

0. **Collect the person-id set `:p`** (1.2.0): the person id plus every `person_merge.merged_id` with `survivor_id` = the person, and the `storage_key`s of the person's files. This happens before anything is deleted, because on DELETE the cascade removes `person_merge` and `person_file` rows. Steps 2 to 4 use this set.
1. **Minimise DSRs** (via JPA, so Envers records it) for every `data_subject_request` with that `person_id`: `requester_name = 'Anonymised person'`, `requester_email = 'hmac:v<k>:' || hex(HMAC-SHA256(PRIVACY_HMAC_KEY, lower(trim(email))))`. The key comes from an env var outside the database; `<k>` is `PRIVACY_HMAC_KEY_VERSION`. Old keys are kept read-only for matching and values are never re-hashed (ADR 0009)., `description = '[removed]'`. Keep the rest as evidence; `resolution` must not hold personal data. DSRs are never purged in Phase 1.
2. **ANONYMISE:** `PersonDataApi.anonymise(personId)` in `accounts` sets `person.name = 'Anonymised person'`, nulls `email, phone, linkedin_url, how_we_know, notes, title, city, company`, sets `tags = '{}'`, deletes `person_employment` and `network_link` rows, sets `anonymised_at` and `retention_status = ANONYMISED`, then publishes `PersonAnonymised(deleted=false)`.
   Since 1.2.0 it also nulls `notice_period_days, mobility, compensation_enc, phone_normalized, linkedin_url_normalized`, sets `languages = '[]'`, deletes `person_education`, `person_file` (the bytes are deleted from the `FileStore` after the commit), `duplicate_candidate` rows of the person, and minimises `cv_intake_item` rows attached to the person (`file_name, extracted_text, storage_key` null). `source_channel` stays (not personal data). `person_merge` rows stay (ids only).
   Since 1.3.0 it also sets `skills = '{}'`, nulls `function_area, seniority, archive_note` (archive date and reason stay), and the archive columns are otherwise kept.
   **DELETE** (`RetentionAction.DELETE`, or a completed DELETION request): `PersonDataApi.delete(personId)` removes the person **through JPA** (`PersonRepository.delete`; never a native `DELETE`). Envers then writes the id-only `person_aud` row with `revtype = 2` (`org.hibernate.envers.store_data_at_delete` stays false). The FKs cascade the rest and set the DSR's `person_id` to null. It then publishes `PersonAnonymised(deleted=true)`. Since 1.2.0 it first collects the `storage_key`s of the person's files and deletes those bytes after the commit (the cascade removes only the rows).
3. **Listeners on `PersonAnonymised`:**
   - `pipeline` sets `timeline_entry.reason` and `summary` to `[removed]` for every entry with that `person_id`. Timeline text is free text written at event time, so it is scrubbed, not re-rendered; the entries and their ids stay. When `deleted=false`, it also replaces `activity.note`, `application.rejection_reason`, `application.off_limits_override_reason` (1.2.0) and `next_action.title` for the person with `[removed]`. `candidate_exposure` rows stay (ids and dates only).
     Since 1.3.0, when `deleted=false`, `pipeline` also: nulls `activity.external_message_id` of the activities whose `note` it scrubs, deletes (through JPA) the person's `activity_attendee` rows; replaces `application.rationale` with `[removed]` and nulls `rating` (`close_reason` stays: a code); **deletes** (through JPA) the person's `candidate_evaluation`, `interview_note` and `candidate_reference` rows (assessments and a referee's data have no use once the person is anonymous); in `client_feedback` keeps `round, feedback_on, outcome, decline_reason` (KPIs) and sets `participants, reason_text, candidate_feedback` to `[removed]`; in `placement` keeps dates, status and fee (commercial evidence, VWC on tax retention) and nulls `compensation_enc` and `status_reason`; when `deleted=true` (the FK has set `placement.person_id` and `application_id` to null) it nulls `compensation_enc` and `status_reason` of every placement `where person_id is null and (compensation_enc is not null or status_reason is not null)` (person_id is always set on insert, so null means erased; idempotent); and in both cases (`deleted` true or false) deletes every `client_report` whose `person_ids` contains the person (row, then bytes after commit). The person merge (pipeline step) rewrites `client_report.person_ids` with `array_replace(merged, survivor)` and moves the 1.3.0 person rows like activities, so the event's `personId` is enough.
   - `privacy` keeps `consent_record` rows (legal evidence; they cascade away on DELETE), nulls `ip_address` and `evidence`, and marks `person_privacy` ANONYMISED.
4. **Envers `_aud` purge**, by `privacy`, after step 3. It is idempotent and runs in its own transaction. It covers these tables and match conditions (`:p` is the set collected in step 0: the person id **plus every merged id**, since 1.2.0: a merged record's history belongs to the survivor; never re-read `person_merge` here, it may already be gone):

| `_aud` table | Match |
| --- | --- |
| `person_aud` | `id IN :p` |
| `person_employment_aud`, `contact_aud`, `activity_aud`, `application_aud`, `next_action_aud`, `data_subject_request_aud`, `person_education_aud` (1.2.0), `person_file_aud` (1.2.0) | `person_id IN :p` |
| `network_link_aud` | `from_person_id IN :p OR to_person_id IN :p` |
| `person_privacy_aud` | `person_id IN :p` |
| `client_feedback_aud`, `candidate_evaluation_aud`, `interview_note_aud`, `candidate_reference_aud`, `placement_aud`, `activity_attendee_aud` (1.3.0) | `person_id IN :p` |

   For a merged id, every matching row is deleted (that person no longer exists), except its `person_aud` row with `revtype = 2`. `person_file_aud` holds `file_name` (personal), so it follows the same rules; `off_limits_rule_aud` holds no person data.

   - ANONYMISE: `DELETE` the matching rows `WHERE revend IS NOT NULL`. In the matching rows `WHERE revend IS NULL`, set the personal columns to the same placeholders as the live row (`[removed]` or null; DSR values from step 1). In `person_aud` this includes `tags = '{}'` (AuditPurger), matching step 2 (`PersonDataService.anonymise`), and since 1.2.0 explicitly `compensation_enc = null, notice_period_days = null, mobility = null, languages = '[]', phone_normalized = null, linkedin_url_normalized = null` (`source_channel` stays). Any later personal column on `person` must be added to this list. The current revision must stay, because `ValidityAuditStrategy` updates its `revend` on the next change.
   - DELETE: `DELETE` all matching rows except two (three since 1.3.0: the current `placement_aud` row of each surviving placement stays with `person_id, application_id, compensation_enc, status_reason` set to null, because the live row outlives the person). Keep the `person_aud` row with `revtype = 2` (id only; the NFR-03 deletion record). Keep the current `data_subject_request_aud` row (`revend IS NULL`, already minimised), and set its `person_id` to null, because the database's SET NULL is not audited.
   - Imports: set `import_job.file_content` and `errors` to null for every import that has ended (`status IN ('COMPLETED','FAILED','DISCARDED')`). A committed CSV would otherwise still hold the person's data.
   - Since 1.3.0, ANONYMISE placeholders in the current revision: `person_aud` also `skills = '{}'`, `function_area = null`, `seniority = null`, `archive_note = null`; `application_aud` `rationale = '[removed]'`, `rating = null`; `activity_aud` `external_message_id = null` (with the `note` placeholder); `client_feedback_aud` `participants, reason_text, candidate_feedback = '[removed]'`; `placement_aud` `compensation_enc = null`, `status_reason = null`. The current revisions of `candidate_evaluation_aud`, `interview_note_aud` and `candidate_reference_aud` are DEL rows after the listener's delete, so all their rows are deleted.
   - `account_aud`, `job_order_aud`, `proposal_aud`, `app_user_aud` and `retention_settings_aud` hold no person data; since 1.3.0 neither do `tag_definition_aud`, `search_brief_aud`, `search_criterion_aud`, `job_order_team_member_aud`, `target_company_aud` (company-level notes, no-personal-data hint in the UI), `agreement_aud` and `fee_schedule_item_aud`. Not audited and person-free: `account_merge`, `conflict_check` (no names in `hits`), `import_job_record`, `export_job` (no `q`/`cv`). `client_report` is handled by the listener above. Any new audited table that holds person data must be added to this table. Otherwise the history keeps the personal data.

**File retention (1.2.0):** person files live as long as the person (no separate clock): they go with anonymisation or deletion (step 2). Pending CV intake items are deleted with their bytes 14 days after upload. A weekly sweep in `accounts` deletes `FileStore` objects under `person/` and `intake/` that no row references (bytes written for a transaction that rolled back). Since 1.3.0 `pipeline` runs the same sweep for `agreement/` (`agreement.signed_file_id`) and `report/` (`client_report.storage_key`); deleting a DRAFT agreement deletes its signed-file bytes after the commit.

**Import retention:** `import_job.file_content` and `errors` are set to null as soon as an import ends (COMPLETED, FAILED or DISCARDED), in the same transaction as the status change. A nightly job in `accounts` also nulls them 30 days after upload for imports that never ended (VALIDATED). The person purge (step 4) nulls any that remain.
