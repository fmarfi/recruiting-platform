package com.recruiting.platform.contracts;

import java.util.List;
import java.util.Map;

/**
 * Thrown when a requested status move is not in the lifecycle's transition table.
 * Mapped to 409 with {@code currentStatus}, {@code requestedStatus} and
 * {@code allowed} in the problem body, so the UI can explain the refusal.
 */
public class IllegalTransitionException extends ApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception for a refused move.
     *
     * @param from the current status
     * @param to   the requested status
     * @param <S>  the lifecycle enum type
     */
    public <S extends Enum<S> & Lifecycle<S>> IllegalTransitionException(S from, S to) {
        super(ErrorCode.ILLEGAL_TRANSITION,
                "Cannot move from " + from.labelEn() + " to " + to.labelEn() + ".",
                Map.of("currentStatus", from.name(),
                        "requestedStatus", to.name(),
                        "allowed", from.next().stream().map(Enum::name).sorted().toList()));
    }

    /**
     * Checks a move against the transition table and the reason rule.
     *
     * @param from   the current status
     * @param to     the requested status
     * @param reason the reason given by the user, may be {@code null}
     * @param <S>    the lifecycle enum type
     * @throws IllegalTransitionException if the move is not allowed
     * @throws ApiException               with {@link ErrorCode#REASON_REQUIRED} if a reason is missing
     */
    public static <S extends Enum<S> & Lifecycle<S>> void check(S from, S to, String reason) {
        if (!from.canMoveTo(to)) {
            throw new IllegalTransitionException(from, to);
        }
        if (from.requiresReason(to) && (reason == null || reason.isBlank())) {
            throw new ApiException(ErrorCode.REASON_REQUIRED,
                    "A reason is required to move to " + to.labelEn() + ".",
                    Map.of("requestedStatus", to.name(), "errors",
                            List.of(Map.of("field", "reason", "code", "REQUIRED",
                                    "message", "A reason is required."))));
        }
    }
}
