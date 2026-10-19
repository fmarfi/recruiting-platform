package com.recruiting.platform.pipeline.application.app;

import com.recruiting.platform.pipeline.application.web.dto.ApplicationResponse;
import com.recruiting.platform.pipeline.domain.Application;
import com.recruiting.platform.pipeline.domain.JobOrder;
import com.recruiting.platform.pipeline.domain.JobOrderRepository;
import com.recruiting.platform.pipeline.shared.Refs;
import com.recruiting.platform.pipeline.shared.dto.JobOrderRef;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Builds application responses for a list with batched lookups. */
@Component
public class ApplicationMapper {

    private final Refs refs;
    private final JobOrderRepository jobOrders;

    ApplicationMapper(Refs refs, JobOrderRepository jobOrders) {
        this.refs = refs;
        this.jobOrders = jobOrders;
    }

    public ApplicationResponse toResponse(Application a) {
        return toResponses(List.of(a)).get(0);
    }

    public List<ApplicationResponse> toResponses(List<Application> apps) {
        if (apps.isEmpty()) {
            return List.of();
        }
        Map<UUID, JobOrder> jobs = jobOrders
                .findAllById(apps.stream().map(Application::getJobOrderId).distinct().toList()).stream()
                .collect(Collectors.toMap(JobOrder::getId, Function.identity()));
        Refs.Lookup l = refs.load(jobs.values().stream().map(JobOrder::getAccountId).toList(),
                apps.stream().map(Application::getPersonId).toList(),
                apps.stream().map(Application::getCreatedBy).toList());
        return apps.stream().map(a -> {
            JobOrder j = jobs.get(a.getJobOrderId());
            return new ApplicationResponse(a.getId(), new JobOrderRef(j.getId(), j.getTitle(), j.getStatus()),
                    l.account(j.getAccountId()), l.person(a.getPersonId()), a.getStage(), a.getRejectionReason(),
                    a.getStageChangedAt(), a.getCreatedAt(), l.user(a.getCreatedBy()), a.getVersion());
        }).toList();
    }
}
