package com.recruiting.platform.accounts.account.app;

import com.recruiting.platform.accounts.account.web.dto.AccountCreateRequest;
import com.recruiting.platform.accounts.account.web.dto.AccountResponse;
import com.recruiting.platform.accounts.account.web.dto.AccountUpdateRequest;
import com.recruiting.platform.accounts.domain.Account;
import com.recruiting.platform.accounts.domain.AccountRepository;
import com.recruiting.platform.accounts.shared.Actor;
import com.recruiting.platform.accounts.shared.Events;
import com.recruiting.platform.accounts.shared.Patch;
import com.recruiting.platform.accounts.shared.PatchBinder;
import com.recruiting.platform.security.UserDirectory;
import com.recruiting.platform.accounts.shared.Versions;
import com.recruiting.platform.contracts.AccountCreated;
import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.AccountUpdated;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Create, read and update accounts (A-01). Status changes are in AccountStatusService. */
@Service
@Transactional
public class AccountService {

    private final AccountRepository accounts;
    private final AccountMapper mapper;
    private final UserDirectory users;
    private final ApplicationEventPublisher events;

    public AccountService(AccountRepository accounts, AccountMapper mapper, UserDirectory users,
            ApplicationEventPublisher events) {
        this.accounts = accounts;
        this.mapper = mapper;
        this.users = users;
        this.events = events;
    }

    public AccountResponse create(AccountCreateRequest r, Actor actor) {
        UUID ownerId = r.ownerId() != null ? r.ownerId() : actor.id();
        if (ownerId == null || !users.isActive(ownerId)) {
            throw PatchBinder.validation("ownerId", "REQUIRED", "an active owner is required");
        }
        failIfNameTaken(r.name(), null);
        Account a = new Account();
        a.setName(r.name().trim());
        a.setSector(r.sector().trim());
        a.setCity(r.city());
        a.setWebsite(r.website());
        a.setNotes(r.notes());
        a.setOwnerId(ownerId);
        a.setStatus(AccountStatus.INITIAL);
        a = accounts.saveAndFlush(a);
        events.publishEvent(new AccountCreated(Events.newId(), Instant.now(), actor.id(), a.getId(), a.getName(),
                ownerId, a.getStatus()));
        return mapper.response(a);
    }

    @Transactional(readOnly = true)
    public AccountResponse get(UUID id) {
        return mapper.response(find(id));
    }

    public AccountResponse update(UUID id, Patch<AccountUpdateRequest> patch, Actor actor) {
        Account a = find(id);
        AccountUpdateRequest r = patch.body();
        Versions.check(r.version(), a.getVersion());
        PatchBinder.notNull(patch, "name", r.name());
        PatchBinder.notNull(patch, "sector", r.sector());
        PatchBinder.notNull(patch, "ownerId", r.ownerId());
        if (patch.has("name")) {
            failIfNameTaken(r.name(), a.getId());
            a.setName(r.name().trim());
        }
        if (patch.has("sector")) {
            a.setSector(r.sector().trim());
        }
        if (patch.has("city")) {
            a.setCity(r.city());
        }
        if (patch.has("website")) {
            a.setWebsite(r.website());
        }
        if (patch.has("notes")) {
            a.setNotes(r.notes());
        }
        boolean ownerChanged = patch.has("ownerId") && !r.ownerId().equals(a.getOwnerId());
        if (ownerChanged) {
            if (!users.isActive(r.ownerId())) {
                throw PatchBinder.validation("ownerId", "INVALID", "unknown or inactive user");
            }
            a.setOwnerId(r.ownerId());
        }
        a = accounts.saveAndFlush(a);
        events.publishEvent(new AccountUpdated(Events.newId(), Instant.now(), actor.id(), a.getId(), a.getName(),
                a.getOwnerId(), ownerChanged));
        return mapper.response(a);
    }

    private Account find(UUID id) {
        return accounts.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Account " + id + " not found."));
    }

    private void failIfNameTaken(String name, UUID selfId) {
        accounts.findByNameIgnoreCase(name.trim()).filter(o -> !o.getId().equals(selfId)).ifPresent(o -> {
            throw new ApiException(ErrorCode.DUPLICATE, "An account with this name already exists.",
                    Map.of("existingId", o.getId()));
        });
    }
}
