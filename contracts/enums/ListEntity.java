package com.recruiting.platform.contracts;

/**
 * The data grids that have saved views and bulk actions (ADR 0010). Each value maps to one
 * list endpoint: PEOPLE {@code GET /people}, ACCOUNTS {@code GET /accounts}, JOB_ORDERS
 * {@code GET /job-orders}, PROPOSALS {@code GET /proposals}; since 1.5.0 APPLICATIONS
 * {@code GET /job-orders/{id}/applications} (bulk only).
 */
public enum ListEntity {
    /** The network of people. */
    PEOPLE,
    /** Client accounts. */
    ACCOUNTS,
    /** Job orders. */
    JOB_ORDERS,
    /** Proposals. */
    PROPOSALS,
    /**
     * Applications (candidates on job orders; the longlist table of a search), since 1.5.0
     * (ADR 0015). Ids are application ids. Bulk only in 1.5: {@code CHANGE_STATUS} with a
     * {@link PipelineStage} as status. Saved views and exports do not accept it yet (400).
     */
    APPLICATIONS
}
