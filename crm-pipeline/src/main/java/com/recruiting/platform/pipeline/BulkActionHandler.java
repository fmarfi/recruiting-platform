package com.recruiting.platform.pipeline;

import com.recruiting.platform.contracts.BulkActionType;
import com.recruiting.platform.contracts.BulkOutcome;
import com.recruiting.platform.contracts.BulkResultCode;
import com.recruiting.platform.contracts.Role;
import java.util.Map;
import java.util.UUID;

/**
 * SPI of the bulk action dispatcher (ADR 0010, {@code POST /bulk-actions}). The dispatcher in pipeline calls the
 * handler registered for the action once per id, each call in its own transaction, and writes the audit row from the
 * result. {@code privacy} implements {@link BulkActionType#REQUEST_CONSENT}.
 *
 * <p>NOTE: minimal copy written by security-privacy so that privacy compiles; backend-pipeline's definition wins at
 * merge if it differs, and {@code RequestConsentBulkHandler} is adapted.
 */
public interface BulkActionHandler {

    /** The action this handler serves. */
    BulkActionType action();

    /**
     * Processes one record. Must not throw for expected refusals: return SKIPPED or FAILED with a code.
     *
     * @param actorId the user who started the bulk action
     * @param role    the user's role
     * @param params  the typed params of the request as a map (no free text is audited, ADR 0010 amendment 1)
     * @param id      the record id
     */
    Result handle(UUID actorId, Role role, Map<String, Object> params, UUID id);

    /**
     * Result for one record; {@code code} is null when OK.
     *
     * @param outcome OK, SKIPPED or FAILED
     * @param code    reason, null when OK
     */
    record Result(BulkOutcome outcome, BulkResultCode code) {

        public static Result ok() {
            return new Result(BulkOutcome.OK, null);
        }

        public static Result skipped(BulkResultCode code) {
            return new Result(BulkOutcome.SKIPPED, code);
        }

        public static Result failed(BulkResultCode code) {
            return new Result(BulkOutcome.FAILED, code);
        }
    }
}
