# Handout: create a better plan and design for Mihenk

You are a senior product strategist, UX designer and software architect. Read this whole file, then the files it points to, then produce the deliverables in section 7. Be decisive: propose, justify briefly, and mark what you could not verify. Do not write feature code.

## 1. The product in one paragraph
Mihenk (working name; no trademark check done) is a web platform for retained executive-search firms (partners, consultants, researchers) in Turkey and internationally. It should be the one system a firm runs live searches in: clients, contacts, candidates, searches (mandates), shortlists, consent and privacy, business development, fees, and client-ready reports. Phase 1 has no AI; Phase 2 adds CV parsing and matching. First customer is a pilot firm with 2-3 consultants over about 8 weeks. Never use the pilot client's name or real people's data anywhere.

## 2. Users and their jobs
- **Managing partner:** wins clients, signs terms and fees, watches the firm's pipeline and conflicts, reads client reports.
- **Consultant:** runs searches end to end: brief, target companies, candidates, longlist, shortlist, client interviews, offer, placement. Logs calls in under 10 seconds. Lives in lists and keyboard shortcuts.
- **Researcher:** maps companies and org charts, sources candidates, prepares call sheets.
- **Data protection and operations lead:** consent, retention, data subject requests, audit, exports, invoicing triggers.

## 3. What exists today (as of 2026-10-29)
Stack: Java 21, Spring Boot 4, Spring Modulith, PostgreSQL, Flyway, Envers; React + TypeScript + Mantine frontend; OpenAPI contract in `contracts/`. About 116 API operations. 151 backend integration tests and 236 frontend tests pass.

Built: accounts (clients), contacts, people (network and candidates), job orders with a pipeline board, proposals, activities and next actions, timeline, saved views, command bar (Ctrl+K), data grid, candidate profile with encrypted compensation, CV files with text extraction, CV intake, duplicate detection and audited merge, off-limits rules with manager override, consent engine and sharing guard (no client-facing move without consent), retention and anonymisation, bulk-action API, exposure history (which clients saw a candidate), login with roles (consultant, manager, admin), TR and EN UI, a first brand look (navy, brass, Source Serif 4 and Inter).

Local login is bypassed in the `local` profile (`app.security.dev-auto-login-as`), local development only.

## 4. What is wrong (evidence in `docs/product/research/critique-*.md`)
- The backend and contract are richer than the UI. Most records cannot be edited after creation; "New client" has 3 fields; Add contact only links an existing person; job orders cannot be created from the Searches list and drop fee terms and description; tags, bulk actions and privacy screens are unreachable.
- No client-ready output (shortlist report, blind version), no longlist and shortlist as real objects, no candidate evaluation (notes, scorecards, references), no search by skill or city in the UI, no fee agreements, placements or invoices, no business-development pipeline, no email or calendar integration.
- Generic CRM vocabulary instead of search-firm language.
- Process problems: "done" meant tests pass, not that a user can do the task; research came after building; layer-based agent ownership caused interface clashes at merge.

## 5. Constraints
- KVKK and GDPR drive the data design: information notice for everyone, explicit consent as a separate step, retention default 2 years, anonymise on request, audit who viewed or exported. Any legal point is "verify with counsel".
- Accessibility WCAG 2.2 AA, keyboard-first, TR and EN for every label, long Turkish strings must not break layouts.
- Speed: lists under 300 ms; fast and uncluttered is a differentiator (competitor Invenias is called slow by reviewers).
- Contract-first: API, schema and enums are defined before building; shared interfaces live in `contracts/`.
- Light professional enterprise UI, not a re-skinned sales CRM. Brand direction "Quiet Authority" (midnight navy, ivory, one brass accent).

## 6. Read these (in this order, only what you need)
1. `README.md` and `docs/plan/STATE.md` (state), `docs/plan/work-plan.md` (waves).
2. `docs/product/brand/brand-direction.md`, `design-tokens-v2.json`, `app-architecture.md` (proposed 7 areas, command centre, data manipulation matrix, build plan).
3. `docs/product/requirements-v2/*.md` (88 requirements from three expert personas).
4. `docs/product/research/*.md` (market, domain workflows, two critiques).
5. `docs/product/product-plan-phase1.md` (the original product plan), `docs/architecture/adr/` (decisions), `contracts/schema.md`.
A code graph exists at `graphify-out/graph.json` (query with `graphify query "<question>" --graph graphify-out/graph.json --budget 1500`); do not read the 47 KB report.

## 7. Deliverables
1. **Critique of the current plan and design:** what is missing or wrong in the seven-area structure, the build plan and the brand direction, ranked by impact on a pilot with real searches. Challenge assumptions; say what you would drop.
2. **A better plan:** phases with goals, scope, exit criteria and risks for the pilot (8 weeks) and the 6 months after; the minimum lovable product per persona; sequencing by dependency; what to cut; what to buy or integrate instead of build (e-mail, calendar, PDF, e-signature, e-invoice).
3. **A better design:** information architecture, the home command centre per persona, the 10 core screens as wireframes (ASCII or Mermaid is fine) with states (empty, loading, error), the shared interaction patterns, and a design system inventory. Show the three flows to optimise: log a call in under 10 seconds, shortlist of 5 in under 3 minutes, onboard a client with complete terms.
4. **Data model review:** gaps against the requirements and a proposed change list with priorities.
5. **Agent-crew plan:** how to split the work into vertical slices (one entity end to end), definition of done including a browser-checked user task, reviewer and critic gates, token-conscious briefs.
6. **Open questions for the customer** (at most 15), each with the working assumption.

## 8. How to work
Short, concrete, no filler. Cite sources for factual market claims and flag vendor marketing. Number every recommendation so it can be referenced. Put a one-page executive summary first.
