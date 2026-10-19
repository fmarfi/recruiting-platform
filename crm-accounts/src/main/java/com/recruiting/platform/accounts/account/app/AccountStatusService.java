package com.recruiting.platform.accounts.account.app;

import com.recruiting.platform.accounts.account.web.dto.AccountResponse;
import com.recruiting.platform.accounts.account.web.dto.AccountStatusChangeRequest;
import com.recruiting.platform.accounts.domain.Account;
import com.recruiting.platform.accounts.domain.AccountOpenJobRepository;
import com.recruiting.platform.accounts.domain.AccountRepository;
import com.recruiting.platform.accounts.shared.Actor;
import com.recruiting.platform.accounts.shared.Events;
import com.recruiting.platform.accounts.shared.Versions;
import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.AccountStatusChanged;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.IllegalTransitionException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account status moves (A-02, docs/status-lifecycles.md). Order of checks: transition table (409), manager-only
 * (403), reason (400), open job orders guard (409 with blockingItems, from the account_open_job projection).
 */
@Service
@Transactional
public class AccountStatusService {

    private final AccountRepository accounts;
    private final AccountOpenJobRepository openJobs;
    private final AccountMapper mapper;
    private final ApplicationEventPublisher events;

    public AccountStatusService(AccountRepository accounts, AccountOpenJobRepository openJobs, AccountMapper mapper,
            ApplicationEventPublisher events) {
        this.accounts = accounts;
        this.openJobs = openJobs;
        this.mapper = mapper;
        this.events = events;
    }

    public AccountResponse change(UUID id, AccountStatusChangeRequest r, Actor actor) {
        Account a = accounts.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Account " + id + " not found."));
        Versions.check(r.version(), a.getVersion());
        AccountStatus from = a.getStatus();
        AccountStatus to = r.status();
        if (!from.canMoveTo(to)) {
            throw new IllegalTransitionException(from, to);
        }
        if (from.requiresElevatedRole(to) && !actor.elevated()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Only a manager or admin may move an account from "
                    + from.labelEn() + " to " + to.labelEn() + ".");
        }
        IllegalTransitionException.check(from, to, r.reason());
        if (to == AccountStatus.CLOSED) {
            failIfOpenJobs(a);
        }
        String reason = r.reason() == null || r.reason().isBlank() ? null : r.reason().trim();
        a.setStatus(to);
        a.setStatusChangedAt(Instant.now());
        a = accounts.saveAndFlush(a);
        events.publishEvent(new AccountStatusChanged(Events.newId(), Instant.now(), actor.id(), a.getId(), from,
                to, reason));
        return mapper.response(a);
    }

    private void failIfOpenJobs(Account a) {
        List<Map<String, Object>> blocking = openJobs.findByAccountIdAndOpenTrue(a.getId()).stream()
                .map(j -> Map.<String, Object>of("id", j.getJobOrderId(), "title", String.valueOf(j.getTitle()),
                        "type", "JOB_ORDER"))
                .toList();
        if (!blocking.isEmpty()) {
            throw new ApiException(ErrorCode.TRANSITION_GUARD_FAILED,
                    "The account has open job orders; fill or cancel them first.",
                    Map.of("currentStatus", a.getStatus().name(), "requestedStatus", AccountStatus.CLOSED.name(),
                            "blockingItems", blocking));
        }
    }
}
