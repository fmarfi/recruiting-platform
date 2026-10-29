package com.recruiting.platform.contracts;

/** Kind of fee agreement (PB-007, since contract 1.3.0, ADR 0013). */
public enum AgreementKind {
    /** Framework terms of business of an account (no job order). */
    TERMS_OF_BUSINESS,
    /** Engagement for one search (job order); its schedule items are invoiced. */
    ENGAGEMENT_LETTER,
    /** A change to a signed agreement. */
    AMENDMENT
}
