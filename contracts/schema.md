# Database schema (contract)

Owner: architect; the db-engineer turns it into Flyway migrations and JPA entities. Source: the core data model in `docs/requirements.md`, the demo domain, `contracts/openapi.yaml`.

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

## Ownership by module

| Module | Tables |
| --- | --- |
| security | `app_user`, `access_log`, `revinfo` |
| accounts | `account`, `person`, `person_employment`, `contact`, `network_link`, `import_job`, `account_open_job`, `saved_view`; since 1.2.0 `person_education`, `person_file`, `cv_intake_item`, `duplicate_candidate`, `person_merge`, `off_limits_rule` |
| pipeline | `job_order`, `application`, `proposal`, `activity`, `next_action`, `timeline_entry`, `bulk_action`, `bulk_action_item`; since 1.2.0 `candidate_exposure` |
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

**revinfo** — Envers revision entity: `rev bigint PK` (sequence `revinfo_seq`), `revtstmp bigint`, `user_id uuid null` (custom `RevisionListener` reads the current user).

### accounts

**account** — audited (except `open_job_count`, `last_activity_at`)

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| name | varchar(200) | not null; unique on `lower(name)`; trigram index |
| sector | varchar(100) | not null; index |
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
| audit columns | | |

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
| compensation_enc | bytea | null; the `Compensation` JSON encrypted with AES-256-GCM by the JPA converter `security.FieldCipher` (format: 1 byte key version, 12 byte IV, ciphertext + tag). Key from env `FIELD_ENCRYPTION_KEY_V<k>`, current version `FIELD_ENCRYPTION_KEY_VERSION`. Audited as ciphertext. Never indexed (1.2.0) |
| languages | jsonb | not null default `'[]'`; `[{language, level}]`, at most 20, validated by the service (1.2.0) |
| phone_normalized | varchar(20) | null; digits with country code (`+90` default), written by the service on every phone change; index (duplicate check, 1.2.0) |
| linkedin_url_normalized | varchar(300) | null; lower case, `https://www.linkedin.com/in/<slug>` form, no query, no trailing slash; index (1.2.0) |
| audit columns | | |

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

**person_merge** — append-only, not audited (1.2.0): `id uuid PK`, `survivor_id uuid FK person on delete cascade` (index), `merged_id uuid not null` (no FK: that row was deleted; unique), `fields_taken varchar(40)[] not null`, `merged_at timestamptz not null`, `merged_by uuid FK app_user not null`. Holds ids only. The `_aud` purge uses it to reach the merged person's history (step 4).

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
| audit columns | | partial unique on the target `where ends_on is null` (the service also rejects overlapping open rules) |

**person_employment** — audited; `id uuid PK`, `person_id FK person on delete cascade`, `company varchar(200) not null`, `title varchar(200)`, `start_date date`, `end_date date`, `sort_order int not null`. Replaced as a whole on update.

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
| audit columns | | |

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

Users are deactivated, never deleted, so `owner_id` stays valid. Filters hold ids and enum names, not personal data.
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
| audit columns | | |

`stageCounts`, `activeCandidates` and `daysOpen` are computed by query (`group by stage`), not stored.

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
| audit columns | | unique `(job_order_id, person_id)`; index `(job_order_id, stage)` |

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
| | | unique `(application_id, channel) where channel = 'PIPELINE'` (first move to PRESENTED only; idempotent) |

Written by `pipeline` in the transaction of the first move to PRESENTED. Holds ids and dates only, so it is kept on anonymisation and cascades on delete. Backfill migration: one PIPELINE row per application whose current stage is PRESENTED, CLIENT_INTERVIEW, OFFER or PLACED, or that reached PRESENTED in `application_aud` (`presented_at` = the earliest such revision time, else `stage_changed_at`).

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
| audit columns | | `CHECK (account_id IS NOT NULL OR person_id IS NOT NULL)` |

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

**Foreign-key rule:** every FK to `person` is `on delete cascade` (`person_employment`, `contact`, `network_link` both ends, `application`, `activity`, `next_action`, `person_privacy`, `consent_request`, `consent_record`; since 1.2.0 `person_education`, `person_file`, `cv_intake_item.attached_person_id`, `duplicate_candidate` both ends, `person_merge.survivor_id`, `candidate_exposure`), except `data_subject_request.person_id` (`on delete set null`). FKs to `account` stay `no action`: accounts are never hard-deleted. `PersonDeleteIT` enforces both rules.

**Order (one retention or DSR action, run by `privacy`):**

