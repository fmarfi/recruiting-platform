package com.recruiting.platform.accounts.account.app;

import com.recruiting.platform.accounts.domain.AccountOpenJob;
import com.recruiting.platform.accounts.domain.AccountOpenJobRepository;
import com.recruiting.platform.accounts.domain.AccountRepository;
import com.recruiting.platform.contracts.ActivityLogged;
import com.recruiting.platform.contracts.JobOrderCreated;
import com.recruiting.platform.contracts.JobOrderStatus;
import com.recruiting.platform.contracts.JobOrderStatusChanged;
import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Pipeline facts kept on the account (contracts/events.md): the account_open_job projection that guards closing
 * an account (A-02), open_job_count, and last_activity_at. Idempotent: upsert by job order id, count recomputed.
 */
@Component
public class AccountEventListener {

    private final AccountOpenJobRepository openJobs;
    private final AccountRepository accounts;

    public AccountEventListener(AccountOpenJobRepository openJobs, AccountRepository accounts) {
        this.openJobs = openJobs;
        this.accounts = accounts;
    }

    @ApplicationModuleListener
    void on(JobOrderCreated e) {
        upsert(e.jobOrderId(), e.accountId(), e.title(), e.status());
    }

    @ApplicationModuleListener
    void on(JobOrderStatusChanged e) {
        AccountOpenJob job = openJobs.findById(e.jobOrderId()).orElse(null);
        upsert(e.jobOrderId(), e.accountId(), job == null ? null : job.getTitle(), e.to());
    }

    @ApplicationModuleListener
    void on(ActivityLogged e) {
        if (e.accountId() != null) {
            accounts.bumpLastActivity(e.accountId(), e.activityAt());
        }
    }

    private void upsert(UUID jobOrderId, UUID accountId, String title, JobOrderStatus status) {
        AccountOpenJob job = openJobs.findById(jobOrderId).orElseGet(() -> new AccountOpenJob(jobOrderId));
        job.setAccountId(accountId);
        if (title != null) {
            job.setTitle(title);
        }
        job.setOpen(status.isOpen());
        job.setUpdatedAt(Instant.now());
        openJobs.saveAndFlush(job);
        accounts.updateOpenJobCount(accountId, (int) openJobs.countByAccountIdAndOpenTrue(accountId));
    }
}
