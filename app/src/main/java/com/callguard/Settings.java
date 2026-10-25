package com.callguard;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

/** Small wrapper around SharedPreferences holding the filter state. */
public final class Settings {

    private static final String PREFS = "callguard";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_PAUSED_UNTIL = "paused_until";
    private static final String KEY_ALLOW_NEXT_UNTIL = "allow_next_until";
    private static final String KEY_BLOCK_UNVERIFIED = "block_unverified";
    private static final String KEY_SCHEDULE_ON = "schedule_on";
    private static final String KEY_SCHEDULE_START = "schedule_start";
    private static final String KEY_SCHEDULE_END = "schedule_end";
    private static final String KEY_SCHEDULE_DAYS = "schedule_days";

    /** Default schedule window: weekday working hours. */
    private static final int DEFAULT_START = 9 * 60;
    private static final int DEFAULT_END = 18 * 60;

    /** How long the one-shot "let the next unknown call through" lasts. */
    public static final long ALLOW_NEXT_WINDOW_MS = 15L * 60L * 1000L;
    /** How long a pause lasts. */
    public static final long PAUSE_MS = 60L * 60L * 1000L;

    private static Settings instance;

    private final SharedPreferences prefs;

    private Settings(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized Settings get(Context context) {
        if (instance == null) {
            instance = new Settings(context);
        }
        return instance;
    }

    public boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, true);
    }

    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public boolean isBlockUnverified() {
        return prefs.getBoolean(KEY_BLOCK_UNVERIFIED, false);
    }

    public void setBlockUnverified(boolean value) {
        prefs.edit().putBoolean(KEY_BLOCK_UNVERIFIED, value).apply();
    }

    public long pausedUntil() {
        return prefs.getLong(KEY_PAUSED_UNTIL, 0L);
    }

    public boolean isPaused() {
        return pausedUntil() > System.currentTimeMillis();
    }

    public void pauseFor(long millis) {
        prefs.edit().putLong(KEY_PAUSED_UNTIL, System.currentTimeMillis() + millis).apply();
    }

    public void clearPause() {
        prefs.edit().putLong(KEY_PAUSED_UNTIL, 0L).apply();
    }

    public long allowNextUntil() {
        return prefs.getLong(KEY_ALLOW_NEXT_UNTIL, 0L);
    }

    public boolean hasAllowNext() {
        return allowNextUntil() > System.currentTimeMillis();
    }

    public void allowNextOnce() {
        prefs.edit()
                .putLong(KEY_ALLOW_NEXT_UNTIL, System.currentTimeMillis() + ALLOW_NEXT_WINDOW_MS)
                .apply();
    }

    /** Consumes the one-shot permission; called when an unknown call is let through. */
    public void consumeAllowNext() {
        prefs.edit().putLong(KEY_ALLOW_NEXT_UNTIL, 0L).apply();
    }

    // ------------------------------------------------------------- schedule

    public boolean isScheduleEnabled() {
        return prefs.getBoolean(KEY_SCHEDULE_ON, false);
    }

    public void setScheduleEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SCHEDULE_ON, enabled).apply();
    }

    public int scheduleStart() {
        return prefs.getInt(KEY_SCHEDULE_START, DEFAULT_START);
    }

    public void setScheduleStart(int minuteOfDay) {
        prefs.edit().putInt(KEY_SCHEDULE_START, minuteOfDay).apply();
    }

    public int scheduleEnd() {
        return prefs.getInt(KEY_SCHEDULE_END, DEFAULT_END);
    }

    public void setScheduleEnd(int minuteOfDay) {
        prefs.edit().putInt(KEY_SCHEDULE_END, minuteOfDay).apply();
    }

    public int scheduleDays() {
        return prefs.getInt(KEY_SCHEDULE_DAYS, Schedule.WEEKDAYS);
    }

    public void setScheduleDays(int mask) {
        prefs.edit().putInt(KEY_SCHEDULE_DAYS, mask).apply();
    }

    /** True when the schedule is enabled and the current local time falls into it. */
    public boolean isScheduleActiveNow() {
        if (!isScheduleEnabled()) {
            return false;
        }
        Calendar calendar = Calendar.getInstance();
        int dayIndex = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % Schedule.DAYS_COUNT;
        int minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        return Schedule.isActive(scheduleDays(), scheduleStart(), scheduleEnd(),
                dayIndex, minuteOfDay);
    }
}
