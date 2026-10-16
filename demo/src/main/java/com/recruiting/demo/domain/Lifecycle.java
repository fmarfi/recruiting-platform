package com.recruiting.demo.domain;

import java.util.Set;

/**
 * A status that belongs to a closed lifecycle with explicit allowed transitions.
 *
 * <p>Implemented by every status enum in the CRM (account, proposal, job order and
 * pipeline stage) so that the UI can show the allowed next statuses and the service
 * can reject illegal moves (requirements A-02, A-05, A-06, A-07).
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
     * Returns the English label shown on the status badge.
     *
     * @return the English label
     */
    String labelEn();

    /**
     * Returns the Turkish label shown on the status badge.
     *
     * @return the Turkish label
     */
    String labelTr();

    /**
     * Returns the badge tone the UI maps to a colour: {@code neutral}, {@code info},
     * {@code progress}, {@code good}, {@code warn} or {@code bad}.
     *
     * @return the badge tone
     */
    String tone();

    /**
     * Tells whether a move from this status to {@code target} is allowed.
     *
     * @param target the requested status
     * @return {@code true} if the transition is part of the lifecycle
     */
    default boolean canMoveTo(S target) {
        return next().contains(target);
    }
}
