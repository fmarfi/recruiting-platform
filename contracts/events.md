# Domain events (contract)

Owner: architect. Java records: `contracts/events/*.java`, package `com.recruiting.platform.contracts` (shared with the enums, so every module can use them without depending on another module, see ADR 0001).

## Rules

1. **Publish** with Spring's `ApplicationEventPublisher.publishEvent(...)` inside the transaction that made the change, right after the change. One event per change (a bulk import publishes one event per row plus `ImportCompleted`).
2. **Consume** across modules only with Spring Modulith `@ApplicationModuleListener` (async, after commit, own transaction). With `spring-modulith-starter-jpa` the publication is stored in `event_publication` and retried, so delivery is **at least once**.
3. **Listeners are idempotent** on `eventId()`: tables written by listeners have a unique `source_event_id` (`timeline_entry`, `next_action`) or the update is naturally idempotent (counters are recomputed with a query, not incremented).
4. **Within a module** the same event may be handled with a plain `@TransactionalEventListener` or a direct call; the rules above are for module boundaries.
5. **Header** on every event (`DomainEvent`): `eventId` (UUID v7), `occurredAt` (UTC instant of the change), `actorId` (user, `null` for system jobs).
6. **Payload** carries ids and the few values listeners need (status from/to, reason, type). Listeners that need more read it through the publisher's public API (`AccountApi`, `PersonApi`, `JobOrderApi`), never through its tables.
7. **Every status change event carries `reason`** (required for the moves listed in `docs/status-lifecycles.md`, otherwise optional and may be `null`).
8. **Adding a field** to an event is a contract change through the architect; listeners must tolerate new fields. Renaming or removing needs all listeners updated in the same PR.
9. Events never contain personal data beyond ids (no names, e-mails, notes), so `event_publication` stays out of the retention scope. Exception: `AccountCreated.name`, `JobOrderCreated.title` and `ProposalCreated.title` are business data, not personal data.

## Catalogue

| Event | Publisher | Payload (besides header) | Listeners and what they do |
| --- | --- | --- | --- |
| `AccountCreated` | accounts | accountId, name, ownerId, status | pipeline: timeline entry `ACCOUNT` |
| `AccountUpdated` | accounts | accountId, name, ownerId, ownerChanged | pipeline: timeline entry `ACCOUNT` when `ownerChanged` |
| `AccountStatusChanged` | accounts | accountId, from, to, reason | pipeline: timeline entry `ACCOUNT_STATUS` with from/to/reason |
| `PersonCreated` | accounts | personId, type, ownerId, hasEmail, importId | privacy: create `person_privacy` (NOT_SENT, retention clock); if `autoSendNotice` and type in `autoSendPersonTypes` and `hasEmail` and not imported → queue consent request (S-01). Imported people wait for the bulk campaign. |
| `PersonUpdated` | accounts | personId, type, emailChanged | privacy: if e-mail added and no request was ever sent and auto-send applies → queue consent request |
| `PersonAnonymised` | accounts | personId, deleted | pipeline: scrub `activity.note`, `application.rejection_reason`, `next_action.title` for the person; privacy: mark `person_privacy` ANONYMISED, null `consent_record.evidence/ip_address` |
| `ContactAdded` | accounts | contactId, accountId, personId, role | pipeline: timeline entry `CONTACT` |
| `ContactRemoved` | accounts | contactId, accountId, personId | pipeline: timeline entry `CONTACT` |
| `ImportCompleted` | accounts | importId, kind, importedRows | privacy: none in Phase 1 beyond logging (the campaign is started by a manager with `POST /consent-requests/bulk`) |
| `JobOrderCreated` | pipeline | jobOrderId, accountId, title, ownerId, status | accounts: recompute `account.open_job_count`; pipeline: timeline `JOB_ORDER` |
| `JobOrderStatusChanged` | pipeline | jobOrderId, accountId, from, to, reason | accounts: recompute `open_job_count` when `from.isOpen() != to.isOpen()`; pipeline: timeline `JOB_ORDER` |
| `ApplicationCreated` | pipeline | applicationId, jobOrderId, accountId, personId, stage | pipeline: timeline `CANDIDATE` |
| `ApplicationStageChanged` | pipeline | applicationId, jobOrderId, accountId, personId, from, to, reason | pipeline: timeline `CANDIDATE` |
| `ProposalCreated` | pipeline | proposalId, accountId, title | pipeline: timeline `PROPOSAL` |
| `ProposalStatusChanged` | pipeline | proposalId, accountId, from, to, reason | pipeline: timeline `PROPOSAL` |
| `ActivityLogged` | pipeline | activityId, type, activityAt, accountId?, personId?, jobOrderId? | accounts: `account.last_activity_at = max(...)`; `person.last_contact_at = max(...)` if `type.countsAsContact()`; privacy: recompute `basis_date`/`retain_until` when basis is FROM_LAST_CONTACT; pipeline: timeline `ACTIVITY` (when accountId set) |
| `ConsentStatusChanged` | privacy | personId, from, to, channel | accounts: copy to `person.consent_status` |
| `RetentionFlagged` | privacy | personId, ownerId, retainUntil | accounts: `person.retention_status = FLAGGED`; pipeline: next action "Renew consent / review retention" for `ownerId` due `retainUntil` (`source_event_id` = eventId) |
| `CandidateShortlisted` | ai (Phase 2) | jobOrderId, personId, score | pipeline: add application in SOURCED unless present (human decision already made, NFR-06) |

