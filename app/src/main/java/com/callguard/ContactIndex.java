package com.callguard;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * In-memory snapshot of every phone number stored in the address book.
 *
 * The call screening callback has to answer within five seconds, so the
 * address book is read once and cached instead of being queried per call.
 */
public final class ContactIndex {

    private static final long TTL_MS = 5L * 60L * 1000L;
    private static final Object LOCK = new Object();

    private static Set<String> exact = Collections.emptySet();
    private static Set<String> tails = Collections.emptySet();
    private static Map<String, String> names = Collections.emptyMap();
    private static Map<String, String> tailNames = Collections.emptyMap();
    private static long loadedAt;
    /** True when the last attempt to read the address book has failed. */
    private static boolean loadFailed;

    private ContactIndex() {
    }

    /** Result of looking a number up in the address book. */
    public static final class Match {

        /** False when the address book could not be read at all. */
        public final boolean available;
        /** Contact name, or {@code null} when the number is unknown. */
        public final String name;

        Match(boolean available, String name) {
            this.available = available;
            this.name = name;
        }
    }

    /** Forces the next lookup to re-read the address book. */
    public static void invalidate() {
        synchronized (LOCK) {
            loadedAt = 0L;
        }
    }

    /** Number of distinct numbers known to the index. */
    public static int count(Context context) {
        if (!hasPermission(context)) {
            return 0;
        }
        synchronized (LOCK) {
            ensureLoadedLocked(context);
            return names.size();
        }
    }

    public static Match lookup(Context context, String canonical) {
        if (canonical == null) {
            return new Match(true, null);
        }
        synchronized (LOCK) {
            ensureLoadedLocked(context);
            if (loadFailed) {
                return new Match(false, null);
            }
            if (!PhoneNumbers.matches(canonical, exact, tails)) {
                return new Match(true, null);
            }
            String name = names.get(canonical);
            if (name == null && canonical.length() >= 11) {
                name = tailNames.get(PhoneNumbers.tail(canonical));
            }
            return new Match(true, name == null ? "" : name);
        }
    }

    private static boolean hasPermission(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static void ensureLoadedLocked(Context context) {
        long now = System.currentTimeMillis();
        if (loadedAt != 0L && now - loadedAt < TTL_MS) {
            return;
        }
        Set<String> newExact = new HashSet<>();
        Set<String> newTails = new HashSet<>();
        Map<String, String> newNames = new HashMap<>();
        Map<String, String> newTailNames = new HashMap<>();
        boolean failed = false;

        if (hasPermission(context)) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{
                                ContactsContract.CommonDataKinds.Phone.NUMBER,
                                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                        },
                        null, null, null);
                if (cursor == null) {
                    failed = true;
                } else {
                    while (cursor.moveToNext()) {
                        String name = cursor.getString(2);
                        if (name == null) {
                            name = "";
                        }
                        add(newExact, newTails, newNames, newTailNames,
                                PhoneNumbers.canonical(cursor.getString(0)), name);
                        add(newExact, newTails, newNames, newTailNames,
                                cursor.getString(1), name);
                    }
                }
            } catch (RuntimeException ignored) {
                // A locked or unavailable contacts provider must not break screening.
                failed = true;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        if (failed && !names.isEmpty()) {
            // Keep the previous snapshot rather than forgetting every contact.
            loadedAt = now;
            return;
        }
        exact = newExact;
        tails = newTails;
        names = newNames;
        tailNames = newTailNames;
        loadFailed = failed;
        loadedAt = now;
    }

    private static void add(Set<String> exactSet, Set<String> tailsSet,
                            Map<String, String> nameMap, Map<String, String> tailNameMap,
                            String value, String name) {
        String canonical = PhoneNumbers.canonical(value);
        if (canonical == null) {
            return;
        }
        exactSet.add(canonical);
        if (!nameMap.containsKey(canonical)) {
            nameMap.put(canonical, name);
        }
        if (canonical.length() >= 11) {
            String tail = PhoneNumbers.tail(canonical);
            tailsSet.add(tail);
            if (!tailNameMap.containsKey(tail)) {
                tailNameMap.put(tail, name);
            }
        }
    }
}
