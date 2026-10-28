# Market and Competitor Research: Executive-Search CRM/ATS

Date: 2026-10-28. Method: ~14 web searches plus one page fetch (English sources). Evidence grading used throughout:
- [S] = found in a cited source (a vendor page or review site; vendor and competitor-authored pages are flagged as marketing).
- [K] = domain knowledge of the agent, NOT verified in this session. Treat as hypothesis to validate in demos.
- Known gaps: no Reddit threads surfaced; Mercury/Haley-style tools could not be identified as products (see section 2 note); field-level schemas for Invenias, Clockwork, Talentis, Zoho, JobAdder, Vincere were not publicly retrievable. Prices conflict across sources; check vendor pages before quoting.

---

## 1. Executive summary: the 15 things our app must get right

1. **Search-native data model, not a sales-CRM or staffing-ATS reskin.** Core objects: Company, Person (candidate/contact are roles of one Person), Assignment (mandate), Assignment-Candidate (stage), BD Opportunity, Fee agreement. Invenias and Clockwork win on this; generic ATSs (Zoho, JobAdder, Vincere) are job-order/temp oriented [S: Topliner, Capterra comparisons].
2. **One Person, many roles.** A person can be a candidate, a client contact, a source/referrer and a former colleague of a client. Invenias reviewers love seeing at a glance whether an email sender is client or candidate and on which assignment [S: Topliner citing Capterra].
3. **Off-limits is a first-class, enforced rule engine.** Scope: person, company, location, inherited subsidiaries; warn or block; end dates; permissioned override; auto-created from live assignment stages [S: Topliner on Invenias]. Typical contractual off-limits term is 1-2 years [S: interviewersignal, chalre].
4. **Company hierarchy (parent/subsidiary/division/location) that drives off-limits, conflicts, and roll-up reporting.** Invenias supports locations and subsidiaries [S]; Crelate docs show no parent-child hierarchy [S: help.crelate.com search result]. This is a differentiator opportunity.
5. **Fee agreement as structured data, not a PDF.** Fee model (retained 1/3 in three instalments, % of first-year cash, flat, hybrid), instalment triggers, guarantee period, expense cap, off-limits term, exclusivity. Benchmarks: 25-33% of first-year cash, thirds at launch/shortlist/start or at 0/30/60 days; guarantee 6-12 months [S: tgsus, majhigroup, adviusgroup; vendor-marketing blogs].
6. **Billing triggers and invoicing hooks.** Milestone events (assignment opened, shortlist presented, offer accepted, start date) should generate invoice-ready records and AR status; export to accounting rather than building accounting. [K for most firms; S for instalment structures.]
7. **Speed and low data-entry friction.** Top Invenias complaints: slow, "many steps to add a person", duplicates [S: Capterra/G2 reviews via Topliner and search results]. Target: add a person in under 15 seconds, dedupe on entry, parse from CV/LinkedIn/email signature.
8. **Duplicate prevention and merge** (email, LinkedIn URL, phone, fuzzy name+company). Duplicates are a named complaint [S].
9. **Consent/GDPR/KVKK built in, not bolted on.** Lawful basis per person per purpose, privacy notice sent/date, bulk consent requests, retention clocks, erasure/export (DSAR), audit log. Invenias sells Premium Data Privacy and bulk consent with purpose and legal basis [S]; Talentis/GatedTalent exists purely for this [S]. For Turkey: KVKK Law 6698, explicit consent as main basis, VERBIS registration (exemption criteria changed Oct 2025; verify), cross-border transfer rules [S: kvkk.gov.tr, commentary; confirm with counsel].
10. **Outlook/Gmail and calendar sync that works on Mac and web.** Invenias needs Windows desktop Office, cached Exchange, no IMAP, no Mac parity [S: Topliner, Capterra]. Cloud-native two-way sync (Microsoft Graph, Google) is a clear win.
11. **Client-facing portal and polished candidate reports as core, not an upsell.** Invenias gates portal and analytics behind the Executive edition [S]; Clockwork includes a client portal with control over what clients see [S]. Branded PDF/DOCX candidate reports are a daily deliverable.
12. **Longlist/shortlist workflow with scorecards.** Build from target companies and past similar assignments [S]; structured stages (identified, contacted, interested, interviewed, shortlisted, presented, offer, placed, declined, off-limits); longlist 20-40 to shortlist 6-10 is one firm's norm [S: job post; not a standard].
13. **Role-based permissions and confidentiality.** Partner / consultant / researcher / admin; confidential assignments hidden from non-team members; field-level and record-level visibility; candidate reference notes private. [K plus S for roles.]
14. **Reporting that partners actually use:** pipeline (BD weighted forecast), fees invoiced vs. forecast, time-to-shortlist, consultant productivity, assignments by stage, client concentration. "Weak reporting" and gated Power BI are repeat complaints [S].
15. **Transparent, simple pricing and fast onboarding.** Invenias has no public price, billing on peak users, add-ons and paid consultants [S: Topliner]; Loxo annual-only billing [S]. Self-serve import (CSV, Bullhorn/Invenias export), migration of history is the main switching barrier [S]. Offer clear per-seat price and free migration tooling.

