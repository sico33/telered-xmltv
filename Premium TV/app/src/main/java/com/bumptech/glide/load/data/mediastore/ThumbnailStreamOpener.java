package com.bumptech.glide.load.data.mediastore;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
class ThumbnailStreamOpener {
    private static final FileService DEFAULT_SERVICE = new FileService();
    private static final String TAG = "ThumbStreamOpener";
    private final ArrayPool byteArrayPool;
    private final ContentResolver contentResolver;
    private final List<ImageHeaderParser> parsers;
    private final ThumbnailQuery query;
    private final FileService service;

    ThumbnailStreamOpener(List<ImageHeaderParser> list, FileService fileService, ThumbnailQuery thumbnailQuery, ArrayPool arrayPool, ContentResolver contentResolver) {
        this.service = fileService;
        this.query = thumbnailQuery;
        this.byteArrayPool = arrayPool;
        this.contentResolver = contentResolver;
        this.parsers = list;
    }

    ThumbnailStreamOpener(List<ImageHeaderParser> list, ThumbnailQuery thumbnailQuery, ArrayPool arrayPool, ContentResolver contentResolver) {
        this(list, DEFAULT_SERVICE, thumbnailQuery, arrayPool, contentResolver);
    }

    /* JADX WARN: Code duplicated, block: B:11:0x001a A[DONT_INVERT] */
    /* JADX WARN: Code duplicated, block: B:12:0x001c  */
    /* JADX WARN: Code duplicated, block: B:23:0x0050  */
    private String getPath(Uri uri) throws Throwable {
        Cursor cursorQuery;
        Throwable th;
        SecurityException e;
        String string = null;
        try {
            cursorQuery = this.query.query(uri);
            if (cursorQuery != null) {
                try {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            string = cursorQuery.getString(0);
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                        } else if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        throw th;
                    }
                } catch (SecurityException e2) {
                    e = e2;
                    if (Log.isLoggable(TAG, 3)) {
                        Log.d(TAG, "Failed to query for thumbnail for Uri: " + uri, e);
                    }
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                }
            } else if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (SecurityException e3) {
            e = e3;
            cursorQuery = null;
        } catch (Throwable th3) {
            cursorQuery = null;
            th = th3;
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            throw th;
        }
        return string;
    }

    private boolean isValid(File file) {
        return this.service.exists(file) && 0 < this.service.length(file);
    }

    /* JADX WARN: Code duplicated, block: B:12:0x0022 A[Catch: all -> 0x005d, TRY_LEAVE, TryCatch #5 {all -> 0x005d, blocks: (B:10:0x0019, B:12:0x0022), top: B:40:0x0019 }] */
    /* JADX WARN: Code duplicated, block: B:34:0x003c A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:42:0x0044 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    int getOrientation(Uri uri) throws Throwable {
        InputStream inputStream;
        Throwable th;
        InputStream inputStream2 = null;
        try {
            try {
                InputStream inputStreamOpenInputStream = this.contentResolver.openInputStream(uri);
                try {
                    int orientation = ImageHeaderParserUtils.getOrientation(this.parsers, inputStreamOpenInputStream, this.byteArrayPool);
                    if (inputStreamOpenInputStream == null) {
                        return orientation;
                    }
                    try {
                        inputStreamOpenInputStream.close();
                        return orientation;
                    } catch (IOException e) {
                        return orientation;
                    }
                } catch (IOException e2) {
                    inputStream = inputStreamOpenInputStream;
                    th = e2;
                    try {
                        if (Log.isLoggable(TAG, 3)) {
                            Log.d(TAG, "Failed to open uri: " + uri, th);
                        }
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException e3) {
                            }
                        }
                        return -1;
                    } catch (Throwable th2) {
                        inputStream2 = inputStream;
                        th = th2;
                        if (inputStream2 != null) {
                            try {
                                inputStream2.close();
                            } catch (IOException e4) {
                            }
                        }
                        throw th;
                    }
                } catch (NullPointerException e5) {
                    inputStream = inputStreamOpenInputStream;
                    th = e5;
                    if (Log.isLoggable(TAG, 3)) {
                        Log.d(TAG, "Failed to open uri: " + uri, th);
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    return -1;
                }
            } catch (IOException e6) {
                inputStream = null;
                th = e6;
            } catch (NullPointerException e7) {
                inputStream = null;
                th = e7;
            }
        } catch (Throwable th3) {
            th = th3;
            if (inputStream2 != null) {
                inputStream2.close();
            }
            throw th;
        }
    }

    public InputStream open(Uri uri) throws Throwable {
        String path = getPath(uri);
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        File file = this.service.get(path);
        if (!isValid(file)) {
            return null;
        }
        Uri uriFromFile = Uri.fromFile(file);
        try {
            return this.contentResolver.openInputStream(uriFromFile);
        } catch (NullPointerException e) {
            throw ((FileNotFoundException) new FileNotFoundException("NPE opening uri: " + uri + " -> " + uriFromFile).initCause(e));
        }
    }
}
