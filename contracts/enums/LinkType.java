package com.recruiting.platform.contracts;

/**
 * Relationship between two people in the network (A-13). A link is stored once with a
 * direction: {@code from} introduced, referred or reports to {@code to}. For the
 * symmetric types the direction carries no meaning.
 */
public enum LinkType {
    /** The two people know each other (symmetric). */
    KNOWS(true),
    /** The two people worked together (symmetric). */
    WORKED_WITH(true),
    /** {@code from} introduced {@code to} to the firm. */
    INTRODUCED(false),
    /** {@code from} referred {@code to} as a candidate. */
    REFERRED(false),
    /** {@code from} reports or reported to {@code to}. */
    REPORTS_TO(false);

    private final boolean symmetric;

    LinkType(boolean symmetric) {
        this.symmetric = symmetric;
    }

    /**
     * Tells whether the link reads the same in both directions.
     *
     * @return {@code true} for {@link #KNOWS} and {@link #WORKED_WITH}
     */
    public boolean isSymmetric() {
        return symmetric;
    }
}
