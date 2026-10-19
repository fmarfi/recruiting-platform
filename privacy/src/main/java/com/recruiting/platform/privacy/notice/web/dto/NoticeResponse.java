package com.recruiting.platform.privacy.notice.web.dto;

import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.security.UserRef;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Contract schema PrivacyNotice. */
public record NoticeResponse(UUID id, String version, Language language, String noticeText, String consentText,
        LocalDate effectiveFrom, boolean current, Instant createdAt, UserRef createdBy) {
}
