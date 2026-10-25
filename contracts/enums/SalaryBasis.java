package com.recruiting.platform.contracts;

/** Whether a salary amount is before or after tax (since contract 1.2.0). */
public enum SalaryBasis {
    /** Before tax and social security (brüt). */
    GROSS,
    /** After tax and social security (net). */
    NET
}
