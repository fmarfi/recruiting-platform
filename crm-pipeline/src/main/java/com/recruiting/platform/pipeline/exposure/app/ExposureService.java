package com.recruiting.platform.pipeline.exposure.app;

import com.recruiting.platform.contracts.PipelineWarningCode;
import com.recruiting.platform.pipeline.application.web.dto.PipelineWarning;
import com.recruiting.platform.pipeline.domain.Application;
import com.recruiting.platform.pipeline.domain.ApplicationRepository;
import com.recruiting.platform.pipeline.domain.CandidateExposure;
import com.recruiting.platform.pipeline.domain.CandidateExposureRepository;
import com.recruiting.platform.pipeline.domain.JobOrder;
import com.recruiting.platform.pipeline.domain.JobOrderRepository;
import com.recruiting.platform.pipeline.exposure.web.dto.ExposureResponse;
import com.recruiting.platform.pipeline.shared.Refs;
import com.recruiting.platform.pipeline.shared.dto.JobOrderRef;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Exposure history of a person (B-05) and the non-blocking warnings shown when adding or presenting (ADR 0011). */
@Service
public class ExposureService {

    static final String PREVIOUSLY_PRESENTED = "Already presented to this account for another job order.";
    static final String ACTIVE_ELSEWHERE = "Already in another open process.";

    private final CandidateExposureRepository exposures;
    private final ApplicationRepository applications;
    private final JobOrderRepository jobOrders;
    private final Refs refs;

    ExposureService(CandidateExposureRepository exposures, ApplicationRepository applications,
            JobOrderRepository jobOrders, Refs refs) {
        this.exposures = exposures;
        this.applications = applications;
        this.jobOrders = jobOrders;
        this.refs = refs;
    }

    /**
     * Presentations of the person, newest first.
     *
     * @param personId  the person (404 when unknown)
     * @param accountId optional filter
     * @return the entries
     */
    @Transactional(readOnly = true)
    public List<ExposureResponse> list(UUID personId, UUID accountId) {
        refs.requirePerson(personId);
        List<CandidateExposure> rows = exposures.findByPersonIdOrderByPresentedAtDesc(personId).stream()
                .filter(e -> accountId == null || accountId.equals(e.getAccountId())).toList();
        return toResponses(rows);
    }

    /**
     * Warnings for an application that was just added or moved into a client-facing stage.
     *
     * @param app the application
     * @return the warnings, {@code null} when there are none (so the field is left out)
     */
    @Transactional(readOnly = true)
    public List<PipelineWarning> warnings(Application app) {
        JobOrder job = jobOrders.findById(app.getJobOrderId()).orElseThrow();
        List<PipelineWarning> out = new ArrayList<>();
        List<CandidateExposure> earlier = exposures.findByPersonIdOrderByPresentedAtDesc(app.getPersonId()).stream()
                .filter(e -> job.getAccountId().equals(e.getAccountId()) && !app.getId().equals(e.getApplicationId()))
                .toList();
        if (!earlier.isEmpty()) {
            out.add(new PipelineWarning(PipelineWarningCode.PREVIOUSLY_PRESENTED_TO_ACCOUNT, PREVIOUSLY_PRESENTED,
                    toResponses(earlier), null));
        }
        List<Application> active = applications.findByPersonId(app.getPersonId()).stream()
                .filter(o -> !o.getId().equals(app.getId()) && o.getStage().isActive()).toList();
        if (!active.isEmpty()) {
            Map<UUID, JobOrder> jobs = jobsById(active.stream().map(Application::getJobOrderId).toList());
            Refs.Lookup l = refs.load(jobs.values().stream().map(JobOrder::getAccountId).toList(), List.of(),
                    List.of());
            out.add(new PipelineWarning(PipelineWarningCode.ACTIVE_IN_OTHER_PROCESS, ACTIVE_ELSEWHERE, null,
                    active.stream().map(o -> {
                        JobOrder j = jobs.get(o.getJobOrderId());
                        return new PipelineWarning.OtherProcess(o.getId(),
                                new JobOrderRef(j.getId(), j.getTitle(), j.getStatus()), l.account(j.getAccountId()),
                                o.getStage());
                    }).toList()));
        }
        return out.isEmpty() ? null : out;
    }

    private Map<UUID, JobOrder> jobsById(Collection<UUID> ids) {
        return jobOrders.findAllById(ids.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(JobOrder::getId, Function.identity()));
    }

    private List<ExposureResponse> toResponses(List<CandidateExposure> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, JobOrder> jobs = jobsById(rows.stream().map(CandidateExposure::getJobOrderId).toList());
        Map<UUID, Application> apps = applications
                .findAllById(rows.stream().map(CandidateExposure::getApplicationId)
                        .filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Application::getId, Function.identity()));
        Refs.Lookup l = refs.load(rows.stream().map(CandidateExposure::getAccountId).toList(), List.of(),
                rows.stream().map(CandidateExposure::getPresentedBy).toList());
        return rows.stream().map(e -> {
            JobOrder j = jobs.get(e.getJobOrderId());
            Application a = e.getApplicationId() == null ? null : apps.get(e.getApplicationId());
            return new ExposureResponse(e.getId(), l.account(e.getAccountId()),
                    j == null ? null : new JobOrderRef(j.getId(), j.getTitle(), j.getStatus()), e.getApplicationId(),
                    e.getChannel(), e.getPresentedAt(), l.user(e.getPresentedBy()), a == null ? null : a.getStage());
        }).toList();
    }
}
