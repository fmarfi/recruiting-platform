package com.recruiting.platform.pipeline.proposal.app;

import com.recruiting.platform.contracts.ProposalStatus;
import com.recruiting.platform.contracts.ProposalStatusChanged;
import com.recruiting.platform.pipeline.domain.Proposal;
import com.recruiting.platform.pipeline.domain.ProposalRepository;
import com.recruiting.platform.pipeline.shared.Actor;
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

    private final ProposalRepository repo;
    private final ApplicationEventPublisher events;

    ProposalStatusService(ProposalRepository repo, ApplicationEventPublisher events) {
        this.repo = repo;
        this.events = events;
    }

    @Transactional
    public Proposal change(UUID id, ProposalStatus to, String reason, Long version, Actor actor) {
        Proposal p = repo.findById(id).orElseThrow(() -> Transitions.notFound("Proposal"));
        Transitions.checkVersion(version, p.getVersion());
        ProposalStatus from = p.getStatus();
        String clean = Transitions.check(from, to, reason, actor.elevated());
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
        events.publishEvent(new ProposalStatusChanged(Uuids.v7(), Instant.now(), actor.id(), p.getId(),
                p.getAccountId(), from, to, clean));
        return p;
    }
}
