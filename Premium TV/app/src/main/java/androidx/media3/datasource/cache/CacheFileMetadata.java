package androidx.media3.datasource.cache;

/* JADX INFO: loaded from: classes.dex */
final class CacheFileMetadata {
    public final long lastTouchTimestamp;
    public final long length;

    public CacheFileMetadata(long length, long lastTouchTimestamp) {
        this.length = length;
        this.lastTouchTimestamp = lastTouchTimestamp;
    }
}
