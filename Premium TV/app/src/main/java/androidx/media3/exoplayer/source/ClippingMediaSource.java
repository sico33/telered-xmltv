package androidx.media3.exoplayer.source;

import androidx.core.os.EnvironmentCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.upstream.Allocator;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public final class ClippingMediaSource extends WrappingMediaSource {
    private final boolean allowDynamicClippingUpdates;
    private IllegalClippingException clippingError;
    private ClippingTimeline clippingTimeline;
    private final boolean enableInitialDiscontinuity;
    private final long endUs;
    private final ArrayList<ClippingMediaPeriod> mediaPeriods;
    private long periodEndUs;
    private long periodStartUs;
    private final boolean relativeToDefaultPosition;
    private final long startUs;
    private final Timeline.Window window;

    public static final class IllegalClippingException extends IOException {
        public static final int REASON_INVALID_PERIOD_COUNT = 0;
        public static final int REASON_NOT_SEEKABLE_TO_START = 1;
        public static final int REASON_START_EXCEEDS_END = 2;
        public final int reason;

        @Target({ElementType.TYPE_USE})
        @Documented
        @Retention(RetentionPolicy.SOURCE)
        public @interface Reason {
        }

        public IllegalClippingException(int reason) {
            super("Illegal clipping: " + getReasonDescription(reason));
            this.reason = reason;
        }

        private static String getReasonDescription(int reason) {
            switch (reason) {
                case 0:
                    return "invalid period count";
                case 1:
                    return "not seekable to start";
                case 2:
                    return "start exceeds end";
                default:
                    return EnvironmentCompat.MEDIA_UNKNOWN;
            }
        }
    }

    public ClippingMediaSource(MediaSource mediaSource, long startPositionUs, long endPositionUs) {
        this(mediaSource, startPositionUs, endPositionUs, true, false, false);
    }

    public ClippingMediaSource(MediaSource mediaSource, long durationUs) {
        this(mediaSource, 0L, durationUs, true, false, true);
    }

    public ClippingMediaSource(MediaSource mediaSource, long startPositionUs, long endPositionUs, boolean enableInitialDiscontinuity, boolean allowDynamicClippingUpdates, boolean relativeToDefaultPosition) {
        super((MediaSource) Assertions.checkNotNull(mediaSource));
        Assertions.checkArgument(startPositionUs >= 0);
        this.startUs = startPositionUs;
        this.endUs = endPositionUs;
        this.enableInitialDiscontinuity = enableInitialDiscontinuity;
        this.allowDynamicClippingUpdates = allowDynamicClippingUpdates;
        this.relativeToDefaultPosition = relativeToDefaultPosition;
        this.mediaPeriods = new ArrayList<>();
        this.window = new Timeline.Window();
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource, androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public boolean canUpdateMediaItem(MediaItem mediaItem) {
        return getMediaItem().clippingConfiguration.equals(mediaItem.clippingConfiguration) && this.mediaSource.canUpdateMediaItem(mediaItem);
    }

    @Override // androidx.media3.exoplayer.source.CompositeMediaSource, androidx.media3.exoplayer.source.MediaSource
    public void maybeThrowSourceInfoRefreshError() throws IOException {
        if (this.clippingError != null) {
            throw this.clippingError;
        }
        super.maybeThrowSourceInfoRefreshError();
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource, androidx.media3.exoplayer.source.MediaSource
    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator, long startPositionUs) {
        ClippingMediaPeriod mediaPeriod = new ClippingMediaPeriod(this.mediaSource.createPeriod(id, allocator, startPositionUs), this.enableInitialDiscontinuity, this.periodStartUs, this.periodEndUs);
        this.mediaPeriods.add(mediaPeriod);
        return mediaPeriod;
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource, androidx.media3.exoplayer.source.MediaSource
    public void releasePeriod(MediaPeriod mediaPeriod) {
        Assertions.checkState(this.mediaPeriods.remove(mediaPeriod));
        this.mediaSource.releasePeriod(((ClippingMediaPeriod) mediaPeriod).mediaPeriod);
        if (this.mediaPeriods.isEmpty() && !this.allowDynamicClippingUpdates) {
            refreshClippedTimeline(((ClippingTimeline) Assertions.checkNotNull(this.clippingTimeline)).timeline);
        }
    }

    @Override // androidx.media3.exoplayer.source.CompositeMediaSource, androidx.media3.exoplayer.source.BaseMediaSource
    protected void releaseSourceInternal() {
        super.releaseSourceInternal();
        this.clippingError = null;
        this.clippingTimeline = null;
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource
    protected void onChildSourceInfoRefreshed(Timeline newTimeline) {
        if (this.clippingError != null) {
            return;
        }
        refreshClippedTimeline(newTimeline);
    }

    private void refreshClippedTimeline(Timeline timeline) {
        long windowStartUs;
        long windowEndUs;
        timeline.getWindow(0, this.window);
        long windowPositionInPeriodUs = this.window.getPositionInFirstPeriodUs();
        if (this.clippingTimeline == null || this.mediaPeriods.isEmpty() || this.allowDynamicClippingUpdates) {
            windowStartUs = this.startUs;
            windowEndUs = this.endUs;
            if (this.relativeToDefaultPosition) {
                long windowDefaultPositionUs = this.window.getDefaultPositionUs();
                windowStartUs += windowDefaultPositionUs;
                windowEndUs += windowDefaultPositionUs;
            }
            this.periodStartUs = windowPositionInPeriodUs + windowStartUs;
            this.periodEndUs = this.endUs != Long.MIN_VALUE ? windowPositionInPeriodUs + windowEndUs : Long.MIN_VALUE;
            int count = this.mediaPeriods.size();
            for (int i = 0; i < count; i++) {
                this.mediaPeriods.get(i).updateClipping(this.periodStartUs, this.periodEndUs);
            }
        } else {
            windowStartUs = this.periodStartUs - windowPositionInPeriodUs;
            windowEndUs = this.endUs != Long.MIN_VALUE ? this.periodEndUs - windowPositionInPeriodUs : Long.MIN_VALUE;
        }
        try {
            this.clippingTimeline = new ClippingTimeline(timeline, windowStartUs, windowEndUs);
            refreshSourceInfo(this.clippingTimeline);
        } catch (IllegalClippingException e) {
            this.clippingError = e;
            for (int i2 = 0; i2 < this.mediaPeriods.size(); i2++) {
                this.mediaPeriods.get(i2).setClippingError(this.clippingError);
            }
        }
    }

    private static final class ClippingTimeline extends ForwardingTimeline {
        private final long durationUs;
        private final long endUs;
        private final boolean isDynamic;
        private final long startUs;

        public ClippingTimeline(Timeline timeline, long startUs, long endUs) throws IllegalClippingException {
            super(timeline);
            boolean z = false;
            if (timeline.getPeriodCount() != 1) {
                throw new IllegalClippingException(0);
            }
            Timeline.Window window = timeline.getWindow(0, new Timeline.Window());
            long startUs2 = Math.max(0L, startUs);
            if (window.isPlaceholder || startUs2 == 0 || window.isSeekable) {
                long resolvedEndUs = endUs == Long.MIN_VALUE ? window.durationUs : Math.max(0L, endUs);
                if (window.durationUs != C.TIME_UNSET) {
                    resolvedEndUs = resolvedEndUs > window.durationUs ? window.durationUs : resolvedEndUs;
                    if (startUs2 > resolvedEndUs) {
                        throw new IllegalClippingException(2);
                    }
                }
                this.startUs = startUs2;
                this.endUs = resolvedEndUs;
                this.durationUs = resolvedEndUs == C.TIME_UNSET ? -9223372036854775807L : resolvedEndUs - startUs2;
                if (window.isDynamic && (resolvedEndUs == C.TIME_UNSET || (window.durationUs != C.TIME_UNSET && resolvedEndUs == window.durationUs))) {
                    z = true;
                }
                this.isDynamic = z;
                return;
            }
            throw new IllegalClippingException(1);
        }

        @Override // androidx.media3.exoplayer.source.ForwardingTimeline, androidx.media3.common.Timeline
        public Timeline.Window getWindow(int windowIndex, Timeline.Window window, long defaultPositionProjectionUs) {
            this.timeline.getWindow(0, window, 0L);
            window.positionInFirstPeriodUs += this.startUs;
            window.durationUs = this.durationUs;
            window.isDynamic = this.isDynamic;
            if (window.defaultPositionUs != C.TIME_UNSET) {
                window.defaultPositionUs = Math.max(window.defaultPositionUs, this.startUs);
                window.defaultPositionUs = this.endUs == C.TIME_UNSET ? window.defaultPositionUs : Math.min(window.defaultPositionUs, this.endUs);
                window.defaultPositionUs -= this.startUs;
            }
            long startMs = Util.usToMs(this.startUs);
            if (window.presentationStartTimeMs != C.TIME_UNSET) {
                window.presentationStartTimeMs += startMs;
            }
            if (window.windowStartTimeMs != C.TIME_UNSET) {
                window.windowStartTimeMs += startMs;
            }
            return window;
        }

        @Override // androidx.media3.exoplayer.source.ForwardingTimeline, androidx.media3.common.Timeline
        public Timeline.Period getPeriod(int periodIndex, Timeline.Period period, boolean setIds) {
            this.timeline.getPeriod(0, period, setIds);
            long positionInClippedWindowUs = period.getPositionInWindowUs() - this.startUs;
            long j = this.durationUs;
            long j2 = C.TIME_UNSET;
            if (j != C.TIME_UNSET) {
                j2 = this.durationUs - positionInClippedWindowUs;
            }
            long periodDurationUs = j2;
            return period.set(period.id, period.uid, 0, periodDurationUs, positionInClippedWindowUs);
        }
    }
}
