package com.bumptech.glide.load.engine.cache;

import android.content.Context;
import java.io.File;

/* JADX INFO: loaded from: classes.dex */
public final class InternalCacheDiskCacheFactory extends DiskLruCacheFactory {
    public InternalCacheDiskCacheFactory(Context context) {
        this(context, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR, 262144000L);
    }

    public InternalCacheDiskCacheFactory(Context context, long j) {
        this(context, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR, j);
    }

    public InternalCacheDiskCacheFactory(Context context, String str, long j) {
        super(new DiskLruCacheFactory.CacheDirectoryGetter(context, str) { // from class: com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory.1
            final Context val$context;
            final String val$diskCacheName;

            {
                this.val$context = context;
                this.val$diskCacheName = str;
            }

            @Override // com.bumptech.glide.load.engine.cache.DiskLruCacheFactory.CacheDirectoryGetter
            public File getCacheDirectory() {
                File cacheDir = this.val$context.getCacheDir();
                if (cacheDir == null) {
                    return null;
                }
                return this.val$diskCacheName != null ? new File(cacheDir, this.val$diskCacheName) : cacheDir;
            }
        }, j);
    }
}
