package com.callguard;

import android.Manifest;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Dashboard: filter state, quick actions, recent blocks and secondary screens. */
public final class MainActivity extends LocalizedActivity {

    private static final int REQ_CONTACTS = 1001;
    private static final int REQ_ROLE = 1002;
    private static final int REQ_NOTIFICATIONS = 1003;
    private static final int RECENT_COUNT = 3;

    private Switch switchFilter;
    private TextView textStatusTitle;
    private TextView textStatusDetail;
    private TextView textTodayBlocked;
    private TextView textLastBlocked;
    private TextView textWeekBlocked;
    private TextView textWeekAllowed;
    private TextView textWhitelistCount;
    private TextView textAboutVersion;
    private TextView textRecentEmpty;
    private View tilePause;
    private View tileSkip;
    private View tileSchedule;
    private TextView textPauseTitle;
    private TextView textPauseSub;
    private TextView textSkipTitle;
    private TextView textSkipSub;
    private TextView textScheduleTitle;
    private TextView textScheduleSub;
    private TextView linkJournal;
    private LinearLayout layoutHero;
    private LinearLayout layoutPermission;
    private LinearLayout layoutRecent;

    /** Guards the switch listener while the UI is being populated. */
    private boolean binding;

    private final SimpleDateFormat clock =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switchFilter = findViewById(R.id.switchFilter);
        textStatusTitle = findViewById(R.id.textStatusTitle);
        textStatusDetail = findViewById(R.id.textStatusDetail);
        textTodayBlocked = findViewById(R.id.textTodayBlocked);
        textLastBlocked = findViewById(R.id.textLastBlocked);
        textWeekBlocked = findViewById(R.id.textWeekBlocked);
        textWeekAllowed = findViewById(R.id.textWeekAllowed);
        textWhitelistCount = findViewById(R.id.textWhitelistCount);
        textAboutVersion = findViewById(R.id.textAboutVersion);
        textRecentEmpty = findViewById(R.id.textRecentEmpty);
        tilePause = findViewById(R.id.tilePause);
        tileSkip = findViewById(R.id.tileSkip);
        tileSchedule = findViewById(R.id.tileSchedule);
        textPauseTitle = findViewById(R.id.textPauseTitle);
        textPauseSub = findViewById(R.id.textPauseSub);
        textSkipTitle = findViewById(R.id.textSkipTitle);
        textSkipSub = findViewById(R.id.textSkipSub);
        textScheduleTitle = findViewById(R.id.textScheduleTitle);
        textScheduleSub = findViewById(R.id.textScheduleSub);
        linkJournal = findViewById(R.id.linkJournal);
        layoutHero = findViewById(R.id.layoutHero);
        layoutPermission = findViewById(R.id.layoutPermission);
        layoutRecent = findViewById(R.id.layoutRecent);

