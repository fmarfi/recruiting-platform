package com.recruiting.platform.pipeline.bulk.app;

import com.recruiting.platform.contracts.BulkActionType;
import com.recruiting.platform.contracts.BulkOutcome;
import com.recruiting.platform.contracts.BulkResultCode;
import com.recruiting.platform.contracts.ListEntity;
import com.recruiting.platform.contracts.ProposalStatus;
import com.recruiting.platform.pipeline.BulkActionHandler;
import com.recruiting.platform.pipeline.BulkCommand;
import com.recruiting.platform.pipeline.BulkItemResult;
import com.recruiting.platform.pipeline.domain.Proposal;
import com.recruiting.platform.pipeline.domain.ProposalRepository;
import com.recruiting.platform.pipeline.proposal.app.ProposalStatusService;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Bulk REASSIGN_OWNER, CHANGE_STATUS, ADD_TAG and REMOVE_TAG on proposals; status goes through the normal service. */
@Component
class ProposalBulkHandler implements BulkActionHandler {

    private final ProposalRepository repo;
    private final ProposalStatusService status;

    ProposalBulkHandler(ProposalRepository repo, ProposalStatusService status) {
        this.repo = repo;
        this.status = status;
    }

    @Override
    public boolean supports(ListEntity entity, BulkActionType action) {
        return entity == ListEntity.PROPOSALS && action != BulkActionType.SET_NEXT_ACTION
                && action != BulkActionType.REQUEST_CONSENT;
    }

    @Override
    public BulkItemResult handle(BulkCommand c, UUID id) {
        Optional<Proposal> found = repo.findById(id);
        if (found.isEmpty()) {
            return BulkItemResult.failed(BulkResultCode.NOT_FOUND);
        }
        Proposal p = found.get();
        switch (c.action()) {
            case REASSIGN_OWNER -> {
                if (c.params().ownerId().equals(p.getOwnerId())) {
                    return BulkItemResult.skipped(BulkResultCode.NO_CHANGE);
                }
                p.setOwnerId(c.params().ownerId());
                repo.saveAndFlush(p);
                return BulkItemResult.ok();
            }
            case CHANGE_STATUS -> {
                ProposalStatus to = ProposalStatus.valueOf(c.params().status());
                if (p.getStatus() == to) {
                    return BulkItemResult.skipped(BulkResultCode.NO_CHANGE);
                }
                status.change(id, to, c.params().reason(), null);
                return BulkItemResult.ok();
            }
            default -> {
                BulkItemResult r = TagOps.apply(p.getTags(), c.action(), c.params().tag(), p::setTags);
                if (r.outcome() == BulkOutcome.OK) {
                    repo.saveAndFlush(p);
                }
                return r;
            }
        }
    }
}
