package com.callguard;

import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Answers the only two questions that matter when something does not work:
 * does the system call us at all, and what do we decide for a given number.
 */
public final class DiagnosticsActivity extends LocalizedActivity {

    private TextView stateView;
    private TextView checkResultView;
    private TextView eventsView;
    private EditText numberInput;

    private final SimpleDateFormat dayTime =
            new SimpleDateFormat("dd.MM HH:mm", Locale.getDefault());
    private final SimpleDateFormat clock =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnostics);

        stateView = findViewById(R.id.textDiagState);
        checkResultView = findViewById(R.id.textCheckResult);
        eventsView = findViewById(R.id.textDiagEvents);
        numberInput = findViewById(R.id.inputNumber);

        findViewById(R.id.buttonCheck).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkNumber();
            }
        });

        findViewById(R.id.buttonClearHistory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DbHelper.get(DiagnosticsActivity.this).clearEvents();
                refresh();
            }
        });

        findViewById(R.id.buttonOpenRoleSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDefaultAppsSettings();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ContactIndex.invalidate();
        refresh();
    }

    private void refresh() {
        Settings settings = Settings.get(this);
        boolean hasRole = hasRole();
        boolean hasContacts = hasContactsPermission();
        DbHelper db = DbHelper.get(this);

        StringBuilder state = new StringBuilder();
        state.append(getString(hasRole ? R.string.diag_role_ok : R.string.diag_role_missing));
        state.append('\n');
        state.append(hasContacts
                ? getString(R.string.diag_contacts_ok, ContactIndex.count(this))
                : getString(R.string.diag_contacts_missing));
        state.append('\n');
        state.append(getString(settings.isEnabled() ? R.string.diag_filter_on : R.string.diag_filter_off));
        state.append('\n');
        state.append(settings.isPaused()
                ? getString(R.string.diag_pause_on, clock.format(new Date(settings.pausedUntil())))
                : getString(R.string.diag_pause_off));
        state.append('\n');
        state.append(settings.isScheduleEnabled()
                ? getString(R.string.diag_schedule_on,
                        ScheduleUi.describe(this, settings.scheduleDays(),
                                settings.scheduleStart(), settings.scheduleEnd()),
                        getString(settings.isScheduleActiveNow()
                                ? R.string.schedule_active_short
                                : R.string.schedule_inactive_short))
                : getString(R.string.diag_schedule_off));
        state.append('\n');
        state.append(getString(R.string.diag_counts, db.eventCount(), db.blockedCount()));
        stateView.setText(state.toString());

        List<CallEvent> events = db.events(20);
        if (events.isEmpty()) {
            eventsView.setText(R.string.diag_no_events);
        } else {
            StringBuilder text = new StringBuilder();
            for (CallEvent event : events) {
                if (text.length() > 0) {
                    text.append('\n');
                }
                String head = dayTime.format(new Date(event.time)) + " · "
                        + Numbers.display(this, event);
                text.append(getString(event.isBlocked() ? R.string.event_blocked
                        : R.string.event_allowed, head, getString(Reasons.labelRes(event.reason))));
            }
            eventsView.setText(text.toString());
        }
    }

    private void checkNumber() {
        String input = numberInput.getText().toString();
        String canonical = Numbers.canonical(this, input);
        if (canonical == null) {
            checkResultView.setText(R.string.whitelist_invalid);
            return;
        }

        Settings settings = Settings.get(this);
        boolean whitelisted = DbHelper.get(this).isWhitelisted(canonical);
        ContactIndex.Match match = ContactIndex.lookup(this, canonical);

        StringBuilder out = new StringBuilder();
        out.append(getString(R.string.diag_check_number, Numbers.format(this, canonical), canonical));
        out.append('\n');
        if (match.name == null) {
            out.append(getString(R.string.diag_check_no_contacts));
        } else if (match.name.isEmpty()) {
            out.append(getString(R.string.diag_check_contacts_noname));
        } else {
            out.append(getString(R.string.diag_check_contacts, match.name));
        }
        out.append('\n');
        out.append(getString(whitelisted ? R.string.diag_check_whitelist
                : R.string.diag_check_no_whitelist));
        out.append('\n');

        boolean wouldBlock = settings.isEnabled() && !settings.isPaused()
                && !whitelisted && match.name == null;
        out.append(getString(wouldBlock ? R.string.diag_would_block : R.string.diag_would_allow));
        checkResultView.setText(out.toString());
    }

    private boolean hasContactsPermission() {
        return checkSelfPermission(android.Manifest.permission.READ_CONTACTS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasRole() {
        RoleManager roleManager = getSystemService(RoleManager.class);
        return roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
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
