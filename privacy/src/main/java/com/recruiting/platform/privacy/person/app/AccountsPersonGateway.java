package com.recruiting.platform.privacy.person.app;

import com.recruiting.platform.accounts.PersonApi;
import com.recruiting.platform.accounts.PersonApi.PersonSummary;
import com.recruiting.platform.accounts.PersonDataApi;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter on accounts' public APIs ({@link PersonApi} to read, {@link PersonDataApi} to anonymise or delete; accounts
 * publishes PersonAnonymised). Only {@code idsWithoutPrivacy} is SQL: accounts has no API for it yet, and it reads
 * only ids.
 */
@Component
class AccountsPersonGateway implements PersonGateway {

    private final PersonApi persons;
    private final PersonDataApi data;
    private final JdbcTemplate jdbc;

    AccountsPersonGateway(PersonApi persons, PersonDataApi data, JdbcTemplate jdbc) {
        this.persons = persons;
        this.data = data;
        this.jdbc = jdbc;
    }

    @Override
    public Optional<PersonInfo> find(UUID id) {
        return persons.findById(id).map(AccountsPersonGateway::info);
    }

    @Override
    public Map<UUID, PersonInfo> findAll(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return persons.findByIds(ids).values().stream()
                .collect(Collectors.toMap(PersonSummary::id, AccountsPersonGateway::info));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> idsWithoutPrivacy(int limit) {
        return jdbc.queryForList("select p.id from person p left join person_privacy pp on pp.person_id = p.id "
                + "where pp.person_id is null and p.anonymised_at is null limit ?", UUID.class, limit);
    }

    @Override
    public void anonymise(UUID personId, UUID actorId) {
        data.anonymise(personId, actorId);
    }

    @Override
    public void delete(UUID personId, UUID actorId) {
        data.delete(personId, actorId);
    }

    private static PersonInfo info(PersonSummary p) {
        // createdAt is not in PersonSummary; the retention clock starts from lastContactAt or the privacy record
        return new PersonInfo(p.id(), p.name(), p.email(), p.type(), p.title(), p.company(), p.city(), p.ownerId(),
                p.lastContactAt(), null, p.anonymised());
    }
}
