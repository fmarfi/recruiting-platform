package com.recruiting.demo.domain;

import java.time.LocalDateTime;

/**
 * One entry in an account's timeline: a status change, activity, proposal or
 * candidate move (requirement A-08).
 *
 * @param accountId the account the event belongs to
 * @param at        when it happened
 * @param kind      {@code status}, {@code activity}, {@code proposal}, {@code candidate} or {@code job}
 * @param text      human-readable description
 * @param user      who did it
 */
public record TimelineEvent(String accountId, LocalDateTime at, String kind, String text,
                            String user) {
}
