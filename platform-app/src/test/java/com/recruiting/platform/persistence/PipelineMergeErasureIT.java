package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.recruiting.platform.accounts.domain.Account;
import com.recruiting.platform.accounts.domain.AccountRepository;
import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.ActivityType;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.ExposureChannel;
import com.recruiting.platform.contracts.PersonAnonymised;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.PipelineStage;
import com.recruiting.platform.contracts.RetentionRenewed;
import com.recruiting.platform.contracts.RetentionStatus;
import com.recruiting.platform.pipeline.PipelinePersonApi;
import com.recruiting.platform.pipeline.domain.Activity;
import com.recruiting.platform.pipeline.domain.ActivityRepository;
import com.recruiting.platform.pipeline.domain.Application;
import com.recruiting.platform.pipeline.domain.ApplicationRepository;
import com.recruiting.platform.pipeline.domain.CandidateExposure;
import com.recruiting.platform.pipeline.domain.CandidateExposureRepository;
import com.recruiting.platform.pipeline.domain.JobOrder;
import com.recruiting.platform.pipeline.domain.JobOrderRepository;
import com.recruiting.platform.pipeline.domain.NextAction;
import com.recruiting.platform.pipeline.domain.NextActionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Slice 4 of Wave 2b: the pipeline merge step (PipelinePersonApi), the RetentionRenewed listener (closes the
 * "Renew consent" next action) and the scrub of off_limits_override_reason on anonymisation.
 */
class PipelineMergeErasureIT extends AbstractPersistenceIT {

    private static final String RENEW_TITLE = "Renew consent / review retention";

    @Autowired
    private AccountRepository accounts;
    @Autowired
    private PersonRepository people;
    @Autowired
    private JobOrderRepository jobOrders;
    @Autowired
    private ApplicationRepository applications;
    @Autowired
    private ActivityRepository activities;
    @Autowired
    private NextActionRepository nextActions;
    @Autowired
    private CandidateExposureRepository exposures;
    @Autowired
    private PipelinePersonApi mergeApi;
    @Autowired
    private ApplicationEventPublisher publisher;

