package androidx.media3.exoplayer.offline;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.text.TextUtils;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.StreamKey;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.database.DatabaseIOException;
import androidx.media3.database.DatabaseProvider;
import androidx.media3.database.VersionTable;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class DefaultDownloadIndex implements WritableDownloadIndex {
    private static final String COLUMN_DATA = "data";
    private static final String COLUMN_ID = "id";
    private static final int COLUMN_INDEX_BYTES_DOWNLOADED = 13;
    private static final int COLUMN_INDEX_CONTENT_LENGTH = 9;
    private static final int COLUMN_INDEX_CUSTOM_CACHE_KEY = 4;
    private static final int COLUMN_INDEX_DATA = 5;
    private static final int COLUMN_INDEX_FAILURE_REASON = 11;
    private static final int COLUMN_INDEX_ID = 0;
    private static final int COLUMN_INDEX_KEY_SET_ID = 14;
    private static final int COLUMN_INDEX_MIME_TYPE = 1;
    private static final int COLUMN_INDEX_PERCENT_DOWNLOADED = 12;
    private static final int COLUMN_INDEX_START_TIME_MS = 7;
    private static final int COLUMN_INDEX_STATE = 6;
    private static final int COLUMN_INDEX_STOP_REASON = 10;
    private static final int COLUMN_INDEX_STREAM_KEYS = 3;
    private static final int COLUMN_INDEX_UPDATE_TIME_MS = 8;
    private static final int COLUMN_INDEX_URI = 2;
    private static final String COLUMN_STOP_REASON = "stop_reason";
    private static final String TABLE_PREFIX = "ExoPlayerDownloads";
    private static final String TABLE_SCHEMA = "(id TEXT PRIMARY KEY NOT NULL,mime_type TEXT,uri TEXT NOT NULL,stream_keys TEXT NOT NULL,custom_cache_key TEXT,data BLOB NOT NULL,state INTEGER NOT NULL,start_time_ms INTEGER NOT NULL,update_time_ms INTEGER NOT NULL,content_length INTEGER NOT NULL,stop_reason INTEGER NOT NULL,failure_reason INTEGER NOT NULL,percent_downloaded REAL NOT NULL,bytes_downloaded INTEGER NOT NULL,key_set_id BLOB NOT NULL)";
    static final int TABLE_VERSION = 3;
    private static final String TRUE = "1";
    private static final String WHERE_ID_EQUALS = "id = ?";
    private static final String WHERE_STATE_IS_DOWNLOADING = "state = 2";
    private final DatabaseProvider databaseProvider;
    private final Object initializationLock;
    private boolean initialized;
    private final String name;
    private final String tableName;
    private static final String WHERE_STATE_IS_TERMINAL = getStateQuery(3, 4);
    private static final String COLUMN_MIME_TYPE = "mime_type";
    private static final String COLUMN_URI = "uri";
    private static final String COLUMN_STREAM_KEYS = "stream_keys";
    private static final String COLUMN_CUSTOM_CACHE_KEY = "custom_cache_key";
    private static final String COLUMN_STATE = "state";
    private static final String COLUMN_START_TIME_MS = "start_time_ms";
    private static final String COLUMN_UPDATE_TIME_MS = "update_time_ms";
    private static final String COLUMN_CONTENT_LENGTH = "content_length";
    private static final String COLUMN_FAILURE_REASON = "failure_reason";
    private static final String COLUMN_PERCENT_DOWNLOADED = "percent_downloaded";
    private static final String COLUMN_BYTES_DOWNLOADED = "bytes_downloaded";
    private static final String COLUMN_KEY_SET_ID = "key_set_id";
    private static final String[] COLUMNS = {"id", COLUMN_MIME_TYPE, COLUMN_URI, COLUMN_STREAM_KEYS, COLUMN_CUSTOM_CACHE_KEY, "data", COLUMN_STATE, COLUMN_START_TIME_MS, COLUMN_UPDATE_TIME_MS, COLUMN_CONTENT_LENGTH, "stop_reason", COLUMN_FAILURE_REASON, COLUMN_PERCENT_DOWNLOADED, COLUMN_BYTES_DOWNLOADED, COLUMN_KEY_SET_ID};

    public DefaultDownloadIndex(DatabaseProvider databaseProvider) {
        this(databaseProvider, "");
    }

    public DefaultDownloadIndex(DatabaseProvider databaseProvider, String name) {
        this.name = name;
        this.databaseProvider = databaseProvider;
        this.tableName = TABLE_PREFIX + name;
        this.initializationLock = new Object();
    }

    @Override // androidx.media3.exoplayer.offline.DownloadIndex
    public Download getDownload(String id) throws DatabaseIOException {
        ensureInitialized();
        try {
            Cursor cursor = getCursor(WHERE_ID_EQUALS, new String[]{id});
            try {
                if (cursor.getCount() != 0) {
                    cursor.moveToNext();
                    Download downloadForCurrentRow = getDownloadForCurrentRow(cursor);
                    if (cursor != null) {
                        cursor.close();
                    }
                    return downloadForCurrentRow;
                }
                if (cursor != null) {
                    cursor.close();
                    return null;
                }
                return null;
            } catch (Throwable th) {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        } catch (SQLiteException e) {
            throw new DatabaseIOException(e);
        }
        throw new DatabaseIOException(e);
    }

    @Override // androidx.media3.exoplayer.offline.DownloadIndex
    public DownloadCursor getDownloads(int... states) throws DatabaseIOException {
        ensureInitialized();
        Cursor cursor = getCursor(getStateQuery(states), null);
        return new DownloadCursorImpl(cursor);
    }

    @Override // androidx.media3.exoplayer.offline.WritableDownloadIndex
    public void putDownload(Download download) throws DatabaseIOException {
        ensureInitialized();
        try {
            SQLiteDatabase writableDatabase = this.databaseProvider.getWritableDatabase();
            putDownloadInternal(download, writableDatabase);
        } catch (SQLiteException e) {
            throw new DatabaseIOException(e);
        }
    }

    @Override // androidx.media3.exoplayer.offline.WritableDownloadIndex
    public void removeDownload(String id) throws DatabaseIOException {
        ensureInitialized();
        try {
            this.databaseProvider.getWritableDatabase().delete(this.tableName, WHERE_ID_EQUALS, new String[]{id});
        } catch (SQLiteException e) {
            throw new DatabaseIOException(e);
        }
    }

    @Override // androidx.media3.exoplayer.offline.WritableDownloadIndex
    public void setDownloadingStatesToQueued() throws DatabaseIOException {
        ensureInitialized();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_STATE, (Integer) 0);
            SQLiteDatabase writableDatabase = this.databaseProvider.getWritableDatabase();
            writableDatabase.update(this.tableName, values, WHERE_STATE_IS_DOWNLOADING, null);
        } catch (SQLException e) {
            throw new DatabaseIOException(e);
        }
    }

    @Override // androidx.media3.exoplayer.offline.WritableDownloadIndex
    public void setStatesToRemoving() throws DatabaseIOException {
        ensureInitialized();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_STATE, (Integer) 5);
            values.put(COLUMN_FAILURE_REASON, (Integer) 0);
            SQLiteDatabase writableDatabase = this.databaseProvider.getWritableDatabase();
            writableDatabase.update(this.tableName, values, null, null);
        } catch (SQLException e) {
            throw new DatabaseIOException(e);
        }
    }

    @Override // androidx.media3.exoplayer.offline.WritableDownloadIndex
    public void setStopReason(int stopReason) throws DatabaseIOException {
        ensureInitialized();
        try {
            ContentValues values = new ContentValues();
            values.put("stop_reason", Integer.valueOf(stopReason));
            SQLiteDatabase writableDatabase = this.databaseProvider.getWritableDatabase();
            writableDatabase.update(this.tableName, values, WHERE_STATE_IS_TERMINAL, null);
        } catch (SQLException e) {
            throw new DatabaseIOException(e);
        }
    }

    @Override // androidx.media3.exoplayer.offline.WritableDownloadIndex
    public void setStopReason(String id, int stopReason) throws DatabaseIOException {
        ensureInitialized();
        try {
            ContentValues values = new ContentValues();
            values.put("stop_reason", Integer.valueOf(stopReason));
            SQLiteDatabase writableDatabase = this.databaseProvider.getWritableDatabase();
            writableDatabase.update(this.tableName, values, WHERE_STATE_IS_TERMINAL + " AND " + WHERE_ID_EQUALS, new String[]{id});
        } catch (SQLException e) {
            throw new DatabaseIOException(e);
        }
    }

    private void ensureInitialized() throws DatabaseIOException {
        synchronized (this.initializationLock) {
            if (this.initialized) {
                return;
            }
            try {
                SQLiteDatabase readableDatabase = this.databaseProvider.getReadableDatabase();
                int version = VersionTable.getVersion(readableDatabase, 0, this.name);
                if (version != 3) {
                    SQLiteDatabase writableDatabase = this.databaseProvider.getWritableDatabase();
                    writableDatabase.beginTransactionNonExclusive();
                    try {
                        VersionTable.setVersion(writableDatabase, 0, this.name, 3);
                        List<Download> upgradedDownloads = version == 2 ? loadDownloadsFromVersion2(writableDatabase) : new ArrayList<>();
                        writableDatabase.execSQL("DROP TABLE IF EXISTS " + this.tableName);
                        writableDatabase.execSQL("CREATE TABLE " + this.tableName + " " + TABLE_SCHEMA);
                        for (Download download : upgradedDownloads) {
                            putDownloadInternal(download, writableDatabase);
                        }
                        writableDatabase.setTransactionSuccessful();
                        writableDatabase.endTransaction();
                    } catch (Throwable th) {
                        writableDatabase.endTransaction();
                        throw th;
                    }
                }
                this.initialized = true;
            } catch (SQLException e) {
                throw new DatabaseIOException(e);
            }
        }
    }

    private void putDownloadInternal(Download download, SQLiteDatabase database) {
        byte[] keySetId = download.request.keySetId == null ? Util.EMPTY_BYTE_ARRAY : download.request.keySetId;
        ContentValues values = new ContentValues();
        values.put("id", download.request.id);
        values.put(COLUMN_MIME_TYPE, download.request.mimeType);
        values.put(COLUMN_URI, download.request.uri.toString());
        values.put(COLUMN_STREAM_KEYS, encodeStreamKeys(download.request.streamKeys));
        values.put(COLUMN_CUSTOM_CACHE_KEY, download.request.customCacheKey);
        values.put("data", download.request.data);
        values.put(COLUMN_STATE, Integer.valueOf(download.state));
        values.put(COLUMN_START_TIME_MS, Long.valueOf(download.startTimeMs));
        values.put(COLUMN_UPDATE_TIME_MS, Long.valueOf(download.updateTimeMs));
        values.put(COLUMN_CONTENT_LENGTH, Long.valueOf(download.contentLength));
        values.put("stop_reason", Integer.valueOf(download.stopReason));
        values.put(COLUMN_FAILURE_REASON, Integer.valueOf(download.failureReason));
        values.put(COLUMN_PERCENT_DOWNLOADED, Float.valueOf(download.getPercentDownloaded()));
        values.put(COLUMN_BYTES_DOWNLOADED, Long.valueOf(download.getBytesDownloaded()));
        values.put(COLUMN_KEY_SET_ID, keySetId);
        database.replaceOrThrow(this.tableName, null, values);
    }

    private List<Download> loadDownloadsFromVersion2(SQLiteDatabase database) {
        List<Download> downloads = new ArrayList<>();
        if (!Util.tableExists(database, this.tableName)) {
            return downloads;
        }
        String[] columnsV2 = {"id", "title", COLUMN_URI, COLUMN_STREAM_KEYS, COLUMN_CUSTOM_CACHE_KEY, "data", COLUMN_STATE, COLUMN_START_TIME_MS, COLUMN_UPDATE_TIME_MS, COLUMN_CONTENT_LENGTH, "stop_reason", COLUMN_FAILURE_REASON, COLUMN_PERCENT_DOWNLOADED, COLUMN_BYTES_DOWNLOADED};
        Cursor cursor = database.query(this.tableName, columnsV2, null, null, null, null, null);
        while (cursor.moveToNext()) {
            try {
                downloads.add(getDownloadForCurrentRowV2(cursor));
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
        }
        if (cursor != null) {
            cursor.close();
        }
        return downloads;
    }

    private static String inferMimeType(String downloadType) {
        if ("dash".equals(downloadType)) {
            return MimeTypes.APPLICATION_MPD;
        }
        if ("hls".equals(downloadType)) {
            return MimeTypes.APPLICATION_M3U8;
        }
        if ("ss".equals(downloadType)) {
            return MimeTypes.APPLICATION_SS;
        }
        return MimeTypes.VIDEO_UNKNOWN;
    }

    private Cursor getCursor(String selection, String[] selectionArgs) throws DatabaseIOException {
        SQLiteException e;
        try {
            try {
                return this.databaseProvider.getReadableDatabase().query(this.tableName, COLUMNS, selection, selectionArgs, null, null, "start_time_ms ASC");
            } catch (SQLiteException e2) {
                e = e2;
                throw new DatabaseIOException(e);
            }
        } catch (SQLiteException e3) {
            e = e3;
        }
    }

    static String encodeStreamKeys(List<StreamKey> streamKeys) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < streamKeys.size(); i++) {
            StreamKey streamKey = streamKeys.get(i);
            stringBuilder.append(streamKey.periodIndex).append('.').append(streamKey.groupIndex).append('.').append(streamKey.streamIndex).append(',');
        }
        int i2 = stringBuilder.length();
        if (i2 > 0) {
            stringBuilder.setLength(stringBuilder.length() - 1);
        }
        return stringBuilder.toString();
    }

    private static String getStateQuery(int... states) {
        if (states.length == 0) {
            return "1";
        }
        StringBuilder selectionBuilder = new StringBuilder();
        selectionBuilder.append(COLUMN_STATE).append(" IN (");
        for (int i = 0; i < states.length; i++) {
            if (i > 0) {
                selectionBuilder.append(',');
            }
            selectionBuilder.append(states[i]);
        }
        selectionBuilder.append(')');
        return selectionBuilder.toString();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Download getDownloadForCurrentRow(Cursor cursor) {
        int failureReason;
        byte[] keySetId = cursor.getBlob(14);
        DownloadRequest request = new DownloadRequest.Builder((String) Assertions.checkNotNull(cursor.getString(0)), Uri.parse((String) Assertions.checkNotNull(cursor.getString(2)))).setMimeType(cursor.getString(1)).setStreamKeys(decodeStreamKeys(cursor.getString(3))).setKeySetId(keySetId.length > 0 ? keySetId : null).setCustomCacheKey(cursor.getString(4)).setData(cursor.getBlob(5)).build();
        DownloadProgress downloadProgress = new DownloadProgress();
        downloadProgress.bytesDownloaded = cursor.getLong(13);
        downloadProgress.percentDownloaded = cursor.getFloat(12);
        int state = cursor.getInt(6);
        if (state == 4) {
            failureReason = cursor.getInt(11);
        } else {
            failureReason = 0;
        }
        return new Download(request, state, cursor.getLong(7), cursor.getLong(8), cursor.getLong(9), cursor.getInt(10), failureReason, downloadProgress);
    }

    private static Download getDownloadForCurrentRowV2(Cursor cursor) {
        DownloadRequest request = new DownloadRequest.Builder((String) Assertions.checkNotNull(cursor.getString(0)), Uri.parse((String) Assertions.checkNotNull(cursor.getString(2)))).setMimeType(inferMimeType(cursor.getString(1))).setStreamKeys(decodeStreamKeys(cursor.getString(3))).setCustomCacheKey(cursor.getString(4)).setData(cursor.getBlob(5)).build();
        DownloadProgress downloadProgress = new DownloadProgress();
        downloadProgress.bytesDownloaded = cursor.getLong(13);
        downloadProgress.percentDownloaded = cursor.getFloat(12);
        int state = cursor.getInt(6);
        int failureReason = state == 4 ? cursor.getInt(11) : 0;
        return new Download(request, state, cursor.getLong(7), cursor.getLong(8), cursor.getLong(9), cursor.getInt(10), failureReason, downloadProgress);
    }

    private static List<StreamKey> decodeStreamKeys(String encodedStreamKeys) {
        ArrayList<StreamKey> streamKeys = new ArrayList<>();
        if (TextUtils.isEmpty(encodedStreamKeys)) {
            return streamKeys;
        }
        String[] streamKeysStrings = Util.split(encodedStreamKeys, ",");
        for (String streamKeysString : streamKeysStrings) {
            String[] indices = Util.split(streamKeysString, "\\.");
            Assertions.checkState(indices.length == 3);
            streamKeys.add(new StreamKey(Integer.parseInt(indices[0]), Integer.parseInt(indices[1]), Integer.parseInt(indices[2])));
        }
        return streamKeys;
    }

    private static final class DownloadCursorImpl implements DownloadCursor {
        private final Cursor cursor;

        @Override // androidx.media3.exoplayer.offline.DownloadCursor
        public /* synthetic */ boolean isAfterLast() {
            return DownloadCursor.CC.$default$isAfterLast(this);
        }

        @Override // androidx.media3.exoplayer.offline.DownloadCursor
        public /* synthetic */ boolean isBeforeFirst() {
            return DownloadCursor.CC.$default$isBeforeFirst(this);
        }

        @Override // androidx.media3.exoplayer.offline.DownloadCursor
        public /* synthetic */ boolean isFirst() {
            return DownloadCursor.CC.$default$isFirst(this);
        }

        @Override // androidx.media3.exoplayer.offline.DownloadCursor
        public /* synthetic */ boolean isLast() {
            return DownloadCursor.CC.$default$isLast(this);
        }

        @Override // androidx.media3.exoplayer.offline.DownloadCursor
        public /* synthetic */ boolean moveToFirst() {
            return moveToPosition(0);
        }

        @Override // androidx.media3.exoplayer.offline.DownloadCursor
        public /* synthetic */ boolean moveToLast() {
            return moveToPosition(getCount() - 1);
        }

        @Override // androidx.media3.exoplayer.offline.DownloadCursor
        public /* synthetic */ boolean moveToNext() {
            return moveToPosition(getPosition() + 1);
        }

        @Override // androidx.media3.exoplayer.offline.DownloadCursor
        public /* synthetic */ boolean moveToPrevious() {
            return moveToPosition(getPosition() - 1);
        }

        private DownloadCursorImpl(Cursor cursor) {
            this.cursor = cursor;
        }

        @Override // androidx.media3.exoplayer.offline.DownloadCursor
        public Download getDownload() {
            return DefaultDownloadIndex.getDownloadForCurrentRow(this.cursor);
        }

        @Override // androidx.media3.exoplayer.offline.DownloadCursor
        public int getCount() {
            return this.cursor.getCount();
        }

        @Override // androidx.media3.exoplayer.offline.DownloadCursor
        public int getPosition() {
            return this.cursor.getPosition();
        }

        @Override // androidx.media3.exoplayer.offline.DownloadCursor
        public boolean moveToPosition(int position) {
            return this.cursor.moveToPosition(position);
        }

        @Override // androidx.media3.exoplayer.offline.DownloadCursor, java.io.Closeable, java.lang.AutoCloseable
        public void close() {
            this.cursor.close();
        }

        @Override // androidx.media3.exoplayer.offline.DownloadCursor
        public boolean isClosed() {
            return this.cursor.isClosed();
        }
    }
}
