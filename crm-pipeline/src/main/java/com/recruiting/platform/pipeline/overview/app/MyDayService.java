package com.recruiting.platform.pipeline.overview.app;

import com.recruiting.platform.contracts.PipelineStage;
import com.recruiting.platform.pipeline.domain.ActivityRepository;
import com.recruiting.platform.pipeline.domain.ApplicationRepository;
import com.recruiting.platform.pipeline.domain.JobOrderRepository;
import com.recruiting.platform.pipeline.domain.NextActionRepository;
import com.recruiting.platform.pipeline.joborder.app.JobOrderMapper;
import com.recruiting.platform.pipeline.nextaction.app.NextActionMapper;
import com.recruiting.platform.pipeline.overview.web.dto.MyDayResponse;
import com.recruiting.platform.pipeline.shared.Actor;
import com.recruiting.platform.pipeline.shared.Refs;
import com.recruiting.platform.pipeline.shared.dto.AccountResponse;
import com.recruiting.platform.pipeline.shared.ref.AccountDirectory;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The home screen "My day" (UX-01): follow-ups, own job orders, recent accounts and a few numbers. */
@Service
public class MyDayService {

    private final Refs refs;
    private final AccountDirectory accounts;
    private final NextActionRepository nextActions;
    private final NextActionMapper nextActionMapper;
    private final JobOrderRepository jobOrders;
    private final JobOrderMapper jobOrderMapper;
    private final ActivityRepository activities;
    private final ApplicationRepository applications;

    MyDayService(Refs refs, AccountDirectory accounts, NextActionRepository nextActions,
            NextActionMapper nextActionMapper, JobOrderRepository jobOrders, JobOrderMapper jobOrderMapper,
            ActivityRepository activities, ApplicationRepository applications) {
        this.refs = refs;
        this.accounts = accounts;
        this.nextActions = nextActions;
        this.nextActionMapper = nextActionMapper;
        this.jobOrders = jobOrders;
        this.jobOrderMapper = jobOrderMapper;
        this.activities = activities;
        this.applications = applications;
    }

    @Transactional(readOnly = true)
    public MyDayResponse myDay(Actor actor) {
        LocalDate today = LocalDate.now();
        UUID me = actor.id();
        var actions = nextActions.findByOwnerIdAndDoneFalseAndDueOnLessThanEqualOrderByDueOnAsc(me,
                today.plusDays(7), PageRequest.of(0, 20));
        var mine = jobOrders.findByOwnerIdAndStatusInOrderByOpenedOnDesc(me, AccountDashboardService.OPEN,
                PageRequest.of(0, 20));
        List<UUID> recentIds = activities.recentAccountIds(PageRequest.of(0, 5));
        Refs.Lookup l = refs.load(recentIds, List.of(), List.of(me));
        List<AccountResponse> recent = recentIds.stream().map(l::accountInfo).filter(java.util.Objects::nonNull)
                .map(a -> AccountResponse.of(a, l.user(a.ownerId()))).toList();
        long placed = applications.countByStageAndStageChangedAtGreaterThanEqual(PipelineStage.PLACED,
                today.withDayOfYear(1).atStartOfDay().toInstant(ZoneOffset.UTC));
        var stats = new MyDayResponse.Stats(accounts.countActiveAccounts(),
                jobOrders.countByStatusIn(AccountDashboardService.OPEN),
                applications.countInProcess(AccountDashboardService.OPEN,
                        List.of(PipelineStage.PLACED, PipelineStage.REJECTED)), placed);
        return new MyDayResponse(l.user(me), today, nextActionMapper.toResponses(actions),
                nextActions.countByOwnerIdAndDoneFalseAndDueOnBefore(me, today),
                jobOrderMapper.toResponses(mine), recent, stats);
    }
}
