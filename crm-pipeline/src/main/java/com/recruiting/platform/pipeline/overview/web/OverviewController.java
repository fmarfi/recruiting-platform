package com.recruiting.platform.pipeline.overview.web;

import com.recruiting.platform.pipeline.overview.app.AccountDashboardService;
import com.recruiting.platform.pipeline.overview.app.MyDayService;
import com.recruiting.platform.pipeline.overview.web.dto.AccountDashboardResponse;
import com.recruiting.platform.pipeline.overview.web.dto.MyDayResponse;
import com.recruiting.platform.pipeline.shared.Actor;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /accounts/{id}/dashboard and GET /home (openapi tags Account overview and Home). */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('CONSULTANT')")
public class OverviewController {

    private final AccountDashboardService dashboard;
    private final MyDayService myDay;

    OverviewController(AccountDashboardService dashboard, MyDayService myDay) {
        this.dashboard = dashboard;
        this.myDay = myDay;
    }

    @GetMapping("/accounts/{id}/dashboard")
    AccountDashboardResponse dashboard(@PathVariable UUID id) {
        return dashboard.dashboard(id);
    }

    @GetMapping("/home")
    MyDayResponse home(Authentication auth) {
        return myDay.myDay(Actor.of(auth));
    }
}
