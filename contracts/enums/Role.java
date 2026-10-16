package com.recruiting.platform.contracts;

/**
 * User roles (NFR-05). Each user has exactly one role; a higher role includes the
 * rights of the lower ones (ADMIN &gt; MANAGER &gt; CONSULTANT).
 */
public enum Role {
    /** Consultant or recruiter: works accounts, people, job orders and pipelines. */
    CONSULTANT,
    /** Account manager or agency management: plus closing accounts, imports, exports, data-subject requests. */
    MANAGER,
    /** Administrator: plus users, privacy notices, retention settings and the audit log. */
    ADMIN;

    /**
     * Tells whether this role has at least the rights of {@code other}.
     *
     * @param other the role to compare with
     * @return {@code true} if this role includes {@code other}
     */
    public boolean includes(Role other) {
        return ordinal() >= other.ordinal();
    }
}
