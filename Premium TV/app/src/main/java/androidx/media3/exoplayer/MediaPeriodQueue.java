package androidx.media3.exoplayer;

import android.util.Pair;
import androidx.media3.common.C;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.HandlerWrapper;
import androidx.media3.exoplayer.analytics.AnalyticsCollector;
import androidx.media3.exoplayer.source.MediaPeriod;
import androidx.media3.exoplayer.source.MediaSource;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class MediaPeriodQueue {
    public static final long INITIAL_RENDERER_POSITION_OFFSET_US = 1000000000000L;
    private static final int MAXIMUM_BUFFER_AHEAD_PERIODS = 100;
    private final AnalyticsCollector analyticsCollector;
    private final HandlerWrapper analyticsCollectorHandler;
    private int length;
    private MediaPeriodHolder loading;
    private final MediaPeriodHolder.Factory mediaPeriodHolderFactory;
    private long nextWindowSequenceNumber;
    private Object oldFrontPeriodUid;
    private long oldFrontPeriodWindowSequenceNumber;
    private MediaPeriodHolder playing;
    private ExoPlayer.PreloadConfiguration preloadConfiguration;
    private MediaPeriodHolder reading;
    private int repeatMode;
    private boolean shuffleModeEnabled;
    private final Timeline.Period period = new Timeline.Period();
    private final Timeline.Window window = new Timeline.Window();
    private List<MediaPeriodHolder> preloadPriorityList = new ArrayList();

    public MediaPeriodQueue(AnalyticsCollector analyticsCollector, HandlerWrapper analyticsCollectorHandler, MediaPeriodHolder.Factory mediaPeriodHolderFactory, ExoPlayer.PreloadConfiguration preloadConfiguration) {
        this.analyticsCollector = analyticsCollector;
        this.analyticsCollectorHandler = analyticsCollectorHandler;
        this.mediaPeriodHolderFactory = mediaPeriodHolderFactory;
        this.preloadConfiguration = preloadConfiguration;
    }

    public boolean updateRepeatMode(Timeline timeline, int repeatMode) {
        this.repeatMode = repeatMode;
        return updateForPlaybackModeChange(timeline);
    }

    public boolean updateShuffleModeEnabled(Timeline timeline, boolean shuffleModeEnabled) {
        this.shuffleModeEnabled = shuffleModeEnabled;
        return updateForPlaybackModeChange(timeline);
    }

    public void updatePreloadConfiguration(Timeline timeline, ExoPlayer.PreloadConfiguration preloadConfiguration) {
        this.preloadConfiguration = preloadConfiguration;
        invalidatePreloadPool(timeline);
    }

    public boolean isLoading(MediaPeriod mediaPeriod) {
        return this.loading != null && this.loading.mediaPeriod == mediaPeriod;
    }

    public void reevaluateBuffer(long rendererPositionUs) {
        if (this.loading != null) {
            this.loading.reevaluateBuffer(rendererPositionUs);
        }
    }

    public boolean shouldLoadNextMediaPeriod() {
        return this.loading == null || (!this.loading.info.isFinal && this.loading.isFullyBuffered() && this.loading.info.durationUs != C.TIME_UNSET && this.length < 100);
    }

    public MediaPeriodInfo getNextMediaPeriodInfo(long rendererPositionUs, PlaybackInfo playbackInfo) {
        if (this.loading == null) {
            return getFirstMediaPeriodInfo(playbackInfo);
        }
        return getFollowingMediaPeriodInfo(playbackInfo.timeline, this.loading, rendererPositionUs);
    }

    public MediaPeriodHolder enqueueNextMediaPeriodHolder(MediaPeriodInfo info) {
        long rendererPositionOffsetUs;
        if (this.loading == null) {
            rendererPositionOffsetUs = INITIAL_RENDERER_POSITION_OFFSET_US;
        } else {
            rendererPositionOffsetUs = (this.loading.getRendererOffset() + this.loading.info.durationUs) - info.startPositionUs;
        }
        MediaPeriodHolder newPeriodHolder = removePreloadedMediaPeriodHolder(info);
        if (newPeriodHolder == null) {
            newPeriodHolder = this.mediaPeriodHolderFactory.create(info, rendererPositionOffsetUs);
        } else {
            newPeriodHolder.info = info;
            newPeriodHolder.setRendererOffset(rendererPositionOffsetUs);
        }
        if (this.loading != null) {
            this.loading.setNext(newPeriodHolder);
        } else {
            this.playing = newPeriodHolder;
            this.reading = newPeriodHolder;
        }
        this.oldFrontPeriodUid = null;
        this.loading = newPeriodHolder;
        this.length++;
        notifyQueueUpdate();
        return newPeriodHolder;
    }

    public void invalidatePreloadPool(Timeline timeline) {
        long windowSequenceNumber;
        if (this.preloadConfiguration.targetPreloadDurationUs == C.TIME_UNSET || this.loading == null) {
            releasePreloadPool();
            return;
        }
        MediaPeriodHolder loading = this.loading;
        List<MediaPeriodHolder> newPreloadPriorityList = new ArrayList<>();
        Pair<Object, Long> defaultPositionOfNextWindow = getDefaultPeriodPositionOfNextWindow(timeline, loading.info.id.periodUid, 0L);
        if (defaultPositionOfNextWindow != null && !timeline.getWindow(timeline.getPeriodByUid(defaultPositionOfNextWindow.first, this.period).windowIndex, this.window).isLive()) {
            long windowSequenceNumber2 = resolvePeriodUidToWindowSequenceNumberInPreloadPeriods(defaultPositionOfNextWindow.first);
            if (windowSequenceNumber2 != -1) {
                windowSequenceNumber = windowSequenceNumber2;
            } else {
                long windowSequenceNumber3 = this.nextWindowSequenceNumber;
                this.nextWindowSequenceNumber = 1 + windowSequenceNumber3;
                windowSequenceNumber = windowSequenceNumber3;
            }
            MediaPeriodInfo nextInfo = getMediaPeriodInfoForPeriodPosition(timeline, defaultPositionOfNextWindow.first, ((Long) defaultPositionOfNextWindow.second).longValue(), windowSequenceNumber);
            MediaPeriodHolder nextMediaPeriodHolder = removePreloadedMediaPeriodHolder(nextInfo);
            if (nextMediaPeriodHolder == null) {
                long rendererPositionOffsetUs = (loading.getRendererOffset() + loading.info.durationUs) - nextInfo.startPositionUs;
                nextMediaPeriodHolder = this.mediaPeriodHolderFactory.create(nextInfo, rendererPositionOffsetUs);
            }
            newPreloadPriorityList.add(nextMediaPeriodHolder);
        }
        releaseAndResetPreloadPriorityList(newPreloadPriorityList);
    }

    public void releasePreloadPool() {
        if (!this.preloadPriorityList.isEmpty()) {
            releaseAndResetPreloadPriorityList(new ArrayList());
        }
    }

    private MediaPeriodHolder removePreloadedMediaPeriodHolder(MediaPeriodInfo info) {
        for (int i = 0; i < this.preloadPriorityList.size(); i++) {
            MediaPeriodHolder mediaPeriodHolder = this.preloadPriorityList.get(i);
            if (mediaPeriodHolder.canBeUsedForMediaPeriodInfo(info)) {
                return this.preloadPriorityList.remove(i);
            }
        }
        return null;
    }

    private void releaseAndResetPreloadPriorityList(List<MediaPeriodHolder> newPriorityList) {
        for (int i = 0; i < this.preloadPriorityList.size(); i++) {
            this.preloadPriorityList.get(i).release();
        }
        this.preloadPriorityList = newPriorityList;
    }

    private MediaPeriodInfo getMediaPeriodInfoForPeriodPosition(Timeline timeline, Object periodUid, long positionUs, long windowSequenceNumber) {
        MediaSource.MediaPeriodId mediaPeriodId = resolveMediaPeriodIdForAds(timeline, periodUid, positionUs, windowSequenceNumber, this.window, this.period);
        return mediaPeriodId.isAd() ? getMediaPeriodInfoForAd(timeline, mediaPeriodId.periodUid, mediaPeriodId.adGroupIndex, mediaPeriodId.adIndexInAdGroup, positionUs, mediaPeriodId.windowSequenceNumber) : getMediaPeriodInfoForContent(timeline, mediaPeriodId.periodUid, positionUs, C.TIME_UNSET, mediaPeriodId.windowSequenceNumber);
    }

    private Pair<Object, Long> getDefaultPeriodPositionOfNextWindow(Timeline timeline, Object periodUid, long defaultPositionProjectionUs) {
        int nextWindowIndex = timeline.getNextWindowIndex(timeline.getPeriodByUid(periodUid, this.period).windowIndex, this.repeatMode, this.shuffleModeEnabled);
        if (nextWindowIndex != -1) {
            return timeline.getPeriodPositionUs(this.window, this.period, nextWindowIndex, C.TIME_UNSET, defaultPositionProjectionUs);
        }
        return null;
    }

    public MediaPeriodHolder getLoadingPeriod() {
        return this.loading;
    }

    public MediaPeriodHolder getPlayingPeriod() {
        return this.playing;
    }

    public MediaPeriodHolder getReadingPeriod() {
        return this.reading;
    }

    public MediaPeriodHolder advanceReadingPeriod() {
        this.reading = ((MediaPeriodHolder) Assertions.checkStateNotNull(this.reading)).getNext();
        notifyQueueUpdate();
        return (MediaPeriodHolder) Assertions.checkStateNotNull(this.reading);
    }

    public MediaPeriodHolder advancePlayingPeriod() {
        if (this.playing == null) {
            return null;
        }
        if (this.playing == this.reading) {
            this.reading = this.playing.getNext();
        }
        this.playing.release();
        this.length--;
        if (this.length == 0) {
            this.loading = null;
            this.oldFrontPeriodUid = this.playing.uid;
            this.oldFrontPeriodWindowSequenceNumber = this.playing.info.id.windowSequenceNumber;
        }
        this.playing = this.playing.getNext();
        notifyQueueUpdate();
        return this.playing;
    }

    public boolean removeAfter(MediaPeriodHolder mediaPeriodHolder) {
        Assertions.checkStateNotNull(mediaPeriodHolder);
        if (mediaPeriodHolder.equals(this.loading)) {
            return false;
        }
        boolean removedReading = false;
        this.loading = mediaPeriodHolder;
        while (mediaPeriodHolder.getNext() != null) {
            mediaPeriodHolder = (MediaPeriodHolder) Assertions.checkNotNull(mediaPeriodHolder.getNext());
            if (mediaPeriodHolder == this.reading) {
                this.reading = this.playing;
                removedReading = true;
            }
            mediaPeriodHolder.release();
            this.length--;
        }
        ((MediaPeriodHolder) Assertions.checkNotNull(this.loading)).setNext(null);
        notifyQueueUpdate();
        return removedReading;
    }

    public void clear() {
        if (this.length == 0) {
            return;
        }
        MediaPeriodHolder front = (MediaPeriodHolder) Assertions.checkStateNotNull(this.playing);
        this.oldFrontPeriodUid = front.uid;
        this.oldFrontPeriodWindowSequenceNumber = front.info.id.windowSequenceNumber;
        while (front != null) {
            front.release();
            front = front.getNext();
        }
        this.playing = null;
        this.loading = null;
        this.reading = null;
        this.length = 0;
        notifyQueueUpdate();
    }

    public boolean updateQueuedPeriods(Timeline timeline, long rendererPositionUs, long maxRendererReadPositionUs) {
        MediaPeriodInfo newPeriodInfo;
        long newDurationInRendererTime;
        MediaPeriodHolder previousPeriodHolder = null;
        MediaPeriodHolder periodHolder = this.playing;
        while (periodHolder != null) {
            MediaPeriodInfo oldPeriodInfo = periodHolder.info;
            if (previousPeriodHolder == null) {
                newPeriodInfo = getUpdatedMediaPeriodInfo(timeline, oldPeriodInfo);
            } else {
                newPeriodInfo = getFollowingMediaPeriodInfo(timeline, previousPeriodHolder, rendererPositionUs);
                if (newPeriodInfo == null) {
                    return true ^ removeAfter(previousPeriodHolder);
                }
                if (!canKeepMediaPeriodHolder(oldPeriodInfo, newPeriodInfo)) {
                    return true ^ removeAfter(previousPeriodHolder);
                }
            }
            periodHolder.info = newPeriodInfo.copyWithRequestedContentPositionUs(oldPeriodInfo.requestedContentPositionUs);
            if (!areDurationsCompatible(oldPeriodInfo.durationUs, newPeriodInfo.durationUs)) {
                periodHolder.updateClipping();
                if (newPeriodInfo.durationUs == C.TIME_UNSET) {
                    newDurationInRendererTime = Long.MAX_VALUE;
                } else {
                    newDurationInRendererTime = periodHolder.toRendererTime(newPeriodInfo.durationUs);
                }
                boolean isReadingAndReadBeyondNewDuration = periodHolder == this.reading && !periodHolder.info.isFollowedByTransitionToSameStream && (maxRendererReadPositionUs == Long.MIN_VALUE || maxRendererReadPositionUs >= newDurationInRendererTime);
                boolean readingPeriodRemoved = removeAfter(periodHolder);
                return (readingPeriodRemoved || isReadingAndReadBeyondNewDuration) ? false : true;
            }
            previousPeriodHolder = periodHolder;
            periodHolder = periodHolder.getNext();
        }
        return true;
    }

    public MediaPeriodInfo getUpdatedMediaPeriodInfo(Timeline timeline, MediaPeriodInfo info) {
        long endPositionUs;
        long durationUs;
        boolean isFollowedByTransitionToSameStream;
        MediaSource.MediaPeriodId id = info.id;
        boolean isLastInPeriod = isLastInPeriod(id);
        boolean isLastInWindow = isLastInWindow(timeline, id);
        boolean isLastInTimeline = isLastInTimeline(timeline, id, isLastInPeriod);
        timeline.getPeriodByUid(info.id.periodUid, this.period);
        if (id.isAd() || id.nextAdGroupIndex == -1) {
            endPositionUs = -9223372036854775807L;
        } else {
            endPositionUs = this.period.getAdGroupTimeUs(id.nextAdGroupIndex);
        }
        if (id.isAd()) {
            durationUs = this.period.getAdDurationUs(id.adGroupIndex, id.adIndexInAdGroup);
        } else if (endPositionUs == C.TIME_UNSET || endPositionUs == Long.MIN_VALUE) {
            durationUs = this.period.getDurationUs();
        } else {
            durationUs = endPositionUs;
        }
        if (id.isAd()) {
            isFollowedByTransitionToSameStream = this.period.isServerSideInsertedAdGroup(id.adGroupIndex);
        } else {
            isFollowedByTransitionToSameStream = id.nextAdGroupIndex != -1 && this.period.isServerSideInsertedAdGroup(id.nextAdGroupIndex);
        }
        return new MediaPeriodInfo(id, info.startPositionUs, info.requestedContentPositionUs, endPositionUs, durationUs, isFollowedByTransitionToSameStream, isLastInPeriod, isLastInWindow, isLastInTimeline);
    }

    public MediaSource.MediaPeriodId resolveMediaPeriodIdForAds(Timeline timeline, Object periodUid, long positionUs) {
        long windowSequenceNumber = resolvePeriodUidToWindowSequenceNumber(timeline, periodUid);
        return resolveMediaPeriodIdForAds(timeline, periodUid, positionUs, windowSequenceNumber, this.window, this.period);
    }

    private static MediaSource.MediaPeriodId resolveMediaPeriodIdForAds(Timeline timeline, Object periodUid, long positionUs, long windowSequenceNumber, Timeline.Window window, Timeline.Period period) {
        timeline.getPeriodByUid(periodUid, period);
        timeline.getWindow(period.windowIndex, window);
        Object periodUid2 = periodUid;
        for (int periodIndex = timeline.getIndexOfPeriod(periodUid); isSkippableAdPeriod(period) && periodIndex <= window.lastPeriodIndex; periodIndex++) {
            timeline.getPeriod(periodIndex, period, true);
            periodUid2 = Assertions.checkNotNull(period.uid);
        }
        timeline.getPeriodByUid(periodUid2, period);
        int adGroupIndex = period.getAdGroupIndexForPositionUs(positionUs);
        if (adGroupIndex == -1) {
            int nextAdGroupIndex = period.getAdGroupIndexAfterPositionUs(positionUs);
            return new MediaSource.MediaPeriodId(periodUid2, windowSequenceNumber, nextAdGroupIndex);
        }
        int adIndexInAdGroup = period.getFirstAdIndexToPlay(adGroupIndex);
        return new MediaSource.MediaPeriodId(periodUid2, adGroupIndex, adIndexInAdGroup, windowSequenceNumber);
    }

    private static boolean isSkippableAdPeriod(Timeline.Period period) {
        int adGroupCount = period.getAdGroupCount();
        if (adGroupCount == 0 || !((adGroupCount != 1 || !period.isLivePostrollPlaceholder(0)) && period.isServerSideInsertedAdGroup(period.getRemovedAdGroupCount()) && period.getAdGroupIndexForPositionUs(0L) == -1)) {
            return false;
        }
        if (period.durationUs == 0) {
            return true;
        }
        long contentResumeOffsetUs = 0;
        int lastIndexInclusive = adGroupCount - (period.isLivePostrollPlaceholder(adGroupCount + (-1)) ? 2 : 1);
        for (int i = 0; i <= lastIndexInclusive; i++) {
            contentResumeOffsetUs += period.getContentResumeOffsetUs(i);
        }
        return period.durationUs <= contentResumeOffsetUs;
    }

    public MediaSource.MediaPeriodId resolveMediaPeriodIdForAdsAfterPeriodPositionChange(Timeline timeline, Object periodUid, long positionUs) {
        Object periodUidToPlay;
        long windowSequenceNumber = resolvePeriodUidToWindowSequenceNumber(timeline, periodUid);
        timeline.getPeriodByUid(periodUid, this.period);
        timeline.getWindow(this.period.windowIndex, this.window);
        Object periodUidToPlay2 = periodUid;
        boolean seenAdPeriod = false;
        for (int i = timeline.getIndexOfPeriod(periodUid); i >= this.window.firstPeriodIndex; i--) {
            timeline.getPeriod(i, this.period, true);
            boolean isAdPeriod = this.period.getAdGroupCount() > 0;
            seenAdPeriod |= isAdPeriod;
            if (this.period.getAdGroupIndexForPositionUs(this.period.durationUs) != -1) {
                periodUidToPlay2 = Assertions.checkNotNull(this.period.uid);
            }
            if (seenAdPeriod && (!isAdPeriod || this.period.durationUs != 0)) {
                periodUidToPlay = periodUidToPlay2;
                return resolveMediaPeriodIdForAds(timeline, periodUidToPlay, positionUs, windowSequenceNumber, this.window, this.period);
            }
        }
        periodUidToPlay = periodUidToPlay2;
        return resolveMediaPeriodIdForAds(timeline, periodUidToPlay, positionUs, windowSequenceNumber, this.window, this.period);
    }

    private void notifyQueueUpdate() {
        final ImmutableList.Builder<MediaSource.MediaPeriodId> builder = ImmutableList.builder();
        for (MediaPeriodHolder period = this.playing; period != null; period = period.getNext()) {
            builder.add(period.info.id);
        }
        final MediaSource.MediaPeriodId readingPeriodId = this.reading == null ? null : this.reading.info.id;
        this.analyticsCollectorHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.MediaPeriodQueue$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m49x6b40a91a(builder, readingPeriodId);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$notifyQueueUpdate$0$androidx-media3-exoplayer-MediaPeriodQueue, reason: not valid java name */
    /* synthetic */ void m49x6b40a91a(ImmutableList.Builder builder, MediaSource.MediaPeriodId readingPeriodId) {
        this.analyticsCollector.updateMediaPeriodQueueInfo(builder.build(), readingPeriodId);
    }

    private long resolvePeriodUidToWindowSequenceNumber(Timeline timeline, Object periodUid) {
        int oldFrontPeriodIndex;
        int windowIndex = timeline.getPeriodByUid(periodUid, this.period).windowIndex;
        if (this.oldFrontPeriodUid != null && (oldFrontPeriodIndex = timeline.getIndexOfPeriod(this.oldFrontPeriodUid)) != -1) {
            int oldFrontWindowIndex = timeline.getPeriod(oldFrontPeriodIndex, this.period).windowIndex;
            if (oldFrontWindowIndex == windowIndex) {
                return this.oldFrontPeriodWindowSequenceNumber;
            }
        }
        for (MediaPeriodHolder mediaPeriodHolder = this.playing; mediaPeriodHolder != null; mediaPeriodHolder = mediaPeriodHolder.getNext()) {
            if (mediaPeriodHolder.uid.equals(periodUid)) {
                return mediaPeriodHolder.info.id.windowSequenceNumber;
            }
        }
        for (MediaPeriodHolder mediaPeriodHolder2 = this.playing; mediaPeriodHolder2 != null; mediaPeriodHolder2 = mediaPeriodHolder2.getNext()) {
            int indexOfHolderInTimeline = timeline.getIndexOfPeriod(mediaPeriodHolder2.uid);
            if (indexOfHolderInTimeline != -1) {
                int holderWindowIndex = timeline.getPeriod(indexOfHolderInTimeline, this.period).windowIndex;
                if (holderWindowIndex == windowIndex) {
                    return mediaPeriodHolder2.info.id.windowSequenceNumber;
                }
            }
        }
        long windowSequenceNumber = resolvePeriodUidToWindowSequenceNumberInPreloadPeriods(periodUid);
        if (windowSequenceNumber != -1) {
            return windowSequenceNumber;
        }
        long windowSequenceNumber2 = this.nextWindowSequenceNumber;
        this.nextWindowSequenceNumber = 1 + windowSequenceNumber2;
        if (this.playing == null) {
            this.oldFrontPeriodUid = periodUid;
            this.oldFrontPeriodWindowSequenceNumber = windowSequenceNumber2;
        }
        return windowSequenceNumber2;
    }

    private long resolvePeriodUidToWindowSequenceNumberInPreloadPeriods(Object periodUid) {
        for (int i = 0; i < this.preloadPriorityList.size(); i++) {
            MediaPeriodHolder preloadHolder = this.preloadPriorityList.get(i);
            if (preloadHolder.uid.equals(periodUid)) {
                return preloadHolder.info.id.windowSequenceNumber;
            }
        }
        return -1L;
    }

    private boolean canKeepMediaPeriodHolder(MediaPeriodInfo oldInfo, MediaPeriodInfo newInfo) {
        return oldInfo.startPositionUs == newInfo.startPositionUs && oldInfo.id.equals(newInfo.id);
    }

    static boolean areDurationsCompatible(long previousDurationUs, long newDurationUs) {
        return previousDurationUs == C.TIME_UNSET || previousDurationUs == newDurationUs;
    }

    private boolean updateForPlaybackModeChange(Timeline timeline) {
        Timeline timeline2;
        MediaPeriodHolder lastValidPeriodHolder = this.playing;
        if (lastValidPeriodHolder == null) {
            return true;
        }
        int currentPeriodIndex = timeline.getIndexOfPeriod(lastValidPeriodHolder.uid);
        while (true) {
            timeline2 = timeline;
            int nextPeriodIndex = timeline2.getNextPeriodIndex(currentPeriodIndex, this.period, this.window, this.repeatMode, this.shuffleModeEnabled);
            while (((MediaPeriodHolder) Assertions.checkNotNull(lastValidPeriodHolder)).getNext() != null && !lastValidPeriodHolder.info.isLastInTimelinePeriod) {
                lastValidPeriodHolder = lastValidPeriodHolder.getNext();
            }
            MediaPeriodHolder nextMediaPeriodHolder = lastValidPeriodHolder.getNext();
            if (nextPeriodIndex == -1 || nextMediaPeriodHolder == null) {
                break;
            }
            int nextPeriodHolderPeriodIndex = timeline2.getIndexOfPeriod(nextMediaPeriodHolder.uid);
            if (nextPeriodHolderPeriodIndex != nextPeriodIndex) {
                break;
            }
            lastValidPeriodHolder = nextMediaPeriodHolder;
            currentPeriodIndex = nextPeriodIndex;
            timeline = timeline2;
        }
        boolean readingPeriodRemoved = removeAfter(lastValidPeriodHolder);
        lastValidPeriodHolder.info = getUpdatedMediaPeriodInfo(timeline2, lastValidPeriodHolder.info);
        return !readingPeriodRemoved;
    }

    private MediaPeriodInfo getFirstMediaPeriodInfo(PlaybackInfo playbackInfo) {
        return getMediaPeriodInfo(playbackInfo.timeline, playbackInfo.periodId, playbackInfo.requestedContentPositionUs, playbackInfo.positionUs);
    }

    private MediaPeriodInfo getFollowingMediaPeriodInfo(Timeline timeline, MediaPeriodHolder mediaPeriodHolder, long rendererPositionUs) {
        MediaPeriodInfo mediaPeriodInfo = mediaPeriodHolder.info;
        long bufferedDurationUs = (mediaPeriodHolder.getRendererOffset() + mediaPeriodInfo.durationUs) - rendererPositionUs;
        if (mediaPeriodInfo.isLastInTimelinePeriod) {
            return getFirstMediaPeriodInfoOfNextPeriod(timeline, mediaPeriodHolder, bufferedDurationUs);
        }
        return getFollowingMediaPeriodInfoOfCurrentPeriod(timeline, mediaPeriodHolder, bufferedDurationUs);
    }

    /* JADX WARN: Code duplicated, block: B:31:0x00f9  */
    private MediaPeriodInfo getFirstMediaPeriodInfoOfNextPeriod(Timeline timeline, MediaPeriodHolder mediaPeriodHolder, long bufferedDurationUs) {
        long contentPositionUs;
        Object nextPeriodUid;
        long startPositionUs;
        long startPositionUs2;
        long startPositionUs3;
        long j;
        MediaPeriodInfo mediaPeriodInfo = mediaPeriodHolder.info;
        int currentPeriodIndex = timeline.getIndexOfPeriod(mediaPeriodInfo.id.periodUid);
        int nextPeriodIndex = timeline.getNextPeriodIndex(currentPeriodIndex, this.period, this.window, this.repeatMode, this.shuffleModeEnabled);
        if (nextPeriodIndex == -1) {
            return null;
        }
        int nextWindowIndex = timeline.getPeriod(nextPeriodIndex, this.period, true).windowIndex;
        Object nextPeriodUid2 = Assertions.checkNotNull(this.period.uid);
        long windowSequenceNumber = mediaPeriodInfo.id.windowSequenceNumber;
        if (timeline.getWindow(nextWindowIndex, this.window).firstPeriodIndex != nextPeriodIndex) {
            contentPositionUs = 0;
            nextPeriodUid = nextPeriodUid2;
            startPositionUs = 0;
        } else {
            contentPositionUs = C.TIME_UNSET;
            Pair<Object, Long> defaultPositionUs = timeline.getPeriodPositionUs(this.window, this.period, nextWindowIndex, C.TIME_UNSET, Math.max(0L, bufferedDurationUs));
            if (defaultPositionUs == null) {
                return null;
            }
            Object nextPeriodUid3 = defaultPositionUs.first;
            startPositionUs = ((Long) defaultPositionUs.second).longValue();
            MediaPeriodHolder nextMediaPeriodHolder = mediaPeriodHolder.getNext();
            if (nextMediaPeriodHolder != null && nextMediaPeriodHolder.uid.equals(nextPeriodUid3)) {
                nextPeriodUid = nextPeriodUid3;
                windowSequenceNumber = nextMediaPeriodHolder.info.id.windowSequenceNumber;
            } else {
                long windowSequenceNumberFromPreload = resolvePeriodUidToWindowSequenceNumberInPreloadPeriods(nextPeriodUid3);
                if (windowSequenceNumberFromPreload == -1) {
                    long j2 = this.nextWindowSequenceNumber;
                    this.nextWindowSequenceNumber = j2 + 1;
                    j = j2;
                } else {
                    j = windowSequenceNumberFromPreload;
                }
                windowSequenceNumber = j;
                nextPeriodUid = nextPeriodUid3;
            }
        }
        MediaSource.MediaPeriodId periodId = resolveMediaPeriodIdForAds(timeline, nextPeriodUid, startPositionUs, windowSequenceNumber, this.window, this.period);
        if (contentPositionUs != C.TIME_UNSET && mediaPeriodInfo.requestedContentPositionUs != C.TIME_UNSET) {
            boolean precedingPeriodHasServerSideInsertedAds = hasServerSideInsertedAds(mediaPeriodInfo.id.periodUid, timeline);
            if (periodId.isAd() && precedingPeriodHasServerSideInsertedAds) {
                startPositionUs2 = startPositionUs;
                startPositionUs3 = mediaPeriodInfo.requestedContentPositionUs;
            } else if (precedingPeriodHasServerSideInsertedAds) {
                long startPositionUs4 = mediaPeriodInfo.requestedContentPositionUs;
                startPositionUs2 = startPositionUs4;
                startPositionUs3 = contentPositionUs;
            } else {
                startPositionUs2 = startPositionUs;
                startPositionUs3 = contentPositionUs;
            }
        } else {
            startPositionUs2 = startPositionUs;
            startPositionUs3 = contentPositionUs;
        }
        return getMediaPeriodInfo(timeline, periodId, startPositionUs3, startPositionUs2);
    }

    private MediaPeriodInfo getFollowingMediaPeriodInfoOfCurrentPeriod(Timeline timeline, MediaPeriodHolder mediaPeriodHolder, long bufferedDurationUs) {
        Timeline timeline2;
        long startPositionUs;
        MediaPeriodInfo mediaPeriodInfo = mediaPeriodHolder.info;
        MediaSource.MediaPeriodId currentPeriodId = mediaPeriodInfo.id;
        timeline.getPeriodByUid(currentPeriodId.periodUid, this.period);
        if (currentPeriodId.isAd()) {
            int adGroupIndex = currentPeriodId.adGroupIndex;
            int adCountInCurrentAdGroup = this.period.getAdCountInAdGroup(adGroupIndex);
            if (adCountInCurrentAdGroup == -1) {
                return null;
            }
            int nextAdIndexInAdGroup = this.period.getNextAdIndexToPlay(adGroupIndex, currentPeriodId.adIndexInAdGroup);
            if (nextAdIndexInAdGroup < adCountInCurrentAdGroup) {
                return getMediaPeriodInfoForAd(timeline, currentPeriodId.periodUid, adGroupIndex, nextAdIndexInAdGroup, mediaPeriodInfo.requestedContentPositionUs, currentPeriodId.windowSequenceNumber);
            }
            long startPositionUs2 = mediaPeriodInfo.requestedContentPositionUs;
            if (startPositionUs2 != C.TIME_UNSET) {
                timeline2 = timeline;
                startPositionUs = startPositionUs2;
            } else {
                timeline2 = timeline;
                Pair<Object, Long> defaultPositionUs = timeline2.getPeriodPositionUs(this.window, this.period, this.period.windowIndex, C.TIME_UNSET, Math.max(0L, bufferedDurationUs));
                if (defaultPositionUs == null) {
                    return null;
                }
                startPositionUs = ((Long) defaultPositionUs.second).longValue();
            }
            long minStartPositionUs = getMinStartPositionAfterAdGroupUs(timeline2, currentPeriodId.periodUid, currentPeriodId.adGroupIndex);
            Object obj = currentPeriodId.periodUid;
            long minStartPositionUs2 = Math.max(minStartPositionUs, startPositionUs);
            long j = mediaPeriodInfo.requestedContentPositionUs;
            long minStartPositionUs3 = currentPeriodId.windowSequenceNumber;
            return getMediaPeriodInfoForContent(timeline2, obj, minStartPositionUs2, j, minStartPositionUs3);
        }
        if (currentPeriodId.nextAdGroupIndex != -1 && this.period.isLivePostrollPlaceholder(currentPeriodId.nextAdGroupIndex)) {
            return getFirstMediaPeriodInfoOfNextPeriod(timeline, mediaPeriodHolder, bufferedDurationUs);
        }
        int adIndexInAdGroup = this.period.getFirstAdIndexToPlay(currentPeriodId.nextAdGroupIndex);
        boolean isPlayedServerSideInsertedAd = this.period.isServerSideInsertedAdGroup(currentPeriodId.nextAdGroupIndex) && this.period.getAdState(currentPeriodId.nextAdGroupIndex, adIndexInAdGroup) == 3;
        if (adIndexInAdGroup == this.period.getAdCountInAdGroup(currentPeriodId.nextAdGroupIndex) || isPlayedServerSideInsertedAd) {
            return getMediaPeriodInfoForContent(timeline, currentPeriodId.periodUid, getMinStartPositionAfterAdGroupUs(timeline, currentPeriodId.periodUid, currentPeriodId.nextAdGroupIndex), mediaPeriodInfo.durationUs, currentPeriodId.windowSequenceNumber);
        }
        return getMediaPeriodInfoForAd(timeline, currentPeriodId.periodUid, currentPeriodId.nextAdGroupIndex, adIndexInAdGroup, mediaPeriodInfo.durationUs, currentPeriodId.windowSequenceNumber);
    }

    private boolean hasServerSideInsertedAds(Object periodUid, Timeline timeline) {
        int adGroupCount = timeline.getPeriodByUid(periodUid, this.period).getAdGroupCount();
        int firstAdGroupIndex = this.period.getRemovedAdGroupCount();
        return adGroupCount > 0 && this.period.isServerSideInsertedAdGroup(firstAdGroupIndex) && (adGroupCount > 1 || this.period.getAdGroupTimeUs(firstAdGroupIndex) != Long.MIN_VALUE);
    }

    private MediaPeriodInfo getMediaPeriodInfo(Timeline timeline, MediaSource.MediaPeriodId id, long requestedContentPositionUs, long startPositionUs) {
        timeline.getPeriodByUid(id.periodUid, this.period);
        if (id.isAd()) {
            return getMediaPeriodInfoForAd(timeline, id.periodUid, id.adGroupIndex, id.adIndexInAdGroup, requestedContentPositionUs, id.windowSequenceNumber);
        }
        return getMediaPeriodInfoForContent(timeline, id.periodUid, startPositionUs, requestedContentPositionUs, id.windowSequenceNumber);
    }

    private MediaPeriodInfo getMediaPeriodInfoForAd(Timeline timeline, Object periodUid, int adGroupIndex, int adIndexInAdGroup, long contentPositionUs, long windowSequenceNumber) {
        long startPositionUs;
        MediaSource.MediaPeriodId id = new MediaSource.MediaPeriodId(periodUid, adGroupIndex, adIndexInAdGroup, windowSequenceNumber);
        long durationUs = timeline.getPeriodByUid(id.periodUid, this.period).getAdDurationUs(id.adGroupIndex, id.adIndexInAdGroup);
        if (adIndexInAdGroup == this.period.getFirstAdIndexToPlay(adGroupIndex)) {
            startPositionUs = this.period.getAdResumePositionUs();
        } else {
            startPositionUs = 0;
        }
        boolean isFollowedByTransitionToSameStream = this.period.isServerSideInsertedAdGroup(id.adGroupIndex);
        if (durationUs != C.TIME_UNSET && startPositionUs >= durationUs) {
            startPositionUs = Math.max(0L, durationUs - 1);
        }
        return new MediaPeriodInfo(id, startPositionUs, contentPositionUs, C.TIME_UNSET, durationUs, isFollowedByTransitionToSameStream, false, false, false);
    }

    private MediaPeriodInfo getMediaPeriodInfoForContent(Timeline timeline, Object periodUid, long startPositionUs, long requestedContentPositionUs, long windowSequenceNumber) {
        long endPositionUs;
        long durationUs;
        long startPositionUs2;
        timeline.getPeriodByUid(periodUid, this.period);
        int nextAdGroupIndex = this.period.getAdGroupIndexAfterPositionUs(startPositionUs);
        boolean isNextAdGroupPostrollPlaceholder = nextAdGroupIndex != -1 && this.period.isLivePostrollPlaceholder(nextAdGroupIndex);
        boolean clipPeriodAtContentDuration = false;
        Timeline.Period period = this.period;
        if (nextAdGroupIndex == -1) {
            clipPeriodAtContentDuration = period.getAdGroupCount() > 0 && this.period.isServerSideInsertedAdGroup(this.period.getRemovedAdGroupCount());
        } else if (period.isServerSideInsertedAdGroup(nextAdGroupIndex) && this.period.getAdGroupTimeUs(nextAdGroupIndex) == this.period.durationUs && this.period.hasPlayedAdGroup(nextAdGroupIndex)) {
            nextAdGroupIndex = -1;
            clipPeriodAtContentDuration = true;
        }
        MediaSource.MediaPeriodId id = new MediaSource.MediaPeriodId(periodUid, windowSequenceNumber, nextAdGroupIndex);
        boolean isLastInPeriod = isLastInPeriod(id);
        boolean isLastInWindow = isLastInWindow(timeline, id);
        boolean isLastInTimeline = isLastInTimeline(timeline, id, isLastInPeriod);
        boolean isFollowedByTransitionToSameStream = (nextAdGroupIndex == -1 || !this.period.isServerSideInsertedAdGroup(nextAdGroupIndex) || isNextAdGroupPostrollPlaceholder) ? false : true;
        if (nextAdGroupIndex != -1 && !isNextAdGroupPostrollPlaceholder) {
            endPositionUs = this.period.getAdGroupTimeUs(nextAdGroupIndex);
        } else if (clipPeriodAtContentDuration) {
            endPositionUs = this.period.durationUs;
        } else {
            endPositionUs = -9223372036854775807L;
        }
        if (endPositionUs == C.TIME_UNSET || endPositionUs == Long.MIN_VALUE) {
            durationUs = this.period.durationUs;
        } else {
            durationUs = endPositionUs;
        }
        if (durationUs != C.TIME_UNSET && startPositionUs >= durationUs) {
            boolean endAtLastFrame = isLastInTimeline || !clipPeriodAtContentDuration;
            startPositionUs2 = Math.max(0L, durationUs - ((long) (endAtLastFrame ? 1 : 0)));
        } else {
            startPositionUs2 = startPositionUs;
        }
        return new MediaPeriodInfo(id, startPositionUs2, requestedContentPositionUs, endPositionUs, durationUs, isFollowedByTransitionToSameStream, isLastInPeriod, isLastInWindow, isLastInTimeline);
    }

    private boolean isLastInPeriod(MediaSource.MediaPeriodId id) {
        return !id.isAd() && id.nextAdGroupIndex == -1;
    }

    private boolean isLastInWindow(Timeline timeline, MediaSource.MediaPeriodId id) {
        if (!isLastInPeriod(id)) {
            return false;
        }
        int windowIndex = timeline.getPeriodByUid(id.periodUid, this.period).windowIndex;
        int periodIndex = timeline.getIndexOfPeriod(id.periodUid);
        return timeline.getWindow(windowIndex, this.window).lastPeriodIndex == periodIndex;
    }

    private boolean isLastInTimeline(Timeline timeline, MediaSource.MediaPeriodId id, boolean isLastMediaPeriodInPeriod) {
        int periodIndex = timeline.getIndexOfPeriod(id.periodUid);
        int windowIndex = timeline.getPeriod(periodIndex, this.period).windowIndex;
        return !timeline.getWindow(windowIndex, this.window).isDynamic && timeline.isLastPeriod(periodIndex, this.period, this.window, this.repeatMode, this.shuffleModeEnabled) && isLastMediaPeriodInPeriod;
    }

    private long getMinStartPositionAfterAdGroupUs(Timeline timeline, Object periodUid, int adGroupIndex) {
        timeline.getPeriodByUid(periodUid, this.period);
        long startPositionUs = this.period.getAdGroupTimeUs(adGroupIndex);
        Timeline.Period period = this.period;
        if (startPositionUs == Long.MIN_VALUE) {
            return period.durationUs;
        }
        return period.getContentResumeOffsetUs(adGroupIndex) + startPositionUs;
    }
}
