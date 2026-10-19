package com.recruiting.platform.pipeline.application.app;

import com.recruiting.platform.contracts.ApplicationStageChanged;
import com.recruiting.platform.contracts.PipelineStage;
import com.recruiting.platform.pipeline.domain.Application;
import com.recruiting.platform.pipeline.domain.ApplicationRepository;
import com.recruiting.platform.pipeline.domain.JobOrder;
import com.recruiting.platform.pipeline.domain.JobOrderRepository;
import com.recruiting.platform.pipeline.shared.Actor;
import com.recruiting.platform.pipeline.shared.Transitions;
import com.recruiting.platform.pipeline.shared.Uuids;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stage moves of a candidate (A-07): transition table, reason, closed job order guard, event. */
@Service
public class ApplicationStageService {

    private final ApplicationRepository repo;
    private final JobOrderRepository jobOrders;
    private final ApplicationEventPublisher events;

    ApplicationStageService(ApplicationRepository repo, JobOrderRepository jobOrders,
            ApplicationEventPublisher events) {
        this.repo = repo;
        this.jobOrders = jobOrders;
        this.events = events;
    }

    @Transactional
    public Application change(UUID id, PipelineStage to, String reason, Long version, Actor actor) {
        Application a = repo.findById(id).orElseThrow(() -> Transitions.notFound("Application"));
        Transitions.checkVersion(version, a.getVersion());
        PipelineStage from = a.getStage();
        String clean = Transitions.check(from, to, reason, actor.elevated());
        JobOrder job = jobOrders.findById(a.getJobOrderId()).orElseThrow(() -> Transitions.notFound("Job order"));
        if (!job.getStatus().isOpen()) {
            throw Transitions.guardFailed(from, to,
                    "The job order is " + job.getStatus().labelEn() + " and accepts no stage moves.");
        }
        a.setStage(to);
        a.setStageChangedAt(Instant.now());
        a.setRejectionReason(to == PipelineStage.REJECTED ? clean : null);
        repo.saveAndFlush(a);
        events.publishEvent(new ApplicationStageChanged(Uuids.v7(), Instant.now(), actor.id(), a.getId(),
                a.getJobOrderId(), job.getAccountId(), a.getPersonId(), from, to, clean));
        return a;
    }
}
