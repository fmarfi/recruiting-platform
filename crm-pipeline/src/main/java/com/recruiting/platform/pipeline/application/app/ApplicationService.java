package com.recruiting.platform.pipeline.application.app;

import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.CandidateList;
import com.recruiting.platform.contracts.OutreachStatus;
import java.util.Comparator;
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
    private final ApplicationGuards guards;
    private final ApplicationRanking ranking;

    ApplicationService(ApplicationRepository repo, JobOrderRepository jobOrders, Refs refs, PersonDirectory people,
            ApplicationEventPublisher events, CurrentUser currentUser, ApplicationGuards guards,
            ApplicationRanking ranking) {
        this.ranking = ranking;
        this.guards = guards;
        this.currentUser = currentUser;
        this.repo = repo;
        this.jobOrders = jobOrders;
        this.refs = refs;
        this.people = people;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<Application> listForJobOrder(UUID jobOrderId, List<PipelineStage> stages, CandidateList list) {
        requireJob(jobOrderId);
        List<Application> all = repo.findByJobOrderId(jobOrderId);
        if (stages != null && !stages.isEmpty()) {
            all = all.stream().filter(a -> stages.contains(a.getStage())).toList();
        }
        if (list == null) {
            return all;
        }
        // ranked first by rank, then the unranked ones (PLACED on the shortlist) by stage change
        return all.stream().filter(a -> a.getStage().candidateList() == list)
                .sorted(Comparator.<Application, Integer>comparing(Application::getRank,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Application::getStageChangedAt))
                .toList();
    }

    /** Number of active people on the shortlist (for the SHORTLIST_LONG warning). */
    @Transactional(readOnly = true)
    public long shortlistSize(UUID jobOrderId) {
        return repo.findByJobOrderId(jobOrderId).stream()
                .filter(a -> a.getStage().isActive() && a.getStage().candidateList() == CandidateList.SHORTLIST)
                .count();
    }

    /** Sets the order of one list (keyboard reordering); 409 STATE_CONFLICT when the ids are stale. */
    @Transactional
    public List<Application> rank(UUID jobOrderId, CandidateList list, List<UUID> orderedIds) {
        requireJob(jobOrderId);
        if (new java.util.HashSet<>(orderedIds).size() != orderedIds.size()) {
            throw Transitions.invalid("applicationIds", "Ids must be unique.");
        }
        return ranking.reorder(jobOrderId, list, orderedIds);
    }

    /**
     * Changes rating, rationale or outreach status (contract 1.3.0). Keys absent from the body stay unchanged; on the
     * shortlist the rating and rationale may not be cleared or shortened below the entry rule (409 with errors).
     */
    @Transactional
    public Application update(UUID id, Map<String, Object> body) {
        Application a = get(id);
        boolean any = false;
        Short rating = a.getRating();
        String rationale = a.getRationale();
        if (body.containsKey("rating")) {
            any = true;
            Object v = body.get("rating");
            if (v == null) {
                rating = null;
            } else if (v instanceof Number n && n.doubleValue() == n.intValue() && n.intValue() >= 1 && n.intValue() <= 5) {
                rating = n.shortValue();
            } else {
                throw Transitions.invalid("rating", "rating must be an integer from 1 to 5.");
            }
        }
        if (body.containsKey("rationale")) {
            any = true;
            Object v = body.get("rationale");
            if (v == null) {
                rationale = null;
            } else if (v instanceof String str && str.length() <= 2000) {
                rationale = str.trim().isEmpty() ? null : str.trim();
            } else {
                throw Transitions.invalid("rationale", "rationale must be text of at most 2000 characters.");
            }
        }
        OutreachStatus outreach = a.getOutreachStatus();
        if (body.containsKey("outreachStatus")) {
            any = true;
            try {
                outreach = OutreachStatus.valueOf(String.valueOf(body.get("outreachStatus")));
            } catch (IllegalArgumentException e) {
                throw Transitions.invalid("outreachStatus", "Unknown outreach status.");
            }
        }
        if (!any) {
            throw Transitions.invalid("body", "At least one of rating, rationale or outreachStatus is required.");
        }
        Object version = body.get("version");
        if (version instanceof Number n) {
            Transitions.checkVersion(n.longValue(), a.getVersion());
        }
        if (a.getStage().candidateList() == CandidateList.SHORTLIST) {
            var issues = ApplicationGuards.shortlistIssues(rating, rationale);
            if (!issues.isEmpty()) {
                throw ApplicationGuards.shortlistFailed(a.getStage(), a.getStage(), issues);
            }
        }
        a.setRating(rating);
        a.setRationale(rationale);
        a.setOutreachStatus(outreach);
        return repo.saveAndFlush(a);
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
    public Application add(UUID jobOrderId, UUID personId, String offLimitsOverride) {
        String override = guards.checkOverride(offLimitsOverride);
        JobOrder job = requireJob(jobOrderId);
        if (!job.getStatus().isOpen()) {
            throw new ApiException(ErrorCode.TRANSITION_GUARD_FAILED,
                    "The job order is " + job.getStatus().labelEn() + " and accepts no new candidates.");
        }
        if (job.getArchivedAt() != null) {
            throw new ApiException(ErrorCode.ARCHIVED, "The job order is archived and accepts no new candidates.");
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
        guards.requireNotOffLimits(personId, job.getAccountId(), override != null);
        Application a = new Application();
        a.setOffLimitsOverrideReason(override);
        a.setJobOrderId(jobOrderId);
        a.setPersonId(personId);
        a.setStage(PipelineStage.INITIAL);
        a.setStageChangedAt(Instant.now());
        ranking.lock(jobOrderId);
        repo.saveAndFlush(a);
        ranking.normalise(jobOrderId, CandidateList.LONGLIST);
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
        ranking.lock(a.getJobOrderId());
        repo.delete(a);
        repo.flush();
        ranking.normalise(a.getJobOrderId(), CandidateList.LONGLIST);
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
