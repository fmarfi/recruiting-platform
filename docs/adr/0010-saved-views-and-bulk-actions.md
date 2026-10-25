# ADR 0010: Saved list views and bulk actions

- Status: accepted (Wave 2a, contract 1.1.0)
- Date: 2026-10-23

## Context

Wave 2a adds one Data Grid for people, accounts, job orders and proposals (docs/mastermind-plan.md §2, §5). Users want to save filters, columns, sort and grouping as private or shared views, and to act on many selected records at once. The contract is frozen, so the change must be additive. Mass actions must not bypass the status machine (ADR 0002), the KVKK consent rules (S-01, mastermind §3.5) or the audit trail (ADR 0005).

## Decision

- **Saved views** (`/views`, module accounts, table `saved_view`). A view has `entity` (`ListEntity`), `name`, `visibility` (`ViewVisibility`), `owner`, a `definition` `{filters, columns, sort, groupBy}` stored as jsonb, and a `version`. The filter keys are the query parameters of the entity's list endpoint, so the client runs a view by calling that endpoint. No new query language. The server validates keys and values per entity. `ownerId: "me"` makes one shared view work for every user.
- **Visibility.** Users see their own PRIVATE views and all SHARED views. Another user's private view returns 404. **ASSUMPTION** (open question for the client): only MANAGER/ADMIN create SHARED views or switch a view to SHARED. A shared view can be changed by its owner and by managers/admins. Views are user settings, not business records, so the pilot rule "any consultant may change any record" (ADR 0007) does not apply to them.
- **Bulk actions** (`POST /bulk-actions`, synchronous, at most 500 ids). The request is a typed union on `action` (`BulkActionType`) with matching `params`. The response is 200 with one result per id (`BulkOutcome` OK/SKIPPED/FAILED plus a `BulkResultCode`). 400 is used only when the request as a whole is invalid.
  - **One transaction per record**, so partial success is normal.
  - **Same services as the single endpoints.** `CHANGE_STATUS` calls the lifecycle check (`IllegalTransitionException.check`, guards, manager-only moves). Lifecycle errors become per-record codes, so there is no second status path. Events, timeline and Envers work unchanged.
  - **Consent gate.** Actions that contact people only reach people with a valid basis. All others are SKIPPED with an explicit code, never silently dropped. Bulk `REQUEST_CONSENT` does not re-ask people who REFUSED or WITHDREW, skips GIVEN, no e-mail, a pending request under 7 days and anonymised people. It is MANAGER/ADMIN only, like the consent campaign (ASSUMPTION). Later contact actions (templated e-mail) require GIVEN, or SKIPPED `NO_CONSENT_BASIS`.
  - **Audit.** A `bulk_action` row stores the actor and params. A `bulk_action_item` row is written per requested id with its outcome, including SKIPPED/FAILED records, which leave no Envers revision. Each item row is written in its own transaction so the evidence survives a rollback.
  - **Placement.** The dispatcher sits in `pipeline` (backend-pipeline), which may call `accounts`. It defines a `BulkActionHandler` SPI in its public package. `privacy` implements the REQUEST_CONSENT handler (privacy → pipeline is an allowed dependency, ADR 0001). This mirrors the `SearchContributor` pattern.
- **Tags** are a `varchar(50)[]` column on `person`, `account`, `job_order` and `proposal`, owned by each table's module. A separate tag table would need cross-module writes and its own erasure rule. Tags hold no personal data and are cleared on anonymisation: `PersonDataService.anonymise` sets `person.tags = '{}'`, and the `_aud` purge (`AuditPurger`) sets `tags = '{}'` in the current `person_aud` revision and deletes the older ones (schema.md, step 4).
- **No new domain event.** Bulk changes publish the existing per-record events.

## Consequences

- New enums: `ListEntity`, `ViewVisibility`, `BulkActionType`, `BulkOutcome`, `BulkResultCode`. No existing enum, schema or operation changes.
- Tags can be written but are not yet in the read models or list filters. Adding `tags` to `Person`/`Account`/`JobOrder`/`Proposal` and a `tag` query parameter is a follow-up additive change (then `tag` becomes a view filter key).
- A manager view of past bulk actions (`GET /bulk-actions`) can be added later from the same tables.
- 500 records with one transaction each stays fast enough for a synchronous call only for simple actions (NFR-08). If REQUEST_CONSENT gets slow, the endpoint can move to 202 + job without changing the result schema.

## Amendment 1 (contract 1.1.1, 2026-10-25): no personal data in the bulk audit

Context: `bulk_action.params` stored the typed params as sent, which includes free text (`SetNextActionParams.title`, `ChangeStatusParams.reason`) that may name a person, and `bulk_action_item.message` was a free `varchar(500)`. Neither is reached by the anonymisation scrub or the `_aud` purge (they are not keyed by person), so a name typed there would outlive the person's erasure.

Decision (the simplest rule that is safe by construction, instead of a purge rule):

- **`bulk_action.params` holds ids, enum names, dates, numbers, the tag and the language only.** The dispatcher removes `title` and `reason` before it inserts the row; a DB `CHECK (NOT (params ?| array['title','reason']))` enforces it. The text is still applied to every record (next action title, status reason), where the existing anonymisation scrub (`next_action.title`, timeline `reason`, Envers purge) already covers it. Any future action with free text follows the same rule: free text goes to the records, never to the bulk audit.
- **`bulk_action_item` has no `message` column** (dropped). `BulkRecordResult.message` is `BulkResultCode.message()`, one fixed English text per code, never a name, e-mail, record value or exception message.
- Tags in params are allowed: tags hold no personal data by rule (`Tag` schema), the same rule as on the records.
- With this, `bulk_action` and `bulk_action_item` hold only ids and codes and are out of the anonymisation scope; `entity_id` of an erased person points at nothing, like `access_log.entity_id`.

Consequences: db-engineer drops `bulk_action_item.message` and adds the CHECK; backend-pipeline strips the free-text members before insert and fills `message` from the code. No API shape changes (1.1.1 is additive).
