# Mihenk: Partner requirements, BD and firm management (v2)

Author view: managing partner, retained search, Turkey and Europe. Pilot = 2-3 consultants, 8 weeks. Priority: Must / Should / Could for that pilot.
Fact vs opinion: items marked [F] rest on a cited source; everything else is practitioner opinion. Fee, guarantee and off-limits norms come from marketing-grade sources and vary by firm. Nothing here is legal or tax advice; items tagged **[LAWYER]** or **[TAX]** need a professional.

Sources: [fee structure, guarantee, off-limits (vendor blog)](https://recruiterflow.com/blog/ai-in-retained-search/), [KiTalent procurement guide (vendor)](https://kitalent.com/executive-search-procurement-guide.md), [KPI definitions (vendor)](https://recruitbpm.com/?p=11143), [leadership vs management dashboards (vendor)](https://www.rocket.new/blog/create-a-live-hiring-dashboard-for-executive-search-teams), [e-Fatura/e-Arsiv summary (vendor)](https://lookuptax.com/docs/einvoicing/e-fatura-e-arsiv-einvoicing-turkey), [VERBIS exemption change (secondary)](https://cottgroup.com/en/blog/work-life/item/verbise-kayit-zorunlulugu-istisnalarinda-degisiklik). Internal: domain-workflows.md 1.1-1.8, critique-consultant-review.md (#9, #11, #12).

## A. Business development

**PB-001 Target-company map (Must).** Target list per sector/theme: trigger event, source, owner, priority, last touch, status (Target, Contacted, Meeting, Proposal, Client, Dormant).
Why: BD is the partner's second job and lives in Excel.
- Given a researcher adds a company with trigger "new CEO" / When saved / Then it appears on the owner's target view, status Target, with a conflict-check badge.
- Given the company is already a client / When added / Then the user is pointed to the existing record, no duplicate.
Fields: company_id, trigger_type, trigger_date, source_url, owner_id, priority, status, last_touch_at.
Edge: parent is a client (see PB-018); Turkish and English trade names; renamed or merged company.

**PB-002 Trigger-event list (Could).** Dated signals (funding, M&A, resignation) on a target; filter "signals this month".
Why: partners call when something happened.
- Given 3 signals this month / When I filter / Then only targets with a signal in range show.
Fields: signal_id, company_id, type, date, note, source. Edge: signal on a subsidiary shows on the parent.

**PB-003 First-meeting record (Must).** Attendees, needs heard, urgency, competitors in play, confidentiality flag, next step with date, how introduced.
Why: the first meeting is the asset; today it dies in notebooks.
- Given a meeting saved without next-step date / When saving / Then the system asks for one or an explicit "no follow-up".
Fields: activity_id, contact_ids, date, needs_text, urgency, competitor_firms[], confidential, introduced_by, next_step, next_step_due. Edge: two partners attended; meeting on a restricted target.

**PB-004 Opportunity with money (Must).** Roles, expected fee (computed from fee model), probability, decision date, stage, owner.
Why: partners think in expected fees, not counts (critique #11).
- Given expected fee 900,000 TRY at 40% / When viewed / Then weighted 360,000 shows and rolls into pipeline totals.
Fields: opportunity_id, account_id, roles[], comp_band_estimate, fee_model_id, expected_fee, currency, probability, decision_date, stage. Edge: mixed currencies (show rate and date); several roles in one opportunity.

**PB-005 Proposal versions (Must).** Generate TR/EN proposal from template with team, approach, timeline, fee, guarantee pulled from the fee model; versions, sent date, validity date.
Why: proposals are re-sent with small edits; versions end disputes.
- Given v2 sent / When the client accepts / Then v2 is marked accepted, opportunity moves to Won, and creation of the agreement is prompted (PB-007).
Fields: proposal_id, version, language, template_id, sent_at, valid_until, status, status_reason, file_id. Edge: client signs an older version; proposal expires; contact left.

**PB-006 Won/lost with reason (Must).** Mandatory reason enum, competitor, decision date.
Why: lost reasons teach pricing and positioning.
- Given status Lost / When saving / Then reason is required and a 12-month re-contact reminder is proposed.
Fields: outcome, lost_reason, competitor, decided_on, recontact_on. Edge: "no decision" distinct from lost; reopened after loss.

## B. Terms, fees, invoicing

**PB-007 Terms of business and engagement letter as data (Must).** Structured agreement: signed date, signatory, language, governing law, exclusivity, fee schedule, guarantee, expense rules, off-limits term, confidentiality, document. One framework agreement per account plus one engagement per search.
Why: a PDF is not queryable; disputes start with "what did we agree".
- Given a search without a signed engagement / When a consultant moves it past Kickoff / Then it is blocked unless a partner overrides with a reason.
Fields: agreement_id, account_id, search_id?, signed_on, signatory_name, signatory_role, exclusivity, governing_law, language, document_id, supersedes_id. Edge: work starts on e-mail approval before signature (override logged); amendments; two group entities contracting separately. **[LAWYER]** clause wording, governing law, e-signature validity; the product stores terms, never interprets them.

**PB-008 Fee models (Must).** Retained in thirds; retained with custom instalments; percent of first-year cash; flat fee; hybrid (retainer + success). Fee basis is explicit with toggles: base, target or paid bonus, allowances, sign-on, car, other cash.
Why: the definition of first-year cash causes most fee arguments. [F] Vendor sources describe thirds (at launch, after a period or milestone, later) and percent of first-year pay including expected bonus; practice varies.
- Given 30%, basis = base + target bonus, offer base 2,000,000 + bonus 500,000 / When offer accepted / Then fee = 750,000 and the final instalment recomputes net of retainers paid.
Fields: fee_schedule_id, model, percent, fixed_amount, basis_flags, cap, floor, currency, vat_flag, withholding_flag. Edge: percent varies by salary band; offer revised after acceptance; comp in another currency; minimum fee.

**PB-009 Instalment triggers (Must).** Each instalment: trigger (engagement signed, calendar +N days, shortlist delivered, offer accepted, start date, abort), percent or amount, due days.
Why: partners now explain this to finance by hand.
- Given trigger "shortlist delivered" / When the shortlist is presented / Then an invoice-ready item (not an invoice) is created and finance is notified.
Fields: trigger_type, offset_days, percent, amount, state (Planned, Due, Invoiced, Paid, Waived). Edge: shortlist never delivered; calendar instalment due after cancellation; trigger event undone.

**PB-010 Invoice-ready items and export (Should).** Mihenk is not accounting. It creates due items with client legal data and exports CSV/Excel; invoice number and paid date return by import or manual entry.
Why: partners need receivables visibility, not a ledger.
- Given 3 due items / When finance exports / Then one row each with VKN, legal name, amount, currency, VAT flag, search reference.
Fields: due_item_id, state, invoice_no, invoice_date, paid_on, amount_paid. Edge: partial payment; credit note; foreign-currency payment. **[TAX]** KDV, withholding and e-Fatura vs e-Arsiv per client. [F] Sources say registered e-Fatura users are invoiced via e-Fatura, others e-Arsiv, and disagree on thresholds and 2026 dates.

**PB-011 Guarantee and replacement (Must).** Length, start basis, triggers, exclusions, client obligations; end-date countdown; replacement search linked to the original, fee 0.
Why: guarantee exposure is a liability partners forget until it bites. [F] length and voiding conditions vary by contract.
- Given a placed candidate leaves on day 90 of a 180-day guarantee / When "Guarantee claim" is raised / Then a linked replacement search is created with fee 0 and original terms.
- Given an overdue instalment / When a claim is raised / Then a warning shows the overdue amount.
Fields: guarantee_months, start_on, end_on, exclusions_text, claim_id, replacement_search_id, status. Edge: leaving for cause; start date moves; second replacement; employer reorganisation.

**PB-012 Expenses and abort fee (Could).** Expense cap and billable categories per agreement; abort fee rule.
Why: unbilled expenses and abandoned searches leak revenue.
- Given cap 50,000 / When expenses reach 90% / Then the partner is alerted.
Fields: expense_cap, billable_categories, abort_fee_rule. Edge: foreign-currency expenses; client pays directly.

**PB-013 Fee credit split (Should).** Originator / lead / researcher percentages sum to 100 per search; changes logged.
Why: compensation disputes are the top partner-meeting risk.
- Given split totals 110 / When saving / Then rejected.
Fields: credit_id, search_id, user_id, role, percent. Edge: consultant leaves mid-search; split changed after placement (partner only, reason).

## C. Client onboarding and record

**PB-014 Onboarding checklist and complete client record (Must).** "Active client" requires: legal name, tax office and VKN, address, group/parent, contacts with decision roles, signed agreement, conflict check, confidentiality level, team, e-Fatura status, language, kickoff date.
Why: critique #9: today the record holds name, sector, city.
- Given VKN missing / When marking Active / Then blocked with the list of missing items.
Fields: legal_name, trade_name, vkn, tax_office, mersis_no, address, einvoice_registered, group_id, billing_contact, language, confidentiality_level, owner_id. Edge: foreign client without VKN (country-aware tax ID); several billing entities; validate length only, not structure. **[TAX]** confirm identifier rules.

**PB-015 Contacts and decision roles (Must).** Per contact: role (sponsor, hiring manager, HR, finance, signatory), language, relationship owner, how known, last touch.
Why: the signer is rarely the briefer.
- Given a contact marked "left company" / When opening open searches or proposals / Then a "no live contact" banner shows.
Fields: person_id, account_id, decision_role[], language, owner_id, left_on. Edge: same person is also a candidate or ex-colleague of a candidate.

**PB-016 Client documents (Should).** Terms, NDAs, proposals, org charts; access follows account confidentiality.
Why: one place, one permission model.
- Given a RESTRICTED client / When a non-team consultant opens it / Then documents are hidden and the record shows "restricted".
Fields: document_id, type, version, uploaded_by, visibility. Edge: signed vs unsigned copies.

## D. Conflicts and off-limits

**PB-017 Group-level off-limits (Must).** Rules attach to a company or whole group (parent, subsidiaries, divisions, locations) with start/end, scope (all roles or named functions), source (contract, guarantee, partner decision), mode (warn/block).
Why: the platform's best differentiator; conflicts arise at group level.
- Given a rule on Group X, 24 months, covering subsidiary Y / When a researcher adds a Y employee to a longlist / Then a block shows rule, end date, owning partner.
- Given an expired rule / When adding / Then no block; rule shows as expired in history.
Fields: rule_id, company_id, scope, starts_on, ends_on, source, mode, created_by. Edge: client acquires a company mid-term (partner confirms extension); employee moved to a non-client entity; rule conflicts with a newer client. [F] vendors call off-limits standard but give no norm for length.

**PB-018 Pre-engagement conflict check (Must).** Before a proposal or search: is the company or its group a current client, in a guarantee period, inside an off-limits term; are our placed candidates there; is a competing search for a similar role running.
Why: conflicts found after a proposal embarrass the firm.
- Given an opportunity at a competitor of a live client / When saved / Then a conflict card lists hits; proceeding needs a partner decision with note.
Fields: conflict_check_id, hits[], decided_by, decision, decided_at. Edge: manual group link (audited); hits on confidential searches show "conflict exists, ask partner X" without detail.

**PB-019 Override with audit (Must).** Partner only, mandatory reason, logged, shown on the rule.
Why: overrides are rare and must be reviewable.
- Given a consultant / When overriding / Then denied. Given a partner / When overriding / Then reason is required and other partners are e-mailed.
Fields: override_id, rule_id, user_id, reason, at. Edge: reason too short; repeated override of the same rule.

**PB-020 Derived off-limits from live work (Should).** Active searches and guarantee periods create temporary rules on the client group, ending automatically.
Why: nobody remembers to enter these.
- Given a placement with a 12-month guarantee / When it starts / Then the placed person's employer is flagged for the guarantee period plus a configured tail.
Fields: derived flag, source_search_id. Edge: cancelled search; failed placement.

## E. Client reporting and relationship health

**PB-021 Client progress report (Must).** Per search, weekly or fortnightly: companies researched, contacted, conversations, longlist/shortlist counts, next steps; branded PDF, TR/EN; draft before send.
Why: clients judge the firm between shortlists.
- Given activity this week / When "Weekly report" is clicked / Then a pre-filled preview shows counts with confidential names masked.
Fields: report_id, search_id, period, counts{}, narrative, sent_at, recipients. Edge: quiet week (say so); candidate withdraws before send.

**PB-022 Relationship health (Should).** Per client: days since last touch, open proposals, active searches, last placement, overdue invoices, partner's manual rating; red/amber/green with visible reason.
Why: partners lose clients through silence.
- Given no touch for 60 days and no open search / When viewing home / Then the client is amber "silent" with last touch date.
Fields: last_touch_at, rag_status, rag_reason, rating_manual. Edge: one-search clients; deliberate dormancy (snooze with date).

**PB-023 Exposure by client (Must).** One list of all candidates presented to this client, date, outcome, rejection reason.
Why: "have we shown them this person?" comes up in every search (critique #12).
- Given a candidate presented 8 months ago / When added to a new search for that client / Then a warning shows date and outcome.
Fields: presentation records. Edge: presented to a subsidiary; presented blind.

## F. Firm KPIs and dashboards

**PB-024 Partner home screen (Must).** The ten numbers in section I, each clickable to the list behind it, with period and consultant filters.
Why: tiles that count things instead of money lose a partner in five seconds.
- Given 2 overdue instalments / When I click "Overdue receivables" / Then both items show with client and days overdue.
Fields: derived. Edge: empty state says "no data yet" rather than convincing zeros; currency conversion visible.

**PB-025 Consultant view (Should).** Per consultant: open searches, shortlist ageing, activity, credited fees; visible to partners; no leaderboard by default.
Why: [F] sources separate leadership trends from individual, actionable management data.
- Given a search without shortlist at day 35 / When the partner opens the view / Then it is flagged.
Fields: derived. Edge: shared searches.

**PB-026 Forecast (Could).** Next 6 months: scheduled instalments plus weighted opportunities.
Why: cash planning for a small firm.
- Given instalments and probabilities / When I open Forecast / Then monthly bars show contracted vs weighted.
Fields: derived. Edge: trigger dates slip; unknown placement date.

**PB-027 Metric definitions (Must).** Every number shows a one-line definition and period on hover.
Why: partners distrust numbers they cannot reconstruct. [F] external benchmarks (time to fill, revenue per consultant) vary widely; show the firm's own history instead.
- Given a metric / When hovering / Then definition and period show.
Fields: metric registry. Edge: definition changes (versioned).

## G. Team, permissions, confidentiality

**PB-028 Roles (Must).** Partner (all, overrides, fees, finance view); Consultant (own and team searches; firm-level fees only if granted); Researcher (longlist and notes; no fee or compensation fields); Finance (fees, instalments, invoices; no candidate notes).
Why: fee and compensation data are the most sensitive.
- Given a researcher / When opening a search / Then fee, compensation and agreement fields are not returned by the API (not merely hidden in the UI).
Fields: user_id, role, extra_grants[]. Edge: promotion; finance user also a consultant; leaver deactivated.

**PB-029 Search confidentiality levels (Must).** NORMAL; RESTRICTED (named team); CONFIDENTIAL (incumbent replacement): hidden from search, timeline, command bar, dashboards and exports for non-team users; partners see it only as an anonymous aggregate unless on the team.
Why: a leaked replacement search destroys client and incumbent.
- Given a CONFIDENTIAL search / When a non-team consultant searches the client name or role title / Then no result and no count leaks.
Fields: confidentiality_level, team_ids[], codename. Edge: codename in e-mails and reports; late team member sees history from start or from joining (setting); conflict check against confidential searches (PB-018).

**PB-030 Audit trail (Must).** Log views and exports of confidential searches, fee data and candidate compensation; partner-readable.
Why: proof when a client asks who saw what.
- Given an export of a CONFIDENTIAL search / When completed / Then user, time and fields are logged.
Fields: audit_event. Edge: bulk finance exports; failed access attempts.

**PB-031 Handover (Should).** Reassign accounts and searches of a leaver with a record.
Why: leavers take relationships; the record must stay with the firm.
- Given a leaver / When a partner reassigns / Then history stays attributed to the original user and the new owner is notified.
Fields: handover_id, from_user, to_user, scope, at. Edge: leaver is the only owner of a confidential search.

## H. Twelve questions a partner asks in a demo
1. Who can see my confidential replacement search? Show me as a junior.
2. Show an off-limits block on a subsidiary of a client, live.
3. Can I model our fee: thirds retainer, success top-up, base plus target bonus?
4. When does the second instalment fall due, and who is told?
5. What does the client receive, in which language? Show the weekly report.
6. Who owns this client relationship and when did we last speak?
7. How much fee is at risk if placements leave inside the guarantee?
8. What is my pipeline in money, weighted?
9. If a consultant leaves, what can they export, and is it logged?
10. Where does my data live, who is the processor, can I get everything out? **[LAWYER]**
11. How long from first meeting to a client that is ready to search?
12. What happens to my data and candidates if I stop paying?

## I. Ten numbers for the partner home screen
1. Fees invoiced this quarter vs target.
2. Receivables overdue and days outstanding.
3. Next 90 days: contracted instalments due plus weighted pipeline.
4. Open searches by stage, with count older than the firm's own norm.
5. Searches with no shortlist after N days (default 35, configurable).
6. Shortlists out this month and client feedback pending.
7. Placements in guarantee: fee at risk and nearest end date.
8. Opportunities: open count, weighted value, oldest without a next step.
9. Clients silent over 60 days.
10. Conflict or override events awaiting partner review.

## J. Turkey-specific items (flagged, not asserted)
- **KVKK and VERBIS [LAWYER].** Candidate data, consent and transfer to clients run through the consent engine. Whether the firm must register in VERBIS depends on current Board decisions. [F] A secondary source reports the exemption criteria were amended in September 2025 and clarified in December 2025; the post-amendment criteria were not confirmed. Do not hard-code.
- **Cross-border data [LAWYER].** Hosting outside Turkey, EU clients receiving Turkish candidate data and the reverse. Record data location and sub-processors per tenant.
- **Private employment bureau rules [LAWYER].** Whether retained executive search needs a licence is not established by our research (domain-workflows 1.8). Product rule regardless: no fee is ever charged to a candidate.
- **E-Fatura, e-Arsiv, KDV, withholding, foreign-currency invoicing [TAX].** Store e-Fatura status, VAT rate and withholding flag on the fee schedule; exchange-rate source and date per invoice. Sources disagree on thresholds and dates.
- **Stamp duty on contracts [TAX/LAWYER].** I assert nothing; add a yes/no/unknown "stamp duty applies" field on the agreement.
- **Language.** TR/EN proposals, terms and reports; governing language per agreement; diacritic-aware search.
- **Group structures.** Family holdings with many sectors are common [opinion]; the group tree in PB-017 matters more here than elsewhere.
- **Currency.** Fees in TRY, EUR or USD, sometimes indexed; keep contract currency and reporting currency.
- **Network culture.** Introductions drive BD; "introduced by" and "who knows whom" are first-class on targets (PB-003).

## K. Pilot cut
Must for the pilot: PB-001, 003, 004, 005, 006, 007, 008, 009, 011, 014, 015, 017, 018, 019, 021, 023, 024, 027, 028, 029, 030. Others after week 4 if the pilot confirms need.
Questions for the firm: does the guarantee start at placement or start date; standard off-limits term; are fee credits percentages; who may see compensation; is finance an external accountant (affects PB-010 and PB-028).
