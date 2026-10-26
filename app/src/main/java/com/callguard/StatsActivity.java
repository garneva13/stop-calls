package com.callguard;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Blocked-call statistics: total, trend and two charts. */
public final class StatsActivity extends LocalizedActivity {

    private static final int PERIOD_DAY = 0;
    private static final int PERIOD_WEEK = 1;
    private static final int PERIOD_MONTH = 2;
    private static final int PERIOD_YEAR = 3;

    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private TextView periodDay;
    private TextView periodWeek;
    private TextView periodMonth;
    private TextView periodYear;
    private TextView textTotal;
    private TextView textDelta;
    private TextView textDeltaSub;
    private TextView textEmpty;
    private View timeCard;
    private BarChartView chart;
    private BarChartView timeChart;

    private int period = PERIOD_WEEK;
    private final List<CallEvent> events = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        periodDay = findViewById(R.id.periodDay);
        periodWeek = findViewById(R.id.periodWeek);
        periodMonth = findViewById(R.id.periodMonth);
        periodYear = findViewById(R.id.periodYear);
        textTotal = findViewById(R.id.textTotal);
        textDelta = findViewById(R.id.textDelta);
        textDeltaSub = findViewById(R.id.textDeltaSub);
        textEmpty = findViewById(R.id.textEmpty);
        timeCard = findViewById(R.id.timeCard);
        chart = findViewById(R.id.chart);
        timeChart = findViewById(R.id.timeChart);

        periodDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPeriod(PERIOD_DAY);
            }
        });
        periodWeek.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPeriod(PERIOD_WEEK);
            }
        });
        periodMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPeriod(PERIOD_MONTH);
            }
        });
        periodYear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPeriod(PERIOD_YEAR);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        events.clear();
        events.addAll(DbHelper.get(this).events(1000));
        refresh();
    }

    private void setPeriod(int value) {
        if (period == value) {
            return;
        }
        period = value;
        refresh();
    }

    private void refresh() {
        styleSegment(periodDay, period == PERIOD_DAY);
        styleSegment(periodWeek, period == PERIOD_WEEK);
        styleSegment(periodMonth, period == PERIOD_MONTH);
        styleSegment(periodYear, period == PERIOD_YEAR);

        Data data = build();

        textTotal.setText(String.valueOf(data.total));
        bindTrend(data);
        chart.setData(data.counts, data.labels, true);
        textEmpty.setVisibility(data.total == 0 ? View.VISIBLE : View.GONE);

        boolean showTime = period != PERIOD_DAY;
        timeCard.setVisibility(showTime ? View.VISIBLE : View.GONE);
        if (showTime) {
            timeChart.setData(data.hours, hourLabels(), false);
        }
    }

    private void bindTrend(Data data) {
        if (data.previous <= 0) {
            textDelta.setVisibility(View.GONE);
            textDeltaSub.setVisibility(View.GONE);
            return;
        }
        textDelta.setVisibility(View.VISIBLE);
        textDeltaSub.setVisibility(View.VISIBLE);

        int percent = Math.round((data.total - data.previous) * 100f / data.previous);
        if (percent > 0) {
            textDelta.setText(getString(R.string.stats_delta_up, percent));
            textDelta.setTextColor(getColor(R.color.chart_bad));
        } else if (percent < 0) {
            textDelta.setText(getString(R.string.stats_delta_down, -percent));
            textDelta.setTextColor(getColor(R.color.chart_good));
        } else {
            textDelta.setText(getString(R.string.stats_delta_up, 0));
            textDelta.setTextColor(getColor(R.color.text_secondary));
        }
        textDeltaSub.setText(comparisonLabel());
    }

    private int comparisonLabel() {
        switch (period) {
            case PERIOD_DAY:
                return R.string.stats_vs_day;
            case PERIOD_MONTH:
                return R.string.stats_vs_month;
            case PERIOD_YEAR:
                return R.string.stats_vs_year;
            default:
                return R.string.stats_vs_week;
        }
    }

    private void styleSegment(TextView segment, boolean selected) {
        if (selected) {
            segment.setBackgroundResource(R.drawable.bg_segment_active);
            segment.setTextColor(getColor(android.R.color.white));
        } else {
            segment.setBackground(null);
            segment.setTextColor(getColor(R.color.text_secondary));
        }
    }

    // ------------------------------------------------------------- data

    private Data build() {
        Calendar now = Calendar.getInstance();
        switch (period) {
            case PERIOD_DAY:
                return buildDay(now);
            case PERIOD_MONTH:
                return buildDays(now, 30);
            case PERIOD_YEAR:
                return buildYear(now);
            default:
                return buildDays(now, 7);
        }
    }

    private Data buildDay(Calendar now) {
        Calendar start = startOfDay(now);
        long startMillis = start.getTimeInMillis();
        long endMillis = startMillis + DAY_MS;

        int[] counts = new int[24];
        int[] previous = new int[24];
        int total = 0;
        int previousTotal = 0;
        for (CallEvent event : events) {
            if (!event.isBlocked()) {
                continue;
            }
            Calendar moment = Calendar.getInstance();
            moment.setTimeInMillis(event.time);
            if (event.time >= startMillis && event.time < endMillis) {
                counts[moment.get(Calendar.HOUR_OF_DAY)]++;
                total++;
            } else if (event.time >= startMillis - DAY_MS && event.time < startMillis) {
                previous[moment.get(Calendar.HOUR_OF_DAY)]++;
                previousTotal++;
            }
        }

        String[] labels = new String[24];
        for (int hour = 0; hour < 24; hour++) {
            labels[hour] = hour % 6 == 0
                    ? String.format(Locale.getDefault(), "%02d:00", hour) : null;
        }

        Data data = new Data();
        data.counts = counts;
        data.labels = labels;
        data.total = total;
        data.previous = previousTotal;
        data.hours = counts;
        return data;
    }

    private Data buildDays(Calendar now, int days) {
        Calendar start = startOfDay(now);
        start.add(Calendar.DAY_OF_YEAR, -(days - 1));
        long startMillis = start.getTimeInMillis();
        long endMillis = startOfDay(now).getTimeInMillis() + DAY_MS;

        int[] counts = new int[days];
        int[] hours = new int[24];
        int total = 0;
        int previousTotal = 0;
        long previousStart = startMillis - days * DAY_MS;

        for (CallEvent event : events) {
            if (!event.isBlocked()) {
                continue;
            }
            if (event.time >= startMillis && event.time < endMillis) {
                int index = (int) ((event.time - startMillis) / DAY_MS);
                if (index >= 0 && index < days) {
                    counts[index]++;
                    total++;
                }
                Calendar moment = Calendar.getInstance();
                moment.setTimeInMillis(event.time);
                hours[moment.get(Calendar.HOUR_OF_DAY)]++;
            } else if (event.time >= previousStart && event.time < startMillis) {
                previousTotal++;
            }
        }

        SimpleDateFormat weekday = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat day = new SimpleDateFormat("d MMM", Locale.getDefault());
        String[] labels = new String[days];
        int every = days <= 7 ? 1 : 5;
        for (int index = 0; index < days; index++) {
            Calendar moment = Calendar.getInstance();
            moment.setTimeInMillis(startMillis + index * DAY_MS);
            boolean show = index % every == 0 || index == days - 1;
            if (!show) {
                labels[index] = null;
            } else if (days <= 7) {
                labels[index] = capitalize(weekday.format(moment.getTime()))
                        + "\n" + day.format(moment.getTime());
            } else {
                labels[index] = day.format(moment.getTime());
            }
        }

        Data data = new Data();
        data.counts = counts;
        data.labels = labels;
        data.total = total;
        data.previous = previousTotal;
        data.hours = hours;
        return data;
    }

    private Data buildYear(Calendar now) {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(now.getTimeInMillis());
        start.add(Calendar.MONTH, -11);
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar previousStart = Calendar.getInstance();
        previousStart.setTimeInMillis(start.getTimeInMillis());
        previousStart.add(Calendar.MONTH, -12);

        int[] counts = new int[12];
        int[] hours = new int[24];
        int total = 0;
        int previousTotal = 0;

        for (CallEvent event : events) {
            if (!event.isBlocked()) {
                continue;
            }
            Calendar moment = Calendar.getInstance();
            moment.setTimeInMillis(event.time);
            int index = (moment.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12
                    + moment.get(Calendar.MONTH) - start.get(Calendar.MONTH);
            if (index >= 0 && index < 12) {
                counts[index]++;
                total++;
                hours[moment.get(Calendar.HOUR_OF_DAY)]++;
            } else if (index < 0 && moment.getTimeInMillis() >= previousStart.getTimeInMillis()) {
                previousTotal++;
            }
        }

        SimpleDateFormat month = new SimpleDateFormat("LLL", Locale.getDefault());
        String[] labels = new String[12];
        for (int index = 0; index < 12; index++) {
            Calendar moment = Calendar.getInstance();
            moment.setTimeInMillis(start.getTimeInMillis());
            moment.add(Calendar.MONTH, index);
            labels[index] = month.format(new Date(moment.getTimeInMillis()));
        }

        Data data = new Data();
        data.counts = counts;
        data.labels = labels;
        data.total = total;
        data.previous = previousTotal;
        data.hours = hours;
        return data;
    }

    private String[] hourLabels() {
        String[] labels = new String[24];
        for (int hour = 0; hour < 24; hour++) {
            labels[hour] = hour % 6 == 0
                    ? String.format(Locale.getDefault(), "%02d:00", hour) : null;
        }
        labels[23] = "24:00";
        return labels;
    }

    private static Calendar startOfDay(Calendar source) {
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(source.getTimeInMillis());
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        return day;
    }

    private static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.getDefault()) + value.substring(1);
    }

    private static final class Data {
        int[] counts;
        String[] labels;
        int[] hours;
        int total;
        int previous;
    }
}
