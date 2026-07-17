package androidx.media3.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import androidx.media3.common.MediaLibraryInfo;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public final class VersionTable {
    private static final String COLUMN_FEATURE = "feature";
    private static final String COLUMN_INSTANCE_UID = "instance_uid";
    private static final String COLUMN_VERSION = "version";
    public static final int FEATURE_CACHE_CONTENT_METADATA = 1;
    public static final int FEATURE_CACHE_FILE_METADATA = 2;
    public static final int FEATURE_EXTERNAL = 1000;
    public static final int FEATURE_OFFLINE = 0;
    private static final String PRIMARY_KEY = "PRIMARY KEY (feature, instance_uid)";
    private static final String SQL_CREATE_TABLE_IF_NOT_EXISTS = "CREATE TABLE IF NOT EXISTS ExoPlayerVersions (feature INTEGER NOT NULL,instance_uid TEXT NOT NULL,version INTEGER NOT NULL,PRIMARY KEY (feature, instance_uid))";
    private static final String TABLE_NAME = "ExoPlayerVersions";
    public static final int VERSION_UNSET = -1;
    private static final String WHERE_FEATURE_AND_INSTANCE_UID_EQUALS = "feature = ? AND instance_uid = ?";

    static {
        MediaLibraryInfo.registerModule("media3.database");
    }

    private VersionTable() {
    }

    public static void setVersion(SQLiteDatabase writableDatabase, int feature, String instanceUid, int version) throws DatabaseIOException {
        try {
            writableDatabase.execSQL(SQL_CREATE_TABLE_IF_NOT_EXISTS);
            ContentValues values = new ContentValues();
            values.put(COLUMN_FEATURE, Integer.valueOf(feature));
            values.put(COLUMN_INSTANCE_UID, instanceUid);
            values.put(COLUMN_VERSION, Integer.valueOf(version));
            writableDatabase.replaceOrThrow(TABLE_NAME, null, values);
        } catch (SQLException e) {
            throw new DatabaseIOException(e);
        }
    }

    public static void removeVersion(SQLiteDatabase writableDatabase, int feature, String instanceUid) throws DatabaseIOException {
        try {
            if (!Util.tableExists(writableDatabase, TABLE_NAME)) {
                return;
            }
            writableDatabase.delete(TABLE_NAME, WHERE_FEATURE_AND_INSTANCE_UID_EQUALS, featureAndInstanceUidArguments(feature, instanceUid));
        } catch (SQLException e) {
            throw new DatabaseIOException(e);
        }
    }

    public static int getVersion(SQLiteDatabase database, int feature, String instanceUid) throws DatabaseIOException {
        SQLException e;
        try {
            if (!Util.tableExists(database, TABLE_NAME)) {
                return -1;
            }
            try {
                Cursor cursor = database.query(TABLE_NAME, new String[]{COLUMN_VERSION}, WHERE_FEATURE_AND_INSTANCE_UID_EQUALS, featureAndInstanceUidArguments(feature, instanceUid), null, null, null);
                try {
                    if (cursor.getCount() != 0) {
                        cursor.moveToNext();
                        int i = cursor.getInt(0);
                        if (cursor != null) {
                            cursor.close();
                        }
                        return i;
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                    return -1;
                } catch (Throwable th) {
                    if (cursor == null) {
                        throw th;
                    }
                    try {
                        cursor.close();
                        throw th;
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                        throw th;
                    }
                }
            } catch (SQLException e2) {
                e = e2;
            }
            e = e2;
        } catch (SQLException e3) {
            e = e3;
        }
        throw new DatabaseIOException(e);
    }

    private static String[] featureAndInstanceUidArguments(int feature, String instance) {
        return new String[]{Integer.toString(feature), instance};
    }
}
