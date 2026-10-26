package com.callguard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Restores the status notification after a reboot or an app update, so the
 * indicator does not disappear while the filter keeps working.
 */
public final class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Indicator.update(context);
        }
    }
}
