package androidx.media3.common.util;

import androidx.media3.common.C;

/* JADX INFO: loaded from: classes.dex */
public final class ConstantRateTimestampIterator implements TimestampIterator {
    private final long endPositionUs;
    private final float frameRate;
    private int framesAdded;
    private final double framesDurationUs;
    private final long startPositionUs;
    private final int totalNumberOfFramesToAdd;

    public ConstantRateTimestampIterator(long durationUs, float frameRate) {
        this(0L, durationUs, frameRate);
    }

    public ConstantRateTimestampIterator(long startPositionUs, long endPositionUs, float frameRate) {
        Assertions.checkArgument(endPositionUs > 0);
        Assertions.checkArgument(frameRate > 0.0f);
        Assertions.checkArgument(0 <= startPositionUs && startPositionUs < endPositionUs);
        this.startPositionUs = startPositionUs;
        this.endPositionUs = endPositionUs;
        this.frameRate = frameRate;
        float durationSecs = (endPositionUs - startPositionUs) / 1000000.0f;
        this.totalNumberOfFramesToAdd = Math.round(frameRate * durationSecs);
        this.framesDurationUs = 1000000.0f / frameRate;
    }

    @Override // androidx.media3.common.util.TimestampIterator
    public boolean hasNext() {
        return this.framesAdded < this.totalNumberOfFramesToAdd;
    }

    @Override // androidx.media3.common.util.TimestampIterator
    public long next() {
        Assertions.checkState(hasNext());
        int i = this.framesAdded;
        this.framesAdded = i + 1;
        return getTimestampUsAfter(i);
    }

    @Override // androidx.media3.common.util.TimestampIterator
    public ConstantRateTimestampIterator copyOf() {
        return new ConstantRateTimestampIterator(this.startPositionUs, this.endPositionUs, this.frameRate);
    }

    @Override // androidx.media3.common.util.TimestampIterator
    public long getLastTimestampUs() {
        if (this.totalNumberOfFramesToAdd == 0) {
            return C.TIME_UNSET;
        }
        return getTimestampUsAfter(this.totalNumberOfFramesToAdd - 1);
    }

    private long getTimestampUsAfter(int numberOfFrames) {
        long timestampUs = this.startPositionUs + Math.round(this.framesDurationUs * ((double) numberOfFrames));
        Assertions.checkState(timestampUs >= 0);
        return timestampUs;
    }
}
