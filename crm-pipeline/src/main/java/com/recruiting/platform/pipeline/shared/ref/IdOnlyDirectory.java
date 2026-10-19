package com.recruiting.platform.pipeline.shared.ref;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Stand-in for the three directories until the accounts and security public APIs are merged. It knows no names
 * (the id is shown instead) and treats every id as existing. The real adapters are marked
 * {@code @Primary} and win over this bean; see the hand-off note.
 */
@Component
class IdOnlyDirectory implements AccountDirectory, PersonDirectory, UserDirectory {

    @Override
    public Map<UUID, AccountInfo> accounts(Collection<UUID> ids) {
        return ids.stream().distinct().collect(Collectors.toMap(id -> id, id -> new AccountInfo(id, id.toString(),
                "", null, null, null, null, com.recruiting.platform.contracts.AccountStatus.ACTIVE, null, null, 0, null,
                null, null, 0L)));
    }

    @Override
    public long countActiveAccounts() {
        return 0;
    }

    @Override
    public Map<UUID, PersonInfo> people(Collection<UUID> ids) {
        return ids.stream().distinct().collect(Collectors.toMap(id -> id, id -> new PersonInfo(id, id.toString(), null,
                null, com.recruiting.platform.contracts.PersonType.CANDIDATE, null, null, null, null,
                com.recruiting.platform.contracts.ConsentStatus.NOT_SENT,
                com.recruiting.platform.contracts.RetentionStatus.ACTIVE)));
    }

    @Override
    public Page<PersonInfo> suggestions(String q, Collection<UUID> excludeIds, Pageable pageable) {
        return new PageImpl<>(List.of(), pageable, 0);
    }

    @Override
    public Map<UUID, UserInfo> users(Collection<UUID> ids) {
        return ids.stream().distinct().collect(Collectors.toMap(id -> id, id -> new UserInfo(id, id.toString())));
    }
}
