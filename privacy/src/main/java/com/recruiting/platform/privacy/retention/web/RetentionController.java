package com.recruiting.platform.privacy.retention.web;

import com.recruiting.platform.privacy.retention.app.RetentionQueryService;
import com.recruiting.platform.privacy.retention.app.RetentionRunService;
import com.recruiting.platform.privacy.retention.app.RetentionSettingsService;
import com.recruiting.platform.privacy.retention.web.dto.RetentionDtos.Item;
import com.recruiting.platform.privacy.retention.web.dto.RetentionDtos.RunRequest;
import com.recruiting.platform.privacy.retention.web.dto.RetentionDtos.RunResponse;
import com.recruiting.platform.privacy.retention.web.dto.RetentionDtos.Settings;
import com.recruiting.platform.security.ApiPage;
import com.recruiting.platform.security.ApiPageable;
import com.recruiting.platform.security.CurrentUser;
import com.recruiting.platform.security.UserDirectory;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tag Retention: settings (managers read, admins change), flagged people and runs. */
@RestController
@RequestMapping("/api/v1/retention")
class RetentionController {

    private static final Map<String, String> SORTABLE = Map.of("retainUntil", "retainUntil",
            "basisDate", "basisDate", "flaggedAt", "flaggedAt");

    private final RetentionSettingsService settings;
    private final RetentionQueryService queries;
    private final RetentionRunService runner;
    private final CurrentUser currentUser;
    private final UserDirectory directory;

    RetentionController(RetentionSettingsService settings, RetentionQueryService queries,
            RetentionRunService runner, CurrentUser currentUser, UserDirectory directory) {
        this.settings = settings;
        this.queries = queries;
        this.runner = runner;
        this.currentUser = currentUser;
        this.directory = directory;
    }

    @GetMapping("/settings")
    @PreAuthorize("hasRole('MANAGER')")
    Settings getSettings() {
        return settings.get();
    }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    Settings updateSettings(@Valid @RequestBody Settings body) {
        return settings.update(body);
    }

    @GetMapping("/flagged")
    @PreAuthorize("hasRole('MANAGER')")
    ApiPage<Item> flagged(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate retainUntilBefore,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort) {
        var pageable = ApiPageable.of(page, size, sort, SORTABLE, Sort.by("retainUntil"));
        return ApiPage.of(queries.flagged(retainUntilBefore, pageable), i -> i);
    }

    @GetMapping("/runs")
    @PreAuthorize("hasRole('MANAGER')")
    ApiPage<RunResponse> runs(@RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        var pageable = ApiPageable.of(page, size, null, Map.of(), Sort.unsorted());
        return ApiPage.of(queries.runs(pageable), r -> r);
    }

    @PostMapping("/runs")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    RunResponse start(@Valid @RequestBody RunRequest body) {
        var run = runner.run(body.dryRun(), currentUser.id());
        return queries.response(run, directory.ref(currentUser.id()).orElse(null));
    }
}
