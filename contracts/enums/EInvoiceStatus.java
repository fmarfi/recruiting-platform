package com.recruiting.platform.contracts;

/** Whether the client receives Turkish e-invoices (CO-019; since contract 1.3.0). */
public enum EInvoiceStatus {
    /** Not checked yet. */
    UNKNOWN,
    /** Registered e-Fatura user (needs the alias). */
    E_FATURA,
    /** Not registered; receives e-Arşiv invoices. */
    E_ARSIV
}