Not events on purpose: user management (security has no listeners in Phase 1), next action changes and network links (no other module needs them), consent requests and notices (internal to privacy).

## Main flows

**Candidate moved on the board (A-07, A-08)**

```
UI PATCH /applications/{id}/stage
  └─ pipeline: IllegalTransitionException.check(from, to, reason); guards; save; publish ApplicationStageChanged
       └─ (after commit) pipeline timeline listener → timeline_entry(kind=CANDIDATE, from, to, reason)
```

**Account closed (A-02)**

```
UI PATCH /accounts/{id}/status {CLOSED, reason}
  └─ accounts: check transition + reason; role MANAGER+ (requiresElevatedRole); guard: account.open_job_count == 0
       save; publish AccountStatusChanged
       └─ pipeline timeline listener → timeline_entry(kind=ACCOUNT_STATUS)
```

`accounts` does not ask `pipeline` for open job orders (that would be a forbidden accounts → pipeline dependency). The guard uses the account's own `open_job_count` (kept current by `JobOrderCreated`/`JobOrderStatusChanged`). Because that copy is eventually consistent, `pipeline` also refuses to create a job order on a CLOSED account, which closes the race from the other side.

**Person added, consent requested (S-01)**

```
POST /people (CANDIDATE with e-mail)
  └─ accounts: save person (consent NOT_SENT copy); publish PersonCreated
       └─ privacy: person_privacy row; queue consent_request; mail job sends (QUEUED→SENT|FAILED)
            status NOT_SENT→SENT; publish ConsentStatusChanged
            └─ accounts: person.consent_status = SENT
person clicks link → POST /public/consent/{token} {GIVEN}
  └─ privacy: consent_record(EMAIL_LINK); status SENT→GIVEN; renew retention; publish ConsentStatusChanged
       └─ accounts: person.consent_status = GIVEN
```

**Retention (S-03)**

```
nightly privacy job
  ├─ retain_until - warning_days <= today and ACTIVE → FLAGGED; publish RetentionFlagged
  │     ├─ accounts: person.retention_status = FLAGGED
  │     └─ pipeline: next action for the owner
  └─ retain_until < today and FLAGGED and consent not renewed → PersonDataApi.anonymise() (privacy → accounts call)
        └─ accounts publishes PersonAnonymised → pipeline scrubs free text; privacy closes its record
```
