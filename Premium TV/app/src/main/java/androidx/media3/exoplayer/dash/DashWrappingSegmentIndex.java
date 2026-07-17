package androidx.media3.exoplayer.dash;

import androidx.media3.common.C;
import androidx.media3.exoplayer.dash.manifest.RangedUri;
import androidx.media3.extractor.ChunkIndex;

/* JADX INFO: loaded from: classes.dex */
public final class DashWrappingSegmentIndex implements DashSegmentIndex {
    private final ChunkIndex chunkIndex;
    private final long timeOffsetUs;

    public DashWrappingSegmentIndex(ChunkIndex chunkIndex, long timeOffsetUs) {
        this.chunkIndex = chunkIndex;
        this.timeOffsetUs = timeOffsetUs;
    }

    @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
    public long getFirstSegmentNum() {
        return 0L;
    }

    @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
    public long getFirstAvailableSegmentNum(long periodDurationUs, long nowUnixTimeUs) {
        return 0L;
    }

    @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
    public long getSegmentCount(long periodDurationUs) {
        return this.chunkIndex.length;
    }

    @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
    public long getAvailableSegmentCount(long periodDurationUs, long nowUnixTimeUs) {
        return this.chunkIndex.length;
    }

    @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
    public long getNextSegmentAvailableTimeUs(long periodDurationUs, long nowUnixTimeUs) {
        return C.TIME_UNSET;
    }

    @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
    public long getTimeUs(long segmentNum) {
        return this.chunkIndex.timesUs[(int) segmentNum] - this.timeOffsetUs;
    }

    @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
    public long getDurationUs(long segmentNum, long periodDurationUs) {
        return this.chunkIndex.durationsUs[(int) segmentNum];
    }

    @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
    public RangedUri getSegmentUrl(long segmentNum) {
        return new RangedUri(null, this.chunkIndex.offsets[(int) segmentNum], this.chunkIndex.sizes[(int) segmentNum]);
    }

    @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
    public long getSegmentNum(long timeUs, long periodDurationUs) {
        return this.chunkIndex.getChunkIndex(this.timeOffsetUs + timeUs);
    }

    @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
    public boolean isExplicit() {
        return true;
    }
}
