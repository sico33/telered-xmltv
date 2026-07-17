package androidx.media3.exoplayer.source.ads;

import android.os.Handler;
import android.util.Pair;
import androidx.media3.common.AdPlaybackState;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.StreamKey;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.TransferListener;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.exoplayer.FormatHolder;
import androidx.media3.exoplayer.LoadingInfo;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.drm.DrmSessionEventListener;
import androidx.media3.exoplayer.source.BaseMediaSource;
import androidx.media3.exoplayer.source.EmptySampleStream;
import androidx.media3.exoplayer.source.ForwardingTimeline;
import androidx.media3.exoplayer.source.LoadEventInfo;
import androidx.media3.exoplayer.source.MediaLoadData;
import androidx.media3.exoplayer.source.MediaPeriod;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.MediaSourceEventListener;
import androidx.media3.exoplayer.source.SampleStream;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.upstream.Allocator;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.UnmodifiableIterator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class ServerSideAdInsertionMediaSource extends BaseMediaSource implements MediaSource.MediaSourceCaller, MediaSourceEventListener, DrmSessionEventListener {
    private final AdPlaybackStateUpdater adPlaybackStateUpdater;
    private SharedMediaPeriod lastUsedMediaPeriod;
    private final MediaSource mediaSource;
    private Handler playbackHandler;
    private final ListMultimap<Pair<Long, Object>, SharedMediaPeriod> mediaPeriods = ArrayListMultimap.create();
    private ImmutableMap<Object, AdPlaybackState> adPlaybackStates = ImmutableMap.of();
    private final MediaSourceEventListener.EventDispatcher mediaSourceEventDispatcherWithoutId = createEventDispatcher(null);
    private final DrmSessionEventListener.EventDispatcher drmEventDispatcherWithoutId = createDrmEventDispatcher(null);

    public interface AdPlaybackStateUpdater {
        boolean onAdPlaybackStateUpdateRequested(Timeline timeline);
    }

    @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
    public /* synthetic */ void onDrmSessionAcquired(int i, MediaSource.MediaPeriodId mediaPeriodId) {
        DrmSessionEventListener.CC.$default$onDrmSessionAcquired(this, i, mediaPeriodId);
    }

    public ServerSideAdInsertionMediaSource(MediaSource mediaSource, AdPlaybackStateUpdater adPlaybackStateUpdater) {
        this.mediaSource = mediaSource;
        this.adPlaybackStateUpdater = adPlaybackStateUpdater;
    }

    public void setAdPlaybackStates(final ImmutableMap<Object, AdPlaybackState> adPlaybackStates, final Timeline contentTimeline) throws Throwable {
        Assertions.checkArgument(!adPlaybackStates.isEmpty());
        Object adsId = Assertions.checkNotNull(adPlaybackStates.values().asList().get(0).adsId);
        UnmodifiableIterator<Map.Entry<Object, AdPlaybackState>> it = adPlaybackStates.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Object, AdPlaybackState> entry = it.next();
            Object periodUid = entry.getKey();
            AdPlaybackState adPlaybackState = entry.getValue();
            Assertions.checkArgument(Util.areEqual(adsId, adPlaybackState.adsId));
            AdPlaybackState oldAdPlaybackState = this.adPlaybackStates.get(periodUid);
            if (oldAdPlaybackState != null) {
                int adGroupIndex = adPlaybackState.removedAdGroupCount;
                while (adGroupIndex < adPlaybackState.adGroupCount) {
                    AdPlaybackState.AdGroup adGroup = adPlaybackState.getAdGroup(adGroupIndex);
                    Assertions.checkArgument(adGroup.isServerSideInserted);
                    if (adGroupIndex < oldAdPlaybackState.adGroupCount && ServerSideAdInsertionUtil.getAdCountInGroup(adPlaybackState, adGroupIndex) < ServerSideAdInsertionUtil.getAdCountInGroup(oldAdPlaybackState, adGroupIndex)) {
                        AdPlaybackState.AdGroup nextAdGroup = adPlaybackState.getAdGroup(adGroupIndex + 1);
                        long sumOfSplitContentResumeOffsetUs = adGroup.contentResumeOffsetUs + nextAdGroup.contentResumeOffsetUs;
                        AdPlaybackState.AdGroup oldAdGroup = oldAdPlaybackState.getAdGroup(adGroupIndex);
                        Assertions.checkArgument(sumOfSplitContentResumeOffsetUs == oldAdGroup.contentResumeOffsetUs);
                        Assertions.checkArgument(adGroup.timeUs + adGroup.contentResumeOffsetUs == nextAdGroup.timeUs);
                    }
                    if (adGroup.timeUs == Long.MIN_VALUE) {
                        Assertions.checkArgument(ServerSideAdInsertionUtil.getAdCountInGroup(adPlaybackState, adGroupIndex) == 0);
                    }
                    adGroupIndex++;
                    adsId = adsId;
                }
            }
            adsId = adsId;
        }
        synchronized (this) {
            try {
                try {
                    if (this.playbackHandler != null) {
                        this.playbackHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.source.ads.ServerSideAdInsertionMediaSource$$ExternalSyntheticLambda0
                            @Override // java.lang.Runnable
                            public final void run() {
                                this.f$0.m127x8b1696bf(adPlaybackStates, contentTimeline);
                            }
                        });
                    } else {
                        this.adPlaybackStates = adPlaybackStates;
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX INFO: renamed from: lambda$setAdPlaybackStates$0$androidx-media3-exoplayer-source-ads-ServerSideAdInsertionMediaSource, reason: not valid java name */
    /* synthetic */ void m127x8b1696bf(ImmutableMap adPlaybackStates, Timeline contentTimeline) {
        AdPlaybackState adPlaybackState;
        for (SharedMediaPeriod mediaPeriod : this.mediaPeriods.values()) {
            AdPlaybackState adPlaybackState2 = (AdPlaybackState) adPlaybackStates.get(mediaPeriod.periodUid);
            if (adPlaybackState2 != null) {
                mediaPeriod.updateAdPlaybackState(adPlaybackState2);
            }
        }
        if (this.lastUsedMediaPeriod != null && (adPlaybackState = (AdPlaybackState) adPlaybackStates.get(this.lastUsedMediaPeriod.periodUid)) != null) {
            this.lastUsedMediaPeriod.updateAdPlaybackState(adPlaybackState);
        }
        this.adPlaybackStates = adPlaybackStates;
        refreshSourceInfo(new ServerSideAdInsertionTimeline(contentTimeline, adPlaybackStates));
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public MediaItem getMediaItem() {
        return this.mediaSource.getMediaItem();
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public boolean canUpdateMediaItem(MediaItem mediaItem) {
        return this.mediaSource.canUpdateMediaItem(mediaItem);
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public void updateMediaItem(MediaItem mediaItem) {
        this.mediaSource.updateMediaItem(mediaItem);
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource
    protected void prepareSourceInternal(TransferListener mediaTransferListener) {
        Handler handler = Util.createHandlerForCurrentLooper();
        synchronized (this) {
            this.playbackHandler = handler;
        }
        this.mediaSource.addEventListener(handler, this);
        this.mediaSource.addDrmEventListener(handler, this);
        this.mediaSource.prepareSource(this, mediaTransferListener, getPlayerId());
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public void maybeThrowSourceInfoRefreshError() throws IOException {
        this.mediaSource.maybeThrowSourceInfoRefreshError();
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource
    protected void enableInternal() {
        this.mediaSource.enable(this);
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource
    protected void disableInternal() {
        releaseLastUsedMediaPeriod();
        this.mediaSource.disable(this);
    }

    @Override // androidx.media3.exoplayer.source.MediaSource.MediaSourceCaller
    public void onSourceInfoRefreshed(MediaSource source, Timeline timeline) {
        if ((this.adPlaybackStateUpdater == null || !this.adPlaybackStateUpdater.onAdPlaybackStateUpdateRequested(timeline)) && !this.adPlaybackStates.isEmpty()) {
            refreshSourceInfo(new ServerSideAdInsertionTimeline(timeline, this.adPlaybackStates));
        }
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource
    protected void releaseSourceInternal() {
        releaseLastUsedMediaPeriod();
        synchronized (this) {
            this.playbackHandler = null;
        }
        this.mediaSource.releaseSource(this);
        this.mediaSource.removeEventListener(this);
        this.mediaSource.removeDrmEventListener(this);
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator, long startPositionUs) {
        SharedMediaPeriod sharedPeriod = null;
        Pair<Long, Object> sharedMediaPeriodKey = new Pair<>(Long.valueOf(id.windowSequenceNumber), id.periodUid);
        boolean reusedSharedPeriod = false;
        if (this.lastUsedMediaPeriod != null) {
            if (!this.lastUsedMediaPeriod.periodUid.equals(id.periodUid)) {
                this.lastUsedMediaPeriod.release(this.mediaSource);
            } else {
                sharedPeriod = this.lastUsedMediaPeriod;
                this.mediaPeriods.put(sharedMediaPeriodKey, sharedPeriod);
                reusedSharedPeriod = true;
            }
            this.lastUsedMediaPeriod = null;
        }
        if (sharedPeriod == null) {
            SharedMediaPeriod lastExistingPeriod = (SharedMediaPeriod) Iterables.getLast(this.mediaPeriods.get(sharedMediaPeriodKey), null);
            if (lastExistingPeriod != null && lastExistingPeriod.canReuseMediaPeriod(id, startPositionUs)) {
                sharedPeriod = lastExistingPeriod;
            } else {
                AdPlaybackState adPlaybackState = (AdPlaybackState) Assertions.checkNotNull(this.adPlaybackStates.get(id.periodUid));
                long streamPositionUs = ServerSideAdInsertionUtil.getStreamPositionUs(startPositionUs, id, adPlaybackState);
                sharedPeriod = new SharedMediaPeriod(this.mediaSource.createPeriod(new MediaSource.MediaPeriodId(id.periodUid, id.windowSequenceNumber), allocator, streamPositionUs), id.periodUid, adPlaybackState);
                this.mediaPeriods.put(sharedMediaPeriodKey, sharedPeriod);
            }
        }
        MediaPeriodImpl mediaPeriod = new MediaPeriodImpl(sharedPeriod, id, createEventDispatcher(id), createDrmEventDispatcher(id));
        sharedPeriod.add(mediaPeriod);
        if (reusedSharedPeriod && sharedPeriod.trackSelections.length > 0) {
            mediaPeriod.seekToUs(startPositionUs);
        }
        return mediaPeriod;
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public void releasePeriod(MediaPeriod mediaPeriod) {
        MediaPeriodImpl mediaPeriodImpl = (MediaPeriodImpl) mediaPeriod;
        mediaPeriodImpl.sharedPeriod.remove(mediaPeriodImpl);
        if (mediaPeriodImpl.sharedPeriod.isUnused()) {
            this.mediaPeriods.remove(new Pair(Long.valueOf(mediaPeriodImpl.mediaPeriodId.windowSequenceNumber), mediaPeriodImpl.mediaPeriodId.periodUid), mediaPeriodImpl.sharedPeriod);
            if (this.mediaPeriods.isEmpty()) {
                this.lastUsedMediaPeriod = mediaPeriodImpl.sharedPeriod;
            } else {
                mediaPeriodImpl.sharedPeriod.release(this.mediaSource);
            }
        }
    }

    @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
    public void onDrmSessionAcquired(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, int state) {
        MediaPeriodImpl mediaPeriod = getMediaPeriodForEvent(mediaPeriodId, null, true);
        if (mediaPeriod == null) {
            this.drmEventDispatcherWithoutId.drmSessionAcquired(state);
        } else {
            mediaPeriod.drmEventDispatcher.drmSessionAcquired(state);
        }
    }

    @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
    public void onDrmKeysLoaded(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        MediaPeriodImpl mediaPeriod = getMediaPeriodForEvent(mediaPeriodId, null, false);
        if (mediaPeriod == null) {
            this.drmEventDispatcherWithoutId.drmKeysLoaded();
        } else {
            mediaPeriod.drmEventDispatcher.drmKeysLoaded();
        }
    }

    @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
    public void onDrmSessionManagerError(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, Exception error) {
        MediaPeriodImpl mediaPeriod = getMediaPeriodForEvent(mediaPeriodId, null, false);
        if (mediaPeriod == null) {
            this.drmEventDispatcherWithoutId.drmSessionManagerError(error);
        } else {
            mediaPeriod.drmEventDispatcher.drmSessionManagerError(error);
        }
    }

    @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
    public void onDrmKeysRestored(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        MediaPeriodImpl mediaPeriod = getMediaPeriodForEvent(mediaPeriodId, null, false);
        if (mediaPeriod == null) {
            this.drmEventDispatcherWithoutId.drmKeysRestored();
        } else {
            mediaPeriod.drmEventDispatcher.drmKeysRestored();
        }
    }

    @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
    public void onDrmKeysRemoved(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        MediaPeriodImpl mediaPeriod = getMediaPeriodForEvent(mediaPeriodId, null, false);
        if (mediaPeriod == null) {
            this.drmEventDispatcherWithoutId.drmKeysRemoved();
        } else {
            mediaPeriod.drmEventDispatcher.drmKeysRemoved();
        }
    }

    @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
    public void onDrmSessionReleased(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        MediaPeriodImpl mediaPeriod = getMediaPeriodForEvent(mediaPeriodId, null, false);
        if (mediaPeriod == null) {
            this.drmEventDispatcherWithoutId.drmSessionReleased();
        } else {
            mediaPeriod.drmEventDispatcher.drmSessionReleased();
        }
    }

    @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
    public void onLoadStarted(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
        MediaPeriodImpl mediaPeriod = getMediaPeriodForEvent(mediaPeriodId, mediaLoadData, true);
        if (mediaPeriod == null) {
            this.mediaSourceEventDispatcherWithoutId.loadStarted(loadEventInfo, mediaLoadData);
        } else {
            mediaPeriod.sharedPeriod.onLoadStarted(loadEventInfo, mediaLoadData);
            mediaPeriod.mediaSourceEventDispatcher.loadStarted(loadEventInfo, correctMediaLoadData(mediaPeriod, mediaLoadData, (AdPlaybackState) Assertions.checkNotNull(this.adPlaybackStates.get(mediaPeriod.mediaPeriodId.periodUid))));
        }
    }

    @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
    public void onLoadCompleted(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
        MediaPeriodImpl mediaPeriod = getMediaPeriodForEvent(mediaPeriodId, mediaLoadData, true);
        if (mediaPeriod == null) {
            this.mediaSourceEventDispatcherWithoutId.loadCompleted(loadEventInfo, mediaLoadData);
        } else {
            mediaPeriod.sharedPeriod.onLoadFinished(loadEventInfo);
            mediaPeriod.mediaSourceEventDispatcher.loadCompleted(loadEventInfo, correctMediaLoadData(mediaPeriod, mediaLoadData, (AdPlaybackState) Assertions.checkNotNull(this.adPlaybackStates.get(mediaPeriod.mediaPeriodId.periodUid))));
        }
    }

    @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
    public void onLoadCanceled(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
        MediaPeriodImpl mediaPeriod = getMediaPeriodForEvent(mediaPeriodId, mediaLoadData, true);
        if (mediaPeriod == null) {
            this.mediaSourceEventDispatcherWithoutId.loadCanceled(loadEventInfo, mediaLoadData);
        } else {
            mediaPeriod.sharedPeriod.onLoadFinished(loadEventInfo);
            mediaPeriod.mediaSourceEventDispatcher.loadCanceled(loadEventInfo, correctMediaLoadData(mediaPeriod, mediaLoadData, (AdPlaybackState) Assertions.checkNotNull(this.adPlaybackStates.get(mediaPeriod.mediaPeriodId.periodUid))));
        }
    }

    @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
    public void onLoadError(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
        MediaPeriodImpl mediaPeriod = getMediaPeriodForEvent(mediaPeriodId, mediaLoadData, true);
        if (mediaPeriod == null) {
            this.mediaSourceEventDispatcherWithoutId.loadError(loadEventInfo, mediaLoadData, error, wasCanceled);
            return;
        }
        if (wasCanceled) {
            mediaPeriod.sharedPeriod.onLoadFinished(loadEventInfo);
        }
        mediaPeriod.mediaSourceEventDispatcher.loadError(loadEventInfo, correctMediaLoadData(mediaPeriod, mediaLoadData, (AdPlaybackState) Assertions.checkNotNull(this.adPlaybackStates.get(mediaPeriod.mediaPeriodId.periodUid))), error, wasCanceled);
    }

    @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
    public void onUpstreamDiscarded(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {
        MediaPeriodImpl mediaPeriod = getMediaPeriodForEvent(mediaPeriodId, mediaLoadData, false);
        if (mediaPeriod == null) {
            this.mediaSourceEventDispatcherWithoutId.upstreamDiscarded(mediaLoadData);
        } else {
            mediaPeriod.mediaSourceEventDispatcher.upstreamDiscarded(correctMediaLoadData(mediaPeriod, mediaLoadData, (AdPlaybackState) Assertions.checkNotNull(this.adPlaybackStates.get(mediaPeriod.mediaPeriodId.periodUid))));
        }
    }

    @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
    public void onDownstreamFormatChanged(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {
        MediaPeriodImpl mediaPeriod = getMediaPeriodForEvent(mediaPeriodId, mediaLoadData, false);
        if (mediaPeriod == null) {
            this.mediaSourceEventDispatcherWithoutId.downstreamFormatChanged(mediaLoadData);
        } else {
            mediaPeriod.sharedPeriod.onDownstreamFormatChanged(mediaPeriod, mediaLoadData);
            mediaPeriod.mediaSourceEventDispatcher.downstreamFormatChanged(correctMediaLoadData(mediaPeriod, mediaLoadData, (AdPlaybackState) Assertions.checkNotNull(this.adPlaybackStates.get(mediaPeriod.mediaPeriodId.periodUid))));
        }
    }

    private void releaseLastUsedMediaPeriod() {
        if (this.lastUsedMediaPeriod != null) {
            this.lastUsedMediaPeriod.release(this.mediaSource);
            this.lastUsedMediaPeriod = null;
        }
    }

    private MediaPeriodImpl getMediaPeriodForEvent(MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData, boolean useLoadingPeriod) {
        if (mediaPeriodId == null) {
            return null;
        }
        List<SharedMediaPeriod> periods = this.mediaPeriods.get(new Pair<>(Long.valueOf(mediaPeriodId.windowSequenceNumber), mediaPeriodId.periodUid));
        if (periods.isEmpty()) {
            return null;
        }
        if (useLoadingPeriod) {
            SharedMediaPeriod loadingPeriod = (SharedMediaPeriod) Iterables.getLast(periods);
            return loadingPeriod.loadingPeriod != null ? loadingPeriod.loadingPeriod : (MediaPeriodImpl) Iterables.getLast(loadingPeriod.mediaPeriods);
        }
        for (int i = 0; i < periods.size(); i++) {
            MediaPeriodImpl period = periods.get(i).getMediaPeriodForEvent(mediaLoadData);
            if (period != null) {
                return period;
            }
        }
        return (MediaPeriodImpl) periods.get(0).mediaPeriods.get(0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static long getMediaPeriodEndPositionUs(MediaPeriodImpl mediaPeriod, AdPlaybackState adPlaybackState) {
        MediaSource.MediaPeriodId id = mediaPeriod.mediaPeriodId;
        if (id.isAd()) {
            AdPlaybackState.AdGroup adGroup = adPlaybackState.getAdGroup(id.adGroupIndex);
            if (adGroup.count == -1) {
                return 0L;
            }
            return adGroup.durationsUs[id.adIndexInAdGroup];
        }
        if (id.nextAdGroupIndex == -1) {
            return Long.MAX_VALUE;
        }
        AdPlaybackState.AdGroup nextAdGroup = adPlaybackState.getAdGroup(id.nextAdGroupIndex);
        if (nextAdGroup.timeUs == Long.MIN_VALUE) {
            return Long.MAX_VALUE;
        }
        return nextAdGroup.timeUs;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static MediaLoadData correctMediaLoadData(MediaPeriodImpl mediaPeriod, MediaLoadData mediaLoadData, AdPlaybackState adPlaybackState) {
        return new MediaLoadData(mediaLoadData.dataType, mediaLoadData.trackType, mediaLoadData.trackFormat, mediaLoadData.trackSelectionReason, mediaLoadData.trackSelectionData, correctMediaLoadDataPositionMs(mediaLoadData.mediaStartTimeMs, mediaPeriod, adPlaybackState), correctMediaLoadDataPositionMs(mediaLoadData.mediaEndTimeMs, mediaPeriod, adPlaybackState));
    }

    private static long correctMediaLoadDataPositionMs(long mediaPositionMs, MediaPeriodImpl mediaPeriod, AdPlaybackState adPlaybackState) {
        long correctedPositionUs;
        if (mediaPositionMs == C.TIME_UNSET) {
            return C.TIME_UNSET;
        }
        long mediaPositionUs = Util.msToUs(mediaPositionMs);
        MediaSource.MediaPeriodId id = mediaPeriod.mediaPeriodId;
        if (id.isAd()) {
            correctedPositionUs = ServerSideAdInsertionUtil.getMediaPeriodPositionUsForAd(mediaPositionUs, id.adGroupIndex, id.adIndexInAdGroup, adPlaybackState);
        } else {
            correctedPositionUs = ServerSideAdInsertionUtil.getMediaPeriodPositionUsForContent(mediaPositionUs, -1, adPlaybackState);
        }
        return Util.usToMs(correctedPositionUs);
    }

    private static final class SharedMediaPeriod implements MediaPeriod.Callback {
        private final MediaPeriod actualMediaPeriod;
        private AdPlaybackState adPlaybackState;
        private boolean hasStartedPreparing;
        private boolean isPrepared;
        private MediaPeriodImpl loadingPeriod;
        private final Object periodUid;
        private final List<MediaPeriodImpl> mediaPeriods = new ArrayList();
        private final Map<Long, Pair<LoadEventInfo, MediaLoadData>> activeLoads = new HashMap();
        public ExoTrackSelection[] trackSelections = new ExoTrackSelection[0];
        public SampleStream[] sampleStreams = new SampleStream[0];
        public MediaLoadData[] lastDownstreamFormatChangeData = new MediaLoadData[0];

        public SharedMediaPeriod(MediaPeriod actualMediaPeriod, Object periodUid, AdPlaybackState adPlaybackState) {
            this.actualMediaPeriod = actualMediaPeriod;
            this.periodUid = periodUid;
            this.adPlaybackState = adPlaybackState;
        }

        public void updateAdPlaybackState(AdPlaybackState adPlaybackState) {
            this.adPlaybackState = adPlaybackState;
        }

        public void add(MediaPeriodImpl mediaPeriod) {
            this.mediaPeriods.add(mediaPeriod);
        }

        public void remove(MediaPeriodImpl mediaPeriod) {
            if (mediaPeriod.equals(this.loadingPeriod)) {
                this.loadingPeriod = null;
                this.activeLoads.clear();
            }
            this.mediaPeriods.remove(mediaPeriod);
        }

        public boolean isUnused() {
            return this.mediaPeriods.isEmpty();
        }

        public void release(MediaSource mediaSource) {
            mediaSource.releasePeriod(this.actualMediaPeriod);
        }

        public boolean canReuseMediaPeriod(MediaSource.MediaPeriodId id, long positionUs) {
            MediaPeriodImpl previousPeriod = (MediaPeriodImpl) Iterables.getLast(this.mediaPeriods);
            long previousEndPositionUs = ServerSideAdInsertionUtil.getStreamPositionUs(ServerSideAdInsertionMediaSource.getMediaPeriodEndPositionUs(previousPeriod, this.adPlaybackState), previousPeriod.mediaPeriodId, this.adPlaybackState);
            long startPositionUs = ServerSideAdInsertionUtil.getStreamPositionUs(positionUs, id, this.adPlaybackState);
            return startPositionUs == previousEndPositionUs;
        }

        public MediaPeriodImpl getMediaPeriodForEvent(MediaLoadData mediaLoadData) {
            if (mediaLoadData != null && mediaLoadData.mediaStartTimeMs != C.TIME_UNSET) {
                for (int i = 0; i < this.mediaPeriods.size(); i++) {
                    MediaPeriodImpl mediaPeriod = this.mediaPeriods.get(i);
                    if (mediaPeriod.isPrepared) {
                        long startTimeInPeriodUs = ServerSideAdInsertionUtil.getMediaPeriodPositionUs(Util.msToUs(mediaLoadData.mediaStartTimeMs), mediaPeriod.mediaPeriodId, this.adPlaybackState);
                        long mediaPeriodEndPositionUs = ServerSideAdInsertionMediaSource.getMediaPeriodEndPositionUs(mediaPeriod, this.adPlaybackState);
                        if (startTimeInPeriodUs >= 0 && startTimeInPeriodUs < mediaPeriodEndPositionUs) {
                            return mediaPeriod;
                        }
                    }
                }
                return null;
            }
            return null;
        }

        public void prepare(MediaPeriodImpl mediaPeriod, long positionUs) {
            mediaPeriod.lastStartPositionUs = positionUs;
            if (this.hasStartedPreparing) {
                if (this.isPrepared) {
                    mediaPeriod.onPrepared();
                }
            } else {
                this.hasStartedPreparing = true;
                long preparePositionUs = ServerSideAdInsertionUtil.getStreamPositionUs(positionUs, mediaPeriod.mediaPeriodId, this.adPlaybackState);
                this.actualMediaPeriod.prepare(this, preparePositionUs);
            }
        }

        public void maybeThrowPrepareError() throws IOException {
            this.actualMediaPeriod.maybeThrowPrepareError();
        }

        public TrackGroupArray getTrackGroups() {
            return this.actualMediaPeriod.getTrackGroups();
        }

        public List<StreamKey> getStreamKeys(List<ExoTrackSelection> trackSelections) {
            return this.actualMediaPeriod.getStreamKeys(trackSelections);
        }

        public boolean continueLoading(MediaPeriodImpl mediaPeriod, LoadingInfo loadingInfo) {
            MediaPeriodImpl loadingPeriod = this.loadingPeriod;
            if (loadingPeriod != null && !mediaPeriod.equals(loadingPeriod)) {
                for (Pair<LoadEventInfo, MediaLoadData> loadData : this.activeLoads.values()) {
                    loadingPeriod.mediaSourceEventDispatcher.loadCompleted((LoadEventInfo) loadData.first, ServerSideAdInsertionMediaSource.correctMediaLoadData(loadingPeriod, (MediaLoadData) loadData.second, this.adPlaybackState));
                    mediaPeriod.mediaSourceEventDispatcher.loadStarted((LoadEventInfo) loadData.first, ServerSideAdInsertionMediaSource.correctMediaLoadData(mediaPeriod, (MediaLoadData) loadData.second, this.adPlaybackState));
                }
            }
            this.loadingPeriod = mediaPeriod;
            long actualPlaybackPositionUs = getStreamPositionUsWithNotYetStartedHandling(mediaPeriod, loadingInfo.playbackPositionUs);
            return this.actualMediaPeriod.continueLoading(loadingInfo.buildUpon().setPlaybackPositionUs(actualPlaybackPositionUs).build());
        }

        public boolean isLoading(MediaPeriodImpl mediaPeriod) {
            return mediaPeriod.equals(this.loadingPeriod) && this.actualMediaPeriod.isLoading();
        }

        public long getBufferedPositionUs(MediaPeriodImpl mediaPeriod) {
            return getMediaPeriodPositionUsWithEndOfSourceHandling(mediaPeriod, this.actualMediaPeriod.getBufferedPositionUs());
        }

        public long getNextLoadPositionUs(MediaPeriodImpl mediaPeriod) {
            return getMediaPeriodPositionUsWithEndOfSourceHandling(mediaPeriod, this.actualMediaPeriod.getNextLoadPositionUs());
        }

        public long seekToUs(MediaPeriodImpl mediaPeriod, long positionUs) {
            long actualRequestedPositionUs = ServerSideAdInsertionUtil.getStreamPositionUs(positionUs, mediaPeriod.mediaPeriodId, this.adPlaybackState);
            long newActualPositionUs = this.actualMediaPeriod.seekToUs(actualRequestedPositionUs);
            return ServerSideAdInsertionUtil.getMediaPeriodPositionUs(newActualPositionUs, mediaPeriod.mediaPeriodId, this.adPlaybackState);
        }

        public long getAdjustedSeekPositionUs(MediaPeriodImpl mediaPeriod, long positionUs, SeekParameters seekParameters) {
            long actualRequestedPositionUs = ServerSideAdInsertionUtil.getStreamPositionUs(positionUs, mediaPeriod.mediaPeriodId, this.adPlaybackState);
            long adjustedActualPositionUs = this.actualMediaPeriod.getAdjustedSeekPositionUs(actualRequestedPositionUs, seekParameters);
            return ServerSideAdInsertionUtil.getMediaPeriodPositionUs(adjustedActualPositionUs, mediaPeriod.mediaPeriodId, this.adPlaybackState);
        }

        public void discardBuffer(MediaPeriodImpl mediaPeriod, long positionUs, boolean toKeyframe) {
            long actualPositionUs = ServerSideAdInsertionUtil.getStreamPositionUs(positionUs, mediaPeriod.mediaPeriodId, this.adPlaybackState);
            this.actualMediaPeriod.discardBuffer(actualPositionUs, toKeyframe);
        }

        public void reevaluateBuffer(MediaPeriodImpl mediaPeriod, long positionUs) {
            this.actualMediaPeriod.reevaluateBuffer(getStreamPositionUsWithNotYetStartedHandling(mediaPeriod, positionUs));
        }

        public long readDiscontinuity(MediaPeriodImpl mediaPeriod) {
            if (!mediaPeriod.equals(this.mediaPeriods.get(0))) {
                return C.TIME_UNSET;
            }
            long actualDiscontinuityPositionUs = this.actualMediaPeriod.readDiscontinuity();
            return actualDiscontinuityPositionUs == C.TIME_UNSET ? C.TIME_UNSET : ServerSideAdInsertionUtil.getMediaPeriodPositionUs(actualDiscontinuityPositionUs, mediaPeriod.mediaPeriodId, this.adPlaybackState);
        }

        public long selectTracks(MediaPeriodImpl mediaPeriod, ExoTrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
            SampleStream emptySampleStream;
            SampleStream[] realStreams;
            mediaPeriod.lastStartPositionUs = positionUs;
            if (mediaPeriod.equals(this.mediaPeriods.get(0))) {
                this.trackSelections = (ExoTrackSelection[]) Arrays.copyOf(selections, selections.length);
                long requestedPositionUs = ServerSideAdInsertionUtil.getStreamPositionUs(positionUs, mediaPeriod.mediaPeriodId, this.adPlaybackState);
                if (this.sampleStreams.length != 0) {
                    realStreams = (SampleStream[]) Arrays.copyOf(this.sampleStreams, this.sampleStreams.length);
                } else {
                    realStreams = new SampleStream[selections.length];
                }
                long startPositionUs = this.actualMediaPeriod.selectTracks(selections, mayRetainStreamFlags, realStreams, streamResetFlags, requestedPositionUs);
                this.sampleStreams = (SampleStream[]) Arrays.copyOf(realStreams, realStreams.length);
                this.lastDownstreamFormatChangeData = (MediaLoadData[]) Arrays.copyOf(this.lastDownstreamFormatChangeData, realStreams.length);
                for (int i = 0; i < realStreams.length; i++) {
                    if (realStreams[i] == null) {
                        streams[i] = null;
                        this.lastDownstreamFormatChangeData[i] = null;
                    } else if (streams[i] == null || streamResetFlags[i]) {
                        streams[i] = new SampleStreamImpl(mediaPeriod, i);
                        this.lastDownstreamFormatChangeData[i] = null;
                    }
                }
                return ServerSideAdInsertionUtil.getMediaPeriodPositionUs(startPositionUs, mediaPeriod.mediaPeriodId, this.adPlaybackState);
            }
            for (int i2 = 0; i2 < selections.length; i2++) {
                boolean z = true;
                if (selections[i2] != null) {
                    if (mayRetainStreamFlags[i2] && streams[i2] != null) {
                        z = false;
                    }
                    streamResetFlags[i2] = z;
                    if (streamResetFlags[i2]) {
                        if (Util.areEqual(this.trackSelections[i2], selections[i2])) {
                            emptySampleStream = new SampleStreamImpl(mediaPeriod, i2);
                        } else {
                            emptySampleStream = new EmptySampleStream();
                        }
                        streams[i2] = emptySampleStream;
                    }
                } else {
                    streams[i2] = null;
                    streamResetFlags[i2] = true;
                }
            }
            return positionUs;
        }

        public int readData(MediaPeriodImpl mediaPeriod, int streamIndex, FormatHolder formatHolder, DecoderInputBuffer buffer, int readFlags) {
            int peekingFlags = readFlags | 1 | 4;
            long bufferedPositionUs = getBufferedPositionUs(mediaPeriod);
            int result = ((SampleStream) Util.castNonNull(this.sampleStreams[streamIndex])).readData(formatHolder, buffer, peekingFlags);
            long adjustedTimeUs = getMediaPeriodPositionUsWithEndOfSourceHandling(mediaPeriod, buffer.timeUs);
            if ((result == -4 && adjustedTimeUs == Long.MIN_VALUE) || (result == -3 && bufferedPositionUs == Long.MIN_VALUE && !buffer.waitingForKeys)) {
                maybeNotifyDownstreamFormatChanged(mediaPeriod, streamIndex);
                buffer.clear();
                buffer.addFlag(4);
                return -4;
            }
            if (result == -4) {
                maybeNotifyDownstreamFormatChanged(mediaPeriod, streamIndex);
                ((SampleStream) Util.castNonNull(this.sampleStreams[streamIndex])).readData(formatHolder, buffer, readFlags);
                buffer.timeUs = adjustedTimeUs;
            }
            return result;
        }

        public int skipData(MediaPeriodImpl mediaPeriod, int streamIndex, long positionUs) {
            long actualPositionUs = ServerSideAdInsertionUtil.getStreamPositionUs(positionUs, mediaPeriod.mediaPeriodId, this.adPlaybackState);
            return ((SampleStream) Util.castNonNull(this.sampleStreams[streamIndex])).skipData(actualPositionUs);
        }

        public boolean isReady(int streamIndex) {
            return ((SampleStream) Util.castNonNull(this.sampleStreams[streamIndex])).isReady();
        }

        public void maybeThrowError(int streamIndex) throws IOException {
            ((SampleStream) Util.castNonNull(this.sampleStreams[streamIndex])).maybeThrowError();
        }

        public void onDownstreamFormatChanged(MediaPeriodImpl mediaPeriod, MediaLoadData mediaLoadData) {
            int streamIndex = findMatchingStreamIndex(mediaLoadData);
            if (streamIndex != -1) {
                this.lastDownstreamFormatChangeData[streamIndex] = mediaLoadData;
                mediaPeriod.hasNotifiedDownstreamFormatChange[streamIndex] = true;
            }
        }

        public void onLoadStarted(LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
            this.activeLoads.put(Long.valueOf(loadEventInfo.loadTaskId), Pair.create(loadEventInfo, mediaLoadData));
        }

        public void onLoadFinished(LoadEventInfo loadEventInfo) {
            this.activeLoads.remove(Long.valueOf(loadEventInfo.loadTaskId));
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod.Callback
        public void onPrepared(MediaPeriod actualMediaPeriod) {
            this.isPrepared = true;
            for (int i = 0; i < this.mediaPeriods.size(); i++) {
                this.mediaPeriods.get(i).onPrepared();
            }
        }

        @Override // androidx.media3.exoplayer.source.SequenceableLoader.Callback
        public void onContinueLoadingRequested(MediaPeriod source) {
            if (this.loadingPeriod == null) {
                return;
            }
            ((MediaPeriod.Callback) Assertions.checkNotNull(this.loadingPeriod.callback)).onContinueLoadingRequested(this.loadingPeriod);
        }

        private long getStreamPositionUsWithNotYetStartedHandling(MediaPeriodImpl mediaPeriod, long positionUs) {
            if (positionUs < mediaPeriod.lastStartPositionUs) {
                long actualStartPositionUs = ServerSideAdInsertionUtil.getStreamPositionUs(mediaPeriod.lastStartPositionUs, mediaPeriod.mediaPeriodId, this.adPlaybackState);
                return actualStartPositionUs - (mediaPeriod.lastStartPositionUs - positionUs);
            }
            return ServerSideAdInsertionUtil.getStreamPositionUs(positionUs, mediaPeriod.mediaPeriodId, this.adPlaybackState);
        }

        private long getMediaPeriodPositionUsWithEndOfSourceHandling(MediaPeriodImpl mediaPeriod, long positionUs) {
            if (positionUs == Long.MIN_VALUE) {
                return Long.MIN_VALUE;
            }
            long mediaPeriodPositionUs = ServerSideAdInsertionUtil.getMediaPeriodPositionUs(positionUs, mediaPeriod.mediaPeriodId, this.adPlaybackState);
            long endPositionUs = ServerSideAdInsertionMediaSource.getMediaPeriodEndPositionUs(mediaPeriod, this.adPlaybackState);
            if (mediaPeriodPositionUs >= endPositionUs) {
                return Long.MIN_VALUE;
            }
            return mediaPeriodPositionUs;
        }

        private int findMatchingStreamIndex(MediaLoadData mediaLoadData) {
            if (mediaLoadData.trackFormat == null) {
                return -1;
            }
            for (int i = 0; i < this.trackSelections.length; i++) {
                if (this.trackSelections[i] != null) {
                    TrackGroup trackGroup = this.trackSelections[i].getTrackGroup();
                    boolean isPrimaryTrackGroup = false;
                    if (mediaLoadData.trackType == 0 && trackGroup.equals(getTrackGroups().get(0))) {
                        isPrimaryTrackGroup = true;
                    }
                    for (int j = 0; j < trackGroup.length; j++) {
                        Format format = trackGroup.getFormat(j);
                        if (format.equals(mediaLoadData.trackFormat) || (isPrimaryTrackGroup && format.id != null && format.id.equals(mediaLoadData.trackFormat.id))) {
                            return i;
                        }
                    }
                }
            }
            return -1;
        }

        private void maybeNotifyDownstreamFormatChanged(MediaPeriodImpl mediaPeriod, int streamIndex) {
            if (!mediaPeriod.hasNotifiedDownstreamFormatChange[streamIndex] && this.lastDownstreamFormatChangeData[streamIndex] != null) {
                mediaPeriod.hasNotifiedDownstreamFormatChange[streamIndex] = true;
                mediaPeriod.mediaSourceEventDispatcher.downstreamFormatChanged(ServerSideAdInsertionMediaSource.correctMediaLoadData(mediaPeriod, this.lastDownstreamFormatChangeData[streamIndex], this.adPlaybackState));
            }
        }
    }

    private static final class ServerSideAdInsertionTimeline extends ForwardingTimeline {
        private final ImmutableMap<Object, AdPlaybackState> adPlaybackStates;

        public ServerSideAdInsertionTimeline(Timeline contentTimeline, ImmutableMap<Object, AdPlaybackState> adPlaybackStates) {
            super(contentTimeline);
            Assertions.checkState(contentTimeline.getWindowCount() == 1);
            Timeline.Period period = new Timeline.Period();
            for (int i = 0; i < contentTimeline.getPeriodCount(); i++) {
                contentTimeline.getPeriod(i, period, true);
                Assertions.checkState(adPlaybackStates.containsKey(Assertions.checkNotNull(period.uid)));
            }
            this.adPlaybackStates = adPlaybackStates;
        }

        @Override // androidx.media3.exoplayer.source.ForwardingTimeline, androidx.media3.common.Timeline
        public Timeline.Window getWindow(int windowIndex, Timeline.Window window, long defaultPositionProjectionUs) {
            super.getWindow(windowIndex, window, defaultPositionProjectionUs);
            Timeline.Period period = new Timeline.Period();
            Object firstPeriodUid = Assertions.checkNotNull(getPeriod(window.firstPeriodIndex, period, true).uid);
            AdPlaybackState firstAdPlaybackState = (AdPlaybackState) Assertions.checkNotNull(this.adPlaybackStates.get(firstPeriodUid));
            long positionInPeriodUs = ServerSideAdInsertionUtil.getMediaPeriodPositionUsForContent(window.positionInFirstPeriodUs, -1, firstAdPlaybackState);
            if (window.durationUs != C.TIME_UNSET) {
                Timeline.Period originalLastPeriod = super.getPeriod(window.lastPeriodIndex, period, true);
                long originalLastPeriodPositionInWindowUs = originalLastPeriod.positionInWindowUs;
                AdPlaybackState lastAdPlaybackState = (AdPlaybackState) Assertions.checkNotNull(this.adPlaybackStates.get(originalLastPeriod.uid));
                Timeline.Period adjustedLastPeriod = getPeriod(window.lastPeriodIndex, period);
                long originalWindowDurationInLastPeriodUs = window.durationUs - originalLastPeriodPositionInWindowUs;
                long adjustedWindowDurationInLastPeriodUs = ServerSideAdInsertionUtil.getMediaPeriodPositionUsForContent(originalWindowDurationInLastPeriodUs, -1, lastAdPlaybackState);
                window.durationUs = adjustedLastPeriod.positionInWindowUs + adjustedWindowDurationInLastPeriodUs;
            } else if (firstAdPlaybackState.contentDurationUs != C.TIME_UNSET) {
                window.durationUs = firstAdPlaybackState.contentDurationUs - positionInPeriodUs;
            }
            window.positionInFirstPeriodUs = positionInPeriodUs;
            return window;
        }

        @Override // androidx.media3.exoplayer.source.ForwardingTimeline, androidx.media3.common.Timeline
        public Timeline.Period getPeriod(int periodIndex, Timeline.Period period, boolean setIds) {
            long durationUs;
            super.getPeriod(periodIndex, period, true);
            AdPlaybackState adPlaybackState = (AdPlaybackState) Assertions.checkNotNull(this.adPlaybackStates.get(period.uid));
            long durationUs2 = period.durationUs;
            if (durationUs2 == C.TIME_UNSET) {
                durationUs = adPlaybackState.contentDurationUs;
            } else {
                durationUs = ServerSideAdInsertionUtil.getMediaPeriodPositionUsForContent(durationUs2, -1, adPlaybackState);
            }
            long positionInWindowUs = 0;
            Timeline.Period innerPeriod = new Timeline.Period();
            for (int i = 0; i < periodIndex + 1; i++) {
                this.timeline.getPeriod(i, innerPeriod, true);
                AdPlaybackState innerAdPlaybackState = (AdPlaybackState) Assertions.checkNotNull(this.adPlaybackStates.get(innerPeriod.uid));
                if (i == 0) {
                    positionInWindowUs = -ServerSideAdInsertionUtil.getMediaPeriodPositionUsForContent(-innerPeriod.getPositionInWindowUs(), -1, innerAdPlaybackState);
                }
                if (i != periodIndex) {
                    positionInWindowUs += ServerSideAdInsertionUtil.getMediaPeriodPositionUsForContent(innerPeriod.durationUs, -1, innerAdPlaybackState);
                }
            }
            period.set(period.id, period.uid, period.windowIndex, durationUs, positionInWindowUs, adPlaybackState, period.isPlaceholder);
            return period;
        }
    }

    private static final class MediaPeriodImpl implements MediaPeriod {
        public MediaPeriod.Callback callback;
        public final DrmSessionEventListener.EventDispatcher drmEventDispatcher;
        public boolean[] hasNotifiedDownstreamFormatChange = new boolean[0];
        public boolean isPrepared;
        public long lastStartPositionUs;
        public final MediaSource.MediaPeriodId mediaPeriodId;
        public final MediaSourceEventListener.EventDispatcher mediaSourceEventDispatcher;
        public final SharedMediaPeriod sharedPeriod;

        public MediaPeriodImpl(SharedMediaPeriod sharedPeriod, MediaSource.MediaPeriodId mediaPeriodId, MediaSourceEventListener.EventDispatcher mediaSourceEventDispatcher, DrmSessionEventListener.EventDispatcher drmEventDispatcher) {
            this.sharedPeriod = sharedPeriod;
            this.mediaPeriodId = mediaPeriodId;
            this.mediaSourceEventDispatcher = mediaSourceEventDispatcher;
            this.drmEventDispatcher = drmEventDispatcher;
        }

        public void onPrepared() {
            if (this.callback != null) {
                this.callback.onPrepared(this);
            }
            this.isPrepared = true;
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public void prepare(MediaPeriod.Callback callback, long positionUs) {
            this.callback = callback;
            this.sharedPeriod.prepare(this, positionUs);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public void maybeThrowPrepareError() throws IOException {
            this.sharedPeriod.maybeThrowPrepareError();
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public TrackGroupArray getTrackGroups() {
            return this.sharedPeriod.getTrackGroups();
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public List<StreamKey> getStreamKeys(List<ExoTrackSelection> trackSelections) {
            return this.sharedPeriod.getStreamKeys(trackSelections);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public long selectTracks(ExoTrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
            if (this.hasNotifiedDownstreamFormatChange.length == 0) {
                this.hasNotifiedDownstreamFormatChange = new boolean[streams.length];
            }
            return this.sharedPeriod.selectTracks(this, selections, mayRetainStreamFlags, streams, streamResetFlags, positionUs);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public void discardBuffer(long positionUs, boolean toKeyframe) {
            this.sharedPeriod.discardBuffer(this, positionUs, toKeyframe);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public long readDiscontinuity() {
            return this.sharedPeriod.readDiscontinuity(this);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public long seekToUs(long positionUs) {
            return this.sharedPeriod.seekToUs(this, positionUs);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
            return this.sharedPeriod.getAdjustedSeekPositionUs(this, positionUs, seekParameters);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
        public long getBufferedPositionUs() {
            return this.sharedPeriod.getBufferedPositionUs(this);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
        public long getNextLoadPositionUs() {
            return this.sharedPeriod.getNextLoadPositionUs(this);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
        public boolean continueLoading(LoadingInfo loadingInfo) {
            return this.sharedPeriod.continueLoading(this, loadingInfo);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
        public boolean isLoading() {
            return this.sharedPeriod.isLoading(this);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
        public void reevaluateBuffer(long positionUs) {
            this.sharedPeriod.reevaluateBuffer(this, positionUs);
        }
    }

    private static final class SampleStreamImpl implements SampleStream {
        private final MediaPeriodImpl mediaPeriod;
        private final int streamIndex;

        public SampleStreamImpl(MediaPeriodImpl mediaPeriod, int streamIndex) {
            this.mediaPeriod = mediaPeriod;
            this.streamIndex = streamIndex;
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public boolean isReady() {
            return this.mediaPeriod.sharedPeriod.isReady(this.streamIndex);
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public void maybeThrowError() throws IOException {
            this.mediaPeriod.sharedPeriod.maybeThrowError(this.streamIndex);
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, int readFlags) {
            return this.mediaPeriod.sharedPeriod.readData(this.mediaPeriod, this.streamIndex, formatHolder, buffer, readFlags);
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public int skipData(long positionUs) {
            return this.mediaPeriod.sharedPeriod.skipData(this.mediaPeriod, this.streamIndex, positionUs);
        }
    }
}
