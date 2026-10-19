package com.recruiting.platform.pipeline.proposal.app;

import com.recruiting.platform.pipeline.domain.Proposal;
import com.recruiting.platform.pipeline.proposal.web.dto.ProposalResponse;
import com.recruiting.platform.pipeline.shared.Refs;
import java.util.List;
import org.springframework.stereotype.Component;

/** Builds proposal responses with batched account and owner lookups. */
@Component
public class ProposalMapper {

    private final Refs refs;

    ProposalMapper(Refs refs) {
        this.refs = refs;
    }

    public ProposalResponse toResponse(Proposal p) {
        return toResponses(List.of(p)).get(0);
    }

    public List<ProposalResponse> toResponses(List<Proposal> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Refs.Lookup l = refs.load(list.stream().map(Proposal::getAccountId).toList(), List.of(),
                list.stream().map(Proposal::getOwnerId).toList());
        return list.stream().map(p -> new ProposalResponse(p.getId(), l.account(p.getAccountId()), p.getTitle(),
                p.getTerms(), p.getFeeAmount() == null ? null : p.getFeeAmount().toPlainString(), p.getCurrency(),
                p.getStatus(), p.getStatusChangedAt(), p.getStatusReason(), p.getSentOn(), p.getDecidedOn(),
                l.user(p.getOwnerId()), p.getCreatedAt(), p.getUpdatedAt(), p.getVersion())).toList();
    }
}
