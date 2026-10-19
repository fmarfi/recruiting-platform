package com.recruiting.platform.pipeline.timeline.app;

import com.recruiting.platform.contracts.AccountCreated;
import com.recruiting.platform.contracts.AccountStatusChanged;
import com.recruiting.platform.contracts.AccountUpdated;
import com.recruiting.platform.contracts.ContactAdded;
import com.recruiting.platform.contracts.ContactRemoved;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.contracts.TimelineKind;
import com.recruiting.platform.pipeline.timeline.app.TimelineWriter.Entry;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Timeline entries for events of the accounts module (account, status and contact changes). */
@Component
class AccountTimelineListener {

    private final TimelineWriter writer;

    AccountTimelineListener(TimelineWriter writer) {
        this.writer = writer;
    }

    @ApplicationModuleListener
    void on(AccountCreated e) {
        writer.append(e, new Entry(e.accountId(), TimelineKind.ACCOUNT, EntityType.ACCOUNT, e.accountId(), null,
                e.status().name(), null, "Account created", null, null));
    }

    @ApplicationModuleListener
    void on(AccountUpdated e) {
        if (e.ownerChanged()) {
            writer.append(e, new Entry(e.accountId(), TimelineKind.ACCOUNT, EntityType.ACCOUNT, e.accountId(), null,
                    null, null, "Account owner changed", null, null));
        }
    }

    @ApplicationModuleListener
    void on(AccountStatusChanged e) {
        writer.append(e, TimelineWriter.status(e.accountId(), TimelineKind.ACCOUNT_STATUS, EntityType.ACCOUNT,
                e.accountId(), e.from(), e.to(), e.reason(), "Account", null, null));
    }

    @ApplicationModuleListener
    void on(ContactAdded e) {
        writer.append(e, new Entry(e.accountId(), TimelineKind.CONTACT, EntityType.CONTACT, e.contactId(), null, null,
                null, "Contact added", e.personId(), null));
    }

    @ApplicationModuleListener
    void on(ContactRemoved e) {
        writer.append(e, new Entry(e.accountId(), TimelineKind.CONTACT, EntityType.CONTACT, e.contactId(), null, null,
                null, "Contact removed", e.personId(), null));
    }
}
