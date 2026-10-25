package com.callguard;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Minimal FileProvider: exposes exactly one cached APK to the system installer,
 * so an update can be opened with ACTION_VIEW the same way a file manager does.
 */
public final class ApkProvider extends ContentProvider {

    public static final String AUTHORITY = "com.callguard.files";
    public static final String FILE_NAME = "callguard-update.apk";

    @Override
    public boolean onCreate() {
        return true;
    }

    private File file() {
        if (getContext() == null) {
            return new File(FILE_NAME);
        }
        return new File(getContext().getCacheDir(), FILE_NAME);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        MatrixCursor cursor = new MatrixCursor(
                new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        cursor.addRow(new Object[]{"CallGuard-update.apk", file().length()});
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return "application/vnd.android.package-archive";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File apk = file();
        if (!apk.exists()) {
            throw new FileNotFoundException(apk.getAbsolutePath());
        }
        return ParcelFileDescriptor.open(apk, ParcelFileDescriptor.MODE_READ_ONLY);
    }
}
