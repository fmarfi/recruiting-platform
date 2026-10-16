package com.recruiting.demo.domain;

import com.recruiting.demo.domain.Types.ActivityType;
import java.time.LocalDateTime;

/**
 * A logged call, visit, meeting, e-mail or note (requirement A-04).
 *
 * @param id        unique id
 * @param accountId related account, or {@code null}
 * @param personId  related person, or {@code null}
 * @param type      kind of activity
 * @param note      what happened
 * @param at        when it happened
 * @param user      who logged it
 */
public record Activity(String id, String accountId, String personId, ActivityType type,
                       String note, LocalDateTime at, String user) {
}
