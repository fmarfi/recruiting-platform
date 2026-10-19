package com.recruiting.platform.pipeline.overview.app;

import com.recruiting.platform.contracts.JobOrderStatus;
import com.recruiting.platform.contracts.PipelineStage;
import com.recruiting.platform.contracts.ProposalStatus;
import com.recruiting.platform.pipeline.domain.ApplicationRepository;
import com.recruiting.platform.pipeline.domain.JobOrder;
import com.recruiting.platform.pipeline.domain.JobOrderRepository;
import com.recruiting.platform.pipeline.domain.NextActionRepository;
import com.recruiting.platform.pipeline.domain.ProposalRepository;
import com.recruiting.platform.pipeline.nextaction.app.NextActionMapper;
import com.recruiting.platform.pipeline.overview.web.dto.AccountDashboardResponse;
import com.recruiting.platform.pipeline.shared.Refs;
import com.recruiting.platform.pipeline.shared.dto.StageCounts;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Per-account numbers for the account page (A-09). */
@Service
public class AccountDashboardService {

    static final List<JobOrderStatus> OPEN = Arrays.stream(JobOrderStatus.values())
            .filter(JobOrderStatus::isOpen).toList();

    private final Refs refs;
    private final JobOrderRepository jobOrders;
    private final ApplicationRepository applications;
    private final ProposalRepository proposals;
    private final NextActionRepository nextActions;
    private final NextActionMapper nextActionMapper;

    AccountDashboardService(Refs refs, JobOrderRepository jobOrders, ApplicationRepository applications,
            ProposalRepository proposals, NextActionRepository nextActions, NextActionMapper nextActionMapper) {
        this.refs = refs;
        this.jobOrders = jobOrders;
        this.applications = applications;
        this.proposals = proposals;
        this.nextActions = nextActions;
        this.nextActionMapper = nextActionMapper;
    }

    @Transactional(readOnly = true)
    public AccountDashboardResponse dashboard(UUID accountId) {
        refs.requireAccount(accountId);
        List<JobOrder> open = jobOrders.findByAccountIdAndStatusIn(accountId, OPEN);
        LocalDate today = LocalDate.now();
        Double avg = open.isEmpty() ? null : open.stream()
                .mapToLong(j -> Math.max(0, ChronoUnit.DAYS.between(j.getOpenedOn(), today))).average().orElse(0);
        return new AccountDashboardResponse(accountId, open.size(),
                StageCounts.of(applications.countByStageForAccount(accountId, OPEN)), avg,
                applications.countByAccountAndStage(accountId, PipelineStage.PLACED),
                proposals.countByAccountIdAndStatusIn(accountId, List.of(ProposalStatus.DRAFT, ProposalStatus.SENT)),
                nextActions.findFirstByAccountIdAndDoneFalseOrderByDueOnAsc(accountId)
                        .map(nextActionMapper::toResponse).orElse(null));
    }
}
