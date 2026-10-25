package com.callguard;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Update check against GitHub Releases plus installation through the system
 * package installer.
 *
 * This is the only place in the app that touches the network: one HTTPS request
 * to api.github.com and one APK download from github.com.
 */
public final class UpdateManager {

    public static final int STATUS_AVAILABLE = 0;
    public static final int STATUS_UP_TO_DATE = 1;
    public static final int STATUS_NOT_CONFIGURED = 2;
    public static final int STATUS_ERROR = 3;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private UpdateManager() {
    }

    /** Result of a version check. */
    public static final class Check {
        public final int status;
        public final String version;
        public final String apkUrl;
        public final String error;

        private Check(int status, String version, String apkUrl, String error) {
            this.status = status;
            this.version = version;
            this.apkUrl = apkUrl;
            this.error = error;
        }
    }

    public interface CheckListener {
        void onChecked(Check check);
    }

    public interface StatusListener {
        void onStatus(String message);

        void onError(String message);
    }

    public static String currentVersion(Context context) {
        try {
            String version = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
            return version == null ? "?" : version;
        } catch (PackageManager.NameNotFoundException e) {
            return "?";
        }
    }

    public static boolean isConfigured(Context context) {
        String repo = context.getString(R.string.update_repo);
        return repo != null && repo.contains("/") && !repo.startsWith("OWNER");
    }

