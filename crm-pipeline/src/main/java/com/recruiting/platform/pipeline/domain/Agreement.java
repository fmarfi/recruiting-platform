package com.recruiting.platform.pipeline.domain;

import com.recruiting.platform.contracts.AgreementKind;
import com.recruiting.platform.contracts.AgreementStatus;
import com.recruiting.platform.contracts.Exclusivity;
import com.recruiting.platform.contracts.FeeModel;
import com.recruiting.platform.contracts.GuaranteeStartBasis;
import com.recruiting.platform.contracts.Language;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

/** Maps table {@code agreement} (1.3.0, PB-007/008): terms of business, engagement letter or amendment. {@code signatoryContactId} is set null when the contact goes with a deleted person. */
@Entity
@Table(name = "agreement")
@Audited
public class Agreement extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "account_id", updatable = false)
    private UUID accountId;

    @Column(name = "job_order_id")
    private UUID jobOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind")
    private AgreementKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AgreementStatus status = AgreementStatus.DRAFT;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "signed_on")
    private LocalDate signedOn;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "language")
    private Language language;

    @Column(name = "governing_law", length = 100)
    private String governingLaw;

    @Enumerated(EnumType.STRING)
    @Column(name = "exclusivity")
    private Exclusivity exclusivity;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_model")
    private FeeModel feeModel;

    @Column(name = "fee_percent")
    private BigDecimal feePercent;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "fee_basis", length = 40)
    private List<String> feeBasis = new ArrayList<>();

    @Column(name = "flat_fee_amount")
    private BigDecimal flatFeeAmount;

    @Column(name = "estimated_fee")
    private BigDecimal estimatedFee;

    @Column(name = "fee_minimum")
    private BigDecimal feeMinimum;

    @Column(name = "fee_cap")
    private BigDecimal feeCap;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "payment_terms_days")
    private Short paymentTermsDays = (short) 30;

    @Column(name = "guarantee_months")
    private Short guaranteeMonths = (short) 6;

    @Enumerated(EnumType.STRING)
    @Column(name = "guarantee_start_basis")
    private GuaranteeStartBasis guaranteeStartBasis = GuaranteeStartBasis.START_DATE;

    @Column(name = "signed_file_id")
    private UUID signedFileId;

    @Column(name = "signed_file_name", length = 255)
    private String signedFileName;

    @Column(name = "signed_file_size")
    private Long signedFileSize;

    @Column(name = "signed_file_uploaded_at")
    private Instant signedFileUploadedAt;

    @Column(name = "signed_file_uploaded_by")
    private UUID signedFileUploadedBy;

    @Column(name = "guarantee_terms", length = 2000)
    private String guaranteeTerms;

    @Column(name = "confidentiality_terms", length = 2000)
    private String confidentialityTerms;

    @Column(name = "expense_policy", length = 1000)
    private String expensePolicy;

    @Column(name = "off_limits_months")
    private Short offLimitsMonths;

    @Column(name = "signatory_contact_id")
    private UUID signatoryContactId;

    @Column(name = "billing_account_id")
    private UUID billingAccountId;

    @Column(name = "notes")
    private String notes;

    public Agreement() {
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

    public UUID getJobOrderId() {
        return jobOrderId;
    }
    public void setJobOrderId(UUID jobOrderId) {
        this.jobOrderId = jobOrderId;
    }

    public AgreementKind getKind() {
        return kind;
    }
    public void setKind(AgreementKind kind) {
        this.kind = kind;
    }

    public AgreementStatus getStatus() {
        return status;
    }
    public void setStatus(AgreementStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getSignedOn() {
        return signedOn;
    }
    public void setSignedOn(LocalDate signedOn) {
        this.signedOn = signedOn;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }
    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }
    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public Language getLanguage() {
        return language;
    }
    public void setLanguage(Language language) {
        this.language = language;
    }

    public String getGoverningLaw() {
        return governingLaw;
    }
    public void setGoverningLaw(String governingLaw) {
        this.governingLaw = governingLaw;
    }

    public Exclusivity getExclusivity() {
        return exclusivity;
    }
    public void setExclusivity(Exclusivity exclusivity) {
        this.exclusivity = exclusivity;
    }

    public FeeModel getFeeModel() {
        return feeModel;
    }
    public void setFeeModel(FeeModel feeModel) {
        this.feeModel = feeModel;
    }

    public BigDecimal getFeePercent() {
        return feePercent;
    }
    public void setFeePercent(BigDecimal feePercent) {
        this.feePercent = feePercent;
    }

    public List<String> getFeeBasis() {
        return feeBasis;
    }
    public void setFeeBasis(List<String> feeBasis) {
        this.feeBasis = feeBasis;
    }

    public BigDecimal getFlatFeeAmount() {
        return flatFeeAmount;
    }
    public void setFlatFeeAmount(BigDecimal flatFeeAmount) {
        this.flatFeeAmount = flatFeeAmount;
    }

    public BigDecimal getEstimatedFee() {
        return estimatedFee;
    }
    public void setEstimatedFee(BigDecimal estimatedFee) {
        this.estimatedFee = estimatedFee;
    }

    public BigDecimal getFeeMinimum() {
        return feeMinimum;
    }
    public void setFeeMinimum(BigDecimal feeMinimum) {
        this.feeMinimum = feeMinimum;
    }

    public BigDecimal getFeeCap() {
        return feeCap;
    }
    public void setFeeCap(BigDecimal feeCap) {
        this.feeCap = feeCap;
    }

    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Short getPaymentTermsDays() {
        return paymentTermsDays;
    }
    public void setPaymentTermsDays(Short paymentTermsDays) {
        this.paymentTermsDays = paymentTermsDays;
    }

    public Short getGuaranteeMonths() {
        return guaranteeMonths;
    }
    public void setGuaranteeMonths(Short guaranteeMonths) {
        this.guaranteeMonths = guaranteeMonths;
    }

    public GuaranteeStartBasis getGuaranteeStartBasis() {
        return guaranteeStartBasis;
    }
    public void setGuaranteeStartBasis(GuaranteeStartBasis guaranteeStartBasis) {
        this.guaranteeStartBasis = guaranteeStartBasis;
    }

    public UUID getSignedFileId() {
        return signedFileId;
    }
    public void setSignedFileId(UUID signedFileId) {
        this.signedFileId = signedFileId;
    }

    public String getSignedFileName() {
        return signedFileName;
    }
    public void setSignedFileName(String signedFileName) {
        this.signedFileName = signedFileName;
    }

    public Long getSignedFileSize() {
        return signedFileSize;
    }
    public void setSignedFileSize(Long signedFileSize) {
        this.signedFileSize = signedFileSize;
    }

    public Instant getSignedFileUploadedAt() {
        return signedFileUploadedAt;
    }
    public void setSignedFileUploadedAt(Instant signedFileUploadedAt) {
        this.signedFileUploadedAt = signedFileUploadedAt;
    }

    public UUID getSignedFileUploadedBy() {
        return signedFileUploadedBy;
    }
    public void setSignedFileUploadedBy(UUID signedFileUploadedBy) {
        this.signedFileUploadedBy = signedFileUploadedBy;
    }

    public String getGuaranteeTerms() {
        return guaranteeTerms;
    }
    public void setGuaranteeTerms(String guaranteeTerms) {
        this.guaranteeTerms = guaranteeTerms;
    }

    public String getConfidentialityTerms() {
        return confidentialityTerms;
    }
    public void setConfidentialityTerms(String confidentialityTerms) {
        this.confidentialityTerms = confidentialityTerms;
    }

    public String getExpensePolicy() {
        return expensePolicy;
    }
    public void setExpensePolicy(String expensePolicy) {
        this.expensePolicy = expensePolicy;
    }

    public Short getOffLimitsMonths() {
        return offLimitsMonths;
    }
    public void setOffLimitsMonths(Short offLimitsMonths) {
        this.offLimitsMonths = offLimitsMonths;
    }

    public UUID getSignatoryContactId() {
        return signatoryContactId;
    }
    public void setSignatoryContactId(UUID signatoryContactId) {
        this.signatoryContactId = signatoryContactId;
    }

    public UUID getBillingAccountId() {
        return billingAccountId;
    }
    public void setBillingAccountId(UUID billingAccountId) {
        this.billingAccountId = billingAccountId;
    }

    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }

}
