package com.callguard;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

/** Applies the language chosen inside the app, independent of the system locale. */
public final class LocaleHelper {

    public static final String SYSTEM = "system";

    private LocaleHelper() {
    }

    /** Returns a context with the chosen locale applied, or the same context for "system". */
    public static Context wrap(Context context) {
        String code = Settings.get(context).language();
        if (code == null || code.isEmpty() || SYSTEM.equals(code)) {
            return context;
        }
        Locale locale = new Locale(code);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }
}
