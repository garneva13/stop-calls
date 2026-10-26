package com.callguard;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/** Version, update check, manual APK install and support links. */
public final class AboutActivity extends LocalizedActivity {

    private static final int REQ_PICK_APK = 2001;

    private TextView textVersion;
    private TextView textUpdateStatus;
    private Button buttonCheck;
    private LinearLayout cardSupport;
    private Button buttonSupport;
    private Button buttonProject;
    private Button buttonLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        textVersion = findViewById(R.id.textVersion);
        textUpdateStatus = findViewById(R.id.textUpdateStatus);
        buttonCheck = findViewById(R.id.buttonCheck);
        cardSupport = findViewById(R.id.cardSupport);
        buttonSupport = findViewById(R.id.buttonSupport);
        buttonProject = findViewById(R.id.buttonProject);

        textVersion.setText(getString(R.string.about_version,
                UpdateManager.currentVersion(this)));

        buttonCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkForUpdates();
            }
        });

        findViewById(R.id.buttonInstallFile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickApk();
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

        findViewById(R.id.buttonBrowser).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl("https://github.com/" + getString(R.string.update_repo)
                        + "/releases/latest");
            }
        });
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

    private void checkForUpdates() {
        buttonCheck.setEnabled(false);
        textUpdateStatus.setText(R.string.about_checking);

        UpdateManager.checkAsync(this, new UpdateManager.CheckListener() {
            @Override
            public void onChecked(UpdateManager.Check check) {
                buttonCheck.setEnabled(true);
                switch (check.status) {
                    case UpdateManager.STATUS_AVAILABLE:
                        textUpdateStatus.setText(getString(R.string.about_available, check.version));
                        offerInstall(check);
                        break;
                    case UpdateManager.STATUS_UP_TO_DATE:
                        textUpdateStatus.setText(R.string.about_latest);
                        break;
                    case UpdateManager.STATUS_NOT_CONFIGURED:
                        textUpdateStatus.setText(R.string.about_not_configured);
                        break;
                    default:
                        textUpdateStatus.setText(getString(R.string.about_error,
                                check.error == null ? "" : check.error));
                        break;
                }
            }
        });
    }

    private void offerInstall(final UpdateManager.Check check) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setMessage(getString(R.string.about_available, check.version))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_update_install,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startDownload(check.apkUrl);
                            }
                        })
                .show();
    }

    private void startDownload(String apkUrl) {
        if (!ensureInstallPermission()) {
            return;
        }
        UpdateManager.downloadAndInstallAsync(this, apkUrl, statusListener());
    }

    private void pickApk() {
        if (!ensureInstallPermission()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.android.package-archive");
        try {
            startActivityForResult(intent, REQ_PICK_APK);
        } catch (RuntimeException error) {
            Toast.makeText(this, R.string.install_bad_file, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_APK || resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            return;
        }
        try {
            File file = copyToCache(uri);
            UpdateManager.installFileAsync(this, file, statusListener());
        } catch (Exception error) {
            Toast.makeText(this, R.string.install_bad_file, Toast.LENGTH_LONG).show();
        }
    }

    private File copyToCache(Uri uri) throws Exception {
        InputStream input = getContentResolver().openInputStream(uri);
        if (input == null) {
            throw new IllegalStateException("no stream");
        }
        File file = new File(getCacheDir(), ApkProvider.FILE_NAME);
        OutputStream output = new FileOutputStream(file);
        try {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) > 0) {
                output.write(buffer, 0, read);
            }
            output.flush();
        } finally {
            output.close();
            input.close();
        }
        return file;
    }

    private UpdateManager.StatusListener statusListener() {
        return new UpdateManager.StatusListener() {
            @Override
            public void onStatus(String message) {
                textUpdateStatus.setText(message);
            }

            @Override
            public void onError(String message) {
                textUpdateStatus.setText(getString(R.string.about_error, message));
            }
        };
    }

    private boolean ensureInstallPermission() {
        if (!UpdateManager.canInstallPackages(this)) {
            Toast.makeText(this, R.string.install_unknown_sources, Toast.LENGTH_LONG).show();
            try {
                startActivity(UpdateManager.unknownSourcesSettings(this));
            } catch (RuntimeException ignored) {
                // Some builds hide the screen; the toast still explains what to do.
            }
            return false;
        }
        return true;
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
