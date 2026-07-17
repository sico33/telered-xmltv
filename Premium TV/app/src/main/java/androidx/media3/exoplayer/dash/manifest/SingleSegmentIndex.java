package androidx.media3.exoplayer.dash.manifest;

import androidx.media3.common.C;
import androidx.media3.exoplayer.dash.DashSegmentIndex;

/* JADX INFO: loaded from: classes.dex */
final class SingleSegmentIndex implements DashSegmentIndex {
    private final RangedUri uri;

    public SingleSegmentIndex(RangedUri uri) {
        this.uri = uri;
    }

    @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
    public long getSegmentNum(long timeUs, long periodDurationUs) {
        return 0L;
    }

    @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
    public long getTimeUs(long segmentNum) {
        return 0L;
    }

    @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
    public long getDurationUs(long segmentNum, long periodDurationUs) {
        return periodDurationUs;
    }

    @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
    public RangedUri getSegmentUrl(long segmentNum) {
        return this.uri;
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
        return 1L;
    }

    @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
    public long getAvailableSegmentCount(long periodDurationUs, long nowUnixTimeUs) {
        return 1L;
    }

    @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
    public long getNextSegmentAvailableTimeUs(long periodDurationUs, long nowUnixTimeUs) {
        return C.TIME_UNSET;
    }

    @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
    public boolean isExplicit() {
        return true;
    }
}
