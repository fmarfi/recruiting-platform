package com.recruiting.platform.pipeline.shared.ref;

import com.recruiting.platform.accounts.PersonApi;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/** {@link PersonDirectory} over the accounts module's public {@link PersonApi}. */
@Component
class PersonDirectoryAdapter implements PersonDirectory {

    private final PersonApi api;

    PersonDirectoryAdapter(PersonApi api) {
        this.api = api;
    }

    @Override
    public Map<UUID, PersonInfo> people(Collection<UUID> ids) {
        return api.findByIds(ids).values().stream().map(PersonDirectoryAdapter::info)
                .collect(Collectors.toMap(PersonInfo::id, p -> p));
    }

    @Override
    public Page<PersonInfo> suggestions(String q, Collection<UUID> excludeIds, Pageable pageable) {
        return api.suggest(q, excludeIds, pageable).map(PersonDirectoryAdapter::info);
    }

    private static PersonInfo info(PersonApi.PersonSummary s) {
        return new PersonInfo(s.id(), s.name(), s.title(), s.company(), s.type(), s.city(), s.email(), s.ownerId(),
                s.lastContactAt(), s.consentStatus(), s.retentionStatus());
    }
}
