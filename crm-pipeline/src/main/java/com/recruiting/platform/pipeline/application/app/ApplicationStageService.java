package com.recruiting.platform.pipeline.application.app;

import com.recruiting.platform.contracts.ApplicationStageChanged;
import com.recruiting.platform.contracts.ExposureChannel;
import com.recruiting.platform.contracts.PipelineStage;
import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.pipeline.domain.Application;
import com.recruiting.platform.pipeline.domain.ApplicationRepository;
import com.recruiting.platform.pipeline.domain.CandidateExposure;
import com.recruiting.platform.pipeline.domain.CandidateExposureRepository;
import com.recruiting.platform.pipeline.domain.JobOrder;
import com.recruiting.platform.pipeline.domain.JobOrderRepository;
import com.recruiting.platform.pipeline.shared.Transitions;
import com.recruiting.platform.pipeline.shared.Uuids;
import com.recruiting.platform.security.CurrentUser;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stage moves of a candidate (A-07): transition table, reason, closed job order guard, then (since 1.2.0, ADR 0011)
 * the guards of a move into a client-facing stage, event. Guard order: role of the override, transition table and
 * reason, closed job order, consent (no override), off-limits (first client-facing move only, manager override),
 * exposure write. Every move into PRESENTED, CLIENT_INTERVIEW, OFFER or PLACED runs the consent guard because forward
 * moves may skip PRESENTED.
 */
@Service
public class ApplicationStageService {

    private final CurrentUser currentUser;
    private final ApplicationRepository repo;
    private final JobOrderRepository jobOrders;
    private final CandidateExposureRepository exposures;
    private final ApplicationGuards guards;
    private final ApplicationEventPublisher events;

    ApplicationStageService(ApplicationRepository repo, JobOrderRepository jobOrders,
            CandidateExposureRepository exposures, ApplicationGuards guards, ApplicationEventPublisher events,
            CurrentUser currentUser) {
        this.currentUser = currentUser;
        this.repo = repo;
        this.jobOrders = jobOrders;
        this.exposures = exposures;
        this.guards = guards;
        this.events = events;
    }

    @Transactional
    public Application change(UUID id, PipelineStage to, String reason, Long version, String offLimitsOverride) {
        Application a = repo.findById(id).orElseThrow(() -> Transitions.notFound("Application"));
        Transitions.checkVersion(version, a.getVersion());
        String override = guards.checkOverride(offLimitsOverride);
        PipelineStage from = a.getStage();
        String clean = Transitions.check(from, to, reason, currentUser.hasAtLeast(Role.MANAGER));
        JobOrder job = jobOrders.findById(a.getJobOrderId()).orElseThrow(() -> Transitions.notFound("Job order"));
        if (!job.getStatus().isOpen()) {
            throw Transitions.guardFailed(from, to,
                    "The job order is " + job.getStatus().labelEn() + " and accepts no stage moves.");
        }
        boolean firstExposure = false;
        if (to.sharedWithClient()) {
            guards.requireShareable(a.getPersonId());
            firstExposure = !exposures.existsByApplicationIdAndChannel(a.getId(), ExposureChannel.PIPELINE);
            if (firstExposure) {
                boolean overridden = a.getOffLimitsOverrideReason() != null || override != null;
                guards.requireNotOffLimits(a.getPersonId(), job.getAccountId(), overridden);
                if (a.getOffLimitsOverrideReason() == null && override != null) {
                    a.setOffLimitsOverrideReason(override);
                }
            }
        }
        a.setStage(to);
        a.setStageChangedAt(Instant.now());
        a.setRejectionReason(to == PipelineStage.REJECTED ? clean : null);
        repo.saveAndFlush(a);
        if (firstExposure) {
            CandidateExposure e = new CandidateExposure();
            e.setPersonId(a.getPersonId());
            e.setAccountId(job.getAccountId());
            e.setJobOrderId(job.getId());
            e.setApplicationId(a.getId());
            e.setChannel(ExposureChannel.PIPELINE);
            e.setPresentedBy(currentUser.id());
            exposures.save(e);
        }
        events.publishEvent(new ApplicationStageChanged(Uuids.v7(), Instant.now(), currentUser.id(), a.getId(),
                a.getJobOrderId(), job.getAccountId(), a.getPersonId(), from, to, clean));
        return a;
    }
}
