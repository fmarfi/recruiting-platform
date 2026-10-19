package com.recruiting.platform.security.audit.web;

import com.recruiting.platform.contracts.AccessAction;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.security.ApiPage;
import com.recruiting.platform.security.UserDirectory;
import com.recruiting.platform.security.UserRef;
import com.recruiting.platform.security.audit.app.AccessLogQueryService;
import com.recruiting.platform.security.audit.web.dto.AccessLogEntryResponse;
import com.recruiting.platform.security.domain.AccessLog;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tag Audit: who viewed or exported what (admin only). */
@RestController
@RequestMapping("/api/v1/audit/access-log")
class AccessLogController {

    private final AccessLogQueryService queries;
    private final UserDirectory directory;

    AccessLogController(AccessLogQueryService queries, UserDirectory directory) {
        this.queries = queries;
        this.directory = directory;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    ApiPage<AccessLogEntryResponse> list(@RequestParam(required = false) UUID userId,
            @RequestParam(required = false) EntityType entityType, @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) AccessAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Page<AccessLog> result = queries.find(userId, entityType, entityId, action, from, to, page, size);
        Map<UUID, UserRef> users = directory.refs(result.getContent().stream().map(AccessLog::getUserId).toList());
        return ApiPage.of(result, row -> new AccessLogEntryResponse(row.getId(),
                users.getOrDefault(row.getUserId(), new UserRef(row.getUserId(), "?")), row.getAction(),
                row.getEntityType(), row.getEntityId(), row.getAt(), row.getIpAddress()));
    }
}
