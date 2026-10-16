package com.recruiting.platform.contracts;

import java.util.Set;

/**
 * A status that belongs to a closed lifecycle with an explicit transition table
 * (ADR 0002). Implemented by every status enum: account, proposal, job order,
 * pipeline stage, consent and data-subject request.
 *
 * <p>The enum is the single source of truth for which moves exist, which moves need a
 * written reason and which moves are limited to managers and admins. Guards that need
 * data (for example "an account cannot close while it has open job orders") are not
 * part of the enum; the owning service checks them and answers 409 with
 * {@link ErrorCode#TRANSITION_GUARD_FAILED}.
 *
 * @param <S> the enum type implementing this lifecycle
 */
public interface Lifecycle<S extends Enum<S> & Lifecycle<S>> {

    /**
     * Returns the statuses this status may move to.
     *
     * @return the allowed next statuses; empty for a final status
     */
    Set<S> next();

    /**
     * Returns the English badge label.
     *
     * @return the English label
     */
    String labelEn();

    /**
     * Returns the Turkish badge label.
     *
     * @return the Turkish label
     */
    String labelTr();

    /**
     * Returns the badge tone.
     *
     * @return the tone the UI maps to a colour token
     */
    Tone tone();

    /**
     * Tells whether a move from this status to {@code target} needs a written reason.
     *
     * @param target the requested status
     * @return {@code true} if a non-blank reason is required
     */
    default boolean requiresReason(S target) {
        return false;
    }

    /**
     * Tells whether a move from this status to {@code target} is limited to the
     * {@link Role#MANAGER} and {@link Role#ADMIN} roles.
     *
     * @param target the requested status
     * @return {@code true} if consultants may not make this move
     */
    default boolean requiresElevatedRole(S target) {
        return false;
    }

    /**
     * Tells whether a move from this status to {@code target} is part of the lifecycle.
     *
     * @param target the requested status
     * @return {@code true} if the transition is allowed
     */
    default boolean canMoveTo(S target) {
        return next().contains(target);
    }

    /**
     * Tells whether this status is final.
     *
     * @return {@code true} if no further move is possible
     */
    default boolean isTerminal() {
        return next().isEmpty();
    }
}
