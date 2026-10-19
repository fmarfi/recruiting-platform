package com.recruiting.platform.privacy.consent.app;

import com.recruiting.platform.privacy.consent.web.dto.ConsentDtos.ConsentRecordResponse;
import com.recruiting.platform.privacy.consent.web.dto.ConsentDtos.ConsentRequestResponse;
import com.recruiting.platform.privacy.consent.web.dto.ConsentDtos.NoticeRef;
import com.recruiting.platform.privacy.domain.ConsentRecord;
import com.recruiting.platform.privacy.domain.ConsentRequest;
import com.recruiting.platform.privacy.domain.PrivacyNotice;
import com.recruiting.platform.privacy.domain.PrivacyNoticeRepository;
import com.recruiting.platform.security.UserDirectory;
import com.recruiting.platform.security.UserRef;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Entities to the contract shapes of the Consent tag, looking up notice and user references in one query each. */
@Component
public class ConsentMapper {

    private final PrivacyNoticeRepository notices;
    private final UserDirectory directory;

    ConsentMapper(PrivacyNoticeRepository notices, UserDirectory directory) {
        this.notices = notices;
        this.directory = directory;
    }

    @Transactional(readOnly = true)
    public List<ConsentRequestResponse> requests(Collection<ConsentRequest> requests) {
        Map<UUID, NoticeRef> refs = noticeRefs(requests.stream().map(ConsentRequest::getNoticeId).toList());
        Map<UUID, UserRef> users = directory.refs(requests.stream().map(ConsentRequest::getRequestedBy).toList());
        return requests.stream().map(r -> new ConsentRequestResponse(r.getId(), r.getPersonId(),
                refs.get(r.getNoticeId()), r.getLanguage(), r.getDeliveryStatus(), r.getQueuedAt(), r.getSentAt(),
                r.getFailureReason(), r.getExpiresAt(), r.getRespondedAt(), r.getResponse(),
                users.get(r.getRequestedBy()))).toList();
    }

    @Transactional(readOnly = true)
    public List<ConsentRecordResponse> records(Collection<ConsentRecord> records) {
        Map<UUID, NoticeRef> refs = noticeRefs(records.stream().map(ConsentRecord::getNoticeId).toList());
        Map<UUID, UserRef> users = directory.refs(records.stream().map(ConsentRecord::getRecordedBy).toList());
        return records.stream().map(r -> new ConsentRecordResponse(r.getId(), r.getPersonId(), r.getDecision(),
                r.getChannel(), refs.get(r.getNoticeId()), r.getRecordedAt(), users.get(r.getRecordedBy()),
                r.getEvidence())).toList();
    }

    private Map<UUID, NoticeRef> noticeRefs(Collection<UUID> ids) {
        return notices.findAllById(ids.stream().filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(PrivacyNotice::getId,
                        n -> new NoticeRef(n.getId(), n.getVersion(), n.getLanguage()), (a, b) -> a,
                        java.util.HashMap::new));
    }
}
