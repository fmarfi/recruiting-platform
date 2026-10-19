package com.recruiting.platform.privacy.person.app;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.PersonCreated;
import com.recruiting.platform.privacy.consent.app.ConsentRequestService;
import com.recruiting.platform.privacy.domain.PersonPrivacy;
import com.recruiting.platform.privacy.domain.PersonPrivacyRepository;
import com.recruiting.platform.privacy.domain.RetentionSettings;
import com.recruiting.platform.privacy.retention.app.RetentionPolicy;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * A person was added (A-12): create the privacy record, start the retention clock and, per the retention settings,
 * queue the KVKK notice and consent e-mail (S-01, NFR-01). Every person with an e-mail address gets the notice
 * unless the settings switch the automation off for their type.
 */
@Component
class PersonCreatedListener {

    private static final Logger LOG = LoggerFactory.getLogger(PersonCreatedListener.class);

    private final PersonPrivacyRepository privacy;
    private final RetentionPolicy retention;
    private final PersonGateway people;
    private final ConsentRequestService requests;

    PersonCreatedListener(PersonPrivacyRepository privacy, RetentionPolicy retention, PersonGateway people,
            ConsentRequestService requests) {
        this.privacy = privacy;
        this.retention = retention;
        this.people = people;
        this.requests = requests;
    }

    @ApplicationModuleListener
    void on(PersonCreated event) {
        PersonPrivacy record = privacy.findById(event.personId())
                .orElseGet(() -> retention.create(event.personId(), people.find(event.personId()).orElse(null)));
        if (event.importId() != null && record.getImportId() == null) {
            record.setImportId(event.importId());
            privacy.save(record);
        }
        RetentionSettings settings = retention.settings();
        boolean wanted = settings.isAutoSendNotice() && event.hasEmail()
                && Arrays.asList(settings.getAutoSendPersonTypes().split(",")).contains(event.type().name());
        if (wanted) {
            try {
                requests.send(event.personId(), Language.TR, null);
            } catch (ApiException ex) {
                LOG.info("Automatic consent e-mail skipped for person {}: {}", event.personId(), ex.code());
            }
        }
    }
}
