package com.recruiting.platform.pipeline.activity.app;

import com.recruiting.platform.contracts.ActivityLogged;
import com.recruiting.platform.contracts.ActivityType;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.pipeline.activity.web.dto.ActivityCreateRequest;
import com.recruiting.platform.pipeline.domain.Activity;
import com.recruiting.platform.pipeline.domain.ActivityRepository;
import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.security.CurrentUser;
import com.recruiting.platform.pipeline.shared.JobOrderRefs;
import com.recruiting.platform.pipeline.shared.Patch;
import com.recruiting.platform.pipeline.shared.Refs;
import com.recruiting.platform.pipeline.shared.Sorts;
import com.recruiting.platform.pipeline.shared.Transitions;
import com.recruiting.platform.pipeline.shared.Uuids;
import com.recruiting.platform.contracts.ActivityDirection;
import com.recruiting.platform.contracts.ActivityOutcome;
import com.recruiting.platform.contracts.ActivitySource;
import com.recruiting.platform.pipeline.domain.ActivityAttendee;
import com.recruiting.platform.pipeline.domain.ActivityAttendeeRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Log, read, correct and delete activities (A-04). Corrections and deletes: author, manager or admin. */
@Service
public class ActivityService {

    private final CurrentUser currentUser;
    private static final Map<String, String> SORTABLE = Map.of("occurredAt", "occurredAt", "createdAt", "createdAt",
            "type", "type");

    private final ActivityRepository repo;
    private final Refs refs;
    private final JobOrderRefs jobs;
    private final ApplicationEventPublisher events;

    private final ActivityAttendeeRepository attendees;