1. **Minimise DSRs** (via JPA, so Envers records it) for every `data_subject_request` with that `person_id`: `requester_name = 'Anonymised person'`, `requester_email = 'hmac:v<k>:' || hex(HMAC-SHA256(PRIVACY_HMAC_KEY, lower(trim(email))))`. The key comes from an env var outside the database; `<k>` is `PRIVACY_HMAC_KEY_VERSION`. Old keys are kept read-only for matching and values are never re-hashed (ADR 0009)., `description = '[removed]'`. Keep the rest as evidence; `resolution` must not hold personal data. DSRs are never purged in Phase 1.
2. **ANONYMISE:** `PersonDataApi.anonymise(personId)` in `accounts` sets `person.name = 'Anonymised person'`, nulls `email, phone, linkedin_url, how_we_know, notes, title, city, company`, sets `tags = '{}'`, deletes `person_employment` and `network_link` rows, sets `anonymised_at` and `retention_status = ANONYMISED`, then publishes `PersonAnonymised(deleted=false)`.
   Since 1.2.0 it also nulls `notice_period_days, mobility, compensation_enc, phone_normalized, linkedin_url_normalized`, sets `languages = '[]'`, deletes `person_education`, `person_file` (the bytes are deleted from the `FileStore` after the commit), `duplicate_candidate` rows of the person, and minimises `cv_intake_item` rows attached to the person (`file_name, extracted_text, storage_key` null). `source_channel` stays (not personal data). `person_merge` rows stay (ids only).
   **DELETE** (`RetentionAction.DELETE`, or a completed DELETION request): `PersonDataApi.delete(personId)` removes the person **through JPA** (`PersonRepository.delete`; never a native `DELETE`). Envers then writes the id-only `person_aud` row with `revtype = 2` (`org.hibernate.envers.store_data_at_delete` stays false). The FKs cascade the rest and set the DSR's `person_id` to null. It then publishes `PersonAnonymised(deleted=true)`. Since 1.2.0 it first collects the `storage_key`s of the person's files and deletes those bytes after the commit (the cascade removes only the rows).
3. **Listeners on `PersonAnonymised`:**
   - `pipeline` sets `timeline_entry.reason` and `summary` to `[removed]` for every entry with that `person_id`. Timeline text is free text written at event time, so it is scrubbed, not re-rendered; the entries and their ids stay. When `deleted=false`, it also replaces `activity.note`, `application.rejection_reason`, `application.off_limits_override_reason` (1.2.0) and `next_action.title` for the person with `[removed]`. `candidate_exposure` rows stay (ids and dates only).
   - `privacy` keeps `consent_record` rows (legal evidence; they cascade away on DELETE), nulls `ip_address` and `evidence`, and marks `person_privacy` ANONYMISED.
4. **Envers `_aud` purge**, by `privacy`, after step 3. It is idempotent and runs in its own transaction. It covers these tables and match conditions (`:p` is the set of the person id **plus every `person_merge.merged_id` with `survivor_id` = the person**, since 1.2.0: a merged record's history belongs to the survivor):

| `_aud` table | Match |
| --- | --- |
| `person_aud` | `id IN :p` |
| `person_employment_aud`, `contact_aud`, `activity_aud`, `application_aud`, `next_action_aud`, `data_subject_request_aud`, `person_education_aud` (1.2.0), `person_file_aud` (1.2.0) | `person_id IN :p` |
| `network_link_aud` | `from_person_id IN :p OR to_person_id IN :p` |
| `person_privacy_aud` | `person_id IN :p` |

   For a merged id, every matching row is deleted (that person no longer exists), except its `person_aud` row with `revtype = 2`. `person_file_aud` holds `file_name` (personal), so it follows the same rules; `off_limits_rule_aud` holds no person data.

   - ANONYMISE: `DELETE` the matching rows `WHERE revend IS NOT NULL`. In the matching rows `WHERE revend IS NULL`, set the personal columns to the same placeholders as the live row (`[removed]` or null; DSR values from step 1). In `person_aud` this includes `tags = '{}'` (AuditPurger), matching step 2 (`PersonDataService.anonymise`). The current revision must stay, because `ValidityAuditStrategy` updates its `revend` on the next change.
   - DELETE: `DELETE` all matching rows except two. Keep the `person_aud` row with `revtype = 2` (id only; the NFR-03 deletion record). Keep the current `data_subject_request_aud` row (`revend IS NULL`, already minimised), and set its `person_id` to null, because the database's SET NULL is not audited.
   - Imports: set `import_job.file_content` and `errors` to null for every import that has ended (`status IN ('COMPLETED','FAILED','DISCARDED')`). A committed CSV would otherwise still hold the person's data.
   - `account_aud`, `job_order_aud`, `proposal_aud`, `app_user_aud` and `retention_settings_aud` hold no person data. Any new audited table that holds person data must be added to this table. Otherwise the history keeps the personal data.

**File retention (1.2.0):** person files live as long as the person (no separate clock): they go with anonymisation or deletion (step 2). Pending CV intake items are deleted with their bytes 14 days after upload. A weekly sweep in `accounts` deletes `FileStore` objects under `person/` and `intake/` that no row references (bytes written for a transaction that rolled back).

**Import retention:** `import_job.file_content` and `errors` are set to null as soon as an import ends (COMPLETED, FAILED or DISCARDED), in the same transaction as the status change. A nightly job in `accounts` also nulls them 30 days after upload for imports that never ended (VALIDATED). The person purge (step 4) nulls any that remain.
