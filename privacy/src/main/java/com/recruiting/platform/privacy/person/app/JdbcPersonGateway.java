package com.recruiting.platform.privacy.person.app;

import com.recruiting.platform.contracts.PersonAnonymised;
import com.recruiting.platform.contracts.PersonType;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Interim adapter: reads the {@code person} table with SQL and applies the ADR 0009 step 2 changes itself, because
 * accounts' {@code PersonDataApi} was not merged when privacy was built. It writes the Envers delete row the way
 * Hibernate would, and publishes {@link PersonAnonymised}, which accounts will do once the real API replaces this.
 */
@Component
class JdbcPersonGateway implements PersonGateway {

    private static final RowMapper<PersonInfo> MAPPER = (rs, row) -> new PersonInfo(
            rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("email"),
            PersonType.valueOf(rs.getString("type")), rs.getString("title"), rs.getString("company"),
            rs.getString("city"), rs.getObject("owner_id", UUID.class), instant(rs.getTimestamp("last_contact_at")),
            instant(rs.getTimestamp("created_at")), rs.getTimestamp("anonymised_at") != null);

    private static final String SELECT = "select id, name, email, type, title, company, city, owner_id, "
            + "last_contact_at, created_at, anonymised_at from person ";

    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    JdbcPersonGateway(JdbcTemplate jdbc, ApplicationEventPublisher events, Clock clock) {
        this.jdbc = jdbc;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonInfo> find(UUID id) {
        return jdbc.query(SELECT + "where id = ?", MAPPER, id).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, PersonInfo> findAll(Collection<UUID> ids) {
        Map<UUID, PersonInfo> result = new HashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        String marks = String.join(",", ids.stream().map(i -> "?").toList());
        jdbc.query(SELECT + "where id in (" + marks + ")", MAPPER, ids.toArray()).forEach(p -> result.put(p.id(), p));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> idsWithoutPrivacy(int limit) {
        return jdbc.queryForList("select p.id from person p left join person_privacy pp on pp.person_id = p.id "
                + "where pp.person_id is null and p.anonymised_at is null limit ?", UUID.class, limit);
    }

    @Override
    @Transactional
    public void anonymise(UUID personId, UUID actorId) {
        jdbc.update("update person set name = 'Anonymised person', email = null, phone = null, linkedin_url = null, "
                + "how_we_know = null, notes = null, title = null, city = null, company = null, "
                + "anonymised_at = now(), retention_status = 'ANONYMISED', updated_at = now(), "
                + "version = version + 1 where id = ?", personId);
        jdbc.update("delete from person_employment where person_id = ?", personId);
        jdbc.update("delete from network_link where from_person_id = ? or to_person_id = ?", personId, personId);
        events.publishEvent(new PersonAnonymised(UUID.randomUUID(), clock.instant(), actorId, personId, false));
    }

    @Override
    @Transactional
    public void delete(UUID personId, UUID actorId) {
        jdbc.update("delete from person where id = ?", personId);
        writeEnversDeleteRow(personId, actorId);
        events.publishEvent(new PersonAnonymised(UUID.randomUUID(), clock.instant(), actorId, personId, true));
    }

    /** Same rows Envers writes for a JPA delete: close the current revision and add an id-only revtype 2 row. */
    private void writeEnversDeleteRow(UUID personId, UUID actorId) {
        Long rev = jdbc.queryForObject("select nextval('revinfo_seq')", Long.class);
        jdbc.update("insert into revinfo (rev, revtstmp, user_id) values (?, ?, ?)", rev, clock.millis(), actorId);
        jdbc.update("update person_aud set revend = ? where id = ? and revend is null", rev, personId);
        jdbc.update("insert into person_aud (id, rev, revtype) values (?, ?, 2)", personId, rev);
    }

    private static Instant instant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
