package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruiting.platform.contracts.ConsentRequestPurpose;
import com.recruiting.platform.contracts.ExposureChannel;
import com.recruiting.platform.pipeline.domain.Application;
import com.recruiting.platform.pipeline.domain.ApplicationRepository;
import com.recruiting.platform.pipeline.domain.CandidateExposure;
import com.recruiting.platform.pipeline.domain.CandidateExposureRepository;
import com.recruiting.platform.privacy.domain.ConsentRequest;
import com.recruiting.platform.privacy.domain.ConsentRequestRepository;
import com.recruiting.platform.privacy.domain.PersonPrivacy;
import com.recruiting.platform.privacy.domain.PersonPrivacyRepository;
import com.recruiting.platform.privacy.domain.RetentionSettings;
import com.recruiting.platform.privacy.domain.RetentionSettingsRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;

/** Wave 2b tables owned by pipeline and privacy (contract 1.2.0) and the 1.1.1 bulk audit rule. */
class Wave2bPipelinePrivacyRepositoryIT extends Wave2bSupport {

    @Autowired
    private CandidateExposureRepository exposures;

    @Autowired
    private ApplicationRepository applications;

    @Autowired
    private ConsentRequestRepository consentRequests;

    @Autowired
    private PersonPrivacyRepository privacy;

    @Autowired
    private RetentionSettingsRepository settings;

    // ---------------------------------------------------------------- 1.1.1 bulk audit

