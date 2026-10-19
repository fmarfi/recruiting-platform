package com.recruiting.platform.pipeline.nextaction.app;

import com.recruiting.platform.pipeline.domain.NextAction;
import com.recruiting.platform.pipeline.nextaction.web.dto.NextActionResponse;
import com.recruiting.platform.pipeline.shared.JobOrderRefs;
import com.recruiting.platform.pipeline.shared.Refs;
import com.recruiting.platform.pipeline.shared.dto.JobOrderRef;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Builds next action responses with batched lookups. */
@Component
public class NextActionMapper {

    private final Refs refs;
    private final JobOrderRefs jobs;

    NextActionMapper(Refs refs, JobOrderRefs jobs) {
        this.refs = refs;
        this.jobs = jobs;
    }

    public NextActionResponse toResponse(NextAction n) {
        return toResponses(List.of(n)).get(0);
    }

    public List<NextActionResponse> toResponses(List<NextAction> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Refs.Lookup l = refs.load(list.stream().map(NextAction::getAccountId).toList(),
                list.stream().map(NextAction::getPersonId).toList(),
                list.stream().map(NextAction::getOwnerId).toList());
        Map<UUID, JobOrderRef> jo = jobs.load(list.stream().map(NextAction::getJobOrderId).toList());
        LocalDate today = LocalDate.now();
        return list.stream().map(n -> new NextActionResponse(n.getId(), n.getTitle(), n.getDueOn(),
                l.user(n.getOwnerId()), l.account(n.getAccountId()), l.personRef(n.getPersonId()),
                jo.get(n.getJobOrderId()), n.isDone(), n.getDoneAt(), !n.isDone() && n.getDueOn().isBefore(today),
                n.getCreatedAt(), n.getVersion())).toList();
    }
}
