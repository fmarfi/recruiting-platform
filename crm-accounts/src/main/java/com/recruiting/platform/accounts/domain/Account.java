package com.recruiting.platform.accounts.domain;

import jakarta.persistence.*;
import com.recruiting.platform.contracts.AccountStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/** Maps table {@code account}. Other modules reference it by id only (ADR 0001). */
@Entity
@Table(name = "account")
@Audited
public class Account extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "sector", length = 100)
    private String sector;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "website", length = 300)
    private String website;

    @Column(name = "notes")
    private String notes;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    @Column(name = "opened_on")
    private LocalDate openedOn;

    @NotAudited
    @Column(name = "open_job_count")
    private Integer openJobCount;

    @NotAudited
    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    public Account() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Instant getStatusChangedAt() {
        return statusChangedAt;
    }

    public void setStatusChangedAt(Instant statusChangedAt) {
        this.statusChangedAt = statusChangedAt;
    }

    public LocalDate getOpenedOn() {
        return openedOn;
    }

    public void setOpenedOn(LocalDate openedOn) {
        this.openedOn = openedOn;
    }

    public Integer getOpenJobCount() {
        return openJobCount;
    }

    public void setOpenJobCount(Integer openJobCount) {
        this.openJobCount = openJobCount;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

}
