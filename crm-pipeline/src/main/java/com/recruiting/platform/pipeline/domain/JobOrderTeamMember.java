package com.recruiting.platform.pipeline.domain;

import com.recruiting.platform.contracts.SearchTeamRole;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.envers.Audited;
import org.springframework.data.domain.Persistable;

/** Maps table {@code job_order_team_member} (1.3.0): the search team, replaced as a whole. */
@Entity
@Table(name = "job_order_team_member")
@Audited
public class JobOrderTeamMember implements Persistable<JobOrderTeamMember.Key> {

    @EmbeddedId
    private Key id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private SearchTeamRole role;

    @Column(name = "added_at")
    private Instant addedAt;

    @Transient
    private boolean isNew = true;

    public JobOrderTeamMember() {
        // for JPA
    }

    public JobOrderTeamMember(UUID jobOrderId, UUID userId) {
        this.id = new Key(jobOrderId, userId);
    }

    @Override
    public Key getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public UUID getJobOrderId() {
        return id.jobOrderId;
    }

    public UUID getUserId() {
        return id.userId;
    }

    public SearchTeamRole getRole() {
        return role;
    }
    public void setRole(SearchTeamRole role) {
        this.role = role;
    }

    public Instant getAddedAt() {
        return addedAt;
    }
    public void setAddedAt(Instant addedAt) {
        this.addedAt = addedAt;
    }

    /** Composite primary key. */
    @Embeddable
    public static class Key implements Serializable {

        @Column(name = "job_order_id")
        private UUID jobOrderId;

        @Column(name = "user_id")
        private UUID userId;

        public Key() {
            // for JPA
        }

        public Key(UUID jobOrderId, UUID userId) {
            this.jobOrderId = jobOrderId;
            this.userId = userId;
        }

        public UUID getJobOrderId() {
            return jobOrderId;
        }

        public UUID getUserId() {
            return userId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key other
                    && Objects.equals(jobOrderId, other.jobOrderId)
                    && Objects.equals(userId, other.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobOrderId, userId);
        }
    }
}
