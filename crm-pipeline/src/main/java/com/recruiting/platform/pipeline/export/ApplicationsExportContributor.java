package com.recruiting.platform.pipeline.export;

import com.recruiting.platform.accounts.PersonExportContributor;
import com.recruiting.platform.pipeline.application.app.ApplicationMapper;
import com.recruiting.platform.pipeline.domain.ApplicationRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adds the {@code applications} section (list of Application, latest stage change first) to GET /people/{id}/export (S-04). */
@Component
class ApplicationsExportContributor implements PersonExportContributor {

    private final ApplicationRepository repo;
    private final ApplicationMapper mapper;

    ApplicationsExportContributor(ApplicationRepository repo, ApplicationMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    public String section() {
        return "applications";
    }

    @Override
    @Transactional(readOnly = true)
    public Object collect(UUID personId) {
        return mapper.toResponses(repo.findByPersonIdOrderByStageChangedAtDesc(personId));
    }
}
