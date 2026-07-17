package androidx.media3.exoplayer.source;

import android.net.Uri;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;

/* JADX INFO: loaded from: classes.dex */
public final class SinglePeriodTimeline extends Timeline {
    private final long elapsedRealtimeEpochOffsetMs;
    private final boolean isDynamic;
    private final boolean isSeekable;
    private final MediaItem.LiveConfiguration liveConfiguration;
    private final Object manifest;
    private final MediaItem mediaItem;
    private final long periodDurationUs;
    private final long presentationStartTimeMs;
    private final boolean suppressPositionProjection;
    private final long windowDefaultStartPositionUs;
    private final long windowDurationUs;
    private final long windowPositionInPeriodUs;
    private final long windowStartTimeMs;
    private static final Object UID = new Object();
    private static final MediaItem MEDIA_ITEM = new MediaItem.Builder().setMediaId("SinglePeriodTimeline").setUri(Uri.EMPTY).build();

    @Deprecated
    public SinglePeriodTimeline(long durationUs, boolean isSeekable, boolean isDynamic, boolean isLive, Object manifest, Object tag) {
        this(durationUs, durationUs, 0L, 0L, isSeekable, isDynamic, isLive, manifest, tag);
    }

    public SinglePeriodTimeline(long durationUs, boolean isSeekable, boolean isDynamic, boolean useLiveConfiguration, Object manifest, MediaItem mediaItem) {
        this(durationUs, durationUs, 0L, 0L, isSeekable, isDynamic, useLiveConfiguration, manifest, mediaItem);
    }

    @Deprecated
    public SinglePeriodTimeline(long periodDurationUs, long windowDurationUs, long windowPositionInPeriodUs, long windowDefaultStartPositionUs, boolean isSeekable, boolean isDynamic, boolean isLive, Object manifest, Object tag) {
        this(C.TIME_UNSET, C.TIME_UNSET, C.TIME_UNSET, periodDurationUs, windowDurationUs, windowPositionInPeriodUs, windowDefaultStartPositionUs, isSeekable, isDynamic, isLive, manifest, tag);
    }

    /* JADX WARN: Illegal instructions before constructor call */
    public SinglePeriodTimeline(long periodDurationUs, long windowDurationUs, long windowPositionInPeriodUs, long windowDefaultStartPositionUs, boolean isSeekable, boolean isDynamic, boolean useLiveConfiguration, Object manifest, MediaItem mediaItem) {
        MediaItem mediaItem2;
        MediaItem.LiveConfiguration liveConfiguration;
        if (useLiveConfiguration) {
            mediaItem2 = mediaItem;
            liveConfiguration = mediaItem2.liveConfiguration;
        } else {
            mediaItem2 = mediaItem;
            liveConfiguration = null;
        }
        this(C.TIME_UNSET, C.TIME_UNSET, C.TIME_UNSET, periodDurationUs, windowDurationUs, windowPositionInPeriodUs, windowDefaultStartPositionUs, isSeekable, isDynamic, false, manifest, mediaItem2, liveConfiguration);
    }

    @Deprecated
    public SinglePeriodTimeline(long presentationStartTimeMs, long windowStartTimeMs, long elapsedRealtimeEpochOffsetMs, long periodDurationUs, long windowDurationUs, long windowPositionInPeriodUs, long windowDefaultStartPositionUs, boolean isSeekable, boolean isDynamic, boolean isLive, Object manifest, Object tag) {
        this(presentationStartTimeMs, windowStartTimeMs, elapsedRealtimeEpochOffsetMs, periodDurationUs, windowDurationUs, windowPositionInPeriodUs, windowDefaultStartPositionUs, isSeekable, isDynamic, false, manifest, MEDIA_ITEM.buildUpon().setTag(tag).build(), isLive ? MEDIA_ITEM.liveConfiguration : null);
    }

