package androidx.media3.exoplayer.source.ads;

import androidx.media3.common.AdPlaybackState;
import androidx.media3.common.C;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.exoplayer.source.ForwardingTimeline;

/* JADX INFO: loaded from: classes.dex */
public final class SinglePeriodAdTimeline extends ForwardingTimeline {
    private final AdPlaybackState adPlaybackState;

    public SinglePeriodAdTimeline(Timeline contentTimeline, AdPlaybackState adPlaybackState) {
        super(contentTimeline);
        Assertions.checkState(contentTimeline.getPeriodCount() == 1);
        Assertions.checkState(contentTimeline.getWindowCount() == 1);
        this.adPlaybackState = adPlaybackState;
    }

    @Override // androidx.media3.exoplayer.source.ForwardingTimeline, androidx.media3.common.Timeline
    public Timeline.Period getPeriod(int periodIndex, Timeline.Period period, boolean setIds) {
        this.timeline.getPeriod(periodIndex, period, setIds);
        long durationUs = period.durationUs == C.TIME_UNSET ? this.adPlaybackState.contentDurationUs : period.durationUs;
        period.set(period.id, period.uid, period.windowIndex, durationUs, period.getPositionInWindowUs(), this.adPlaybackState, period.isPlaceholder);
        return period;
    }
}
