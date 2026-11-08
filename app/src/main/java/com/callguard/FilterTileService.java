package com.callguard;

import android.app.PendingIntent;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

/** Quick Settings tile: toggles the filter without opening the app. */
public final class FilterTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        Context localized = LocaleHelper.wrap(this);
        Settings settings = Settings.get(this);
        if (!hasRole(this)) {
            // The filter cannot work without the screening role; send the user to the app.
            Toast.makeText(this, localized.getString(R.string.qs_role_missing), Toast.LENGTH_LONG)
                    .show();
            openApp();
            return;
        }
        if (!ContactIndex.hasAccess(this)) {
            // Toggling would be meaningless: with no address book every call is let
            // through whatever the switch says. Send the user where it can be fixed.
            Toast.makeText(this, localized.getString(R.string.qs_no_contacts_missing),
                    Toast.LENGTH_LONG).show();
            openApp();
            return;
        }
        boolean enabled = !settings.isEnabled();
        settings.setEnabled(enabled);
        Indicator.update(this);
        updateTile();
        Toast.makeText(this, localized.getString(enabled
                        ? R.string.qs_toast_on : R.string.qs_toast_off), Toast.LENGTH_SHORT)
                .show();
    }

    /** Asks the system to refresh the tile after the state changed elsewhere. */
    public static void requestRefresh(Context context) {
        try {
            TileService.requestListeningState(context,
                    new ComponentName(context, FilterTileService.class));
        } catch (RuntimeException ignored) {
            // The tile is not available on this build; nothing to refresh.
        }
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        Settings settings = Settings.get(this);
        Context localized = LocaleHelper.wrap(this);
        boolean hasRole = hasRole(this);
        boolean hasContacts = ContactIndex.hasAccess(this);
        // Without the address book the filter cannot reject anything, so the tile must
        // not show itself as active.
        boolean enabled = settings.isEnabled() && hasRole && hasContacts;
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_tile_shield));
        tile.setLabel(localized.getString(R.string.qs_label));
        tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int subtitle;
            if (!hasRole) {
                subtitle = R.string.qs_role;
            } else if (!hasContacts) {
                subtitle = R.string.qs_no_contacts;
            } else {
                subtitle = settings.isEnabled() ? R.string.qs_on : R.string.qs_off;
            }
            tile.setSubtitle(localized.getString(subtitle));
        }
        tile.updateTile();
    }

    private void openApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 34) {
            PendingIntent pending = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            startActivityAndCollapse(pending);
        } else {
            startActivityAndCollapse(intent);
        }
    }

    private static boolean hasRole(Context context) {
        RoleManager roleManager = context.getSystemService(RoleManager.class);
        return roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
    }
}
