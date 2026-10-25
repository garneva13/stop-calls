package com.callguard;

import java.util.Locale;

/**
 * Weekly pause schedule: during the selected days and hours the filter lets
 * unknown calls through.
 *
 * Pure Java (no Android imports) so the midnight-crossing rules can be
 * unit-tested on a plain JVM — see tools/ScheduleSelfTest.java.
 */
public final class Schedule {

    public static final int DAYS_COUNT = 7;

    /** Monday is bit 0, Sunday is bit 6. */
    public static final int EVERY_DAY = 0x7F;
    public static final int WEEKDAYS = 0x1F;
    public static final int WEEKEND = 0x60;

    private Schedule() {
    }

    public static boolean isDaySelected(int mask, int dayIndex) {
        return dayIndex >= 0 && dayIndex < DAYS_COUNT && (mask & (1 << dayIndex)) != 0;
    }

    public static int withDay(int mask, int dayIndex, boolean selected) {
        if (dayIndex < 0 || dayIndex >= DAYS_COUNT) {
            return mask;
        }
        return selected ? (mask | (1 << dayIndex)) : (mask & ~(1 << dayIndex));
    }

    public static int selectedCount(int mask) {
        int count = 0;
        for (int i = 0; i < DAYS_COUNT; i++) {
            if (isDaySelected(mask, i)) {
                count++;
            }
        }
        return count;
    }

    public static boolean crossesMidnight(int startMinute, int endMinute) {
        return startMinute > endMinute;
    }

    /**
     * @param dayIndex     0 = Monday ... 6 = Sunday
     * @param minuteOfDay  minutes since local midnight
     */
    public static boolean isActive(int mask, int startMinute, int endMinute,
                                   int dayIndex, int minuteOfDay) {
        if (startMinute == endMinute) {
            return false;
        }
        if (startMinute < endMinute) {
            return isDaySelected(mask, dayIndex)
                    && minuteOfDay >= startMinute && minuteOfDay < endMinute;
        }
        // The window crosses midnight and belongs to the day it started on:
        // yesterday evening still counts, and today's early morning finishes it.
        if (isDaySelected(mask, dayIndex) && minuteOfDay >= startMinute) {
            return true;
        }
        int yesterday = (dayIndex + DAYS_COUNT - 1) % DAYS_COUNT;
        return isDaySelected(mask, yesterday) && minuteOfDay < endMinute;
    }

    /** "09:05" for 545. */
    public static String time(int minuteOfDay) {
        int normalized = ((minuteOfDay % 1440) + 1440) % 1440;
        return String.format(Locale.US, "%02d:%02d", normalized / 60, normalized % 60);
    }
}
