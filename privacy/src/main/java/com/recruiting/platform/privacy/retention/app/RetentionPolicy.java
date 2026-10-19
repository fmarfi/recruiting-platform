package com.recruiting.platform.privacy.retention.app;

import com.recruiting.platform.privacy.domain.PersonPrivacy;
import com.recruiting.platform.privacy.domain.PersonPrivacyRepository;
import com.recruiting.platform.privacy.domain.RetentionSettings;
import com.recruiting.platform.privacy.domain.RetentionSettingsRepository;
import com.recruiting.platform.privacy.person.app.PersonGateway;
import com.recruiting.platform.privacy.person.app.PersonInfo;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Computes retention dates from the settings (NFR-03) and creates missing privacy records. */
@Component
public class RetentionPolicy {

    private final RetentionSettingsRepository settings;
    private final PersonPrivacyRepository privacy;
    private final PersonGateway people;
    private final Clock clock;

    RetentionPolicy(RetentionSettingsRepository settings, PersonPrivacyRepository privacy, PersonGateway people,
            Clock clock) {
        this.settings = settings;
        this.privacy = privacy;
        this.people = people;
        this.clock = clock;
    }

    public RetentionSettings settings() {
        return settings.findById((short) 1).orElseGet(RetentionSettings::new);
    }

    public LocalDate retainUntil(LocalDate basisDate) {
        return basisDate.plusMonths(settings().getRetentionMonths());
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** The privacy record of a person; created on first use with the retention clock starting from the basis date. */
    @Transactional
    public PersonPrivacy ensure(UUID personId) {
        return privacy.findById(personId).orElseGet(() -> create(personId, people.find(personId).orElse(null)));
    }

    @Transactional
    public PersonPrivacy create(UUID personId, PersonInfo person) {
        PersonPrivacy created = new PersonPrivacy(personId);
        created.setBasisDate(basisFor(person));
        created.setRetainUntil(retainUntil(created.getBasisDate()));
        return privacy.save(created);
    }

    /** Start of the retention period for a person under the configured basis. */
    public LocalDate basisFor(PersonInfo person) {
        ZoneId zone = clock.getZone();
        LocalDate created = person == null || person.createdAt() == null ? today()
                : person.createdAt().atZone(zone).toLocalDate();
        if (person != null && person.lastContactAt() != null
                && settings().getBasis() == com.recruiting.platform.contracts.RetentionBasis.FROM_LAST_CONTACT) {
            LocalDate contact = person.lastContactAt().atZone(zone).toLocalDate();
            return contact.isAfter(created) ? contact : created;
        }
        return created;
    }
}
