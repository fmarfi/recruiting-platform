package com.recruiting.platform.pipeline.domain;

import com.recruiting.platform.contracts.JobFunction;
import com.recruiting.platform.contracts.Seniority;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

/** Maps table {@code search_brief} (1.3.0, SX-001): one row per job order. The pay band is the BriefCompensation JSON encrypted by security.FieldCipher (R73), exposed here as the JSON string. */
@Entity
@Table(name = "search_brief")
@Audited
public class SearchBrief extends AuditableEntity {

    @Id
    @Column(name = "job_order_id")
    private UUID jobOrderId;

    @Column(name = "reporting_line", length = 200)
    private String reportingLine;

    @Column(name = "purpose")
    private String purpose;

    @Column(name = "ideal_profile")
    private String idealProfile;

    @Column(name = "notes")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "seniority_level")
    private Seniority seniorityLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "function_area")
    private JobFunction functionArea;

    @Convert(converter = EncryptedJsonConverter.class)
    @Column(name = "comp_band_enc")
    private String compBandJson;

    @Column(name = "team_size")
    private Short teamSize;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "languages")
    private List<Map<String,Object>> languages = new ArrayList<>();

    @Column(name = "kickoff_on")
    private LocalDate kickoffOn;

    @Column(name = "longlist_due_on")
    private LocalDate longlistDueOn;

    @Column(name = "shortlist_due_on")
    private LocalDate shortlistDueOn;

    @Column(name = "target_start_on")
    private LocalDate targetStartOn;

    @Column(name = "brief_version")
    private Integer briefVersion = 1;

    public SearchBrief() {
        // for JPA
    }

    public UUID getJobOrderId() {
        return jobOrderId;
    }

    public void setJobOrderId(UUID jobOrderId) {
        this.jobOrderId = jobOrderId;
    }

    public String getReportingLine() {
        return reportingLine;
    }
    public void setReportingLine(String reportingLine) {
        this.reportingLine = reportingLine;
    }

    public String getPurpose() {
        return purpose;
    }
    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getIdealProfile() {
        return idealProfile;
    }
    public void setIdealProfile(String idealProfile) {
        this.idealProfile = idealProfile;
    }

    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Seniority getSeniorityLevel() {
        return seniorityLevel;
    }
    public void setSeniorityLevel(Seniority seniorityLevel) {
        this.seniorityLevel = seniorityLevel;
    }

    public JobFunction getFunctionArea() {
        return functionArea;
    }
    public void setFunctionArea(JobFunction functionArea) {
        this.functionArea = functionArea;
    }

    public String getCompBandJson() {
        return compBandJson;
    }
    public void setCompBandJson(String compBandJson) {
        this.compBandJson = compBandJson;
    }

    public Short getTeamSize() {
        return teamSize;
    }
    public void setTeamSize(Short teamSize) {
        this.teamSize = teamSize;
    }

    public List<Map<String,Object>> getLanguages() {
        return languages;
    }
    public void setLanguages(List<Map<String,Object>> languages) {
        this.languages = languages;
    }

    public LocalDate getKickoffOn() {
        return kickoffOn;
    }
    public void setKickoffOn(LocalDate kickoffOn) {
        this.kickoffOn = kickoffOn;
    }

    public LocalDate getLonglistDueOn() {
        return longlistDueOn;
    }
    public void setLonglistDueOn(LocalDate longlistDueOn) {
        this.longlistDueOn = longlistDueOn;
    }

    public LocalDate getShortlistDueOn() {
        return shortlistDueOn;
    }
    public void setShortlistDueOn(LocalDate shortlistDueOn) {
        this.shortlistDueOn = shortlistDueOn;
    }

    public LocalDate getTargetStartOn() {
        return targetStartOn;
    }
    public void setTargetStartOn(LocalDate targetStartOn) {
        this.targetStartOn = targetStartOn;
    }

    public Integer getBriefVersion() {
        return briefVersion;
    }
    public void setBriefVersion(Integer briefVersion) {
        this.briefVersion = briefVersion;
    }

}
