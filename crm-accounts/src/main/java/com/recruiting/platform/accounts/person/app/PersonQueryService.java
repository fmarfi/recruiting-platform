package com.recruiting.platform.accounts.person.app;

import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.accounts.person.web.dto.PersonSummaryResponse;
import com.recruiting.platform.accounts.shared.Specs;
import com.recruiting.platform.accounts.shared.dto.PageResponse;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.PersonType;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Filtered network list (A-12, UX-06) and the people predicate reused by global search (A-14). */
@Service
@Transactional(readOnly = true)
public class PersonQueryService {

    public static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");

    private final PersonRepository people;
    private final PersonMapper mapper;

    public PersonQueryService(PersonRepository people, PersonMapper mapper) {
        this.people = people;
        this.mapper = mapper;
    }

    public PageResponse<PersonSummaryResponse> list(String q, List<PersonType> types, UUID ownerId,
            List<ConsentStatus> consent, LocalDate lastContactBefore, LocalDate lastContactAfter, Pageable pageable) {
        Page<Person> page = people.findAll(spec(q, types, ownerId, consent, lastContactBefore, lastContactAfter),
                pageable);
        return PageResponse.of(page, mapper.summaries(page.getContent()));
    }

    /** Same filters as the list; used by search. */
    public Specification<Person> spec(String q, List<PersonType> types, UUID ownerId, List<ConsentStatus> consent,
            LocalDate lastContactBefore, LocalDate lastContactAfter) {
        return (root, query, cb) -> {
            List<Predicate> all = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                all.add(cb.or(Specs.unaccentContains(cb, root.get("name"), q),
                        Specs.unaccentContains(cb, root.get("company"), q),
                        Specs.unaccentContains(cb, root.get("email"), q)));
            }
            if (types != null && !types.isEmpty()) {
                all.add(root.get("type").in(types));
            }
            if (ownerId != null) {
                all.add(cb.equal(root.get("ownerId"), ownerId));
            }
            if (consent != null && !consent.isEmpty()) {
                all.add(root.get("consentStatus").in(consent));
            }
            if (lastContactBefore != null) {
                Instant end = lastContactBefore.plusDays(1).atStartOfDay(ZONE).toInstant();
                all.add(cb.or(cb.isNull(root.get("lastContactAt")), cb.lessThan(root.<Instant>get("lastContactAt"), end)));
            }
            if (lastContactAfter != null) {
                Instant start = lastContactAfter.atStartOfDay(ZONE).toInstant();
                all.add(cb.greaterThanOrEqualTo(root.<Instant>get("lastContactAt"), start));
            }
            return cb.and(all.toArray(new Predicate[0]));
        };
    }
}
