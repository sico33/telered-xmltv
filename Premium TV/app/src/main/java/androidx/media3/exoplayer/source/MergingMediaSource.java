package androidx.media3.exoplayer.source;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.datasource.TransferListener;
import androidx.media3.exoplayer.upstream.Allocator;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class MergingMediaSource extends CompositeMediaSource<Integer> {
    private static final int PERIOD_COUNT_UNSET = -1;
    private static final MediaItem PLACEHOLDER_MEDIA_ITEM = new MediaItem.Builder().setMediaId("MergingMediaSource").build();
    private final boolean adjustPeriodTimeOffsets;
    private final boolean clipDurations;
    private final Map<Object, Long> clippedDurationsUs;
    private final Multimap<Object, ClippingMediaPeriod> clippedMediaPeriods;
    private final CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory;
    private final MediaSource[] mediaSources;
    private IllegalMergeException mergeError;
    private final ArrayList<MediaSource> pendingTimelineSources;
    private int periodCount;
    private long[][] periodTimeOffsetsUs;
    private final Timeline[] timelines;

    public static final class IllegalMergeException extends IOException {
        public static final int REASON_PERIOD_COUNT_MISMATCH = 0;
        public final int reason;

        @Target({ElementType.TYPE_USE})
        @Documented
        @Retention(RetentionPolicy.SOURCE)
        public @interface Reason {
        }

        public IllegalMergeException(int reason) {
            this.reason = reason;
        }
    }

    public MergingMediaSource(MediaSource... mediaSources) {
        this(false, mediaSources);
    }

    public MergingMediaSource(boolean adjustPeriodTimeOffsets, MediaSource... mediaSources) {
        this(adjustPeriodTimeOffsets, false, mediaSources);
    }

    public MergingMediaSource(boolean adjustPeriodTimeOffsets, boolean clipDurations, MediaSource... mediaSources) {
        this(adjustPeriodTimeOffsets, clipDurations, new DefaultCompositeSequenceableLoaderFactory(), mediaSources);
    }

    public MergingMediaSource(boolean adjustPeriodTimeOffsets, boolean clipDurations, CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory, MediaSource... mediaSources) {
        this.adjustPeriodTimeOffsets = adjustPeriodTimeOffsets;
        this.clipDurations = clipDurations;
        this.mediaSources = mediaSources;
        this.compositeSequenceableLoaderFactory = compositeSequenceableLoaderFactory;
        this.pendingTimelineSources = new ArrayList<>(Arrays.asList(mediaSources));
        this.periodCount = -1;
        this.timelines = new Timeline[mediaSources.length];
        this.periodTimeOffsetsUs = new long[0][];
        this.clippedDurationsUs = new HashMap();
        this.clippedMediaPeriods = MultimapBuilder.hashKeys().arrayListValues().build();
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public MediaItem getMediaItem() {
        return this.mediaSources.length > 0 ? this.mediaSources[0].getMediaItem() : PLACEHOLDER_MEDIA_ITEM;
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public boolean canUpdateMediaItem(MediaItem mediaItem) {
        return this.mediaSources.length > 0 && this.mediaSources[0].canUpdateMediaItem(mediaItem);
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public void updateMediaItem(MediaItem mediaItem) {
        this.mediaSources[0].updateMediaItem(mediaItem);
    }

    @Override // androidx.media3.exoplayer.source.CompositeMediaSource, androidx.media3.exoplayer.source.BaseMediaSource
    protected void prepareSourceInternal(TransferListener mediaTransferListener) {
        super.prepareSourceInternal(mediaTransferListener);
        for (int i = 0; i < this.mediaSources.length; i++) {
            prepareChildSource(Integer.valueOf(i), this.mediaSources[i]);
        }
    }

    @Override // androidx.media3.exoplayer.source.CompositeMediaSource, androidx.media3.exoplayer.source.MediaSource
    public void maybeThrowSourceInfoRefreshError() throws IOException {
        if (this.mergeError != null) {
            throw this.mergeError;
        }
        super.maybeThrowSourceInfoRefreshError();
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator, long startPositionUs) {
        MediaPeriod[] periods = new MediaPeriod[this.mediaSources.length];
        int periodIndex = this.timelines[0].getIndexOfPeriod(id.periodUid);
        for (int i = 0; i < periods.length; i++) {
            MediaSource.MediaPeriodId childMediaPeriodId = id.copyWithPeriodUid(this.timelines[i].getUidOfPeriod(periodIndex));
            periods[i] = this.mediaSources[i].createPeriod(childMediaPeriodId, allocator, startPositionUs - this.periodTimeOffsetsUs[periodIndex][i]);
        }
        MediaPeriod mediaPeriod = new MergingMediaPeriod(this.compositeSequenceableLoaderFactory, this.periodTimeOffsetsUs[periodIndex], periods);
        if (!this.clipDurations) {
            return mediaPeriod;
        }
        MediaPeriod mediaPeriod2 = new ClippingMediaPeriod(mediaPeriod, true, 0L, ((Long) Assertions.checkNotNull(this.clippedDurationsUs.get(id.periodUid))).longValue());
        this.clippedMediaPeriods.put(id.periodUid, (ClippingMediaPeriod) mediaPeriod2);
        return mediaPeriod2;
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public void releasePeriod(MediaPeriod mediaPeriod) {
        if (this.clipDurations) {
            ClippingMediaPeriod clippingMediaPeriod = (ClippingMediaPeriod) mediaPeriod;
            for (Map.Entry<Object, ClippingMediaPeriod> entry : this.clippedMediaPeriods.entries()) {
                if (entry.getValue().equals(clippingMediaPeriod)) {
                    this.clippedMediaPeriods.remove(entry.getKey(), entry.getValue());
                    break;
                }
            }
            mediaPeriod = clippingMediaPeriod.mediaPeriod;
        }
        MergingMediaPeriod mergingPeriod = (MergingMediaPeriod) mediaPeriod;
        for (int i = 0; i < this.mediaSources.length; i++) {
            this.mediaSources[i].releasePeriod(mergingPeriod.getChildPeriod(i));
        }
    }

    @Override // androidx.media3.exoplayer.source.CompositeMediaSource, androidx.media3.exoplayer.source.BaseMediaSource
    protected void releaseSourceInternal() {
        super.releaseSourceInternal();
        Arrays.fill(this.timelines, (Object) null);
        this.periodCount = -1;
        this.mergeError = null;
        this.pendingTimelineSources.clear();
        Collections.addAll(this.pendingTimelineSources, this.mediaSources);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.exoplayer.source.CompositeMediaSource
    /* JADX INFO: renamed from: onChildSourceInfoRefreshed, reason: avoid collision after fix types in other method and merged with bridge method [inline-methods] */
    public void m108x28f9175(Integer childSourceId, MediaSource mediaSource, Timeline newTimeline) {
        if (this.mergeError != null) {
            return;
        }
        if (this.periodCount == -1) {
            this.periodCount = newTimeline.getPeriodCount();
        } else if (newTimeline.getPeriodCount() != this.periodCount) {
            this.mergeError = new IllegalMergeException(0);
            return;
        }
        if (this.periodTimeOffsetsUs.length == 0) {
            this.periodTimeOffsetsUs = (long[][]) Array.newInstance((Class<?>) Long.TYPE, this.periodCount, this.timelines.length);
        }
        this.pendingTimelineSources.remove(mediaSource);
        this.timelines[childSourceId.intValue()] = newTimeline;
        if (this.pendingTimelineSources.isEmpty()) {
            if (this.adjustPeriodTimeOffsets) {
                computePeriodTimeOffsets();
            }
            Timeline mergedTimeline = this.timelines[0];
            if (this.clipDurations) {
                updateClippedDuration();
                mergedTimeline = new ClippedTimeline(mergedTimeline, this.clippedDurationsUs);
            }
            refreshSourceInfo(mergedTimeline);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.exoplayer.source.CompositeMediaSource
    public MediaSource.MediaPeriodId getMediaPeriodIdForChildMediaPeriodId(Integer childSourceId, MediaSource.MediaPeriodId mediaPeriodId) {
        if (childSourceId.intValue() == 0) {
            return mediaPeriodId;
        }
        return null;
    }

    private void computePeriodTimeOffsets() {
        Timeline.Period period = new Timeline.Period();
        for (int periodIndex = 0; periodIndex < this.periodCount; periodIndex++) {
            long primaryWindowOffsetUs = -this.timelines[0].getPeriod(periodIndex, period).getPositionInWindowUs();
            for (int timelineIndex = 1; timelineIndex < this.timelines.length; timelineIndex++) {
                long secondaryWindowOffsetUs = -this.timelines[timelineIndex].getPeriod(periodIndex, period).getPositionInWindowUs();
                this.periodTimeOffsetsUs[periodIndex][timelineIndex] = primaryWindowOffsetUs - secondaryWindowOffsetUs;
            }
        }
    }

    private void updateClippedDuration() {
        Timeline[] timelineArr;
        Timeline.Period period = new Timeline.Period();
        for (int periodIndex = 0; periodIndex < this.periodCount; periodIndex++) {
            long minDurationUs = Long.MIN_VALUE;
            int timelineIndex = 0;
            while (true) {
                int length = this.timelines.length;
                timelineArr = this.timelines;
                if (timelineIndex >= length) {
                    break;
                }
                long durationUs = timelineArr[timelineIndex].getPeriod(periodIndex, period).getDurationUs();
                if (durationUs != C.TIME_UNSET) {
                    long adjustedDurationUs = this.periodTimeOffsetsUs[periodIndex][timelineIndex] + durationUs;
                    if (minDurationUs == Long.MIN_VALUE || adjustedDurationUs < minDurationUs) {
                        minDurationUs = adjustedDurationUs;
                    }
                }
                timelineIndex++;
            }
            Object periodUid = timelineArr[0].getUidOfPeriod(periodIndex);
            this.clippedDurationsUs.put(periodUid, Long.valueOf(minDurationUs));
            for (ClippingMediaPeriod clippingMediaPeriod : this.clippedMediaPeriods.get(periodUid)) {
                clippingMediaPeriod.updateClipping(0L, minDurationUs);
            }
        }
    }

    private static final class ClippedTimeline extends ForwardingTimeline {
        private final long[] periodDurationsUs;
        private final long[] windowDurationsUs;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public ClippedTimeline(Timeline timeline, Map<Object, Long> clippedDurationsUs) {
            super(timeline);
            Timeline timeline2 = timeline;
            int windowCount = timeline2.getWindowCount();
            this.windowDurationsUs = new long[timeline2.getWindowCount()];
            Timeline.Window window = new Timeline.Window();
            for (int i = 0; i < windowCount; i++) {
                this.windowDurationsUs[i] = timeline2.getWindow(i, window).durationUs;
            }
            int periodCount = timeline2.getPeriodCount();
            this.periodDurationsUs = new long[periodCount];
            Timeline.Period period = new Timeline.Period();
            int i2 = 0;
            while (i2 < periodCount) {
                timeline2.getPeriod(i2, period, true);
                long clippedDurationUs = ((Long) Assertions.checkNotNull(clippedDurationsUs.get(period.uid))).longValue();
                this.periodDurationsUs[i2] = clippedDurationUs != Long.MIN_VALUE ? clippedDurationUs : period.durationUs;
                if (period.durationUs != C.TIME_UNSET) {
                    long[] jArr = this.windowDurationsUs;
                    int i3 = period.windowIndex;
                    jArr[i3] = jArr[i3] - (period.durationUs - this.periodDurationsUs[i2]);
                }
                i2++;
                timeline2 = timeline;
            }
        }

        @Override // androidx.media3.exoplayer.source.ForwardingTimeline, androidx.media3.common.Timeline
        public Timeline.Window getWindow(int windowIndex, Timeline.Window window, long defaultPositionProjectionUs) {
            long jMin;
            super.getWindow(windowIndex, window, defaultPositionProjectionUs);
            window.durationUs = this.windowDurationsUs[windowIndex];
            if (window.durationUs == C.TIME_UNSET || window.defaultPositionUs == C.TIME_UNSET) {
                jMin = window.defaultPositionUs;
            } else {
                jMin = Math.min(window.defaultPositionUs, window.durationUs);
            }
            window.defaultPositionUs = jMin;
            return window;
        }

        @Override // androidx.media3.exoplayer.source.ForwardingTimeline, androidx.media3.common.Timeline
        public Timeline.Period getPeriod(int periodIndex, Timeline.Period period, boolean setIds) {
            super.getPeriod(periodIndex, period, setIds);
            period.durationUs = this.periodDurationsUs[periodIndex];
            return period;
        }
    }
}
