# ADR 0008: One `person` record for the network and for candidates; consent and retention per person

- Status: accepted (Wave 0)
- Date: 2026-10-16

## Context

The core data model puts the candidate pool in Module B (Phase 2), with an Application joining one candidate to one job order. Phase 1 already needs candidates on the pipeline board (added by hand, A-07), and the network (A-12) holds executives, client contacts and referrers — often the same people who later become candidates. KVKK applies to all of them (NFR-01, risk table), not only to CV owners.

## Decision

- **`person` (module accounts) is the single identity** for everyone the firm knows: executives, client contacts, referrers, candidates (`PersonType`). A contact is a person in a role at an account; an application references `person_id`.
- **Phase 2 adds `candidate_profile`** (module ai): 1–1 with `person`, holding the parsed CV, skills, source channel (B-03), embeddings. It extends the person; it does not replace it. B-05 (one candidate across accounts) is then already true.
- **Consent and retention attach to the person** (module privacy, `person_privacy`, `consent_record`, `consent_request`), so a client contact gets the same notice handling as a candidate. `PersonType` decides which people get the automatic e-mail (`RetentionSettings.autoSendPersonTypes`).
- **Duplicates**: e-mail is unique when present (409 `DUPLICATE` with `existingId`); smarter matching is B-04 in Phase 2.

## Consequences

- No migration between Phase 1 and 2: the pool is the network plus profiles.
- The person page shows connections, contact roles and every pipeline the person is in (B-05 preview) from day one.
- Anonymising a person affects network links and contact roles too; this is intended (the person asked to be forgotten).
