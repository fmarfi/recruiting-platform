package com.recruiting.platform.accounts.person.app;

import com.recruiting.platform.accounts.PersonMergeApi;
import com.recruiting.platform.accounts.domain.Contact;
import com.recruiting.platform.accounts.domain.ContactRepository;
import com.recruiting.platform.accounts.domain.DuplicateCandidate;
import com.recruiting.platform.accounts.domain.DuplicateCandidateRepository;
import com.recruiting.platform.accounts.domain.NetworkLink;
import com.recruiting.platform.accounts.domain.NetworkLinkRepository;
import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonEducation;
import com.recruiting.platform.accounts.domain.PersonEducationRepository;
import com.recruiting.platform.accounts.domain.PersonEmployment;
import com.recruiting.platform.accounts.domain.PersonEmploymentRepository;
import com.recruiting.platform.accounts.domain.PersonFile;
import com.recruiting.platform.accounts.domain.PersonFileRepository;
import com.recruiting.platform.accounts.domain.PersonMerge;
import com.recruiting.platform.accounts.domain.PersonMergeRepository;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.accounts.shared.AfterCommit;
import com.recruiting.platform.accounts.shared.Events;
import com.recruiting.platform.accounts.shared.PatchBinder;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.DuplicateStatus;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.FileStore;
import com.recruiting.platform.contracts.PersonFileKind;
import com.recruiting.platform.contracts.PersonMerged;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The accounts side of a person merge (B-04, ADR 0011 item 6): steps 1 and 4 of the orchestration in privacy. */
@Service
@Transactional
public class PersonMergeService implements PersonMergeApi {

    private static final Logger LOG = LoggerFactory.getLogger(PersonMergeService.class);

    private final PersonRepository people;
    private final PersonEmploymentRepository employments;
    private final PersonEducationRepository educations;
    private final PersonFileRepository files;
    private final ContactRepository contacts;
    private final NetworkLinkRepository links;
    private final DuplicateCandidateRepository candidates;
    private final PersonMergeRepository merges;
    private final FileStore store;
    private final ApplicationEventPublisher events;

    public PersonMergeService(PersonRepository people, PersonEmploymentRepository employments,
            PersonEducationRepository educations, PersonFileRepository files, ContactRepository contacts,
            NetworkLinkRepository links, DuplicateCandidateRepository candidates, PersonMergeRepository merges,
            FileStore store, ApplicationEventPublisher events) {
        this.people = people;
        this.employments = employments;
        this.educations = educations;
        this.files = files;
        this.contacts = contacts;
        this.links = links;
        this.candidates = candidates;
        this.merges = merges;
        this.store = store;
        this.events = events;
    }

    @Override
    public void mergeProfile(UUID survivorId, UUID mergedId, Set<String> take, long survivorVersion, UUID actorId) {
        if (survivorId.equals(mergedId)) {
            throw new ApiException(ErrorCode.STATE_CONFLICT, "A person cannot be merged into itself.");
        }
        Person s = find(survivorId);
        Person m = find(mergedId);
        if (s.getAnonymisedAt() != null || m.getAnonymisedAt() != null) {
            throw new ApiException(ErrorCode.STATE_CONFLICT, "Anonymised people cannot be merged.");
        }
        if (s.getVersion() == null || s.getVersion() != survivorVersion) {
            throw new ApiException(ErrorCode.VERSION_CONFLICT,
                    "The survivor was changed by someone else (version " + s.getVersion() + ").");
        }
        Set<String> takeFields = take == null ? Set.of() : take;
        for (String f : takeFields) {
            if (!TAKEABLE_FIELDS.contains(f)) {
                throw PatchBinder.validation("takeFromMerged", "INVALID", "unknown field " + f);
            }
        }
        // the e-mail is unique: free it on the merged person before the survivor takes it
        String mergedEmail = m.getEmail();
        if (mergedEmail != null) {
            m.setEmail(null);
            people.saveAndFlush(m);
        }
        copyFields(s, m, mergedEmail, takeFields);
        s.setTags(unionTags(s.getTags(), m.getTags()));
        s.setLanguages(unionLanguages(s.getLanguages(), m.getLanguages()));
        s.setPhoneNormalized(PersonNormalizer.phone(s.getPhone()));
        s.setLinkedinUrlNormalized(PersonNormalizer.linkedin(s.getLinkedinUrl()));
        people.saveAndFlush(s);
        appendEmployments(survivorId, mergedId);
        appendEducations(survivorId, mergedId);
        moveFiles(survivorId, mergedId);
        moveContacts(survivorId, mergedId);
        moveLinks(survivorId, mergedId);
        closePairs(survivorId, mergedId, actorId);
    }

