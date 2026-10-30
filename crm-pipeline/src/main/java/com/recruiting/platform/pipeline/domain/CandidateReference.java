package com.recruiting.platform.pipeline.domain;

import com.recruiting.platform.contracts.ReferenceStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;

/** Maps table {@code candidate_reference} (1.3.0, SX-010): a referee check. The referee's data is third-party data (ThirdPartyErasure matches on {@code lower(referee_email)}). */
@Entity
@Table(name = "candidate_reference")
@Audited
public class CandidateReference extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "person_id", updatable = false)
    private UUID personId;

    @Column(name = "job_order_id")
    private UUID jobOrderId;

    @Column(name = "referee_name", length = 200)
    private String refereeName;

    @Column(name = "referee_relationship", length = 200)
    private String refereeRelationship;

    @Column(name = "referee_company", length = 200)
    private String refereeCompany;

    @Column(name = "referee_email", length = 254)
    private String refereeEmail;

    @Column(name = "referee_phone", length = 50)
    private String refereePhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ReferenceStatus status = ReferenceStatus.TO_REQUEST;

    @Column(name = "summary")
    private String summary;

    @Column(name = "checked_on")
    private LocalDate checkedOn;

    @Column(name = "checked_by")
    private UUID checkedBy;

    @Column(name = "include_in_report")
    private boolean includeInReport;

    public CandidateReference() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public UUID getPersonId() {
        return personId;
    }
    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public UUID getJobOrderId() {
        return jobOrderId;
    }
    public void setJobOrderId(UUID jobOrderId) {
        this.jobOrderId = jobOrderId;
    }

    public String getRefereeName() {
        return refereeName;
    }
    public void setRefereeName(String refereeName) {
        this.refereeName = refereeName;
    }

    public String getRefereeRelationship() {
        return refereeRelationship;
    }
    public void setRefereeRelationship(String refereeRelationship) {
        this.refereeRelationship = refereeRelationship;
    }

    public String getRefereeCompany() {
        return refereeCompany;
    }
    public void setRefereeCompany(String refereeCompany) {
        this.refereeCompany = refereeCompany;
    }

    public String getRefereeEmail() {
        return refereeEmail;
    }
    public void setRefereeEmail(String refereeEmail) {
        this.refereeEmail = refereeEmail;
    }

    public String getRefereePhone() {
        return refereePhone;
    }
    public void setRefereePhone(String refereePhone) {
        this.refereePhone = refereePhone;
    }

    public ReferenceStatus getStatus() {
        return status;
    }
    public void setStatus(ReferenceStatus status) {
        this.status = status;
    }

    public String getSummary() {
        return summary;
    }
    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDate getCheckedOn() {
        return checkedOn;
    }
    public void setCheckedOn(LocalDate checkedOn) {
        this.checkedOn = checkedOn;
    }

    public UUID getCheckedBy() {
        return checkedBy;
    }
    public void setCheckedBy(UUID checkedBy) {
        this.checkedBy = checkedBy;
    }

    public boolean isIncludeInReport() {
        return includeInReport;
    }
    public void setIncludeInReport(boolean includeInReport) {
        this.includeInReport = includeInReport;
    }

}
