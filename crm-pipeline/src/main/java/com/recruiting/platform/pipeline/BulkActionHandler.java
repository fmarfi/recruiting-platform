package com.recruiting.platform.pipeline;

import com.recruiting.platform.contracts.BulkActionType;
import com.recruiting.platform.contracts.ListEntity;
import java.util.UUID;

/**
 * SPI for {@code POST /bulk-actions} (ADR 0010, same pattern as accounts' {@code SearchContributor}). The dispatcher
 * in pipeline calls the first handler that {@link #supports supports} the (entity, action) pair, once per record,
 * inside that record's own transaction. accounts implements PEOPLE and ACCOUNTS, privacy implements REQUEST_CONSENT.
 *
 * <p>Rules for implementations: change records only through the module's normal services (lifecycle checks, events,
 * Envers), never around them. Return {@link BulkItemResult#skipped} or {@link BulkItemResult#failed} before writing
 * anything; a thrown {@link com.recruiting.platform.contracts.ApiException} is mapped to the matching
 * {@link com.recruiting.platform.contracts.BulkResultCode} by the dispatcher and the record's transaction is rolled
 * back. Never put names, e-mail addresses or exception messages in a result: only the code is used.
 */
public interface BulkActionHandler {

    /**
     * Tells whether this handler runs the action on the entity.
     *
     * @param entity the grid entity
     * @param action the action
     * @return {@code true} if this handler is responsible
     */
    boolean supports(ListEntity entity, BulkActionType action);

    /**
     * Applies the action to one record.
     *
     * @param command the action and its validated params
     * @param id      the record id
     * @return the outcome of this record
     */
    BulkItemResult handle(BulkCommand command, UUID id);
}