---

## 2. Per-product findings

Note on "Mercury/Haley-style retained search tools": no product matching these names was found in search. Haley Marketing is a recruiter-marketing/training firm, not a search CRM [K]. Treat as unverified; the relevant retained-search specialists found were Invenias, Clockwork, Talentis/GatedTalent (Dillistone), FileFinder (Dillistone), Thrive TRM, Recruit CRM, Loxo, Salesforce-based builds (KonaSearch).

| Product | Positioning | Key capabilities found | Pricing found | Reviews / pain points | Source tags |
|---|---|---|---|---|---|
| **Invenias (Bullhorn)** | Retained/boutique exec search; ~700+ firms, 10,000+ users claimed | Assignments, longlists from competitor targeting and past similar searches, off-limits (person/company/location/subsidiary, warn/block, end dates), BD pipeline, scorecards, candidate assignment report via Document Editor, bulk consent requests, Outlook integration, Proximity radius search, client portal + Analytics + Power BI + Premium Data Privacy only in Executive edition, mobile app, AI roadmap (NL search, matching, relationship intelligence; client-supplied OpenAI/Azure key) | No public price; demo only; billed on peak monthly users; Power BI Pro licence per viewer extra | Capterra 4.2/5 (15 reviews, mostly 2017-2021). Dislikes: slow, many steps to add a person, duplicates, weak reporting, limited automation, no Zapier, Mac gaps, slow support. Likes: client/candidate identification in mail, granular off-limits, consent tooling, support. Note: Topliner is a competitor | Topliner (competitor, flag), Capterra, G2, getapp, bullhorn.com |
| **Bullhorn (core ATS/CRM)** | Staffing and recruiting enterprise | ClientCorporation entity: id, name, URL, industry/business sectors, ManagedFromId (managing branch), BranchID, InvoiceFormat (electronic/consolidated/paper). No parent-company or status field found in the CData mirror | Not retrieved | Not researched in depth | CData docs (third-party driver docs; may not match core API) |
| **Vincere (Access Group)** | Staffing/recruitment agency CRM+ATS | CRM+ATS, temp staffing module, video interviewing, analytics, LinkedIn tools, job board integration, email templates | From GBP 69/user/month (Capterra/GetApp); GBP 85 (SoftwareFinder, may be stale) | Capterra 4.8/5 (329); value for money praised; add-ons/services seen as overpriced | Capterra, GetApp, SoftwareFinder |
| **Loxo** | Sourcing-led exec search/agency/RPO | ATS+CRM, Sales CRM module for BD, NL search, Loxo Source database, AI outreach and agents, dashboards, API | Core $149, Professional $199 per user/month annual (some sources conflict: $109/$119/$169); 2,500 AI credits/seat/month on Professional | G2 4.6 (168) / Capterra 4.5-4.6 (~126-133). Annual billing mandatory; email deliverability issues; advanced sourcing behind higher tier; contact data quality varies | Sprad, Pin.com, Atlas, Capterra; some from Recruiterflow blog (competitor) |
| **Crelate** | Configurable recruiting CRM/ATS, agency and search | Core records = Contacts, Companies, Opportunities; company tabs: Summary, Related Clients, Related Candidates, Worksites, Related Opportunities; custom fields; mobile app; "Using Related Companies" article exists | ~$119/user/month (vendor-comparison sites) | G2 4.4 (210), Capterra 4.5 (443). Highly configurable; English-only; no built-in outreach sequences; bespoke exec workflows need config. No documented parent-child hierarchy found | help.crelate.com, Sprad, G2 |
| **Recruiterflow** | Easy all-in-one ATS+CRM for agencies/search | Combined ATS/CRM, AI, automations, sequences | ~$119/user/month | G2 4.6 (156), Capterra 4.7 (332) (Sprad); vendor blog claims 4.8. Back-office (pay/bill, timesheets) via integrations. Easier to use than Crelate per G2 head-to-head | Sprad, G2; vendor blog is marketing |
| **JobAdder** | Staffing/recruitment ATS (AU origin) | General ATS/CRM, job posting | Not published | Support praised vs Zoho in G2 comparison | G2, TrustRadius |
| **Zoho Recruit** | Low-cost generic ATS with Zoho CRM links | Resume parsing, social recruiting, CRM integration, automation, client portal add-on | $25-$75/user/month tiers (G2 lists $30 entry; sources conflict); client portal add-on ~$6/licence | Integration and automation praised; support slightly less responsive | G2, TrustRadius |
| **Clockwork Recruiting** | Purpose-built for exec search, Oakland CA, since 2010 | Project-centric ATS, longlists, scorecards, reports, client portal with visibility control, contact grabber (LinkedIn) , import | From ~$149 (comparison site) | Interface liked; lagging status updates; contact grabber skips work-history detail | Capterra, iSmartRecruit, clockworkrecruiting.com |
| **Talentis / GatedTalent / FileFinder (Dillistone)** | Sourcing (TalentGraph AI) and GDPR consent gate for exec search | Privacy notice and consent/legitimate-interest workflows, refresh of legacy data, integration with FileFinder Anywhere CRM | Not retrieved | Not researched. No hierarchy/off-limits detail found | dillistone.com, talentis.global |
| **Salesforce-based setups** (e.g. KonaSearch add-on; custom Candidate/Job objects) | Large/custom firms | Custom objects for candidates, projects; multi-object search (vendor case study: 580 vs 172 candidate hits) | Salesforce licences + build/admin | Cross-object search difficult by default; heavy admin burden [K] | konasearch.com (vendor marketing) |
| **Thrive TRM, Recruit CRM, Encore, Gem** | Newer/alternative options named in comparisons | Not examined | n/a | Recruit CRM: high price, slow performance (reviews) | iSmartRecruit, RFP.wiki (comparison sites with commercial interest) |

