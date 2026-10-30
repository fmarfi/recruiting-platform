package com.recruiting.platform.pipeline.domain;

import com.recruiting.platform.contracts.FeeTrigger;
import com.recruiting.platform.contracts.InvoiceState;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;

/** Maps table {@code fee_schedule_item} (1.3.0, PB-009, CO-020): one invoice milestone of an agreement. Amounts are net. */
@Entity
@Table(name = "fee_schedule_item")
@Audited
public class FeeScheduleItem extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "agreement_id", updatable = false)
    private UUID agreementId;

    @Column(name = "seq")
    private Short seq;

    @Column(name = "label", length = 200)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger")
    private FeeTrigger trigger;

    @Column(name = "offset_days")
    private Short offsetDays;

    @Column(name = "percent")
    private BigDecimal percent;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "due_days")
    private Short dueDays = (short) 30;

    @Column(name = "vat_applies")
    private boolean vatApplies = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_state")
    private InvoiceState invoiceState = InvoiceState.NOT_DUE;

    @Column(name = "triggered_at")
    private Instant triggeredAt;

    @Column(name = "due_on")
    private LocalDate dueOn;

    @Column(name = "einvoice_ref", length = 100)
    private String einvoiceRef;

    @Column(name = "invoiced_on")
    private LocalDate invoicedOn;

    @Column(name = "paid_on")
    private LocalDate paidOn;

    @Column(name = "invoiced_amount")
    private BigDecimal invoicedAmount;

    @Column(name = "state_reason", length = 500)
    private String stateReason;

    public FeeScheduleItem() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public UUID getAgreementId() {
        return agreementId;
    }
    public void setAgreementId(UUID agreementId) {
        this.agreementId = agreementId;
    }

    public Short getSeq() {
        return seq;
    }
    public void setSeq(Short seq) {
        this.seq = seq;
    }

    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }

    public FeeTrigger getTrigger() {
        return trigger;
    }
    public void setTrigger(FeeTrigger trigger) {
        this.trigger = trigger;
    }

    public Short getOffsetDays() {
        return offsetDays;
    }
    public void setOffsetDays(Short offsetDays) {
        this.offsetDays = offsetDays;
    }

    public BigDecimal getPercent() {
        return percent;
    }
    public void setPercent(BigDecimal percent) {
        this.percent = percent;
    }

    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Short getDueDays() {
        return dueDays;
    }
    public void setDueDays(Short dueDays) {
        this.dueDays = dueDays;
    }

    public boolean isVatApplies() {
        return vatApplies;
    }
    public void setVatApplies(boolean vatApplies) {
        this.vatApplies = vatApplies;
    }

    public InvoiceState getInvoiceState() {
        return invoiceState;
    }
    public void setInvoiceState(InvoiceState invoiceState) {
        this.invoiceState = invoiceState;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }
    public void setTriggeredAt(Instant triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public LocalDate getDueOn() {
        return dueOn;
    }
    public void setDueOn(LocalDate dueOn) {
        this.dueOn = dueOn;
    }

    public String getEinvoiceRef() {
        return einvoiceRef;
    }
    public void setEinvoiceRef(String einvoiceRef) {
        this.einvoiceRef = einvoiceRef;
    }

    public LocalDate getInvoicedOn() {
        return invoicedOn;
    }
    public void setInvoicedOn(LocalDate invoicedOn) {
        this.invoicedOn = invoicedOn;
    }

    public LocalDate getPaidOn() {
        return paidOn;
    }
    public void setPaidOn(LocalDate paidOn) {
        this.paidOn = paidOn;
    }

    public BigDecimal getInvoicedAmount() {
        return invoicedAmount;
    }
    public void setInvoicedAmount(BigDecimal invoicedAmount) {
        this.invoicedAmount = invoicedAmount;
    }

    public String getStateReason() {
        return stateReason;
    }
    public void setStateReason(String stateReason) {
        this.stateReason = stateReason;
    }

}
