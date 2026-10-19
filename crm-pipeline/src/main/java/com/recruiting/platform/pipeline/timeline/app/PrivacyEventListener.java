package com.recruiting.platform.pipeline.timeline.app;

import com.recruiting.platform.contracts.PersonAnonymised;
import com.recruiting.platform.contracts.RetentionFlagged;
import com.recruiting.platform.pipeline.domain.ActivityRepository;
import com.recruiting.platform.pipeline.domain.ApplicationRepository;
import com.recruiting.platform.pipeline.domain.NextAction;
import com.recruiting.platform.pipeline.domain.NextActionRepository;
import com.recruiting.platform.pipeline.domain.TimelineEntryRepository;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to privacy events: scrubs free text of an anonymised person (schema.md, Anonymisation) and creates the
 * "renew consent" follow-up for a flagged person (S-03). Both are safe to run twice.
 */
@Component
class PrivacyEventListener {

    private static final String RENEW_TITLE = "Renew consent / review retention";

    private final TimelineEntryRepository timeline;
    private final ActivityRepository activities;
    private final ApplicationRepository applications;
    private final NextActionRepository nextActions;

    PrivacyEventListener(TimelineEntryRepository timeline, ActivityRepository activities,
            ApplicationRepository applications, NextActionRepository nextActions) {
        this.timeline = timeline;
        this.activities = activities;
        this.applications = applications;
        this.nextActions = nextActions;
    }

    @ApplicationModuleListener
    void on(PersonAnonymised e) {
        timeline.scrubPerson(e.personId());
        if (!e.deleted()) {
            activities.scrubNotes(e.personId());
            applications.scrubRejectionReason(e.personId());
            nextActions.scrubTitles(e.personId());
        }
    }

    @ApplicationModuleListener
    void on(RetentionFlagged e) {
        if (e.ownerId() == null || nextActions.existsBySourceEventId(e.eventId())) {
            return;
        }
        NextAction n = new NextAction();
        n.setTitle(RENEW_TITLE);
        n.setDueOn(e.retainUntil());
        n.setOwnerId(e.ownerId());
        n.setPersonId(e.personId());
        n.setSourceEventId(e.eventId());
        nextActions.save(n);
    }
}