---

## 3. Feature-by-feature comparison matrix

Legend: Y = documented in a source; P = partial/add-on/tier-gated; ? = not found/unverified; N = documented absent. Mostly "?" cells reflect research gaps, not absence.

| Feature | Invenias | Clockwork | Loxo | Crelate | Recruiterflow | Vincere | Zoho Recruit | JobAdder | Salesforce build |
|---|---|---|---|---|---|---|---|---|---|
| Company record with locations | Y | ? | ? | Y (worksites, tabs) | ? | ? | ? | ? | custom |
| Parent/subsidiary hierarchy | Y (subsidiaries) | ? | ? | ? (none documented) | ? | ? | ? | ? | Y (account hierarchy) [K] |
| Off-limits rules | Y (granular) | ? | ? | ? | ? | ? | ? | ? | custom |
| BD/opportunity pipeline | Y | ? | Y (Sales CRM) | Y (opportunities) | ? | ? | ? | ? | Y native |
| Fee agreement / terms structured | ? | ? | ? | ? | ? | ? | ? | ? | custom |
| Assignment/mandate + longlist | Y | Y | P (job-centric) | P | P | P | P | P | custom |
| Scorecards | Y | Y | ? | ? | ? | ? | ? | ? | custom |
| Candidate report/export | Y (Document Editor; partial) | ? | ? | ? | ? | ? | ? | ? | custom |
| Client portal | P (Executive tier) | Y | ? | ? | ? | ? | P (add-on) | ? | Experience Cloud [K] |
| CV parsing | ? | ? | Y [K] | Y [K] | Y [K] | Y [K] | Y | ? | add-on |
| LinkedIn capture | ? | Y (grabber) | Y [K] | Y [K] | Y [K] | Y | Y (social) | ? | add-on |
| Consent/GDPR tooling | Y (Premium Data Privacy, bulk consent) | ? | ? | ? | ? | ? | ? | ? | custom |
| Email/calendar sync | Y (Outlook desktop; Mac gaps) | ? | Y [K] | Y [K] | Y [K] | ? | Y [K] | ? | Y (Einstein/Inbox) |
| Reports/dashboards | P (weak; Power BI Executive) | Y | Y (dashboards) | ? | ? | Y (analytics) | ? | ? | Y native |
| Mobile | Y | ? | ? | Y | ? | ? | ? | ? | Y |
| AI | P (roadmap, BYO key) | ? | Y (credits) | ? | Y | ? | ? | ? | Y (add-on) |
| Public pricing | N | P | Y | P | P | P | Y | N | Y (licences) |

