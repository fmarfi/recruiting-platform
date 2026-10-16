package com.recruiting.platform.security.domain;

import jakarta.persistence.*;
import com.recruiting.platform.contracts.AccessAction;
import com.recruiting.platform.contracts.EntityType;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/** Maps table {@code access_log}. Other modules reference it by id only (ADR 0001). */
@Entity
@org.hibernate.annotations.Immutable
@Table(name = "access_log")
public class AccessLog {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "action")
    @Enumerated(EnumType.STRING)
    private AccessAction action;

    @Column(name = "entity_type")
    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "at")
    private Instant at = Instant.now();

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    public AccessLog() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public AccessAction getAction() {
        return action;
    }

    public void setAction(AccessAction action) {
        this.action = action;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public Instant getAt() {
        return at;
    }

    public void setAt(Instant at) {
        this.at = at;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

}
