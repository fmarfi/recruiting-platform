package com.recruiting.platform.pipeline.application.app;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ApplicationStageChanged;
import com.recruiting.platform.contracts.CandidateList;
import com.recruiting.platform.contracts.CloseReason;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.OutreachStatus;
import com.recruiting.platform.pipeline.application.web.dto.StageChangeRequest;
import java.util.List;
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
    private final ApplicationRanking ranking;
    private final ApplicationEventPublisher events;

    ApplicationStageService(ApplicationRepository repo, JobOrderRepository jobOrders,
            CandidateExposureRepository exposures, ApplicationGuards guards, ApplicationEventPublisher events,
            CurrentUser currentUser, ApplicationRanking ranking) {
        this.ranking = ranking;
        this.currentUser = currentUser;
        this.repo = repo;
        this.jobOrders = jobOrders;
        this.exposures = exposures;
        this.guards = guards;
        this.events = events;
    }

    @Transactional
    public Application change(UUID id, StageChangeRequest req) {
        PipelineStage to = req.stage();
        String reason = req.reason();
        String offLimitsOverride = req.offLimitsOverrideReason();
        Long version = req.version();
        if (req.closeReason() != null && to != PipelineStage.REJECTED) {
            throw Transitions.invalid("closeReason", "closeReason is only allowed when moving to REJECTED.");
        }
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
        if (job.getArchivedAt() != null) {
            throw new ApiException(ErrorCode.ARCHIVED, "The job order is archived and accepts no stage moves.");
        }
        boolean firstExposure = false;
        if (to.sharedWithClient()) {
            guards.requireShareable(a.getPersonId());
            if (from.candidateList() != CandidateList.SHORTLIST) {
                requireShortlistEntry(from, to, a, req);
            }
            firstExposure = !exposures.existsByApplicationIdAndChannel(a.getId(), ExposureChannel.PIPELINE);
            if (firstExposure) {
                boolean overridden = a.getOffLimitsOverrideReason() != null || override != null;
                guards.requireNotOffLimits(a.getPersonId(), job.getAccountId(), overridden);
                if (a.getOffLimitsOverrideReason() == null && override != null) {
                    a.setOffLimitsOverrideReason(override);
                }
            }
        }
        if (req.rating() != null) {
            a.setRating(req.rating().shortValue());
        }
        if (req.rationale() != null) {
            a.setRationale(req.rationale().trim());
        }
        boolean listChanged = from.candidateList() != to.candidateList() || !to.isActive();
        if (listChanged) {
            ranking.lock(job.getId());
            a.setRank(null);
        }
        a.setStage(to);
        a.setStageChangedAt(Instant.now());
        a.setRejectionReason(to == PipelineStage.REJECTED ? clean : null);
        a.setCloseReason(to == PipelineStage.REJECTED
                ? (req.closeReason() == null ? CloseReason.OTHER : req.closeReason()) : null);
        if (to == PipelineStage.SCREENED) {
            a.setOutreachStatus(OutreachStatus.SCREENED);
        }
        repo.saveAndFlush(a);
        if (listChanged) {
            ranking.normalise(job.getId(), from.candidateList());
            ranking.normalise(job.getId(), to.candidateList());
        }
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

    /** Shortlist entry (ADR 0013, SX-014): rating 1-5 and a rationale of at least 20 characters, else 409. */
    private static void requireShortlistEntry(PipelineStage from, PipelineStage to, Application a,
            StageChangeRequest req) {
        Short rating = req.rating() != null ? Short.valueOf(req.rating().shortValue()) : a.getRating();
        String rationale = req.rationale() != null ? req.rationale() : a.getRationale();
        List<ApplicationGuards.FieldIssue> issues = ApplicationGuards.shortlistIssues(rating, rationale);
        if (!issues.isEmpty()) {
            throw ApplicationGuards.shortlistFailed(from, to, issues);
        }
    }
}
