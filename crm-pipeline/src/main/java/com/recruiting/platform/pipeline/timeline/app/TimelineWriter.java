package com.recruiting.platform.pipeline.timeline.app;

import com.recruiting.platform.contracts.DomainEvent;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.contracts.TimelineKind;
import com.recruiting.platform.pipeline.domain.TimelineEntry;
import com.recruiting.platform.pipeline.domain.TimelineEntryRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Appends timeline entries from events (A-08). Events are delivered at least once, so an entry is written only
 * when none exists yet for the event id (the column is unique as well).
 */
@Component
public class TimelineWriter {

    private final TimelineEntryRepository repo;

    TimelineWriter(TimelineEntryRepository repo) {
        this.repo = repo;
    }

    /** Fields of one entry besides the event header. */
    public record Entry(UUID accountId, TimelineKind kind, EntityType entityType, UUID entityId, String from,
                        String to, String reason, String summary, UUID personId, UUID jobOrderId) {
    }

    /**
     * Writes the entry for the event unless it was written before.
     *
     * @param event the event (gives id, time and actor)
     * @param e     the entry content
     */
    public void append(DomainEvent event, Entry e) {
        if (e.accountId() == null || repo.existsBySourceEventId(event.eventId())) {
            return;
        }
        TimelineEntry t = new TimelineEntry();
        t.setAccountId(e.accountId());
        t.setOccurredAt(event.occurredAt());
        t.setKind(e.kind());
        t.setEntityType(e.entityType());
        t.setEntityId(e.entityId());
        t.setFromStatus(e.from());
        t.setToStatus(e.to());
        t.setReason(e.reason());
        t.setSummary(e.summary().length() > 500 ? e.summary().substring(0, 500) : e.summary());
        t.setPersonId(e.personId());
        t.setJobOrderId(e.jobOrderId());
        t.setActorId(event.actorId());
        t.setSourceEventId(event.eventId());
        repo.save(t);
    }

    /**
     * Builds a status-change entry.
     *
     * @param accountId  account
     * @param kind       timeline kind
     * @param type       entity type
     * @param entityId   changed entity
     * @param from       old status
     * @param to         new status
     * @param reason     reason or null
     * @param what       English noun for the summary, e.g. "Job order"
     * @param personId   person or null
     * @param jobOrderId job order or null
     * @return the entry
     */
    public static Entry status(UUID accountId, TimelineKind kind, EntityType type, UUID entityId, Enum<?> from,
            Enum<?> to, String reason, String what, UUID personId, UUID jobOrderId) {
        return new Entry(accountId, kind, type, entityId, from.name(), to.name(), reason,
                what + " moved from " + from.name() + " to " + to.name(), personId, jobOrderId);
    }
}
