package com.recruiting.platform.contracts;

/** Company size by headcount (since contract 1.3.0). */
public enum EmployeeBand {
    /** 1 to 50 employees. */
    B1_50,
    /** 51 to 250. */
    B51_250,
    /** 251 to 1,000. */
    B251_1000,
    /** 1,001 to 5,000. */
    B1001_5000,
    /** More than 5,000. */
    B5000_PLUS
}
