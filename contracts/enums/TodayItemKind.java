package com.recruiting.platform.contracts;

/** Kind of a row in the Home TODAY block (`MyDay.todayItems`, since contract 1.4.0, ADR 0014). */
public enum TodayItemKind {
    /** An open next action of the user, due today or overdue by fewer than {@link HomeRiskType#ACTION_OVERDUE_DAYS} days. */
    ACTION,
    /** A MEETING or VISIT activity dated today, logged by the user or on one of the user's searches. */
    EVENT
}
