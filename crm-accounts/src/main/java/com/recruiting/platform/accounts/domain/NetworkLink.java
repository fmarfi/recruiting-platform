package com.recruiting.platform.accounts.domain;

import jakarta.persistence.*;
import com.recruiting.platform.contracts.LinkType;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;

/** Maps table {@code network_link}. Other modules reference it by id only (ADR 0001). */
@Entity
@Table(name = "network_link")
@Audited
public class NetworkLink extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "from_person_id")
    private UUID fromPersonId;

    @Column(name = "to_person_id")
    private UUID toPersonId;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private LinkType type;

    @Column(name = "note")
    private String note;

    public NetworkLink() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public UUID getFromPersonId() {
        return fromPersonId;
    }

    public void setFromPersonId(UUID fromPersonId) {
        this.fromPersonId = fromPersonId;
    }

    public UUID getToPersonId() {
        return toPersonId;
    }

    public void setToPersonId(UUID toPersonId) {
        this.toPersonId = toPersonId;
    }

    public LinkType getType() {
        return type;
    }

    public void setType(LinkType type) {
        this.type = type;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

}