    ActivityService(ActivityRepository repo, ActivityAttendeeRepository attendees, Refs refs, JobOrderRefs jobs,
            ApplicationEventPublisher events, CurrentUser currentUser) {
        this.attendees = attendees;
        this.currentUser = currentUser;
        this.repo = repo;
        this.refs = refs;
        this.jobs = jobs;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Page<Activity> list(ActivityFilter f, int page, int size, List<String> sort) {
        Sort s = Sorts.parse(sort, SORTABLE, Sort.by(Sort.Direction.DESC, "occurredAt"));
        return repo.findAll(f.toSpec(), PageRequest.of(page, size, s));
    }

    @Transactional(readOnly = true)
    public Activity get(UUID id) {
        return repo.findById(id).orElseThrow(() -> Transitions.notFound("Activity"));
    }

    @Transactional
    public Activity log(ActivityCreateRequest r) {
        if (r.accountId() == null && r.personId() == null) {
            throw Transitions.invalid("accountId", "Give an accountId or a personId.");
        }
        Instant at = r.occurredAt() == null ? Instant.now() : r.occurredAt();
        checkNotFuture(at);
        if (r.accountId() != null) {
            refs.requireAccount(r.accountId());
        }
        if (r.personId() != null) {
            refs.requirePerson(r.personId());
        }
        if (r.jobOrderId() != null) {
            jobs.require(r.jobOrderId());
        }
        if (r.direction() != null && r.type() == ActivityType.NOTE) {
            throw Transitions.invalid("direction", "direction is not allowed for a NOTE.");
        }
        ActivitySource source = r.source() == null ? ActivitySource.MANUAL : r.source();
        if (r.externalMessageId() != null) {
            if (source != ActivitySource.EMAIL) {
                throw Transitions.invalid("externalMessageId", "externalMessageId needs source EMAIL.");
            }
            repo.findByExternalMessageId(r.externalMessageId()).ifPresent(o -> {
                throw new ApiException(ErrorCode.DUPLICATE, "This e-mail was already captured.",
                        Map.of("existingId", o.getId()));
            });
        }
        List<UUID> attendeeIds = attendeeIds(r.attendeePersonIds(), r.personId());
        Activity a = new Activity();
        a.setOutcome(r.outcome());
        a.setDirection(r.direction());
        a.setDurationMin(r.durationMin() == null ? null : r.durationMin().shortValue());
        a.setSource(source);
        a.setExternalMessageId(r.externalMessageId());
        a.setType(r.type());
        a.setNote(r.note().trim());
        a.setOccurredAt(at);
        a.setAccountId(r.accountId());
        a.setPersonId(r.personId());
        a.setJobOrderId(r.jobOrderId());
        a.setUserId(currentUser.id());
        repo.saveAndFlush(a);
        attendeeIds.forEach(p -> attendees.save(new ActivityAttendee(a.getId(), p)));
        attendees.flush();
        publish(a, attendeeIds);
        return a;
    }

    private List<UUID> attendeeIds(List<UUID> raw, UUID personId) {
        List<UUID> ids = raw == null ? List.of() : raw.stream().distinct().toList();
        if (ids.size() > 20) {
            throw Transitions.invalid("attendeePersonIds", "At most 20 attendees.");
        }
        if (personId != null && ids.contains(personId)) {
            throw Transitions.invalid("attendeePersonIds", "The main person is not an attendee.");
        }
        refs.requireLivePeople(ids, "attendeePersonIds");
        return ids;
    }

    @Transactional
    public Activity update(UUID id, Patch patch) {
        Activity a = get(id);
        requireAuthor(a);
        Transitions.checkVersion(patch.version(), a.getVersion());
        if (patch.has("type") && !patch.isNull("type")) {
            try {
                a.setType(ActivityType.valueOf(patch.text("type", 1, 20, false)));
            } catch (IllegalArgumentException e) {
                throw Transitions.invalid("type", "Unknown activity type.");
            }
        }
        if (patch.has("note")) {
            String note = patch.text("note", 1, 4000, false).trim();
            if (note.isEmpty()) {
                throw Transitions.invalid("note", "note must not be blank.");
            }
            a.setNote(note);
        }
        if (patch.has("occurredAt")) {
            Instant at = patch.instant("occurredAt");
            checkNotFuture(at);
            a.setOccurredAt(at);
        }
        for (String immutable : List.of("source", "externalMessageId")) {
            if (patch.has(immutable)) {
                throw Transitions.invalid(immutable, immutable + " cannot be changed.");
            }
        }
        if (patch.has("outcome")) {
            if (patch.isNull("outcome")) {
                a.setOutcome(null);
            } else {
                try {
                    a.setOutcome(ActivityOutcome.valueOf(patch.text("outcome", 1, 40, false)));
                } catch (IllegalArgumentException e) {
                    throw Transitions.invalid("outcome", "Unknown outcome.");
                }
            }
        }
        if (patch.has("direction")) {
            if (patch.isNull("direction")) {
                a.setDirection(null);
            } else {
                try {
                    a.setDirection(ActivityDirection.valueOf(patch.text("direction", 1, 20, false)));
                } catch (IllegalArgumentException e) {
                    throw Transitions.invalid("direction", "Unknown direction.");
                }
            }
        }
        if (a.getDirection() != null && a.getType() == ActivityType.NOTE) {
            throw Transitions.invalid("direction", "direction is not allowed for a NOTE.");
        }
        if (patch.has("durationMin")) {
            if (patch.isNull("durationMin")) {
                a.setDurationMin(null);
            } else {
                int d = patch.integer("durationMin", 0);
                if (d > 1440) {
                    throw Transitions.invalid("durationMin", "durationMin must be 0 to 1440.");
                }
                a.setDurationMin((short) d);
            }
        }
        List<UUID> attendeeIds;
        if (patch.has("attendeePersonIds")) {
            attendeeIds = attendeeIds(patch.uuidList("attendeePersonIds"), a.getPersonId());
            Set<UUID> want = new HashSet<>(attendeeIds);
            List<ActivityAttendee> have = attendees.findByIdActivityId(a.getId());
            Set<UUID> haveIds = have.stream().map(ActivityAttendee::getPersonId).collect(Collectors.toSet());
            have.stream().filter(x -> !want.contains(x.getPersonId())).forEach(attendees::delete);
            attendeeIds.stream().filter(p -> !haveIds.contains(p))
                    .forEach(p -> attendees.save(new ActivityAttendee(a.getId(), p)));
        } else {
            attendeeIds = attendees.findByIdActivityId(a.getId()).stream().map(ActivityAttendee::getPersonId)
                    .toList();
        }
        repo.saveAndFlush(a);
        attendees.flush();
        publish(a, attendeeIds);
        return a;
    }

    @Transactional
    public void delete(UUID id) {
        Activity a = get(id);
        requireAuthor(a);
        attendees.deleteAll(attendees.findByIdActivityId(id));
        attendees.flush();
        repo.delete(a);
    }

    private void publish(Activity a, List<UUID> attendeeIds) {
        events.publishEvent(new ActivityLogged(Uuids.v7(), Instant.now(), currentUser.id(), a.getId(), a.getType(),
                a.getOccurredAt(), a.getAccountId(), a.getPersonId(), a.getJobOrderId(), attendeeIds));
    }

    private static void checkNotFuture(Instant at) {
        if (at.isAfter(Instant.now().plusSeconds(60))) {
            throw Transitions.invalid("occurredAt", "occurredAt may not be in the future.");
        }
    }

    private void requireAuthor(Activity a) {
        if (!currentUser.hasAtLeast(Role.MANAGER) && !currentUser.id().equals(a.getUserId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Only the author, a manager or an admin may change this.");
        }
    }
}
