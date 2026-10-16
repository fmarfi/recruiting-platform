package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruiting.platform.contracts.AccessAction;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.security.domain.AccessLog;
import com.recruiting.platform.security.domain.AccessLogRepository;
import com.recruiting.platform.security.domain.AppUser;
import com.recruiting.platform.security.domain.AppUserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class SecurityRepositoryIT extends AbstractPersistenceIT {

    @Autowired
    private AppUserRepository users;

    @Autowired
    private AccessLogRepository accessLog;

    @Test
    void userIsFoundByEmailIgnoringCaseAndGetsAUuidV7() {
        AppUser u = newUser();
        assertThat(u.getId()).isNotNull();
        assertThat(u.getId().version()).isEqualTo(7);
        assertThat(u.getVersion()).isNotNull();
        assertThat(u.getCreatedAt()).isNotNull();
        assertThat(users.findByEmailIgnoreCase(u.getEmail().toUpperCase())).isPresent();
    }

    @Test
    void accessLogIsQueryableByEntityNewestFirst() {
        AppUser u = newUser();
        UUID entityId = UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            AccessLog l = new AccessLog();
            l.setUserId(u.getId());
            l.setAction(AccessAction.VIEW);
            l.setEntityType(EntityType.PERSON);
            l.setEntityId(entityId);
            l.setAt(Instant.now().plusSeconds(i));
            accessLog.save(l);
        }
        var page = accessLog.findByEntityTypeAndEntityIdOrderByAtDesc(EntityType.PERSON, entityId, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).getAt()).isAfter(page.getContent().get(2).getAt());
    }
}
