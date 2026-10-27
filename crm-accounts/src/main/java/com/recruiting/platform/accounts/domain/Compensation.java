package com.recruiting.platform.accounts.domain;

import com.recruiting.platform.contracts.CompensationPeriod;
import com.recruiting.platform.contracts.SalaryBasis;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The Compensation schema (contract 1.2.0). Stored as one JSON value encrypted by {@link
 * CompensationEncryptionConverter}; never in lists, search or events.
 */
public record Compensation(@DecimalMin("0") BigDecimal currentBase, @DecimalMin("0") BigDecimal currentVariable,
        @DecimalMin("0") BigDecimal expected, @NotNull @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @NotNull CompensationPeriod period, @NotNull SalaryBasis basis, @Size(max = 500) String benefits,
        LocalDate updatedOn) {
}
