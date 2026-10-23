package com.recruiting.platform.contracts;

/**
 * The data grids that have saved views and bulk actions (ADR 0010). Each value maps to one
 * list endpoint: PEOPLE {@code GET /people}, ACCOUNTS {@code GET /accounts}, JOB_ORDERS
 * {@code GET /job-orders}, PROPOSALS {@code GET /proposals}.
 */
public enum ListEntity {
    /** The network of people. */
    PEOPLE,
    /** Client accounts. */
    ACCOUNTS,
    /** Job orders. */
    JOB_ORDERS,
    /** Proposals. */
    PROPOSALS
}
