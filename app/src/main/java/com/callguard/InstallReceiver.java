package com.callguard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.widget.Toast;

/** Receives the result of a PackageInstaller session started by {@link UpdateManager}. */
public final class InstallReceiver extends BroadcastReceiver {

    public static final String ACTION_INSTALL = "com.callguard.INSTALL_RESULT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_SUCCESS) {
            Toast.makeText(context, R.string.install_success, Toast.LENGTH_SHORT).show();
        } else {
            String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            String detail = (message == null || message.trim().isEmpty()) ? "" : " " + message;
            Toast.makeText(context,
                    context.getString(R.string.install_failed, status, detail),
                    Toast.LENGTH_LONG).show();
        }
    }
}
