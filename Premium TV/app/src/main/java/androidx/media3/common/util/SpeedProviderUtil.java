package androidx.media3.common.util;

import androidx.media3.common.C;
import androidx.media3.common.audio.SpeedProvider;

/* JADX INFO: loaded from: classes.dex */
public class SpeedProviderUtil {
    private SpeedProviderUtil() {
    }

    public static long getDurationAfterSpeedProviderApplied(SpeedProvider speedProvider, long durationUs) {
        long speedChangeTimeUs = 0;
        double outputDurationUs = 0.0d;
        while (speedChangeTimeUs < durationUs) {
            long nextSpeedChangeTimeUs = speedProvider.getNextSpeedChangeTimeUs(speedChangeTimeUs);
            if (nextSpeedChangeTimeUs == C.TIME_UNSET) {
                nextSpeedChangeTimeUs = Long.MAX_VALUE;
            }
            outputDurationUs += (Math.min(nextSpeedChangeTimeUs, durationUs) - speedChangeTimeUs) / ((double) speedProvider.getSpeed(speedChangeTimeUs));
            speedChangeTimeUs = nextSpeedChangeTimeUs;
        }
        return Math.round(outputDurationUs);
    }
}
