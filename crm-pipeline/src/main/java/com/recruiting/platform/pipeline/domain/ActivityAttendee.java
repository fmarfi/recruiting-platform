package com.recruiting.platform.pipeline.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.envers.Audited;
import org.springframework.data.domain.Persistable;

/** Maps table {@code activity_attendee} (1.3.0, R70): other people present at a call or meeting. Both FKs cascade; the row is the attendee's own data. */
@Entity
@Table(name = "activity_attendee")
@Audited
public class ActivityAttendee implements Persistable<ActivityAttendee.Key> {

    @EmbeddedId
    private Key id;

    @Transient
    private boolean isNew = true;

    public ActivityAttendee() {
        // for JPA
    }

    public ActivityAttendee(UUID activityId, UUID personId) {
        this.id = new Key(activityId, personId);
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

    public UUID getActivityId() {
        return id.activityId;
    }

    public UUID getPersonId() {
        return id.personId;
    }

    /** Composite primary key. */
    @Embeddable
    public static class Key implements Serializable {

        @Column(name = "activity_id")
        private UUID activityId;

        @Column(name = "person_id")
        private UUID personId;

        public Key() {
            // for JPA
        }

        public Key(UUID activityId, UUID personId) {
            this.activityId = activityId;
            this.personId = personId;
        }

        public UUID getActivityId() {
            return activityId;
        }

        public UUID getPersonId() {
            return personId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key other
                    && Objects.equals(activityId, other.activityId)
                    && Objects.equals(personId, other.personId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(activityId, personId);
        }
    }
}
