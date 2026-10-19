package com.recruiting.platform.pipeline.proposal.app;

import com.recruiting.platform.contracts.ProposalStatus;
import com.recruiting.platform.contracts.ProposalStatusChanged;
import com.recruiting.platform.pipeline.domain.Proposal;
import com.recruiting.platform.pipeline.domain.ProposalRepository;
import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.security.CurrentUser;
import com.recruiting.platform.pipeline.shared.Transitions;
import com.recruiting.platform.pipeline.shared.Uuids;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Proposal status moves (A-05): transition table, reason for REJECTED, sent and decided dates, event. */
@Service
public class ProposalStatusService {

    private final CurrentUser currentUser;
    private final ProposalRepository repo;
    private final ApplicationEventPublisher events;

    ProposalStatusService(ProposalRepository repo, ApplicationEventPublisher events, CurrentUser currentUser) {
        this.currentUser = currentUser;
        this.repo = repo;
        this.events = events;
    }

    @Transactional
    public Proposal change(UUID id, ProposalStatus to, String reason, Long version) {
        Proposal p = repo.findById(id).orElseThrow(() -> Transitions.notFound("Proposal"));
        Transitions.checkVersion(version, p.getVersion());
        ProposalStatus from = p.getStatus();
        String clean = Transitions.check(from, to, reason, currentUser.hasAtLeast(Role.MANAGER));
        p.setStatus(to);
        p.setStatusChangedAt(Instant.now());
        p.setStatusReason(clean);
        switch (to) {
            case SENT -> {
                p.setSentOn(LocalDate.now());
                p.setDecidedOn(null);
            }
            case ACCEPTED, REJECTED -> p.setDecidedOn(LocalDate.now());
            case DRAFT -> {
                p.setSentOn(null);
                p.setDecidedOn(null);
            }
        }
        repo.saveAndFlush(p);
        events.publishEvent(new ProposalStatusChanged(Uuids.v7(), Instant.now(), currentUser.id(), p.getId(),
                p.getAccountId(), from, to, clean));
        return p;
    }
}
