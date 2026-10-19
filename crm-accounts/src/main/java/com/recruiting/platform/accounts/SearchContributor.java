package com.recruiting.platform.accounts;

import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.JobOrderStatus;
import com.recruiting.platform.contracts.SearchType;
import java.util.List;
import java.util.UUID;

/**
 * SPI for global search (A-14, ADR 0001): a module that owns searchable data implements this and is picked up
 * by accounts. pipeline implements it for {@link SearchType#JOB_ORDER}; {@code items} must be the API
 * representation (the JobOrder schema of openapi.yaml), they are written to the response as they are.
 */
public interface SearchContributor {

    SearchType type();

    Result search(Criteria criteria);

    /**
     * @param query           raw user text (min 2 characters); match accent- and case-insensitively
     * @param sector          only records of accounts in this sector, or null
     * @param accountStatus   only records of accounts in this status, or null
     * @param jobOrderStatus  only job orders in this status, or null
     * @param ownerId         only records owned by this user, or null
     * @param limit           maximum number of items to return
     */
    record Criteria(String query, String sector, AccountStatus accountStatus, JobOrderStatus jobOrderStatus,
            UUID ownerId, int limit) {
    }

    /** @param total number of matches in all, not only the returned items */
    record Result(List<?> items, long total) {
    }
}
