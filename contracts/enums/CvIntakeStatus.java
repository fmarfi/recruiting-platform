package com.recruiting.platform.contracts;

/**
 * State of one uploaded CV in the intake queue, before it belongs to a person (bulk upload,
 * since contract 1.2.0, ADR 0011). Pending items are deleted with their file after 14 days.
 */
public enum CvIntakeStatus {
    /** Uploaded, waiting for a consultant to attach it to an existing or a new person. */
    PENDING,
    /** Attached to a person; the file is now a person file. */
    ATTACHED,
    /** Thrown away by a consultant, or expired; the file is deleted. */
    DISCARDED
}
