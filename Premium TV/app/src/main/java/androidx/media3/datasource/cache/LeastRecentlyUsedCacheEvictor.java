package androidx.media3.datasource.cache;

import java.util.Comparator;
import java.util.TreeSet;

/* JADX INFO: loaded from: classes.dex */
public final class LeastRecentlyUsedCacheEvictor implements CacheEvictor {
    private long currentSize;
    private final TreeSet<CacheSpan> leastRecentlyUsed = new TreeSet<>(new Comparator() { // from class: androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor$$ExternalSyntheticLambda0
        @Override // java.util.Comparator
        public final int compare(Object obj, Object obj2) {
            return LeastRecentlyUsedCacheEvictor.compare((CacheSpan) obj, (CacheSpan) obj2);
        }
    });
    private final long maxBytes;

    public LeastRecentlyUsedCacheEvictor(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    @Override // androidx.media3.datasource.cache.CacheEvictor
    public boolean requiresCacheSpanTouches() {
        return true;
    }

    @Override // androidx.media3.datasource.cache.CacheEvictor
    public void onCacheInitialized() {
    }

    @Override // androidx.media3.datasource.cache.CacheEvictor
    public void onStartFile(Cache cache, String key, long position, long length) {
        if (length != -1) {
            evictCache(cache, length);
        }
    }

    @Override // androidx.media3.datasource.cache.Cache.Listener
    public void onSpanAdded(Cache cache, CacheSpan span) {
        this.leastRecentlyUsed.add(span);
        this.currentSize += span.length;
        evictCache(cache, 0L);
    }

    @Override // androidx.media3.datasource.cache.Cache.Listener
    public void onSpanRemoved(Cache cache, CacheSpan span) {
        this.leastRecentlyUsed.remove(span);
        this.currentSize -= span.length;
    }

    @Override // androidx.media3.datasource.cache.Cache.Listener
    public void onSpanTouched(Cache cache, CacheSpan oldSpan, CacheSpan newSpan) {
        onSpanRemoved(cache, oldSpan);
        onSpanAdded(cache, newSpan);
    }

    private void evictCache(Cache cache, long requiredSpace) {
        while (this.currentSize + requiredSpace > this.maxBytes && !this.leastRecentlyUsed.isEmpty()) {
            cache.removeSpan(this.leastRecentlyUsed.first());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int compare(CacheSpan lhs, CacheSpan rhs) {
        long lastTouchTimestampDelta = lhs.lastTouchTimestamp - rhs.lastTouchTimestamp;
        if (lastTouchTimestampDelta == 0) {
            return lhs.compareTo(rhs);
        }
        return lhs.lastTouchTimestamp < rhs.lastTouchTimestamp ? -1 : 1;
    }
}
