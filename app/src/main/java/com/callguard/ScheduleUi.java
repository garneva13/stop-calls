package com.callguard;

import android.content.Context;

/** Human-readable descriptions of a {@link Schedule}. */
public final class ScheduleUi {

    private ScheduleUi() {
    }

    /** e.g. "будни, 09:00–18:00" */
    public static String describe(Context context, int mask, int start, int end) {
        String days;
        if (mask == Schedule.EVERY_DAY) {
            days = context.getString(R.string.schedule_every_day);
        } else if (mask == Schedule.WEEKDAYS) {
            days = context.getString(R.string.schedule_weekdays);
        } else if (mask == Schedule.WEEKEND) {
            days = context.getString(R.string.schedule_weekend);
        } else if (Schedule.selectedCount(mask) == 0) {
            days = context.getString(R.string.schedule_no_days);
        } else {
            String[] names = context.getResources().getStringArray(R.array.schedule_day_names);
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < Schedule.DAYS_COUNT; i++) {
                if (Schedule.isDaySelected(mask, i)) {
                    if (builder.length() > 0) {
                        builder.append(", ");
                    }
                    builder.append(names[i]);
                }
            }
            days = builder.toString();
        }
        return context.getString(R.string.schedule_format, days,
                Schedule.time(start), Schedule.time(end));
    }
}