        switchFilter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (binding) {
                    return;
                }
                Settings settings = Settings.get(MainActivity.this);
                settings.setEnabled(isChecked);
                if (isChecked) {
                    settings.clearPause();
                }
                refresh();
            }
        });

        layoutHero.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasScreeningRole()) {
                    requestScreeningRole();
                }
            }
        });

        findViewById(R.id.buttonPermission).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestContactsPermission();
            }
        });

        tilePause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPauseDialog();
            }
        });

        tileSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Settings settings = Settings.get(MainActivity.this);
                if (settings.hasAllowNext()) {
                    settings.cancelAllowNext();
                    Toast.makeText(MainActivity.this, R.string.toast_allow_next_off,
                            Toast.LENGTH_SHORT).show();
                } else {
                    settings.allowNextOnce();
                    Toast.makeText(MainActivity.this, R.string.toast_allow_next,
                            Toast.LENGTH_SHORT).show();
                }
                refresh();
            }
        });

        tileSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ScheduleActivity.class));
            }
        });

        linkJournal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LogActivity.class));
            }
        });

        findViewById(R.id.rowWhitelist).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, WhitelistActivity.class));
            }
        });

        findViewById(R.id.rowRules).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRulesDialog();
            }
        });

        findViewById(R.id.rowStats).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, StatsActivity.class));
            }
        });

        findViewById(R.id.rowDiagnostics).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, DiagnosticsActivity.class));
            }
        });

        findViewById(R.id.rowAbout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            }
        });

        tintSecondaryIcons();
        maybeRequestNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The address book may have changed while the app was in background.
        ContactIndex.invalidate();
        refresh();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CONTACTS) {
            ContactIndex.invalidate();
            if (!hasContactsPermission()) {
                Toast.makeText(this, R.string.toast_permission_needed, Toast.LENGTH_LONG).show();
            }
            refresh();
        } else if (requestCode == REQ_NOTIFICATIONS) {
            // The status indicator needs this permission on Android 13+.
            Indicator.update(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ROLE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, R.string.toast_role_granted, Toast.LENGTH_SHORT).show();
            } else {
                showRoleHelpDialog();
            }
            refresh();
        }
    }

    // ------------------------------------------------------------------ ui

    private void tintSecondaryIcons() {
        int circle = getColor(R.color.icon_blue_bg);
        int fg = getColor(R.color.text_primary);
        for (int id : new int[]{R.id.iconStats, R.id.iconWhitelist, R.id.iconRules,
                R.id.iconDiagnostics, R.id.iconAbout}) {
            ImageView icon = findViewById(id);
            icon.setBackgroundTintList(ColorStateList.valueOf(circle));
            icon.setColorFilter(fg);
        }
    }

    private void refresh() {
        Settings settings = Settings.get(this);
        boolean hasRole = hasScreeningRole();
        boolean hasContacts = hasContactsPermission();

        binding = true;
        switchFilter.setChecked(settings.isEnabled());
        binding = false;

        layoutPermission.setVisibility(hasContacts ? View.GONE : View.VISIBLE);

        applyHeroState(settings, hasRole);

        DbHelper db = DbHelper.get(this);
        long now = System.currentTimeMillis();
        long weekAgo = now - 7L * 24L * 60L * 60L * 1000L;
        int today = db.blockedSince(startOfToday());

        textTodayBlocked.setText(today > 0
                ? getString(R.string.today_blocked, today)
                : getString(R.string.today_none));
        long last = db.lastBlockedTime();
        textLastBlocked.setText(last > 0
                ? getString(R.string.last_blocked, clock.format(new Date(last)))
                : "");
        textWeekBlocked.setText(String.valueOf(db.blockedSince(weekAgo)));
        textWeekAllowed.setText(String.valueOf(db.allowedSince(weekAgo)));

        int allowedNumbers = db.whitelistCount();
        textWhitelistCount.setText(getString(R.string.more_whitelist_count, allowedNumbers));
        textAboutVersion.setText(getString(R.string.about_version,
                UpdateManager.currentVersion(this)));

        boolean paused = settings.isPaused();
        tilePause.setBackgroundResource(paused ? R.drawable.bg_tile_warn : R.drawable.bg_tile);
        textPauseTitle.setText(paused
                ? getString(R.string.tile_until, shortTime(settings.pausedUntil()))
                : getString(R.string.tile_pause));
        textPauseTitle.setTextColor(getColor(
                paused ? R.color.hero_warn_text : R.color.text_primary));
        textPauseSub.setText(paused ? R.string.chip_resume : R.string.tile_pause_sub);

        boolean skipActive = settings.hasAllowNext();
        tileSkip.setBackgroundResource(skipActive ? R.drawable.bg_tile_accent : R.drawable.bg_tile);
        textSkipTitle.setText(skipActive
                ? getString(R.string.tile_skip_active)
                : getString(R.string.tile_skip));
        textSkipTitle.setTextColor(getColor(skipActive ? R.color.accent : R.color.text_primary));
        textSkipSub.setText(skipActive ? R.string.tile_skip_cancel : R.string.tile_skip_sub);

        boolean scheduleNow = settings.isScheduleActiveNow();
        tileSchedule.setBackgroundResource(scheduleNow
                ? R.drawable.bg_tile_accent : R.drawable.bg_tile);
        textScheduleTitle.setTextColor(getColor(
                scheduleNow ? R.color.accent : R.color.text_primary));
        textScheduleSub.setText(scheduleNow
                ? R.string.tile_schedule_active : R.string.tile_schedule_sub);

        bindRecent(db.blockedEvents(RECENT_COUNT));

        Indicator.update(this);
    }

    /** The status indicator needs POST_NOTIFICATIONS on Android 13+, ask once. */
    private void maybeRequestNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (getPreferences(MODE_PRIVATE).getBoolean("notifications_asked", false)) {
            return;
        }
        getPreferences(MODE_PRIVATE).edit().putBoolean("notifications_asked", true).apply();
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQ_NOTIFICATIONS);
    }

    private void applyHeroState(Settings settings, boolean hasRole) {
        int background;
        int titleColor;
        int hintColor;
        CharSequence title;
        CharSequence hint;

        if (!hasRole) {
            background = R.color.hero_off_bg;
            titleColor = R.color.hero_off_text;
            hintColor = R.color.hero_off_hint;
            title = getString(R.string.hero_inactive_title);
            hint = getString(R.string.hero_inactive_hint);
        } else if (!settings.isEnabled()) {
            background = R.color.hero_off_bg;
            titleColor = R.color.hero_off_text;
            hintColor = R.color.hero_off_hint;
            title = getString(R.string.hero_off_title);
            hint = getString(R.string.hero_off_hint);
        } else if (settings.isPaused()) {
            background = R.color.hero_warn_bg;
            titleColor = R.color.hero_warn_text;
            hintColor = R.color.hero_warn_hint;
            title = getString(R.string.hero_paused_title);
            hint = getString(R.string.hero_paused_hint,
                    clock.format(new Date(settings.pausedUntil())));
        } else if (settings.isScheduleActiveNow()) {
            background = R.color.hero_warn_bg;
            titleColor = R.color.hero_warn_text;
            hintColor = R.color.hero_warn_hint;
            title = getString(R.string.hero_schedule_title);
            hint = getString(R.string.hero_schedule_hint, Schedule.time(settings.scheduleEnd()));
        } else {
            background = R.color.hero_ok_bg;
            titleColor = R.color.hero_ok_text;
            hintColor = R.color.hero_ok_hint;
            title = getString(R.string.hero_on_title);
            hint = getString(R.string.hero_on_hint);
        }

        layoutHero.setBackgroundTintList(ColorStateList.valueOf(getColor(background)));
        textStatusTitle.setText(title);
        textStatusTitle.setTextColor(getColor(titleColor));
        textStatusDetail.setText(hint);
        textStatusDetail.setTextColor(getColor(hintColor));
        textTodayBlocked.setTextColor(getColor(titleColor));
        textLastBlocked.setTextColor(getColor(hintColor));
    }

    private void bindRecent(List<CallEvent> events) {
        layoutRecent.removeAllViews();
        boolean empty = events.isEmpty();
        layoutRecent.setVisibility(empty ? View.GONE : View.VISIBLE);
        textRecentEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            return;
        }

        LayoutInflater inflater = getLayoutInflater();
        for (int index = 0; index < events.size(); index++) {
            CallEvent event = events.get(index);
            View row = inflater.inflate(R.layout.item_recent, layoutRecent, false);
            TextView number = row.findViewById(R.id.textRecentNumber);
            TextView reason = row.findViewById(R.id.textRecentReason);
            TextView time = row.findViewById(R.id.textRecentTime);
            number.setText(Numbers.display(MainActivity.this, event));
            reason.setText(Reasons.labelRes(event.reason));
            time.setText(shortTime(event.time));
            styleRowIcon(row.findViewById(R.id.iconRecent), event);
            row.findViewById(R.id.dividerRecent)
                    .setVisibility(index == events.size() - 1 ? View.GONE : View.VISIBLE);
            layoutRecent.addView(row);
        }
    }

    private void styleRowIcon(ImageView icon, CallEvent event) {
        boolean blocked = event.isBlocked();
        int background = getColor(blocked ? R.color.icon_red_bg : R.color.icon_green_bg);
        int foreground = getColor(blocked ? R.color.icon_red : R.color.icon_green);
        icon.setBackgroundTintList(ColorStateList.valueOf(background));
        icon.setImageResource(blocked ? R.drawable.ic_ban : R.drawable.ic_check);
        icon.setColorFilter(foreground);
    }

    private String shortTime(long millis) {
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(millis);
        Calendar today = Calendar.getInstance();
        boolean sameDay = then.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && then.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
        if (sameDay) {
            return clock.format(new Date(millis));
        }
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        boolean isYesterday = then.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR)
                && then.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR);
        if (isYesterday) {
            return getString(R.string.log_yesterday).toLowerCase(Locale.getDefault());
        }
        return new SimpleDateFormat("dd.MM", Locale.getDefault()).format(new Date(millis));
    }

    private long startOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    // -------------------------------------------------------------- dialogs

    private void showRulesDialog() {
        View content = getLayoutInflater().inflate(R.layout.dialog_rules, null);
        final CheckBox check = content.findViewById(R.id.checkBlockUnverified);
        check.setChecked(Settings.get(this).isBlockUnverified());

        new AlertDialog.Builder(this)
                .setTitle(R.string.more_rules)
                .setView(content)
                .setPositiveButton(R.string.action_done, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Settings.get(MainActivity.this).setBlockUnverified(check.isChecked());
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /** Asks how long the pause should last; offers to resume when already paused. */
    private void showPauseDialog() {
        final Settings settings = Settings.get(this);
        final long[] durations = {
                30L * 60L * 1000L,
                60L * 60L * 1000L,
                2L * 60L * 60L * 1000L,
                3L * 60L * 60L * 1000L
        };
        String[] options = {
                getString(R.string.pause_30m),
                getString(R.string.pause_1h),
                getString(R.string.pause_2h),
                getString(R.string.pause_3h)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.pause_dialog_title)
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        settings.pauseFor(durations[which]);
                        refresh();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null);
        if (settings.isPaused()) {
            builder.setPositiveButton(R.string.action_resume, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    settings.clearPause();
                    refresh();
                }
            });
        }
        builder.show();
    }

    /** Explains what to pick in the system role dialog, where the phone app is listed first. */
    private void showRoleHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.role_dialog_title)
                .setMessage(R.string.role_dialog_message)
                .setPositiveButton(R.string.action_open_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openDefaultAppsSettings();
                    }
                })
                .setNegativeButton(R.string.action_close, null)
                .show();
    }

    // ------------------------------------------------------------ plumbing

    private boolean hasContactsPermission() {
        return checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasScreeningRole() {
        RoleManager roleManager = getSystemService(RoleManager.class);
        return roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
    }

    private void requestContactsPermission() {
        if (hasContactsPermission()) {
            refresh();
            return;
        }
        requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQ_CONTACTS);
    }

    private void requestScreeningRole() {
        RoleManager roleManager = getSystemService(RoleManager.class);
        if (roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(this, R.string.toast_role_unavailable, Toast.LENGTH_LONG).show();
            return;
        }
        startActivityForResult(
                roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), REQ_ROLE);
    }

    private void openDefaultAppsSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intent = new Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
        } else {
            intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
        }
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
        }
    }
}
