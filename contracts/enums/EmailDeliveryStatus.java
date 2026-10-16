package com.recruiting.platform.contracts;

/** Delivery status of a KVKK notice and consent e-mail (S-01). */
public enum EmailDeliveryStatus {
    /** Waiting to be sent by the mail job. */
    QUEUED,
    /** Accepted by the mail server. */
    SENT,
    /** Sending failed; see the failure reason. Can be resent. */
    FAILED
}
