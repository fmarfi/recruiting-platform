package com.recruiting.platform.privacy.merge.app;

import com.recruiting.platform.accounts.PersonMergeApi;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.ConsentStatusChanged;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.RetentionBasis;
import com.recruiting.platform.contracts.RetentionStatus;
import com.recruiting.platform.pipeline.PipelinePersonApi;
import com.recruiting.platform.privacy.domain.ConsentRecord;
import com.recruiting.platform.privacy.domain.ConsentRecordRepository;
import com.recruiting.platform.privacy.domain.ConsentRequest;
import com.recruiting.platform.privacy.domain.ConsentRequestRepository;
import com.recruiting.platform.privacy.domain.DataSubjectRequest;
import com.recruiting.platform.privacy.domain.DataSubjectRequestRepository;
import com.recruiting.platform.privacy.domain.PersonPrivacy;
import com.recruiting.platform.privacy.domain.PersonPrivacyRepository;
import com.recruiting.platform.privacy.person.app.PersonGateway;
import com.recruiting.platform.privacy.person.app.PersonInfo;
import com.recruiting.platform.privacy.retention.app.RetentionPolicy;
import com.recruiting.platform.security.CurrentUser;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates {@code POST /people/{id}/merge} in ONE transaction (ADR 0011 decision 6), in the order of the contract:
 * 1 accounts moves profile, files, contacts and links; 2 pipeline moves applications, activities, next actions,
 * exposures and timeline; 3 privacy moves consent records, requests and DSRs and recomputes status and retention;
 * 4 accounts writes person_merge and deletes the merged person through JPA; then {@code PersonMerged} is published.
 * The merged id's Envers history is purged later together with the survivor's (the purge reads person_merge first).
 */
@Service
public class PersonMergeOrchestrator {

    private final ObjectProvider<PersonMergeApi> accounts;
    private final ObjectProvider<PipelinePersonApi> pipeline;
    private final PersonGateway people;
    private final PersonPrivacyRepository privacy;
    private final ConsentRecordRepository records;
    private final ConsentRequestRepository requests;
    private final DataSubjectRequestRepository dsrs;
    private final RetentionPolicy retention;
    private final CurrentUser currentUser;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    PersonMergeOrchestrator(ObjectProvider<PersonMergeApi> accounts, ObjectProvider<PipelinePersonApi> pipeline,
            PersonGateway people, PersonPrivacyRepository privacy, ConsentRecordRepository records,
            ConsentRequestRepository requests, DataSubjectRequestRepository dsrs, RetentionPolicy retention,
            CurrentUser currentUser, ApplicationEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.pipeline = pipeline;
        this.people = people;
        this.privacy = privacy;
        this.records = records;
        this.requests = requests;
        this.dsrs = dsrs;
        this.retention = retention;
        this.currentUser = currentUser;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public Object merge(UUID survivorId, UUID mergedId, List<String> takeFromMerged, long version) {
        if (survivorId.equals(mergedId)) {
            throw new ApiException(ErrorCode.STATE_CONFLICT, "A person cannot be merged into itself.");
        }
        PersonMergeApi accountsApi = accounts.getIfAvailable();
        PipelinePersonApi pipelineApi = pipeline.getIfAvailable();
        if (accountsApi == null || pipelineApi == null) {
            throw new ApiException(ErrorCode.STATE_CONFLICT, "Merging people is not available in this build.");
        }
        PersonInfo survivor = people.find(survivorId).orElseThrow(() -> notFound(survivorId));
        PersonInfo merged = people.find(mergedId).orElseThrow(() -> notFound(mergedId));
        if (survivor.anonymised() || merged.anonymised()) {
            throw new ApiException(ErrorCode.STATE_CONFLICT, "An anonymised person cannot be merged.");
        }
        UUID actor = currentUser.idOrEmpty().orElse(null);
        Set<String> take = takeFromMerged == null ? Set.of() : new LinkedHashSet<>(takeFromMerged);

        accountsApi.mergeProfile(survivorId, mergedId, take, version, actor); // 1
        pipelineApi.mergePerson(survivorId, mergedId); // 2
        mergeConsent(survivorId, mergedId, actor); // 3
        accountsApi.finish(survivorId, mergedId, take, actor); // 4, also publishes PersonMerged
        return people.find(survivorId).orElseThrow(() -> notFound(survivorId));
    }

    /** Step 3: consent records, requests and DSRs move; status = latest decision of the two; retention recomputed. */
    private void mergeConsent(UUID survivorId, UUID mergedId, UUID actor) {
        PersonPrivacy keep = retention.ensure(survivorId);
        PersonPrivacy gone = retention.ensure(mergedId);
        List<ConsentRecord> moved = records.findByPersonIdOrderByRecordedAtDesc(mergedId);
        moved.forEach(r -> r.setPersonId(survivorId));
        records.saveAll(moved);
        List<ConsentRequest> movedRequests = requests.findByPersonId(mergedId);
        movedRequests.forEach(r -> {
            r.setPersonId(survivorId);
            r.setReminderDaysBefore(null); // reminder uniqueness is per person and period; the history is not needed
            r.setReminderForRetainUntil(null);
        });
        requests.saveAll(movedRequests);
        List<DataSubjectRequest> movedDsrs = dsrs.findByPersonId(mergedId);
        movedDsrs.forEach(d -> d.setPersonId(survivorId));
        dsrs.saveAll(movedDsrs);
        records.flush();

        ConsentStatus before = keep.getConsentStatus();
        List<ConsentRecord> all = records.findByPersonIdOrderByRecordedAtDesc(survivorId);
        ConsentStatus status = all.isEmpty() ? furthest(keep.getConsentStatus(), gone.getConsentStatus())
                : ConsentStatus.valueOf(all.get(0).getDecision().name());
        keep.setConsentStatus(status);
        if (keep.getNoticeSentAt() == null
                || (gone.getNoticeSentAt() != null && gone.getNoticeSentAt().isBefore(keep.getNoticeSentAt()))) {
            keep.setNoticeSentAt(gone.getNoticeSentAt());
        }
        all.stream().filter(r -> r.getDecision().name().equals("GIVEN")).findFirst()
                .ifPresent(r -> keep.setLastConsentAt(r.getRecordedAt()));
        LocalDate basis = keep.getBasisDate().isBefore(gone.getBasisDate()) ? keep.getBasisDate() : gone.getBasisDate();
        if (retention.settings().getBasis() == RetentionBasis.FROM_LAST_CONSENT && keep.getLastConsentAt() != null) {
            basis = retention.dateOf(keep.getLastConsentAt());
        }
        keep.setBasisDate(basis);
        keep.setRetainUntil(retention.retainUntil(basis));
        if (keep.getRetentionStatus() != RetentionStatus.ANONYMISED
                && gone.getRetentionStatus() == RetentionStatus.FLAGGED) {
            keep.setRetentionStatus(RetentionStatus.FLAGGED);
        }
        privacy.save(keep);
        privacy.delete(gone); // through JPA so Envers writes the delete row
        privacy.flush();
        if (before != status) {
            events.publishEvent(new ConsentStatusChanged(UUID.randomUUID(), clock.instant(), actor, survivorId, before,
                    status, null, "merge"));
        }
    }

    private static ConsentStatus furthest(ConsentStatus a, ConsentStatus b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    private static ApiException notFound(UUID id) {
        return new ApiException(ErrorCode.NOT_FOUND, "Person " + id + " does not exist.");
    }
}
