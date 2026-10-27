package com.recruiting.platform.accounts.duplicate.app;

import com.recruiting.platform.accounts.domain.DuplicateCandidate;
import com.recruiting.platform.accounts.domain.DuplicateCandidateRepository;
import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.accounts.duplicate.web.dto.DuplicateCandidateResponse;
import com.recruiting.platform.accounts.duplicate.web.dto.DuplicateCheckRequest;
import com.recruiting.platform.accounts.duplicate.web.dto.DuplicateMatchResponse;
import com.recruiting.platform.accounts.person.app.PersonMapper;
import com.recruiting.platform.accounts.person.app.PersonNormalizer;
import com.recruiting.platform.accounts.person.web.dto.PersonSummaryResponse;
import com.recruiting.platform.accounts.shared.Paging;
import com.recruiting.platform.accounts.shared.dto.PageResponse;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.DuplicateMatchReason;
import com.recruiting.platform.contracts.DuplicateStatus;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.security.UserDirectory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Duplicate check before create (B-04), the duplicate queue and its dismiss action. Anonymised people never match. */
@Service
@Transactional
public class DuplicateService {

    private static final int MAX_MATCHES = 10;

    private final PersonRepository people;
    private final DuplicateCandidateRepository candidates;
    private final PersonMapper mapper;
    private final UserDirectory users;

    public DuplicateService(PersonRepository people, DuplicateCandidateRepository candidates, PersonMapper mapper,
            UserDirectory users) {
        this.people = people;
        this.candidates = candidates;
        this.mapper = mapper;
        this.users = users;
    }

    /** One match: the other person and why. Strongest reason first. */
    public record Match(Person person, List<DuplicateMatchReason> reasons) {
    }

    @Transactional(readOnly = true)
    public List<DuplicateMatchResponse> check(DuplicateCheckRequest r) {
        List<Match> matches = find(r.name(), r.company(), r.email(), r.phone(), r.linkedinUrl(), r.excludePersonId());
        List<PersonSummaryResponse> summaries = mapper.summaries(matches.stream().map(Match::person).toList());
        List<DuplicateMatchResponse> out = new ArrayList<>();
        for (int i = 0; i < matches.size(); i++) {
            out.add(new DuplicateMatchResponse(summaries.get(i), matches.get(i).reasons()));
        }
        return out;
    }

    /** Matches on e-mail, LinkedIn URL, phone, and name + company; at most 10, strongest first. */
    @Transactional(readOnly = true)
    public List<Match> find(String name, String company, String email, String phone, String linkedinUrl,
            UUID excludeId) {
        Map<UUID, Person> byId = new LinkedHashMap<>();
        Map<UUID, Set<DuplicateMatchReason>> reasons = new LinkedHashMap<>();
        if (email != null && !email.isBlank()) {
            people.findByEmailIgnoreCase(email.trim()).ifPresent(p -> add(byId, reasons, p, DuplicateMatchReason.EMAIL));
        }
        String linkedin = PersonNormalizer.linkedin(linkedinUrl);
        if (linkedin != null) {
            people.findByLinkedinUrlNormalized(linkedin)
                    .forEach(p -> add(byId, reasons, p, DuplicateMatchReason.LINKEDIN_URL));
        }
        String phoneNormalized = PersonNormalizer.phone(phone);
        if (phoneNormalized != null) {
            people.findByPhoneNormalized(phoneNormalized).forEach(p -> add(byId, reasons, p, DuplicateMatchReason.PHONE));
        }
        if (name != null && !name.isBlank() && company != null && !company.isBlank()) {
            people.findByNameAndCompanyNormalized(name, company)
                    .forEach(p -> add(byId, reasons, p, DuplicateMatchReason.NAME_COMPANY));
        }
        List<Match> out = new ArrayList<>();
        for (Map.Entry<UUID, Person> e : byId.entrySet()) {
            Person p = e.getValue();
            if (p.getAnonymisedAt() != null || p.getId().equals(excludeId)) {
                continue;
            }
            out.add(new Match(p, reasons.get(e.getKey()).stream().sorted().toList()));
        }
        out.sort(Comparator.comparing((Match m) -> m.reasons().get(0)).thenComparing(m -> m.person().getName(),
                Comparator.nullsLast(Comparator.naturalOrder())));
        return out.size() > MAX_MATCHES ? out.subList(0, MAX_MATCHES) : out;
    }

