package com.recruiting.platform.accounts.networklink.app;

import com.recruiting.platform.accounts.domain.NetworkLink;
import com.recruiting.platform.accounts.domain.NetworkLinkRepository;
import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.accounts.networklink.web.dto.NetworkLinkCreateRequest;
import com.recruiting.platform.accounts.networklink.web.dto.NetworkLinkResponse;
import com.recruiting.platform.accounts.networklink.web.dto.NetworkLinkUpdateRequest;
import com.recruiting.platform.accounts.shared.Patch;
import com.recruiting.platform.accounts.shared.PatchBinder;
import com.recruiting.platform.accounts.shared.UserLookup;
import com.recruiting.platform.accounts.shared.dto.PersonRef;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.LinkType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Network links between two people (A-13): create, read, change type or note, delete. */
@Service
@Transactional
public class NetworkLinkService {

    private final NetworkLinkRepository links;
    private final PersonRepository people;
    private final UserLookup users;

    public NetworkLinkService(NetworkLinkRepository links, PersonRepository people, UserLookup users) {
        this.links = links;
        this.people = people;
        this.users = users;
    }

    public NetworkLinkResponse create(NetworkLinkCreateRequest r) {
        if (r.fromPersonId().equals(r.toPersonId())) {
            throw PatchBinder.validation("toPersonId", "DIFFERENT", "a person cannot be linked to themselves");
        }
        requirePerson(r.fromPersonId());
        requirePerson(r.toPersonId());
        failIfDuplicate(r.fromPersonId(), r.toPersonId(), r.type(), null);
        NetworkLink l = new NetworkLink();
        l.setFromPersonId(r.fromPersonId());
        l.setToPersonId(r.toPersonId());
        l.setType(r.type());
        l.setNote(r.note());
        return response(links.saveAndFlush(l));
    }

    @Transactional(readOnly = true)
    public NetworkLinkResponse get(UUID id) {
        return response(find(id));
    }

    public NetworkLinkResponse update(UUID id, Patch<NetworkLinkUpdateRequest> patch) {
        NetworkLink l = find(id);
        NetworkLinkUpdateRequest r = patch.body();
        PatchBinder.notNull(patch, "type", r.type());
        if (patch.has("type")) {
            failIfDuplicate(l.getFromPersonId(), l.getToPersonId(), r.type(), l.getId());
            l.setType(r.type());
        }
        if (patch.has("note")) {
            l.setNote(r.note());
        }
        return response(links.saveAndFlush(l));
    }

    public void delete(UUID id) {
        links.delete(find(id));
    }

    /** Same (from, to, type) is a duplicate; for symmetric types the reverse direction is the same link. */
    private void failIfDuplicate(UUID from, UUID to, LinkType type, UUID selfId) {
        Optional<NetworkLink> same = links.findByFromPersonIdAndToPersonIdAndType(from, to, type)
                .filter(e -> !e.getId().equals(selfId));
        if (same.isEmpty() && type.isSymmetric()) {
            same = links.findByFromPersonIdAndToPersonIdAndType(to, from, type)
                    .filter(e -> !e.getId().equals(selfId));
        }
        same.ifPresent(e -> {
            throw new ApiException(ErrorCode.DUPLICATE, "This link already exists.", Map.of("existingId", e.getId()));
        });
    }

    private NetworkLink find(UUID id) {
        return links.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Network link " + id + " not found."));
    }

    private void requirePerson(UUID id) {
        if (!people.existsById(id)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Person " + id + " not found.");
        }
    }

    private NetworkLinkResponse response(NetworkLink l) {
        Map<UUID, Person> found = new HashMap<>();
        people.findAllByIdIn(List.of(l.getFromPersonId(), l.getToPersonId())).forEach(p -> found.put(p.getId(), p));
        return new NetworkLinkResponse(l.getId(), ref(found.get(l.getFromPersonId())),
                ref(found.get(l.getToPersonId())), l.getType(), l.getNote(), l.getCreatedAt(),
                users.refs(List.of(l.getCreatedBy())).get(l.getCreatedBy()));
    }

    private static PersonRef ref(Person p) {
        return new PersonRef(p.getId(), p.getName());
    }
}
