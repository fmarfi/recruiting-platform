package com.recruiting.platform.privacy.retention.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** The nightly retention run (default 02:30, {@code app.privacy.retention-cron}); logged in retention_run. */
@Component
class RetentionJob {

    private static final Logger LOG = LoggerFactory.getLogger(RetentionJob.class);

    private final RetentionRunService service;

    RetentionJob(RetentionRunService service) {
        this.service = service;
    }

    @Scheduled(cron = "${app.privacy.retention-cron:0 30 2 * * *}")
    void nightly() {
        try {
            var run = service.run(false, null);
            LOG.info("Retention run {}: flagged {}, anonymised {}, deleted {}", run.getId(), run.getFlaggedCount(),
                    run.getAnonymisedCount(), run.getDeletedCount());
        } catch (RuntimeException ex) {
            LOG.warn("Nightly retention run skipped: {}", ex.getClass().getSimpleName());
        }
    }
}
