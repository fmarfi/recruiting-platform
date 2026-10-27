package com.recruiting.platform.pipeline.merge.app;

import com.recruiting.platform.pipeline.PipelinePersonApi;
import com.recruiting.platform.pipeline.domain.Activity;
import com.recruiting.platform.pipeline.domain.ActivityRepository;
import com.recruiting.platform.pipeline.domain.Application;
import com.recruiting.platform.pipeline.domain.ApplicationRepository;
import com.recruiting.platform.pipeline.domain.CandidateExposureRepository;
import com.recruiting.platform.pipeline.domain.NextAction;
import com.recruiting.platform.pipeline.domain.NextActionRepository;
import com.recruiting.platform.pipeline.domain.TimelineEntryRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Pipeline step of a person merge; audited entities are changed through JPA so Envers writes revisions. */
@Service("pipelinePersonMergeService")
class PersonMergeService implements PipelinePersonApi {

    private final ApplicationRepository applications;
    private final ActivityRepository activities;
    private final NextActionRepository nextActions;
    private final CandidateExposureRepository exposures;
    private final TimelineEntryRepository timeline;

    PersonMergeService(ApplicationRepository applications, ActivityRepository activities,
            NextActionRepository nextActions, CandidateExposureRepository exposures,
            TimelineEntryRepository timeline) {
        this.applications = applications;
        this.activities = activities;
        this.nextActions = nextActions;
        this.exposures = exposures;
        this.timeline = timeline;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public MergeResult mergePerson(UUID survivorId, UUID mergedId) {
        int moved = 0;
        int dropped = 0;
        for (Application a : applications.findByPersonId(mergedId)) {
            Optional<Application> same = applications.findByJobOrderIdAndPersonId(a.getJobOrderId(), survivorId);
            if (same.isPresent()) {
                exposures.moveApplication(a.getId(), same.get().getId());
                applications.delete(a);
                applications.flush();
                dropped++;
            } else {
                a.setPersonId(survivorId);
                applications.saveAndFlush(a);
                moved++;
            }
        }
        List<Activity> acts = activities.findByPersonIdOrderByOccurredAtDesc(mergedId);
        acts.forEach(a -> a.setPersonId(survivorId));
        activities.saveAllAndFlush(acts);
        List<NextAction> actions = nextActions.findByPersonId(mergedId);
        actions.forEach(n -> n.setPersonId(survivorId));
        nextActions.saveAllAndFlush(actions);
        int exp = exposures.movePerson(mergedId, survivorId);
        int tl = timeline.movePerson(mergedId, survivorId);
        return new MergeResult(moved, dropped, acts.size(), actions.size(), exp, tl);
    }
}
