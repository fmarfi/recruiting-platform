package com.recruiting.platform.pipeline.joborder.app;

import com.recruiting.platform.contracts.JobOrderStatus;
import com.recruiting.platform.contracts.JobOrderStatusChanged;
import com.recruiting.platform.contracts.PipelineStage;
import com.recruiting.platform.pipeline.domain.ApplicationRepository;
import com.recruiting.platform.pipeline.domain.JobOrder;
import com.recruiting.platform.pipeline.domain.JobOrderRepository;
import com.recruiting.platform.pipeline.shared.Actor;
import com.recruiting.platform.pipeline.shared.Transitions;
import com.recruiting.platform.pipeline.shared.Uuids;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Job order status moves (A-06): transition table, reason, manager-only reopen, FILLED guard, event. */
@Service
public class JobOrderStatusService {

    private final JobOrderRepository repo;
    private final ApplicationRepository applications;
    private final ApplicationEventPublisher events;

    JobOrderStatusService(JobOrderRepository repo, ApplicationRepository applications,
            ApplicationEventPublisher events) {
        this.repo = repo;
        this.applications = applications;
        this.events = events;
    }

    @Transactional
    public JobOrder change(UUID id, JobOrderStatus to, String reason, Long version, Actor actor) {
        JobOrder j = repo.findById(id).orElseThrow(() -> Transitions.notFound("Job order"));
        Transitions.checkVersion(version, j.getVersion());
        JobOrderStatus from = j.getStatus();
        String cleanReason = Transitions.check(from, to, reason, actor.elevated());
        if (to == JobOrderStatus.FILLED && !applications.existsByJobOrderIdAndStage(id, PipelineStage.PLACED)) {
            throw Transitions.guardFailed(from, to,
                    "A job order can be filled only when a candidate is in stage Placed.");
        }
        j.setStatus(to);
        j.setStatusChangedAt(Instant.now());
        j.setClosedOn(to.isOpen() ? null : LocalDate.now());
        repo.saveAndFlush(j);
        events.publishEvent(new JobOrderStatusChanged(Uuids.v7(), Instant.now(), actor.id(), j.getId(),
                j.getAccountId(), from, to, cleanReason));
        return j;
    }
}