---

## 4. Client/company record: complete field checklist

Priority key: M = must (v1), S = should (v1.x), C = could (later). Evidence: [S] sourced, [K] domain knowledge.

### 4.1 Identity and classification
| Field/section | Pri | Why |
|---|---|---|
| Legal name, display/trading name, former names | M | Search/dedupe; contracts use legal entity; M&A renames common [K] |
| Aliases / abbreviations | S | Consultants search by short names; reduces duplicates |
| Website/domain(s) | M | Primary dedupe key; email-domain matching to auto-link mail (Invenias-style sender recognition [S]) |
| Industry (hierarchical taxonomy, multi-select) and sub-sector | M | Longlist targeting by sector; Bullhorn exposes IndustryCategory/BusinessSectors [S] |
| Company type (public, private, PE-backed, family, non-profit, government) | S | Affects comp and approach [K] |
| Size: employees band, revenue band, as-of date | S | Mapping and fee-negotiation context |
| Ticker / registration no. / tax ID / VAT | S | Invoicing and KYC; tax ID needed for Turkish invoices (e-fatura) [K] |
| Ownership (PE sponsor, listed parent) | C | Sponsor-led searches are a distinct BD channel [K] |
| Logo, description | C | UI polish |

### 4.2 Hierarchy and locations
| Field | Pri | Why |
|---|---|---|
| Parent company, ultimate parent | M | Off-limits inheritance and conflict checks [S: Invenias inherits subsidiaries] |
| Subsidiaries/divisions/brands (tree view) | M | Same |
| Locations/offices (address, country, city, geo, HQ flag, type) | M | Invenias supports locations and radius search [S]; billing vs site addresses |
| Org chart (seats/positions with incumbent, reports-to, last verified) | S | Mapping/research phase; Invenias test steps mention "seats" in companies [S: Topliner] |
| Competitor / peer set links | S | Longlists built from competitors [S] |

### 4.3 Relationship and BD
| Field | Pri | Why |
|---|---|---|
| Status (prospect, target, active client, dormant, lost, do-not-contact, blocked) | M | Core filter; no status field seen in Bullhorn ClientCorporation mirror [S], so define ourselves |
| Relationship owner (partner) + secondary owners/team | M | Accountability, commission credit, BD territory [K] |
| Source of relationship / referrer | S | BD attribution |
| Relationship strength/heat score, last contact date, next action | S | Partners ask "who last spoke to them?" [K] |
| Key decision makers / buyers, board members, HR leads, with roles (hiring authority, influencer, billing) | M | Needed per assignment |
| Contacts list (linked Persons) with role at company, start/end date, primary flag | M | Contact history; Crelate links company to current and former employees [S] |
| BD opportunities (linked) with value, probability, expected close | M | BD pipeline is table stakes [S] |
| Activity timeline (emails, meetings, calls, notes) | M | Invenias/Crelate show interactions [S] |
| Hiring plans/triggers (funding, new CEO, M&A) and news feed | S/C | News is a BD trigger; vendor AI roadmap includes relationship intelligence [S]. Auto-feed = C; manual entry = S |

### 4.4 Commercial and legal terms
| Field | Pri | Why |
|---|---|---|
| Terms of business / agreement: document, signed date, effective and expiry, governing law, version, signatory | M | Gate to billing; legal evidence |
| Fee model default: retained % (typ. 25-33% of first-year cash), flat, hybrid, contingent %; basis of comp definition (base, bonus, LTI) | M | [S: fee guides, vendor marketing] |
| Instalment schedule template (e.g. thirds at launch / shortlist / start, or 0/30/60 days) and trigger events | M | [S] drives invoicing |
| Guarantee/replacement period (typ. 6-12 months), exclusions, replacement terms | M | [S] |
| Expenses policy (cap %, pass-through), indirect fee/admin fee | S | Common in retained contracts [K] |
| Payment terms (days), currency, VAT/tax rules, PO required, invoice format (electronic/consolidated/paper) | M | Bullhorn has InvoiceFormat [S] |
| Billing entity, billing address, billing contact (AP e-mail) | M | Invoicing |
| Exclusivity (sole mandate rights), conversion/contingent-to-retained terms | S | [K] |
| Off-limits / non-solicit terms: scope (entities, locations), start, end (typ. 1-2 yrs), carve-outs, direction (what we cannot approach / what they cannot be approached) | M | [S] Enforced by rule engine |
| Blocked / conflict flags and reason, who may override | M | Warn vs block [S] |
| Preferred supplier list (PSL) membership, MSP/VMS constraints | C | Matters for corporate accounts [K] |
| Rate cards / fee history across assignments, lifetime billings | S | Partner negotiation |

