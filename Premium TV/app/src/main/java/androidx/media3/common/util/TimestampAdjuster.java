package androidx.media3.common.util;

import androidx.media3.common.C;
import java.util.concurrent.TimeoutException;

/* JADX INFO: loaded from: classes.dex */
public final class TimestampAdjuster {
    private static final long MAX_PTS_PLUS_ONE = 8589934592L;
    public static final long MODE_NO_OFFSET = Long.MAX_VALUE;
    public static final long MODE_SHARED = 9223372036854775806L;
    private long firstSampleTimestampUs;
    private long lastUnadjustedTimestampUs;
    private final ThreadLocal<Long> nextSampleTimestampUs = new ThreadLocal<>();
    private long timestampOffsetUs;

    public TimestampAdjuster(long firstSampleTimestampUs) {
        reset(firstSampleTimestampUs);
    }

    public synchronized void sharedInitializeOrWait(boolean canInitialize, long nextSampleTimestampUs, long timeoutMs) throws InterruptedException, TimeoutException {
        Assertions.checkState(this.firstSampleTimestampUs == MODE_SHARED);
        if (isInitialized()) {
            return;
        }
        if (canInitialize) {
            this.nextSampleTimestampUs.set(Long.valueOf(nextSampleTimestampUs));
        } else {
            long totalWaitDurationMs = 0;
            long remainingTimeoutMs = timeoutMs;
            while (!isInitialized()) {
                if (timeoutMs == 0) {
                    wait();
                } else {
                    Assertions.checkState(remainingTimeoutMs > 0);
                    long waitStartingTimeMs = android.os.SystemClock.elapsedRealtime();
                    wait(remainingTimeoutMs);
                    totalWaitDurationMs += android.os.SystemClock.elapsedRealtime() - waitStartingTimeMs;
                    if (totalWaitDurationMs >= timeoutMs && !isInitialized()) {
                        String message = "TimestampAdjuster failed to initialize in " + timeoutMs + " milliseconds";
                        throw new TimeoutException(message);
                    }
                    remainingTimeoutMs = timeoutMs - totalWaitDurationMs;
                }
            }
        }
    }

    public synchronized long getFirstSampleTimestampUs() {
        long j;
        if (this.firstSampleTimestampUs == Long.MAX_VALUE || this.firstSampleTimestampUs == MODE_SHARED) {
            j = C.TIME_UNSET;
        } else {
            j = this.firstSampleTimestampUs;
        }
        return j;
    }

    public synchronized long getLastAdjustedTimestampUs() {
        long firstSampleTimestampUs;
        if (this.lastUnadjustedTimestampUs != C.TIME_UNSET) {
            firstSampleTimestampUs = this.lastUnadjustedTimestampUs + this.timestampOffsetUs;
        } else {
            firstSampleTimestampUs = getFirstSampleTimestampUs();
        }
        return firstSampleTimestampUs;
    }

    public synchronized long getTimestampOffsetUs() {
        return this.timestampOffsetUs;
    }

    public synchronized void reset(long firstSampleTimestampUs) {
        this.firstSampleTimestampUs = firstSampleTimestampUs;
        this.timestampOffsetUs = firstSampleTimestampUs == Long.MAX_VALUE ? 0L : -9223372036854775807L;
        this.lastUnadjustedTimestampUs = C.TIME_UNSET;
    }

    public synchronized long adjustTsTimestamp(long pts90Khz) {
        long j;
        if (pts90Khz == C.TIME_UNSET) {
            return C.TIME_UNSET;
        }
        if (this.lastUnadjustedTimestampUs != C.TIME_UNSET) {
            long lastPts = usToNonWrappedPts(this.lastUnadjustedTimestampUs);
            long closestWrapCount = (4294967296L + lastPts) / MAX_PTS_PLUS_ONE;
            long ptsWrapBelow = ((closestWrapCount - 1) * MAX_PTS_PLUS_ONE) + pts90Khz;
            long ptsWrapAbove = (MAX_PTS_PLUS_ONE * closestWrapCount) + pts90Khz;
            if (Math.abs(ptsWrapBelow - lastPts) < Math.abs(ptsWrapAbove - lastPts)) {
                j = ptsWrapBelow;
            } else {
                j = ptsWrapAbove;
            }
            pts90Khz = j;
        }
        return adjustSampleTimestamp(ptsToUs(pts90Khz));
    }

    public synchronized long adjustTsTimestampGreaterThanPreviousTimestamp(long pts90Khz) {
        if (pts90Khz == C.TIME_UNSET) {
            return C.TIME_UNSET;
        }
        if (this.lastUnadjustedTimestampUs != C.TIME_UNSET) {
            long lastPts = usToNonWrappedPts(this.lastUnadjustedTimestampUs);
            long wrapCount = lastPts / MAX_PTS_PLUS_ONE;
            Long.signum(wrapCount);
            long ptsSameWrap = (wrapCount * MAX_PTS_PLUS_ONE) + pts90Khz;
            long ptsNextWrap = ((1 + wrapCount) * MAX_PTS_PLUS_ONE) + pts90Khz;
            pts90Khz = ptsSameWrap >= lastPts ? ptsSameWrap : ptsNextWrap;
        }
        return adjustSampleTimestamp(ptsToUs(pts90Khz));
    }

    public synchronized long adjustSampleTimestamp(long timeUs) {
        long desiredSampleTimestampUs;
        if (timeUs == C.TIME_UNSET) {
            return C.TIME_UNSET;
        }
        if (!isInitialized()) {
            if (this.firstSampleTimestampUs == MODE_SHARED) {
                desiredSampleTimestampUs = ((Long) Assertions.checkNotNull(this.nextSampleTimestampUs.get())).longValue();
            } else {
                desiredSampleTimestampUs = this.firstSampleTimestampUs;
            }
            this.timestampOffsetUs = desiredSampleTimestampUs - timeUs;
            notifyAll();
        }
        this.lastUnadjustedTimestampUs = timeUs;
        return this.timestampOffsetUs + timeUs;
    }

    public synchronized boolean isInitialized() {
        return this.timestampOffsetUs != C.TIME_UNSET;
    }

    public static long ptsToUs(long pts) {
        return (1000000 * pts) / 90000;
    }

    public static long usToWrappedPts(long us) {
        return usToNonWrappedPts(us) % MAX_PTS_PLUS_ONE;
    }

    public static long usToNonWrappedPts(long us) {
        return (90000 * us) / 1000000;
    }
}
