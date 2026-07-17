package androidx.media3.extractor;

import androidx.media3.common.util.Util;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class ChunkIndex implements SeekMap {
    private final long durationUs;
    public final long[] durationsUs;
    public final int length;
    public final long[] offsets;
    public final int[] sizes;
    public final long[] timesUs;

    public ChunkIndex(int[] sizes, long[] offsets, long[] durationsUs, long[] timesUs) {
        this.sizes = sizes;
        this.offsets = offsets;
        this.durationsUs = durationsUs;
        this.timesUs = timesUs;
        this.length = sizes.length;
        if (this.length > 0) {
            this.durationUs = durationsUs[this.length - 1] + timesUs[this.length - 1];
        } else {
            this.durationUs = 0L;
        }
    }

    public int getChunkIndex(long timeUs) {
        return Util.binarySearchFloor(this.timesUs, timeUs, true, true);
    }

    @Override // androidx.media3.extractor.SeekMap
    public boolean isSeekable() {
        return true;
    }

    @Override // androidx.media3.extractor.SeekMap
    public long getDurationUs() {
        return this.durationUs;
    }

    @Override // androidx.media3.extractor.SeekMap
    public SeekMap.SeekPoints getSeekPoints(long timeUs) {
        int chunkIndex = getChunkIndex(timeUs);
        SeekPoint seekPoint = new SeekPoint(this.timesUs[chunkIndex], this.offsets[chunkIndex]);
        if (seekPoint.timeUs >= timeUs || chunkIndex == this.length - 1) {
            return new SeekMap.SeekPoints(seekPoint);
        }
        SeekPoint nextSeekPoint = new SeekPoint(this.timesUs[chunkIndex + 1], this.offsets[chunkIndex + 1]);
        return new SeekMap.SeekPoints(seekPoint, nextSeekPoint);
    }

    public String toString() {
        return "ChunkIndex(length=" + this.length + ", sizes=" + Arrays.toString(this.sizes) + ", offsets=" + Arrays.toString(this.offsets) + ", timeUs=" + Arrays.toString(this.timesUs) + ", durationsUs=" + Arrays.toString(this.durationsUs) + ")";
    }
}
