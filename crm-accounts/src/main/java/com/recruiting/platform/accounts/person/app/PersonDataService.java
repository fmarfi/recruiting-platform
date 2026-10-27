package com.recruiting.platform.accounts.person.app;

import com.recruiting.platform.accounts.PersonDataApi;
import com.recruiting.platform.accounts.domain.CvIntakeItem;
import com.recruiting.platform.accounts.domain.CvIntakeItemRepository;
import com.recruiting.platform.accounts.domain.DuplicateCandidateRepository;
import com.recruiting.platform.accounts.domain.NetworkLinkRepository;
import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonEducationRepository;
import com.recruiting.platform.accounts.domain.PersonEmploymentRepository;
import com.recruiting.platform.accounts.domain.PersonFile;
import com.recruiting.platform.accounts.domain.PersonFileRepository;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.accounts.shared.AfterCommit;
import com.recruiting.platform.accounts.shared.Events;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.FileStore;
import com.recruiting.platform.contracts.PersonAnonymised;
import com.recruiting.platform.contracts.RetentionStatus;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PersonDataApi: anonymise and delete (ADR 0009 step 2, extended for 1.2.0 by ADR 0011). */
@Service
@Transactional
public class PersonDataService implements PersonDataApi {

    private static final Logger LOG = LoggerFactory.getLogger(PersonDataService.class);

    private final PersonRepository people;
    private final PersonEmploymentRepository employments;
    private final NetworkLinkRepository links;
    private final PersonEducationRepository educations;
    private final PersonFileRepository files;
    private final CvIntakeItemRepository intake;
    private final DuplicateCandidateRepository duplicates;
    private final FileStore store;
    private final ApplicationEventPublisher events;

    public PersonDataService(PersonRepository people, PersonEmploymentRepository employments,
            NetworkLinkRepository links, PersonEducationRepository educations, PersonFileRepository files,
            CvIntakeItemRepository intake, DuplicateCandidateRepository duplicates, FileStore store,
            ApplicationEventPublisher events) {
        this.people = people;
        this.employments = employments;
        this.links = links;
        this.educations = educations;
        this.files = files;
        this.intake = intake;
        this.duplicates = duplicates;
        this.store = store;
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
            p.setTags(null); // tags are cleared on anonymisation (ADR 0010)
            // candidate profile (1.2.0); source_channel is not personal data and stays
            p.setNoticePeriodDays(null);
            p.setMobility(null);
            p.setCompensation(null);
            p.setPhoneNormalized(null);
            p.setLinkedinUrlNormalized(null);
            p.setLanguages(null);
            p.setAnonymisedAt(Instant.now());
            p.setRetentionStatus(RetentionStatus.ANONYMISED);
            people.saveAndFlush(p);
            employments.deleteByPersonId(personId);
            educations.deleteByPersonId(personId);
            links.deleteAll(links.findByFromPersonIdOrToPersonId(personId, personId));
            duplicates.deleteAll(duplicates.findByPerson(personId));
            deleteFiles(personId);
            for (CvIntakeItem item : intake.findByAttachedPersonId(personId)) {
                item.setFileName(null);
                item.setExtractedText(null);
                item.setDetected(null);
                String key = item.getStorageKey();
                item.setStorageKey(null);
                intake.save(item);
                deleteBytesAfterCommit(key);
            }
        }
        events.publishEvent(new PersonAnonymised(Events.newId(), Instant.now(), actorId, personId, false));
    }

    @Override
    public void delete(UUID personId, UUID actorId) {
        Person p = find(personId);
        // the cascade removes the rows, so the keys are collected first and the bytes go after the commit
        files.findByPersonIdOrderByKindAscVersionDesc(personId).forEach(f -> deleteBytesAfterCommit(f.getStorageKey()));
        intake.findByAttachedPersonId(personId).forEach(i -> deleteBytesAfterCommit(i.getStorageKey()));
        people.delete(p);
        people.flush();
        events.publishEvent(new PersonAnonymised(Events.newId(), Instant.now(), actorId, personId, true));
    }

    private void deleteFiles(UUID personId) {
        List<PersonFile> own = new ArrayList<>(files.findByPersonIdOrderByKindAscVersionDesc(personId));
        for (PersonFile f : own) {
            deleteBytesAfterCommit(f.getStorageKey());
        }
        files.deleteAll(own);
        files.flush();
    }

    private void deleteBytesAfterCommit(String key) {
        if (key == null) {
            return;
        }
        AfterCommit.run(() -> {
            try {
                store.delete(key);
            } catch (IOException e) {
                LOG.warn("Could not delete stored bytes {}; the weekly sweep removes them", key);
            }
        });
    }

    private Person find(UUID id) {
        return people.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Person " + id + " not found."));
    }
}
