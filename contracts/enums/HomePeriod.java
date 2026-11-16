package com.recruiting.platform.contracts;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Period of the personal numbers on Home ({@code GET /home?period=}, {@code MyDay.myNumbers},
 * since contract 1.5.0, ADR 0015). Days are calendar days in {@code Europe/Istanbul}; both ends are
 * inclusive; weeks start on Monday (ISO). Default {@link #LAST_30_DAYS}, the client's own setting.
 */
public enum HomePeriod {
    /** Today only. */
    TODAY,
    /** Monday of this week up to today. */
    THIS_WEEK,
    /** The previous full week, Monday to Sunday (ASSUMPTION for "last week", meeting 02 Q8). */
    LAST_WEEK,
    /** Today and the 6 days before (rolling week). */
    LAST_7_DAYS,
    /** Today and the 29 days before (default). */
    LAST_30_DAYS,
    /** The first of this month up to today. */
    THIS_MONTH;

    /** Period used when the request has none. */
    public static final HomePeriod DEFAULT = LAST_30_DAYS;

    /**
     * First day of the period.
     *
     * @param today today in Europe/Istanbul
     * @return the first day, inclusive
     */
    public LocalDate from(LocalDate today) {
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return switch (this) {
            case TODAY -> today;
            case THIS_WEEK -> monday;
            case LAST_WEEK -> monday.minusWeeks(1);
            case LAST_7_DAYS -> today.minusDays(6);
            case LAST_30_DAYS -> today.minusDays(29);
            case THIS_MONTH -> today.withDayOfMonth(1);
        };
    }

    /**
     * Last day of the period.
     *
     * @param today today in Europe/Istanbul
     * @return the last day, inclusive: today, or last Sunday for {@link #LAST_WEEK}
     */
    public LocalDate to(LocalDate today) {
        return this == LAST_WEEK
                ? today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusDays(1)
                : today;
    }
}
