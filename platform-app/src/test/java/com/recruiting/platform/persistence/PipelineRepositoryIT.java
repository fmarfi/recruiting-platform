package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruiting.platform.accounts.domain.Account;
import com.recruiting.platform.accounts.domain.AccountRepository;
import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.ActivityType;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.contracts.JobOrderStatus;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.PipelineStage;
import com.recruiting.platform.contracts.RetentionStatus;
import com.recruiting.platform.contracts.TimelineKind;
import com.recruiting.platform.pipeline.domain.Activity;
import com.recruiting.platform.pipeline.domain.ActivityRepository;
import com.recruiting.platform.pipeline.domain.Application;
import com.recruiting.platform.pipeline.domain.ApplicationRepository;
import com.recruiting.platform.pipeline.domain.JobOrder;
import com.recruiting.platform.pipeline.domain.JobOrderRepository;
import com.recruiting.platform.pipeline.domain.TimelineEntry;
import com.recruiting.platform.pipeline.domain.TimelineEntryRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

class PipelineRepositoryIT extends AbstractPersistenceIT {

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
    private TimelineEntryRepository timeline;

    private Account account;
    private Person person;
    private UUID owner;

    private void givenAccountAndPerson() {
        owner = newUser().getId();
        account = new Account();
        account.setName("Pipeline Corp " + UUID.randomUUID());
        account.setSector("Retail");
        account.setOwnerId(owner);
        account.setStatus(AccountStatus.ACTIVE);
        account.setStatusChangedAt(Instant.now());
        account.setOpenedOn(LocalDate.now());
        account.setOpenJobCount(0);
        person = new Person();
        person.setName("Candidate " + UUID.randomUUID());
        person.setType(PersonType.CANDIDATE);
        person.setOwnerId(owner);
        person.setConsentStatus(ConsentStatus.NOT_SENT);
        person.setRetentionStatus(RetentionStatus.ACTIVE);
        tx.executeWithoutResult(s -> {
            accounts.save(account);
            people.save(person);
        });
    }

    private JobOrder newJobOrder(String title) {
        JobOrder j = new JobOrder();
        j.setAccountId(account.getId());
        j.setTitle(title);
        j.setOpenings(1);
        j.setStatus(JobOrderStatus.OPEN);
        j.setStatusChangedAt(Instant.now());
        j.setOwnerId(owner);
        j.setOpenedOn(LocalDate.now());
        return tx.execute(s -> jobOrders.save(j));
    }

    @Test
    void jobOrderTitleSearchIsAccentInsensitive() {
        givenAccountAndPerson();
        String tag = UUID.randomUUID().toString().substring(0, 8);
        newJobOrder("Genel Müdür " + tag);
        assertThat(jobOrders.searchByTitle("genel mudur " + tag, 10)).hasSize(1);
        assertThat(jobOrders.findByAccountId(account.getId(), PageRequest.of(0, 5)).getTotalElements()).isEqualTo(1);
    }

    @Test
    void applicationRulesAndStageCounts() {
        givenAccountAndPerson();
        JobOrder job = newJobOrder("CFO " + UUID.randomUUID());

        Application app = new Application();
        app.setJobOrderId(job.getId());
        app.setPersonId(person.getId());
        app.setStage(PipelineStage.SOURCED);
        app.setStageChangedAt(Instant.now());
        tx.executeWithoutResult(s -> applications.save(app));

        assertThat(applications.existsByJobOrderIdAndPersonId(job.getId(), person.getId())).isTrue();
        List<Object[]> counts = applications.countByStage(job.getId());
        assertThat(counts).hasSize(1);
        assertThat(counts.get(0)[0]).isEqualTo(PipelineStage.SOURCED);
        assertThat(counts.get(0)[1]).isEqualTo(1L);

        // a rejection needs a reason (CHECK), and one person can be on a job order only once (UNIQUE)
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            Application a = applications.findById(app.getId()).orElseThrow();
            a.setStage(PipelineStage.REJECTED);
            a.setRejectionReason(null);
            applications.flush();
        })).isInstanceOf(DataIntegrityViolationException.class);
        Application dup = new Application();
        dup.setJobOrderId(job.getId());
        dup.setPersonId(person.getId());
        dup.setStage(PipelineStage.SOURCED);
        dup.setStageChangedAt(Instant.now());
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> applications.save(dup)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void activityNeedsAnAccountOrAPerson() {
        givenAccountAndPerson();
        Activity bad = new Activity();
        bad.setType(ActivityType.NOTE);
        bad.setNote("orphan");
        bad.setOccurredAt(Instant.now());
        bad.setUserId(owner);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> activities.save(bad)))
                .isInstanceOf(DataIntegrityViolationException.class);

        Activity ok = new Activity();
        ok.setType(ActivityType.CALL);
        ok.setNote("called");
        ok.setOccurredAt(Instant.now());
        ok.setAccountId(account.getId());
        ok.setUserId(owner);
        tx.executeWithoutResult(s -> activities.save(ok));
        assertThat(activities.findByAccountIdOrderByOccurredAtDesc(account.getId(), PageRequest.of(0, 5))
                        .getContent())
                .hasSize(1);
    }

    @Test
    void timelineIsIdempotentBySourceEvent() {
        givenAccountAndPerson();
        UUID eventId = UUID.randomUUID();
        TimelineEntry t = new TimelineEntry();
        t.setAccountId(account.getId());
        t.setOccurredAt(Instant.now());
        t.setKind(TimelineKind.ACCOUNT_STATUS);
        t.setEntityType(EntityType.ACCOUNT);
        t.setEntityId(account.getId());
        t.setFromStatus("LEAD");
        t.setToStatus("ACTIVE");
        t.setSourceEventId(eventId);
        tx.executeWithoutResult(s -> timeline.save(t));
        assertThat(timeline.existsBySourceEventId(eventId)).isTrue();

        TimelineEntry again = new TimelineEntry();
        again.setAccountId(account.getId());
        again.setOccurredAt(Instant.now());
        again.setKind(TimelineKind.ACCOUNT_STATUS);
        again.setEntityType(EntityType.ACCOUNT);
        again.setEntityId(account.getId());
        again.setSourceEventId(eventId);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> timeline.save(again)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
