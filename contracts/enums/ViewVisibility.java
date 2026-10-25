package com.recruiting.platform.contracts;

/** Who sees a saved list view (ADR 0010). */
public enum ViewVisibility {
    /** Only the owner sees and changes it. */
    PRIVATE,
    /** Every user sees it; only the owner, managers and admins change it. Creating one is manager/admin only (assumption). */
    SHARED
}
