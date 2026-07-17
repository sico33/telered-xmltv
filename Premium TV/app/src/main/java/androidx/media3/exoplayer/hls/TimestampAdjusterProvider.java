package androidx.media3.exoplayer.hls;

import android.util.SparseArray;
import androidx.media3.common.util.TimestampAdjuster;

/* JADX INFO: loaded from: classes.dex */
public final class TimestampAdjusterProvider {
    private final SparseArray<TimestampAdjuster> timestampAdjusters = new SparseArray<>();

    public TimestampAdjuster getAdjuster(int discontinuitySequence) {
        TimestampAdjuster adjuster = this.timestampAdjusters.get(discontinuitySequence);
        if (adjuster == null) {
            TimestampAdjuster adjuster2 = new TimestampAdjuster(TimestampAdjuster.MODE_SHARED);
            this.timestampAdjusters.put(discontinuitySequence, adjuster2);
            return adjuster2;
        }
        return adjuster;
    }

    public void reset() {
        this.timestampAdjusters.clear();
    }
}
