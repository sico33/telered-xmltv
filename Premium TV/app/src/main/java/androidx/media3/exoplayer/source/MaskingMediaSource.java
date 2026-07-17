package androidx.media3.exoplayer.source;

import android.util.Pair;
import androidx.media3.common.AdPlaybackState;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.upstream.Allocator;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class MaskingMediaSource extends WrappingMediaSource {
    private boolean hasRealTimeline;
    private boolean hasStartedPreparing;
    private boolean isPrepared;
    private final Timeline.Period period;
    private MaskingTimeline timeline;
    private MaskingMediaPeriod unpreparedMaskingMediaPeriod;
    private final boolean useLazyPreparation;
    private final Timeline.Window window;

    public MaskingMediaSource(MediaSource mediaSource, boolean useLazyPreparation) {
        super(mediaSource);
        this.useLazyPreparation = useLazyPreparation && mediaSource.isSingleWindow();
        this.window = new Timeline.Window();
        this.period = new Timeline.Period();
        Timeline initialTimeline = mediaSource.getInitialTimeline();
        if (initialTimeline != null) {
            this.timeline = MaskingTimeline.createWithRealTimeline(initialTimeline, null, null);
            this.hasRealTimeline = true;
        } else {
            this.timeline = MaskingTimeline.createWithPlaceholderTimeline(mediaSource.getMediaItem());
        }
    }

    public Timeline getTimeline() {
        return this.timeline;
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource, androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public boolean canUpdateMediaItem(MediaItem mediaItem) {
        return this.mediaSource.canUpdateMediaItem(mediaItem);
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource, androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public void updateMediaItem(MediaItem mediaItem) {
        if (this.hasRealTimeline) {
            this.timeline = this.timeline.cloneWithUpdatedTimeline(new TimelineWithUpdatedMediaItem(this.timeline.timeline, mediaItem));
        } else {
            this.timeline = MaskingTimeline.createWithPlaceholderTimeline(mediaItem);
        }
        this.mediaSource.updateMediaItem(mediaItem);
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource
    public void prepareSourceInternal() {
        if (!this.useLazyPreparation) {
            this.hasStartedPreparing = true;
            prepareChildSource();
        }
    }

    @Override // androidx.media3.exoplayer.source.CompositeMediaSource, androidx.media3.exoplayer.source.MediaSource
    public void maybeThrowSourceInfoRefreshError() {
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource, androidx.media3.exoplayer.source.MediaSource
    public MaskingMediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator, long startPositionUs) {
        MaskingMediaPeriod mediaPeriod = new MaskingMediaPeriod(id, allocator, startPositionUs);
        mediaPeriod.setMediaSource(this.mediaSource);
        if (this.isPrepared) {
            MediaSource.MediaPeriodId idInSource = id.copyWithPeriodUid(getInternalPeriodUid(id.periodUid));
            mediaPeriod.createPeriod(idInSource);
        } else {
            this.unpreparedMaskingMediaPeriod = mediaPeriod;
            if (!this.hasStartedPreparing) {
                this.hasStartedPreparing = true;
                prepareChildSource();
            }
        }
        return mediaPeriod;
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource, androidx.media3.exoplayer.source.MediaSource
    public void releasePeriod(MediaPeriod mediaPeriod) {
        ((MaskingMediaPeriod) mediaPeriod).releasePeriod();
        if (mediaPeriod == this.unpreparedMaskingMediaPeriod) {
            this.unpreparedMaskingMediaPeriod = null;
        }
    }

    @Override // androidx.media3.exoplayer.source.CompositeMediaSource, androidx.media3.exoplayer.source.BaseMediaSource
    public void releaseSourceInternal() {
        this.isPrepared = false;
        this.hasStartedPreparing = false;
        super.releaseSourceInternal();
    }

    /* JADX WARN: Code duplicated, block: B:19:0x007b  */
    @Override // androidx.media3.exoplayer.source.WrappingMediaSource
    protected void onChildSourceInfoRefreshed(Timeline newTimeline) {
        long windowStartPositionUs;
        MaskingTimeline maskingTimelineCreateWithRealTimeline;
        MaskingTimeline maskingTimelineCreateWithRealTimeline2;
        MediaSource.MediaPeriodId idForMaskingPeriodPreparation = null;
        if (this.isPrepared) {
            this.timeline = this.timeline.cloneWithUpdatedTimeline(newTimeline);
            if (this.unpreparedMaskingMediaPeriod != null) {
                setPreparePositionOverrideToUnpreparedMaskingPeriod(this.unpreparedMaskingMediaPeriod.getPreparePositionOverrideUs());
            }
        } else if (newTimeline.isEmpty()) {
            if (this.hasRealTimeline) {
                maskingTimelineCreateWithRealTimeline2 = this.timeline.cloneWithUpdatedTimeline(newTimeline);
            } else {
                maskingTimelineCreateWithRealTimeline2 = MaskingTimeline.createWithRealTimeline(newTimeline, Timeline.Window.SINGLE_WINDOW_UID, MaskingTimeline.MASKING_EXTERNAL_PERIOD_UID);
            }
            this.timeline = maskingTimelineCreateWithRealTimeline2;
        } else {
            newTimeline.getWindow(0, this.window);
            long windowStartPositionUs2 = this.window.getDefaultPositionUs();
            Object windowUid = this.window.uid;
            if (this.unpreparedMaskingMediaPeriod != null) {
                long periodPreparePositionUs = this.unpreparedMaskingMediaPeriod.getPreparePositionUs();
                this.timeline.getPeriodByUid(this.unpreparedMaskingMediaPeriod.id.periodUid, this.period);
                long windowPreparePositionUs = this.period.getPositionInWindowUs() + periodPreparePositionUs;
                long oldWindowDefaultPositionUs = this.timeline.getWindow(0, this.window).getDefaultPositionUs();
                if (windowPreparePositionUs != oldWindowDefaultPositionUs) {
                    windowStartPositionUs = windowPreparePositionUs;
                } else {
                    windowStartPositionUs = windowStartPositionUs2;
                }
            } else {
                windowStartPositionUs = windowStartPositionUs2;
            }
            Pair<Object, Long> periodUidAndPositionUs = newTimeline.getPeriodPositionUs(this.window, this.period, 0, windowStartPositionUs);
            Object periodUid = periodUidAndPositionUs.first;
            long periodPositionUs = ((Long) periodUidAndPositionUs.second).longValue();
            if (this.hasRealTimeline) {
                maskingTimelineCreateWithRealTimeline = this.timeline.cloneWithUpdatedTimeline(newTimeline);
            } else {
                maskingTimelineCreateWithRealTimeline = MaskingTimeline.createWithRealTimeline(newTimeline, windowUid, periodUid);
            }
            this.timeline = maskingTimelineCreateWithRealTimeline;
            if (this.unpreparedMaskingMediaPeriod != null) {
                MaskingMediaPeriod maskingPeriod = this.unpreparedMaskingMediaPeriod;
                if (setPreparePositionOverrideToUnpreparedMaskingPeriod(periodPositionUs)) {
                    idForMaskingPeriodPreparation = maskingPeriod.id.copyWithPeriodUid(getInternalPeriodUid(maskingPeriod.id.periodUid));
                }
            }
        }
        this.hasRealTimeline = true;
        this.isPrepared = true;
        refreshSourceInfo(this.timeline);
        if (idForMaskingPeriodPreparation != null) {
            ((MaskingMediaPeriod) Assertions.checkNotNull(this.unpreparedMaskingMediaPeriod)).createPeriod(idForMaskingPeriodPreparation);
        }
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource
    protected MediaSource.MediaPeriodId getMediaPeriodIdForChildMediaPeriodId(MediaSource.MediaPeriodId mediaPeriodId) {
        return mediaPeriodId.copyWithPeriodUid(getExternalPeriodUid(mediaPeriodId.periodUid));
    }

    private Object getInternalPeriodUid(Object externalPeriodUid) {
        if (this.timeline.replacedInternalPeriodUid == null || !externalPeriodUid.equals(MaskingTimeline.MASKING_EXTERNAL_PERIOD_UID)) {
            return externalPeriodUid;
        }
        return this.timeline.replacedInternalPeriodUid;
    }

    private Object getExternalPeriodUid(Object internalPeriodUid) {
        if (this.timeline.replacedInternalPeriodUid != null && this.timeline.replacedInternalPeriodUid.equals(internalPeriodUid)) {
            return MaskingTimeline.MASKING_EXTERNAL_PERIOD_UID;
        }
        return internalPeriodUid;
    }

    @RequiresNonNull({"unpreparedMaskingMediaPeriod"})
    private boolean setPreparePositionOverrideToUnpreparedMaskingPeriod(long preparePositionOverrideUs) {
        MaskingMediaPeriod maskingPeriod = this.unpreparedMaskingMediaPeriod;
        int maskingPeriodIndex = this.timeline.getIndexOfPeriod(maskingPeriod.id.periodUid);
        if (maskingPeriodIndex == -1) {
            return false;
        }
        long periodDurationUs = this.timeline.getPeriod(maskingPeriodIndex, this.period).durationUs;
        if (periodDurationUs != C.TIME_UNSET && preparePositionOverrideUs >= periodDurationUs) {
            preparePositionOverrideUs = Math.max(0L, periodDurationUs - 1);
        }
        maskingPeriod.overridePreparePositionUs(preparePositionOverrideUs);
        return true;
    }

    private static final class MaskingTimeline extends ForwardingTimeline {
        public static final Object MASKING_EXTERNAL_PERIOD_UID = new Object();
        private final Object replacedInternalPeriodUid;
        private final Object replacedInternalWindowUid;

        public static MaskingTimeline createWithPlaceholderTimeline(MediaItem mediaItem) {
            return new MaskingTimeline(new PlaceholderTimeline(mediaItem), Timeline.Window.SINGLE_WINDOW_UID, MASKING_EXTERNAL_PERIOD_UID);
        }

        public static MaskingTimeline createWithRealTimeline(Timeline timeline, Object firstWindowUid, Object firstPeriodUid) {
            return new MaskingTimeline(timeline, firstWindowUid, firstPeriodUid);
        }

        private MaskingTimeline(Timeline timeline, Object replacedInternalWindowUid, Object replacedInternalPeriodUid) {
            super(timeline);
            this.replacedInternalWindowUid = replacedInternalWindowUid;
            this.replacedInternalPeriodUid = replacedInternalPeriodUid;
        }

        public MaskingTimeline cloneWithUpdatedTimeline(Timeline timeline) {
            return new MaskingTimeline(timeline, this.replacedInternalWindowUid, this.replacedInternalPeriodUid);
        }

        @Override // androidx.media3.exoplayer.source.ForwardingTimeline, androidx.media3.common.Timeline
        public Timeline.Window getWindow(int windowIndex, Timeline.Window window, long defaultPositionProjectionUs) {
            this.timeline.getWindow(windowIndex, window, defaultPositionProjectionUs);
            if (Util.areEqual(window.uid, this.replacedInternalWindowUid)) {
                window.uid = Timeline.Window.SINGLE_WINDOW_UID;
            }
            return window;
        }

        @Override // androidx.media3.exoplayer.source.ForwardingTimeline, androidx.media3.common.Timeline
        public Timeline.Period getPeriod(int periodIndex, Timeline.Period period, boolean setIds) {
            this.timeline.getPeriod(periodIndex, period, setIds);
            if (Util.areEqual(period.uid, this.replacedInternalPeriodUid) && setIds) {
                period.uid = MASKING_EXTERNAL_PERIOD_UID;
            }
            return period;
        }

        @Override // androidx.media3.exoplayer.source.ForwardingTimeline, androidx.media3.common.Timeline
        public int getIndexOfPeriod(Object uid) {
            Object obj;
            Timeline timeline = this.timeline;
            if (MASKING_EXTERNAL_PERIOD_UID.equals(uid) && this.replacedInternalPeriodUid != null) {
                obj = this.replacedInternalPeriodUid;
            } else {
                obj = uid;
            }
            return timeline.getIndexOfPeriod(obj);
        }

        @Override // androidx.media3.exoplayer.source.ForwardingTimeline, androidx.media3.common.Timeline
        public Object getUidOfPeriod(int periodIndex) {
            Object uid = this.timeline.getUidOfPeriod(periodIndex);
            return Util.areEqual(uid, this.replacedInternalPeriodUid) ? MASKING_EXTERNAL_PERIOD_UID : uid;
        }
    }

    public static final class PlaceholderTimeline extends Timeline {
        private final MediaItem mediaItem;

        public PlaceholderTimeline(MediaItem mediaItem) {
            this.mediaItem = mediaItem;
        }

        @Override // androidx.media3.common.Timeline
        public int getWindowCount() {
            return 1;
        }

        @Override // androidx.media3.common.Timeline
        public Timeline.Window getWindow(int windowIndex, Timeline.Window window, long defaultPositionProjectionUs) {
            window.set(Timeline.Window.SINGLE_WINDOW_UID, this.mediaItem, null, C.TIME_UNSET, C.TIME_UNSET, C.TIME_UNSET, false, true, null, 0L, C.TIME_UNSET, 0, 0, 0L);
            window.isPlaceholder = true;
            return window;
        }

        @Override // androidx.media3.common.Timeline
        public int getPeriodCount() {
            return 1;
        }

        @Override // androidx.media3.common.Timeline
        public Timeline.Period getPeriod(int periodIndex, Timeline.Period period, boolean setIds) {
            period.set(setIds ? 0 : null, setIds ? MaskingTimeline.MASKING_EXTERNAL_PERIOD_UID : null, 0, C.TIME_UNSET, 0L, AdPlaybackState.NONE, true);
            return period;
        }

        @Override // androidx.media3.common.Timeline
        public int getIndexOfPeriod(Object uid) {
            return uid == MaskingTimeline.MASKING_EXTERNAL_PERIOD_UID ? 0 : -1;
        }

        @Override // androidx.media3.common.Timeline
        public Object getUidOfPeriod(int periodIndex) {
            return MaskingTimeline.MASKING_EXTERNAL_PERIOD_UID;
        }
    }
}
