package com.recruiting.platform.accounts.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Maps table {@code account_open_job}. Other modules reference it by id only (ADR 0001). */
@Entity
@Table(name = "account_open_job")
public class AccountOpenJob {

    @Id
    @Column(name = "job_order_id")
    private UUID jobOrderId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "open")
    private boolean open;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public AccountOpenJob() {
        // for JPA
    }

    public AccountOpenJob(UUID jobOrderId) {
        this.jobOrderId = jobOrderId;
    }

    public UUID getJobOrderId() {
        return jobOrderId;
    }

    public void setJobOrderId(UUID jobOrderId) {
        this.jobOrderId = jobOrderId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

}
