package com.recruiting.platform.accounts.referencedata.web.dto;

/** All lifecycles of the reference data. */
public record Lifecycles(LifecycleDefinition account, LifecycleDefinition proposal, LifecycleDefinition jobOrder,
        LifecycleDefinition pipelineStage, LifecycleDefinition consent, LifecycleDefinition dataSubjectRequest) {
}
