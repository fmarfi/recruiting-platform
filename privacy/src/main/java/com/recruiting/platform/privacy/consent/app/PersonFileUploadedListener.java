package com.recruiting.platform.privacy.consent.app;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.PersonFileKind;
import com.recruiting.platform.contracts.PersonFileUploaded;
import com.recruiting.platform.privacy.retention.app.RetentionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * A CV was attached to a person (B-01): queue the KVKK notice and explicit consent e-mail when the setting
 * {@code autoSendOnCvUpload} is on (S-01, ADR 0011). Never to REFUSED or WITHDRAWN people and never while an earlier
 * request is still waiting for an answer; see {@link ConsentRequestService#sendAutomaticInitial}.
 */
@Component
class PersonFileUploadedListener {

    private static final Logger LOG = LoggerFactory.getLogger(PersonFileUploadedListener.class);

    private final ConsentRequestService requests;
    private final RetentionPolicy retention;

    PersonFileUploadedListener(ConsentRequestService requests, RetentionPolicy retention) {
        this.requests = requests;
        this.retention = retention;
    }

    @ApplicationModuleListener
    void on(PersonFileUploaded event) {
        if (event.kind() != PersonFileKind.CV || !retention.settings().isAutoSendOnCvUpload()) {
            return;
        }
        try {
            requests.sendAutomaticInitial(event.personId(), Language.TR);
        } catch (ApiException ex) {
            LOG.info("Automatic consent e-mail after CV upload skipped for person {}: {}", event.personId(),
                    ex.code());
        }
    }
}
