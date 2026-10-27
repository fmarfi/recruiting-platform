# AI Recruiting System — Requirements Analysis (Draft)

Oct 15, 2026 · @asim

## Purpose, scope and status

We are building a recruitment platform for a Turkish recruitment consultancy so that its candidate and client data lives in its own system instead of being spread across LinkedIn and other tools. The product has two modules, and this draft structures every requirement under one of them; it is partially done and every requirement stays "Proposed" until the client validates it.

- **Phase 1 — Module A: CRM app for clients and network.** A Salesforce-style app to view and track client accounts and the firm's network of people; inside each account the firm tracks the status of the account, its proposals, its job orders and every candidate put forward.
- **Phase 2 — Module B: AI recruitment tool.** Built on top of the CRM: a central candidate pool with AI CV screening, candidate–job matching, blind screening, candidate reports and, later, assessments and interview simulation.
- **Across both:** a simple, easy-to-follow UI; KVKK consent and retention; users, roles and audit log.
- **Pilot:** an executive search firm, Russell & Williams (not public yet).
- **Out of scope for now:** billing/invoicing (open question), payroll, onboarding after placement.

Source: client meeting notes, 15 Oct 2026. Items marked **\[Note\]** come straight from the notes; items marked **\[Derived\]** are our own inference to be confirmed.

## Business context and current process

