package com.recruiting.platform.privacy.consent.app;

import com.recruiting.platform.contracts.AccessAction;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.EmailDeliveryStatus;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.privacy.consent.web.dto.ConsentDtos.ConsentOverviewResponse;
import com.recruiting.platform.privacy.consent.web.dto.ConsentDtos.ConsentRequestResponse;
import com.recruiting.platform.privacy.consent.web.dto.ConsentDtos.RetentionInfo;
import com.recruiting.platform.privacy.domain.ConsentRecordRepository;
import com.recruiting.platform.privacy.domain.ConsentRequest;
import com.recruiting.platform.privacy.domain.ConsentRequestRepository;
import com.recruiting.platform.privacy.domain.PersonPrivacy;
import com.recruiting.platform.privacy.person.app.PersonGateway;
import com.recruiting.platform.privacy.retention.app.RetentionPolicy;
import com.recruiting.platform.security.AccessRecorder;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads of the Consent tag: a person's overview and the list of consent e-mails. */
@Service
public class ConsentQueryService {

    private final PersonGateway people;
    private final RetentionPolicy retention;
    private final ConsentRecordRepository records;
    private final ConsentRequestRepository requests;
    private final ConsentMapper mapper;
    private final AccessRecorder access;

    ConsentQueryService(PersonGateway people, RetentionPolicy retention, ConsentRecordRepository records,
            ConsentRequestRepository requests, ConsentMapper mapper, AccessRecorder access) {
        this.people = people;
        this.retention = retention;
        this.records = records;
        this.requests = requests;
        this.mapper = mapper;
        this.access = access;
    }

    /** Creates the privacy record if the person predates the module, so this is not a read-only transaction. */
    @Transactional
    public ConsentOverviewResponse overview(UUID personId) {
        if (people.find(personId).isEmpty()) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Person " + personId + " does not exist.");
        }
        PersonPrivacy privacy = retention.ensure(personId);
        access.record(AccessAction.VIEW, EntityType.PERSON, personId);
        ConsentRequestResponse last = requests.findFirstByPersonIdOrderByQueuedAtDesc(personId)
                .map(r -> mapper.requests(List.of(r)).get(0)).orElse(null);
        return new ConsentOverviewResponse(personId, privacy.getConsentStatus(), privacy.getNoticeSentAt(), last,
                new RetentionInfo(privacy.getRetentionStatus(), privacy.getBasisDate(), privacy.getRetainUntil(),
                        privacy.getFlaggedAt()),
                mapper.records(records.findByPersonIdOrderByRecordedAtDesc(personId)));
    }

    @Transactional(readOnly = true)
    public Page<ConsentRequest> requests(UUID personId, List<EmailDeliveryStatus> statuses, Boolean answered,
            Pageable pageable) {
        Specification<ConsentRequest> spec = (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (personId != null) {
                parts.add(cb.equal(root.get("personId"), personId));
            }
            if (statuses != null && !statuses.isEmpty()) {
                parts.add(root.get("deliveryStatus").in(statuses));
            }
            if (answered != null) {
                parts.add(answered ? cb.isNotNull(root.get("respondedAt")) : cb.isNull(root.get("respondedAt")));
            }
            return cb.and(parts.toArray(new Predicate[0]));
        };
        return requests.findAll(spec, pageable);
    }
}
