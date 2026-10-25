package com.recruiting.platform.contracts;

/**
 * Mass actions on selected grid records ({@code POST /bulk-actions}, ADR 0010). Every action
 * runs through the normal services, per record.
 */
public enum BulkActionType {
    /** Set {@code owner_id}; all entities. */
    REASSIGN_OWNER,
    /** Move through the entity's lifecycle (never around it); ACCOUNTS, JOB_ORDERS, PROPOSALS. */
    CHANGE_STATUS,
    /** Add a tag; all entities. */
    ADD_TAG,
    /** Remove a tag; all entities. */
    REMOVE_TAG,
    /** Create one next action linked to each record; PEOPLE, ACCOUNTS, JOB_ORDERS. */
    SET_NEXT_ACTION,
    /** Send the KVKK consent request e-mail; PEOPLE only, consent-gated, manager/admin only. */
    REQUEST_CONSENT
}
