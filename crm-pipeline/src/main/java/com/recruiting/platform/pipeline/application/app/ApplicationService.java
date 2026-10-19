package com.recruiting.platform.pipeline.application.app;

import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ApplicationCreated;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.PipelineStage;
import com.recruiting.platform.pipeline.domain.Application;
import com.recruiting.platform.pipeline.domain.ApplicationRepository;
import com.recruiting.platform.pipeline.domain.JobOrder;
import com.recruiting.platform.pipeline.domain.JobOrderRepository;
import com.recruiting.platform.security.CurrentUser;
import com.recruiting.platform.pipeline.shared.Refs;
import com.recruiting.platform.pipeline.shared.Transitions;
import com.recruiting.platform.pipeline.shared.Uuids;
import com.recruiting.platform.pipeline.shared.ref.PersonDirectory;
import com.recruiting.platform.pipeline.shared.ref.PersonInfo;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Candidates on a job order: board list, add, read, remove, suggestions (A-07). */
@Service
public class ApplicationService {

    private final CurrentUser currentUser;
    private final ApplicationRepository repo;
    private final JobOrderRepository jobOrders;
    private final Refs refs;
    private final PersonDirectory people;
    private final ApplicationEventPublisher events;

    ApplicationService(ApplicationRepository repo, JobOrderRepository jobOrders, Refs refs, PersonDirectory people,
            ApplicationEventPublisher events, CurrentUser currentUser) {
        this.currentUser = currentUser;
        this.repo = repo;
        this.jobOrders = jobOrders;
        this.refs = refs;
        this.people = people;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<Application> listForJobOrder(UUID jobOrderId, List<PipelineStage> stages) {
        requireJob(jobOrderId);
        List<Application> all = repo.findByJobOrderId(jobOrderId);
        if (stages == null || stages.isEmpty()) {
            return all;
        }
        return all.stream().filter(a -> stages.contains(a.getStage())).toList();
    }

    @Transactional(readOnly = true)
    public List<Application> listForPerson(UUID personId) {
        refs.requirePerson(personId);
        return repo.findByPersonIdOrderByStageChangedAtDesc(personId);
    }

    @Transactional(readOnly = true)
    public Application get(UUID id) {
        return repo.findById(id).orElseThrow(() -> Transitions.notFound("Application"));
    }

    @Transactional
    public Application add(UUID jobOrderId, UUID personId) {
        JobOrder job = requireJob(jobOrderId);
        if (!job.getStatus().isOpen()) {
            throw new ApiException(ErrorCode.TRANSITION_GUARD_FAILED,
                    "The job order is " + job.getStatus().labelEn() + " and accepts no new candidates.");
        }
        if (refs.requireAccount(job.getAccountId()).status() == AccountStatus.CLOSED) {
            throw new ApiException(ErrorCode.TRANSITION_GUARD_FAILED, "The account is closed.");
        }
        refs.requirePerson(personId);
        var existing = repo.findByJobOrderIdAndPersonId(jobOrderId, personId);
        if (existing.isPresent()) {
            throw new ApiException(ErrorCode.DUPLICATE, "This person is already on the job order.",
                    Map.of("existingId", existing.get().getId().toString()));
        }
        Application a = new Application();
        a.setJobOrderId(jobOrderId);
        a.setPersonId(personId);
        a.setStage(PipelineStage.INITIAL);
        a.setStageChangedAt(Instant.now());
        repo.saveAndFlush(a);
        events.publishEvent(new ApplicationCreated(Uuids.v7(), Instant.now(), currentUser.id(), a.getId(), jobOrderId,
                job.getAccountId(), personId, a.getStage()));
        return a;
    }

    @Transactional
    public void remove(UUID id) {
        Application a = get(id);
        if (a.getStage() != PipelineStage.SOURCED) {
            throw new ApiException(ErrorCode.STATE_CONFLICT,
                    "Only a candidate in stage Sourced can be removed; reject the candidate instead.");
        }
        repo.delete(a);
    }

    @Transactional(readOnly = true)
    public Page<PersonInfo> suggestions(UUID jobOrderId, String q, int page, int size) {
        requireJob(jobOrderId);
        Set<UUID> exclude = repo.findByJobOrderId(jobOrderId).stream().map(Application::getPersonId)
                .collect(Collectors.toSet());
        return people.suggestions(q, exclude, PageRequest.of(page, size));
    }

    private JobOrder requireJob(UUID id) {
        return jobOrders.findById(id).orElseThrow(() -> Transitions.notFound("Job order"));
    }
}
