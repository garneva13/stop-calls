package com.callguard;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Version, language switch and links to the project. */
public final class AboutActivity extends LocalizedActivity {

    private LinearLayout cardSupport;
    private Button buttonSupport;
    private Button buttonProject;
    private Button buttonLanguage;
    private Button buttonCheck;
    private Button buttonDownload;
    private TextView textUpdateStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView textVersion = findViewById(R.id.textVersion);
        cardSupport = findViewById(R.id.cardSupport);
        buttonSupport = findViewById(R.id.buttonSupport);
        buttonProject = findViewById(R.id.buttonProject);
        buttonCheck = findViewById(R.id.buttonCheck);
        buttonDownload = findViewById(R.id.buttonDownload);
        textUpdateStatus = findViewById(R.id.textUpdateStatus);

        textVersion.setText(getString(R.string.about_version, currentVersion()));

        buttonCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkForUpdates();
            }
        });
        buttonDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Installation stays in the browser: no package-install permission in the app.
                openUrl(getString(R.string.releases_url));
            }
        });

        bindSupport();

        buttonLanguage = findViewById(R.id.buttonLanguage);
        buttonLanguage.setText(languageLabel());
        buttonLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLanguageDialog();
            }
        });
    }

    private void checkForUpdates() {
        buttonCheck.setEnabled(false);
        buttonDownload.setVisibility(View.GONE);
        textUpdateStatus.setText(R.string.about_checking);

        UpdateChecker.checkAsync(this, new UpdateChecker.Listener() {
            @Override
            public void onResult(UpdateChecker.Result result) {
                buttonCheck.setEnabled(true);
                switch (result.status) {
                    case UpdateChecker.STATUS_AVAILABLE:
                        textUpdateStatus.setText(getString(R.string.about_available, result.version));
                        buttonDownload.setText(R.string.about_download);
                        buttonDownload.setVisibility(View.VISIBLE);
                        break;
                    case UpdateChecker.STATUS_UP_TO_DATE:
                        textUpdateStatus.setText(R.string.about_latest);
                        break;
                    default:
                        textUpdateStatus.setText(errorMessage(result));
                        // The browser page is the fallback for every failure, so the
                        // button stays available even when the API check did not work.
                        buttonDownload.setText(R.string.about_open_releases);
                        buttonDownload.setVisibility(View.VISIBLE);
                        break;
                }
            }
        });
    }

    private String errorMessage(UpdateChecker.Result result) {
        switch (result.kind) {
            case UpdateChecker.KIND_NETWORK:
                return getString(R.string.about_error_network);
            case UpdateChecker.KIND_SERVER:
                return getString(R.string.about_error_server, result.httpCode);
            case UpdateChecker.KIND_RATE_LIMIT:
                return getString(R.string.about_error_rate);
            case UpdateChecker.KIND_NOT_FOUND:
                return getString(R.string.about_error_not_found);
            default:
                if (result.httpCode >= 400) {
                    return getString(R.string.about_error_other, result.httpCode);
                }
                return getString(R.string.about_error_unknown);
        }
    }

    private String currentVersion() {
        try {
            String version = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            return version == null ? "" : version;
        } catch (PackageManager.NameNotFoundException error) {
            return "";
        }
    }

    private void bindSupport() {
        final String projectUrl = getString(R.string.project_url);
        final String supportUrl = getString(R.string.support_url);
        boolean hasProject = projectUrl != null && !projectUrl.isEmpty();
        boolean hasSupport = supportUrl != null && !supportUrl.isEmpty();

        cardSupport.setVisibility(hasProject || hasSupport ? View.VISIBLE : View.GONE);
        buttonProject.setVisibility(hasProject ? View.VISIBLE : View.GONE);
        buttonSupport.setVisibility(hasSupport ? View.VISIBLE : View.GONE);

        buttonProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl(projectUrl);
            }
        });
        buttonSupport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl(supportUrl);
            }
        });
    }

    private String languageLabel() {
        String code = Settings.get(this).language();
        if ("ru".equals(code)) {
            return getString(R.string.language_ru);
        }
        if ("en".equals(code)) {
            return getString(R.string.language_en);
        }
        return getString(R.string.language_system);
    }

    private void showLanguageDialog() {
        final String[] codes = {LocaleHelper.SYSTEM, "ru", "en"};
        final String[] labels = {
                getString(R.string.language_system),
                getString(R.string.language_ru),
                getString(R.string.language_en)
        };
        String current = Settings.get(this).language();
        int checked = 0;
        for (int index = 0; index < codes.length; index++) {
            if (codes[index].equals(current)) {
                checked = index;
                break;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.language_title)
                .setSingleChoiceItems(labels, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Settings.get(AboutActivity.this).setLanguage(codes[which]);
                        dialog.dismiss();
                        restartApp();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /** Recreates the task so every screen picks up the new language. */
    private void restartApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity();
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (RuntimeException ignored) {
            Toast.makeText(this, R.string.toast_no_browser, Toast.LENGTH_SHORT).show();
        }
    }
}