    private static void add(Map<UUID, Person> byId, Map<UUID, Set<DuplicateMatchReason>> reasons, Person p,
            DuplicateMatchReason reason) {
        byId.putIfAbsent(p.getId(), p);
        reasons.computeIfAbsent(p.getId(), k -> EnumSet.noneOf(DuplicateMatchReason.class)).add(reason);
    }

    /**
     * Queues pairs for the person after create, update or CV attach. A DISMISSED or MERGED pair is never re-opened;
     * an OPEN pair gets the current reasons.
     */
    public void record(Person person) {
        if (person.getAnonymisedAt() != null) {
            return;
        }
        for (Match m : find(person.getName(), person.getCompany(), person.getEmail(), person.getPhone(),
                person.getLinkedinUrl(), person.getId())) {
            record(person.getId(), m.person().getId(), m.reasons());
        }
    }

    public void record(UUID one, UUID other, List<DuplicateMatchReason> reasons) {
        UUID a = DuplicateCandidate.before(one, other) ? one : other;
        UUID b = a.equals(one) ? other : one;
        List<String> names = reasons.stream().map(Enum::name).toList();
        var existing = candidates.findByPersonAIdAndPersonBId(a, b);
        if (existing.isEmpty()) {
            candidates.save(DuplicateCandidate.of(a, b, names));
        } else if (existing.get().getStatus() == DuplicateStatus.OPEN) {
            existing.get().setReasons(new ArrayList<>(names));
            candidates.save(existing.get());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<DuplicateCandidateResponse> list(List<DuplicateStatus> status, UUID personId, Integer page,
            Integer size, List<String> sort) {
        List<DuplicateStatus> wanted = status == null || status.isEmpty() ? List.of(DuplicateStatus.OPEN) : status;
        var pageable = Paging.of(page, size, sort, Map.of("detectedAt", "detectedAt"), Sort.by(Sort.Order.desc("detectedAt")));
        Page<DuplicateCandidate> result = personId == null ? candidates.findByStatusIn(wanted, pageable)
                : candidates.searchByPerson(wanted, personId, pageable);
        return PageResponse.of(result, result.getContent().stream().map(this::response).toList());
    }

    public DuplicateCandidateResponse dismiss(UUID id, UUID actorId) {
        DuplicateCandidate d = candidates.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Duplicate pair " + id + " not found."));
        if (d.getStatus() != DuplicateStatus.OPEN) {
            throw new ApiException(ErrorCode.STATE_CONFLICT, "Only open pairs can be dismissed.");
        }
        d.setStatus(DuplicateStatus.DISMISSED);
        d.setResolvedAt(Instant.now());
        d.setResolvedBy(actorId);
        return response(candidates.save(d));
    }

    private DuplicateCandidateResponse response(DuplicateCandidate d) {
        Map<UUID, Person> both = people.findAllByIdIn(List.of(d.getPersonAId(), d.getPersonBId())).stream()
                .collect(Collectors.toMap(Person::getId, p -> p));
        return new DuplicateCandidateResponse(d.getId(), mapper.summary(both.get(d.getPersonAId())),
                mapper.summary(both.get(d.getPersonBId())),
                d.getReasons().stream().map(DuplicateMatchReason::valueOf).toList(), d.getStatus(), d.getDetectedAt(),
                d.getResolvedAt(), d.getResolvedBy() == null ? null : users.ref(d.getResolvedBy()).orElse(null));
    }
}
