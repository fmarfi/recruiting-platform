package com.recruiting.platform.security.audit.app;

import com.recruiting.platform.security.CurrentUser;
import com.recruiting.platform.security.domain.AuditRevision;
import org.hibernate.envers.RevisionListener;

/** Envers hook: stores the acting user on every revision (revinfo.user_id; null for system jobs). */
public class UserRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        CurrentUser.currentId().ifPresent(id -> ((AuditRevision) revisionEntity).setUserId(id));
    }
}
