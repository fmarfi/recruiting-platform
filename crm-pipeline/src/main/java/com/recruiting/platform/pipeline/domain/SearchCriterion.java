package com.recruiting.platform.pipeline.domain;

import com.recruiting.platform.contracts.CriterionKind;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;

/** Maps table {@code search_criterion} (1.3.0): a must-have or nice-to-have of a search. Never hard-deleted (evaluation scores refer to the id); {@code removedAt} marks removal. */
@Entity
@Table(name = "search_criterion")
@Audited
public class SearchCriterion extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "job_order_id", updatable = false)
    private UUID jobOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind")
    private CriterionKind kind;

    @Column(name = "text", length = 300)
    private String text;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "removed_at")
    private Instant removedAt;

    public SearchCriterion() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public UUID getJobOrderId() {
        return jobOrderId;
    }
    public void setJobOrderId(UUID jobOrderId) {
        this.jobOrderId = jobOrderId;
    }

    public CriterionKind getKind() {
        return kind;
    }
    public void setKind(CriterionKind kind) {
        this.kind = kind;
    }

    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getRemovedAt() {
        return removedAt;
    }
    public void setRemovedAt(Instant removedAt) {
        this.removedAt = removedAt;
    }

}
