package com.recruiting.platform.accounts.person.app;

import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonEmployment;
import com.recruiting.platform.accounts.domain.PersonEmploymentRepository;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.accounts.person.web.dto.Employment;
import com.recruiting.platform.accounts.person.web.dto.PersonCreateRequest;
import com.recruiting.platform.accounts.person.web.dto.PersonResponse;
import com.recruiting.platform.accounts.person.web.dto.PersonUpdateRequest;
import com.recruiting.platform.accounts.shared.Actor;
import com.recruiting.platform.accounts.shared.Events;
import com.recruiting.platform.accounts.shared.Patch;
import com.recruiting.platform.accounts.shared.PatchBinder;
import com.recruiting.platform.accounts.shared.UserLookup;
import com.recruiting.platform.accounts.shared.Versions;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.PersonCreated;
import com.recruiting.platform.contracts.PersonUpdated;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Create, read and update a person (A-12). Publishes PersonCreated and PersonUpdated. */
@Service
@Transactional
public class PersonService {

    private final PersonRepository people;
    private final PersonEmploymentRepository employments;
    private final PersonMapper mapper;
    private final UserLookup users;
    private final ApplicationEventPublisher events;

    public PersonService(PersonRepository people, PersonEmploymentRepository employments, PersonMapper mapper,
            UserLookup users, ApplicationEventPublisher events) {
        this.people = people;
        this.employments = employments;
        this.mapper = mapper;
        this.users = users;
        this.events = events;
    }

    public PersonResponse create(PersonCreateRequest request, Actor actor) {
        return mapper.response(createEntity(request, actor, null));
    }

    /** Also used by contact creation and import; {@code importId} is null for single adds. */
    public Person createEntity(PersonCreateRequest r, Actor actor, UUID importId) {
        UUID ownerId = r.ownerId() != null ? r.ownerId() : actor.id();
        if (ownerId == null || !users.exists(ownerId)) {
            throw PatchBinder.validation("ownerId", "REQUIRED", "an active owner is required");
        }
        String email = blankToNull(r.email());
        failIfEmailTaken(email, null);
        Person p = new Person();
        p.setName(r.name().trim());
        p.setType(r.type());
        p.setTitle(r.title());
        p.setCompany(r.company());
        p.setEmail(email);
        p.setPhone(r.phone());
        p.setCity(r.city());
        p.setLinkedinUrl(r.linkedinUrl());
        p.setHowWeKnow(r.howWeKnow());
        p.setNotes(r.notes());
        p.setOwnerId(ownerId);
        p = people.saveAndFlush(p);
        replaceEmployments(p.getId(), r.employments());
        events.publishEvent(new PersonCreated(Events.newId(), Instant.now(), actor.id(), p.getId(), p.getType(),
                ownerId, email != null, importId));
        return p;
    }

    @Transactional(readOnly = true)
    public PersonResponse get(UUID id) {
        return mapper.response(find(id));
    }

    public PersonResponse update(UUID id, Patch<PersonUpdateRequest> patch, Actor actor) {
        Person p = find(id);
        if (p.getAnonymisedAt() != null) {
            throw new ApiException(ErrorCode.STATE_CONFLICT, "Anonymised people cannot be edited.");
        }
        PersonUpdateRequest r = patch.body();
        Versions.check(r.version(), p.getVersion());
        PatchBinder.notNull(patch, "name", r.name());
        PatchBinder.notNull(patch, "type", r.type());
        PatchBinder.notNull(patch, "ownerId", r.ownerId());
        if (patch.has("name")) {
            p.setName(r.name().trim());
        }
        if (patch.has("type")) {
            p.setType(r.type());
        }
        if (patch.has("title")) {
            p.setTitle(r.title());
        }
        if (patch.has("company")) {
            p.setCompany(r.company());
        }
        boolean emailChanged = false;
        if (patch.has("email")) {
            String email = blankToNull(r.email());
            failIfEmailTaken(email, p.getId());
            emailChanged = email != null && !email.equalsIgnoreCase(p.getEmail());
            p.setEmail(email);
        }
        if (patch.has("phone")) {
            p.setPhone(r.phone());
        }
        if (patch.has("city")) {
            p.setCity(r.city());
        }
        if (patch.has("linkedinUrl")) {
            p.setLinkedinUrl(r.linkedinUrl());
        }
        if (patch.has("howWeKnow")) {
            p.setHowWeKnow(r.howWeKnow());
        }
        if (patch.has("notes")) {
            p.setNotes(r.notes());
        }
        if (patch.has("ownerId")) {
            if (!users.exists(r.ownerId())) {
                throw PatchBinder.validation("ownerId", "INVALID", "unknown or inactive user");
            }
            p.setOwnerId(r.ownerId());
        }
        p = people.saveAndFlush(p);
        if (patch.has("employments")) {
            replaceEmployments(p.getId(), r.employments() == null ? List.of() : r.employments());
        }
        events.publishEvent(new PersonUpdated(Events.newId(), Instant.now(), actor.id(), p.getId(), p.getType(),
                emailChanged));
        return mapper.response(p);
    }

    private Person find(UUID id) {
        return people.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Person " + id + " not found."));
    }

    private void failIfEmailTaken(String email, UUID selfId) {
        if (email == null) {
            return;
        }
        people.findByEmailIgnoreCase(email).filter(o -> !o.getId().equals(selfId)).ifPresent(o -> {
            throw new ApiException(ErrorCode.DUPLICATE, "A person with this e-mail already exists.",
                    Map.of("existingId", o.getId()));
        });
    }

    private void replaceEmployments(UUID personId, List<Employment> history) {
        employments.deleteByPersonId(personId);
        if (history == null) {
            return;
        }
        int order = 0;
        for (Employment e : history) {
            PersonEmployment pe = new PersonEmployment();
            pe.setPersonId(personId);
            pe.setCompany(e.company());
            pe.setTitle(e.title());
            pe.setStartDate(e.startDate());
            pe.setEndDate(e.endDate());
            pe.setSortOrder(order++);
            employments.save(pe);
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
