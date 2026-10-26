package com.recruiting.platform.accounts.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/**
 * Maps table {@code person_merge} (1.2.0): append-only, ids only, not audited. {@code merged_id} has no FK (that row
 * was deleted). The cascade from the survivor stays; privacy reads the merged ids before the delete (purge step 0).
 */
@Entity
@Table(name = "person_merge")
public class PersonMerge {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "survivor_id", updatable = false)
    private UUID survivorId;

    @Column(name = "merged_id", updatable = false)
    private UUID mergedId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "fields_taken", length = 40)
    private List<String> fieldsTaken = new ArrayList<>();

    @Column(name = "merged_at")
    private Instant mergedAt = Instant.now();

    @Column(name = "merged_by")
    private UUID mergedBy;

    public PersonMerge() {
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

    public List<String> getFieldsTaken() {
        return fieldsTaken;
    }

    public void setFieldsTaken(List<String> fieldsTaken) {
        this.fieldsTaken = new ArrayList<>(fieldsTaken);
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
