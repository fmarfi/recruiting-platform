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
 * Step 0 collects merged ids and file keys first. Deliberately not one big transaction: each step commits on its own, so a failure leaves a state that can be retried.
 */
@Service
public class PersonErasureService {

    private final PersonGateway people;
    private final DsrMinimiser minimiser;
    private final ErasureScopes scopes;
    private final AuditPurger purger;

    PersonErasureService(PersonGateway people, DsrMinimiser minimiser, ErasureScopes scopes, AuditPurger purger) {
        this.people = people;
        this.minimiser = minimiser;
        this.scopes = scopes;
        this.purger = purger;
    }

    public void erase(UUID personId, RetentionAction action, UUID actorId) {
        PersonInfo person = people.find(personId).orElseThrow(
                () -> new ApiException(ErrorCode.NOT_FOUND, "Person " + personId + " does not exist."));
        if (person.anonymised() && action == RetentionAction.ANONYMISE) {
            throw new ApiException(ErrorCode.STATE_CONFLICT, "This person has already been anonymised.");
        }
        // step 0: collect merged ids and file keys before anything is deleted (the cascade removes them)
        ErasureScopes.Scope scope = scopes.collect(personId);
        scopes.discardPendingByEmail(person.email());
        minimiser.minimise(personId);
        boolean delete = action == RetentionAction.DELETE;
        if (delete) {
            people.delete(personId, actorId);
        } else {
            people.anonymise(personId, actorId);
            scopes.minimiseAttachedIntake(personId);
        }
        scopes.deleteBytes(scope.fileKeys()); // after the commit that removed the rows
        purger.purge(personId, scope.mergedIds(), delete); // step 4, also covers the merged ids
    }
}
