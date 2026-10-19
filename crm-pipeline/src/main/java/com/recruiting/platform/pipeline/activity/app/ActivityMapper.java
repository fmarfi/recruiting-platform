package com.recruiting.platform.pipeline.activity.app;

import com.recruiting.platform.pipeline.activity.web.dto.ActivityResponse;
import com.recruiting.platform.pipeline.domain.Activity;
import com.recruiting.platform.pipeline.shared.JobOrderRefs;
import com.recruiting.platform.pipeline.shared.Refs;
import com.recruiting.platform.pipeline.shared.dto.JobOrderRef;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Builds activity responses with batched account, person, user and job order lookups. */
@Component
public class ActivityMapper {

    private final Refs refs;
    private final JobOrderRefs jobs;

    ActivityMapper(Refs refs, JobOrderRefs jobs) {
        this.refs = refs;
        this.jobs = jobs;
    }

    public ActivityResponse toResponse(Activity a) {
        return toResponses(List.of(a)).get(0);
    }

    public List<ActivityResponse> toResponses(List<Activity> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Refs.Lookup l = refs.load(list.stream().map(Activity::getAccountId).toList(),
                list.stream().map(Activity::getPersonId).toList(), list.stream().map(Activity::getUserId).toList());
        Map<UUID, JobOrderRef> jo = jobs.load(list.stream().map(Activity::getJobOrderId).toList());
        return list.stream().map(a -> new ActivityResponse(a.getId(), a.getType(), a.getNote(), a.getOccurredAt(),
                l.account(a.getAccountId()), l.personRef(a.getPersonId()), jo.get(a.getJobOrderId()),
                l.user(a.getUserId()), a.getCreatedAt(), a.getVersion())).toList();
    }
}
