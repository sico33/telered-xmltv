package androidx.media3.exoplayer.source;

import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.exoplayer.AbstractConcatenatedTimeline;
import androidx.media3.exoplayer.upstream.Allocator;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
@Deprecated
public final class LoopingMediaSource extends WrappingMediaSource {
    private final Map<MediaSource.MediaPeriodId, MediaSource.MediaPeriodId> childMediaPeriodIdToMediaPeriodId;
    private final int loopCount;
    private final Map<MediaPeriod, MediaSource.MediaPeriodId> mediaPeriodToChildMediaPeriodId;

    public LoopingMediaSource(MediaSource childSource) {
        this(childSource, Integer.MAX_VALUE);
    }

    public LoopingMediaSource(MediaSource childSource, int loopCount) {
        super(new MaskingMediaSource(childSource, false));
        Assertions.checkArgument(loopCount > 0);
        this.loopCount = loopCount;
        this.childMediaPeriodIdToMediaPeriodId = new HashMap();
        this.mediaPeriodToChildMediaPeriodId = new HashMap();
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource, androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public Timeline getInitialTimeline() {
        MaskingMediaSource maskingMediaSource = (MaskingMediaSource) this.mediaSource;
        if (this.loopCount != Integer.MAX_VALUE) {
            return new LoopingTimeline(maskingMediaSource.getTimeline(), this.loopCount);
        }
        return new InfinitelyLoopingTimeline(maskingMediaSource.getTimeline());
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource, androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public boolean isSingleWindow() {
        return false;
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource, androidx.media3.exoplayer.source.MediaSource
    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator, long startPositionUs) {
        if (this.loopCount == Integer.MAX_VALUE) {
            return this.mediaSource.createPeriod(id, allocator, startPositionUs);
        }
        Object childPeriodUid = LoopingTimeline.getChildPeriodUidFromConcatenatedUid(id.periodUid);
        MediaSource.MediaPeriodId childMediaPeriodId = id.copyWithPeriodUid(childPeriodUid);
        this.childMediaPeriodIdToMediaPeriodId.put(childMediaPeriodId, id);
        MediaPeriod mediaPeriod = this.mediaSource.createPeriod(childMediaPeriodId, allocator, startPositionUs);
        this.mediaPeriodToChildMediaPeriodId.put(mediaPeriod, childMediaPeriodId);
        return mediaPeriod;
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource, androidx.media3.exoplayer.source.MediaSource
    public void releasePeriod(MediaPeriod mediaPeriod) {
        this.mediaSource.releasePeriod(mediaPeriod);
        MediaSource.MediaPeriodId childMediaPeriodId = this.mediaPeriodToChildMediaPeriodId.remove(mediaPeriod);
        if (childMediaPeriodId != null) {
            this.childMediaPeriodIdToMediaPeriodId.remove(childMediaPeriodId);
        }
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource
    protected void onChildSourceInfoRefreshed(Timeline newTimeline) {
        Timeline loopingTimeline;
        if (this.loopCount != Integer.MAX_VALUE) {
            loopingTimeline = new LoopingTimeline(newTimeline, this.loopCount);
        } else {
            loopingTimeline = new InfinitelyLoopingTimeline(newTimeline);
        }
        refreshSourceInfo(loopingTimeline);
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource
    protected MediaSource.MediaPeriodId getMediaPeriodIdForChildMediaPeriodId(MediaSource.MediaPeriodId mediaPeriodId) {
        if (this.loopCount != Integer.MAX_VALUE) {
            return this.childMediaPeriodIdToMediaPeriodId.get(mediaPeriodId);
        }
        return mediaPeriodId;
    }

    private static final class LoopingTimeline extends AbstractConcatenatedTimeline {
        private final int childPeriodCount;
        private final Timeline childTimeline;
        private final int childWindowCount;
        private final int loopCount;

        public LoopingTimeline(Timeline childTimeline, int loopCount) {
            super(false, new ShuffleOrder.UnshuffledShuffleOrder(loopCount));
            this.childTimeline = childTimeline;
            this.childPeriodCount = childTimeline.getPeriodCount();
            this.childWindowCount = childTimeline.getWindowCount();
            this.loopCount = loopCount;
            if (this.childPeriodCount > 0) {
                Assertions.checkState(loopCount <= Integer.MAX_VALUE / this.childPeriodCount, "LoopingMediaSource contains too many periods");
            }
        }

        @Override // androidx.media3.common.Timeline
        public int getWindowCount() {
            return this.childWindowCount * this.loopCount;
        }

        @Override // androidx.media3.common.Timeline
        public int getPeriodCount() {
            return this.childPeriodCount * this.loopCount;
        }

        @Override // androidx.media3.exoplayer.AbstractConcatenatedTimeline
        protected int getChildIndexByPeriodIndex(int periodIndex) {
            return periodIndex / this.childPeriodCount;
        }

        @Override // androidx.media3.exoplayer.AbstractConcatenatedTimeline
        protected int getChildIndexByWindowIndex(int windowIndex) {
            return windowIndex / this.childWindowCount;
        }

        @Override // androidx.media3.exoplayer.AbstractConcatenatedTimeline
        protected int getChildIndexByChildUid(Object childUid) {
            if (!(childUid instanceof Integer)) {
                return -1;
            }
            return ((Integer) childUid).intValue();
        }

        @Override // androidx.media3.exoplayer.AbstractConcatenatedTimeline
        protected Timeline getTimelineByChildIndex(int childIndex) {
            return this.childTimeline;
        }

        @Override // androidx.media3.exoplayer.AbstractConcatenatedTimeline
        protected int getFirstPeriodIndexByChildIndex(int childIndex) {
            return this.childPeriodCount * childIndex;
        }

        @Override // androidx.media3.exoplayer.AbstractConcatenatedTimeline
        protected int getFirstWindowIndexByChildIndex(int childIndex) {
            return this.childWindowCount * childIndex;
        }

        @Override // androidx.media3.exoplayer.AbstractConcatenatedTimeline
        protected Object getChildUidByChildIndex(int childIndex) {
            return Integer.valueOf(childIndex);
        }
    }

    private static final class InfinitelyLoopingTimeline extends ForwardingTimeline {
        public InfinitelyLoopingTimeline(Timeline timeline) {
            super(timeline);
        }

        @Override // androidx.media3.exoplayer.source.ForwardingTimeline, androidx.media3.common.Timeline
        public int getNextWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
            int childNextWindowIndex = this.timeline.getNextWindowIndex(windowIndex, repeatMode, shuffleModeEnabled);
            if (childNextWindowIndex == -1) {
                return getFirstWindowIndex(shuffleModeEnabled);
            }
            return childNextWindowIndex;
        }

        @Override // androidx.media3.exoplayer.source.ForwardingTimeline, androidx.media3.common.Timeline
        public int getPreviousWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
            int childPreviousWindowIndex = this.timeline.getPreviousWindowIndex(windowIndex, repeatMode, shuffleModeEnabled);
            if (childPreviousWindowIndex == -1) {
                return getLastWindowIndex(shuffleModeEnabled);
            }
            return childPreviousWindowIndex;
        }
    }
}