    @Test
    void bulkAuditHoldsNoFreeText() {
        UUID actor = newUser().getId();
        assertThat(count("SELECT count(*) FROM information_schema.columns "
                + "WHERE table_name = 'bulk_action_item' AND column_name = 'message'")).isZero();
        String insert = "INSERT INTO bulk_action (id, entity, action, params, actor_id, started_at, finished_at, "
                + "requested_count, ok_count, skipped_count, failed_count) "
                + "VALUES (?, 'PEOPLE', 'CHANGE_STATUS', ?::jsonb, ?, now(), now(), 1, 1, 0, 0)";
        jdbc.update(insert, UUID.randomUUID(), "{\"status\":\"CLIENT\"}", actor);
        assertThatThrownBy(() -> jdbc.update(insert, UUID.randomUUID(), "{\"status\":\"CLIENT\",\"reason\":\"x\"}", actor))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insert, UUID.randomUUID(), "{\"title\":\"Call Mr Fictional\"}", actor))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------------------------------------------------------------- candidate_exposure

    private CandidateExposure exposure(UUID personId, UUID accountId, UUID jobOrderId, UUID applicationId,
            ExposureChannel channel) {
        CandidateExposure e = new CandidateExposure();
        e.setPersonId(personId);
        e.setAccountId(accountId);
        e.setJobOrderId(jobOrderId);
        e.setApplicationId(applicationId);
        e.setChannel(channel);
        return tx.execute(s -> exposures.saveAndFlush(e));
    }

    @Test
    void exposureIsIdempotentPerApplicationForThePipelineChannel() {
        UUID owner = newUser().getId();
        UUID personId = person(owner);
        UUID account = account(owner);
        UUID job = jobOrder(account, owner);
        UUID app = application(job, personId, "PRESENTED");

        CandidateExposure first = exposure(personId, account, job, app, ExposureChannel.PIPELINE);
        assertThat(exposures.findByPersonIdOrderByPresentedAtDesc(personId)).extracting(CandidateExposure::getId)
                .containsExactly(first.getId());
        assertThat(exposures.existsByPersonIdAndAccountId(personId, account)).isTrue();
        assertThat(exposures.existsByPersonIdAndAccountId(personId, UUID.randomUUID())).isFalse();
        assertThat(exposures.existsByApplicationIdAndChannel(app, ExposureChannel.PIPELINE)).isTrue();

        assertThatThrownBy(() -> exposure(personId, account, job, app, ExposureChannel.PIPELINE))
                .isInstanceOf(DataIntegrityViolationException.class);
        // other channels may repeat (report and share link are separate events)
        exposure(personId, account, job, app, ExposureChannel.REPORT);
        exposure(personId, account, job, null, ExposureChannel.SHARE_LINK);
        exposure(personId, account, job, null, ExposureChannel.SHARE_LINK);
        assertThatThrownBy(() -> jdbc.update("UPDATE candidate_exposure SET channel = 'TELEPATHY' WHERE person_id = ?",
                personId)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void exposureSurvivesApplicationDeleteWithNullAndCascadesWithThePerson() {
        UUID owner = newUser().getId();
        UUID personId = person(owner);
        UUID account = account(owner);
        UUID job = jobOrder(account, owner);
        UUID app = application(job, personId, "OFFER");
        CandidateExposure e = exposure(personId, account, job, app, ExposureChannel.PIPELINE);

        jdbc.update("DELETE FROM application WHERE id = ?", app);
        assertThat(exposures.findById(e.getId()).orElseThrow().getApplicationId()).isNull();

        tx.executeWithoutResult(s -> people.deleteById(personId));
        assertThat(exposures.findById(e.getId())).isEmpty();
    }

    @Test
    void backfillCreatesOneExposurePerClientFacingApplicationAndIsIdempotent() throws IOException {
        UUID owner = newUser().getId();
        UUID account = account(owner);
        UUID job = jobOrder(account, owner);
        UUID viaStage = application(job, person(owner), "OFFER");
        UUID viaHistory = application(job, person(owner), "REJECTED");
        UUID viaHistoryOnlyLater = application(job, person(owner), "REJECTED");
        UUID viaOfferThenRejected = application(job, person(owner), "REJECTED");
        UUID never = application(job, person(owner), "SCREENED");
        UUID sourced = application(job, person(owner), "SOURCED");

        long early = Instant.parse("2026-03-01T10:00:00Z").toEpochMilli();
        long late = Instant.parse("2026-04-01T10:00:00Z").toEpochMilli();
        auditRow(viaStage, "PRESENTED", early);
        auditRow(viaStage, "CLIENT_INTERVIEW", late);
        auditRow(viaHistory, "SCREENED", early);
        auditRow(viaHistory, "PRESENTED", late);
        auditRow(viaHistoryOnlyLater, "PRESENTED", early);
        auditRow(never, "SCREENED", early);
        auditRow(viaOfferThenRejected, "OFFER", late);

        String sql = new String(new ClassPathResource("db/migration/V202610260008__pipeline_candidate_exposure_backfill.sql")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        jdbc.execute(sql);
        jdbc.execute(sql);

        assertThat(count("SELECT count(*) FROM candidate_exposure WHERE application_id = ? AND channel = 'PIPELINE'",
                viaStage)).isOne();
        assertThat(count("SELECT count(*) FROM candidate_exposure WHERE application_id = ?", viaHistory)).isOne();
        assertThat(count("SELECT count(*) FROM candidate_exposure WHERE application_id = ?", viaHistoryOnlyLater)).isOne();
        assertThat(count("SELECT count(*) FROM candidate_exposure WHERE application_id = ?", viaOfferThenRejected)).isOne();
        assertThat(count("SELECT count(*) FROM candidate_exposure WHERE application_id IN (?, ?)", never, sourced))
                .isZero();
        // presented_at is the earliest client-facing revision
        assertThat(jdbc.queryForObject("SELECT presented_at FROM candidate_exposure WHERE application_id = ?",
                java.sql.Timestamp.class, viaStage).toInstant()).isEqualTo(Instant.ofEpochMilli(early));
        assertThat(jdbc.queryForObject("SELECT account_id FROM candidate_exposure WHERE application_id = ?", UUID.class,
                viaStage)).isEqualTo(account);
        assertThat(jdbc.queryForObject("SELECT presented_by FROM candidate_exposure WHERE application_id = ?",
                UUID.class, viaStage)).isNull();
    }

    private void auditRow(UUID applicationId, String stage, long epochMillis) {
        long rev = jdbc.queryForObject("SELECT nextval('revinfo_seq')", Long.class);
        jdbc.update("INSERT INTO revinfo (rev, revtstmp) VALUES (?, ?)", rev, epochMillis);
        jdbc.update("INSERT INTO application_aud (id, rev, revtype, stage) VALUES (?, ?, 1, ?)",
                applicationId, rev, stage);
    }

    // ---------------------------------------------------------------- application.off_limits_override_reason

    @Test
    void applicationOverrideReasonRoundTripAndAudit() {
        UUID owner = newUser().getId();
        UUID personId = person(owner);
        UUID job = jobOrder(account(owner), owner);
        Application a = new Application();
        a.setJobOrderId(job);
        a.setPersonId(personId);
        a.setOffLimitsOverrideReason("Client agreed in writing, reviewed by manager");
        UUID id = tx.execute(s -> applications.save(a)).getId();
        assertThat(applications.findById(id).orElseThrow().getOffLimitsOverrideReason()).startsWith("Client agreed");
        assertThat(count("SELECT count(*) FROM application_aud WHERE id = ? AND off_limits_override_reason IS NOT NULL",
                id)).isOne();
    }

    // ---------------------------------------------------------------- consent_request additions

    private ConsentRequest request(UUID personId, UUID notice, ConsentRequestPurpose purpose, Short daysBefore,
            LocalDate forRetainUntil) {
        ConsentRequest r = new ConsentRequest();
        r.setPersonId(personId);
        r.setNoticeId(notice);
        r.setLanguage(com.recruiting.platform.contracts.Language.TR);
        r.setTokenHash(UUID.randomUUID().toString().replace("-", "").repeat(2));
        r.setDeliveryStatus(com.recruiting.platform.contracts.EmailDeliveryStatus.QUEUED);
        r.setExpiresAt(Instant.now().plusSeconds(30 * 86400L));
        r.setPurpose(purpose);
        r.setReminderDaysBefore(daysBefore);
        r.setReminderForRetainUntil(forRetainUntil);
        return tx.execute(s -> consentRequests.saveAndFlush(r));
    }

    private UUID notice() {
        UUID notice = UUID.randomUUID();
        jdbc.update("INSERT INTO privacy_notice (id, version, language, notice_text, consent_text, effective_from) "
                + "VALUES (?, ?, 'TR', 'notice', 'consent', current_date)", notice, unique("v"));
        return notice;
    }

    @Test
    void consentRequestReminderIsUniquePerPersonStepAndPeriod() {
        UUID owner = newUser().getId();
        UUID personId = person(owner);
        UUID notice = notice();
        LocalDate until = LocalDate.now().plusDays(60);

        ConsentRequest initial = request(personId, notice, ConsentRequestPurpose.INITIAL, null, null);
        request(personId, notice, ConsentRequestPurpose.INITIAL, null, null); // resends are unlimited
        assertThat(consentRequests.findById(initial.getId()).orElseThrow().getPurpose())
                .isEqualTo(ConsentRequestPurpose.INITIAL);
        assertThat(consentRequests.findById(initial.getId()).orElseThrow().getOpenedAt()).isNull();

        request(personId, notice, ConsentRequestPurpose.RENEWAL, (short) 60, until);
        request(personId, notice, ConsentRequestPurpose.RENEWAL, (short) 14, until); // other step
        request(personId, notice, ConsentRequestPurpose.RENEWAL, (short) 60, until.plusYears(2)); // next period
        request(person(owner), notice, ConsentRequestPurpose.RENEWAL, (short) 60, until); // other person
        assertThatThrownBy(() -> request(personId, notice, ConsentRequestPurpose.RENEWAL, (short) 60, until))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE consent_request SET purpose = 'NUDGE' WHERE id = ?",
                initial.getId())).isInstanceOf(DataIntegrityViolationException.class);

        initial.setOpenedAt(Instant.now());
        tx.execute(s -> consentRequests.saveAndFlush(initial));
        assertThat(consentRequests.findById(initial.getId()).orElseThrow().getOpenedAt()).isNotNull();
    }

    // ---------------------------------------------------------------- person_privacy and retention_settings

    @Test
    void personPrivacyLastConsentAtRoundTripAndAudit() {
        UUID owner = newUser().getId();
        UUID personId = person(owner);
        PersonPrivacy pp = new PersonPrivacy(personId);
        pp.setRetainUntil(LocalDate.now().plusYears(2));
        Instant at = Instant.parse("2026-05-05T08:00:00Z");
        pp.setLastConsentAt(at);
        tx.execute(s -> privacy.save(pp));
        assertThat(privacy.findById(personId).orElseThrow().getLastConsentAt()).isEqualTo(at);
        assertThat(count("SELECT count(*) FROM person_privacy_aud WHERE person_id = ? AND last_consent_at IS NOT NULL",
                personId)).isOne();
    }

    @Test
    void retentionSettingsNewColumnsAndChecks() {
        RetentionSettings s = settings.findById((short) 1).orElseThrow();
        assertThat(s.isAutoSendOnCvUpload()).isTrue();
        assertThat(s.getRenewalReminderDays()).containsExactly((short) 60, (short) 14);
        assertThat(s.getBasis()).isNotNull();

        String update = "UPDATE retention_settings SET renewal_reminder_days = ?::smallint[] WHERE id = 1";
        assertThatThrownBy(() -> jdbc.update(update, "{0}")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(update, "{366}")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(update, "{1,2,3,4,5,6}")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE retention_settings SET basis = 'FROM_NEVER' WHERE id = 1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        // the new basis is accepted; put the current value back afterwards
        String current = jdbc.queryForObject("SELECT basis FROM retention_settings WHERE id = 1", String.class);
        jdbc.update("UPDATE retention_settings SET basis = 'FROM_LAST_CONSENT' WHERE id = 1");
        jdbc.update("UPDATE retention_settings SET basis = ? WHERE id = 1", current);
        assertThat(count("SELECT count(*) FROM information_schema.columns WHERE table_name = 'retention_settings_aud' "
                + "AND column_name IN ('auto_send_on_cv_upload', 'renewal_reminder_days')")).isEqualTo(2);
    }
}
