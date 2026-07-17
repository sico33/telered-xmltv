package androidx.media3.exoplayer.source;

import androidx.media3.common.Timeline;

/* JADX INFO: loaded from: classes.dex */
public abstract class ForwardingTimeline extends Timeline {
    protected final Timeline timeline;

    public ForwardingTimeline(Timeline timeline) {
        this.timeline = timeline;
    }

    @Override // androidx.media3.common.Timeline
    public int getWindowCount() {
        return this.timeline.getWindowCount();
    }

    @Override // androidx.media3.common.Timeline
    public int getNextWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
        return this.timeline.getNextWindowIndex(windowIndex, repeatMode, shuffleModeEnabled);
    }

    @Override // androidx.media3.common.Timeline
    public int getPreviousWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
        return this.timeline.getPreviousWindowIndex(windowIndex, repeatMode, shuffleModeEnabled);
    }

    @Override // androidx.media3.common.Timeline
    public int getLastWindowIndex(boolean shuffleModeEnabled) {
        return this.timeline.getLastWindowIndex(shuffleModeEnabled);
    }

    @Override // androidx.media3.common.Timeline
    public int getFirstWindowIndex(boolean shuffleModeEnabled) {
        return this.timeline.getFirstWindowIndex(shuffleModeEnabled);
    }

    @Override // androidx.media3.common.Timeline
    public Timeline.Window getWindow(int windowIndex, Timeline.Window window, long defaultPositionProjectionUs) {
        return this.timeline.getWindow(windowIndex, window, defaultPositionProjectionUs);
    }

    @Override // androidx.media3.common.Timeline
    public int getPeriodCount() {
        return this.timeline.getPeriodCount();
    }

    @Override // androidx.media3.common.Timeline
    public Timeline.Period getPeriod(int periodIndex, Timeline.Period period, boolean setIds) {
        return this.timeline.getPeriod(periodIndex, period, setIds);
    }

    @Override // androidx.media3.common.Timeline
    public int getIndexOfPeriod(Object uid) {
        return this.timeline.getIndexOfPeriod(uid);
    }

    @Override // androidx.media3.common.Timeline
    public Object getUidOfPeriod(int periodIndex) {
        return this.timeline.getUidOfPeriod(periodIndex);
    }
}
