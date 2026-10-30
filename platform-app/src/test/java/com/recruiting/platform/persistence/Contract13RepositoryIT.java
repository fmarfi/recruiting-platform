package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruiting.platform.accounts.domain.Account;
import com.recruiting.platform.accounts.domain.AccountRepository;
import com.recruiting.platform.accounts.domain.ImportJobRecord;
import com.recruiting.platform.accounts.domain.ImportJobRecordRepository;
import com.recruiting.platform.accounts.domain.TagDefinition;
import com.recruiting.platform.accounts.domain.TagDefinitionRepository;
import com.recruiting.platform.contracts.AgreementKind;
import com.recruiting.platform.contracts.ConflictDecision;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.contracts.ExportStatus;
import com.recruiting.platform.contracts.FeeModel;
import com.recruiting.platform.contracts.FeeTrigger;
import com.recruiting.platform.contracts.InterviewType;
import com.recruiting.platform.contracts.ListEntity;
import com.recruiting.platform.contracts.Recommendation;
import com.recruiting.platform.contracts.ReportKind;
import com.recruiting.platform.contracts.SearchTeamRole;
import com.recruiting.platform.contracts.TargetTier;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.pipeline.domain.Agreement;
import com.recruiting.platform.pipeline.domain.AgreementRepository;
import com.recruiting.platform.pipeline.domain.ActivityAttendee;
import com.recruiting.platform.pipeline.domain.ActivityAttendeeRepository;
import com.recruiting.platform.pipeline.domain.CandidateEvaluation;
import com.recruiting.platform.pipeline.domain.CandidateEvaluationRepository;
import com.recruiting.platform.pipeline.domain.ClientReport;
import com.recruiting.platform.pipeline.domain.ClientReportRepository;
import com.recruiting.platform.pipeline.domain.ConflictCheck;
import com.recruiting.platform.pipeline.domain.ConflictCheckRepository;
import com.recruiting.platform.pipeline.domain.FeeScheduleItem;
import com.recruiting.platform.pipeline.domain.FeeScheduleItemRepository;
import com.recruiting.platform.pipeline.domain.JobOrderTeamMember;
import com.recruiting.platform.pipeline.domain.JobOrderTeamMemberRepository;
import com.recruiting.platform.pipeline.domain.Placement;
import com.recruiting.platform.pipeline.domain.PlacementRepository;
import com.recruiting.platform.pipeline.domain.SearchBrief;
import com.recruiting.platform.pipeline.domain.SearchBriefRepository;
import com.recruiting.platform.pipeline.domain.TargetCompany;
import com.recruiting.platform.pipeline.domain.TargetCompanyRepository;
import com.recruiting.platform.security.domain.ExportJob;
import com.recruiting.platform.security.domain.ExportJobRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Contract 1.3.0 entities round-trip through JPA, with Envers rows, encryption and generated columns. */
class Contract13RepositoryIT extends Wave2bSupport {

    @Autowired AccountRepository accounts;
    @Autowired TagDefinitionRepository tags;
    @Autowired ImportJobRecordRepository importRecords;
    @Autowired SearchBriefRepository briefs;
    @Autowired ActivityAttendeeRepository attendees;
    @Autowired JobOrderTeamMemberRepository team;
    @Autowired TargetCompanyRepository targets;
    @Autowired CandidateEvaluationRepository evaluations;
    @Autowired ClientReportRepository reports;
    @Autowired AgreementRepository agreements;
    @Autowired FeeScheduleItemRepository feeItems;
    @Autowired PlacementRepository placements;
    @Autowired ConflictCheckRepository conflicts;
    @Autowired ExportJobRepository exports;

    @Test
    void accountRolesAliasesAndTheGeneratedNormalisedName() {
        UUID owner = newUser().getId();
        Account a = new Account();
        a.setName("Örnek Gıda Sanayi ve Ticaret A.Ş. " + UUID.randomUUID().toString().replace("-", "x"));
        a.setSector("Food");
        a.setOwnerId(owner);
        a.setClient(true);
        a.setAliases(List.of("Ornek Brand Ltd."));
        a.setBillingEmails(List.of("billing@example.test"));
        UUID id = tx.execute(s -> accounts.saveAndFlush(a)).getId();

        Account loaded = accounts.findById(id).orElseThrow();
        assertThat(loaded.getNormalizedName()).startsWith("ornek gida ");
        assertThat(loaded.isClient()).isTrue();
        assertThat(loaded.getAliases()).containsExactly("Ornek Brand Ltd.");
        assertThat(loaded.getEinvoiceStatus().name()).isEqualTo("UNKNOWN");
        assertThat(count("SELECT count(*) FROM account_aud WHERE id = ? AND is_client", id)).isOne();
        assertThat(count("SELECT count(*) FROM account WHERE normalize_org_names(aliases) @> ARRAY['ornek brand']"
                + " AND id = ?", id)).isOne();
    }

