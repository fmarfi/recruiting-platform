package com.recruiting.platform.contracts;

/**
 * The one action of a Home NEEDS YOU item (since contract 1.4.0, ADR 0014). Each code maps to an
 * existing endpoint; Home adds no write endpoint.
 */
public enum HomeAction {
    /** {@code POST /people/{personId}/consent/requests} (consent NOT_SENT or SENT). */
    REQUEST_CONSENT,
    /** {@code PATCH /applications/{applicationId}/stage} to REJECTED with a reason (consent REFUSED or WITHDRAWN). */
    REMOVE_FROM_SHORTLIST,
    /** {@code POST /activities} with the search's account and job order (the "Nudge"); recording feedback uses {@code POST /applications/{applicationId}/client-feedback}. */
    LOG_CLIENT_CONTACT,
    /** {@code PATCH /applications/{applicationId}/stage}: advance or reject. */
    MOVE_STAGE,
    /** {@code POST /next-actions} with the job order and its account. */
    PLAN_NEXT_ACTION,
    /** {@code PATCH /next-actions/{nextActionId}} with {@code done: true} (snooze sends {@code dueOn}). */
    COMPLETE_ACTION
}
