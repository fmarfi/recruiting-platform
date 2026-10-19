package com.recruiting.platform.privacy.consent.app;

import com.recruiting.platform.accounts.PersonExportContributor;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adds the {@code consent} section (ConsentOverview) to GET /people/{id}/export (S-04). */
@Component
class ConsentExportContributor implements PersonExportContributor {

    private final ConsentQueryService consent;

    ConsentExportContributor(ConsentQueryService consent) {
        this.consent = consent;
    }

    @Override
    public String section() {
        return "consent";
    }

    @Override
    public Object collect(UUID personId) {
        return consent.overview(personId);
    }
}
