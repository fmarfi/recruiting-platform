package com.recruiting.platform.pipeline.bulk.app;

import com.recruiting.platform.contracts.BulkActionType;
import com.recruiting.platform.contracts.BulkOutcome;
import com.recruiting.platform.contracts.BulkResultCode;
import com.recruiting.platform.contracts.JobOrderStatus;
import com.recruiting.platform.contracts.ListEntity;
import com.recruiting.platform.pipeline.BulkActionHandler;
import com.recruiting.platform.pipeline.BulkCommand;
import com.recruiting.platform.pipeline.BulkItemResult;
import com.recruiting.platform.pipeline.domain.JobOrder;
import com.recruiting.platform.pipeline.domain.JobOrderRepository;
import com.recruiting.platform.pipeline.joborder.app.JobOrderStatusService;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Bulk REASSIGN_OWNER, CHANGE_STATUS, ADD_TAG and REMOVE_TAG on job orders; status goes through the normal service. */
@Component
class JobOrderBulkHandler implements BulkActionHandler {

    private final JobOrderRepository repo;
    private final JobOrderStatusService status;

    JobOrderBulkHandler(JobOrderRepository repo, JobOrderStatusService status) {
        this.repo = repo;
        this.status = status;
    }

    @Override
    public boolean supports(ListEntity entity, BulkActionType action) {
        return entity == ListEntity.JOB_ORDERS && action != BulkActionType.SET_NEXT_ACTION
                && action != BulkActionType.REQUEST_CONSENT;
    }

    @Override
    public BulkItemResult handle(BulkCommand c, UUID id) {
        Optional<JobOrder> found = repo.findById(id);
        if (found.isEmpty()) {
            return BulkItemResult.failed(BulkResultCode.NOT_FOUND);
        }
        JobOrder j = found.get();
        switch (c.action()) {
            case REASSIGN_OWNER -> {
                if (c.params().ownerId().equals(j.getOwnerId())) {
                    return BulkItemResult.skipped(BulkResultCode.NO_CHANGE);
                }
                j.setOwnerId(c.params().ownerId());
                repo.saveAndFlush(j);
                return BulkItemResult.ok();
            }
            case CHANGE_STATUS -> {
                JobOrderStatus to = JobOrderStatus.valueOf(c.params().status());
                if (j.getStatus() == to) {
                    return BulkItemResult.skipped(BulkResultCode.NO_CHANGE);
                }
                status.change(id, to, c.params().reason(), null);
                return BulkItemResult.ok();
            }
            default -> {
                BulkItemResult r = TagOps.apply(j.getTags(), c.action(), c.params().tag(), j::setTags);
                if (r.outcome() == BulkOutcome.OK) {
                    repo.saveAndFlush(j);
                }
                return r;
            }
        }
    }
}
