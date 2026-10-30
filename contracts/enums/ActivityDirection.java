package com.recruiting.platform.contracts;

/** Who started the contact (R70, since contract 1.3.0). Null for notes. */
public enum ActivityDirection {
    /** The person contacted us. */
    INBOUND,
    /** We contacted the person. */
    OUTBOUND
}
