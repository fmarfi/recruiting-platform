package com.recruiting.platform.accounts.person.web.dto;

import com.recruiting.platform.contracts.LanguageLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** The LanguageSkill schema (1.2.0): ISO 639-1 code and level. */
public record LanguageSkillDto(@NotNull @Pattern(regexp = "^[a-z]{2}$") String language, @NotNull LanguageLevel level) {
}
