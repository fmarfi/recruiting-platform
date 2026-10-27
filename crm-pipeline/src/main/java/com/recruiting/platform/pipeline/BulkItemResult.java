package com.recruiting.platform.pipeline;

import com.recruiting.platform.contracts.BulkOutcome;
import com.recruiting.platform.contracts.BulkResultCode;

/**
 * Outcome of one record of a bulk action (ADR 0010). OK carries no code; SKIPPED and FAILED always do. The text shown
 * to the client is {@link BulkResultCode#message()}, never anything else.
 *
 * @param outcome OK, SKIPPED or FAILED
 * @param code    reason, {@code null} when OK
 */
public record BulkItemResult(BulkOutcome outcome, BulkResultCode code) {

    /** @return a successful result */
    public static BulkItemResult ok() {
        return new BulkItemResult(BulkOutcome.OK, null);
    }

    /**
     * @param code why nothing was done on purpose
     * @return a SKIPPED result
     */
    public static BulkItemResult skipped(BulkResultCode code) {
        return new BulkItemResult(BulkOutcome.SKIPPED, code);
    }

    /**
     * @param code why the record could not be changed
     * @return a FAILED result
     */
    public static BulkItemResult failed(BulkResultCode code) {
        return new BulkItemResult(BulkOutcome.FAILED, code);
    }
}
