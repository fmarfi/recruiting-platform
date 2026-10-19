package com.recruiting.platform.pipeline.nextaction.app;

import com.recruiting.platform.pipeline.domain.NextAction;
import com.recruiting.platform.pipeline.domain.NextActionRepository;
import com.recruiting.platform.pipeline.nextaction.web.dto.NextActionCreateRequest;
import com.recruiting.platform.pipeline.shared.Actor;
import com.recruiting.platform.pipeline.shared.JobOrderRefs;
import com.recruiting.platform.pipeline.shared.Patch;
import com.recruiting.platform.pipeline.shared.Refs;
import com.recruiting.platform.pipeline.shared.Sorts;
import com.recruiting.platform.pipeline.shared.Transitions;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Follow-ups (A-10): list, create, change, complete or reopen, delete. */
@Service
public class NextActionService {

    private static final Map<String, String> SORTABLE = Map.of("dueOn", "dueOn", "createdAt", "createdAt",
            "title", "title");

    private final NextActionRepository repo;
    private final Refs refs;
    private final JobOrderRefs jobs;

    NextActionService(NextActionRepository repo, Refs refs, JobOrderRefs jobs) {
        this.repo = repo;
        this.refs = refs;
        this.jobs = jobs;
    }

    @Transactional(readOnly = true)
    public Page<NextAction> list(NextActionFilter f, Actor actor, int page, int size, List<String> sort) {
        UUID owner = Boolean.TRUE.equals(f.mine()) ? actor.id() : f.ownerId();
        Specification<NextAction> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (owner != null) {
                ps.add(cb.equal(root.get("ownerId"), owner));
            }
            if (f.accountId() != null) {
                ps.add(cb.equal(root.get("accountId"), f.accountId()));
            }
            if (f.personId() != null) {
                ps.add(cb.equal(root.get("personId"), f.personId()));
            }
            if (f.jobOrderId() != null) {
                ps.add(cb.equal(root.get("jobOrderId"), f.jobOrderId()));
            }
            if (f.done() != null) {
                ps.add(cb.equal(root.get("done"), f.done()));
            }
            if (f.dueBefore() != null) {
                ps.add(cb.lessThanOrEqualTo(root.<LocalDate>get("dueOn"), f.dueBefore()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Sort s = Sorts.parse(sort, SORTABLE, Sort.by(Sort.Direction.ASC, "dueOn"));
        return repo.findAll(spec, PageRequest.of(page, size, s));
    }

    @Transactional
    public NextAction create(NextActionCreateRequest r, Actor actor) {
        UUID owner = r.ownerId() != null ? r.ownerId() : actor.id();
        if (r.ownerId() != null) {
            refs.requireUser(owner);
        }
        if (r.accountId() != null) {
            refs.requireAccount(r.accountId());
        }
        if (r.personId() != null) {
            refs.requirePerson(r.personId());
        }
        if (r.jobOrderId() != null) {
            jobs.require(r.jobOrderId());
        }
        NextAction n = new NextAction();
        n.setTitle(r.title().trim());
        n.setDueOn(r.dueOn());
        n.setOwnerId(owner);
        n.setAccountId(r.accountId());
        n.setPersonId(r.personId());
        n.setJobOrderId(r.jobOrderId());
        return repo.saveAndFlush(n);
    }

    @Transactional
    public NextAction update(UUID id, Patch patch) {
        NextAction n = repo.findById(id).orElseThrow(() -> Transitions.notFound("Next action"));
        Transitions.checkVersion(patch.version(), n.getVersion());
        if (patch.has("title")) {
            String t = patch.text("title", 1, 300, false).trim();
            if (t.isEmpty()) {
                throw Transitions.invalid("title", "title must not be blank.");
            }
            n.setTitle(t);
        }
        if (patch.has("dueOn")) {
            n.setDueOn(patch.date("dueOn"));
        }
        if (patch.has("ownerId")) {
            UUID owner = patch.uuid("ownerId");
            refs.requireUser(owner);
            n.setOwnerId(owner);
        }
        if (patch.has("done")) {
            boolean done = patch.bool("done");
            if (done != n.isDone()) {
                n.setDone(done);
                n.setDoneAt(done ? Instant.now() : null);
            }
        }
        return repo.saveAndFlush(n);
    }

    @Transactional
    public void delete(UUID id) {
        NextAction n = repo.findById(id).orElseThrow(() -> Transitions.notFound("Next action"));
        repo.delete(n);
    }
}
