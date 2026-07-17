package com.bumptech.glide.load.engine.cache;

import android.content.Context;
import java.io.File;

/* JADX INFO: loaded from: classes.dex */
@Deprecated
public final class ExternalCacheDiskCacheFactory extends DiskLruCacheFactory {
    public ExternalCacheDiskCacheFactory(Context context) {
        this(context, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR, DiskCache.Factory.DEFAULT_DISK_CACHE_SIZE);
    }

    public ExternalCacheDiskCacheFactory(Context context, int i) {
        this(context, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR, i);
    }

    public ExternalCacheDiskCacheFactory(Context context, String str, int i) {
        super(new DiskLruCacheFactory.CacheDirectoryGetter(context, str) { // from class: com.bumptech.glide.load.engine.cache.ExternalCacheDiskCacheFactory.1
            final Context val$context;
            final String val$diskCacheName;

            {
                this.val$context = context;
                this.val$diskCacheName = str;
            }

            @Override // com.bumptech.glide.load.engine.cache.DiskLruCacheFactory.CacheDirectoryGetter
            public File getCacheDirectory() {
                File externalCacheDir = this.val$context.getExternalCacheDir();
                if (externalCacheDir == null) {
                    return null;
                }
                return this.val$diskCacheName != null ? new File(externalCacheDir, this.val$diskCacheName) : externalCacheDir;
            }
        }, i);
    }
}
