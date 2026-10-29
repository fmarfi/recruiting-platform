# Domain events (contract)

Owner: architect. Java records: `contracts/events/*.java`, package `com.recruiting.platform.contracts` (shared with the enums, so every module can use them without depending on another module, see ADR 0001).

## Rules

1. **Publish** with Spring's `ApplicationEventPublisher.publishEvent(...)` inside the transaction that made the change, right after the change. One event per change (a bulk import publishes one event per row plus `ImportCompleted`).
2. **Consume** across modules only with Spring Modulith `@ApplicationModuleListener` (async, after commit, own transaction). With `spring-modulith-starter-jpa` the publication is stored in `event_publication` and retried, so delivery is **at least once**.
3. **Listeners are idempotent** on `eventId()`: tables written by listeners have a unique `source_event_id` (`timeline_entry`, `next_action`) or the update is naturally idempotent (counters are recomputed with a query, not incremented).
4. **Within a module** the same event may be handled with a plain `@TransactionalEventListener` or a direct call; the rules above are for module boundaries.
5. **Header** on every event (`DomainEvent`): `eventId` (UUID v7), `occurredAt` (UTC instant of the change), `actorId` (user, `null` for system jobs).
6. **Payload** carries ids and the few values listeners need (status from/to, reason, type). Listeners that need more read it through the publisher's public API (`AccountApi`, `PersonApi`, `JobOrderApi`), never through its tables.
7. **Every status change event carries `reason`** (required for the moves listed in `docs/plan/status-lifecycles.md`, otherwise optional and may be `null`).
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
| `PersonAnonymised` | accounts | personId, deleted | Published after anonymisation (`deleted=false`) or deletion (`deleted=true`, the FKs have cascaded, ADR 0009). pipeline: scrub `timeline_entry.reason/summary` for the person, and when not deleted also `activity.note`, `application.rejection_reason`, `next_action.title`. privacy: mark `person_privacy` ANONYMISED and null `consent_record.evidence/ip_address` (when not deleted), then run the `_aud` purge (rules in schema.md, "Anonymisation and deletion"). privacy minimises the person's DSRs before it calls accounts. |
| `ContactAdded` | accounts | contactId, accountId, personId, role | pipeline: timeline entry `CONTACT` |
| `ContactRemoved` | accounts | contactId, accountId, personId | pipeline: timeline entry `CONTACT` (the contact was ended: `ended_on` set, the row stays) |
| `ImportCompleted` | accounts | importId, kind, importedRows | privacy: none in Phase 1 beyond logging (the campaign is started by a manager with `POST /consent-requests/bulk`) |
| `JobOrderCreated` | pipeline | jobOrderId, accountId, title, ownerId, status | accounts: upsert `account_open_job` by `jobOrderId` (open = status.isOpen()), refresh `open_job_count`; pipeline: timeline `JOB_ORDER` |
| `JobOrderStatusChanged` | pipeline | jobOrderId, accountId, from, to, reason | accounts: upsert `account_open_job` by `jobOrderId` (idempotent, redelivery harmless), refresh `open_job_count`; pipeline: timeline `JOB_ORDER` |
| `ApplicationCreated` | pipeline | applicationId, jobOrderId, accountId, personId, stage | pipeline: timeline `CANDIDATE` |
| `ApplicationStageChanged` | pipeline | applicationId, jobOrderId, accountId, personId, from, to, reason | pipeline: timeline `CANDIDATE` |
| `ProposalCreated` | pipeline | proposalId, accountId, title | pipeline: timeline `PROPOSAL` |
| `ProposalStatusChanged` | pipeline | proposalId, accountId, from, to, reason | pipeline: timeline `PROPOSAL` |
| `ActivityLogged` | pipeline | activityId, type, activityAt, accountId?, personId?, jobOrderId? | accounts: `account.last_activity_at = max(...)`; `person.last_contact_at = max(...)` if `type.countsAsContact()`; privacy: recompute `basis_date`/`retain_until` when basis is FROM_LAST_CONTACT; pipeline: timeline `ACTIVITY` (when accountId set) |
| `ConsentStatusChanged` | privacy | personId, from, to, channel, reason? | accounts: copy to `person.consent_status` |
| `PersonFileUploaded` (1.2.0) | accounts | personId, fileId, kind, version | privacy: if `kind = CV`, `autoSendOnCvUpload`, the person has an e-mail, status is NOT_SENT or SENT, and no request is younger than 7 days → queue an INITIAL consent request (S-01). Any person type (ASSUMPTION: consent on every candidate). REFUSED/WITHDRAWN are not re-asked automatically. |
| `PersonMerged` (1.2.0) | privacy (merge orchestrator) | survivorId, mergedId | none in Phase 1 (every row moved synchronously in the merge transaction); later search index and `ai` re-read the survivor |
| `RetentionRenewed` (1.2.0) | privacy | personId, retainUntil | accounts: `person.retention_status = ACTIVE`; pipeline: mark the open next action created from `RetentionFlagged` for this person done |
| `RetentionFlagged` | privacy | personId, ownerId, retainUntil | accounts: `person.retention_status = FLAGGED`; pipeline: next action "Renew consent / review retention" for `ownerId` due `retainUntil` (`source_event_id` = eventId) |
| `ArchiveStateChanged` (1.3.0) | accounts (ACCOUNT, PERSON), pipeline (JOB_ORDER, PROPOSAL) | entityType, entityId, accountId?, archived, reason? | pipeline: timeline `ARCHIVE` when `accountId` is set; accounts: for JOB_ORDER upsert `account_open_job` with `open = status.isOpen() && !archived` |
| `AccountMerged` (1.3.0) | accounts (merge orchestrator) | survivorId, mergedId | pipeline: timeline `MERGE` on the survivor (rows were already moved synchronously through `AccountMergeParticipant`) |
| `TagChanged` (1.3.0) | accounts | change (`TagChangeKind`), tag, newTag? | pipeline: rewrite `job_order.tags` and `proposal.tags` set-based (`array_replace`/`array_remove`, de-duplicated, `version + 1`); idempotent. Tags are not personal data (the `Tag` rule), so rule 9 holds |
| `TargetCompanyAdded` (1.3.0, R63) | pipeline | targetCompanyId, jobOrderId, accountId | accounts: set `account.is_target = true` (role TARGET); idempotent, never cleared by events |
| `PlacementCreated` (1.3.0) | pipeline | placementId, applicationId, jobOrderId, accountId, personId, startDate, guaranteeEndOn | accounts: off-limits rule GUARANTEE_PERIOD on `accountId` until `guaranteeEndOn` unless an active rule covers it (idempotent on `placementId`, kept in the rule's `note` as `placement:<id>`); pipeline: timeline `PLACEMENT`. No pay or fee in the payload |
| `CandidateShortlisted` | ai (Phase 2) | jobOrderId, personId, score | pipeline: add application in SOURCED unless present (human decision already made, NFR-06) |

Not events on purpose: user management (security has no listeners in Phase 1), next action changes and network links (no other module needs them), consent requests and notices (internal to privacy). Since 1.2.0 also: text extraction, CV intake, duplicate detection and off-limits rules (internal to accounts), exposures (written by pipeline itself in the stage-move transaction). The sharing guard is a synchronous SPI call (`pipeline.ShareGuard`, implemented by privacy), not an event, because it must block. Since 1.3.0 also not events: ranking, client feedback, evaluations, interview notes, references, briefs, target companies, agreements, fee schedule items and their triggers, conflict checks and client reports (all inside pipeline), exports (inside security). The account merge and list exports use synchronous SPIs (`AccountMergeParticipant`, `ExportSource`) because they must run in one transaction or stream one file.

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
  └─ accounts: check transition + reason; role MANAGER+ (requiresElevatedRole); guard: no `account_open_job` row with open = true (else 409 with `blockingItems`)
       save; publish AccountStatusChanged
       └─ pipeline timeline listener → timeline_entry(kind=ACCOUNT_STATUS)
```

`accounts` does not ask `pipeline` for open job orders (that would be a forbidden accounts → pipeline dependency). The guard uses the account's own projection table `account_open_job` (upserted by job order id from `JobOrderCreated`/`JobOrderStatusChanged`, so redelivery is harmless); `open_job_count` is derived from it and the open rows become `blockingItems` in the 409. Because that copy is eventually consistent, `pipeline` also refuses to create a job order on a CLOSED account, which closes the race from the other side.

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

**CV upload, consent e-mail, sharing guard (1.2.0, ADR 0011)**

```
POST /cv-intake (files)            → accounts: cv_intake_item PENDING; Tika text; patterns → detected; duplicate check
POST /cv-intake/{id}/attach        → accounts: person (new or existing) + person_file CV v(n); publish PersonCreated?, PersonFileUploaded
  └─ privacy (PersonFileUploaded, CV): queue consent_request INITIAL → e-mail → ConsentStatusChanged NOT_SENT→SENT
person opens link (openedAt) and gives consent → consent_record GIVEN; last_consent_at; retain_until = +24 months
PATCH /applications/{id}/stage {PRESENTED or later}
  └─ pipeline: target.sharedWithClient() (PRESENTED, CLIENT_INTERVIEW, OFFER, PLACED; any forward jump)
       → ShareGuard.check(personId) (privacy SPI impl) → not GIVEN → 409 CONSENT_REQUIRED (UI: one-click resend)
       → first client-facing move: OffLimitsApi.match → 409 OFF_LIMITS unless overridden
       → ok → save; first client-facing move writes candidate_exposure(PIPELINE); warnings; publish ApplicationStageChanged
```

**Renewal (S-03, 1.2.0)**

```
nightly privacy job (basis FROM_LAST_CONSENT by default)
  ├─ retain_until - d == today for d in renewalReminderDays (60, 14), e-mail known:
  │     GIVEN → consent_request RENEWAL; SENT → INITIAL resend (both unique per person, period, d)
  ├─ retain_until - warning_days <= today → FLAGGED; RetentionFlagged (unchanged)
  ├─ person gives consent via the renewal link → consent_record GIVEN; new retain_until; FLAGGED→ACTIVE; publish RetentionRenewed
  └─ retain_until < today, not renewed → anonymise (unchanged)
```
