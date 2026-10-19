package com.recruiting.platform.pipeline.activity.app;

import com.recruiting.platform.contracts.ActivityLogged;
import com.recruiting.platform.contracts.ActivityType;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.pipeline.activity.web.dto.ActivityCreateRequest;
import com.recruiting.platform.pipeline.domain.Activity;
import com.recruiting.platform.pipeline.domain.ActivityRepository;
import com.recruiting.platform.pipeline.shared.Actor;
import com.recruiting.platform.pipeline.shared.JobOrderRefs;
import com.recruiting.platform.pipeline.shared.Patch;
import com.recruiting.platform.pipeline.shared.Refs;
import com.recruiting.platform.pipeline.shared.Sorts;
import com.recruiting.platform.pipeline.shared.Transitions;
import com.recruiting.platform.pipeline.shared.Uuids;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Log, read, correct and delete activities (A-04). Corrections and deletes: author, manager or admin. */
@Service
public class ActivityService {

    private static final Map<String, String> SORTABLE = Map.of("occurredAt", "occurredAt", "createdAt", "createdAt",
            "type", "type");

    private final ActivityRepository repo;
    private final Refs refs;
    private final JobOrderRefs jobs;
    private final ApplicationEventPublisher events;

    ActivityService(ActivityRepository repo, Refs refs, JobOrderRefs jobs, ApplicationEventPublisher events) {
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
    public Activity log(ActivityCreateRequest r, Actor actor) {
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
        Activity a = new Activity();
        a.setType(r.type());
        a.setNote(r.note().trim());
        a.setOccurredAt(at);
        a.setAccountId(r.accountId());
        a.setPersonId(r.personId());
        a.setJobOrderId(r.jobOrderId());
        a.setUserId(actor.id());
        repo.saveAndFlush(a);
        publish(a, actor);
        return a;
    }

    @Transactional
    public Activity update(UUID id, Patch patch, Actor actor) {
        Activity a = get(id);
        requireAuthor(a, actor);
        Transitions.checkVersion(patch.version(), a.getVersion());
        if (patch.has("type")) {
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
        repo.saveAndFlush(a);
        publish(a, actor);
        return a;
    }

    @Transactional
    public void delete(UUID id, Actor actor) {
        Activity a = get(id);
        requireAuthor(a, actor);
        repo.delete(a);
    }

    private void publish(Activity a, Actor actor) {
        events.publishEvent(new ActivityLogged(Uuids.v7(), Instant.now(), actor.id(), a.getId(), a.getType(),
                a.getOccurredAt(), a.getAccountId(), a.getPersonId(), a.getJobOrderId()));
    }

    private static void checkNotFuture(Instant at) {
        if (at.isAfter(Instant.now().plusSeconds(60))) {
            throw Transitions.invalid("occurredAt", "occurredAt may not be in the future.");
        }
    }

    private static void requireAuthor(Activity a, Actor actor) {
        if (!actor.elevated() && !actor.id().equals(a.getUserId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Only the author, a manager or an admin may change this.");
        }
    }
}