    @Deprecated
    public SinglePeriodTimeline(long presentationStartTimeMs, long windowStartTimeMs, long elapsedRealtimeEpochOffsetMs, long periodDurationUs, long windowDurationUs, long windowPositionInPeriodUs, long windowDefaultStartPositionUs, boolean isSeekable, boolean isDynamic, Object manifest, MediaItem mediaItem, MediaItem.LiveConfiguration liveConfiguration) {
        this(presentationStartTimeMs, windowStartTimeMs, elapsedRealtimeEpochOffsetMs, periodDurationUs, windowDurationUs, windowPositionInPeriodUs, windowDefaultStartPositionUs, isSeekable, isDynamic, false, manifest, mediaItem, liveConfiguration);
    }

    public SinglePeriodTimeline(long presentationStartTimeMs, long windowStartTimeMs, long elapsedRealtimeEpochOffsetMs, long periodDurationUs, long windowDurationUs, long windowPositionInPeriodUs, long windowDefaultStartPositionUs, boolean isSeekable, boolean isDynamic, boolean suppressPositionProjection, Object manifest, MediaItem mediaItem, MediaItem.LiveConfiguration liveConfiguration) {
        this.presentationStartTimeMs = presentationStartTimeMs;
        this.windowStartTimeMs = windowStartTimeMs;
        this.elapsedRealtimeEpochOffsetMs = elapsedRealtimeEpochOffsetMs;
        this.periodDurationUs = periodDurationUs;
        this.windowDurationUs = windowDurationUs;
        this.windowPositionInPeriodUs = windowPositionInPeriodUs;
        this.windowDefaultStartPositionUs = windowDefaultStartPositionUs;
        this.isSeekable = isSeekable;
        this.isDynamic = isDynamic;
        this.suppressPositionProjection = suppressPositionProjection;
        this.manifest = manifest;
        this.mediaItem = (MediaItem) Assertions.checkNotNull(mediaItem);
        this.liveConfiguration = liveConfiguration;
    }

    @Override // androidx.media3.common.Timeline
    public int getWindowCount() {
        return 1;
    }

    @Override // androidx.media3.common.Timeline
    public Timeline.Window getWindow(int windowIndex, Timeline.Window window, long defaultPositionProjectionUs) {
        long windowDefaultStartPositionUs;
        Assertions.checkIndex(windowIndex, 0, 1);
        long windowDefaultStartPositionUs2 = this.windowDefaultStartPositionUs;
        if (!this.isDynamic || this.suppressPositionProjection || defaultPositionProjectionUs == 0) {
            windowDefaultStartPositionUs = windowDefaultStartPositionUs2;
        } else if (this.windowDurationUs == C.TIME_UNSET) {
            windowDefaultStartPositionUs = -9223372036854775807L;
        } else {
            long windowDefaultStartPositionUs3 = windowDefaultStartPositionUs2 + defaultPositionProjectionUs;
            if (windowDefaultStartPositionUs3 <= this.windowDurationUs) {
                windowDefaultStartPositionUs = windowDefaultStartPositionUs3;
            } else {
                windowDefaultStartPositionUs = -9223372036854775807L;
            }
        }
        return window.set(Timeline.Window.SINGLE_WINDOW_UID, this.mediaItem, this.manifest, this.presentationStartTimeMs, this.windowStartTimeMs, this.elapsedRealtimeEpochOffsetMs, this.isSeekable, this.isDynamic, this.liveConfiguration, windowDefaultStartPositionUs, this.windowDurationUs, 0, 0, this.windowPositionInPeriodUs);
    }

    @Override // androidx.media3.common.Timeline
    public int getPeriodCount() {
        return 1;
    }

    @Override // androidx.media3.common.Timeline
    public Timeline.Period getPeriod(int periodIndex, Timeline.Period period, boolean setIds) {
        Assertions.checkIndex(periodIndex, 0, 1);
        Object uid = setIds ? UID : null;
        return period.set(null, uid, 0, this.periodDurationUs, -this.windowPositionInPeriodUs);
    }

    @Override // androidx.media3.common.Timeline
    public int getIndexOfPeriod(Object uid) {
        return UID.equals(uid) ? 0 : -1;
    }

    @Override // androidx.media3.common.Timeline
    public Object getUidOfPeriod(int periodIndex) {
        Assertions.checkIndex(periodIndex, 0, 1);
        return UID;
    }
}