    private UUID owner;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        owner = newUser().getId();
        Account a = new Account();
        a.setName("Merge Corp " + UUID.randomUUID());
        a.setSector("Retail");
        a.setOwnerId(owner);
        a.setStatus(AccountStatus.ACTIVE);
        a.setStatusChangedAt(Instant.now());
        a.setOpenedOn(LocalDate.now());
        a.setOpenJobCount(0);
        accountId = tx.execute(s -> accounts.save(a)).getId();
    }

    private UUID person() {
        Person p = new Person();
        p.setName("Person " + UUID.randomUUID());
        p.setType(PersonType.CANDIDATE);
        p.setOwnerId(owner);
        p.setConsentStatus(ConsentStatus.GIVEN);
        p.setRetentionStatus(RetentionStatus.ACTIVE);
        return tx.execute(s -> people.save(p)).getId();
    }

    private UUID job() {
        JobOrder j = new JobOrder();
        j.setAccountId(accountId);
        j.setTitle("Merge job " + UUID.randomUUID());
        j.setOwnerId(owner);
        return tx.execute(s -> jobOrders.save(j)).getId();
    }

    private Application application(UUID job, UUID person, PipelineStage stage, String override) {
        Application a = new Application();
        a.setJobOrderId(job);
        a.setPersonId(person);
        a.setStage(stage);
        a.setOffLimitsOverrideReason(override);
        return tx.execute(s -> applications.save(a));
    }

    private CandidateExposure exposure(UUID job, UUID person, UUID application) {
        CandidateExposure e = new CandidateExposure();
        e.setPersonId(person);
        e.setAccountId(accountId);
        e.setJobOrderId(job);
        e.setApplicationId(application);
        e.setChannel(ExposureChannel.PIPELINE);
        return tx.execute(s -> exposures.save(e));
    }

    @Test
    void mergeMovesEverythingAndKeepsTheSurvivorsApplicationOnTheSameJobOrder() {
        UUID survivor = person();
        UUID merged = person();
        UUID shared = job();
        UUID onlyMerged = job();
        Application survivorApp = application(shared, survivor, PipelineStage.SCREENED, null);
        Application mergedShared = application(shared, merged, PipelineStage.PRESENTED, null);
        Application mergedOnly = application(onlyMerged, merged, PipelineStage.OFFER, null);
        CandidateExposure e1 = exposure(shared, merged, mergedShared.getId());
        CandidateExposure e2 = exposure(onlyMerged, merged, mergedOnly.getId());
        Activity act = new Activity();
        act.setType(ActivityType.CALL);
        act.setNote("Call");
        act.setUserId(owner);
        act.setPersonId(merged);
        act.setAccountId(accountId);
        act.setOccurredAt(Instant.now());
        tx.executeWithoutResult(s -> activities.save(act));
        NextAction n = new NextAction();
        n.setTitle("Follow up");
        n.setDueOn(LocalDate.now().plusDays(2));
        n.setOwnerId(owner);
        n.setPersonId(merged);
        tx.executeWithoutResult(s -> nextActions.save(n));

        var result = tx.execute(s -> mergeApi.mergePerson(survivor, merged));

        assertThat(result.applications()).isEqualTo(1);
        assertThat(result.droppedApplications()).isEqualTo(1);
        assertThat(result.activities()).isEqualTo(1);
        assertThat(result.nextActions()).isEqualTo(1);
        assertThat(result.exposures()).isEqualTo(2);
        assertThat(applications.findByPersonId(merged)).isEmpty();
        assertThat(applications.findById(mergedShared.getId())).isEmpty();
        assertThat(applications.findById(survivorApp.getId())).get().extracting(Application::getStage)
                .isEqualTo(PipelineStage.SCREENED);
        assertThat(applications.findById(mergedOnly.getId())).get().extracting(Application::getPersonId)
                .isEqualTo(survivor);
        assertThat(activities.findByPersonIdOrderByOccurredAtDesc(survivor)).hasSize(1);
        assertThat(nextActions.findByPersonId(survivor)).hasSize(1);
        assertThat(exposures.findById(e1.getId())).get().satisfies(e -> {
            assertThat(e.getPersonId()).isEqualTo(survivor);
            assertThat(e.getApplicationId()).isEqualTo(survivorApp.getId());
        });
        assertThat(exposures.findById(e2.getId())).get().extracting(CandidateExposure::getPersonId)
                .isEqualTo(survivor);
        assertThat(exposures.findByPersonIdOrderByPresentedAtDesc(merged)).isEmpty();
    }

    @Test
    void retentionRenewedClosesTheRenewConsentAction() {
        UUID p = person();
        NextAction renew = new NextAction();
        renew.setTitle(RENEW_TITLE);
        renew.setDueOn(LocalDate.now().plusDays(10));
        renew.setOwnerId(owner);
        renew.setPersonId(p);
        UUID renewId = tx.execute(s -> nextActions.save(renew)).getId();
        NextAction other = new NextAction();
        other.setTitle("Something else");
        other.setDueOn(LocalDate.now().plusDays(10));
        other.setOwnerId(owner);
        other.setPersonId(p);
        UUID otherId = tx.execute(s -> nextActions.save(other)).getId();
        tx.executeWithoutResult(s -> publisher.publishEvent(new RetentionRenewed(UUID.randomUUID(), Instant.now(),
                null, p, LocalDate.now().plusYears(2))));
        await().atMost(java.time.Duration.ofSeconds(20)).untilAsserted(() -> {
            var done = nextActions.findById(renewId).orElseThrow();
            assertThat(done.isDone()).isTrue();
            assertThat(done.getDoneAt()).isNotNull();
        });
        assertThat(nextActions.findById(otherId).orElseThrow().isDone()).isFalse();
    }

    @Test
    void anonymisationScrubsTheOffLimitsOverrideReason() {
        UUID p = person();
        Application withReason = application(job(), p, PipelineStage.PRESENTED, "Partner agreed in writing");
        Application without = application(job(), p, PipelineStage.SCREENED, null);
        tx.executeWithoutResult(s -> publisher.publishEvent(new PersonAnonymised(UUID.randomUUID(), Instant.now(),
                null, p, false)));
        await().atMost(java.time.Duration.ofSeconds(20)).untilAsserted(() -> assertThat(
                applications.findById(withReason.getId()).orElseThrow().getOffLimitsOverrideReason())
                .isEqualTo("[removed]"));
        assertThat(applications.findById(without.getId()).orElseThrow().getOffLimitsOverrideReason()).isNull();
    }
}
