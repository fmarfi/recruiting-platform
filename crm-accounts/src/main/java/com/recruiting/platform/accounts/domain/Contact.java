package com.recruiting.platform.accounts.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;

/** Maps table {@code contact}. Other modules reference it by id only (ADR 0001). */
@Entity
@Table(name = "contact")
@Audited
public class Contact extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "role", length = 200)
    private String role;

    @Column(name = "is_primary")
    private boolean primary;

    @Column(name = "ended_on")
    private LocalDate endedOn;

    public Contact() {
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

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public LocalDate getEndedOn() {
        return endedOn;
    }

    public void setEndedOn(LocalDate endedOn) {
        this.endedOn = endedOn;
    }

}
