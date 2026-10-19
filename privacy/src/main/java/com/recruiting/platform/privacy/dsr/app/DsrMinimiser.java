package com.recruiting.platform.privacy.dsr.app;

import com.recruiting.platform.privacy.domain.DataSubjectRequest;
import com.recruiting.platform.privacy.domain.DataSubjectRequestRepository;
import com.recruiting.platform.privacy.shared.HmacService;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR 0009 step 1: a data-subject request is kept as evidence that it was handled, but minimised. Name becomes a
 * placeholder, the e-mail a keyed hash, the description "[removed]". Done through JPA so Envers records it and the
 * later audit purge can rewrite the current revision to the same values.
 */
@Component
class DsrMinimiser {

    static final String NAME = "Anonymised person";
    static final String REMOVED = "[removed]";

    private final DataSubjectRequestRepository requests;
    private final HmacService hmac;

    DsrMinimiser(DataSubjectRequestRepository requests, HmacService hmac) {
        this.requests = requests;
        this.hmac = hmac;
    }

    @Transactional
    public int minimise(UUID personId) {
        int count = 0;
        for (DataSubjectRequest request : requests.findByPersonId(personId)) {
            if (!request.getRequesterEmail().startsWith("hmac:")) {
                request.setRequesterEmail(hmac.hashEmail(request.getRequesterEmail()));
            }
            request.setRequesterName(NAME);
            request.setDescription(REMOVED);
            count++;
        }
        return count;
    }
}
