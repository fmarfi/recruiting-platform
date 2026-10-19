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

    public PersonApiImpl(PersonRepository people) {
        this.people = people;
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

    private static PersonSummary toSummary(Person p) {
        return new PersonSummary(p.getId(), p.getName(), p.getType(), p.getTitle(), p.getCompany(), p.getEmail(),
                p.getOwnerId(), p.getAnonymisedAt() != null);
    }
}
