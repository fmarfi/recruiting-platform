package com.recruiting.platform.contracts;

/** Display colour of a tag (since contract 1.3.0, ADR 0013); the frontend maps it to a token. */
public enum TagColour {
    /** Default for tags without a definition. */
    GREY,
    /** Blue. */
    BLUE,
    /** Green. */
    GREEN,
    /** Amber. */
    AMBER,
    /** Red. */
    RED,
    /** Purple. */
    PURPLE,
    /** Teal. */
    TEAL
}
