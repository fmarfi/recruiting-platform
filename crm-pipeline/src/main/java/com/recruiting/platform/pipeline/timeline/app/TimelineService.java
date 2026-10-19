package com.recruiting.platform.pipeline.timeline.app;

import com.recruiting.platform.contracts.TimelineKind;
import com.recruiting.platform.pipeline.domain.TimelineEntry;
import com.recruiting.platform.pipeline.domain.TimelineEntryRepository;
import com.recruiting.platform.pipeline.shared.JobOrderRefs;
import com.recruiting.platform.pipeline.shared.Refs;
import com.recruiting.platform.pipeline.shared.dto.JobOrderRef;
import com.recruiting.platform.pipeline.timeline.web.dto.TimelineEntryResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads the timeline of an account (A-08), newest first. */
@Service
public class TimelineService {

    private final TimelineEntryRepository repo;
    private final Refs refs;
    private final JobOrderRefs jobs;

    TimelineService(TimelineEntryRepository repo, Refs refs, JobOrderRefs jobs) {
        this.repo = repo;
        this.refs = refs;
        this.jobs = jobs;
    }

    @Transactional(readOnly = true)
    public Page<TimelineEntry> page(UUID accountId, List<TimelineKind> kinds, int page, int size) {
        refs.requireAccount(accountId);
        PageRequest p = PageRequest.of(page, size);
        return kinds == null || kinds.isEmpty() ? repo.findByAccountIdOrderByOccurredAtDesc(accountId, p)
                : repo.findByAccountIdAndKindInOrderByOccurredAtDesc(accountId, kinds, p);
    }

    @Transactional(readOnly = true)
    public List<TimelineEntryResponse> toResponses(List<TimelineEntry> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Refs.Lookup l = refs.load(List.of(), list.stream().map(TimelineEntry::getPersonId).toList(),
                list.stream().map(TimelineEntry::getActorId).toList());
        Map<UUID, JobOrderRef> jo = jobs.load(list.stream().map(TimelineEntry::getJobOrderId).toList());
        return list.stream().map(t -> new TimelineEntryResponse(t.getId(), t.getAccountId(), t.getOccurredAt(),
                t.getKind(), t.getEntityType(), t.getEntityId(), t.getFromStatus(), t.getToStatus(), t.getReason(),
                t.getSummary(), l.personRef(t.getPersonId()), jo.get(t.getJobOrderId()),
                l.user(t.getActorId()))).toList();
    }
}
