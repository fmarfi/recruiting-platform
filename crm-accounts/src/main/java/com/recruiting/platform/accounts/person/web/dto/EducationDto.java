package com.recruiting.platform.accounts.person.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The Education schema (1.2.0). */
public record EducationDto(@NotBlank @Size(max = 200) String school, @Size(max = 100) String degree,
        @Size(max = 200) String field, @Min(1940) @Max(2100) Integer startYear, @Min(1940) @Max(2100) Integer endYear) {
}
