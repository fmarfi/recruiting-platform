package com.recruiting.platform.accounts.contact.app;

import com.recruiting.platform.accounts.contact.web.dto.ContactCreateRequest;
import com.recruiting.platform.accounts.contact.web.dto.ContactResponse;
import com.recruiting.platform.accounts.contact.web.dto.ContactUpdateRequest;
import com.recruiting.platform.accounts.domain.AccountRepository;
import com.recruiting.platform.accounts.domain.Contact;
import com.recruiting.platform.accounts.domain.ContactRepository;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.accounts.person.app.PersonQueryService;
import com.recruiting.platform.accounts.person.app.PersonService;
import com.recruiting.platform.accounts.shared.Actor;
import com.recruiting.platform.accounts.shared.Events;
import com.recruiting.platform.accounts.shared.Patch;
import com.recruiting.platform.accounts.shared.PatchBinder;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ContactAdded;
import com.recruiting.platform.contracts.ContactRemoved;
import com.recruiting.platform.contracts.ErrorCode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Contacts of an account (A-03): list, add (existing or new person), change, end. */
@Service
@Transactional
public class ContactService {

    private final ContactRepository contacts;
    private final AccountRepository accounts;
    private final PersonRepository people;
    private final PersonService personService;
    private final ContactMapper mapper;
    private final ApplicationEventPublisher events;

    public ContactService(ContactRepository contacts, AccountRepository accounts, PersonRepository people,
            PersonService personService, ContactMapper mapper, ApplicationEventPublisher events) {
        this.contacts = contacts;
        this.accounts = accounts;
        this.people = people;
        this.personService = personService;
        this.mapper = mapper;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> listForAccount(UUID accountId, boolean activeOnly) {
        requireAccount(accountId);
        List<Contact> found = activeOnly ? contacts.findByAccountIdAndEndedOnIsNull(accountId)
                : contacts.findByAccountId(accountId);
        return mapper.responses(found.stream()
                .sorted(Comparator.comparing(Contact::isPrimary).reversed()
                        .thenComparing(Contact::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList());
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> listForPerson(UUID personId) {
        if (!people.existsById(personId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Person " + personId + " not found.");
        }
        return mapper.responses(contacts.findByPersonId(personId));
    }

    public ContactResponse add(UUID accountId, ContactCreateRequest r, Actor actor) {
        requireAccount(accountId);
        if ((r.personId() == null) == (r.person() == null)) {
            throw PatchBinder.validation("personId", "ONE_OF", "give exactly one of personId and person");
        }
        UUID personId = r.personId();
        if (personId == null) {
            personId = personService.createEntity(r.person(), actor, null).getId();
        } else if (!people.existsById(personId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Person " + personId + " not found.");
        }
        contacts.findByAccountIdAndPersonIdAndEndedOnIsNull(accountId, personId).ifPresent(c -> {
            throw new ApiException(ErrorCode.DUPLICATE, "The person is already a contact of this account.",
                    Map.of("existingId", c.getId()));
        });
        boolean primary = Boolean.TRUE.equals(r.primary());
        if (primary) {
            clearPrimary(accountId);
        }
        Contact c = new Contact();
        c.setAccountId(accountId);
        c.setPersonId(personId);
        c.setRole(r.role());
        c.setPrimary(primary);
        c = contacts.saveAndFlush(c);
        events.publishEvent(new ContactAdded(Events.newId(), Instant.now(), actor.id(), c.getId(), accountId,
                personId, c.getRole()));
        return mapper.response(c);
    }

    public ContactResponse update(UUID accountId, UUID contactId, Patch<ContactUpdateRequest> patch) {
        Contact c = find(accountId, contactId);
        ContactUpdateRequest r = patch.body();
        PatchBinder.notNull(patch, "primary", r.primary());
        if (patch.has("role")) {
            c.setRole(r.role());
        }
        if (patch.has("primary")) {
            if (r.primary() && c.getEndedOn() == null) {
                clearPrimary(accountId);
            }
            c.setPrimary(r.primary() && c.getEndedOn() == null);
        }
        return mapper.response(contacts.saveAndFlush(c));
    }

    /** Ends the contact; already ended contacts are returned unchanged and publish nothing. */
    public ContactResponse end(UUID accountId, UUID contactId, Actor actor) {
        Contact c = find(accountId, contactId);
        if (c.getEndedOn() == null) {
            c.setEndedOn(LocalDate.now(PersonQueryService.ZONE));
            c.setPrimary(false);
            c = contacts.saveAndFlush(c);
            events.publishEvent(new ContactRemoved(Events.newId(), Instant.now(), actor.id(), c.getId(), accountId,
                    c.getPersonId()));
        }
        return mapper.response(c);
    }

    private void clearPrimary(UUID accountId) {
        contacts.findByAccountIdAndEndedOnIsNull(accountId).stream().filter(Contact::isPrimary).forEach(o -> {
            o.setPrimary(false);
            contacts.saveAndFlush(o);
        });
    }

    private Contact find(UUID accountId, UUID contactId) {
        return contacts.findById(contactId).filter(c -> c.getAccountId().equals(accountId))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Contact " + contactId + " not found."));
    }

    private void requireAccount(UUID accountId) {
        if (!accounts.existsById(accountId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Account " + accountId + " not found.");
        }
    }
}
