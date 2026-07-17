package com.bumptech.glide.load.engine.cache;

import android.content.Context;
import java.io.File;

/* JADX INFO: loaded from: classes.dex */
public final class ExternalPreferredCacheDiskCacheFactory extends DiskLruCacheFactory {
    public ExternalPreferredCacheDiskCacheFactory(Context context) {
        this(context, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR, 262144000L);
    }

    public ExternalPreferredCacheDiskCacheFactory(Context context, long j) {
        this(context, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR, j);
    }

    public ExternalPreferredCacheDiskCacheFactory(Context context, String str, long j) {
        super(new DiskLruCacheFactory.CacheDirectoryGetter(context, str) { // from class: com.bumptech.glide.load.engine.cache.ExternalPreferredCacheDiskCacheFactory.1
            final Context val$context;
            final String val$diskCacheName;

            {
                this.val$context = context;
                this.val$diskCacheName = str;
            }

            private File getInternalCacheDirectory() {
                File cacheDir = this.val$context.getCacheDir();
                if (cacheDir == null) {
                    return null;
                }
                return this.val$diskCacheName != null ? new File(cacheDir, this.val$diskCacheName) : cacheDir;
            }

            @Override // com.bumptech.glide.load.engine.cache.DiskLruCacheFactory.CacheDirectoryGetter
            public File getCacheDirectory() {
                File externalCacheDir;
                File internalCacheDirectory = getInternalCacheDirectory();
                if ((internalCacheDirectory == null || !internalCacheDirectory.exists()) && (externalCacheDir = this.val$context.getExternalCacheDir()) != null && externalCacheDir.canWrite()) {
                    return this.val$diskCacheName != null ? new File(externalCacheDir, this.val$diskCacheName) : externalCacheDir;
                }
                return internalCacheDirectory;
            }
        }, j);
    }
}
