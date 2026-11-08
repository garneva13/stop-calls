package com.callguard;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Checks the latest GitHub release. It only reports whether a newer version
 * exists — downloading and installing stay the user's action in the browser.
 *
 * A failure is classified, because "no internet", "the proxy answered 502" and
 * "the release is missing" need different words for the user: the first two are
 * retried later, and in all cases the browser page stays the fallback.
 */
public final class UpdateChecker {

    public static final int STATUS_AVAILABLE = 0;
    public static final int STATUS_UP_TO_DATE = 1;
    public static final int STATUS_ERROR = 2;

    /** Why the check failed; only meaningful when status is STATUS_ERROR. */
    public static final int KIND_NONE = 0;
    public static final int KIND_NETWORK = 1;
    public static final int KIND_SERVER = 2;
    public static final int KIND_RATE_LIMIT = 3;
    public static final int KIND_NOT_FOUND = 4;
    public static final int KIND_OTHER = 5;

    private static final String API =
            "https://api.github.com/repos/garneva13/stop-calls/releases/latest";
    private static final int TIMEOUT_MS = 10000;

    public interface Listener {
        void onResult(Result result);
    }

    public static final class Result {
        public final int status;
        public final int kind;
        public final int httpCode;
        public final String version;
        public final String detail;

        private Result(int status, int kind, int httpCode, String version, String detail) {
            this.status = status;
            this.kind = kind;
            this.httpCode = httpCode;
            this.version = version;
            this.detail = detail;
        }
    }

    private UpdateChecker() {
    }

    public static void checkAsync(final Context context, final Listener listener) {
        final Context app = context.getApplicationContext();
        final Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Result result = check(app);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onResult(result);
                    }
                });
            }
        }, "update-check").start();
    }

    public static String currentVersion(Context context) {
        try {
            String version = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
            return version == null ? "" : version;
        } catch (PackageManager.NameNotFoundException error) {
            return "";
        }
    }

    private static Result check(Context context) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(API).openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", "CallGuard");
            int code = connection.getResponseCode();
            if (code != 200) {
                return failure(kindForCode(code), code, "HTTP " + code);
            }
            String body = read(connection);
            String tag = new JSONObject(body).optString("tag_name", "");
            String version = tag.replaceFirst("^[vV]", "").trim();
            if (version.isEmpty()) {
                return failure(KIND_OTHER, 200, "no tag_name");
            }
            String current = currentVersion(context);
            int comparison = compare(version, current);
            int status = comparison > 0 ? STATUS_AVAILABLE : STATUS_UP_TO_DATE;
            return new Result(status, KIND_NONE, 200, version, null);
        } catch (IOException network) {
            // No connection, DNS failure, timeout, or TLS reset by a proxy/DPI box.
            String message = network.getMessage();
            return failure(KIND_NETWORK, 0, message == null ? network.toString() : message);
        } catch (Exception other) {
            String message = other.getMessage();
            return failure(KIND_OTHER, 0, message == null ? other.toString() : message);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static int kindForCode(int code) {
        if (code == HttpURLConnection.HTTP_NOT_FOUND) {
            return KIND_NOT_FOUND;
        }
        if (code == HttpURLConnection.HTTP_FORBIDDEN || code == 429) {
            return KIND_RATE_LIMIT;
        }
        if (code >= 500) {
            return KIND_SERVER;
        }
        return KIND_OTHER;
    }

    private static Result failure(int kind, int httpCode, String detail) {
        return new Result(STATUS_ERROR, kind, httpCode, null, detail);
    }

    private static String read(HttpURLConnection connection) throws Exception {
        StringBuilder text = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line);
            }
        } finally {
            reader.close();
        }
        return text.toString();
    }

    /** Compares dotted versions numerically: "1.13" > "1.9". */
    static int compare(String first, String second) {
        String[] a = first.split("[^0-9]+");
        String[] b = second.split("[^0-9]+");
        int length = Math.max(a.length, b.length);
        for (int index = 0; index < length; index++) {
            int left = index < a.length ? parse(a[index]) : 0;
            int right = index < b.length ? parse(b[index]) : 0;
            if (left != right) {
                return left > right ? 1 : -1;
            }
        }
        return 0;
    }

    private static int parse(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
