package com.recruiting.platform.accounts.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/** Maps table {@code account_merge} (1.3.0): append-only record of an account merge, counts only, not audited. */
@Entity
@Table(name = "account_merge")
public class AccountMerge {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "survivor_id", updatable = false)
    private UUID survivorId;

    @Column(name = "merged_id", updatable = false)
    private UUID mergedId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "moved", updatable = false)
    private Map<String,Object> moved;

    @Column(name = "merged_at", updatable = false)
    private Instant mergedAt;

    @Column(name = "merged_by", updatable = false)
    private UUID mergedBy;

    public AccountMerge() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public UUID getSurvivorId() {
        return survivorId;
    }
    public void setSurvivorId(UUID survivorId) {
        this.survivorId = survivorId;
    }

    public UUID getMergedId() {
        return mergedId;
    }
    public void setMergedId(UUID mergedId) {
        this.mergedId = mergedId;
    }

    public Map<String,Object> getMoved() {
        return moved;
    }
    public void setMoved(Map<String,Object> moved) {
        this.moved = moved;
    }

    public Instant getMergedAt() {
        return mergedAt;
    }
    public void setMergedAt(Instant mergedAt) {
        this.mergedAt = mergedAt;
    }

    public UUID getMergedBy() {
        return mergedBy;
    }
    public void setMergedBy(UUID mergedBy) {
        this.mergedBy = mergedBy;
    }

}
