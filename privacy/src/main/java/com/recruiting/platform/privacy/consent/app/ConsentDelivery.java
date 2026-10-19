package com.recruiting.platform.privacy.consent.app;

import com.recruiting.platform.contracts.EmailDeliveryStatus;
import com.recruiting.platform.privacy.domain.ConsentRequestRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Stores the outcome of sending a consent e-mail (delivery runs after the request transaction has committed). */
@Component
class ConsentDelivery {

    private final ConsentRequestRepository requests;
    private final Clock clock;

    ConsentDelivery(ConsentRequestRepository requests, Clock clock) {
        this.requests = requests;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(UUID requestId) {
        requests.findById(requestId).ifPresent(r -> {
            r.setDeliveryStatus(EmailDeliveryStatus.SENT);
            r.setSentAt(clock.instant());
            r.setFailureReason(null);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID requestId, String reason) {
        requests.findById(requestId).ifPresent(r -> {
            r.setDeliveryStatus(EmailDeliveryStatus.FAILED);
            r.setFailureReason(reason.length() > 300 ? reason.substring(0, 300) : reason);
        });
    }
}
