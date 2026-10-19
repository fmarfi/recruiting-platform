package com.recruiting.platform.pipeline.overview.web.dto;

import com.recruiting.platform.pipeline.joborder.web.dto.JobOrderResponse;
import com.recruiting.platform.pipeline.nextaction.web.dto.NextActionResponse;
import com.recruiting.platform.pipeline.shared.dto.AccountResponse;
import com.recruiting.platform.pipeline.shared.dto.UserRef;
import java.time.LocalDate;
import java.util.List;

/** OpenAPI MyDay. */
public record MyDayResponse(UserRef user, LocalDate today, List<NextActionResponse> nextActions, long overdueCount,
                            List<JobOrderResponse> openJobOrders, List<AccountResponse> recentAccounts,
                            Stats stats) {

    /** The statistics block of My day. */
    public record Stats(long activeAccounts, long openJobOrders, long candidatesInProcess, long placedThisYear) {
    }
}
