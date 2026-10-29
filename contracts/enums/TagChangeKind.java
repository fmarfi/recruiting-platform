package com.recruiting.platform.contracts;

/** What happened to a tag across all records (event {@link TagChanged}, since contract 1.3.0). */
public enum TagChangeKind {
    /** The tag got a new name on every record. */
    RENAMED,
    /** The tag was replaced by another existing tag on every record. */
    MERGED,
    /** The tag was removed from every record. */
    DELETED
}
