package com.recruiting.platform.pipeline.bulk.app;

import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.BulkActionType;
import com.recruiting.platform.contracts.BulkResultCode;
import com.recruiting.platform.contracts.ListEntity;
import com.recruiting.platform.pipeline.BulkActionHandler;
import com.recruiting.platform.pipeline.BulkCommand;
import com.recruiting.platform.pipeline.BulkItemResult;
import com.recruiting.platform.pipeline.domain.JobOrder;
import com.recruiting.platform.pipeline.domain.JobOrderRepository;
import com.recruiting.platform.pipeline.nextaction.app.NextActionService;
import com.recruiting.platform.pipeline.nextaction.web.dto.NextActionCreateRequest;
import com.recruiting.platform.pipeline.shared.Refs;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Bulk SET_NEXT_ACTION for people, accounts and job orders: one next action per record through the normal service. The
 * title is written to the next action only, never to the bulk audit (ADR 0010 amendment 1).
 */
@Component
class NextActionBulkHandler implements BulkActionHandler {

    private final NextActionService nextActions;
    private final JobOrderRepository jobOrders;
    private final Refs refs;

    NextActionBulkHandler(NextActionService nextActions, JobOrderRepository jobOrders, Refs refs) {
        this.nextActions = nextActions;
        this.jobOrders = jobOrders;
        this.refs = refs;
    }

    @Override
    public boolean supports(ListEntity entity, BulkActionType action) {
        return action == BulkActionType.SET_NEXT_ACTION && entity != ListEntity.PROPOSALS;
    }

    @Override
    public BulkItemResult handle(BulkCommand c, UUID id) {
        UUID person = null;
        UUID account = null;
        UUID job = null;
        try {
            switch (c.entity()) {
                case PEOPLE -> {
                    refs.requirePerson(id);
                    person = id;
                }
                case ACCOUNTS -> {
                    if (refs.requireAccount(id).status() == AccountStatus.CLOSED) {
                        return BulkItemResult.failed(BulkResultCode.STATE_CONFLICT);
                    }
                    account = id;
                }
                default -> {
                    JobOrder j = jobOrders.findById(id).orElse(null);
                    if (j == null) {
                        return BulkItemResult.failed(BulkResultCode.NOT_FOUND);
                    }
                    if (!j.getStatus().isOpen()) {
                        return BulkItemResult.failed(BulkResultCode.STATE_CONFLICT);
                    }
                    job = id;
                }
            }
        } catch (ApiException e) {
            return BulkItemResult.failed(BulkActionService.codeOf(e.code()));
        }
        var p = c.params();
        nextActions.create(new NextActionCreateRequest(p.title(), p.dueOn(), p.ownerId(), account, person, job));
        return BulkItemResult.ok();
    }
}
