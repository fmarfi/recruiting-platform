package com.recruiting.platform.accounts.person.app;

import com.recruiting.platform.accounts.PersonApi;
import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonRepository;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implements the public PersonApi for other modules; entities never leave accounts. */
@Service
@Transactional(readOnly = true)
public class PersonApiImpl implements PersonApi {

    private final PersonRepository people;
    private final PersonQueryService queries;

    public PersonApiImpl(PersonRepository people, PersonQueryService queries) {
        this.people = people;
        this.queries = queries;
    }

    @Override
    public Optional<PersonSummary> findById(UUID id) {
        return people.findById(id).map(PersonApiImpl::toSummary);
    }

    @Override
    public Map<UUID, PersonSummary> findByIds(Collection<UUID> ids) {
        return people.findAllByIdIn(ids).stream().map(PersonApiImpl::toSummary)
                .collect(Collectors.toMap(PersonSummary::id, Function.identity()));
    }

    @Override
    public org.springframework.data.domain.Page<PersonSummary> suggest(String q, Collection<UUID> excludeIds,
            org.springframework.data.domain.Pageable pageable) {
        var base = queries.spec(q, null, null, null, null, null);
        org.springframework.data.jpa.domain.Specification<Person> spec = (root, query, cb) -> {
            var all = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            all.add(base.toPredicate(root, query, cb));
            all.add(cb.isNull(root.get("anonymisedAt")));
            if (excludeIds != null && !excludeIds.isEmpty()) {
                all.add(cb.not(root.get("id").in(excludeIds)));
            }
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                var first = cb.selectCase().when(root.get("type").in(
                        com.recruiting.platform.contracts.PersonType.CANDIDATE,
                        com.recruiting.platform.contracts.PersonType.EXECUTIVE), 0).otherwise(1);
                query.orderBy(cb.asc(first), cb.asc(root.get("name")));
            }
            return cb.and(all.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return people.findAll(spec, org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(),
                pageable.getPageSize())).map(PersonApiImpl::toSummary);
    }

    private static PersonSummary toSummary(Person p) {
        return new PersonSummary(p.getId(), p.getName(), p.getType(), p.getTitle(), p.getCompany(), p.getEmail(),
                p.getOwnerId(), p.getAnonymisedAt() != null, p.getCity(), p.getLastContactAt(), p.getConsentStatus(),
                p.getRetentionStatus());
    }
}
