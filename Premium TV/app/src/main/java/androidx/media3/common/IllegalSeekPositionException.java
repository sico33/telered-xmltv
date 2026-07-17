package androidx.media3.common;

/* JADX INFO: loaded from: classes.dex */
public final class IllegalSeekPositionException extends IllegalStateException {
    public final long positionMs;
    public final Timeline timeline;
    public final int windowIndex;

    public IllegalSeekPositionException(Timeline timeline, int windowIndex, long positionMs) {
        this.timeline = timeline;
        this.windowIndex = windowIndex;
        this.positionMs = positionMs;
    }
}
