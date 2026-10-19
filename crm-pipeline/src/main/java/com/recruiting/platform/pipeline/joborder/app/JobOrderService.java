package com.recruiting.platform.pipeline.joborder.app;

import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.JobOrderCreated;
import com.recruiting.platform.contracts.JobOrderStatus;
import com.recruiting.platform.pipeline.domain.JobOrder;
import com.recruiting.platform.pipeline.domain.JobOrderRepository;
import com.recruiting.platform.pipeline.joborder.web.dto.JobOrderCreateRequest;
import com.recruiting.platform.pipeline.shared.Actor;
import com.recruiting.platform.pipeline.shared.Patch;
import com.recruiting.platform.pipeline.shared.Refs;
import com.recruiting.platform.pipeline.shared.Sorts;
import com.recruiting.platform.pipeline.shared.Transitions;
import com.recruiting.platform.pipeline.shared.Uuids;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Create, read, list and update job orders (A-06). Status moves are in {@link JobOrderStatusService}. */
@Service
public class JobOrderService {

    private static final Map<String, String> SORTABLE = Map.of("title", "title", "status", "status",
            "openedOn", "openedOn");

    private final JobOrderRepository repo;
    private final Refs refs;
    private final ApplicationEventPublisher events;

    JobOrderService(JobOrderRepository repo, Refs refs, ApplicationEventPublisher events) {
        this.repo = repo;
        this.refs = refs;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Page<JobOrder> list(String q, UUID accountId, List<JobOrderStatus> statuses, Boolean open, UUID ownerId,
            int page, int size, List<String> sort) {
        return repo.findAll(JobOrderQuery.of(q, accountId, statuses, open, ownerId),
                PageRequest.of(page, size, sortOf(sort)));
    }

    private static Sort sortOf(List<String> sort) {
        // daysOpen is derived from openedOn, so it sorts the other way round
        if (sort != null && sort.size() == 1 && sort.get(0).startsWith("daysOpen")) {
            boolean desc = sort.get(0).toLowerCase().endsWith("desc");
            return desc ? Sort.by("openedOn").ascending() : Sort.by("openedOn").descending();
        }
        return Sorts.parse(sort, SORTABLE, Sort.by(Sort.Direction.DESC, "openedOn"));
    }

    @Transactional(readOnly = true)
    public JobOrder get(UUID id) {
        return repo.findById(id).orElseThrow(() -> Transitions.notFound("Job order"));
    }

    @Transactional
    public JobOrder create(JobOrderCreateRequest r, Actor actor) {
        var account = refs.requireAccount(r.accountId());
        if (account.status() == AccountStatus.CLOSED) {
            throw new ApiException(ErrorCode.TRANSITION_GUARD_FAILED, "A closed account accepts no new job orders.");
        }
        UUID owner = r.ownerId() != null ? r.ownerId() : actor.id();
        if (r.ownerId() != null) {
            refs.requireUser(owner);
        }
        JobOrder j = new JobOrder();
        j.setAccountId(r.accountId());
        j.setTitle(r.title().trim());
        j.setLocation(r.location());
        j.setSeniority(r.seniority());
        j.setDescription(r.description());
        j.setFeeTerms(r.feeTerms());
        j.setOpenings(r.openings() == null ? 1 : r.openings());
        j.setOwnerId(owner);
        j.setOpenedOn(r.openedOn() == null ? LocalDate.now() : r.openedOn());
        j.setStatus(JobOrderStatus.INITIAL);
        j.setStatusChangedAt(Instant.now());
        repo.saveAndFlush(j);
        events.publishEvent(new JobOrderCreated(Uuids.v7(), Instant.now(), actor.id(), j.getId(), j.getAccountId(),
                j.getTitle(), j.getOwnerId(), j.getStatus()));
        return j;
    }

    @Transactional
    public JobOrder update(UUID id, Patch p) {
        JobOrder j = get(id);
        Transitions.checkVersion(p.version(), j.getVersion());
        if (p.has("title")) {
            j.setTitle(p.text("title", 1, 200, false).trim());
        }
        if (p.has("location")) {
            j.setLocation(p.text("location", 0, 200, true));
        }
        if (p.has("seniority")) {
            j.setSeniority(p.text("seniority", 0, 100, true));
        }
        if (p.has("description")) {
            j.setDescription(p.text("description", 0, 10000, true));
        }
        if (p.has("feeTerms")) {
            j.setFeeTerms(p.text("feeTerms", 0, 2000, true));
        }
        if (p.has("openings")) {
            j.setOpenings(p.integer("openings", 1));
        }
        if (p.has("openedOn")) {
            j.setOpenedOn(p.date("openedOn"));
        }
        if (p.has("ownerId")) {
            UUID owner = p.uuid("ownerId");
            refs.requireUser(owner);
            j.setOwnerId(owner);
        }
        return repo.saveAndFlush(j);
    }
}
