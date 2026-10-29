package com.recruiting.platform.contracts;

/**
 * Roles of an organisation (R63, since contract 1.3.0, ADR 0013). An organisation is an
 * {@code account} row; one organisation may have several roles. Stored as four booleans on
 * {@code account} ({@code is_client}, {@code is_prospect}, {@code is_target}, {@code is_employer}),
 * exposed as {@code roles} in the API. {@code AccountStatus} stays the client lifecycle and is only
 * meaningful for CLIENT and PROSPECT.
 */
public enum OrganisationRole {
    /** A client we work for (set automatically when the status becomes ACTIVE). */
    CLIENT,
    /** A potential client (default for {@code POST /accounts} without roles). */
    PROSPECT,
    /** A company we search in (set automatically when it becomes a target company of a search). */
    TARGET,
    /** A current or former employer of a person in the network (set automatically on an employment link). */
    EMPLOYER
}
