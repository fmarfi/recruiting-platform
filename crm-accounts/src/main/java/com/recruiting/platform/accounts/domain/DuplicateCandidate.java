package com.recruiting.platform.accounts.domain;

import com.recruiting.platform.contracts.DuplicateStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/**
 * Maps table {@code duplicate_candidate} (1.2.0, not audited). The database enforces
 * {@code person_a_id < person_b_id}: use {@link #of} to order the pair. A DISMISSED pair is never re-opened.
 */
@Entity
@Table(name = "duplicate_candidate")
public class DuplicateCandidate {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "person_a_id")
    private UUID personAId;

    @Column(name = "person_b_id")
    private UUID personBId;

    /** {@code DuplicateMatchReason} names, at least one. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "reasons", length = 40)
    private List<String> reasons = new ArrayList<>();

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DuplicateStatus status = DuplicateStatus.OPEN;

    @Column(name = "detected_at")
    private Instant detectedAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    public DuplicateCandidate() {
        // for JPA
    }

    /** Postgres orders uuid values as unsigned bytes; java.util.UUID.compareTo is signed, so compare by hand. */
    public static boolean before(UUID one, UUID other) {
        int c = Long.compareUnsigned(one.getMostSignificantBits(), other.getMostSignificantBits());
        if (c == 0) {
            c = Long.compareUnsigned(one.getLeastSignificantBits(), other.getLeastSignificantBits());
        }
        return c < 0;
    }

    /** Orders the two ids so that a < b, as the CHECK requires. */
    public static DuplicateCandidate of(UUID one, UUID other, List<String> reasons) {
        DuplicateCandidate d = new DuplicateCandidate();
        boolean oneFirst = before(one, other);
        d.personAId = oneFirst ? one : other;
        d.personBId = oneFirst ? other : one;
        d.reasons = new ArrayList<>(reasons);
        return d;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPersonAId() {
        return personAId;
    }

    public UUID getPersonBId() {
        return personBId;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = new ArrayList<>(reasons);
    }

    public DuplicateStatus getStatus() {
        return status;
    }

    public void setStatus(DuplicateStatus status) {
        this.status = status;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public UUID getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(UUID resolvedBy) {
        this.resolvedBy = resolvedBy;
    }
}
