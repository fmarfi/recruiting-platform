package com.recruiting.platform.accounts.person.web.dto;

import com.recruiting.platform.accounts.domain.Compensation;
import com.recruiting.platform.contracts.Mobility;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.SourceChannel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.hibernate.validator.constraints.URL;

/** Body of POST /people (also nested in contact creation and CV intake attach). Since 1.2.0 with profile fields. */
public record PersonCreateRequest(@NotBlank @Size(max = 200) String name,
        @NotNull PersonType type,
        @Size(max = 200) String title,
        @Size(max = 200) String company,
        @Email @Size(max = 254) String email,
        @Size(max = 50) String phone,
        @Size(max = 100) String city,
        @URL @Size(max = 300) String linkedinUrl,
        @Size(max = 1000) String howWeKnow,
        @Size(max = 4000) String notes,
        UUID ownerId,
        @Valid List<Employment> employments,
        SourceChannel sourceChannel,
        @Min(0) @Max(365) Integer noticePeriodDays,
        Mobility mobility,
        @Valid Compensation compensation,
        @Valid @Size(max = 20) List<LanguageSkillDto> languages,
        @Valid @Size(max = 20) List<EducationDto> educations) {

    /** The 1.1 shape (no profile fields), used by import and older callers. */
    public PersonCreateRequest(String name, PersonType type, String title, String company, String email, String phone,
            String city, String linkedinUrl, String howWeKnow, String notes, UUID ownerId,
            List<Employment> employments) {
        this(name, type, title, company, email, phone, city, linkedinUrl, howWeKnow, notes, ownerId, employments,
                null, null, null, null, null, null);
    }
}
