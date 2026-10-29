package com.recruiting.platform.contracts;

/** Invoice state of a fee schedule item (CO-020, since contract 1.3.0). The platform does not issue invoices; finance records them. */
public enum InvoiceState {
    /** The trigger has not happened (default). */
    NOT_DUE,
    /** The trigger happened; listed on Invoice ready. */
    DUE,
    /** Invoiced; e-invoice reference recorded. */
    INVOICED,
    /** Paid. */
    PAID,
    /** Will not be invoiced. */
    CANCELLED
}