    public static void checkAsync(final Context context, final CheckListener listener) {
        final Context app = context.getApplicationContext();
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                Check check;
                try {
                    check = fetchLatest(app);
                } catch (Exception error) {
                    String message = error.getMessage();
                    check = new Check(STATUS_ERROR, null, null,
                            message == null ? error.getClass().getSimpleName() : message);
                }
                final Check result = check;
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onChecked(result);
                    }
                });
            }
        });
    }

    public static void downloadAndInstallAsync(final Context context, final String apkUrl,
                                               final StatusListener listener) {
        final Context app = context.getApplicationContext();
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    postStatus(listener, app.getString(R.string.about_downloading));
                    File file = new File(app.getCacheDir(), ApkProvider.FILE_NAME);
                    download(apkUrl, file);
                    postStatus(listener, app.getString(R.string.about_installing));
                    install(app, file);
                } catch (final Exception error) {
                    final String message = error.getMessage() == null
                            ? error.getClass().getSimpleName() : error.getMessage();
                    MAIN.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onError(message);
                        }
                    });
                }
            }
        });
    }

    /** Copies an already picked APK into the cache and hands it to the installer. */
    public static void installFileAsync(final Context context, final File file,
                                        final StatusListener listener) {
        final Context app = context.getApplicationContext();
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    postStatus(listener, app.getString(R.string.about_installing));
                    install(app, file);
                } catch (final Exception error) {
                    final String message = error.getMessage() == null
                            ? error.getClass().getSimpleName() : error.getMessage();
                    MAIN.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onError(message);
                        }
                    });
                }
            }
        });
    }

    public static boolean canInstallPackages(Context context) {
        return context.getPackageManager().canRequestPackageInstalls();
    }

    public static Intent unknownSourcesSettings(Context context) {
        return new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                android.net.Uri.parse("package:" + context.getPackageName()));
    }

    // ------------------------------------------------------------- internals

    private static void postStatus(final StatusListener listener, final String message) {
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                listener.onStatus(message);
            }
        });
    }

    private static Check fetchLatest(Context context) throws Exception {
        if (!isConfigured(context)) {
            return new Check(STATUS_NOT_CONFIGURED, null, null, null);
        }
        String repo = context.getString(R.string.update_repo);
        String body = readUrl("https://api.github.com/repos/" + repo + "/releases/latest");
        JSONObject release = new JSONObject(body);
        String tag = release.optString("tag_name", "");

        String apkUrl = null;
        JSONArray assets = release.optJSONArray("assets");
        if (assets != null) {
            for (int index = 0; index < assets.length(); index++) {
                JSONObject asset = assets.getJSONObject(index);
                String name = asset.optString("name", "");
                if (name.toLowerCase(Locale.US).endsWith(".apk")) {
                    apkUrl = asset.optString("browser_download_url", null);
                    break;
                }
            }
        }
        if (apkUrl == null || apkUrl.isEmpty()) {
            throw new IllegalStateException("в последнем релизе нет APK");
        }

        String latest = clean(tag);
        String current = clean(currentVersion(context));
        return compare(latest, current) > 0
                ? new Check(STATUS_AVAILABLE, latest, apkUrl, null)
                : new Check(STATUS_UP_TO_DATE, latest, apkUrl, null);
    }

    private static String readUrl(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("User-Agent", "CallGuard-Android");
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        try {
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("HTTP " + code);
            }
            InputStream input = connection.getInputStream();
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) > 0) {
                    output.write(buffer, 0, read);
                }
                return output.toString("UTF-8");
            } finally {
                input.close();
            }
        } finally {
            connection.disconnect();
        }
    }

    private static void download(String address, File target) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("User-Agent", "CallGuard-Android");
        try {
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("HTTP " + code);
            }
            InputStream input = connection.getInputStream();
            FileOutputStream output = new FileOutputStream(target);
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
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Hands the APK to the system installer; the user still confirms the update.
     *
     * Two paths exist. The primary one mirrors a file-manager install: a
     * content:// URI opened with ACTION_VIEW. The PackageInstaller session is
     * kept as a fallback for devices without an activity for that intent.
     */
    private static void install(Context context, File apk) throws Exception {
        if (apk == null || !apk.exists() || apk.length() == 0) {
            throw new IllegalStateException(context.getString(R.string.install_bad_file));
        }
        File target = new File(context.getCacheDir(), ApkProvider.FILE_NAME);
        if (!target.equals(apk)) {
            copy(apk, target);
        }
        if (launchInstaller(context, target)) {
            return;
        }
        installWithSession(context, target);
    }

    private static boolean launchInstaller(Context context, File apk) {
        try {
            android.net.Uri uri = android.net.Uri.parse(
                    "content://" + ApkProvider.AUTHORITY + "/" + apk.getName());
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }

    private static void copy(File from, File to) throws Exception {
        InputStream input = new java.io.FileInputStream(from);
        FileOutputStream output = new FileOutputStream(to);
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
    }

    private static void installWithSession(Context context, File apk) throws Exception {
        PackageInstaller installer = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        int sessionId = installer.createSession(params);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, sessionId,
                new Intent(context, InstallReceiver.class).setAction(InstallReceiver.ACTION_INSTALL),
                flags);

        PackageInstaller.Session session = installer.openSession(sessionId);
        try {
            OutputStream output = session.openWrite("base.apk", 0, apk.length());
            try {
                InputStream input = new java.io.FileInputStream(apk);
                try {
                    byte[] buffer = new byte[64 * 1024];
                    int read;
                    while ((read = input.read(buffer)) > 0) {
                        output.write(buffer, 0, read);
                    }
                } finally {
                    input.close();
                }
                session.fsync(output);
            } finally {
                output.close();
            }
            session.commit(pendingIntent.getIntentSender());
        } finally {
            session.close();
        }
    }

    private static String clean(String version) {
        if (version == null) {
            return "0";
        }
        String value = version.trim();
        if (value.startsWith("v") || value.startsWith("V")) {
            value = value.substring(1);
        }
        int dash = value.indexOf('-');
        return dash > 0 ? value.substring(0, dash) : value;
    }

    /** Compares dotted versions; returns positive when {@code left} is newer. */
    static int compare(String left, String right) {
        String[] a = left.split("\\.");
        String[] b = right.split("\\.");
        int count = Math.max(a.length, b.length);
        for (int index = 0; index < count; index++) {
            int first = index < a.length ? number(a[index]) : 0;
            int second = index < b.length ? number(b[index]) : 0;
            if (first != second) {
                return first - second;
            }
        }
        return 0;
    }

    private static int number(String value) {
        StringBuilder digits = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (ch >= '0' && ch <= '9') {
                digits.append(ch);
            } else {
                break;
            }
        }
        if (digits.length() == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException error) {
            return 0;
        }
    }
}
