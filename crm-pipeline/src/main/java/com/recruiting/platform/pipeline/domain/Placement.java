package com.recruiting.platform.pipeline.domain;

import com.recruiting.platform.contracts.PlacementStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

/** Maps table {@code placement} (1.3.0, CO-021, PB-011): a successful placement. {@code personId} and {@code applicationId} are set null when the person is deleted (commercial evidence outlives the person). The compensation is the PlacementCompensation JSON encrypted by security.FieldCipher. */
@Entity
@Table(name = "placement")
@Audited
public class Placement extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "application_id")
    private UUID applicationId;

    @Column(name = "job_order_id", updatable = false)
    private UUID jobOrderId;

    @Column(name = "account_id", updatable = false)
    private UUID accountId;

    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "agreement_id")
    private UUID agreementId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PlacementStatus status = PlacementStatus.ACTIVE;

    @Column(name = "offer_accepted_on")
    private LocalDate offerAcceptedOn;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Convert(converter = EncryptedJsonConverter.class)
    @Column(name = "compensation_enc")
    private String compensationJson;

    @Column(name = "fee_amount")
    private BigDecimal feeAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "fee_currency", length = 3)
    private String feeCurrency;

    @Column(name = "fee_overridden")
    private boolean feeOverridden;

    @Column(name = "guarantee_end_on")
    private LocalDate guaranteeEndOn;

    @Column(name = "left_on")
    private LocalDate leftOn;

    @Column(name = "replacement_job_order_id")
    private UUID replacementJobOrderId;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    public Placement() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }
    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public UUID getJobOrderId() {
        return jobOrderId;
    }
    public void setJobOrderId(UUID jobOrderId) {
        this.jobOrderId = jobOrderId;
    }

    public UUID getAccountId() {
        return accountId;
    }
    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getPersonId() {
        return personId;
    }
    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public UUID getAgreementId() {
        return agreementId;
    }
    public void setAgreementId(UUID agreementId) {
        this.agreementId = agreementId;
    }

    public PlacementStatus getStatus() {
        return status;
    }
    public void setStatus(PlacementStatus status) {
        this.status = status;
    }

    public LocalDate getOfferAcceptedOn() {
        return offerAcceptedOn;
    }
    public void setOfferAcceptedOn(LocalDate offerAcceptedOn) {
        this.offerAcceptedOn = offerAcceptedOn;
    }

    public LocalDate getStartDate() {
        return startDate;
    }
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public String getCompensationJson() {
        return compensationJson;
    }
    public void setCompensationJson(String compensationJson) {
        this.compensationJson = compensationJson;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }
    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public String getFeeCurrency() {
        return feeCurrency;
    }
    public void setFeeCurrency(String feeCurrency) {
        this.feeCurrency = feeCurrency;
    }

    public boolean isFeeOverridden() {
        return feeOverridden;
    }
    public void setFeeOverridden(boolean feeOverridden) {
        this.feeOverridden = feeOverridden;
    }

    public LocalDate getGuaranteeEndOn() {
        return guaranteeEndOn;
    }
    public void setGuaranteeEndOn(LocalDate guaranteeEndOn) {
        this.guaranteeEndOn = guaranteeEndOn;
    }

    public LocalDate getLeftOn() {
        return leftOn;
    }
    public void setLeftOn(LocalDate leftOn) {
        this.leftOn = leftOn;
    }

    public UUID getReplacementJobOrderId() {
        return replacementJobOrderId;
    }
    public void setReplacementJobOrderId(UUID replacementJobOrderId) {
        this.replacementJobOrderId = replacementJobOrderId;
    }

    public String getStatusReason() {
        return statusReason;
    }
    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

}
