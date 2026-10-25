package com.callguard;

import android.content.Context;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;

import java.util.Locale;

/**
 * Country-aware phone numbers on top of the pure {@link PhoneNumbers}.
 *
 * Android already ships libphonenumber through ICU, so {@link PhoneNumberUtils}
 * turns any number into E.164 once it knows the country. The country comes from
 * the SIM, then the network, then the device locale.
 */
public final class Numbers {

    private Numbers() {
    }

    /** ISO-3166 alpha-2 country of the device, never {@code null}. */
    public static String countryIso(Context context) {
        TelephonyManager manager = context.getSystemService(TelephonyManager.class);
        if (manager != null) {
            String iso = simCountry(manager);
            if (isEmpty(iso)) {
                iso = networkCountry(manager);
            }
            if (!isEmpty(iso)) {
                return iso.toUpperCase(Locale.US);
            }
        }
        String locale = Locale.getDefault().getCountry();
        return locale == null ? "" : locale.toUpperCase(Locale.US);
    }

    /**
     * Canonical digit string used for matching: E.164 without the plus sign
     * ("493012345678"), or the best local guess when the number cannot be parsed.
     */
    public static String canonical(Context context, String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String iso = countryIso(context);
        try {
            String e164 = PhoneNumberUtils.formatNumberToE164(trimmed, iso);
            if (e164 != null) {
                String digits = PhoneNumbers.digitsOnly(e164);
                if (digits.length() >= 5) {
                    return digits;
                }
            }
        } catch (RuntimeException ignored) {
            // Fall through to the local heuristics.
        }
        String countryCode = "RU".equals(iso) ? PhoneNumbers.DEFAULT_COUNTRY_CODE : "";
        return PhoneNumbers.canonical(raw, countryCode);
    }

    /** Pretty form for the UI, e.g. "+49 30 12345678". */
    public static String format(Context context, String canonical) {
        if (canonical == null || canonical.isEmpty()) {
            return "";
        }
        try {
            String pretty = PhoneNumberUtils.formatNumber("+" + canonical, countryIso(context));
            if (pretty != null && !pretty.isEmpty()) {
                return pretty;
            }
        } catch (RuntimeException ignored) {
            // Fall back to the simple formatter.
        }
        return PhoneNumbers.format(canonical);
    }

    /** Number to show for a logged call. */
    public static String display(Context context, CallEvent event) {
        if (event.canonical != null) {
            return format(context, event.canonical);
        }
        return event.raw == null ? "" : event.raw;
    }

    // ------------------------------------------------------------- internals

    private static String simCountry(TelephonyManager manager) {
        try {
            return manager.getSimCountryIso();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String networkCountry(TelephonyManager manager) {
        try {
            return manager.getNetworkCountryIso();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
