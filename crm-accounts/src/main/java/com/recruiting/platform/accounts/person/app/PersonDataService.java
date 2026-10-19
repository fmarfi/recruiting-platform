package com.recruiting.platform.accounts.person.app;

import com.recruiting.platform.accounts.PersonDataApi;
import com.recruiting.platform.accounts.domain.NetworkLinkRepository;
import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonEmploymentRepository;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.accounts.shared.Events;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.PersonAnonymised;
import com.recruiting.platform.contracts.RetentionStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PersonDataApi: anonymise and delete (ADR 0009 step 2). */
@Service
@Transactional
public class PersonDataService implements PersonDataApi {

    private final PersonRepository people;
    private final PersonEmploymentRepository employments;
    private final NetworkLinkRepository links;
    private final ApplicationEventPublisher events;

    public PersonDataService(PersonRepository people, PersonEmploymentRepository employments,
            NetworkLinkRepository links, ApplicationEventPublisher events) {
        this.people = people;
        this.employments = employments;
        this.links = links;
        this.events = events;
    }

    @Override
    public void anonymise(UUID personId, UUID actorId) {
        Person p = find(personId);
        if (p.getAnonymisedAt() == null) {
            p.setName("Anonymised person");
            p.setEmail(null);
            p.setPhone(null);
            p.setLinkedinUrl(null);
            p.setHowWeKnow(null);
            p.setNotes(null);
            p.setTitle(null);
            p.setCity(null);
            p.setCompany(null);
            p.setAnonymisedAt(Instant.now());
            p.setRetentionStatus(RetentionStatus.ANONYMISED);
            people.saveAndFlush(p);
            employments.deleteByPersonId(personId);
            links.deleteAll(links.findByFromPersonIdOrToPersonId(personId, personId));
        }
        events.publishEvent(new PersonAnonymised(Events.newId(), Instant.now(), actorId, personId, false));
    }

    @Override
    public void delete(UUID personId, UUID actorId) {
        people.delete(find(personId));
        people.flush();
        events.publishEvent(new PersonAnonymised(Events.newId(), Instant.now(), actorId, personId, true));
    }

    private Person find(UUID id) {
        return people.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Person " + id + " not found."));
    }
}
