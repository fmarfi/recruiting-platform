package com.recruiting.platform.pipeline.domain;

import com.recruiting.platform.contracts.InterviewType;
import com.recruiting.platform.contracts.Recommendation;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

/** Maps table {@code candidate_evaluation} (1.3.0, SX-008/017): structured assessment of a person, optionally per search. Array columns hold names of {@code MotivationTag} and {@code EvaluationBlock}. */
@Entity
@Table(name = "candidate_evaluation")
@Audited
public class CandidateEvaluation extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "person_id", updatable = false)
    private UUID personId;

    @Column(name = "job_order_id")
    private UUID jobOrderId;

    @Column(name = "application_id")
    private UUID applicationId;

    @Column(name = "evaluator_id")
    private UUID evaluatorId;

    @Column(name = "evaluated_on")
    private LocalDate evaluatedOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type")
    private InterviewType interviewType;

    @Column(name = "overall_rating")
    private Short overallRating;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation")
    private Recommendation recommendation;

    @Column(name = "recommendation_reason", length = 500)
    private String recommendationReason;

    @Column(name = "summary", length = 600)
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "key_achievements")
    private List<String> keyAchievements = new ArrayList<>();

    @Column(name = "motivation", length = 400)
    private String motivation;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "motivation_tags", length = 40)
    private List<String> motivationTags = new ArrayList<>();

    @Column(name = "concerns", length = 400)
    private String concerns;

    @Column(name = "other_processes", length = 1000)
    private String otherProcesses;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scores")
    private List<Map<String,Object>> scores = new ArrayList<>();

    @Column(name = "private_notes")
    private String privateNotes;

    @Column(name = "internal_only")
    private boolean internalOnly;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "report_blocks", length = 40)
    private List<String> reportBlocks = new ArrayList<>(List.of("SUMMARY","ACHIEVEMENTS","MOTIVATION","CONCERNS"));

    public CandidateEvaluation() {
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

    public UUID getApplicationId() {
        return applicationId;
    }
    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public UUID getEvaluatorId() {
        return evaluatorId;
    }
    public void setEvaluatorId(UUID evaluatorId) {
        this.evaluatorId = evaluatorId;
    }

    public LocalDate getEvaluatedOn() {
        return evaluatedOn;
    }
    public void setEvaluatedOn(LocalDate evaluatedOn) {
        this.evaluatedOn = evaluatedOn;
    }

    public InterviewType getInterviewType() {
        return interviewType;
    }
    public void setInterviewType(InterviewType interviewType) {
        this.interviewType = interviewType;
    }

    public Short getOverallRating() {
        return overallRating;
    }
    public void setOverallRating(Short overallRating) {
        this.overallRating = overallRating;
    }

    public Recommendation getRecommendation() {
        return recommendation;
    }
    public void setRecommendation(Recommendation recommendation) {
        this.recommendation = recommendation;
    }

    public String getRecommendationReason() {
        return recommendationReason;
    }
    public void setRecommendationReason(String recommendationReason) {
        this.recommendationReason = recommendationReason;
    }

    public String getSummary() {
        return summary;
    }
    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getKeyAchievements() {
        return keyAchievements;
    }
    public void setKeyAchievements(List<String> keyAchievements) {
        this.keyAchievements = keyAchievements;
    }

    public String getMotivation() {
        return motivation;
    }
    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

    public List<String> getMotivationTags() {
        return motivationTags;
    }
    public void setMotivationTags(List<String> motivationTags) {
        this.motivationTags = motivationTags;
    }

    public String getConcerns() {
        return concerns;
    }
    public void setConcerns(String concerns) {
        this.concerns = concerns;
    }

    public String getOtherProcesses() {
        return otherProcesses;
    }
    public void setOtherProcesses(String otherProcesses) {
        this.otherProcesses = otherProcesses;
    }

    public List<Map<String,Object>> getScores() {
        return scores;
    }
    public void setScores(List<Map<String,Object>> scores) {
        this.scores = scores;
    }

    public String getPrivateNotes() {
        return privateNotes;
    }
    public void setPrivateNotes(String privateNotes) {
        this.privateNotes = privateNotes;
    }

    public boolean isInternalOnly() {
        return internalOnly;
    }
    public void setInternalOnly(boolean internalOnly) {
        this.internalOnly = internalOnly;
    }

    public List<String> getReportBlocks() {
        return reportBlocks;
    }
    public void setReportBlocks(List<String> reportBlocks) {
        this.reportBlocks = reportBlocks;
    }

}
