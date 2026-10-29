package com.recruiting.platform.contracts;

/** Functional area of a person or a search (SX-025 pool filter; since contract 1.3.0). */
public enum JobFunction {
    /** CEO, general manager, country manager. */
    GENERAL_MANAGEMENT,
    /** Finance, accounting, treasury, audit. */
    FINANCE,
    /** Human resources. */
    HR,
    /** Sales and business development. */
    SALES,
    /** Marketing and communications. */
    MARKETING,
    /** Operations and production. */
    OPERATIONS,
    /** Supply chain and logistics. */
    SUPPLY_CHAIN,
    /** Procurement and purchasing. */
    PROCUREMENT,
    /** Engineering. */
    ENGINEERING,
    /** IT and digital. */
    IT,
    /** Research and development. */
    RND,
    /** Legal and compliance. */
    LEGAL,
    /** Anything else. */
    OTHER
}
