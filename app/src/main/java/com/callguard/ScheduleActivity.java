package com.callguard;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

/** Weekly schedule during which the filter lets unknown calls through. */
public final class ScheduleActivity extends Activity {

    private Switch switchSchedule;
    private LinearLayout dayContainer;
    private Button buttonStart;
    private Button buttonEnd;
    private TextView textSummary;
    private TextView textNow;

    private final TextView[] chips = new TextView[Schedule.DAYS_COUNT];

    /** Guards the compound-button listener while the UI is being populated. */
    private boolean binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        switchSchedule = findViewById(R.id.switchSchedule);
        dayContainer = findViewById(R.id.layoutDays);
        buttonStart = findViewById(R.id.buttonStart);
        buttonEnd = findViewById(R.id.buttonEnd);
        textSummary = findViewById(R.id.textScheduleSummary);
        textNow = findViewById(R.id.textScheduleNow);

        switchSchedule.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (binding) {
                    return;
                }
                Settings.get(ScheduleActivity.this).setScheduleEnabled(isChecked);
                refresh();
            }
        });

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickTime(true);
            }
        });

        buttonEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickTime(false);
            }
        });

        buildChips();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void buildChips() {
        String[] names = getResources().getStringArray(R.array.schedule_day_names);
        for (int i = 0; i < Schedule.DAYS_COUNT; i++) {
            final int index = i;
            TextView chip = new TextView(this);
            chip.setText(names[i]);
            chip.setGravity(Gravity.CENTER);
            chip.setTextSize(13f);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleDay(index);
                }
            });
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(0, dp(40), 1f);
            params.setMarginEnd(dp(4));
            chip.setLayoutParams(params);
            dayContainer.addView(chip);
            chips[i] = chip;
        }
    }

    private void toggleDay(int index) {
        Settings settings = Settings.get(this);
        int mask = settings.scheduleDays();
        settings.setScheduleDays(
                Schedule.withDay(mask, index, !Schedule.isDaySelected(mask, index)));
        refresh();
    }

    private void pickTime(final boolean isStart) {
        Settings settings = Settings.get(this);
        int minutes = isStart ? settings.scheduleStart() : settings.scheduleEnd();
        TimePickerDialog dialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        Settings current = Settings.get(ScheduleActivity.this);
                        if (isStart) {
                            current.setScheduleStart(hourOfDay * 60 + minute);
                        } else {
                            current.setScheduleEnd(hourOfDay * 60 + minute);
                        }
                        refresh();
                    }
                }, minutes / 60, minutes % 60, true);
        dialog.show();
    }

    private void refresh() {
        Settings settings = Settings.get(this);
        binding = true;
        switchSchedule.setChecked(settings.isScheduleEnabled());
        binding = false;

        buttonStart.setText(Schedule.time(settings.scheduleStart()));
        buttonEnd.setText(Schedule.time(settings.scheduleEnd()));

        int mask = settings.scheduleDays();
        updateChips(mask);

        textSummary.setText(getString(R.string.schedule_summary,
                ScheduleUi.describe(this, mask, settings.scheduleStart(), settings.scheduleEnd())));
        textNow.setText(settings.isScheduleActiveNow()
                ? R.string.schedule_now_active : R.string.schedule_now_inactive);
    }

    private void updateChips(int mask) {
        for (int i = 0; i < Schedule.DAYS_COUNT; i++) {
            boolean selected = Schedule.isDaySelected(mask, i);
            GradientDrawable background = new GradientDrawable();
            background.setCornerRadius(dp(12));
            background.setColor(selected ? getColor(R.color.accent) : getColor(R.color.chip_bg));
            chips[i].setBackground(background);
            chips[i].setTextColor(selected ? Color.WHITE : getColor(R.color.text_secondary));
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
