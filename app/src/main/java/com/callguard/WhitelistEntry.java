package com.callguard;

/** A number the user allowed manually even though it is not in the contacts. */
public final class WhitelistEntry {

    public final String canonical;
    public final String display;

    public WhitelistEntry(String canonical, String display) {
        this.canonical = canonical;
        this.display = display;
    }
}
