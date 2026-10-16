package com.recruiting.platform.privacy.domain;

import jakarta.persistence.*;
import com.recruiting.platform.contracts.RetentionAction;
import com.recruiting.platform.contracts.RetentionBasis;
import org.hibernate.envers.Audited;

/** Maps table {@code retention_settings}. Other modules reference it by id only (ADR 0001). */
@Entity
@Table(name = "retention_settings")
@Audited
public class RetentionSettings extends AuditableEntity {

    @Id
    private Short id = 1;

    @Column(name = "retention_months")
    private Integer retentionMonths = 24;

    @Column(name = "basis")
    @Enumerated(EnumType.STRING)
    private RetentionBasis basis = RetentionBasis.FROM_CREATED;

    @Column(name = "action")
    @Enumerated(EnumType.STRING)
    private RetentionAction action = RetentionAction.ANONYMISE;

    @Column(name = "warning_days")
    private Integer warningDays = 30;

    @Column(name = "auto_send_notice")
    private boolean autoSendNotice = true;

    @Column(name = "auto_send_person_types", length = 200)
    private String autoSendPersonTypes = "CANDIDATE,EXECUTIVE";

    @Column(name = "renew_on_consent")
    private boolean renewOnConsent = true;

    public RetentionSettings() {
        // for JPA
    }

    public Short getId() {
        return id;
    }

    public void setId(Short id) {
        this.id = id;
    }

    public Integer getRetentionMonths() {
        return retentionMonths;
    }

    public void setRetentionMonths(Integer retentionMonths) {
        this.retentionMonths = retentionMonths;
    }

    public RetentionBasis getBasis() {
        return basis;
    }

    public void setBasis(RetentionBasis basis) {
        this.basis = basis;
    }

    public RetentionAction getAction() {
        return action;
    }

    public void setAction(RetentionAction action) {
        this.action = action;
    }

    public Integer getWarningDays() {
        return warningDays;
    }

    public void setWarningDays(Integer warningDays) {
        this.warningDays = warningDays;
    }

    public boolean isAutoSendNotice() {
        return autoSendNotice;
    }

    public void setAutoSendNotice(boolean autoSendNotice) {
        this.autoSendNotice = autoSendNotice;
    }

    public String getAutoSendPersonTypes() {
        return autoSendPersonTypes;
    }

    public void setAutoSendPersonTypes(String autoSendPersonTypes) {
        this.autoSendPersonTypes = autoSendPersonTypes;
    }

    public boolean isRenewOnConsent() {
        return renewOnConsent;
    }

    public void setRenewOnConsent(boolean renewOnConsent) {
        this.renewOnConsent = renewOnConsent;
    }

}
