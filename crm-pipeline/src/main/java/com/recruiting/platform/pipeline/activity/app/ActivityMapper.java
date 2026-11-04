package com.recruiting.platform.pipeline.activity.app;

import com.recruiting.platform.pipeline.activity.web.dto.ActivityResponse;
import com.recruiting.platform.pipeline.domain.Activity;
import com.recruiting.platform.pipeline.domain.ActivityAttendee;
import com.recruiting.platform.pipeline.domain.ActivityAttendeeRepository;
import com.recruiting.platform.pipeline.shared.JobOrderRefs;
import com.recruiting.platform.pipeline.shared.Refs;
import com.recruiting.platform.pipeline.shared.dto.JobOrderRef;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Builds activity responses with batched account, person, user, attendee and job order lookups. */
@Component
public class ActivityMapper {

    private final Refs refs;
    private final JobOrderRefs jobs;
    private final ActivityAttendeeRepository attendees;

    ActivityMapper(Refs refs, JobOrderRefs jobs, ActivityAttendeeRepository attendees) {
        this.refs = refs;
        this.jobs = jobs;
        this.attendees = attendees;
    }

    public ActivityResponse toResponse(Activity a) {
        return toResponses(List.of(a)).get(0);
    }

    public List<ActivityResponse> toResponses(List<Activity> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<UUID>> att = new HashMap<>();
        for (ActivityAttendee x : attendees.findByIdActivityIdIn(list.stream().map(Activity::getId).toList())) {
            att.computeIfAbsent(x.getActivityId(), k -> new ArrayList<>()).add(x.getPersonId());
        }
        List<UUID> personIds = new ArrayList<>(list.stream().map(Activity::getPersonId).toList());
        att.values().forEach(personIds::addAll);
        Refs.Lookup l = refs.load(list.stream().map(Activity::getAccountId).toList(), personIds,
                list.stream().map(Activity::getUserId).toList());
        Map<UUID, JobOrderRef> jo = jobs.load(list.stream().map(Activity::getJobOrderId).toList());
        return list.stream().map(a -> new ActivityResponse(a.getId(), a.getType(), a.getNote(), a.getOccurredAt(),
                l.account(a.getAccountId()), l.personRef(a.getPersonId()), jo.get(a.getJobOrderId()),
                l.user(a.getUserId()), a.getCreatedAt(), a.getVersion(), a.getOutcome(), a.getDirection(),
                att.getOrDefault(a.getId(), List.of()).stream().map(l::personRef).filter(Objects::nonNull).toList(),
                a.getDurationMin(), a.getSource(), a.getExternalMessageId())).toList();
    }
}