    @Override
    public void finish(UUID survivorId, UUID mergedId, Set<String> fieldsTaken, UUID actorId) {
        PersonMerge row = new PersonMerge();
        row.setSurvivorId(survivorId);
        row.setMergedId(mergedId);
        row.setFieldsTaken(new ArrayList<>(new TreeSet<>(fieldsTaken == null ? Set.of() : fieldsTaken)));
        row.setMergedBy(actorId);
        merges.saveAndFlush(row);
        people.delete(find(mergedId));
        people.flush();
        events.publishEvent(new PersonMerged(Events.newId(), Instant.now(), actorId, survivorId, mergedId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> mergedIds(UUID survivorId) {
        return merges.findMergedIds(survivorId);
    }

    // ---------------------------------------------------------------- steps

    private void copyFields(Person s, Person m, String mergedEmail, Set<String> take) {
        if (take.contains("name") && m.getName() != null) {
            s.setName(m.getName());
        }
        if (take.contains("type") && m.getType() != null) {
            s.setType(m.getType());
        }
        if (take.contains("ownerId") && m.getOwnerId() != null) {
            s.setOwnerId(m.getOwnerId());
        }
        s.setTitle(pick(take, "title", s.getTitle(), m.getTitle()));
        s.setCompany(pick(take, "company", s.getCompany(), m.getCompany()));
        s.setEmail(pick(take, "email", s.getEmail(), mergedEmail));
        s.setPhone(pick(take, "phone", s.getPhone(), m.getPhone()));
        s.setCity(pick(take, "city", s.getCity(), m.getCity()));
        s.setLinkedinUrl(pick(take, "linkedinUrl", s.getLinkedinUrl(), m.getLinkedinUrl()));
        s.setHowWeKnow(pick(take, "howWeKnow", s.getHowWeKnow(), m.getHowWeKnow()));
        s.setNotes(pick(take, "notes", s.getNotes(), m.getNotes()));
        s.setSourceChannel(pick(take, "sourceChannel", s.getSourceChannel(), m.getSourceChannel()));
        s.setNoticePeriodDays(pick(take, "noticePeriodDays", s.getNoticePeriodDays(), m.getNoticePeriodDays()));
        s.setMobility(pick(take, "mobility", s.getMobility(), m.getMobility()));
        s.setCompensation(pick(take, "compensation", s.getCompensation(), m.getCompensation()));
    }

    /** Merged value when asked for (and present), or when the survivor has none; else the survivor's value. */
    private static <T> T pick(Set<String> take, String field, T survivor, T merged) {
        if (merged == null) {
            return survivor;
        }
        return take.contains(field) || survivor == null || (survivor instanceof String str && str.isBlank())
                ? merged : survivor;
    }

    private static List<String> unionTags(List<String> a, List<String> b) {
        Set<String> all = new LinkedHashSet<>(a);
        all.addAll(b);
        return all.stream().limit(20).toList();
    }

    private static List<Map<String, Object>> unionLanguages(List<Map<String, Object>> a, List<Map<String, Object>> b) {
        List<Map<String, Object>> out = new ArrayList<>(a);
        Set<String> have = new HashSet<>();
        a.forEach(l -> have.add(String.valueOf(l.get("language")).toLowerCase()));
        for (Map<String, Object> l : b) {
            if (out.size() < 20 && have.add(String.valueOf(l.get("language")).toLowerCase())) {
                out.add(l);
            }
        }
        return out;
    }

    private void appendEmployments(UUID survivorId, UUID mergedId) {
        List<PersonEmployment> have = employments.findByPersonIdOrderBySortOrder(survivorId);
        int order = have.size();
        for (PersonEmployment e : employments.findByPersonIdOrderBySortOrder(mergedId)) {
            boolean exists = have.stream().anyMatch(h -> sameText(h.getCompany(), e.getCompany())
                    && sameText(h.getTitle(), e.getTitle()) && Objects.equals(h.getStartDate(), e.getStartDate()));
            if (!exists) {
                PersonEmployment copy = new PersonEmployment();
                copy.setPersonId(survivorId);
                copy.setCompany(e.getCompany());
                copy.setTitle(e.getTitle());
                copy.setStartDate(e.getStartDate());
                copy.setEndDate(e.getEndDate());
                copy.setSortOrder(order++);
                employments.save(copy);
            }
        }
    }

    private void appendEducations(UUID survivorId, UUID mergedId) {
        List<PersonEducation> have = educations.findByPersonIdOrderBySortOrder(survivorId);
        int order = have.size();
        for (PersonEducation e : educations.findByPersonIdOrderBySortOrder(mergedId)) {
            if (have.size() + 0 >= 20 || order >= 20) {
                break;
            }
            boolean exists = have.stream()
                    .anyMatch(h -> sameText(h.getSchool(), e.getSchool()) && sameText(h.getDegree(), e.getDegree()));
            if (!exists) {
                PersonEducation copy = new PersonEducation();
                copy.setPersonId(survivorId);
                copy.setSchool(e.getSchool());
                copy.setDegree(e.getDegree());
                copy.setField(e.getField());
                copy.setStartYear(e.getStartYear());
                copy.setEndYear(e.getEndYear());
                copy.setSortOrder(order++);
                educations.save(copy);
            }
        }
    }

    private static boolean sameText(String a, String b) {
        return a == null ? b == null : b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    private void moveFiles(UUID survivorId, UUID mergedId) {
        List<PersonFile> mine = new ArrayList<>(files.findByPersonIdOrderByKindAscVersionDesc(survivorId));
        Set<String> shas = new HashSet<>();
        mine.forEach(f -> shas.add(f.getSha256()));
        List<PersonFile> cvs = new ArrayList<>(mine.stream().filter(f -> f.getKind() == PersonFileKind.CV).toList());
        for (PersonFile f : files.findByPersonIdOrderByKindAscVersionDesc(mergedId)) {
            if (!shas.add(f.getSha256())) {
                String key = f.getStorageKey();
                files.delete(f);
                files.flush();
                AfterCommit.run(() -> deleteBytes(key));
                continue;
            }
            f.setPersonId(survivorId);
            f.setCurrent(false);
            f.setVersion(f.getKind() == PersonFileKind.CV ? 1000 + cvs.size() + 1 : 1);
            files.saveAndFlush(f);
            if (f.getKind() == PersonFileKind.CV) {
                cvs.add(f);
            }
        }
        // renumber CV versions by upload time; the newest is current (two phases: unique indexes)
        cvs.sort(Comparator.comparing(PersonFile::getCreatedAt).thenComparing(PersonFile::getId));
        int i = 0;
        for (PersonFile f : cvs) {
            f.setCurrent(false);
            f.setVersion(2000 + i++);
            files.saveAndFlush(f);
        }
        i = 0;
        for (PersonFile f : cvs) {
            f.setVersion(++i);
            f.setCurrent(i == cvs.size());
            files.saveAndFlush(f);
        }
    }

    private void moveContacts(UUID survivorId, UUID mergedId) {
        for (Contact c : contacts.findByPersonId(mergedId)) {
            boolean clash = c.getEndedOn() == null
                    && contacts.findByAccountIdAndPersonIdAndEndedOnIsNull(c.getAccountId(), survivorId).isPresent();
            if (clash) {
                boolean wasPrimary = c.isPrimary();
                UUID accountId = c.getAccountId();
                contacts.delete(c);
                contacts.flush();
                if (wasPrimary) {
                    contacts.findByAccountIdAndPersonIdAndEndedOnIsNull(accountId, survivorId).ifPresent(keep -> {
                        keep.setPrimary(true);
                        contacts.saveAndFlush(keep);
                    });
                }
            } else {
                c.setPersonId(survivorId);
                contacts.saveAndFlush(c);
            }
        }
    }

    private void moveLinks(UUID survivorId, UUID mergedId) {
        for (NetworkLink l : links.findByFromPersonIdOrToPersonId(mergedId, mergedId)) {
            UUID from = l.getFromPersonId().equals(mergedId) ? survivorId : l.getFromPersonId();
            UUID to = l.getToPersonId().equals(mergedId) ? survivorId : l.getToPersonId();
            boolean duplicate = links.findByFromPersonIdAndToPersonIdAndType(from, to, l.getType()).isPresent();
            if (from.equals(to) || duplicate) {
                links.delete(l);
                links.flush();
            } else {
                l.setFromPersonId(from);
                l.setToPersonId(to);
                links.saveAndFlush(l);
            }
        }
    }

    private void closePairs(UUID survivorId, UUID mergedId, UUID actorId) {
        Set<UUID> seen = new HashSet<>();
        List<DuplicateCandidate> pairs = new ArrayList<>(candidates.findByPerson(survivorId));
        pairs.addAll(candidates.findByPerson(mergedId));
        for (DuplicateCandidate d : pairs) {
            if (seen.add(d.getId()) && d.getStatus() == DuplicateStatus.OPEN) {
                d.setStatus(DuplicateStatus.MERGED);
                d.setResolvedAt(Instant.now());
                d.setResolvedBy(actorId);
                candidates.save(d);
            }
        }
    }

    private void deleteBytes(String key) {
        try {
            store.delete(key);
        } catch (IOException e) {
            LOG.warn("Could not delete stored bytes {}; the weekly sweep removes them", key);
        }
    }

    private Person find(UUID id) {
        return people.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Person " + id + " not found."));
    }
}
