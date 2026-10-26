package com.callguard;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Journal: every screened call, grouped by day and filterable. */
public final class LogActivity extends LocalizedActivity {

    private static final int MAX_ROWS = 500;

    private static final int FILTER_ALL = 0;
    private static final int FILTER_BLOCKED = 1;
    private static final int FILTER_ALLOWED = 2;

    /** Mixed list: {@link String} day headers and {@link CallEvent} rows. */
    private final List<Object> rows = new ArrayList<>();

    private final SimpleDateFormat clock = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("d MMMM", Locale.getDefault());

    private LogAdapter adapter;
    private ListView list;
    private TextView emptyView;
    private TextView summaryView;
    private TextView filterAll;
    private TextView filterBlocked;
    private TextView filterAllowed;

    private int filter = FILTER_ALL;
    private int total;
    private int blockedTotal;
    private int allowedTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        list = findViewById(R.id.listLog);
        emptyView = findViewById(R.id.textLogEmpty);
        summaryView = findViewById(R.id.textLogSummary);
        filterAll = findViewById(R.id.filterAll);
        filterBlocked = findViewById(R.id.filterBlocked);
        filterAllowed = findViewById(R.id.filterAllowed);

        adapter = new LogAdapter();
        list.setAdapter(adapter);

        // Long press copies the number so it can be pasted into a search engine.
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                                           long id) {
                Object item = rows.get(position);
                if (!(item instanceof CallEvent)) {
                    return false;
                }
                copyNumber((CallEvent) item);
                return true;
            }
        });

        filterAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilter(FILTER_ALL);
            }
        });
        filterBlocked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilter(FILTER_BLOCKED);
            }
        });
        filterAllowed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilter(FILTER_ALLOWED);
            }
        });

        findViewById(R.id.buttonClear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmClear();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void copyNumber(CallEvent event) {
        String number = event.canonical != null
                ? Numbers.format(this, event.canonical)
                : (event.raw == null ? "" : event.raw);
        if (number.isEmpty()) {
            return;
        }
        ClipboardManager clipboard = getSystemService(ClipboardManager.class);
        if (clipboard == null) {
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.log_title), number));
        Toast.makeText(this, R.string.copy_number_done, Toast.LENGTH_SHORT).show();
    }

    private void setFilter(int value) {        if (filter == value) {
            return;
        }
        filter = value;
        reload();
    }

    private void reload() {
        List<CallEvent> all = DbHelper.get(this).events(MAX_ROWS);
        total = all.size();
        blockedTotal = 0;
        for (CallEvent event : all) {
            if (event.isBlocked()) {
                blockedTotal++;
            }
        }
        allowedTotal = total - blockedTotal;

        filterAll.setText(getString(R.string.log_filter_all, total));
        filterBlocked.setText(getString(R.string.log_filter_blocked, blockedTotal));
        filterAllowed.setText(getString(R.string.log_filter_allowed, allowedTotal));
        styleChip(filterAll, filter == FILTER_ALL);
        styleChip(filterBlocked, filter == FILTER_BLOCKED);
        styleChip(filterAllowed, filter == FILTER_ALLOWED);

        rows.clear();
        String group = null;
        for (CallEvent event : all) {
            if (filter == FILTER_BLOCKED && !event.isBlocked()) {
                continue;
            }
            if (filter == FILTER_ALLOWED && event.isBlocked()) {
                continue;
            }
            String label = groupLabel(event.time);
            if (!label.equals(group)) {
                rows.add(label);
                group = label;
            }
            rows.add(event);
        }

        adapter.notifyDataSetChanged();
        summaryView.setText(getString(R.string.log_summary, total) + " · "
                + getString(R.string.log_copy_hint));

        boolean isEmpty = rows.isEmpty();
        list.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        emptyView.setText(filter == FILTER_ALL ? R.string.log_empty : R.string.log_empty_filtered);
    }

    private void styleChip(TextView chip, boolean selected) {
        chip.setBackgroundTintList(selected
                ? ColorStateList.valueOf(getColor(R.color.accent)) : null);
        chip.setTextColor(getColor(selected ? android.R.color.white : R.color.text_primary));
    }

    private String groupLabel(long millis) {
        Calendar moment = Calendar.getInstance();
        moment.setTimeInMillis(millis);
        Calendar today = Calendar.getInstance();
        if (sameDay(moment, today)) {
            return getString(R.string.log_today);
        }
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (sameDay(moment, yesterday)) {
            return getString(R.string.log_yesterday);
        }
        return dayFormat.format(new Date(millis));
    }

    private static boolean sameDay(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.log_clear_confirm)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_clear, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DbHelper.get(LogActivity.this).clearEvents();
                        reload();
                    }
                })
                .show();
    }

    private void styleIcon(ImageView icon, CallEvent event) {
        int background;
        int foreground;
        int drawable;
        if (event.isBlocked()) {
            background = R.color.icon_red_bg;
            foreground = R.color.icon_red;
            drawable = R.drawable.ic_ban;
        } else if (Reasons.ALLOW_SCHEDULE.equals(event.reason)
                || Reasons.ALLOW_PAUSED.equals(event.reason)) {
            background = R.color.icon_amber_bg;
            foreground = R.color.icon_amber;
            drawable = R.drawable.ic_clock;
        } else {
            background = R.color.icon_green_bg;
            foreground = R.color.icon_green;
            drawable = R.drawable.ic_check;
        }
        icon.setBackgroundTintList(ColorStateList.valueOf(getColor(background)));
        icon.setImageResource(drawable);
        icon.setColorFilter(getColor(foreground));
    }

    private final class LogAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return rows.size();
        }

        @Override
        public Object getItem(int position) {
            return rows.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return rows.get(position) instanceof String ? 0 : 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(LogActivity.this);
            Object item = rows.get(position);

            if (item instanceof String) {
                View header = convertView;
                if (header == null) {
                    header = inflater.inflate(R.layout.item_log_header, parent, false);
                }
                ((TextView) header.findViewById(R.id.textLogHeader)).setText((String) item);
                return header;
            }

            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_log, parent, false);
            }
            CallEvent event = (CallEvent) item;

            TextView number = view.findViewById(R.id.textLogNumber);
            TextView meta = view.findViewById(R.id.textLogMeta);
            ImageView icon = view.findViewById(R.id.iconLog);
            View divider = view.findViewById(R.id.dividerLog);

            number.setText(Numbers.display(LogActivity.this, event));
            meta.setText(clock.format(new Date(event.time)) + " · "
                    + getString(Reasons.labelRes(event.reason)));
            styleIcon(icon, event);

            boolean last = position == rows.size() - 1;
            boolean nextIsHeader = !last && rows.get(position + 1) instanceof String;
            divider.setVisibility(last || nextIsHeader ? View.GONE : View.VISIBLE);
            return view;
        }
    }
}
