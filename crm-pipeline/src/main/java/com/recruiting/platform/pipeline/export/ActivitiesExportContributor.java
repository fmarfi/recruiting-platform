package com.recruiting.platform.pipeline.export;

import com.recruiting.platform.accounts.PersonExportContributor;
import com.recruiting.platform.pipeline.activity.app.ActivityMapper;
import com.recruiting.platform.pipeline.domain.ActivityRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adds the {@code activities} section (list of Activity, newest first) to GET /people/{id}/export (S-04). */
@Component
class ActivitiesExportContributor implements PersonExportContributor {

    private final ActivityRepository repo;
    private final ActivityMapper mapper;

    ActivitiesExportContributor(ActivityRepository repo, ActivityMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    public String section() {
        return "activities";
    }

    @Override
    @Transactional(readOnly = true)
    public Object collect(UUID personId) {
        return mapper.toResponses(repo.findByPersonIdOrderByOccurredAtDesc(personId));
    }
}
