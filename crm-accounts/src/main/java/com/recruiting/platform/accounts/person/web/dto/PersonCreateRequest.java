package com.recruiting.platform.accounts.person.web.dto;

import com.recruiting.platform.contracts.PersonType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.hibernate.validator.constraints.URL;

/** Body of POST /people (also nested in contact creation). */
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
        @Valid List<Employment> employments) {
}
