package com.recruiting.platform.pipeline.domain;

import jakarta.persistence.*;
import com.recruiting.platform.contracts.ProposalStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

/** Maps table {@code proposal}. Other modules reference it by id only (ADR 0001). */
@Entity
@Table(name = "proposal")
@Audited
public class Proposal extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "terms")
    private String terms;

    @Column(name = "fee_amount", precision = 14, scale = 2)
    private BigDecimal feeAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ProposalStatus status = ProposalStatus.DRAFT;

    @Column(name = "status_changed_at")
    private Instant statusChangedAt = Instant.now();

    @Column(name = "status_reason")
    private String statusReason;

    @Column(name = "sent_on")
    private LocalDate sentOn;

    @Column(name = "decided_on")
    private LocalDate decidedOn;

    @Column(name = "owner_id")
    private UUID ownerId;

    public Proposal() {
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

    public String getTerms() {
        return terms;
    }

    public void setTerms(String terms) {
        this.terms = terms;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public void setStatus(ProposalStatus status) {
        this.status = status;
    }

    public Instant getStatusChangedAt() {
        return statusChangedAt;
    }

    public void setStatusChangedAt(Instant statusChangedAt) {
        this.statusChangedAt = statusChangedAt;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public LocalDate getSentOn() {
        return sentOn;
    }

    public void setSentOn(LocalDate sentOn) {
        this.sentOn = sentOn;
    }

    public LocalDate getDecidedOn() {
        return decidedOn;
    }

    public void setDecidedOn(LocalDate decidedOn) {
        this.decidedOn = decidedOn;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

}
