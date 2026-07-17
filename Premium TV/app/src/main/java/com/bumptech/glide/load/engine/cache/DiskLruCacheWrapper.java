package com.bumptech.glide.load.engine.cache;

import android.util.Log;
import com.bumptech.glide.disklrucache.DiskLruCache;
import com.bumptech.glide.load.Key;
import java.io.File;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public class DiskLruCacheWrapper implements DiskCache {
    private static final int APP_VERSION = 1;
    private static final String TAG = "DiskLruCacheWrapper";
    private static final int VALUE_COUNT = 1;
    private static DiskLruCacheWrapper wrapper;
    private final File directory;
    private DiskLruCache diskLruCache;
    private final long maxSize;
    private final DiskCacheWriteLocker writeLocker = new DiskCacheWriteLocker();
    private final SafeKeyGenerator safeKeyGenerator = new SafeKeyGenerator();

    @Deprecated
    protected DiskLruCacheWrapper(File file, long j) {
        this.directory = file;
        this.maxSize = j;
    }

    public static DiskCache create(File file, long j) {
        return new DiskLruCacheWrapper(file, j);
    }

    @Deprecated
    public static DiskCache get(File file, long j) {
        DiskLruCacheWrapper diskLruCacheWrapper;
        synchronized (DiskLruCacheWrapper.class) {
            try {
                if (wrapper == null) {
                    wrapper = new DiskLruCacheWrapper(file, j);
                }
                diskLruCacheWrapper = wrapper;
            } catch (Throwable th) {
                throw th;
            }
        }
        return diskLruCacheWrapper;
    }

    private DiskLruCache getDiskCache() throws IOException {
        DiskLruCache diskLruCache;
        synchronized (this) {
            if (this.diskLruCache == null) {
                this.diskLruCache = DiskLruCache.open(this.directory, 1, 1, this.maxSize);
            }
            diskLruCache = this.diskLruCache;
        }
        return diskLruCache;
    }

    private void resetDiskCache() {
        synchronized (this) {
            this.diskLruCache = null;
        }
    }

    @Override // com.bumptech.glide.load.engine.cache.DiskCache
    public void clear() {
        synchronized (this) {
            try {
                try {
                    getDiskCache().delete();
                    resetDiskCache();
                } catch (IOException e) {
                    if (Log.isLoggable(TAG, 5)) {
                        Log.w(TAG, "Unable to clear disk cache or disk cache cleared externally", e);
                    }
                    resetDiskCache();
                }
            } catch (Throwable th) {
                resetDiskCache();
                throw th;
            }
        }
    }

    @Override // com.bumptech.glide.load.engine.cache.DiskCache
    public void delete(Key key) {
        try {
            getDiskCache().remove(this.safeKeyGenerator.getSafeKey(key));
        } catch (IOException e) {
            if (Log.isLoggable(TAG, 5)) {
                Log.w(TAG, "Unable to delete from disk cache", e);
            }
        }
    }

    @Override // com.bumptech.glide.load.engine.cache.DiskCache
    public File get(Key key) throws Throwable {
        String safeKey = this.safeKeyGenerator.getSafeKey(key);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "Get: Obtained: " + safeKey + " for for Key: " + key);
        }
        try {
            DiskLruCache.Value value = getDiskCache().get(safeKey);
            if (value != null) {
                return value.getFile(0);
            }
            return null;
        } catch (IOException e) {
            if (!Log.isLoggable(TAG, 5)) {
                return null;
            }
            Log.w(TAG, "Unable to get from disk cache", e);
            return null;
        }
    }

    @Override // com.bumptech.glide.load.engine.cache.DiskCache
    public void put(Key key, DiskCache.Writer writer) {
        String safeKey = this.safeKeyGenerator.getSafeKey(key);
        this.writeLocker.acquire(safeKey);
        try {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "Put: Obtained: " + safeKey + " for for Key: " + key);
            }
            try {
                DiskLruCache diskCache = getDiskCache();
                if (diskCache.get(safeKey) != null) {
                    this.writeLocker.release(safeKey);
                    return;
                }
                DiskLruCache.Editor editorEdit = diskCache.edit(safeKey);
                if (editorEdit == null) {
                    throw new IllegalStateException("Had two simultaneous puts for: " + safeKey);
                }
                try {
                    if (writer.write(editorEdit.getFile(0))) {
                        editorEdit.commit();
                    }
                    editorEdit.abortUnlessCommitted();
                    this.writeLocker.release(safeKey);
                } catch (Throwable th) {
                    editorEdit.abortUnlessCommitted();
                    throw th;
                }
            } catch (IOException e) {
                if (Log.isLoggable(TAG, 5)) {
                    Log.w(TAG, "Unable to put to disk cache", e);
                }
            }
        } catch (Throwable th2) {
            this.writeLocker.release(safeKey);
            throw th2;
        }
    }
}
