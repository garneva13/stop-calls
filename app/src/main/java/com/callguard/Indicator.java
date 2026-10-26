package com.callguard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Ongoing, silent notification that shows whether the filter is running.
 *
 * It is the only piece of the app that stays visible in the system UI; the
 * filter itself has no background service and is started by Telecom per call.
 */
public final class Indicator {

    private static final String CHANNEL_ID = "status";
    private static final int NOTIFICATION_ID = 1001;

    private Indicator() {
    }

    /** Shows, updates or hides the status notification depending on the current state. */
    public static void update(Context context) {
        Context app = LocaleHelper.wrap(context.getApplicationContext());
        Settings settings = Settings.get(app);

        if (!hasRole(app) || !settings.isEnabled()) {
            hide(app);
            return;
        }

        ensureChannel(app);

        String title;
        String text;
        if (settings.isPaused()) {
            title = app.getString(R.string.notification_title_paused);
            text = app.getString(R.string.notification_text_paused,
                    clock(settings.pausedUntil()));
        } else if (settings.isScheduleActiveNow()) {
            title = app.getString(R.string.notification_title_paused);
            text = app.getString(R.string.notification_text_paused,
                    Schedule.time(settings.scheduleEnd()));
        } else {
            title = app.getString(R.string.notification_title_on);
            int today = DbHelper.get(app).blockedSince(startOfToday());
            text = today > 0
                    ? app.getString(R.string.notification_text_blocked, today)
                    : app.getString(R.string.notification_text_on);
        }

        PendingIntent contentIntent = PendingIntent.getActivity(app, 0,
                new Intent(app, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_shield)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setShowWhen(false)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(contentIntent)
                .build();

        NotificationManager manager = app.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        try {
            manager.notify(NOTIFICATION_ID, notification);
        } catch (RuntimeException ignored) {
            // Notifications are disabled for the app; nothing to show.
        }
    }

    public static void hide(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);
        }
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(context.getString(R.string.notification_channel_desc));
        channel.setShowBadge(false);
        channel.enableVibration(false);
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);
    }

    private static boolean hasRole(Context context) {
        RoleManager roleManager = context.getSystemService(RoleManager.class);
        return roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
    }

    private static long startOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static String clock(long millis) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(millis));
    }
}
