package com.recruiting.platform.privacy.notice.app;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.privacy.domain.PrivacyNotice;
import com.recruiting.platform.privacy.domain.PrivacyNoticeRepository;
import com.recruiting.platform.privacy.notice.web.dto.NoticeCreateRequest;
import com.recruiting.platform.privacy.notice.web.dto.NoticeResponse;
import com.recruiting.platform.security.UserDirectory;
import com.recruiting.platform.security.UserRef;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Versioned KVKK information and consent texts (NFR-02). A version is immutable once created. */
@Service
public class PrivacyNoticeService {

    private static final UserRef SYSTEM = new UserRef(new UUID(0, 0), "System");

    private final PrivacyNoticeRepository notices;
    private final UserDirectory directory;
    private final Clock clock;

    PrivacyNoticeService(PrivacyNoticeRepository notices, UserDirectory directory, Clock clock) {
        this.notices = notices;
        this.directory = directory;
        this.clock = clock;
    }

    /** All versions, newest first; {@code currentOnly} keeps the version in force per language. */
    @Transactional(readOnly = true)
    public List<NoticeResponse> list(Language language, boolean currentOnly) {
        List<PrivacyNotice> all = notices.findAll().stream()
                .filter(n -> language == null || n.getLanguage() == language)
                .sorted(Comparator.comparing(PrivacyNotice::getEffectiveFrom).reversed()
                        .thenComparing(PrivacyNotice::getCreatedAt, Comparator.reverseOrder()))
                .toList();
        Set<UUID> current = currentIds();
        return respond(all.stream().filter(n -> !currentOnly || current.contains(n.getId())).toList(), current);
    }

    @Transactional(readOnly = true)
    public NoticeResponse get(UUID id) {
        PrivacyNotice notice = notices.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Notice " + id + " does not exist."));
        return respond(List.of(notice), currentIds()).get(0);
    }

    @Transactional
    public NoticeResponse create(NoticeCreateRequest request) {
        String version = request.version().trim();
        if (notices.existsByVersionAndLanguage(version, request.language())) {
            throw new ApiException(ErrorCode.DUPLICATE, "This version already exists for this language.");
        }
        PrivacyNotice notice = new PrivacyNotice();
        notice.setVersion(version);
        notice.setLanguage(request.language());
        notice.setNoticeText(request.noticeText());
        notice.setConsentText(request.consentText());
        notice.setEffectiveFrom(request.effectiveFrom());
        PrivacyNotice saved = notices.saveAndFlush(notice);
        return respond(List.of(saved), currentIds()).get(0);
    }

    /** The notice in force today for the language (used when a consent request is sent). */
    @Transactional(readOnly = true)
    public Optional<PrivacyNotice> currentFor(Language language) {
        return notices.findFirstByLanguageAndEffectiveFromLessThanEqualOrderByEffectiveFromDescCreatedAtDesc(
                language, LocalDate.now(clock));
    }

    private Set<UUID> currentIds() {
        return java.util.Arrays.stream(Language.values())
                .map(this::currentFor).flatMap(Optional::stream).map(PrivacyNotice::getId)
                .collect(Collectors.toSet());
    }

    private List<NoticeResponse> respond(List<PrivacyNotice> list, Set<UUID> current) {
        Map<UUID, UserRef> users = directory.refs(list.stream().map(PrivacyNotice::getCreatedBy).toList());
        return list.stream().map(n -> new NoticeResponse(n.getId(), n.getVersion(), n.getLanguage(),
                n.getNoticeText(), n.getConsentText(), n.getEffectiveFrom(), current.contains(n.getId()),
                n.getCreatedAt(), users.getOrDefault(n.getCreatedBy(), SYSTEM))).toList();
    }
}
