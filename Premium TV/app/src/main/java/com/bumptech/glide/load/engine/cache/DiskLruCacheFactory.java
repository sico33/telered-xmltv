package com.bumptech.glide.load.engine.cache;

import java.io.File;

/* JADX INFO: loaded from: classes.dex */
public class DiskLruCacheFactory implements DiskCache.Factory {
    private final CacheDirectoryGetter cacheDirectoryGetter;
    private final long diskCacheSize;

    public interface CacheDirectoryGetter {
        File getCacheDirectory();
    }

    public DiskLruCacheFactory(CacheDirectoryGetter cacheDirectoryGetter, long j) {
        this.diskCacheSize = j;
        this.cacheDirectoryGetter = cacheDirectoryGetter;
    }

    public DiskLruCacheFactory(String str, long j) {
        this(new CacheDirectoryGetter(str) { // from class: com.bumptech.glide.load.engine.cache.DiskLruCacheFactory.1
            final String val$diskCacheFolder;

            {
                this.val$diskCacheFolder = str;
            }

            @Override // com.bumptech.glide.load.engine.cache.DiskLruCacheFactory.CacheDirectoryGetter
            public File getCacheDirectory() {
                return new File(this.val$diskCacheFolder);
            }
        }, j);
    }

    public DiskLruCacheFactory(String str, String str2, long j) {
        this(new CacheDirectoryGetter(str, str2) { // from class: com.bumptech.glide.load.engine.cache.DiskLruCacheFactory.2
            final String val$diskCacheFolder;
            final String val$diskCacheName;

            {
                this.val$diskCacheFolder = str;
                this.val$diskCacheName = str2;
            }

            @Override // com.bumptech.glide.load.engine.cache.DiskLruCacheFactory.CacheDirectoryGetter
            public File getCacheDirectory() {
                return new File(this.val$diskCacheFolder, this.val$diskCacheName);
            }
        }, j);
    }

    @Override // com.bumptech.glide.load.engine.cache.DiskCache.Factory
    public DiskCache build() {
        File cacheDirectory = this.cacheDirectoryGetter.getCacheDirectory();
        if (cacheDirectory == null) {
            return null;
        }
        if (cacheDirectory.isDirectory() || cacheDirectory.mkdirs()) {
            return DiskLruCacheWrapper.create(cacheDirectory, this.diskCacheSize);
        }
        return null;
    }
}
