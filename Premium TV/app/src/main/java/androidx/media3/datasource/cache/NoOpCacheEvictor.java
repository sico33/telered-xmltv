package androidx.media3.datasource.cache;

/* JADX INFO: loaded from: classes.dex */
public final class NoOpCacheEvictor implements CacheEvictor {
    @Override // androidx.media3.datasource.cache.CacheEvictor
    public boolean requiresCacheSpanTouches() {
        return false;
    }

    @Override // androidx.media3.datasource.cache.CacheEvictor
    public void onCacheInitialized() {
    }

    @Override // androidx.media3.datasource.cache.CacheEvictor
    public void onStartFile(Cache cache, String key, long position, long length) {
    }

    @Override // androidx.media3.datasource.cache.Cache.Listener
    public void onSpanAdded(Cache cache, CacheSpan span) {
    }

    @Override // androidx.media3.datasource.cache.Cache.Listener
    public void onSpanRemoved(Cache cache, CacheSpan span) {
    }

    @Override // androidx.media3.datasource.cache.Cache.Listener
    public void onSpanTouched(Cache cache, CacheSpan oldSpan, CacheSpan newSpan) {
    }
}
