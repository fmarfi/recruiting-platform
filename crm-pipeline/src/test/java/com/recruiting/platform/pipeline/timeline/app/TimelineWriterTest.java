package com.recruiting.platform.pipeline.timeline.app;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.AccountStatusChanged;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.contracts.TimelineKind;
import com.recruiting.platform.pipeline.domain.TimelineEntry;
import com.recruiting.platform.pipeline.domain.TimelineEntryRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TimelineWriterTest {

    private final TimelineEntryRepository repo = mock(TimelineEntryRepository.class);
    private final TimelineWriter writer = new TimelineWriter(repo);
    private final UUID account = UUID.randomUUID();
    private final AccountStatusChanged event = new AccountStatusChanged(UUID.randomUUID(), Instant.now(),
            UUID.randomUUID(), account, AccountStatus.ACTIVE, AccountStatus.ON_HOLD, "Client froze hiring");

    @Test
    void writesStatusEntryWithFromToAndReason() {
        writer.append(event, TimelineWriter.status(account, TimelineKind.ACCOUNT_STATUS, EntityType.ACCOUNT, account,
                event.from(), event.to(), event.reason(), "Account", null, null));
        ArgumentCaptor<TimelineEntry> c = ArgumentCaptor.forClass(TimelineEntry.class);
        verify(repo).save(c.capture());
        TimelineEntry t = c.getValue();
        org.assertj.core.api.Assertions.assertThat(t.getFromStatus()).isEqualTo("ACTIVE");
        org.assertj.core.api.Assertions.assertThat(t.getToStatus()).isEqualTo("ON_HOLD");
        org.assertj.core.api.Assertions.assertThat(t.getReason()).isEqualTo("Client froze hiring");
        org.assertj.core.api.Assertions.assertThat(t.getSourceEventId()).isEqualTo(event.eventId());
        org.assertj.core.api.Assertions.assertThat(t.getActorId()).isEqualTo(event.actorId());
    }

    @Test
    void redeliveredEventIsIgnored() {
        when(repo.existsBySourceEventId(event.eventId())).thenReturn(true);
        writer.append(event, TimelineWriter.status(account, TimelineKind.ACCOUNT_STATUS, EntityType.ACCOUNT, account,
                event.from(), event.to(), event.reason(), "Account", null, null));
        verify(repo, never()).save(any());
    }
}
