package com.recruiting.platform.privacy.consent.app;

import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.ShareBlockReason;
import com.recruiting.platform.pipeline.ShareGuard;
import com.recruiting.platform.privacy.domain.PersonPrivacyRepository;
import com.recruiting.platform.privacy.person.app.PersonGateway;
import com.recruiting.platform.privacy.person.app.PersonInfo;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements pipeline's {@link ShareGuard} from the master consent data: shareable = consent GIVEN and not anonymised
 * (ADR 0011). A person without a privacy row has never been asked (NOT_SENT).
 */
@Component
public class ShareGuardImpl implements ShareGuard {

    private final PersonPrivacyRepository privacy;
    private final PersonGateway people;

    ShareGuardImpl(PersonPrivacyRepository privacy, PersonGateway people) {
        this.privacy = privacy;
        this.people = people;
    }

    /** The one rule, shared by the SPI, share-check and the consent overview. */
    public static ShareBlockReason reason(ConsentStatus status, boolean anonymised) {
        if (anonymised) {
            return ShareBlockReason.ANONYMISED;
        }
        return switch (status) {
            case GIVEN -> null;
            case NOT_SENT -> ShareBlockReason.CONSENT_NOT_REQUESTED;
            case SENT -> ShareBlockReason.CONSENT_PENDING;
            case REFUSED -> ShareBlockReason.CONSENT_REFUSED;
            case WITHDRAWN -> ShareBlockReason.CONSENT_WITHDRAWN;
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShareCheck> check(UUID personId) {
        return Optional.ofNullable(checkAll(java.util.List.of(personId)).get(personId));
    }

    /** One result per known id, in request order; unknown ids are left out. */
    @Transactional(readOnly = true)
    public Map<UUID, ShareCheck> checkAll(Collection<UUID> ids) {
        Map<UUID, PersonInfo> infos = people.findAll(ids);
        Map<UUID, ConsentStatus> statuses = new LinkedHashMap<>();
        privacy.findAllById(infos.keySet()).forEach(p -> statuses.put(p.getPersonId(), p.getConsentStatus()));
        Map<UUID, ShareCheck> result = new LinkedHashMap<>();
        for (UUID id : ids) {
            PersonInfo info = infos.get(id);
            if (info == null) {
                continue;
            }
            ShareBlockReason reason = reason(statuses.getOrDefault(id, ConsentStatus.NOT_SENT), info.anonymised());
            result.put(id, new ShareCheck(id, reason == null, reason));
        }
        return result;
    }
}
