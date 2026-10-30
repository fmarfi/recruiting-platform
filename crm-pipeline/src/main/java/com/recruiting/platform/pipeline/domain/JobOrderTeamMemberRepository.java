package com.recruiting.platform.pipeline.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link JobOrderTeamMember}. */
public interface JobOrderTeamMemberRepository extends JpaRepository<JobOrderTeamMember, JobOrderTeamMember.Key> {
    List<JobOrderTeamMember> findByIdJobOrderId(UUID jobOrderId);

    List<JobOrderTeamMember> findByIdUserId(UUID userId);
}
