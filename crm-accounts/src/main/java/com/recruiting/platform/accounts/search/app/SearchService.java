package com.recruiting.platform.accounts.search.app;

import com.recruiting.platform.accounts.SearchContributor;
import com.recruiting.platform.accounts.account.app.AccountMapper;
import com.recruiting.platform.accounts.account.app.AccountQueryService;
import com.recruiting.platform.accounts.domain.AccountRepository;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.accounts.person.app.PersonMapper;
import com.recruiting.platform.accounts.person.app.PersonQueryService;
import com.recruiting.platform.accounts.search.web.dto.SearchResponse;
import com.recruiting.platform.accounts.search.web.dto.SearchTotals;
import com.recruiting.platform.accounts.shared.PatchBinder;
import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.JobOrderStatus;
import com.recruiting.platform.contracts.SearchType;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Global search (A-14): accounts and people from this module, job orders from the pipeline's
 * {@link SearchContributor}. Reuses the list predicates so search and lists agree.
 */
@Service
@Transactional(readOnly = true)
public class SearchService {

    private final AccountRepository accounts;
    private final PersonRepository people;
    private final AccountQueryService accountQueries;
    private final PersonQueryService personQueries;
    private final AccountMapper accountMapper;
    private final PersonMapper personMapper;
    private final List<SearchContributor> contributors;

    public SearchService(AccountRepository accounts, PersonRepository people, AccountQueryService accountQueries,
            PersonQueryService personQueries, AccountMapper accountMapper, PersonMapper personMapper,
            List<SearchContributor> contributors) {
        this.accounts = accounts;
        this.people = people;
        this.accountQueries = accountQueries;
        this.personQueries = personQueries;
        this.accountMapper = accountMapper;
        this.personMapper = personMapper;
        this.contributors = contributors;
    }

    /** Filters of GET /search. */
    public record Query(String q, Set<SearchType> types, String sector, AccountStatus accountStatus,
            JobOrderStatus jobOrderStatus, UUID ownerId, LocalDate lastContactBefore, LocalDate lastContactAfter,
            Integer limit) {
    }

    public SearchResponse search(Query in) {
        String q = in.q() == null ? "" : in.q().trim();
        if (q.length() < 2 || q.length() > 200) {
            throw PatchBinder.validation("q", "SIZE", "must be between 2 and 200 characters");
        }
        int limit = in.limit() == null ? 10 : in.limit();
        if (limit < 1 || limit > 50) {
            throw PatchBinder.validation("limit", "RANGE", "must be between 1 and 50");
        }
        Set<SearchType> types = in.types() == null || in.types().isEmpty() ? Set.of(SearchType.values()) : in.types();
        PageRequest page = PageRequest.of(0, limit, Sort.by("name"));

        var accountItems = List.<com.recruiting.platform.accounts.account.web.dto.AccountResponse>of();
        long accountTotal = 0;
        if (types.contains(SearchType.ACCOUNT)) {
            var spec = accountQueries.spec(q, in.accountStatus() == null ? null : List.of(in.accountStatus()),
                    in.sector(), in.ownerId());
            var found = accounts.findAll(spec, page);
            accountItems = accountMapper.responses(found.getContent());
            accountTotal = found.getTotalElements();
        }
        var personItems = List.<com.recruiting.platform.accounts.person.web.dto.PersonSummaryResponse>of();
        long personTotal = 0;
        if (types.contains(SearchType.PERSON)) {
            var spec = personQueries.spec(q, null, in.ownerId(), null, in.lastContactBefore(),
                    in.lastContactAfter());
            var found = people.findAll(spec, page);
            personItems = personMapper.summaries(found.getContent());
            personTotal = found.getTotalElements();
        }
        List<?> jobItems = List.of();
        long jobTotal = 0;
        if (types.contains(SearchType.JOB_ORDER)) {
            var criteria = new SearchContributor.Criteria(q, in.sector(), in.accountStatus(), in.jobOrderStatus(),
                    in.ownerId(), limit);
            for (SearchContributor c : contributors) {
                if (c.type() == SearchType.JOB_ORDER) {
                    SearchContributor.Result r = c.search(criteria);
                    jobItems = r.items();
                    jobTotal = r.total();
                }
            }
        }
        return new SearchResponse(q, accountItems, personItems, jobItems,
                new SearchTotals(accountTotal, personTotal, jobTotal));
    }
}
