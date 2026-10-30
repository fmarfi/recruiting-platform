package com.recruiting.platform.pipeline.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link ActivityAttendee}. */
public interface ActivityAttendeeRepository extends JpaRepository<ActivityAttendee, ActivityAttendee.Key> {
    List<ActivityAttendee> findByIdActivityId(UUID activityId);

    List<ActivityAttendee> findByIdPersonId(UUID personId);
}
