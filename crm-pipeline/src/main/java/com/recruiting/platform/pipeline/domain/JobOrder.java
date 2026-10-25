package com.recruiting.platform.pipeline.domain;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import com.recruiting.platform.contracts.JobOrderStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;

/** Maps table {@code job_order}. Other modules reference it by id only (ADR 0001). */
@Entity
@Table(name = "job_order")
@Audited
public class JobOrder extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "seniority", length = 100)
    private String seniority;

    @Column(name = "description")
    private String description;

    @Column(name = "fee_terms")
    private String feeTerms;

    @Column(name = "openings")
    private Integer openings = 1;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private JobOrderStatus status = JobOrderStatus.OPEN;

    @Column(name = "status_changed_at")
    private Instant statusChangedAt = Instant.now();

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "opened_on")
    private LocalDate openedOn = LocalDate.now();

    @Column(name = "closed_on")
    private LocalDate closedOn;

    /** Lower-case labels, at most 20 (ADR 0010); column {@code tags varchar(50)[]}. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", length = 50)
    private List<String> tags = new ArrayList<>();

    public JobOrder() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSeniority() {
        return seniority;
    }

    public void setSeniority(String seniority) {
        this.seniority = seniority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFeeTerms() {
        return feeTerms;
    }

    public void setFeeTerms(String feeTerms) {
        this.feeTerms = feeTerms;
    }

    public Integer getOpenings() {
        return openings;
    }

    public void setOpenings(Integer openings) {
        this.openings = openings;
    }

    public JobOrderStatus getStatus() {
        return status;
    }

    public void setStatus(JobOrderStatus status) {
        this.status = status;
    }

    public Instant getStatusChangedAt() {
        return statusChangedAt;
    }

    public void setStatusChangedAt(Instant statusChangedAt) {
        this.statusChangedAt = statusChangedAt;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDate getOpenedOn() {
        return openedOn;
    }

    public void setOpenedOn(LocalDate openedOn) {
        this.openedOn = openedOn;
    }

    public LocalDate getClosedOn() {
        return closedOn;
    }

    public void setClosedOn(LocalDate closedOn) {
        this.closedOn = closedOn;
    }


    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
    }
}
