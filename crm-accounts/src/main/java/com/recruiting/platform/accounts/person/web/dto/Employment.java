package com.recruiting.platform.accounts.person.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** One entry of a person company history. */
public record Employment(@NotBlank @Size(max = 200) String company, @Size(max = 200) String title,
        LocalDate startDate, LocalDate endDate) {
}
