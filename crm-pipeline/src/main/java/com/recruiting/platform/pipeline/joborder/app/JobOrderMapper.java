package com.recruiting.platform.pipeline.joborder.app;

import com.recruiting.platform.contracts.PipelineStage;
import com.recruiting.platform.pipeline.domain.ApplicationRepository;
import com.recruiting.platform.pipeline.domain.JobOrder;
import com.recruiting.platform.pipeline.joborder.web.dto.JobOrderResponse;
import com.recruiting.platform.pipeline.shared.Refs;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Builds job order responses for a whole list with batched lookups (accounts, owners, stage counts). */
@Component
public class JobOrderMapper {

    private final Refs refs;
    private final ApplicationRepository applications;

    JobOrderMapper(Refs refs, ApplicationRepository applications) {
        this.refs = refs;
        this.applications = applications;
    }

    public JobOrderResponse toResponse(JobOrder j) {
        return toResponses(List.of(j)).get(0);
    }

    public List<JobOrderResponse> toResponses(List<JobOrder> jobs) {
        if (jobs.isEmpty()) {
            return List.of();
        }
        Refs.Lookup lookup = refs.load(jobs.stream().map(JobOrder::getAccountId).toList(), List.of(),
                jobs.stream().map(JobOrder::getOwnerId).toList());
        Map<UUID, Map<PipelineStage, Long>> counts = new HashMap<>();
        for (Object[] r : applications.countByStageForJobOrders(jobs.stream().map(JobOrder::getId).toList())) {
            counts.computeIfAbsent((UUID) r[0], k -> new EnumMap<>(PipelineStage.class))
                    .put((PipelineStage) r[1], ((Number) r[2]).longValue());
        }
        List<JobOrderResponse> out = new ArrayList<>();
        for (JobOrder j : jobs) {
            Map<PipelineStage, Long> sc = new EnumMap<>(PipelineStage.class);
            Map<PipelineStage, Long> have = counts.getOrDefault(j.getId(), Map.of());
            long active = 0;
            for (PipelineStage s : PipelineStage.values()) {
                long n = have.getOrDefault(s, 0L);
                sc.put(s, n);
                if (s.isActive()) {
                    active += n;
                }
            }
            LocalDate end = j.getClosedOn() != null ? j.getClosedOn() : LocalDate.now();
            out.add(new JobOrderResponse(j.getId(), lookup.account(j.getAccountId()), j.getTitle(), j.getLocation(),
                    j.getSeniority(), j.getDescription(), j.getFeeTerms(), j.getOpenings(), j.getStatus(),
                    j.getStatusChangedAt(), lookup.user(j.getOwnerId()), j.getOpenedOn(), j.getClosedOn(),
                    Math.max(0, ChronoUnit.DAYS.between(j.getOpenedOn(), end)), active, sc, j.getCreatedAt(),
                    j.getUpdatedAt(), j.getVersion()));
        }
        return out;
    }
}
