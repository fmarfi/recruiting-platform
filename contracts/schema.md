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
| accounts | `account`, `person`, `person_employment`, `contact`, `network_link`, `import_job`, `account_open_job` |
| pipeline | `job_order`, `application`, `proposal`, `activity`, `next_action`, `timeline_entry` |
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
```

Phase 2 adds `candidate_profile` (1–1 with `person`, owned by `ai`), `cv_file`, `cv_embedding vector(n)` and an AI score on `application`. Retention and consent already work on `person`, so nothing moves (ADR 0008).

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
| city | varchar(100) | null |
| linkedin_url | varchar(300) | null |
| how_we_know | text | null |
| notes | text | null |
| owner_id | uuid FK app_user | not null; index |
| last_contact_at | timestamptz | null; from `ActivityLogged` where `type.countsAsContact()`; index |
| consent_status | varchar(40) | `ConsentStatus`, default `NOT_SENT`; read-only copy, maintained from `ConsentStatusChanged` |
| retention_status | varchar(40) | `RetentionStatus`, default `ACTIVE`; copy, maintained from `RetentionFlagged` and anonymisation |
| anonymised_at | timestamptz | null; when set, personal columns hold placeholders |
| audit columns | | |

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
| audit columns | | unique `(job_order_id, person_id)`; index `(job_order_id, stage)` |

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
| audit columns | | |

**activity** — audited

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| type | varchar(40) | `ActivityType` |
| note | text | not null |
| occurred_at | timestamptz | not null |
| account_id | uuid FK account | null; index `(account_id, occurred_at desc)` |
| person_id | uuid FK person | null; index `(person_id, occurred_at desc)` |
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
| account_id, person_id, job_order_id | uuid FK | all null allowed |
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

**retention_settings** — single row (`id smallint PK CHECK (id = 1)`), audited: `retention_months int default 24`, `basis varchar(40) default 'FROM_CREATED'` (ASSUMPTION until the client answers), `action varchar(40) default 'ANONYMISE'`, `warning_days int default 30`, `auto_send_notice boolean default true`, `auto_send_person_types varchar(200) default 'CANDIDATE,EXECUTIVE'`, `renew_on_consent boolean default true`, `version`.

**retention_run** — `id uuid PK`, `started_at`, `finished_at`, `triggered_by uuid FK app_user null`, `dry_run boolean`, `flagged_count`, `anonymised_count`, `deleted_count int`, `failure_reason text`.

**data_subject_request** — audited

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid PK | |
| person_id | uuid FK person | null until identified; `on delete set null` |
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

## Anonymisation (S-03, S-04)

`PersonDataApi.anonymise(personId)` in `accounts` sets `person.name = 'Anonymised person'`, nulls `email, phone, linkedin_url, how_we_know, notes, title, city, company`, deletes `person_employment` and `network_link` rows, sets `anonymised_at` and `retention_status = ANONYMISED`, then publishes `PersonAnonymised`. On that event:

- `pipeline` replaces `activity.note`, `application.rejection_reason`, `next_action.title` for that person with `[removed]`, and also scrubs `timeline_entry.reason` and `timeline_entry.summary` of every entry with that `person_id` (set to `[removed]`). Decision: timeline text is free text written at event time and may contain names, so it is scrubbed (not re-rendered at read time); the entries and their ids stay.
- `privacy` keeps `consent_record` rows (legal evidence) but nulls `ip_address` and `evidence`, and marks `person_privacy` ANONYMISED.
- **Envers `_aud` purge**, in the same run, by person id: `person_aud`, `person_employment_aud`, `network_link_aud` (either end), `contact_aud`, `activity_aud`, `application_aud`, `next_action_aud`, `person_privacy_aud`. Any other audited table that holds person data must be added to this list (`account_aud`, `job_order_aud`, `proposal_aud` hold none). Otherwise the history would keep the personal data.
- With `RetentionAction.DELETE` the person row is deleted instead; FKs from `application`, `activity`, `contact` must then be `on delete cascade` or nulled — the db-engineer applies `on delete cascade` to `contact`, `person_employment`, `network_link`, `application`, `person_privacy`, `consent_request`, `consent_record`, and `on delete set null` to `activity.person_id`, `next_action.person_id`, `data_subject_request.person_id`.

**Import retention:** `import_job.file_content` and `errors` are deleted (set to null) 30 days after upload when the import was not committed; a nightly retention job in `accounts` does this.