    @Test
    void tagDefinitionAndImportRecordRoundTrip() {
        TagDefinition t = new TagDefinition();
        t.setName(unique("pool"));
        t.setTalentPool(true);
        TagDefinition saved = tx.execute(s -> tags.save(t));
        assertThat(tags.findByName(saved.getName())).get().extracting(TagDefinition::isTalentPool).isEqualTo(true);
        assertThat(count("SELECT count(*) FROM tag_definition_aud WHERE id = ?", saved.getId())).isOne();

        UUID user = newUser().getId();
        UUID importId = UUID.randomUUID();
        jdbc.update("INSERT INTO import_job (id, kind, status) VALUES (?, 'PEOPLE', 'COMPLETED')", importId);
        UUID entity = UUID.randomUUID();
        importRecords.save(new ImportJobRecord(importId, EntityType.PERSON, entity));
        assertThat(importRecords.findByIdImportId(importId)).hasSize(1);
        assertThat(user).isNotNull();
    }

    @Test
    void searchBriefBandIsEncryptedAtRestAndAuditedAsCiphertext() {
        UUID owner = newUser().getId();
        UUID job = jobOrder(account(owner), owner);
        SearchBrief b = new SearchBrief();
        b.setJobOrderId(job);
        b.setPurpose("Replace the CFO");
        b.setCompBandJson("{\"min\":100,\"max\":200,\"currency\":\"TRY\",\"period\":\"MONTHLY\"}");
        b.setTeamSize((short) 12);
        b.setKickoffOn(LocalDate.now());
        tx.execute(s -> briefs.save(b));

        byte[] stored = jdbc.queryForObject("SELECT comp_band_enc FROM search_brief WHERE job_order_id = ?",
                byte[].class, job);
        assertThat(stored).isNotNull();
        assertThat(new String(stored)).doesNotContain("TRY");
        assertThat(briefs.findById(job).orElseThrow().getCompBandJson()).contains("\"max\":200");
        assertThat(count("SELECT count(*) FROM search_brief_aud WHERE job_order_id = ? AND comp_band_enc IS NOT NULL",
                job)).isOne();
    }

    @Test
    void attendeesTeamAndTargetsAreAudited() {
        UUID owner = newUser().getId();
        UUID account = account(owner);
        UUID job = jobOrder(account, owner);
        UUID person = person(owner);
        UUID activity = UUID.randomUUID();
        jdbc.update("INSERT INTO activity (id, type, note, occurred_at, account_id, user_id) "
                + "VALUES (?, 'MEETING', 'Client meeting', now(), ?, ?)", activity, account, owner);

        tx.execute(s -> attendees.save(new ActivityAttendee(activity, person)));
        assertThat(attendees.findByIdActivityId(activity)).hasSize(1);
        assertThat(count("SELECT count(*) FROM activity_attendee_aud WHERE activity_id = ? AND person_id = ?",
                activity, person)).isOne();

        JobOrderTeamMember m = new JobOrderTeamMember(job, owner);
        m.setRole(SearchTeamRole.LEAD);
        m.setAddedAt(Instant.now());
        tx.execute(s -> team.save(m));
        assertThat(team.findByIdJobOrderId(job)).hasSize(1);

        TargetCompany tc = new TargetCompany();
        tc.setJobOrderId(job);
        tc.setCompanyName("Fictional Rival " + UUID.randomUUID());
        tc.setTier(TargetTier.A);
        tc.setOwnerId(owner);
        TargetCompany savedTarget = tx.execute(s -> targets.save(tc));
        assertThat(targets.countByJobOrderId(job)).isOne();
        assertThat(count("SELECT count(*) FROM target_company_aud WHERE id = ?", savedTarget.getId())).isOne();
    }

