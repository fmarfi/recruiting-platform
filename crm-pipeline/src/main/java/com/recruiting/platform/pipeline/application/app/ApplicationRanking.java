package com.recruiting.platform.pipeline.application.app;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.CandidateList;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.pipeline.domain.Application;
import com.recruiting.platform.pipeline.domain.ApplicationRepository;
import com.recruiting.platform.pipeline.domain.JobOrderRepository;
import com.recruiting.platform.pipeline.shared.Transitions;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Rank bookkeeping of the longlist and the shortlist (R65, contract 1.3.0): ranks are 1..n, gapless per job order and
 * list, among the active applications only. Every change locks the job order row first so reorders serialise.
 */
@Component
public class ApplicationRanking {

    private final ApplicationRepository repo;
    private final JobOrderRepository jobOrders;

    ApplicationRanking(ApplicationRepository repo, JobOrderRepository jobOrders) {
        this.repo = repo;
        this.jobOrders = jobOrders;
    }

    /** Takes the row lock of the job order (held until the transaction ends). */
    public void lock(UUID jobOrderId) {
        jobOrders.lockById(jobOrderId).orElseThrow(() -> Transitions.notFound("Job order"));
    }

    /**
     * Renumbers the active applications of one list 1..n. Applications without a rank (just appended, or created before
     * ranks existed) go after the ranked ones, oldest stage change first.
     */
    public void normalise(UUID jobOrderId, CandidateList list) {
        if (list == null) {
            return;
        }
        List<Application> onList = onList(jobOrderId, list);
        int n = 1;
        for (Application a : onList) {
            if (a.getRank() == null || a.getRank() != n) {
                a.setRank(n);
            }
            n++;
        }
        repo.saveAllAndFlush(onList);
    }

    /** Active applications of a list in rank order (unranked last). */
    public List<Application> onList(UUID jobOrderId, CandidateList list) {
        return repo.findByJobOrderId(jobOrderId).stream()
                .filter(a -> a.getStage().isActive() && a.getStage().candidateList() == list)
                .sorted(Comparator.<Application, Integer>comparing(Application::getRank,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Application::getStageChangedAt))
                .toList();
    }

    /** Sets the order of one list; the ids must be exactly the active applications of the list (else 409). */
    public List<Application> reorder(UUID jobOrderId, CandidateList list, List<UUID> orderedIds) {
        lock(jobOrderId);
        List<Application> current = onList(jobOrderId, list);
        Set<UUID> have = new HashSet<>();
        current.forEach(a -> have.add(a.getId()));
        if (orderedIds.size() != have.size() || !have.equals(new HashSet<>(orderedIds))) {
            throw new ApiException(ErrorCode.STATE_CONFLICT,
                    "The list changed since you loaded it. Reload and retry.");
        }
        int n = 1;
        for (UUID id : orderedIds) {
            Application a = current.stream().filter(x -> x.getId().equals(id)).findFirst().orElseThrow();
            a.setRank(n++);
        }
        repo.saveAllAndFlush(current);
        return onList(jobOrderId, list);
    }
}
