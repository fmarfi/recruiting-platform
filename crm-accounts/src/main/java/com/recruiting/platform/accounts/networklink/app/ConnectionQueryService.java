package com.recruiting.platform.accounts.networklink.app;

import com.recruiting.platform.accounts.domain.AccountRepository;
import com.recruiting.platform.accounts.domain.Contact;
import com.recruiting.platform.accounts.domain.ContactRepository;
import com.recruiting.platform.accounts.domain.NetworkLink;
import com.recruiting.platform.accounts.domain.NetworkLinkRepository;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.accounts.networklink.web.dto.AccountConnectionResponse;
import com.recruiting.platform.accounts.networklink.web.dto.PersonConnectionResponse;
import com.recruiting.platform.accounts.person.app.PersonMapper;
import com.recruiting.platform.accounts.person.web.dto.PersonSummaryResponse;
import com.recruiting.platform.accounts.shared.dto.PersonRef;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side of A-13: a person's connections and the connections of an account's contacts. */
@Service
@Transactional(readOnly = true)
public class ConnectionQueryService {

    private final NetworkLinkRepository links;
    private final PersonRepository people;
    private final ContactRepository contacts;
    private final AccountRepository accounts;
    private final PersonMapper mapper;

    public ConnectionQueryService(NetworkLinkRepository links, PersonRepository people, ContactRepository contacts,
            AccountRepository accounts, PersonMapper mapper) {
        this.links = links;
        this.people = people;
        this.contacts = contacts;
        this.accounts = accounts;
        this.mapper = mapper;
    }

    public List<PersonConnectionResponse> forPerson(UUID personId) {
        if (!people.existsById(personId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Person " + personId + " not found.");
        }
        List<NetworkLink> found = links.findByFromPersonIdOrToPersonId(personId, personId);
        Map<UUID, PersonSummaryResponse> others = others(personId, found);
        return found.stream()
                .map(l -> new PersonConnectionResponse(l.getId(), l.getType(), direction(l, personId),
                        others.get(other(l, personId)), l.getNote()))
                .sorted(Comparator.comparing((PersonConnectionResponse c) -> c.person().name())).toList();
    }

    public List<AccountConnectionResponse> forAccount(UUID accountId) {
        if (!accounts.existsById(accountId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Account " + accountId + " not found.");
        }
        List<AccountConnectionResponse> out = new ArrayList<>();
        for (Contact c : contacts.findByAccountIdAndEndedOnIsNull(accountId)) {
            people.findById(c.getPersonId()).ifPresent(contact -> {
                PersonRef ref = new PersonRef(contact.getId(), contact.getName());
                for (PersonConnectionResponse pc : forPerson(contact.getId())) {
                    out.add(new AccountConnectionResponse(ref, pc.person(), pc.type(), pc.direction()));
                }
            });
        }
        return out;
    }

    private Map<UUID, PersonSummaryResponse> others(UUID personId, List<NetworkLink> found) {
        List<UUID> ids = found.stream().map(l -> other(l, personId)).distinct().toList();
        Map<UUID, PersonSummaryResponse> out = new HashMap<>();
        mapper.summaries(people.findAllByIdIn(ids)).forEach(s -> out.put(s.id(), s));
        return out;
    }

    private static UUID other(NetworkLink l, UUID personId) {
        return l.getFromPersonId().equals(personId) ? l.getToPersonId() : l.getFromPersonId();
    }

    private static String direction(NetworkLink l, UUID personId) {
        if (l.getType().isSymmetric()) {
            return "BOTH";
        }
        return l.getFromPersonId().equals(personId) ? "OUTGOING" : "INCOMING";
    }
}
