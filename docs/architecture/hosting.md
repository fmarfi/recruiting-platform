# Hosting options for the pilot (NFR-04)

Status: draft by devops, Wave 0. Not legal advice. Items marked **[unverified]** come from general knowledge and must be confirmed with the firm's KVKK counsel or the provider before a decision.

## The constraint

NFR-04: data stored outside Türkiye needs a KVKK transfer mechanism such as standard contracts. The platform holds candidate and client-contact personal data (KVKK applies to all of it), so where the database, backups, logs and e-mail are physically located matters.

Background (as understood, **[unverified]** against the current text of Law No. 6698 Art. 9):

- Since the 2024 amendment of Art. 9, transfers abroad are allowed on an adequacy decision, or, without one, on appropriate safeguards: a standard contract published by the KVKK Authority, binding corporate rules, an undertaking approved by the Board, or a non-repeated-use agreement.
- The standard contract must be signed and then **notified to the KVKK Authority within a short deadline after signing** (believed to be 5 business days) **[unverified]**. Notification is done by the data controller (the firm), not the hosting provider.
- To our knowledge no country adequacy decision has been issued yet **[unverified]**, so EU hosting would rely on the standard contract.
- Hosting inside Türkiye means no cross-border transfer for storage, but sub-processors, support access, backups and the LLM provider (Phase 2) can still create transfers.

## Options

| Option | Data location | KVKK transfer mechanism | Pros | Cons / risks |
| --- | --- | --- | --- | --- |
| A. Turkish data centre or Turkish cloud/VPS provider (e.g. Turkcell, Türk Telekom, Natro and similar) **[providers unverified]** | Türkiye | None for storage | Simplest compliance story; no notification for hosting | Less managed tooling; check backups and support staff location; pgvector support depends on provider (a plain VM with our Docker image avoids this) |
| B. Hyperscaler region in Türkiye, if one exists **[unverified]** | Türkiye | None for storage | Managed Postgres, good tooling | Availability of a Turkish region and pgvector on the managed DB must be checked; provider support access from abroad may still count as transfer |
| C. EU region (e.g. Hetzner DE/FI, OVH FR, or AWS/Azure/GCP EU regions) | EU | Standard contract signed and notified to the Authority; transfer-impact thinking by counsel | Cheap, mature, managed Postgres with pgvector common | Extra legal step before the first real personal data; contract and notification must be repeated or maintained if the recipient changes |
| D. Local laptop / on-prem at the firm for demo only | Firm premises | None | Zero cost; fine for the synthetic-data demo | Not suitable for the pilot with real data (backup, uptime, security) |

## Recommendation

1. **Pilot with real data: Option A (Türkiye)** on a single VM or small managed host running the same Docker images we use locally (PostgreSQL 17 + pgvector, the app jar, a reverse proxy with TLS). It removes the transfer question for the core data and the schedule risk of the notification step. Backups stay in Türkiye too.
2. **Until a decision is taken, everything runs on synthetic data** (demo and Wave 1 to 2 development) on any host, including EU or local, since no personal data is involved.
3. **Keep the option of C**: nothing in the stack is host-specific (plain Postgres, SMTP, env-var config), so moving to an EU region later is a redeploy plus the standard-contract paperwork.
4. **Phase 2 (AI)**: sending CVs to an LLM API hosted outside Türkiye is itself a cross-border transfer. Decide the provider and region before B-01 starts; this may outweigh the choice made for the database.

## Technical requirements for any host

- PostgreSQL 17 with the `vector` extension (pgvector 0.8); or a VM where we run the `pgvector/pgvector` image.
- Java 21 runtime, about 1 to 2 GB RAM for the app, TLS in front, an SMTP relay for the KVKK e-mails.
- Encrypted disks and encrypted backups; backups kept in the same jurisdiction as the primary.
- Secrets only in the host's secret store or environment (see `.env.example`); pilot profile has no defaults for them.

## Open questions

1. Does the firm already have a hosting contract or a preferred provider? Does it have KVKK counsel who will sign off on the transfer mechanism?
2. Who is the data controller and who is the processor for the pilot (the firm, or the firm plus us)? Affects who signs and notifies.
3. Which e-mail relay will send the consent e-mails, and where is it located (SMTP provider is also a processor)?
4. Which LLM provider for Phase 2, and is a Türkiye-resident or EU-resident option acceptable?
5. Is there a Turkish provider that offers managed PostgreSQL with pgvector, or do we run it ourselves on a VM? **[unverified]**
6. Expected data volume and number of users for the pilot (sizing), and required availability/backup RPO.
7. Verify the current Art. 9 text, the notification deadline and whether any adequacy decision has been published.
