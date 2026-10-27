package com.recruiting.platform.pipeline.application.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.recruiting.platform.contracts.PipelineStage;
import com.recruiting.platform.contracts.PipelineWarningCode;
import com.recruiting.platform.pipeline.exposure.web.dto.ExposureResponse;
import com.recruiting.platform.pipeline.shared.dto.AccountRef;
import com.recruiting.platform.pipeline.shared.dto.JobOrderRef;
import java.util.List;
import java.util.UUID;

/**
 * OpenAPI PipelineWarning: a non-blocking hint with a fixed English text per code (the UI translates the code).
 *
 * @param code           the warning
 * @param message        fixed text of the code
 * @param exposures      PREVIOUSLY_PRESENTED_TO_ACCOUNT: the earlier presentations to this account
 * @param otherProcesses ACTIVE_IN_OTHER_PROCESS: the other open applications of the person
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PipelineWarning(PipelineWarningCode code, String message, List<ExposureResponse> exposures,
        List<OtherProcess> otherProcesses) {

    /** OpenAPI OtherProcessRef. */
    public record OtherProcess(UUID applicationId, JobOrderRef jobOrder, AccountRef account, PipelineStage stage) {
    }
}
