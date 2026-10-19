package com.recruiting.platform.pipeline.shared;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.IllegalTransitionException;
import com.recruiting.platform.contracts.Lifecycle;
import java.util.List;
import java.util.Map;

/** Shared status-move checks: transition table, reason rule, manager-only rule, guards, error builders. */
public final class Transitions {

    private Transitions() {
    }

    /**
     * Checks a move and returns the trimmed reason.
     *
     * @param from     current status
     * @param to       requested status
     * @param reason   reason given, may be {@code null}
     * @param elevated whether the caller is MANAGER or ADMIN
     * @param <S>      lifecycle enum
     * @return the trimmed reason, {@code null} when blank
     * @throws IllegalTransitionException 409 when the move is not in the table (also for a move to the same status)
     * @throws ApiException               400 REASON_REQUIRED or VALIDATION_FAILED, or 403 FORBIDDEN for a manager-only move
     */
    public static <S extends Enum<S> & Lifecycle<S>> String check(S from, S to, String reason, boolean elevated) {
        IllegalTransitionException.check(from, to, reason);
        if (from.requiresElevatedRole(to) && !elevated) {
            throw new ApiException(ErrorCode.FORBIDDEN,
                    "Only a manager or admin may move from " + from.labelEn() + " to " + to.labelEn() + ".");
        }
        String trimmed = reason == null ? null : reason.trim();
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Builds the 409 for a data guard.
     *
     * @param from   current status
     * @param to     requested status
     * @param detail what blocks the move
     * @return the exception to throw
     */
    public static ApiException guardFailed(Enum<?> from, Enum<?> to, String detail) {
        return new ApiException(ErrorCode.TRANSITION_GUARD_FAILED, detail,
                Map.of("currentStatus", from.name(), "requestedStatus", to.name()));
    }

    /**
     * Builds a 400 validation error for one field.
     *
     * @param field   field name
     * @param message message
     * @return the exception to throw
     */
    public static ApiException invalid(String field, String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, message,
                Map.of("errors", List.of(Map.of("field", field, "code", "INVALID", "message", message))));
    }

    /**
     * Builds a 404.
     *
     * @param what the missing record kind
     * @return the exception to throw
     */
    public static ApiException notFound(String what) {
        return new ApiException(ErrorCode.NOT_FOUND, what + " not found.");
    }

    /**
     * Checks the optimistic-lock version sent by the client.
     *
     * @param sent    version from the request, {@code null} skips the check
     * @param current version of the stored record
     */
    public static void checkVersion(Long sent, Long current) {
        if (sent != null && !sent.equals(current)) {
            throw new ApiException(ErrorCode.VERSION_CONFLICT,
                    "The record was changed by someone else. Reload and retry.");
        }
    }
}
