package androidx.media3.exoplayer.source;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Pair;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.TransferListener;
import androidx.media3.exoplayer.upstream.Allocator;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class ConcatenatingMediaSource2 extends CompositeMediaSource<Integer> {
    private static final int MSG_UPDATE_TIMELINE = 1;
    private MediaItem mediaItem;
    private final IdentityHashMap<MediaPeriod, MediaSourceHolder> mediaSourceByMediaPeriod;
    private final ImmutableList<MediaSourceHolder> mediaSourceHolders;
    private Handler playbackThreadHandler;
    private boolean timelineUpdateScheduled;

    public static final class Builder {
        private int index;
        private MediaItem mediaItem;
        private MediaSource.Factory mediaSourceFactory;
        private final ImmutableList.Builder<MediaSourceHolder> mediaSourceHoldersBuilder = ImmutableList.builder();

        public Builder useDefaultMediaSourceFactory(Context context) {
            return setMediaSourceFactory(new DefaultMediaSourceFactory(context));
        }

        public Builder setMediaSourceFactory(MediaSource.Factory mediaSourceFactory) {
            this.mediaSourceFactory = (MediaSource.Factory) Assertions.checkNotNull(mediaSourceFactory);
            return this;
        }

        public Builder setMediaItem(MediaItem mediaItem) {
            this.mediaItem = mediaItem;
            return this;
        }

        public Builder add(MediaItem mediaItem) {
            return add(mediaItem, C.TIME_UNSET);
        }

        public Builder add(MediaItem mediaItem, long initialPlaceholderDurationMs) {
            Assertions.checkNotNull(mediaItem);
            if (initialPlaceholderDurationMs == C.TIME_UNSET && mediaItem.clippingConfiguration.endPositionMs != Long.MIN_VALUE) {
                initialPlaceholderDurationMs = Util.usToMs(mediaItem.clippingConfiguration.endPositionUs - mediaItem.clippingConfiguration.startPositionUs);
            }
            Assertions.checkStateNotNull(this.mediaSourceFactory, "Must use useDefaultMediaSourceFactory or setMediaSourceFactory first.");
            return add(this.mediaSourceFactory.createMediaSource(mediaItem), initialPlaceholderDurationMs);
        }

        public Builder add(MediaSource mediaSource) {
            return add(mediaSource, C.TIME_UNSET);
        }

        public Builder add(MediaSource mediaSource, long initialPlaceholderDurationMs) {
            Assertions.checkNotNull(mediaSource);
            Assertions.checkState(((mediaSource instanceof ProgressiveMediaSource) && initialPlaceholderDurationMs == C.TIME_UNSET) ? false : true, "Progressive media source must define an initial placeholder duration.");
            ImmutableList.Builder<MediaSourceHolder> builder = this.mediaSourceHoldersBuilder;
            int i = this.index;
            this.index = i + 1;
            builder.add(new MediaSourceHolder(mediaSource, i, Util.msToUs(initialPlaceholderDurationMs)));
            return this;
        }

        public ConcatenatingMediaSource2 build() {
            Assertions.checkArgument(this.index > 0, "Must add at least one source to the concatenation.");
            if (this.mediaItem == null) {
                this.mediaItem = MediaItem.fromUri(Uri.EMPTY);
            }
            return new ConcatenatingMediaSource2(this.mediaItem, this.mediaSourceHoldersBuilder.build());
        }
    }

    private ConcatenatingMediaSource2(MediaItem mediaItem, ImmutableList<MediaSourceHolder> mediaSourceHolders) {
        this.mediaItem = mediaItem;
        this.mediaSourceHolders = mediaSourceHolders;
        this.mediaSourceByMediaPeriod = new IdentityHashMap<>();
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public Timeline getInitialTimeline() {
        return maybeCreateConcatenatedTimeline();
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public synchronized MediaItem getMediaItem() {
        return this.mediaItem;
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public boolean canUpdateMediaItem(MediaItem mediaItem) {
        return true;
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public synchronized void updateMediaItem(MediaItem mediaItem) {
        this.mediaItem = mediaItem;
    }

    @Override // androidx.media3.exoplayer.source.CompositeMediaSource, androidx.media3.exoplayer.source.BaseMediaSource
    protected void prepareSourceInternal(TransferListener mediaTransferListener) {
        super.prepareSourceInternal(mediaTransferListener);
        this.playbackThreadHandler = new Handler(new Handler.Callback() { // from class: androidx.media3.exoplayer.source.ConcatenatingMediaSource2$$ExternalSyntheticLambda0
            @Override // android.os.Handler.Callback
            public final boolean handleMessage(Message message) {
                return this.f$0.handleMessage(message);
            }
        });
        for (int i = 0; i < this.mediaSourceHolders.size(); i++) {
            MediaSourceHolder holder = this.mediaSourceHolders.get(i);
            prepareChildSource(Integer.valueOf(i), holder.mediaSource);
        }
        scheduleTimelineUpdate();
    }

    @Override // androidx.media3.exoplayer.source.CompositeMediaSource, androidx.media3.exoplayer.source.BaseMediaSource
    protected void enableInternal() {
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator, long startPositionUs) {
        long timeOffsetUs;
        int holderIndex = getChildIndex(id.periodUid);
        MediaSourceHolder holder = this.mediaSourceHolders.get(holderIndex);
        MediaSource.MediaPeriodId childMediaPeriodId = id.copyWithPeriodUid(getChildPeriodUid(id.periodUid)).copyWithWindowSequenceNumber(getChildWindowSequenceNumber(id.windowSequenceNumber, this.mediaSourceHolders.size(), holder.index));
        enableChildSource(Integer.valueOf(holder.index));
        holder.activeMediaPeriods++;
        if (id.isAd()) {
            timeOffsetUs = 0;
        } else {
            timeOffsetUs = ((Long) Assertions.checkNotNull(holder.periodTimeOffsetsByUid.get(childMediaPeriodId.periodUid))).longValue();
        }
        MediaPeriod mediaPeriod = new TimeOffsetMediaPeriod(holder.mediaSource.createPeriod(childMediaPeriodId, allocator, startPositionUs - timeOffsetUs), timeOffsetUs);
        this.mediaSourceByMediaPeriod.put(mediaPeriod, holder);
        disableUnusedMediaSources();
        return mediaPeriod;
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public void releasePeriod(MediaPeriod mediaPeriod) {
        MediaSourceHolder holder = (MediaSourceHolder) Assertions.checkNotNull(this.mediaSourceByMediaPeriod.remove(mediaPeriod));
        holder.mediaSource.releasePeriod(((TimeOffsetMediaPeriod) mediaPeriod).getWrappedMediaPeriod());
        holder.activeMediaPeriods--;
        if (!this.mediaSourceByMediaPeriod.isEmpty()) {
            disableUnusedMediaSources();
        }
    }

    @Override // androidx.media3.exoplayer.source.CompositeMediaSource, androidx.media3.exoplayer.source.BaseMediaSource
    protected void releaseSourceInternal() {
        super.releaseSourceInternal();
        if (this.playbackThreadHandler != null) {
            this.playbackThreadHandler.removeCallbacksAndMessages(null);
            this.playbackThreadHandler = null;
        }
        this.timelineUpdateScheduled = false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.exoplayer.source.CompositeMediaSource
    /* JADX INFO: renamed from: onChildSourceInfoRefreshed, reason: avoid collision after fix types in other method and merged with bridge method [inline-methods] */
    public void m108x28f9175(Integer childSourceId, MediaSource mediaSource, Timeline newTimeline) {
        scheduleTimelineUpdate();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.exoplayer.source.CompositeMediaSource
    public MediaSource.MediaPeriodId getMediaPeriodIdForChildMediaPeriodId(Integer childSourceId, MediaSource.MediaPeriodId mediaPeriodId) {
        int childIndex = getChildIndexFromChildWindowSequenceNumber(mediaPeriodId.windowSequenceNumber, this.mediaSourceHolders.size());
        if (childSourceId.intValue() != childIndex) {
            return null;
        }
        long windowSequenceNumber = getWindowSequenceNumberFromChildWindowSequenceNumber(mediaPeriodId.windowSequenceNumber, this.mediaSourceHolders.size());
        Object periodUid = getPeriodUid(childSourceId.intValue(), mediaPeriodId.periodUid);
        return mediaPeriodId.copyWithPeriodUid(periodUid).copyWithWindowSequenceNumber(windowSequenceNumber);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.exoplayer.source.CompositeMediaSource
    public int getWindowIndexForChildWindowIndex(Integer childSourceId, int windowIndex) {
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.exoplayer.source.CompositeMediaSource
    public long getMediaTimeForChildMediaTime(Integer childSourceId, long mediaTimeMs, MediaSource.MediaPeriodId mediaPeriodId) {
        Long timeOffsetUs;
        if (mediaTimeMs == C.TIME_UNSET || mediaPeriodId == null || mediaPeriodId.isAd() || (timeOffsetUs = this.mediaSourceHolders.get(childSourceId.intValue()).periodTimeOffsetsByUid.get(mediaPeriodId.periodUid)) == null) {
            return mediaTimeMs;
        }
        return Util.usToMs(timeOffsetUs.longValue()) + mediaTimeMs;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean handleMessage(Message msg) {
        if (msg.what == 1) {
            updateTimeline();
        }
        return true;
    }

    private void scheduleTimelineUpdate() {
        if (!this.timelineUpdateScheduled) {
            ((Handler) Assertions.checkNotNull(this.playbackThreadHandler)).obtainMessage(1).sendToTarget();
            this.timelineUpdateScheduled = true;
        }
    }

    private void updateTimeline() {
        this.timelineUpdateScheduled = false;
        ConcatenatedTimeline timeline = maybeCreateConcatenatedTimeline();
        if (timeline != null) {
            refreshSourceInfo(timeline);
        }
    }

    private void disableUnusedMediaSources() {
        for (int i = 0; i < this.mediaSourceHolders.size(); i++) {
            MediaSourceHolder holder = this.mediaSourceHolders.get(i);
            if (holder.activeMediaPeriods == 0) {
                disableChildSource(Integer.valueOf(holder.index));
            }
        }
    }

    private ConcatenatedTimeline maybeCreateConcatenatedTimeline() {
        Object initialManifest;
        long timeOffsetUsForPeriod;
        Timeline.Period period;
        boolean z;
        ConcatenatingMediaSource2 concatenatingMediaSource2 = this;
        Timeline.Window window = new Timeline.Window();
        Timeline.Period period2 = new Timeline.Period();
        ImmutableList.Builder<Timeline> timelinesBuilder = ImmutableList.builder();
        ImmutableList.Builder<Integer> firstPeriodIndicesBuilder = ImmutableList.builder();
        ImmutableList.Builder<Long> periodOffsetsInWindowUsBuilder = ImmutableList.builder();
        long nextPeriodOffsetInWindowUs = 0;
        boolean manifestsAreIdentical = true;
        boolean hasInitialManifest = false;
        Object initialManifest2 = null;
        int periodCount = 0;
        int mediaSourceHoldersCount = concatenatingMediaSource2.mediaSourceHolders.size();
        boolean isSeekable = true;
        boolean isDynamic = false;
        long durationUs = 0;
        long defaultPositionUs = 0;
        int i = 0;
        while (i < mediaSourceHoldersCount) {
            MediaSourceHolder holder = concatenatingMediaSource2.mediaSourceHolders.get(i);
            Timeline timeline = holder.mediaSource.getTimeline();
            Assertions.checkArgument(!timeline.isEmpty(), "Can't concatenate empty child Timeline.");
            timelinesBuilder.add(timeline);
            firstPeriodIndicesBuilder.add(Integer.valueOf(periodCount));
            int periodCountInMediaSourceHolder = timeline.getPeriodCount();
            periodCount += periodCountInMediaSourceHolder;
            int j = 0;
            while (j < timeline.getWindowCount()) {
                timeline.getWindow(j, window);
                if (hasInitialManifest) {
                    initialManifest = initialManifest2;
                } else {
                    initialManifest = window.manifest;
                    hasInitialManifest = true;
                }
                manifestsAreIdentical = manifestsAreIdentical && Util.areEqual(initialManifest, window.manifest);
                ImmutableList.Builder<Timeline> timelinesBuilder2 = timelinesBuilder;
                ImmutableList.Builder<Integer> firstPeriodIndicesBuilder2 = firstPeriodIndicesBuilder;
                long windowDurationUs = window.durationUs;
                if (windowDurationUs == C.TIME_UNSET) {
                    if (holder.initialPlaceholderDurationUs == C.TIME_UNSET) {
                        return null;
                    }
                    windowDurationUs = holder.initialPlaceholderDurationUs;
                }
                durationUs += windowDurationUs;
                if (holder.index == 0 && j == 0) {
                    defaultPositionUs = window.defaultPositionUs;
                    long defaultPositionUs2 = window.positionInFirstPeriodUs;
                    nextPeriodOffsetInWindowUs = -defaultPositionUs2;
                }
                isSeekable &= window.isSeekable || window.isPlaceholder;
                isDynamic |= window.isDynamic;
                int k = window.firstPeriodIndex;
                while (k <= window.lastPeriodIndex) {
                    periodOffsetsInWindowUsBuilder.add(Long.valueOf(nextPeriodOffsetInWindowUs));
                    timeline.getPeriod(k, period2, true);
                    long periodDurationUs = period2.durationUs;
                    if (periodDurationUs == C.TIME_UNSET) {
                        Assertions.checkArgument(window.firstPeriodIndex == window.lastPeriodIndex, "Can't apply placeholder duration to multiple periods with unknown duration in a single window.");
                        periodDurationUs = windowDurationUs + window.positionInFirstPeriodUs;
                    }
                    long timeOffsetUsForPeriod2 = 0;
                    long periodDurationUs2 = periodDurationUs;
                    boolean isFirstPeriodInNonFirstWindow = k == window.firstPeriodIndex && !(holder.index == 0 && j == 0);
                    if (isFirstPeriodInNonFirstWindow && periodDurationUs2 != C.TIME_UNSET) {
                        long timeOffsetUsForPeriod3 = -window.positionInFirstPeriodUs;
                        timeOffsetUsForPeriod2 = timeOffsetUsForPeriod3;
                        timeOffsetUsForPeriod = periodDurationUs2 + timeOffsetUsForPeriod3;
                    } else {
                        timeOffsetUsForPeriod = periodDurationUs2;
                    }
                    int k2 = k;
                    Object periodUid = Assertions.checkNotNull(period2.uid);
                    Timeline.Window window2 = window;
                    if (holder.activeMediaPeriods == 0 || !holder.periodTimeOffsetsByUid.containsKey(periodUid)) {
                        period = period2;
                    } else {
                        period = period2;
                        if (!holder.periodTimeOffsetsByUid.get(periodUid).equals(Long.valueOf(timeOffsetUsForPeriod2))) {
                            z = false;
                        }
                        Assertions.checkArgument(z, "Can't handle windows with changing offset in first period.");
                        holder.periodTimeOffsetsByUid.put(periodUid, Long.valueOf(timeOffsetUsForPeriod2));
                        nextPeriodOffsetInWindowUs += timeOffsetUsForPeriod;
                        k = k2 + 1;
                        window = window2;
                        period2 = period;
                    }
                    z = true;
                    Assertions.checkArgument(z, "Can't handle windows with changing offset in first period.");
                    holder.periodTimeOffsetsByUid.put(periodUid, Long.valueOf(timeOffsetUsForPeriod2));
                    nextPeriodOffsetInWindowUs += timeOffsetUsForPeriod;
                    k = k2 + 1;
                    window = window2;
                    period2 = period;
                }
                j++;
                initialManifest2 = initialManifest;
                firstPeriodIndicesBuilder = firstPeriodIndicesBuilder2;
                timelinesBuilder = timelinesBuilder2;
            }
            i++;
            concatenatingMediaSource2 = this;
        }
        return new ConcatenatedTimeline(getMediaItem(), timelinesBuilder.build(), firstPeriodIndicesBuilder.build(), periodOffsetsInWindowUsBuilder.build(), isSeekable, isDynamic, durationUs, defaultPositionUs, manifestsAreIdentical ? initialManifest2 : null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Object getPeriodUid(int childIndex, Object childPeriodUid) {
        return Pair.create(Integer.valueOf(childIndex), childPeriodUid);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int getChildIndex(Object periodUid) {
        return ((Integer) ((Pair) periodUid).first).intValue();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Object getChildPeriodUid(Object periodUid) {
        return ((Pair) periodUid).second;
    }

    private static long getChildWindowSequenceNumber(long windowSequenceNumber, int childCount, int childIndex) {
        return (((long) childCount) * windowSequenceNumber) + ((long) childIndex);
    }

    private static int getChildIndexFromChildWindowSequenceNumber(long childWindowSequenceNumber, int childCount) {
        return (int) (childWindowSequenceNumber % ((long) childCount));
    }

    private static long getWindowSequenceNumberFromChildWindowSequenceNumber(long childWindowSequenceNumber, int childCount) {
        return childWindowSequenceNumber / ((long) childCount);
    }

    static final class MediaSourceHolder {
        public int activeMediaPeriods;
        public final int index;
        public final long initialPlaceholderDurationUs;
        public final MaskingMediaSource mediaSource;
        public final HashMap<Object, Long> periodTimeOffsetsByUid = new HashMap<>();

        public MediaSourceHolder(MediaSource mediaSource, int index, long initialPlaceholderDurationUs) {
            this.mediaSource = new MaskingMediaSource(mediaSource, false);
            this.index = index;
            this.initialPlaceholderDurationUs = initialPlaceholderDurationUs;
        }
    }

    private static final class ConcatenatedTimeline extends Timeline {
        private final long defaultPositionUs;
        private final long durationUs;
        private final ImmutableList<Integer> firstPeriodIndices;
        private final boolean isDynamic;
        private final boolean isSeekable;
        private final Object manifest;
        private final MediaItem mediaItem;
        private final ImmutableList<Long> periodOffsetsInWindowUs;
        private final ImmutableList<Timeline> timelines;

        public ConcatenatedTimeline(MediaItem mediaItem, ImmutableList<Timeline> timelines, ImmutableList<Integer> firstPeriodIndices, ImmutableList<Long> periodOffsetsInWindowUs, boolean isSeekable, boolean isDynamic, long durationUs, long defaultPositionUs, Object manifest) {
            this.mediaItem = mediaItem;
            this.timelines = timelines;
            this.firstPeriodIndices = firstPeriodIndices;
            this.periodOffsetsInWindowUs = periodOffsetsInWindowUs;
            this.isSeekable = isSeekable;
            this.isDynamic = isDynamic;
            this.durationUs = durationUs;
            this.defaultPositionUs = defaultPositionUs;
            this.manifest = manifest;
        }

        @Override // androidx.media3.common.Timeline
        public int getWindowCount() {
            return 1;
        }

        @Override // androidx.media3.common.Timeline
        public int getPeriodCount() {
            return this.periodOffsetsInWindowUs.size();
        }

        @Override // androidx.media3.common.Timeline
        public Timeline.Window getWindow(int windowIndex, Timeline.Window window, long defaultPositionProjectionUs) {
            return window.set(Timeline.Window.SINGLE_WINDOW_UID, this.mediaItem, this.manifest, C.TIME_UNSET, C.TIME_UNSET, C.TIME_UNSET, this.isSeekable, this.isDynamic, null, this.defaultPositionUs, this.durationUs, 0, getPeriodCount() - 1, -this.periodOffsetsInWindowUs.get(0).longValue());
        }

        @Override // androidx.media3.common.Timeline
        public Timeline.Period getPeriodByUid(Object periodUid, Timeline.Period period) {
            int childIndex = ConcatenatingMediaSource2.getChildIndex(periodUid);
            Object childPeriodUid = ConcatenatingMediaSource2.getChildPeriodUid(periodUid);
            Timeline timeline = this.timelines.get(childIndex);
            int periodIndex = this.firstPeriodIndices.get(childIndex).intValue() + timeline.getIndexOfPeriod(childPeriodUid);
            timeline.getPeriodByUid(childPeriodUid, period);
            period.windowIndex = 0;
            period.positionInWindowUs = this.periodOffsetsInWindowUs.get(periodIndex).longValue();
            period.durationUs = getPeriodDurationUs(period, periodIndex);
            period.uid = periodUid;
            return period;
        }

        @Override // androidx.media3.common.Timeline
        public Timeline.Period getPeriod(int periodIndex, Timeline.Period period, boolean setIds) {
            int childIndex = getChildIndexByPeriodIndex(periodIndex);
            int firstPeriodIndexInChild = this.firstPeriodIndices.get(childIndex).intValue();
            this.timelines.get(childIndex).getPeriod(periodIndex - firstPeriodIndexInChild, period, setIds);
            period.windowIndex = 0;
            period.positionInWindowUs = this.periodOffsetsInWindowUs.get(periodIndex).longValue();
            period.durationUs = getPeriodDurationUs(period, periodIndex);
            if (setIds) {
                period.uid = ConcatenatingMediaSource2.getPeriodUid(childIndex, Assertions.checkNotNull(period.uid));
            }
            return period;
        }

        @Override // androidx.media3.common.Timeline
        public int getIndexOfPeriod(Object uid) {
            if (!(uid instanceof Pair) || !(((Pair) uid).first instanceof Integer)) {
                return -1;
            }
            int childIndex = ConcatenatingMediaSource2.getChildIndex(uid);
            Object periodUid = ConcatenatingMediaSource2.getChildPeriodUid(uid);
            int periodIndexInChild = this.timelines.get(childIndex).getIndexOfPeriod(periodUid);
            if (periodIndexInChild == -1) {
                return -1;
            }
            return this.firstPeriodIndices.get(childIndex).intValue() + periodIndexInChild;
        }

        @Override // androidx.media3.common.Timeline
        public Object getUidOfPeriod(int periodIndex) {
            int childIndex = getChildIndexByPeriodIndex(periodIndex);
            int firstPeriodIndexInChild = this.firstPeriodIndices.get(childIndex).intValue();
            Object periodUidInChild = this.timelines.get(childIndex).getUidOfPeriod(periodIndex - firstPeriodIndexInChild);
            return ConcatenatingMediaSource2.getPeriodUid(childIndex, periodUidInChild);
        }

        private int getChildIndexByPeriodIndex(int periodIndex) {
            return Util.binarySearchFloor((List<? extends Comparable<? super Integer>>) this.firstPeriodIndices, Integer.valueOf(periodIndex + 1), false, false);
        }

        private long getPeriodDurationUs(Timeline.Period childPeriod, int periodIndex) {
            long periodEndTimeInWindowUs;
            if (childPeriod.durationUs == C.TIME_UNSET) {
                return C.TIME_UNSET;
            }
            long periodStartTimeInWindowUs = this.periodOffsetsInWindowUs.get(periodIndex).longValue();
            if (periodIndex == this.periodOffsetsInWindowUs.size() - 1) {
                periodEndTimeInWindowUs = this.durationUs;
            } else {
                periodEndTimeInWindowUs = this.periodOffsetsInWindowUs.get(periodIndex + 1).longValue();
            }
            return periodEndTimeInWindowUs - periodStartTimeInWindowUs;
        }
    }
}
