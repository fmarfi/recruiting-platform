package com.recruiting.platform.accounts.search.web.dto;

import com.recruiting.platform.accounts.account.web.dto.AccountResponse;
import com.recruiting.platform.accounts.person.web.dto.PersonSummaryResponse;
import java.util.List;

/** Grouped search results (SearchResults schema); jobOrders come from the pipeline SearchContributor. */
public record SearchResponse(String query, List<AccountResponse> accounts, List<PersonSummaryResponse> people,
        List<?> jobOrders, SearchTotals totals) {
}
