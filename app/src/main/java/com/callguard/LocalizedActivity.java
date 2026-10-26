package com.callguard;

import android.app.Activity;
import android.content.Context;

/**
 * Base activity that applies the language picked in the app before any view is
 * inflated, so every screen honours the in-app switch.
 */
public abstract class LocalizedActivity extends Activity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }
}
