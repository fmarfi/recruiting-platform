package com.recruiting.platform.contracts;

/**
 * A block of an evaluation that may go into a client report (SX-008 field 21, since 1.3.0).
 * Private notes are never a block: they never reach a report.
 */
public enum EvaluationBlock {
    /** Summary (3-5 lines). */
    SUMMARY,
    /** Key achievements. */
    ACHIEVEMENTS,
    /** Motivation to move. */
    MOTIVATION,
    /** Concerns and gaps. */
    CONCERNS,
    /** Criterion scores against the brief. */
    CRITERIA,
    /** Reference status. */
    REFERENCES
}
