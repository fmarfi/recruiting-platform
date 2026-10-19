package com.recruiting.platform.privacy.consent.app;

import jakarta.mail.internet.MimeMessage;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Sends consent e-mails after the request transaction has committed, on a virtual thread, and records the result.
 * The one-time token only lives in memory here: it is not stored, not published as an event and not logged, so
 * a lost mail is fixed by sending a new request.
 */
@Component
class ConsentMailDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(ConsentMailDispatcher.class);

    /** Everything needed to send one mail. */
    record Job(UUID requestId, String to, String subject, String body) {
    }

    private final JavaMailSender mailSender;
    private final ConsentDelivery delivery;
    private final String from;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    ConsentMailDispatcher(JavaMailSender mailSender, ConsentDelivery delivery,
            @Value("${app.mail.from:no-reply@recruiting.local}") String from) {
        this.mailSender = mailSender;
        this.delivery = delivery;
        this.from = from;
    }

    void sendAfterCommit(Job job) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.execute(() -> send(job));
                }
            });
        } else {
            executor.execute(() -> send(job));
        }
    }

    private void send(Job job) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(job.to());
            helper.setSubject(job.subject());
            helper.setText(job.body(), false);
            mailSender.send(message);
            delivery.markSent(job.requestId());
        } catch (Exception ex) {
            LOG.warn("Consent e-mail for request {} failed: {}", job.requestId(), ex.getClass().getSimpleName());
            // Only class names: the message may contain the address.
            String cause = ex.getCause() == null ? "" : " / " + ex.getCause().getClass().getSimpleName();
            delivery.markFailed(job.requestId(), ex.getClass().getSimpleName() + cause);
        }
    }
}
