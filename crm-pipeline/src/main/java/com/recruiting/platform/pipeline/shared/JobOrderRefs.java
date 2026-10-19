package com.recruiting.platform.pipeline.shared;

import com.recruiting.platform.pipeline.domain.JobOrder;
import com.recruiting.platform.pipeline.domain.JobOrderRepository;
import com.recruiting.platform.pipeline.shared.dto.JobOrderRef;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Batch lookup of job order references (id, title, status) for responses of other features. */
@Component
public class JobOrderRefs {

    private final JobOrderRepository repo;

    JobOrderRefs(JobOrderRepository repo) {
        this.repo = repo;
    }

    /**
     * Resolves job order ids in one query.
     *
     * @param ids ids, nulls ignored
     * @return refs by id
     */
    public Map<UUID, JobOrderRef> load(Collection<UUID> ids) {
        var clean = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (clean.isEmpty()) {
            return Map.of();
        }
        return repo.findAllById(clean).stream().collect(
                Collectors.toMap(JobOrder::getId, j -> new JobOrderRef(j.getId(), j.getTitle(), j.getStatus())));
    }

    /**
     * Answers 404 when the job order does not exist.
     *
     * @param id job order id
     * @return the job order
     */
    public JobOrder require(UUID id) {
        return repo.findById(id).orElseThrow(() -> Transitions.notFound("Job order"));
    }
}
