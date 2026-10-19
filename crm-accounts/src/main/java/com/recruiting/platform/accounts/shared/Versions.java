package com.recruiting.platform.accounts.shared;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;

/** Optimistic-lock check for the optional {@code version} of PATCH bodies (ADR 0006). */
public final class Versions {

    private Versions() {
    }

    public static void check(Long sent, Long actual) {
        if (sent != null && actual != null && !sent.equals(actual)) {
            throw new ApiException(ErrorCode.VERSION_CONFLICT,
                    "The record was changed by someone else (version " + actual + ").");
        }
    }
}
