package com.callguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Numbers that are always allowed through, even when the filter is on. */
public final class WhitelistActivity extends Activity {

    private final List<WhitelistEntry> items = new ArrayList<>();

    private WhitelistAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whitelist);

        ListView list = findViewById(R.id.listWhitelist);
        TextView emptyView = findViewById(R.id.textWhitelistEmpty);

        adapter = new WhitelistAdapter();
        list.setAdapter(adapter);
        list.setEmptyView(emptyView);

        findViewById(R.id.buttonAdd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddDialog();
            }
        });

        reload();
    }

    private void reload() {
        items.clear();
        items.addAll(DbHelper.get(this).whitelist());
        adapter.notifyDataSetChanged();
    }

    private void showAddDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setHint(R.string.whitelist_add_hint);
        input.setSingleLine(true);

        int padding = Math.round(getResources().getDisplayMetrics().density * 20f);
        FrameLayout container = new FrameLayout(this);
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(input, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.whitelist_add)
                .setView(container)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_add, null)
                .create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        input.requestFocus();

        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String canonical = PhoneNumbers.canonical(input.getText().toString());
                if (canonical == null) {
                    Toast.makeText(WhitelistActivity.this, R.string.whitelist_invalid,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                DbHelper.get(WhitelistActivity.this).addWhitelisted(
                        canonical, PhoneNumbers.format(canonical));
                Toast.makeText(WhitelistActivity.this, R.string.whitelist_added,
                        Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                reload();
            }
        });
    }

    private void confirmDelete(final WhitelistEntry entry) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.whitelist_delete_confirm, entry.display))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DbHelper.get(WhitelistActivity.this).removeWhitelisted(entry.canonical);
                        Toast.makeText(WhitelistActivity.this, R.string.whitelist_removed,
                                Toast.LENGTH_SHORT).show();
                        reload();
                    }
                })
                .show();
    }

    private final class WhitelistAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public WhitelistEntry getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(WhitelistActivity.this)
                        .inflate(R.layout.item_whitelist, parent, false);
            }
            final WhitelistEntry item = getItem(position);
            TextView number = view.findViewById(R.id.textWhitelistNumber);
            number.setText(item.display);
            view.findViewById(R.id.buttonWhitelistDelete).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            confirmDelete(item);
                        }
                    });
            return view;
        }
    }
}
