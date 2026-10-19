package com.recruiting.platform.accounts.referencedata.web.dto;

import com.recruiting.platform.contracts.PipelineStage;
import java.time.LocalDate;
import java.util.List;

/** Body of GET /reference-data. */
public record ReferenceDataResponse(LocalDate today, Lifecycles lifecycles, List<PipelineStage> stageOrder,
        Vocabularies vocabularies, List<String> sectors) {
}
