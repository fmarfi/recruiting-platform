package com.recruiting.platform.privacy.dsr.app;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.RetentionAction;
import com.recruiting.platform.privacy.person.app.PersonGateway;
import com.recruiting.platform.privacy.person.app.PersonInfo;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Runs ADR 0009 for one person: minimise data-subject requests, then anonymise or delete the person. The
 * PersonAnonymised event then drives the listeners and the audit purge (see {@link ErasureFollowUpListener}).
 * Deliberately not one big transaction: each step commits on its own, so a failure leaves a state that can be retried.
 */
@Service
public class PersonErasureService {

    private final PersonGateway people;
    private final DsrMinimiser minimiser;

    PersonErasureService(PersonGateway people, DsrMinimiser minimiser) {
        this.people = people;
        this.minimiser = minimiser;
    }

    public void erase(UUID personId, RetentionAction action, UUID actorId) {
        PersonInfo person = people.find(personId).orElseThrow(
                () -> new ApiException(ErrorCode.NOT_FOUND, "Person " + personId + " does not exist."));
        if (person.anonymised() && action == RetentionAction.ANONYMISE) {
            throw new ApiException(ErrorCode.STATE_CONFLICT, "This person has already been anonymised.");
        }
        minimiser.minimise(personId);
        if (action == RetentionAction.DELETE) {
            people.delete(personId, actorId);
        } else {
            people.anonymise(personId, actorId);
        }
    }
}
