package com.recruiting.platform.accounts.person.web.dto;

import com.recruiting.platform.accounts.domain.Compensation;
import com.recruiting.platform.accounts.offlimits.web.dto.OffLimitsRuleResponse;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.Mobility;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.RetentionStatus;
import com.recruiting.platform.contracts.SourceChannel;
import com.recruiting.platform.security.UserRef;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Person as in the Person schema. {@code compensation} is null with {@code compensationRestricted = true} for users
 * who are neither MANAGER/ADMIN nor the owner (ADR 0011).
 */
public record PersonResponse(UUID id, String name, String title, String company, PersonType type, String city,
        String email, UserRef owner, Instant lastContactAt, ConsentStatus consentStatus,
        RetentionStatus retentionStatus, List<String> tags, SourceChannel sourceChannel, boolean shareable,
        String cvMatch, String phone, String linkedinUrl, String howWeKnow, String notes,
        List<Employment> employments, boolean anonymised, Instant createdAt, Instant updatedAt, long version,
        Integer noticePeriodDays, Mobility mobility, Compensation compensation, boolean compensationRestricted,
        List<LanguageSkillDto> languages, List<EducationDto> educations, PersonFileResponse currentCv,
        List<OffLimitsRuleResponse> offLimits) {
}
