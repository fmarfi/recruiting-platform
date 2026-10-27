package com.recruiting.platform.pipeline.bulk.app;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.BulkActionType;
import com.recruiting.platform.contracts.BulkOutcome;
import com.recruiting.platform.contracts.BulkResultCode;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.ListEntity;
import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.pipeline.BulkActionHandler;
import com.recruiting.platform.pipeline.BulkCommand;
import com.recruiting.platform.pipeline.BulkItemResult;
import com.recruiting.platform.pipeline.BulkParams;
import com.recruiting.platform.pipeline.bulk.web.dto.BulkActionResponse;
import com.recruiting.platform.pipeline.bulk.web.dto.BulkRecordResponse;
import com.recruiting.platform.pipeline.domain.BulkAction;
import com.recruiting.platform.pipeline.domain.BulkActionItem;
import com.recruiting.platform.pipeline.domain.BulkActionItemRepository;
import com.recruiting.platform.pipeline.domain.BulkActionRepository;
import com.recruiting.platform.pipeline.shared.Refs;
import com.recruiting.platform.pipeline.shared.Transitions;
import com.recruiting.platform.security.CurrentUser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

/**
 * Dispatcher of {@code POST /bulk-actions} (ADR 0010). Not transactional itself: every record runs in its own
 * transaction (rolled back when the record fails), and its audit row is written in a separate transaction, so the
 * evidence survives a rollback. The audit holds ids, outcomes and codes only (amendment 1).
 */
@Service
public class BulkActionService {

    private static final Logger LOG = LoggerFactory.getLogger(BulkActionService.class);

    private final List<BulkActionHandler> handlers;
    private final BulkActionRepository actions;
    private final BulkActionItemRepository items;
    private final Refs refs;
    private final CurrentUser currentUser;
    private final TransactionTemplate perRecord;
    private final TransactionTemplate audit;

    BulkActionService(List<BulkActionHandler> handlers, BulkActionRepository actions, BulkActionItemRepository items,
            Refs refs, CurrentUser currentUser, PlatformTransactionManager tm) {
        this.handlers = handlers;
        this.actions = actions;
        this.items = items;
        this.refs = refs;
        this.currentUser = currentUser;
        this.perRecord = new TransactionTemplate(tm);
        this.perRecord.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.audit = new TransactionTemplate(tm);
        this.audit.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Runs the action on every id and returns one result per id, in request order.
     *
     * @param body the request body
     * @return the result with counts
     */
    public BulkActionResponse run(JsonNode body) {
        var parsed = BulkRequestParser.parse(body);
        Role role = currentUser.role();
        if (parsed.action() == BulkActionType.REQUEST_CONSENT && !currentUser.hasAtLeast(Role.MANAGER)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Only a manager or admin may request consent in bulk.");
        }
        checkUsers(parsed.params());
        BulkActionHandler handler = handlers.stream().filter(h -> h.supports(parsed.entity(), parsed.action()))
                .findFirst().orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No handler for " + parsed.action() + " on " + parsed.entity() + "."));
        UUID actor = currentUser.id();
        BulkCommand command = new BulkCommand(parsed.entity(), parsed.action(), parsed.params(), actor, role);
        BulkAction header = audit.execute(s -> actions.save(newHeader(parsed.entity(), parsed.action(),
                parsed.params(), actor, parsed.ids().size())));
        List<BulkRecordResponse> results = new ArrayList<>();
        int ok = 0;
        int skipped = 0;
        int failed = 0;
        for (UUID id : parsed.ids()) {
            BulkItemResult r = process(handler, command, id);
            audit.executeWithoutResult(s -> items.save(new BulkActionItem(header.getId(), id, r.outcome(), r.code())));
            results.add(new BulkRecordResponse(id, r.outcome(), r.code(), r.code() == null ? null : r.code().message()));
            switch (r.outcome()) {
                case OK -> ok++;
                case SKIPPED -> skipped++;
                case FAILED -> failed++;
            }
        }
        int o = ok;
        int s2 = skipped;
        int f = failed;
        audit.executeWithoutResult(s -> {
            BulkAction a = actions.findById(header.getId()).orElseThrow();
            a.setFinishedAt(Instant.now());
            a.setOkCount(o);
            a.setSkippedCount(s2);
            a.setFailedCount(f);
            actions.save(a);
        });
        return new BulkActionResponse(header.getId(), parsed.entity(), parsed.action(), parsed.ids().size(), ok,
                skipped, failed, results);
    }

    private BulkItemResult process(BulkActionHandler handler, BulkCommand command, UUID id) {
        try {
            BulkItemResult r = perRecord.execute(s -> {
                BulkItemResult result = handler.handle(command, id);
                if (result.outcome() == BulkOutcome.FAILED) {
                    s.setRollbackOnly();
                }
                return result;
            });
            return r == null ? BulkItemResult.failed(BulkResultCode.INTERNAL_ERROR) : r;
        } catch (ApiException e) {
            return BulkItemResult.failed(codeOf(e.code()));
        } catch (RuntimeException e) {
            LOG.warn("Bulk {} on {} failed for one record: {}", command.action(), command.entity(),
                    e.getClass().getSimpleName());
            return BulkItemResult.failed(BulkResultCode.INTERNAL_ERROR);
        }
    }

    /** Maps a lifecycle or guard error of the single endpoints to the bulk code (ADR 0010). */
    static BulkResultCode codeOf(ErrorCode code) {
        return switch (code) {
            case NOT_FOUND -> BulkResultCode.NOT_FOUND;
            case FORBIDDEN -> BulkResultCode.FORBIDDEN;
            case ILLEGAL_TRANSITION -> BulkResultCode.ILLEGAL_TRANSITION;
            case TRANSITION_GUARD_FAILED -> BulkResultCode.TRANSITION_GUARD_FAILED;
            case REASON_REQUIRED, VALIDATION_FAILED -> BulkResultCode.REASON_REQUIRED;
            case STATE_CONFLICT -> BulkResultCode.STATE_CONFLICT;
            case VERSION_CONFLICT -> BulkResultCode.VERSION_CONFLICT;
            default -> BulkResultCode.INTERNAL_ERROR;
        };
    }

    private void checkUsers(BulkParams p) {
        if (p.ownerId() != null) {
            try {
                refs.requireUser(p.ownerId());
            } catch (ApiException e) {
                throw Transitions.invalid("params.ownerId", "ownerId is not a known user.");
            }
        }
    }

    /** The audit params: ids, enum names, dates, the tag and the language only; never title or reason. */
    static Map<String, Object> auditParams(BulkParams p) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (p.ownerId() != null) {
            m.put("ownerId", p.ownerId().toString());
        }
        if (p.status() != null) {
            m.put("status", p.status());
        }
        if (p.tag() != null) {
            m.put("tag", p.tag());
        }
        if (p.dueOn() != null) {
            m.put("dueOn", p.dueOn().toString());
        }
        if (p.language() != null) {
            m.put("language", p.language().name());
        }
        return m;
    }

    private static BulkAction newHeader(ListEntity entity, BulkActionType action, BulkParams params, UUID actor,
            int requested) {
        BulkAction a = new BulkAction();
        a.setEntity(entity);
        a.setAction(action);
        a.setParams(auditParams(params));
        a.setActorId(actor);
        a.setStartedAt(Instant.now());
        // finished_at is NOT NULL: the header is inserted first (the item rows reference it) and updated at the end
        a.setFinishedAt(a.getStartedAt());
        a.setRequestedCount(requested);
        return a;
    }
}
