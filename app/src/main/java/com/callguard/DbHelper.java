package com.callguard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite storage: every screened call and the manual allow list.
 *
 * Version 2 replaced the "blocked" table with "events" so that allowed calls
 * can be inspected too; that is what makes the diagnostics screen possible.
 */
public final class DbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "callguard.db";
    /** v2 replaced "blocked" with "events"; v3 added "whitelist.tail". */
    private static final int DB_VERSION = 3;

    private static final String T_EVENTS = "events";
    private static final String T_WHITELIST = "whitelist";

    /** Older rows are dropped so that the database stays tiny. */
    private static final int MAX_LOG_ROWS = 1000;

    private static DbHelper instance;

    public static synchronized DbHelper get(Context context) {
        if (instance == null) {
            instance = new DbHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createEvents(db);
        db.execSQL("CREATE TABLE " + T_WHITELIST + " ("
                + "canonical TEXT PRIMARY KEY,"
                + "display TEXT NOT NULL,"
                + "time INTEGER NOT NULL,"
                + "tail TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createEvents(db);
            // Carry over the journal written by version 1, if there is one.
            try {
                db.execSQL("INSERT INTO " + T_EVENTS
                        + " (time, raw, canonical, reason, verification) "
                        + "SELECT time, number, number, reason, verification FROM blocked");
            } catch (RuntimeException ignored) {
                // Nothing to migrate.
            }
            try {
                db.execSQL("DROP TABLE IF EXISTS blocked");
            } catch (RuntimeException ignored) {
                // Ignore: the old table may not exist at all.
            }
        }
        if (oldVersion < 3) {
            // The allow list used to be matched by exact string only, so a number the
            // user had allowed stopped being allowed as soon as the same call arrived
            // in a different spelling. Store the tail and fill it for existing rows.
            // Two separate statements, each in its own try. The column may already
            // exist — an install of the older build between two newer ones leaves it
            // behind while taking the schema version back down — and in that case the
            // ALTER throws. Sharing one try would skip the UPDATE with it and leave
            // those rows with a NULL tail, silently unmatched for good.
            try {
                db.execSQL("ALTER TABLE " + T_WHITELIST + " ADD COLUMN tail TEXT");
            } catch (RuntimeException ignored) {
                // The column is already there; the UPDATE below still has to run.
            }
            try {
                // Only the rows that still need it, so the migration can run twice.
                db.execSQL("UPDATE " + T_WHITELIST
                        + " SET tail = substr(canonical, length(canonical) - 9)"
                        + " WHERE tail IS NULL AND length(canonical) > 10");
            } catch (RuntimeException ignored) {
                // Without the column there is nothing to fill.
            }
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // An older APK installed over a newer one would otherwise make every call to
        // getReadableDatabase() throw, which silently turns the filter into a no-op.
        // The journal is expendable; the allow list is not, so it is kept as it is —
        // an older build simply ignores the columns it does not know.
        try {
            db.execSQL("DROP TABLE IF EXISTS " + T_EVENTS);
        } catch (RuntimeException ignored) {
            // Nothing to drop.
        }
        createEvents(db);
    }

    private void createEvents(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_EVENTS + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "time INTEGER NOT NULL,"
                + "raw TEXT,"
                + "canonical TEXT,"
                + "reason TEXT NOT NULL,"
                + "verification INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_time ON " + T_EVENTS + "(time)");
    }

    // ---------------------------------------------------------------- events

    public void addEvent(long time, String raw, String canonical, String reason, int verification) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("time", time);
        values.put("raw", raw);
        values.put("canonical", canonical);
        values.put("reason", reason);
        values.put("verification", verification);
        db.insert(T_EVENTS, null, values);
        db.execSQL("DELETE FROM " + T_EVENTS + " WHERE _id NOT IN "
                + "(SELECT _id FROM " + T_EVENTS + " ORDER BY time DESC LIMIT " + MAX_LOG_ROWS + ")");
    }

    public List<CallEvent> events(int limit) {
        return queryEvents(null, limit);
    }

    public List<CallEvent> blockedEvents(int limit) {
        return queryEvents("reason IN ('" + Reasons.BLOCK_UNKNOWN + "', '"
                + Reasons.BLOCK_UNKNOWN_VERIFIED + "', '"
                + Reasons.BLOCK_CONTACT_UNVERIFIED + "')", limit);
    }

    private List<CallEvent> queryEvents(String selection, int limit) {
        List<CallEvent> out = new ArrayList<>();
        Cursor c = getReadableDatabase().query(T_EVENTS,
                new String[]{"time", "raw", "canonical", "reason", "verification"},
                selection, null, null, null, "time DESC", String.valueOf(limit));
        try {
            while (c.moveToNext()) {
                out.add(new CallEvent(c.getLong(0), c.getString(1), c.getString(2),
                        c.getString(3), c.getInt(4)));
            }
        } finally {
            c.close();
        }
        return out;
    }

    public int blockedCount() {
        long value = DatabaseUtils.longForQuery(getReadableDatabase(),
                "SELECT COUNT(*) FROM " + T_EVENTS + " WHERE reason IN ('"
                        + Reasons.BLOCK_UNKNOWN + "', '" + Reasons.BLOCK_UNKNOWN_VERIFIED + "', '"
                        + Reasons.BLOCK_CONTACT_UNVERIFIED + "')", null);
        return (int) value;
    }

    public int eventCount() {
        long value = DatabaseUtils.longForQuery(getReadableDatabase(),
                "SELECT COUNT(*) FROM " + T_EVENTS, null);
        return (int) value;
    }

    /** SQL fragment matching the reasons that mean "the call was rejected". */
    private static String blockedSelection() {
        return "reason IN ('" + Reasons.BLOCK_UNKNOWN + "', '"
                + Reasons.BLOCK_UNKNOWN_VERIFIED + "', '"
                + Reasons.BLOCK_CONTACT_UNVERIFIED + "')";
    }

    public int blockedSince(long sinceMillis) {
        long value = DatabaseUtils.longForQuery(getReadableDatabase(),
                "SELECT COUNT(*) FROM " + T_EVENTS + " WHERE " + blockedSelection()
                        + " AND time >= " + sinceMillis, null);
        return (int) value;
    }

    public int allowedSince(long sinceMillis) {
        long value = DatabaseUtils.longForQuery(getReadableDatabase(),
                "SELECT COUNT(*) FROM " + T_EVENTS + " WHERE NOT (" + blockedSelection()
                        + ") AND time >= " + sinceMillis, null);
        return (int) value;
    }

    public long lastBlockedTime() {
        return DatabaseUtils.longForQuery(getReadableDatabase(),
                "SELECT COALESCE(MAX(time), 0) FROM " + T_EVENTS + " WHERE "
                        + blockedSelection(), null);
    }

    public void clearEvents() {
        getWritableDatabase().delete(T_EVENTS, null, null);
    }

    // -------------------------------------------------------------- allow list

    public void addWhitelisted(String canonical, String display) {
        ContentValues values = new ContentValues();
        values.put("canonical", canonical);
        values.put("display", display);
        values.put("time", System.currentTimeMillis());
        values.put("tail", canonical != null && canonical.length() > 10
                ? PhoneNumbers.tail(canonical) : null);
        getWritableDatabase().insertWithOnConflict(T_WHITELIST, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void removeWhitelisted(String canonical) {
        getWritableDatabase().delete(T_WHITELIST, "canonical = ?", new String[]{canonical});
    }

    /**
     * True when the user has allowed this number.
     *
     * Matching follows the same rule as the address book
     * ({@link PhoneNumbers#sameSubscriber}): a number the user allowed as
     * "+7 916 …" stays allowed when the same call arrives as "8 916 …", while a
     * longer spoofed number that merely shares the last ten digits does not.
     */
    public boolean isWhitelisted(String canonical) {
        if (canonical == null) {
            return false;
        }
        SQLiteDatabase db = getReadableDatabase();
        Cursor exact = db.query(T_WHITELIST, new String[]{"canonical"},
                "canonical = ?", new String[]{canonical}, null, null, null, "1");
        try {
            if (exact.moveToFirst()) {
                return true;
            }
        } finally {
            exact.close();
        }
        if (canonical.length() <= 10) {
            return false;
        }
        Cursor byTail = db.query(T_WHITELIST, new String[]{"canonical"},
                "tail = ?", new String[]{PhoneNumbers.tail(canonical)}, null, null, null);
        try {
            while (byTail.moveToNext()) {
                if (PhoneNumbers.sameSubscriber(canonical, byTail.getString(0))) {
                    return true;
                }
            }
        } finally {
            byTail.close();
        }
        return false;
    }

    public List<WhitelistEntry> whitelist() {
        List<WhitelistEntry> out = new ArrayList<>();
        Cursor c = getReadableDatabase().query(T_WHITELIST,
                new String[]{"canonical", "display"}, null, null, null, null, "time DESC");
        try {
            while (c.moveToNext()) {
                out.add(new WhitelistEntry(c.getString(0), c.getString(1)));
            }
        } finally {
            c.close();
        }
        return out;
    }

    public int whitelistCount() {
        long value = DatabaseUtils.longForQuery(getReadableDatabase(),
                "SELECT COUNT(*) FROM " + T_WHITELIST, null);
        return (int) value;
    }
}