### 4.5 Compliance and operations
| Field | Pri | Why |
|---|---|---|
| Confidentiality level (record and assignment) and access list | M | Confidential replacement searches [K] |
| Data-processing agreement (DPA) with client, data residency | S | GDPR/KVKK B2B processing [K] |
| Contact consent/lawful basis for client contacts (legitimate interest note) | S | Contacts are personal data too |
| Documents (contracts, NDAs, briefs, org charts, proposals) with type, date, owner | M | Central doc store |
| Notes (rich text, pinned, visibility-scoped) | M | Universal |
| Tags/lists/segments (free tags + managed lists) | M | Crelate/Loxo use custom fields; tags are the cheap universal filter |
| Custom fields (admin-defined) | S | Crelate's top-cited strength [S] |
| Audit log, created/modified by, merge history | M | Dedupe and compliance |
| Language, timezone, preferred communication channel | C | Multi-country firms |
| Client-portal access settings | S | [S: Clockwork] |
| Market intelligence: comp bands, culture notes, "why people leave" | C | Differentiator for mapping |

---

## 5. Common complaints to avoid (with evidence)

1. Slow UI and "many steps to add a person" [S: Invenias reviews]. Avoid heavy forms; inline create; keyboard-first.
2. Duplicate records [S]. Match on email/LinkedIn/phone; show merge suggestions.
3. Weak reporting; BI gated and extra-cost [S]. Ship partner dashboards by default.
4. Limited automation/no Zapier/API [S]. Provide webhooks and an open API from v1.
5. Mac/web second-class citizen, Windows-only Outlook dependency, IMAP unsupported [S]. Be cloud-native.
6. Opaque pricing, peak-user billing, paid consultants, feature tiers for portal/analytics/privacy [S]. Publish prices.
7. Mandatory annual billing [S: Loxo]. Offer monthly option for pilots.
8. Outreach/email deliverability problems [S: Loxo]. Use customers' own mailboxes for 1:1 mail.
9. Sourcing-data quality variance and credit metering [S: Loxo]. Be honest about data sources; avoid credit anxiety.
10. Lagging status updates [S: Clockwork]. Real-time updates on stage changes.
11. Scrape tools that capture only top-level profile data [S: Clockwork]. Capture full work history.
12. English-only [S: Crelate]. Support Turkish and other locales (dates, names, diacritics, currency).
13. Generic staffing models (job orders, timesheets, pay/bill) that confuse a retained workflow [S: Recruiterflow/Crelate positioning]. Hide or omit.
14. Migration pain and loss of history [S]. Provide import with activity history.
15. Candidate reports only partially available (Invenias progress/research reports still in development as of June 2026) [S]. Make client reports first-class.
16. Admin burden after calls (manual), AI that needs BYO keys and legal addenda [S]. Keep AI optional, simple, auditable.
17. Pilot pain points generally [K, validate]: data migration scope creep, mailbox permission approvals by IT, partners refusing to change habits, sparse early data making dashboards look empty, off-limits list hand-off from spreadsheets.

---

## 6. Executive-search workflow norms (for requirements)