    @Test
    void evaluationArraysAndClientReportPersonLookup() {
        UUID owner = newUser().getId();
        UUID person = person(owner);
        CandidateEvaluation e = new CandidateEvaluation();
        e.setPersonId(person);
        e.setEvaluatorId(owner);
        e.setEvaluatedOn(LocalDate.now());
        e.setInterviewType(InterviewType.VIDEO);
        e.setRecommendation(Recommendation.SHORTLIST);
        e.setMotivationTags(List.of("GROWTH", "PAY"));
        e.setKeyAchievements(List.of("Led a merger"));
        e.setScores(List.of(Map.of("criterionId", UUID.randomUUID().toString(), "score", 4)));
        tx.execute(s -> evaluations.save(e));
        CandidateEvaluation loaded = evaluations.findByPersonIdOrderByEvaluatedOnDesc(person).get(0);
        assertThat(loaded.getMotivationTags()).containsExactly("GROWTH", "PAY");
        assertThat(loaded.getReportBlocks()).containsExactly("SUMMARY", "ACHIEVEMENTS", "MOTIVATION", "CONCERNS");

        UUID job = jobOrder(account(owner), owner);
        ClientReport r = new ClientReport();
        r.setJobOrderId(job);
        r.setKind(ReportKind.SHORTLIST);
        r.setVersion(1);
        r.setLanguage(Language.EN);
        r.setBlind(true);
        r.setPersonIds(List.of(person));
        r.setStorageKey("report/" + job + "/" + UUID.randomUUID() + ".pdf");
        r.setSizeBytes(1234L);
        r.setGeneratedBy(owner);
        r.setGeneratedAt(Instant.now());
        reports.save(r);
        assertThat(reports.findByPersonId(person)).hasSize(1);
        assertThat(reports.findByPersonId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void agreementFeeScheduleAndPlacementKeepTheirNumbers() {
        UUID owner = newUser().getId();
        UUID account = account(owner);
        UUID job = jobOrder(account, owner);
        Agreement ag = new Agreement();
        ag.setAccountId(account);
        ag.setBillingAccountId(account);
        ag.setKind(AgreementKind.TERMS_OF_BUSINESS);
        ag.setFeeModel(FeeModel.PERCENT_FIRST_YEAR_CASH);
        ag.setFeePercent(new BigDecimal("25.00"));
        ag.setFeeBasis(List.of("BASE", "TARGET_BONUS"));
        ag.setCurrency("TRY");
        UUID agId = tx.execute(s -> agreements.save(ag)).getId();
        Agreement back = agreements.findById(agId).orElseThrow();
        assertThat(back.getGuaranteeMonths()).isEqualTo((short) 6);
        assertThat(back.getFeeBasis()).containsExactly("BASE", "TARGET_BONUS");

        FeeScheduleItem item = new FeeScheduleItem();
        item.setAgreementId(agId);
        item.setSeq((short) 1);
        item.setLabel("On signing");
        item.setTrigger(FeeTrigger.SIGNING);
        item.setPercent(new BigDecimal("33.33"));
        tx.execute(s -> feeItems.save(item));
        assertThat(feeItems.findByAgreementIdOrderBySeqAsc(agId)).hasSize(1);
        assertThat(feeItems.findByAgreementIdOrderBySeqAsc(agId).get(0).isVatApplies()).isTrue();

        Placement p = new Placement();
        p.setJobOrderId(job);
        p.setAccountId(account);
        p.setAgreementId(agId);
        p.setStartDate(LocalDate.now());
        p.setFeeAmount(new BigDecimal("1000.00"));
        p.setFeeCurrency("TRY");
        p.setGuaranteeEndOn(LocalDate.now().plusMonths(6));
        p.setCompensationJson("{\"base\":1}");
        UUID pid = tx.execute(s -> placements.save(p)).getId();
        assertThat(placements.findById(pid).orElseThrow().getCompensationJson()).isEqualTo("{\"base\":1}");
        assertThat(count("SELECT count(*) FROM placement_aud WHERE id = ?", pid)).isOne();
    }

    @Test
    void conflictCheckAndExportJobRoundTrip() {
        UUID owner = newUser().getId();
        ConflictCheck c = new ConflictCheck();
        c.setCompanyName("Fictional Prospect");
        c.setDecision(ConflictDecision.CLEAR);
        c.setCheckedBy(owner);
        c.setCheckedAt(Instant.now());
        c.setHits(List.of(Map.of("kind", "OFF_LIMITS", "refType", "ACCOUNT")));
        tx.execute(s -> conflicts.save(c));
        assertThat(count("SELECT count(*) FROM conflict_check WHERE checked_by = ?", owner)).isOne();

        ExportJob j = new ExportJob();
        j.setEntity(ListEntity.PEOPLE);
        j.setStatus(ExportStatus.PENDING_APPROVAL);
        j.setFilters(Map.of("city", "istanbul"));
        j.setFilterKeys(List.of("city", "q"));
        j.setColumns(List.of("name", "city"));
        j.setPurpose("Quarterly pool review");
        j.setRowCount(42);
        j.setRequestedBy(owner);
        j.setRequestedAt(Instant.now());
        tx.execute(s -> exports.save(j));
        assertThat(exports.findByStatusOrderByRequestedAtAsc(ExportStatus.PENDING_APPROVAL)).isNotEmpty();
    }
}
