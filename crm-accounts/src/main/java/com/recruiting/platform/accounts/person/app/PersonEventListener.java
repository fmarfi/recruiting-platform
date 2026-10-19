package com.recruiting.platform.accounts.person.app;

import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.contracts.ActivityLogged;
import com.recruiting.platform.contracts.ConsentStatusChanged;
import com.recruiting.platform.contracts.RetentionFlagged;
import com.recruiting.platform.contracts.RetentionStatus;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Keeps the copies on person (contracts/events.md): last contact from pipeline, consent and retention status
 * from privacy. All updates are idempotent bulk updates that leave updated_at and the version alone.
 */
@Component
public class PersonEventListener {

    private final PersonRepository people;

    public PersonEventListener(PersonRepository people) {
        this.people = people;
    }

    @ApplicationModuleListener
    void on(ActivityLogged e) {
        if (e.personId() != null && e.type().countsAsContact()) {
            people.bumpLastContact(e.personId(), e.activityAt());
        }
    }

    @ApplicationModuleListener
    void on(ConsentStatusChanged e) {
        people.updateConsentStatus(e.personId(), e.to());
    }

    @ApplicationModuleListener
    void on(RetentionFlagged e) {
        people.updateRetentionStatus(e.personId(), RetentionStatus.FLAGGED);
    }
}
