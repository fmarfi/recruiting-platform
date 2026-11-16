package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruiting.platform.contracts.ActivityType;
import com.recruiting.platform.contracts.CloseReason;
import com.recruiting.platform.contracts.ExposureChannel;
import com.recruiting.platform.contracts.PipelineStage;
import com.recruiting.platform.contracts.PlacementStatus;
import com.recruiting.platform.pipeline.domain.Activity;
import com.recruiting.platform.pipeline.domain.ActivityRepository;
import com.recruiting.platform.pipeline.domain.Application;
import com.recruiting.platform.pipeline.domain.ApplicationRepository;
import com.recruiting.platform.pipeline.domain.CandidateExposure;
import com.recruiting.platform.pipeline.domain.CandidateExposureRepository;
import com.recruiting.platform.pipeline.domain.JobOrder;
import com.recruiting.platform.pipeline.domain.JobOrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/** Contract 1.5.0 columns: JPA round trip, Envers rows, CHECKs, set-null and cascade behaviour. */
class Contract15RepositoryIT extends Wave2bSupport {

    @Autowired JobOrderRepository jobOrders;
    @Autowired ApplicationRepository applications;
    @Autowired ActivityRepository activities;
    @Autowired CandidateExposureRepository exposures;

    private UUID contact(UUID account, UUID person) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO contact (id, account_id, person_id) VALUES (?, ?, ?)", id, account, person);
        return id;
    }

    @Test
    void jobOrderContactIsStoredAudited_andClearedWhenTheContactIsDeleted() {
        UUID owner = newUser().getId();
        UUID account = account(owner);
        UUID contact = contact(account, person(owner));
        JobOrder created = new JobOrder();
        created.setAccountId(account);
        created.setTitle("CFO");
        created.setOwnerId(owner);
        UUID jo = tx.execute(s -> jobOrders.saveAndFlush(created)).getId();

        tx.executeWithoutResult(s -> jobOrders.saveAndFlush(setContact(jobOrders.findById(jo).orElseThrow(), contact)));
        assertThat(jobOrders.findById(jo).orElseThrow().getContactId()).isEqualTo(contact);
        assertThat(jobOrders.findByContactId(contact)).extracting(JobOrder::getId).containsExactly(jo);
        assertThat(count("SELECT count(*) FROM job_order_aud WHERE id = ? AND contact_id = ?", jo, contact)).isOne();

        jdbc.update("DELETE FROM contact WHERE id = ?", contact);
        assertThat(jobOrders.findById(jo).orElseThrow().getContactId()).isNull();
    }

    private static JobOrder setContact(JobOrder j, UUID contact) {
        j.setContactId(contact);
        return j;
    }

    @Test
    void moveLinkChecksUniqueSourceAndSetNull() {
        UUID owner = newUser().getId();
        UUID account = account(owner);
        UUID person = person(owner);
        UUID joA = jobOrder(account, owner);
        UUID joB = jobOrder(account, owner);
        UUID source = application(joA, person, "SCREENED");
        Application created = new Application();
        created.setJobOrderId(joB);
        created.setPersonId(person);
        created.setStage(PipelineStage.SOURCED);
        UUID target = tx.execute(s -> applications.saveAndFlush(created)).getId();

        // all four set through JPA
        tx.executeWithoutResult(s -> {
            Application t = applications.findById(target).orElseThrow();
            t.setMovedFromApplicationId(source);
            t.setMovedFromStage(PipelineStage.SCREENED);
            t.setMovedAt(Instant.now());
            t.setMovedBy(owner);
            applications.saveAndFlush(t);
        });
        assertThat(applications.findByMovedFromApplicationId(source)).get().extracting(Application::getId)
                .isEqualTo(target);
        assertThat(applications.existsByMovedFromApplicationId(source)).isTrue();
        assertThat(count("SELECT count(*) FROM application_aud WHERE id = ? AND moved_from_stage = 'SCREENED'", target))
                .isOne();

        // a source is moved at most once
        UUID joC = jobOrder(account, owner);
        UUID other = application(joC, person, "SOURCED");
        assertThatThrownBy(() -> jdbc.update("UPDATE application SET moved_from_application_id = ?, moved_from_stage = "
                + "'SCREENED', moved_at = now() WHERE id = ?", source, other))
                .isInstanceOf(DataIntegrityViolationException.class);
        // moved_at and moved_from_stage are set together; a link needs moved_at; no self link
        assertThatThrownBy(() -> jdbc.update("UPDATE application SET moved_at = now() WHERE id = ?", other))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE application SET moved_from_application_id = id, "
                + "moved_from_stage = 'SOURCED', moved_at = now() WHERE id = ?", other))
                .isInstanceOf(DataIntegrityViolationException.class);

        // the close reason accepts the move code
        jdbc.update("UPDATE application SET stage = 'REJECTED', rejection_reason = 'moved', "
                + "close_reason = 'MOVED_TO_OTHER_SEARCH' WHERE id = ?", source);
        assertThat(applications.findById(source).orElseThrow().getCloseReason())
                .isEqualTo(CloseReason.MOVED_TO_OTHER_SEARCH);

        // deleting the source clears the link only; the history columns stay
        jdbc.update("DELETE FROM application WHERE id = ?", source);
        Application kept = applications.findById(target).orElseThrow();
        assertThat(kept.getMovedFromApplicationId()).isNull();
        assertThat(kept.getMovedFromStage()).isEqualTo(PipelineStage.SCREENED);
        assertThat(kept.getMovedAt()).isNotNull();
        assertThat(kept.getMovedBy()).isEqualTo(owner);
    }

    @Test
    void cvPushActivityAndItsExposure() {
        UUID owner = newUser().getId();
        UUID account = account(owner);
        UUID person = person(owner);

        Activity a = new Activity();
        a.setType(ActivityType.EMAIL);
        a.setNote("CV sent");
        a.setOccurredAt(Instant.now());
        a.setAccountId(account);
        a.setPersonId(person);
        a.setUserId(owner);
        a.setCvPush(true);
        UUID activityId = tx.execute(s -> activities.saveAndFlush(a)).getId();
        assertThat(activities.findById(activityId).orElseThrow().isCvPush()).isTrue();
        assertThat(count("SELECT count(*) FROM activity_aud WHERE id = ? AND cv_push", activityId)).isOne();

        Instant at = Instant.parse("2026-11-10T09:00:00Z");
        CandidateExposure e = new CandidateExposure();
        e.setPersonId(person);
        e.setAccountId(account);
        e.setChannel(ExposureChannel.CV_PUSH);
        e.setActivityId(activityId);
        e.setPresentedAt(at);
        e.setPresentedBy(owner);
        tx.execute(s -> exposures.saveAndFlush(e));
        assertThat(exposures.findByActivityId(activityId)).isPresent();
        assertThat(exposures.countByChannelAndPresentedByAndPresentedAtGreaterThanEqualAndPresentedAtLessThan(
                ExposureChannel.CV_PUSH, owner, at, at.plusSeconds(1))).isOne();
        assertThat(exposures.countByChannelAndPresentedByAndPresentedAtGreaterThanEqualAndPresentedAtLessThan(
                ExposureChannel.PIPELINE, owner, at, at.plusSeconds(1))).isZero();

        Instant later = at.plusSeconds(3600);
        tx.executeWithoutResult(s -> exposures.updatePresentedAt(activityId, later));
        assertThat(exposures.findByActivityId(activityId).orElseThrow().getPresentedAt()).isEqualTo(later);

        // one exposure per activity
        assertThatThrownBy(() -> jdbc.update("INSERT INTO candidate_exposure (id, person_id, account_id, channel, "
                + "presented_at, activity_id) VALUES (?, ?, ?, 'CV_PUSH', now(), ?)",
                UUID.randomUUID(), person, account, activityId)).isInstanceOf(DataIntegrityViolationException.class);
        // CV_PUSH needs an activity and no job order; other channels the opposite
        assertThatThrownBy(() -> jdbc.update("INSERT INTO candidate_exposure (id, person_id, account_id, channel, "
                + "presented_at) VALUES (?, ?, ?, 'CV_PUSH', now())", UUID.randomUUID(), person, account))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO candidate_exposure (id, person_id, account_id, channel, "
                + "presented_at) VALUES (?, ?, ?, 'PIPELINE', now())", UUID.randomUUID(), person, account))
                .isInstanceOf(DataIntegrityViolationException.class);

        // cv_push CHECK: needs person and account, no job order, not a NOTE
        assertThatThrownBy(() -> jdbc.update("INSERT INTO activity (id, type, occurred_at, account_id, cv_push) "
                + "VALUES (?, 'EMAIL', now(), ?, true)", UUID.randomUUID(), account))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO activity (id, type, occurred_at, account_id, person_id, "
                + "cv_push) VALUES (?, 'NOTE', now(), ?, ?, true)", UUID.randomUUID(), account, person))
                .isInstanceOf(DataIntegrityViolationException.class);

        // deleting the activity deletes its exposure
        jdbc.update("DELETE FROM activity WHERE id = ?", activityId);
        assertThat(exposures.findByActivityId(activityId)).isEmpty();
    }

    @Test
    void filledOpeningsCountsPlacedWithoutPlacementOrWithActiveOrGuaranteeEnded() {
        UUID owner = newUser().getId();
        UUID account = account(owner);
        UUID jo = jobOrder(account, owner);
        application(jo, person(owner), "PLACED");
        application(jo, person(owner), "OFFER");
        assertThat(applications.countFilledOpenings(jo,
                List.of(PlacementStatus.ACTIVE, PlacementStatus.GUARANTEE_ENDED))).isOne();
    }
}
