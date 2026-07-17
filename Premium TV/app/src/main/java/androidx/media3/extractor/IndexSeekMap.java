package androidx.media3.extractor;

import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public final class IndexSeekMap implements SeekMap {
    private final long durationUs;
    private final boolean isSeekable;
    private final long[] positions;
    private final long[] timesUs;

    public IndexSeekMap(long[] positions, long[] timesUs, long durationUs) {
        Assertions.checkArgument(positions.length == timesUs.length);
        int length = timesUs.length;
        this.isSeekable = length > 0;
        if (this.isSeekable && timesUs[0] > 0) {
            this.positions = new long[length + 1];
            this.timesUs = new long[length + 1];
            System.arraycopy(positions, 0, this.positions, 1, length);
            System.arraycopy(timesUs, 0, this.timesUs, 1, length);
        } else {
            this.positions = positions;
            this.timesUs = timesUs;
        }
        this.durationUs = durationUs;
    }

    @Override // androidx.media3.extractor.SeekMap
    public boolean isSeekable() {
        return this.isSeekable;
    }

    @Override // androidx.media3.extractor.SeekMap
    public long getDurationUs() {
        return this.durationUs;
    }

    @Override // androidx.media3.extractor.SeekMap
    public SeekMap.SeekPoints getSeekPoints(long timeUs) {
        if (!this.isSeekable) {
            return new SeekMap.SeekPoints(SeekPoint.START);
        }
        int targetIndex = Util.binarySearchFloor(this.timesUs, timeUs, true, true);
        SeekPoint leftSeekPoint = new SeekPoint(this.timesUs[targetIndex], this.positions[targetIndex]);
        if (leftSeekPoint.timeUs == timeUs || targetIndex == this.timesUs.length - 1) {
            return new SeekMap.SeekPoints(leftSeekPoint);
        }
        SeekPoint rightSeekPoint = new SeekPoint(this.timesUs[targetIndex + 1], this.positions[targetIndex + 1]);
        return new SeekMap.SeekPoints(leftSeekPoint, rightSeekPoint);
    }
}
