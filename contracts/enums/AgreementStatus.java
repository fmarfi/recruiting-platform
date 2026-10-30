package com.recruiting.platform.contracts;

/** State of a fee agreement (since contract 1.3.0). A plain field, not a lifecycle with a transition table. */
public enum AgreementStatus {
    /** Being prepared (default; may be deleted). */
    DRAFT,
    /** Sent to the client. */
    SENT,
    /** Signed; needs the signing date and makes SIGNING items due. */
    SIGNED,
    /** Past its validity. */
    EXPIRED,
    /** Ended early. */
    TERMINATED
}
