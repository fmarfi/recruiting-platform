package com.recruiting.platform.accounts.person.app;

import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonEducation;
import com.recruiting.platform.accounts.domain.PersonEducationRepository;
import com.recruiting.platform.accounts.domain.PersonEmployment;
import com.recruiting.platform.accounts.domain.PersonEmploymentRepository;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.accounts.person.web.dto.EducationDto;
import com.recruiting.platform.accounts.person.web.dto.Employment;
import com.recruiting.platform.accounts.person.web.dto.LanguageSkillDto;
import com.recruiting.platform.accounts.person.web.dto.PersonCreateRequest;
import com.recruiting.platform.accounts.person.web.dto.PersonResponse;
import com.recruiting.platform.accounts.person.web.dto.PersonUpdateRequest;
import com.recruiting.platform.accounts.shared.Actor;
import com.recruiting.platform.accounts.shared.Events;
import com.recruiting.platform.accounts.shared.Patch;
import com.recruiting.platform.accounts.shared.PatchBinder;
import com.recruiting.platform.security.UserDirectory;
import com.recruiting.platform.accounts.shared.Versions;
import com.recruiting.platform.contracts.AccessAction;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.security.AccessRecorder;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.PersonCreated;
import com.recruiting.platform.contracts.PersonUpdated;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final PersonEducationRepository educations;
    private final PersonMapper mapper;
    private final UserDirectory users;
    private final ApplicationEventPublisher events;
    private final CompensationAccess compensationAccess;
    private final AccessRecorder access;

    public PersonService(PersonRepository people, PersonEmploymentRepository employments,
            PersonEducationRepository educations, PersonMapper mapper, UserDirectory users,
            ApplicationEventPublisher events, CompensationAccess compensationAccess, AccessRecorder access) {
        this.people = people;
        this.employments = employments;
        this.educations = educations;
        this.mapper = mapper;
        this.users = users;
        this.events = events;
        this.compensationAccess = compensationAccess;
        this.access = access;
    }

    public PersonResponse create(PersonCreateRequest request, Actor actor) {
        return mapper.response(createEntity(request, actor, null));
    }

    /** Also used by contact creation and import; {@code importId} is null for single adds. */
    public Person createEntity(PersonCreateRequest r, Actor actor, UUID importId) {
        UUID ownerId = r.ownerId() != null ? r.ownerId() : actor.id();
        if (ownerId == null || !users.isActive(ownerId)) {
            throw PatchBinder.validation("ownerId", "REQUIRED", "an active owner is required");
        }
        String email = blankToNull(r.email());
        failIfEmailTaken(email, null);
        if (r.compensation() != null && !compensationAccess.allowed(ownerId)) {
            throw compensationForbidden();
        }
        validateProfile(r.languages(), r.educations());
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
        p.setPhoneNormalized(PersonNormalizer.phone(r.phone()));
        p.setLinkedinUrlNormalized(PersonNormalizer.linkedin(r.linkedinUrl()));
        p.setSourceChannel(r.sourceChannel());
        p.setNoticePeriodDays(r.noticePeriodDays() == null ? null : r.noticePeriodDays().shortValue());
        p.setMobility(r.mobility());
        p.setCompensation(r.compensation());
        p.setLanguages(languageMaps(r.languages()));
        p = people.saveAndFlush(p);
        replaceEmployments(p.getId(), r.employments());
        replaceEducations(p.getId(), r.educations());
        events.publishEvent(new PersonCreated(Events.newId(), Instant.now(), actor.id(), p.getId(), p.getType(),
                ownerId, email != null, importId));
        return p;
    }

    /** Read without an access-log row (exports and internal callers). */
    @Transactional(readOnly = true)
    public PersonResponse get(UUID id) {
        return mapper.response(find(id));
    }

    /** GET /people/{id}: the read, with the VIEW access-log row (compensation rides on it, ADR 0011). */
    public PersonResponse view(UUID id) {
        Person p = find(id);
        access.record(AccessAction.VIEW, EntityType.PERSON, p.getId());
        return mapper.response(p);
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
        if (patch.has("compensation") && !compensationAccess.allowed(p)) {
            throw compensationForbidden();
        }
        if (patch.has("languages") || patch.has("educations")) {
            validateProfile(patch.has("languages") ? r.languages() : null,
                    patch.has("educations") ? r.educations() : null);
        }
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
            p.setPhoneNormalized(PersonNormalizer.phone(r.phone()));
        }
        if (patch.has("city")) {
            p.setCity(r.city());
        }
        if (patch.has("linkedinUrl")) {
            p.setLinkedinUrl(r.linkedinUrl());
            p.setLinkedinUrlNormalized(PersonNormalizer.linkedin(r.linkedinUrl()));
        }
        if (patch.has("sourceChannel")) {
            p.setSourceChannel(r.sourceChannel());
        }
        if (patch.has("noticePeriodDays")) {
            p.setNoticePeriodDays(r.noticePeriodDays() == null ? null : r.noticePeriodDays().shortValue());
        }
        if (patch.has("mobility")) {
            p.setMobility(r.mobility());
        }
        if (patch.has("compensation")) {
            p.setCompensation(r.compensation());
        }
        if (patch.has("languages")) {
            p.setLanguages(languageMaps(r.languages()));
        }
        if (patch.has("howWeKnow")) {
            p.setHowWeKnow(r.howWeKnow());
        }
        if (patch.has("notes")) {
            p.setNotes(r.notes());
        }
        if (patch.has("ownerId")) {
            if (!users.isActive(r.ownerId())) {
                throw PatchBinder.validation("ownerId", "INVALID", "unknown or inactive user");
            }
            p.setOwnerId(r.ownerId());
        }
        p = people.saveAndFlush(p);
        if (patch.has("employments")) {
            replaceEmployments(p.getId(), r.employments() == null ? List.of() : r.employments());
        }
        if (patch.has("educations")) {
            replaceEducations(p.getId(), r.educations() == null ? List.of() : r.educations());
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

    private static ApiException compensationForbidden() {
        return new ApiException(ErrorCode.FORBIDDEN,
                "Only a manager, an admin or the relationship owner may change compensation.");
    }

    private static void validateProfile(List<LanguageSkillDto> languages, List<EducationDto> educations) {
        if (languages != null) {
            Set<String> seen = new HashSet<>();
            for (LanguageSkillDto l : languages) {
                if (!seen.add(l.language())) {
                    throw PatchBinder.validation("languages", "DUPLICATE", "each language only once");
                }
            }
        }
        if (educations != null) {
            for (EducationDto e : educations) {
                if (e.startYear() != null && e.endYear() != null && e.endYear() < e.startYear()) {
                    throw PatchBinder.validation("educations", "RANGE", "endYear must not be before startYear");
                }
            }
        }
    }

    private static List<Map<String, Object>> languageMaps(List<LanguageSkillDto> languages) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (languages != null) {
            for (LanguageSkillDto l : languages) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("language", l.language());
                m.put("level", l.level().name());
                out.add(m);
            }
        }
        return out;
    }

    private void replaceEducations(UUID personId, List<EducationDto> list) {
        educations.deleteByPersonId(personId);
        educations.flush();
        if (list == null) {
            return;
        }
        int order = 0;
        for (EducationDto e : list) {
            PersonEducation pe = new PersonEducation();
            pe.setPersonId(personId);
            pe.setSchool(e.school());
            pe.setDegree(e.degree());
            pe.setField(e.field());
            pe.setStartYear(e.startYear() == null ? null : e.startYear().shortValue());
            pe.setEndYear(e.endYear() == null ? null : e.endYear().shortValue());
            pe.setSortOrder(order++);
            educations.save(pe);
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
