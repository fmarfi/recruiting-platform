package com.recruiting.platform.contracts;

/**
 * Progress of the plain-text extraction of an uploaded file with Apache Tika (no AI, no OCR;
 * since contract 1.2.0, ADR 0011). Runs asynchronously after the upload.
 */
public enum TextExtractionStatus {
    /** Queued or running. */
    PENDING,
    /** Text extracted; the file is in the full-text search. */
    DONE,
    /** The file contains no text layer (e.g. a scanned image); there is no OCR in Phase 1. */
    NO_TEXT,
    /** Tika could not read the file (corrupt, encrypted, timeout). */
    FAILED
}
