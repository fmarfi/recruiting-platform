package com.recruiting.platform.accounts.referencedata.app;

import com.recruiting.platform.accounts.domain.AccountRepository;
import com.recruiting.platform.accounts.person.app.PersonQueryService;
import com.recruiting.platform.accounts.referencedata.web.dto.LifecycleDefinition;
import com.recruiting.platform.accounts.referencedata.web.dto.Lifecycles;
import com.recruiting.platform.accounts.referencedata.web.dto.ReferenceDataResponse;
import com.recruiting.platform.accounts.referencedata.web.dto.StatusDefinition;
import com.recruiting.platform.accounts.referencedata.web.dto.TransitionDefinition;
import com.recruiting.platform.accounts.referencedata.web.dto.Vocabularies;
import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.ActivityType;
import com.recruiting.platform.contracts.ConsentChannel;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.DataSubjectRequestStatus;
import com.recruiting.platform.contracts.DataSubjectRequestType;
import com.recruiting.platform.contracts.JobOrderStatus;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.Lifecycle;
import com.recruiting.platform.contracts.LinkType;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.PipelineStage;
import com.recruiting.platform.contracts.ProposalStatus;
import com.recruiting.platform.contracts.Role;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds GET /reference-data from the lifecycle enums (ADR 0002) so the UI never hard-codes transitions.
 * Statuses are listed in enum declaration order; the first status of each lifecycle is its initial one.
 */
@Service
@Transactional(readOnly = true)
public class ReferenceDataService {

    private final AccountRepository accounts;

    public ReferenceDataService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    public ReferenceDataResponse get() {
        Lifecycles lifecycles = new Lifecycles(lifecycle(AccountStatus.class), lifecycle(ProposalStatus.class),
                lifecycle(JobOrderStatus.class), lifecycle(PipelineStage.class), lifecycle(ConsentStatus.class),
                lifecycle(DataSubjectRequestStatus.class));
        Vocabularies vocabularies = new Vocabularies(List.of(PersonType.values()), List.of(ActivityType.values()),
                List.of(LinkType.values()), List.of(Role.values()), List.of(ConsentChannel.values()),
                List.of(DataSubjectRequestType.values()), List.of(Language.values()));
        return new ReferenceDataResponse(LocalDate.now(PersonQueryService.ZONE), lifecycles,
                List.of(PipelineStage.values()), vocabularies, accounts.findDistinctSectors());
    }

    private static <S extends Enum<S> & Lifecycle<S>> LifecycleDefinition lifecycle(Class<S> type) {
        S[] all = type.getEnumConstants();
        List<StatusDefinition> statuses = Arrays.stream(all).map(s -> status(s, all)).toList();
        return new LifecycleDefinition(all[0].name(), statuses);
    }

    private static <S extends Enum<S> & Lifecycle<S>> StatusDefinition status(S s, S[] order) {
        List<TransitionDefinition> next = Arrays.stream(order).filter(s::canMoveTo)
                .map(to -> new TransitionDefinition(to.name(), s.requiresReason(to), s.requiresElevatedRole(to)))
                .toList();
        return new StatusDefinition(s.name(), s.labelEn(), s.labelTr(), s.tone(), s.isTerminal(), next);
    }
}