- **Retained vs contingent:** retained = paid in instalments regardless of outcome, exclusive, partner-led, confidential; contingent = paid on placement, non-exclusive [S: cowenpartners, lessie (vendor)].
- **Fees:** typically about one-third of first-year total cash (25-33%), billed in thirds: at engagement, at shortlist presentation (or day 30), at placement/offer/start (or day 60) [S: tgsus; vendor-marketing blogs; actual terms vary].
- **Guarantee:** 6-12 months; replacement search, exclusions for dismissal for cause, layoffs, role elimination [S].
- **Off-limits:** firm will not approach client's employees during and for typically 1-2 years after the search; broad lists restrict the firm's own pool [S].
- **Phases:** client research and brief, mandate definition and search strategy, market mapping and target-company list, candidate research, outreach, longlist (e.g. 20-40), interviews/assessment, shortlist (e.g. 6-10) presented in a meeting, client interviews, references, offer negotiation, onboarding follow-up [S: AESC and a job post; sizes are one source's numbers].
- **Roles:** Partner wins and owns client and outcome; (lead) consultant runs the search with the hiring manager; researcher/research associate maps companies and builds lists; admin/finance handle billing [S: Clockwork blog, AESC].
- **Confidentiality:** client identity often hidden from candidates until consent; confidential replacement searches; sensitive notes [K].
- **Billing triggers:** assignment signed/launched, shortlist presented, offer accepted/start date; expenses invoiced per policy [S for thirds; K for exact triggers].

---

## 7. Sources

Invenias and competitors
- https://topliner.app/library/bullhorn-invenias-review (competitor-authored; used for Invenias feature and complaint detail, flag as biased)
- https://www.bullhorn.com/products/invenias-executive-search/ (vendor)
- https://www.capterra.com/p/132632/Invenias/reviews/
- https://www.g2.com/products/invenias-by-bullhorn/reviews
- https://www.getapp.com/hr-employee-management-software/a/invenias/
- https://www.ismartrecruit.com/tools/invenias (comparison site)
- https://www.rfp.wiki/hr-office/talent-acquisition-recruiting-suites/executive-search-headhunting/invenias (comparison site)
- https://cdn.cdata.com/help/HAN/cis/pg_table-clientcorporation2.htm and ...clientcorporation4.htm (Bullhorn ClientCorporation schema via third-party driver)

Other products
- https://help.crelate.com/en/articles/5653488-what-is-a-company-record
- https://help.crelate.com/en/articles/4120329-adding-fields-to-record
- https://sprad.io/provider-categories/executive-search-software
- https://recruiterflow.com/blog/loxo-alternatives/ and https://recruiterflow.com/blog/executive-search-software/ (vendor marketing)
- https://www.g2.com/compare/crelate-vs-recruiterflow
- https://www.pin.com/blog/loxo-pricing, https://recruitwithatlas.com/blog/loxo-pricing/, https://www.capterra.ca/software/165046/loxo
- https://www.capterra.ca/software/152350/vincere, https://www.getapp.com/hr-employee-management-software/a/vincere
- https://www.g2.com/products/jobadder/pricing, https://www.trustradius.com/compare-products/jobadder-recruitment-software-vs-zoho-recruit, https://www.g2.com/compare/jobadder-vs-manatal-vs-zoho-recruit
- https://www.clockworkrecruiting.com/recruiting-software, https://capterra.com/p/161016/Clockwork-Recruiting/reviews/, https://ismartrecruit.com/tools/clockwork
- https://www.talentis.global/for-recruiters-old/faq-for-recruiters/, https://dillistone.com/?p=4744
- https://www.konasearch.com/case-study/digitalent/ (vendor case study)

Workflow, fees, compliance
- https://tgsus.com/executive-search-blog/executive-search-fees-search-firm-pricing/
- https://interviewersignal.com/blog/retained-search-agreement-template
- https://www.chalre.com/hiring_managers/executive_search_contracts.htm
- https://www.majhigroup.com/retained-search-fee-benchmarks-2026.html, https://adviusgroup.com/executive-search-cost-guide/, https://cowenpartners.com/the-two-types-of-executive-search-firms-fees/ (recruiter blogs, marketing)
- https://www.aesc.org/insights/executive-search/executive-search-process-what-expect
- https://www.clockworkrecruiting.com/blog/what-makes-a-successful-search-the-4-core-roles-broken-down
- https://www.kvkk.gov.tr/Icerik/7431/2022-172 (KVKK Board decision on recruitment data requests)
- https://cottgroup.com/en/blog/kvkk-gdpr, https://koksal.av.tr/en/practice-areas/personal-data-protection/ (law-firm commentary; verify Law 6698 Arts. 5, 6, 9 and VERBIS rules with counsel)

## 8. Recommended next validation steps
- Run demos/trials of Invenias, Clockwork, Loxo, Crelate to capture actual company-record fields (public docs were thin).
- Interview 3-5 consultants and 1 partner about off-limits, fee triggers, and what they do in spreadsheets today.
- Confirm KVKK/VERBIS obligations and e-invoicing needs with a Turkish lawyer.
- Search Reddit/forums directly (not reachable via this search tool) for unfiltered pilot pain points.
