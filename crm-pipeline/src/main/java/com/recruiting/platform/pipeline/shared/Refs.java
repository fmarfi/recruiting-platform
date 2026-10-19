package com.recruiting.platform.pipeline.shared;

import com.recruiting.platform.pipeline.shared.dto.AccountRef;
import com.recruiting.platform.pipeline.shared.dto.PersonRef;
import com.recruiting.platform.pipeline.shared.dto.PersonSummary;
import com.recruiting.platform.pipeline.shared.dto.UserRef;
import com.recruiting.platform.pipeline.shared.ref.AccountDirectory;
import com.recruiting.platform.pipeline.shared.ref.AccountInfo;
import com.recruiting.platform.pipeline.shared.ref.PersonDirectory;
import com.recruiting.platform.pipeline.shared.ref.PersonInfo;
import com.recruiting.platform.pipeline.shared.ref.UserDirectory;
import com.recruiting.platform.pipeline.shared.ref.UserInfo;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Batch lookup of the account, person and user references shown in responses. A view mapper calls the
 * {@code load*} methods once per list and then reads from the returned lookup, to avoid one query per row.
 */
@Component
public class Refs {

    private final AccountDirectory accounts;
    private final PersonDirectory people;
    private final UserDirectory users;

    Refs(AccountDirectory accounts, PersonDirectory people, UserDirectory users) {
        this.accounts = accounts;
        this.people = people;
        this.users = users;
    }

    /** Resolved references for one response. */
    public final class Lookup {
        private final Map<UUID, AccountInfo> acc;
        private final Map<UUID, PersonInfo> per;
        private final Map<UUID, UserInfo> usr;

        Lookup(Map<UUID, AccountInfo> acc, Map<UUID, PersonInfo> per, Map<UUID, UserInfo> usr) {
            this.acc = acc;
            this.per = per;
            this.usr = usr;
        }

        public AccountRef account(UUID id) {
            AccountInfo a = id == null ? null : acc.get(id);
            return a == null ? null : new AccountRef(a.id(), a.name(), a.status());
        }

        public AccountInfo accountInfo(UUID id) {
            return id == null ? null : acc.get(id);
        }

        public PersonRef personRef(UUID id) {
            PersonInfo p = id == null ? null : per.get(id);
            return p == null ? null : new PersonRef(p.id(), p.name());
        }

        public PersonSummary person(UUID id) {
            PersonInfo p = id == null ? null : per.get(id);
            return p == null ? null : new PersonSummary(p.id(), p.name(), p.title(), p.company(), p.type(), p.city(),
                    p.email(), user(p.ownerId()), p.lastContactAt(), p.consentStatus(), p.retentionStatus());
        }

        public UserRef user(UUID id) {
            UserInfo u = id == null ? null : usr.get(id);
            return u == null ? null : new UserRef(u.id(), u.name());
        }
    }

    /**
     * Resolves the given ids in one go.
     *
     * @param accountIds account ids (nulls ignored)
     * @param personIds  person ids (nulls ignored)
     * @param userIds    user ids (nulls ignored)
     * @return the lookup
     */
    public Lookup load(Collection<UUID> accountIds, Collection<UUID> personIds, Collection<UUID> userIds) {
        Map<UUID, AccountInfo> acc = accountIds.isEmpty() ? Map.of() : accounts.accounts(clean(accountIds));
        Map<UUID, PersonInfo> per = personIds.isEmpty() ? Map.of() : people.people(clean(personIds));
        Map<UUID, UserInfo> usr = new HashMap<>();
        Collection<UUID> uids = new java.util.HashSet<>(userIds);
        per.values().forEach(p -> uids.add(p.ownerId()));
        acc.values().forEach(a -> uids.add(a.ownerId()));
        if (!clean(uids).isEmpty()) {
            usr.putAll(users.users(clean(uids)));
        }
        return new Lookup(acc, per, usr);
    }

    /**
     * Returns the account or answers 404.
     *
     * @param id account id
     * @return the account
     */
    public AccountInfo requireAccount(UUID id) {
        AccountInfo a = accounts.accounts(java.util.List.of(id)).get(id);
        if (a == null) {
            throw Transitions.notFound("Account");
        }
        return a;
    }

    /**
     * Answers 404 when the person does not exist.
     *
     * @param id person id
     */
    public void requirePerson(UUID id) {
        if (!people.people(java.util.List.of(id)).containsKey(id)) {
            throw Transitions.notFound("Person");
        }
    }

    /**
     * Answers 404 when the user does not exist.
     *
     * @param id user id
     */
    public void requireUser(UUID id) {
        if (!users.users(java.util.List.of(id)).containsKey(id)) {
            throw Transitions.notFound("User");
        }
    }

    private static Collection<UUID> clean(Collection<UUID> ids) {
        return ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }
}
