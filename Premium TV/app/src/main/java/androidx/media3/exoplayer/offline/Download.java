package androidx.media3.exoplayer.offline;

import androidx.media3.common.util.Assertions;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: classes.dex */
public final class Download {
    public static final int FAILURE_REASON_NONE = 0;
    public static final int FAILURE_REASON_UNKNOWN = 1;
    public static final int STATE_COMPLETED = 3;
    public static final int STATE_DOWNLOADING = 2;
    public static final int STATE_FAILED = 4;
    public static final int STATE_QUEUED = 0;
    public static final int STATE_REMOVING = 5;
    public static final int STATE_RESTARTING = 7;
    public static final int STATE_STOPPED = 1;
    public static final int STOP_REASON_NONE = 0;
    public final long contentLength;
    public final int failureReason;
    final DownloadProgress progress;
    public final DownloadRequest request;
    public final long startTimeMs;
    public final int state;
    public final int stopReason;
    public final long updateTimeMs;

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface FailureReason {
    }

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
    }

    public Download(DownloadRequest request, int state, long startTimeMs, long updateTimeMs, long contentLength, int stopReason, int failureReason) {
        this(request, state, startTimeMs, updateTimeMs, contentLength, stopReason, failureReason, new DownloadProgress());
    }

    public Download(DownloadRequest request, int state, long startTimeMs, long updateTimeMs, long contentLength, int stopReason, int failureReason, DownloadProgress progress) {
        Assertions.checkNotNull(progress);
        Assertions.checkArgument((failureReason == 0) == (state != 4));
        if (stopReason != 0) {
            Assertions.checkArgument((state == 2 || state == 0) ? false : true);
        }
        this.request = request;
        this.state = state;
        this.startTimeMs = startTimeMs;
        this.updateTimeMs = updateTimeMs;
        this.contentLength = contentLength;
        this.stopReason = stopReason;
        this.failureReason = failureReason;
        this.progress = progress;
    }

    public boolean isTerminalState() {
        return this.state == 3 || this.state == 4;
    }

    public long getBytesDownloaded() {
        return this.progress.bytesDownloaded;
    }

    public float getPercentDownloaded() {
        return this.progress.percentDownloaded;
    }
}
