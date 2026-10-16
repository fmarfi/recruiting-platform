package com.recruiting.platform.contracts;

/**
 * Badge tone of a status. The UI maps each tone to a colour token in
 * {@code contracts/design-tokens.json}; the backend never deals with colours.
 */
public enum Tone {
    /** Starting or idle state. */
    NEUTRAL,
    /** Waiting for someone else. */
    INFO,
    /** Work moving forward. */
    PROGRESS,
    /** Successful outcome. */
    GOOD,
    /** Needs attention. */
    WARN,
    /** Negative outcome. */
    BAD,
    /** Closed or archived. */
    MUTED
}
