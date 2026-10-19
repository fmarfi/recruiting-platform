package com.recruiting.platform.privacy.notice.web.dto;

import com.recruiting.platform.contracts.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Contract schema PrivacyNoticeCreateRequest. */
public record NoticeCreateRequest(@NotBlank @Size(max = 40) String version, @NotNull Language language,
        @NotBlank @Size(max = 50000) String noticeText, @NotBlank @Size(max = 10000) String consentText,
        @NotNull LocalDate effectiveFrom) {
}
