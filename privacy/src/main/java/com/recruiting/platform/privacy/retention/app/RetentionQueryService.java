package com.recruiting.platform.privacy.retention.app;

import com.recruiting.platform.contracts.RetentionStatus;
import com.recruiting.platform.privacy.consent.web.dto.ConsentDtos.RetentionInfo;
import com.recruiting.platform.privacy.domain.PersonPrivacy;
import com.recruiting.platform.privacy.domain.PersonPrivacyRepository;
import com.recruiting.platform.privacy.domain.RetentionRun;
import com.recruiting.platform.privacy.domain.RetentionRunRepository;
import com.recruiting.platform.privacy.person.app.PersonGateway;
import com.recruiting.platform.privacy.person.app.PersonInfo;
import com.recruiting.platform.privacy.retention.web.dto.RetentionDtos.Item;
import com.recruiting.platform.privacy.retention.web.dto.RetentionDtos.PersonSummary;
import com.recruiting.platform.privacy.retention.web.dto.RetentionDtos.RunResponse;
import com.recruiting.platform.security.UserDirectory;
import com.recruiting.platform.security.UserRef;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads of the Retention tag: flagged people and past runs. */
@Service
public class RetentionQueryService {

    private final PersonPrivacyRepository privacy;
    private final RetentionRunRepository runs;
    private final PersonGateway people;
    private final RetentionPolicy policy;
    private final UserDirectory directory;

    RetentionQueryService(PersonPrivacyRepository privacy, RetentionRunRepository runs, PersonGateway people,
            RetentionPolicy policy, UserDirectory directory) {
        this.privacy = privacy;
        this.runs = runs;
        this.people = people;
        this.policy = policy;
        this.directory = directory;
    }

    /** People whose period ends on or before the date (default: today plus the warning days), soonest first. */
    @Transactional(readOnly = true)
    public Page<Item> flagged(LocalDate retainUntilBefore, Pageable pageable) {
        LocalDate limit = retainUntilBefore != null ? retainUntilBefore
                : policy.today().plusDays(policy.settings().getWarningDays());
        Page<PersonPrivacy> page = privacy.findByRetentionStatusNotAndRetainUntilLessThanEqual(
                RetentionStatus.ANONYMISED, limit, pageable);
        Map<UUID, PersonInfo> infos = people.findAll(page.getContent().stream().map(PersonPrivacy::getPersonId)
                .toList());
        Map<UUID, UserRef> owners = directory.refs(infos.values().stream().map(PersonInfo::ownerId).toList());
        List<Item> items = page.getContent().stream().filter(p -> infos.containsKey(p.getPersonId()))
                .map(p -> item(p, infos.get(p.getPersonId()), owners)).toList();
        return new PageImpl<>(items, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<RunResponse> runs(Pageable pageable) {
        Page<RetentionRun> page = runs.findAllByOrderByStartedAtDesc(pageable);
        Map<UUID, UserRef> users = directory.refs(page.getContent().stream().map(RetentionRun::getTriggeredBy)
                .toList());
        return page.map(r -> response(r, users.get(r.getTriggeredBy())));
    }

    public RunResponse response(RetentionRun r, UserRef by) {
        return new RunResponse(r.getId(), r.getStartedAt(), r.getFinishedAt(), by, r.isDryRun(),
                r.getFlaggedCount(), r.getAnonymisedCount(), r.getDeletedCount(), r.getFailureReason());
    }

    private static Item item(PersonPrivacy p, PersonInfo i, Map<UUID, UserRef> owners) {
        return new Item(new PersonSummary(i.id(), i.name(), i.title(), i.company(), i.type(), i.city(), i.email(),
                owners.get(i.ownerId()), i.lastContactAt(), p.getConsentStatus(), p.getRetentionStatus()),
                new RetentionInfo(p.getRetentionStatus(), p.getBasisDate(), p.getRetainUntil(), p.getFlaggedAt()));
    }
}
