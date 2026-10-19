package com.recruiting.platform.pipeline.timeline.app;

import com.recruiting.platform.contracts.ActivityLogged;
import com.recruiting.platform.contracts.ApplicationCreated;
import com.recruiting.platform.contracts.ApplicationStageChanged;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.contracts.JobOrderCreated;
import com.recruiting.platform.contracts.JobOrderStatusChanged;
import com.recruiting.platform.contracts.ProposalCreated;
import com.recruiting.platform.contracts.ProposalStatusChanged;
import com.recruiting.platform.contracts.TimelineKind;
import com.recruiting.platform.pipeline.timeline.app.TimelineWriter.Entry;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Timeline entries for events of this module (job orders, candidates, proposals, activities). */
@Component
class PipelineTimelineListener {

    private final TimelineWriter writer;

    PipelineTimelineListener(TimelineWriter writer) {
        this.writer = writer;
    }

    @ApplicationModuleListener
    void on(JobOrderCreated e) {
        writer.append(e, new Entry(e.accountId(), TimelineKind.JOB_ORDER, EntityType.JOB_ORDER, e.jobOrderId(), null,
                e.status().name(), null, "Job order created: " + e.title(), null, e.jobOrderId()));
    }

    @ApplicationModuleListener
    void on(JobOrderStatusChanged e) {
        writer.append(e, TimelineWriter.status(e.accountId(), TimelineKind.JOB_ORDER, EntityType.JOB_ORDER,
                e.jobOrderId(), e.from(), e.to(), e.reason(), "Job order", null, e.jobOrderId()));
    }

    @ApplicationModuleListener
    void on(ApplicationCreated e) {
        writer.append(e, new Entry(e.accountId(), TimelineKind.CANDIDATE, EntityType.APPLICATION, e.applicationId(),
                null, e.stage().name(), null, "Candidate added to job order", e.personId(), e.jobOrderId()));
    }

    @ApplicationModuleListener
    void on(ApplicationStageChanged e) {
        writer.append(e, TimelineWriter.status(e.accountId(), TimelineKind.CANDIDATE, EntityType.APPLICATION,
                e.applicationId(), e.from(), e.to(), e.reason(), "Candidate", e.personId(), e.jobOrderId()));
    }

    @ApplicationModuleListener
    void on(ProposalCreated e) {
        writer.append(e, new Entry(e.accountId(), TimelineKind.PROPOSAL, EntityType.PROPOSAL, e.proposalId(), null,
                null, null, "Proposal created: " + e.title(), null, null));
    }

    @ApplicationModuleListener
    void on(ProposalStatusChanged e) {
        writer.append(e, TimelineWriter.status(e.accountId(), TimelineKind.PROPOSAL, EntityType.PROPOSAL,
                e.proposalId(), e.from(), e.to(), e.reason(), "Proposal", null, null));
    }

    @ApplicationModuleListener
    void on(ActivityLogged e) {
        writer.append(e, new Entry(e.accountId(), TimelineKind.ACTIVITY, EntityType.ACTIVITY, e.activityId(), null,
                null, null, "Activity logged: " + e.type().name(), e.personId(), e.jobOrderId()));
    }
}