The client sells three services: recruitment services, executive search (consultancy for corporates' mid- and senior-level hires) and staffing (high-volume hiring and exits). The pilot targets executive search; staffing is where automation matters most, e.g. a retailer such as Gratis suddenly needing 400 people for 200 stores. **\[Note\]**

**Current (as-is) workflow, as described:**

1. **Client side (modelled on Salesforce as a reference):** an account is opened per client company; activities such as client visits and proposals are logged; contacts, the roles being searched and the proposal's terms are entered.
2. **Job order:** a job posting with job details is created for each role.
3. **Sourcing:** for lower positions an ad is opened on LinkedIn; some candidates reach out directly via LinkedIn; others apply through the website; existing CVs in the pool are reused.
4. **Consent:** after a CV is uploaded into the system, a KVKK consent e-mail is sent to the candidate. LinkedIn-sourced candidates reportedly need no extra consent; website applicants do.
5. **Candidate research and CV screening** by consultants.
6. **Shortlist, candidate reports and finish** (placement).

**Observations from the notes:**

- The same CVs are reused across different clients' openings for the same position, so the candidate pool is the core asset.
- CVs can stay in the pool for up to two years.
- Each client has one account; a general overview shows all accounts together.
- Networking is core to the business: the CRM app must track the firm's network of people (executives, client contacts, referrers), not only client companies.
- The main goal: candidate and client data should be owned and kept in the firm's own system, not live on LinkedIn or in scattered tools. LinkedIn stays a sourcing channel; whatever comes from it is stored in the firm's pool.

## Stakeholders and user roles

| Role | Who | Main needs |
| --- | --- | --- |
| Consultant / recruiter | Client's executive search and staffing consultants | Search and screen the pool fast, match to job orders, write candidate reports |
| Account / sales manager | Client's business development staff | Manage accounts, visits, proposals; see the funnel per client |
| Agency management | Client's partners | Overview of all accounts, pipeline and performance |
| Candidate | Applicants from LinkedIn, website, referrals | Apply easily, give or withdraw KVKK consent, take assessments |
| Hiring company (end client) | e.g. corporates, retailers like Gratis | Receive shortlists and candidate reports |
| Data protection officer / legal | Client side | KVKK compliance, retention, VERBIS registration |
| Our team | Developers | Build, host and maintain the platform |

Whether hiring companies log in themselves (a client portal) is still open.

## Functional requirements

Phase 1 delivers Module A, the CRM app; Module B, the AI tool, follows in Phase 2 on top of the same data. Priorities (MoSCoW) apply within each module's phase — change them in the dropdowns as the client confirms.

### Module A (Phase 1): CRM app — clients, network and status tracking

| ID | Requirement | Priority | Source |
| --- | --- | --- | --- |
| A-01 | One account per client company (name, sector, responsible consultant), and an overview of all accounts filterable by status. | Must | Note |
| A-02 | Account status with a defined lifecycle (e.g. lead, proposal sent, active, on hold, closed); every change is logged with date and user. | Must | Note |
| A-03 | Contacts per account with their role and contact details, linked to the network (A-12). | Must | Note |
| A-04 | Activity log per account and per person: client visits, calls, meetings, e-mails. | Should | Note |
| A-05 | Proposals per account with their commercial terms and status (draft, sent, accepted, rejected). | Should | Note |
| A-06 | Job orders inside an account with role details and job status (open, sourcing, shortlist sent, interviewing, offer, filled, cancelled). | Must | Note |
| A-07 | Candidate pipeline per job order: each candidate's stage (sourced, screened, presented to client, client interview, offer, placed, rejected) with a rejection reason. In Phase 1 candidates are added by hand; Phase 2 adds AI. | Must | Note |
| A-08 | Account timeline: every status change, activity, proposal and candidate move for that client in one chronological view. | Should | Derived |
| A-09 | Account dashboard: open jobs, candidates per stage, days open, next action. | Should | Derived |
| A-10 | Next actions and reminders with owner and due date, shown on the user's home screen. | Should | Derived |
| A-11 | Import existing accounts and contacts from CSV/Excel as a one-off migration. | Could | Note |
| A-12 | Network: one record per person the firm knows (executive, client contact, referrer, candidate) with company history, how the firm knows them, relationship owner and last contact date. | Must | Note |
| A-13 | Network links: who knows whom and who introduced whom, and a view of a person's or company's connections. | Should | Note |
| A-14 | One search box across accounts, people and job orders, with filters (sector, status, owner, last contact). | Must | Derived |

### Module B (Phase 2): AI recruitment tool

| ID | Requirement | Priority | Source |
| --- | --- | --- | --- |
| B-01 | Upload CVs (PDF/DOCX) one by one or in bulk and parse them into a structured profile: contact, experience, education, skills, languages. | Must | Note |
| B-02 | Central candidate pool owned by the firm, searchable by keyword and meaning, with filters such as sector, seniority, location. | Must | Note |
| B-03 | Record the source channel per candidate: LinkedIn ad, LinkedIn direct contact, website, referral, existing pool. | Must | Note |
| B-04 | Detect and merge duplicate candidates arriving through several channels. | Should | Derived |
| B-05 | Reuse one candidate across job orders of different accounts, with their full history visible on the profile. | Must | Note |
| B-06 | AI suitability score for a candidate against a job order and its sector, with a short explanation. | Must | Note |
| B-07 | AI CV screening: propose a ranked shortlist from the pool and new applicants for a job order; the consultant makes the final call and the result flows into A-07. | Must | Note |
| B-08 | Blind screening mode that hides name, photo, age, gender, nationality and similar fields. | Should | Note |
| B-09 | Generate an editable candidate report for the client from the profile, screening notes and assessments. | Should | Note |
| B-10 | Bulk matching for staffing: one request for many people across many locations (e.g. 400 staff for 200 stores). | Could | Note |
| B-11 | Send competency inventories to candidates and store results on the profile, ideally from a single provider. | Could | Note |
| B-12 | AI interview simulation (practice or pre-screening). | Won't (pilot) | Note ("??") |
| B-13 | Website application form that feeds directly into the pool. | Should | Note |

### Shared: KVKK consent and retention

| ID | Requirement | Priority | Source |
| --- | --- | --- | --- |
| S-01 | After a CV enters the pool, send the KVKK information notice and consent request by e-mail automatically; track status (sent, given, refused, withdrawn). | Must | Note |
| S-02 | Website form shows the KVKK information text and a separate, unticked consent checkbox. | Must | Note |
| S-03 | Flag, then delete or anonymise candidate data when the retention period (2 years per the notes) ends, unless consent is renewed. | Must | Note |
| S-04 | Let candidates request access, correction or deletion of their data, and log each request. | Should | Derived |

## Non-functional requirements

Privacy and legal compliance dominate: the system holds CVs of people who never applied to a specific job, so KVKK drives the data design. Targets marked TBD need client input.

| ID | Category | Requirement |
| --- | --- | --- |
| NFR-01 | Privacy (KVKK) | Every candidate gets an information notice (aydınlatma) — including LinkedIn-sourced ones, at first contact. Explicit consent (açık rıza) is collected as a separate, later step where needed. |
| NFR-02 | Privacy (KVKK) | Store the consent record per candidate: text version, timestamp, channel, and withdrawal date. |
| NFR-03 | Retention | Retention period configurable (default 2 years per the notes); deletion or anonymisation runs automatically and is logged. |
| NFR-04 | Data location | Hosting decided with the data-transfer rules in mind: data stored outside Türkiye needs a KVKK transfer mechanism such as standard contracts. |
| NFR-05 | Security | Role-based access (consultant, manager, admin), encryption in transit and at rest, audit log of who viewed or exported a CV. |
| NFR-06 | AI transparency | Every AI score shows its reasons; a human can override every AI suggestion; candidates can be told AI assisted the evaluation. |
| NFR-07 | Fairness | AI matching ignores protected attributes (age, gender, photo, nationality, health); periodic bias checks on shortlists. |
| NFR-08 | Performance | Pool search returns in under 2 s for a pool of TBD CVs; bulk matching of TBD candidates to 200+ locations finishes in TBD minutes. |
| NFR-09 | Language | UI in Turkish and English; CV parsing for Turkish and English CVs (open: the demo plan is English-only). |
| NFR-10 | Usability | Consultants can do the core loop (upload, match, shortlist, report) without training beyond a 30-minute walkthrough (TBD with pilot users). |
| NFR-11 | Portability | Data exportable (CSV/JSON) so the client is not locked in; a REST API leaves room for later integrations. |

## UI/UX requirements

The app must feel simpler than Salesforce: consultants should find any client, person or job in seconds and always see what to do next. Each screen answers one question, and status is always visible at a glance.

| ID | Requirement |
| --- | --- |
| UX-01 | **Home = "My day":** the user's next actions, overdue follow-ups, recent accounts and open jobs on one screen. |
| UX-02 | **Any record in 3 clicks or fewer** from home, plus a global search box (A-14) on every screen. |
| UX-03 | **Status as coloured badges** everywhere (accounts, proposals, jobs, candidates); changed with one click or by drag-and-drop on a board. |
| UX-04 | **Pipeline as a board (kanban)** per job order: one column per stage, candidates as cards. |
| UX-05 | **Account page in one view:** header with status and owner, tabs for contacts, job orders and proposals, timeline (A-08) on the side. |
| UX-06 | **Quick add:** log a call, visit or note in under 30 seconds from any account or person page, including on a phone after a client visit. |
| UX-07 | **Consistent layout:** same list, filter and detail patterns on every screen; plain labels in Turkish and English. |
| UX-08 | **Guided empty states:** a new user sees what to do first (e.g. "Add your first client"). |
| UX-09 | **Responsive:** usable on laptop and phone. |
| UX-10 | **Tested with real users:** 3–5 pilot consultants complete core tasks (add a client, change a status, move a candidate) unaided before release. |

**Core screens (Phase 1):**

1. My day (home)
2. Accounts list → Account page
3. Job order → pipeline board
4. Network list → Person page with connections
5. Search results
6. Settings: users, roles, status lists

Phase 2 adds the candidate pool, candidate profile with AI score, and the shortlist and report views.

## Core data model

Module A owns the client side (accounts, contacts, activities, proposals, job orders and their status log); Module B owns the candidate pool. The Application joins them: it ties one pooled candidate to one job order, so the same CV can serve several clients' openings while consent, files and assessments stay with the candidate.

&#91;embedded content: core data model · 11 entities\]

Retention and deletion work on the Candidate side; the AI score lives on the Application because suitability depends on the job.

## Candidate Java stack

A Spring Boot 4 modular monolith fits best: one deployable app with Module A and Module B as separate packages, Spring AI for the LLM parts, and PostgreSQL with pgvector holding both the relational data and the CV embeddings, so all data stays in one database the firm owns.

| Need | Framework | Why it fits |
| --- | --- | --- |
| Application core | **Spring Boot 4** | Current major line, on Spring Framework 7; Spring Boot 3.5 reached end of open-source support on 30 Jun 2026, so new work should start on 4.x ([HeroDevs](https://www.herodevs.com/blog-posts/spring-boot-versions-eol-dates-and-latest-releases-april-2026)). |
| Module boundaries | **Spring Modulith** | Enforces that Module A and Module B only talk through defined interfaces and events (e.g. "candidate shortlisted" updates the pipeline in A-07); can be split into services later. |
| AI: scoring, screening, reports | **Spring AI 2.0** | GA since Jun 2026, built on Spring Boot 4; structured output that self-corrects on validation failure suits typed AI scores with reasons, and it works with OpenAI, Anthropic and Google models ([Spring blog](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now/)). |
| AI alternative | **LangChain4j** | Framework-independent Java LLM library with RAG and agents; version 1.18 released Jul 2026 ([Open Collective](https://opencollective.com/langchain4j/updates)). Pick one of the two, not both. |
| Database and semantic search | **PostgreSQL + pgvector**, Spring Data JPA / Hibernate | One store for accounts, statuses, candidates and CV embeddings; Spring AI ships a pgvector vector-store starter. |
| Keyword search | **Hibernate Search** (Lucene backend) | Full-text search over CVs alongside the semantic search (B-02). |
| CV parsing | **Apache Tika**, Apache PDFBox, Apache POI | Text extraction from PDF and DOCX before the LLM turns it into a structured profile (B-01). |
| Status history and audit | **Hibernate Envers** | Automatic revision history of every entity: who changed an account or job status and when (A-02, A-08, NFR-05). |
| Status lifecycles | Enums with explicit transition rules, or **Spring Statemachine** | Guarantees only valid status moves (A-02, A-05, A-06, A-07); a plain transition table is enough for the pilot. |
| Retention and scheduled jobs | **Spring Batch** or `@Scheduled` | Nightly KVKK retention run, consent reminders (S-01, S-03). |
| Security and roles | **Spring Security** (+ Keycloak if SSO is needed) | Role-based access and login (NFR-05). |
| E-mail | **Spring Mail** + Thymeleaf templates | KVKK information and consent e-mails in Turkish and English (S-01). |
| Candidate reports | **OpenPDF** or Flying Saucer (HTML to PDF), or Apache POI for DOCX | Generates the editable report for clients (B-09). |
| Schema migrations | **Flyway** | Versioned database changes. |
| Tests | **JUnit 5**, **Testcontainers** | Integration tests against a real PostgreSQL with pgvector. |
| UI | **Vaadin** (all-Java) or a React front end on a REST API | Vaadin keeps the team in Java and ships ready-made grids, forms and layouts; React gives more freedom for custom UX like the pipeline board. Either way, use one component library throughout (UX-07). |

### Is Java the right language?

Yes, for this product. Phase 1 is a classic business app (accounts, statuses, audit history, roles), which is where Java and Spring are strongest. The AI in Phase 2 mostly calls LLM and embedding APIs, which Spring AI and LangChain4j now handle well. Python only wins if we train our own models.

| Option | Strengths here | Weaknesses here |
| --- | --- | --- |
| **Java + Spring Boot** (recommended) | Mature for CRUD, security, audit (Envers) and scheduled jobs; strong typing keeps status rules and the data model safe as the app grows; Spring AI covers LLM calls, structured output and pgvector; the lead developer is already strong in Java, which matters more than any language benchmark. | More boilerplate and slower UI iteration than a full-stack TypeScript app; fewer ready-made NLP and ML libraries than Python. |
| **Python** (FastAPI/Django) | Best ecosystem for NLP and ML (spaCy, Hugging Face, scikit-learn); Django admin gives a quick back office. | Weaker typing for a large business domain; LLM-API work is no easier than in Spring AI. |
| **TypeScript** (Node/NestJS + React) | One language front to back; fastest for polished UI. | Less mature for audit, batch jobs and strict domain modelling; easier to drift into loose data. |

**Recommendation:** build the core in Java/Spring Boot. If Phase 2 later needs custom NLP — e.g. a Turkish CV parser or our own matching model — add a small Python service next to it rather than switching languages.

## Keyword research

Owning the data is feasible as long as everything enters the firm's own pool through lawful routes: CV uploads, its own website, and LinkedIn's official integrations. KVKK requires informing every candidate, including LinkedIn-sourced ones, which the notes assumed was unnecessary.

| Keyword | What it is | What it means for us |
| --- | --- | --- |
| **KVKK** (Law No. 6698) | Türkiye's personal data protection law; the Authority (KVKK) enforces it. | Informing (aydınlatma) and explicit consent (açık rıza) are separate steps: inform first, then ask consent with a separate declaration. Data obtained from third parties still needs informing within a reasonable time or at first contact ([Cottgroup on KVKK's 21 Jul 2026 announcement](https://www.cottgroup.com/en/legislation/item/ucuncu-kisilerden-elde-edilen-kisisel-verilerin-islenmesinde-dikkat-edilmesi-gereken-hususlar)). Since 1 Sep 2024, transfers abroad can no longer rest on consent alone; standard contracts must be notified to the Authority ([Cottgroup](https://www.cottgroup.com/en/legislation/item/standart-sozlesme-bildirim-yukumlulugu-ve-yaptirimlari)). A 2-year retention for job applications is common practice ([TP Vision notice](https://www.tpvision.com/policy/TPV-Calisan-Adayi-Aydinlatma-Metni-v2.0.pdf)). Kariyer.net already sells a consent module that sends consent requests to pooled candidates ([Kariyer.net Uyum Modülü](https://kariyer.net/isveren/uyum-modulu/)) — a reference design for S-01. |
| **LinkedIn as a source** | LinkedIn stays a sourcing channel; the data must end up in the firm's pool. | Lawful ways to bring LinkedIn candidates in: the candidate sends a CV or profile PDF, or LinkedIn's ATS integrations push applicants into the firm's system (Apply Connect, Apply with LinkedIn, Recruiter System Connect) ([LinkedIn ATS partners](https://business.linkedin.com/talent-solutions/partners)). Those integrations need an approved partner, so they are a later step. Automated scraping is banned by LinkedIn's User Agreement and enforced in court ([ConductAtlas summary](https://conductatlas.com/platform/linkedin/linkedin-user-agreement/prohibition-on-scraping-and-automated-data-collection/)). |
| **Salesforce** | The CRM we use as the reference model for Module A; no integration planned. | Module A borrows its proven concepts: Account, Contact, Activity, Opportunity with sales stages (our proposal and job order statuses) and a per-record timeline. Recruitment software built on Salesforce follows the same pattern: Bullhorn is the largest Salesforce partner for recruitment and bought KonaSearch for search-and-match for executive search ([Bullhorn press release](https://www.bullhorn.com/news-and-press/press-releases/bullhorn-acquires-konasearch-to-bolster-sourcing-for-executive-search-firms-using-bullhorn-for-salesforce)). We copy the concepts that fit, not the whole CRM. |
| **Vinter** | AI recruitment SaaS from Norm Digital (Izmir), London-registered, R&D in Türkiye; raised €1.4M seed in Nov 2024 ([AI Insider](https://theaiinsider.tech/?p=29525)). | Offers asynchronous AI interviews, competency inventories, language tests and AI scoring, positioned as decision support ([Türkiye Today](https://www.turkiyetoday.com/business/ai-powered-recruitment-revolutionizes-hiring-processes-7157)). Either a competitor or the "single system" for inventories (B-11) and interview simulation (B-12) via integration. |
| **Executive search** | Retained search for senior and C-level roles; consultants headhunt passive candidates. | Low volume, high value: matching quality, candidate research and reports matter more than throughput. Pilot focus. |
| **Staffing** (notes: "stuffing") | High-volume hiring, and exits, for many similar roles. | Bulk matching across locations (B-10). Example scale: Gratis runs 700+ stores in 81 provinces with 8,000+ store staff ([Kariyer.net listing](https://www.kariyer.net/is-ilani/gratis-gratis-magaza-muduru-istanbul-tuzla-marina-avm-4455307)). |
| **Funnel management** | Tracking candidates through hiring stages, and clients through sales stages. | Two funnels, both inside Module A: candidate pipeline per job order (A-07) and account and proposal status (A-02, A-05). |
| **Competency inventory** (yetkinlik envanteri) | Standardised questionnaire measuring behavioural competencies and personality. | Prefer integrating one provider over building our own; results feed reports (B-09). |
| **Blind screening** (blindlama) | Hiding name, photo, age and similar fields during CV screening. | Evidence: it reduces bias at the screening stage only, and works better when blinding is thorough than name-only ([JOIN glossary](https://join.com/en/glossary/blind-hiring)). Fits B-08 and NFR-07. |
| **EU AI Act** | EU law classing AI for recruitment and CV screening as high-risk (Annex III, point 4). | Only applies if the product is offered in the EU, but relevant since our team is EU-based. High-risk obligations now start 2 Dec 2027; telling candidates AI was involved applies from 2 Aug 2026 ([MintHCM](https://minthcm.org/ai-act/)). Building human oversight and explanations in now (NFR-06) keeps that door open. |

## Constraints, assumptions and risks

The biggest risk is the existing pool: if current CVs lack a KVKK basis, the firm owns data it cannot lawfully use for matching, so consent handling has to ship with the first version.

**Constraints**

- KVKK applies to all candidate and client-contact data; transfers outside Türkiye need a valid mechanism (NFR-04).
- LinkedIn data only through official integrations or candidate-provided files.
- Private employment agencies in Türkiye work under an İŞKUR licence and may not charge job seekers, so all costs sit on the hiring-company side.

**Assumptions (to confirm)**

- The pilot runs with one executive search team at Russell & Williams (not public yet; keep the name out of external material).
- We first build a demo on synthetic CVs, then a client version.
- The firm is the data controller; we act as data processor under a processing agreement.
- Module A is modelled on Salesforce's account concepts; there is no Salesforce integration.

**Risks**

| Risk | Impact | Mitigation |
| --- | --- | --- |
| KVKK consent gaps in the existing pool | Data cannot be used for matching | Run a consent campaign on import (S-01); mark unconsented records |
| LinkedIn data entered by scraping tools | Legal exposure, account bans | Only CV uploads, the website form and official LinkedIn integrations feed the pool |
| AI bias or unexplained scores | Lost trust, discrimination claims | Explanations, human override, blind mode, bias checks |
| Turkish CVs while the demo is English-only | Poor parsing in the pilot | Decide language scope before building the parser |
| Module A grows into a full CRM | Pilot slips; screens too complex for consultants | Copy only the account, contact, activity and status concepts the pilot needs |
| Status lists don't match how consultants work | Statuses ignored, timeline incomplete | Validate the status lifecycles (A-02, A-05, A-06, A-07) with pilot consultants first |
| Scope creep (interview simulation, inventories) | Pilot slips | Keep them out of the pilot (Won't / Could) |

## Open questions for the client

These came up as gaps or "??" in the notes; answers decide scope and priorities.

- [ ] How are CVs stored today (shared drive, e-mail, another tool), in what format, and how many are in the pool?
- [ ] What exactly is invoiced, and to whom (retainer, success fee, per hire)? Does billing belong in the system?
- [ ] Which statuses do they use today for accounts, proposals, job orders and candidates, and who may change them?
- [ ] Which parts of the Salesforce account model do they want (accounts, contacts, activities, opportunity stages), and which can go?
- [ ] Which LinkedIn products do they pay for (Recruiter, Recruiter Lite, job slots)?
- [ ] What is the legal basis used for LinkedIn-sourced candidates today, and is there an information notice?
- [ ] Is 2 years the agreed retention period, counted from upload or from last contact?
- [ ] Which competency inventory provider do they use now, if any?
- [ ] Should hiring companies get their own login to see shortlists and reports?
- [ ] Which CV languages appear: Turkish only, or Turkish and English?
- [ ] What does a good candidate report look like? Ask for an anonymised sample.
- [ ] Is interview simulation for candidate practice, or for pre-screening?
- [ ] Where must data be hosted (Türkiye only, or EU allowed under standard contracts)?
- [ ] Pilot success criteria: time to shortlist, consultant hours saved, placements?

## Proposed pilot scope and next steps

Phase 1 delivers the CRM app consultants use every day to track clients, their network and every status; Phase 2 adds the AI tool on the same data, so nothing has to be migrated.

1. **Phase 1a — CRM demo (synthetic data):** accounts with status, contacts, network, job orders with the pipeline board, My day home and search (A-01 to A-03, A-06, A-07, A-10, A-12, A-14; UX-01 to UX-05).
2. **Phase 1b — CRM pilot with Russell & Williams:** activities, proposals, account timeline and dashboard, network links, CSV import (A-04, A-05, A-08, A-09, A-11, A-13), plus KVKK consent and retention for every person stored (S-01 to S-03).
3. **Phase 2 — AI recruitment tool:** CV upload and parsing, candidate pool, AI scoring and screening feeding the pipeline board, blind mode, candidate reports, website form (B-01 to B-09, B-13).
4. **Phase 3:** staffing bulk matching (B-10), competency inventories (B-11), interview simulation (B-12), LinkedIn partner integrations.

**Next steps**

- [ ] Send the open questions to the client before the next meeting.
- [ ] Validate the Must list with the pilot consultants.
- [ ] Get a KVKK-qualified lawyer to review the consent flow and hosting choice.
- [ ] Ask for 10–20 anonymised sample CVs and one sample job order.
- [ ] Make clickable wireframes of the 6 core screens and walk 2–3 consultants through them.

## Sources

- [KVKK third-party data announcement, summary by Cottgroup](https://www.cottgroup.com/en/legislation/item/ucuncu-kisilerden-elde-edilen-kisisel-verilerin-islenmesinde-dikkat-edilmesi-gereken-hususlar)
- [KVKK standard contracts and transfers abroad, Cottgroup](https://www.cottgroup.com/en/legislation/item/standart-sozlesme-bildirim-yukumlulugu-ve-yaptirimlari)
- [TP Vision candidate information notice (2-year retention example)](https://www.tpvision.com/policy/TPV-Calisan-Adayi-Aydinlatma-Metni-v2.0.pdf)
- [Kariyer.net Uyum Modülü](https://kariyer.net/isveren/uyum-modulu/)
- [LinkedIn ATS & Talent CRM partners](https://business.linkedin.com/talent-solutions/partners)
- [LinkedIn User Agreement scraping clause, ConductAtlas](https://conductatlas.com/platform/linkedin/linkedin-user-agreement/prohibition-on-scraping-and-automated-data-collection/)
- [Bullhorn acquires KonaSearch](https://www.bullhorn.com/news-and-press/press-releases/bullhorn-acquires-konasearch-to-bolster-sourcing-for-executive-search-firms-using-bullhorn-for-salesforce)
- [Vinter seed funding, AI Insider](https://theaiinsider.tech/?p=29525)
- [Vinter by Norm Digital, Türkiye Today](https://www.turkiyetoday.com/business/ai-powered-recruitment-revolutionizes-hiring-processes-7157)
- [Gratis job listing (store count), Kariyer.net](https://www.kariyer.net/is-ilani/gratis-gratis-magaza-muduru-istanbul-tuzla-marina-avm-4455307)
- [Stanton Chase Istanbul (İŞKUR licence note)](https://pre-web.stantonchase.com/office/executive-search-firm-in-istanbul-turkey)
- [Blind hiring, JOIN glossary](https://join.com/en/glossary/blind-hiring)
- [EU AI Act and HR, MintHCM](https://minthcm.org/ai-act/)
- [Spring Boot versions and EOL dates, HeroDevs](https://www.herodevs.com/blog-posts/spring-boot-versions-eol-dates-and-latest-releases-april-2026)
- [Spring AI 2.0.0 GA, Spring blog](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now/)
- [LangChain4j releases, Open Collective](https://opencollective.com/langchain4j/updates)
