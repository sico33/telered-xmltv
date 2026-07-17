package androidx.media3.exoplayer.source.preload;

import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.LoadingInfo;
import androidx.media3.exoplayer.RendererCapabilities;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider;
import androidx.media3.exoplayer.source.MediaPeriod;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.source.WrappingMediaSource;
import androidx.media3.exoplayer.trackselection.TrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelectorResult;
import androidx.media3.exoplayer.upstream.Allocator;
import androidx.media3.exoplayer.upstream.BandwidthMeter;
import androidx.media3.exoplayer.upstream.CmcdConfiguration;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import androidx.media3.extractor.text.SubtitleParser;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class PreloadMediaSource extends WrappingMediaSource {
    private static final String TAG = "PreloadMediaSource";
    private final Allocator allocator;
    private final BandwidthMeter bandwidthMeter;
    private boolean onSourcePreparedNotified;
    private boolean onUsedByPlayerNotified;
    private Pair<PreloadMediaPeriod, MediaSource.MediaPeriodId> playingPreloadedMediaPeriodAndId;
    private boolean preloadCalled;
    private final PreloadControl preloadControl;
    private final Handler preloadHandler;
    private Pair<PreloadMediaPeriod, MediaPeriodKey> preloadingMediaPeriodAndKey;
    private boolean prepareChildSourceCalled;
    private final RendererCapabilities[] rendererCapabilities;
    private long startPositionUs;
    private Timeline timeline;
    private final TrackSelector trackSelector;

    public interface PreloadControl {
        boolean onContinueLoadingRequested(PreloadMediaSource preloadMediaSource, long j);

        void onLoadedToTheEndOfSource(PreloadMediaSource preloadMediaSource);

        boolean onSourcePrepared(PreloadMediaSource preloadMediaSource);

        boolean onTracksSelected(PreloadMediaSource preloadMediaSource);

        void onUsedByPlayer(PreloadMediaSource preloadMediaSource);

        /* JADX INFO: renamed from: androidx.media3.exoplayer.source.preload.PreloadMediaSource$PreloadControl$-CC, reason: invalid class name */
        public final /* synthetic */ class CC {
            public static void $default$onLoadedToTheEndOfSource(PreloadControl _this, PreloadMediaSource mediaSource) {
            }
        }
    }

    public static final class Factory implements MediaSource.Factory {
        private final Allocator allocator;
        private final BandwidthMeter bandwidthMeter;
        private final MediaSource.Factory mediaSourceFactory;
        private final PreloadControl preloadControl;
        private final Looper preloadLooper;
        private final RendererCapabilities[] rendererCapabilities;
        private final TrackSelector trackSelector;

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public /* synthetic */ MediaSource.Factory experimentalParseSubtitlesDuringExtraction(boolean z) {
            return MediaSource.Factory.CC.$default$experimentalParseSubtitlesDuringExtraction(this, z);
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public /* synthetic */ MediaSource.Factory setSubtitleParserFactory(SubtitleParser.Factory factory) {
            return MediaSource.Factory.CC.$default$setSubtitleParserFactory(this, factory);
        }

        public Factory(MediaSource.Factory mediaSourceFactory, PreloadControl preloadControl, TrackSelector trackSelector, BandwidthMeter bandwidthMeter, RendererCapabilities[] rendererCapabilities, Allocator allocator, Looper preloadLooper) {
            this.mediaSourceFactory = mediaSourceFactory;
            this.preloadControl = preloadControl;
            this.trackSelector = trackSelector;
            this.bandwidthMeter = bandwidthMeter;
            this.rendererCapabilities = (RendererCapabilities[]) Arrays.copyOf(rendererCapabilities, rendererCapabilities.length);
            this.allocator = allocator;
            this.preloadLooper = preloadLooper;
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public Factory setCmcdConfigurationFactory(CmcdConfiguration.Factory cmcdConfigurationFactory) {
            this.mediaSourceFactory.setCmcdConfigurationFactory(cmcdConfigurationFactory);
            return this;
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public Factory setDrmSessionManagerProvider(DrmSessionManagerProvider drmSessionManagerProvider) {
            this.mediaSourceFactory.setDrmSessionManagerProvider(drmSessionManagerProvider);
            return this;
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public Factory setLoadErrorHandlingPolicy(LoadErrorHandlingPolicy loadErrorHandlingPolicy) {
            this.mediaSourceFactory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy);
            return this;
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public int[] getSupportedTypes() {
            return this.mediaSourceFactory.getSupportedTypes();
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public PreloadMediaSource createMediaSource(MediaItem mediaItem) {
            return new PreloadMediaSource(this.mediaSourceFactory.createMediaSource(mediaItem), this.preloadControl, this.trackSelector, this.bandwidthMeter, this.rendererCapabilities, this.allocator, this.preloadLooper);
        }

        public PreloadMediaSource createMediaSource(MediaSource mediaSource) {
            return new PreloadMediaSource(mediaSource, this.preloadControl, this.trackSelector, this.bandwidthMeter, this.rendererCapabilities, this.allocator, this.preloadLooper);
        }
    }

    private PreloadMediaSource(MediaSource mediaSource, PreloadControl preloadControl, TrackSelector trackSelector, BandwidthMeter bandwidthMeter, RendererCapabilities[] rendererCapabilities, Allocator allocator, Looper preloadLooper) {
        super(mediaSource);
        this.preloadControl = preloadControl;
        this.trackSelector = trackSelector;
        this.bandwidthMeter = bandwidthMeter;
        this.rendererCapabilities = rendererCapabilities;
        this.allocator = allocator;
        this.preloadHandler = Util.createHandler(preloadLooper, null);
        this.startPositionUs = C.TIME_UNSET;
    }

    public void preload(final long startPositionUs) {
        this.preloadHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.source.preload.PreloadMediaSource$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m131xf99e9a56(startPositionUs);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$preload$0$androidx-media3-exoplayer-source-preload-PreloadMediaSource, reason: not valid java name */
    /* synthetic */ void m131xf99e9a56(long startPositionUs) {
        this.preloadCalled = true;
        this.startPositionUs = startPositionUs;
        this.onSourcePreparedNotified = false;
        if (isUsedByPlayer()) {
            notifyOnUsedByPlayer();
        } else {
            setPlayerId(PlayerId.UNSET);
            prepareSourceInternal(this.bandwidthMeter.getTransferListener());
        }
    }

    public void clear() {
        this.preloadHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.source.preload.PreloadMediaSource$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m129xcf4780db();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$clear$1$androidx-media3-exoplayer-source-preload-PreloadMediaSource, reason: not valid java name */
    /* synthetic */ void m129xcf4780db() {
        if (this.preloadingMediaPeriodAndKey != null) {
            this.mediaSource.releasePeriod(((PreloadMediaPeriod) this.preloadingMediaPeriodAndKey.first).mediaPeriod);
            this.preloadingMediaPeriodAndKey = null;
        }
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource
    protected void prepareSourceInternal() {
        if (isUsedByPlayer() && !this.onUsedByPlayerNotified) {
            notifyOnUsedByPlayer();
        }
        if (this.timeline != null) {
            onChildSourceInfoRefreshed(this.timeline);
        } else if (!this.prepareChildSourceCalled) {
            this.prepareChildSourceCalled = true;
            prepareChildSource();
        }
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource
    protected void onChildSourceInfoRefreshed(final Timeline newTimeline) {
        this.timeline = newTimeline;
        refreshSourceInfo(newTimeline);
        this.preloadHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.source.preload.PreloadMediaSource$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m130xd674fafd(newTimeline);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$onChildSourceInfoRefreshed$2$androidx-media3-exoplayer-source-preload-PreloadMediaSource, reason: not valid java name */
    /* synthetic */ void m130xd674fafd(Timeline newTimeline) {
        if (!isUsedByPlayer() && !this.onSourcePreparedNotified) {
            this.onSourcePreparedNotified = true;
            if (!this.preloadControl.onSourcePrepared(this)) {
                return;
            }
            Pair<Object, Long> periodPosition = newTimeline.getPeriodPositionUs(new Timeline.Window(), new Timeline.Period(), 0, this.startPositionUs);
            MediaSource.MediaPeriodId mediaPeriodId = new MediaSource.MediaPeriodId(periodPosition.first);
            PreloadMediaPeriod mediaPeriod = createPeriod(mediaPeriodId, this.allocator, ((Long) periodPosition.second).longValue());
            mediaPeriod.preload(new PreloadMediaPeriodCallback(((Long) periodPosition.second).longValue()), ((Long) periodPosition.second).longValue());
        }
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource, androidx.media3.exoplayer.source.MediaSource
    public PreloadMediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator, long startPositionUs) {
        MediaPeriodKey key = new MediaPeriodKey(id, startPositionUs);
        if (this.preloadingMediaPeriodAndKey != null && key.equals(this.preloadingMediaPeriodAndKey.second)) {
            PreloadMediaPeriod mediaPeriod = (PreloadMediaPeriod) ((Pair) Assertions.checkNotNull(this.preloadingMediaPeriodAndKey)).first;
            if (isUsedByPlayer()) {
                this.preloadingMediaPeriodAndKey = null;
                this.playingPreloadedMediaPeriodAndId = new Pair<>(mediaPeriod, id);
            }
            return mediaPeriod;
        }
        if (this.preloadingMediaPeriodAndKey != null) {
            this.mediaSource.releasePeriod(((PreloadMediaPeriod) ((Pair) Assertions.checkNotNull(this.preloadingMediaPeriodAndKey)).first).mediaPeriod);
            this.preloadingMediaPeriodAndKey = null;
        }
        PreloadMediaPeriod mediaPeriod2 = new PreloadMediaPeriod(this.mediaSource.createPeriod(id, allocator, startPositionUs));
        if (!isUsedByPlayer()) {
            this.preloadingMediaPeriodAndKey = new Pair<>(mediaPeriod2, key);
        }
        return mediaPeriod2;
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource
    protected MediaSource.MediaPeriodId getMediaPeriodIdForChildMediaPeriodId(MediaSource.MediaPeriodId mediaPeriodId) {
        if (this.playingPreloadedMediaPeriodAndId != null && mediaPeriodIdEqualsWithoutWindowSequenceNumber(mediaPeriodId, (MediaSource.MediaPeriodId) ((Pair) Assertions.checkNotNull(this.playingPreloadedMediaPeriodAndId)).second)) {
            return (MediaSource.MediaPeriodId) ((Pair) Assertions.checkNotNull(this.playingPreloadedMediaPeriodAndId)).second;
        }
        return mediaPeriodId;
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource, androidx.media3.exoplayer.source.MediaSource
    public void releasePeriod(MediaPeriod mediaPeriod) {
        PreloadMediaPeriod preloadMediaPeriod = (PreloadMediaPeriod) mediaPeriod;
        if (this.preloadingMediaPeriodAndKey != null && preloadMediaPeriod == ((Pair) Assertions.checkNotNull(this.preloadingMediaPeriodAndKey)).first) {
            this.preloadingMediaPeriodAndKey = null;
        } else if (this.playingPreloadedMediaPeriodAndId != null && preloadMediaPeriod == ((Pair) Assertions.checkNotNull(this.playingPreloadedMediaPeriodAndId)).first) {
            this.playingPreloadedMediaPeriodAndId = null;
        }
        this.mediaSource.releasePeriod(preloadMediaPeriod.mediaPeriod);
    }

    @Override // androidx.media3.exoplayer.source.CompositeMediaSource, androidx.media3.exoplayer.source.BaseMediaSource
    protected void releaseSourceInternal() {
        if (!isUsedByPlayer()) {
            this.onUsedByPlayerNotified = false;
            if (!this.preloadCalled) {
                this.timeline = null;
                this.prepareChildSourceCalled = false;
                super.releaseSourceInternal();
            }
        }
    }

    public void releasePreloadMediaSource() {
        this.preloadHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.source.preload.PreloadMediaSource$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m132xd9831bcd();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$releasePreloadMediaSource$3$androidx-media3-exoplayer-source-preload-PreloadMediaSource, reason: not valid java name */
    /* synthetic */ void m132xd9831bcd() {
        this.preloadCalled = false;
        this.startPositionUs = C.TIME_UNSET;
        this.onSourcePreparedNotified = false;
        if (this.preloadingMediaPeriodAndKey != null) {
            this.mediaSource.releasePeriod(((PreloadMediaPeriod) this.preloadingMediaPeriodAndKey.first).mediaPeriod);
            this.preloadingMediaPeriodAndKey = null;
        }
        releaseSourceInternal();
        this.preloadHandler.removeCallbacksAndMessages(null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    class PreloadMediaPeriodCallback implements MediaPeriod.Callback {
        private final long periodStartPositionUs;
        private boolean prepared;

        public PreloadMediaPeriodCallback(long periodStartPositionUs) {
            this.periodStartPositionUs = periodStartPositionUs;
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod.Callback
        public void onPrepared(final MediaPeriod mediaPeriod) {
            this.prepared = true;
            PreloadMediaSource.this.preloadHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.source.preload.PreloadMediaSource$PreloadMediaPeriodCallback$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() throws ExoPlaybackException {
                    this.f$0.m134x5a2a8722(mediaPeriod);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$onPrepared$0$androidx-media3-exoplayer-source-preload-PreloadMediaSource$PreloadMediaPeriodCallback, reason: not valid java name */
        /* synthetic */ void m134x5a2a8722(MediaPeriod mediaPeriod) throws ExoPlaybackException {
            if (PreloadMediaSource.this.isUsedByPlayer()) {
                return;
            }
            PreloadMediaPeriod preloadMediaPeriod = (PreloadMediaPeriod) mediaPeriod;
            TrackGroupArray trackGroups = preloadMediaPeriod.getTrackGroups();
            TrackSelectorResult trackSelectorResult = null;
            MediaPeriodKey key = (MediaPeriodKey) ((Pair) Assertions.checkNotNull(PreloadMediaSource.this.preloadingMediaPeriodAndKey)).second;
            try {
                trackSelectorResult = PreloadMediaSource.this.trackSelector.selectTracks(PreloadMediaSource.this.rendererCapabilities, trackGroups, key.mediaPeriodId, (Timeline) Assertions.checkNotNull(PreloadMediaSource.this.timeline));
            } catch (ExoPlaybackException e) {
                Log.e(PreloadMediaSource.TAG, "Failed to select tracks", e);
            }
            if (trackSelectorResult != null) {
                preloadMediaPeriod.selectTracksForPreloading(trackSelectorResult.selections, this.periodStartPositionUs);
                if (PreloadMediaSource.this.preloadControl.onTracksSelected(PreloadMediaSource.this)) {
                    preloadMediaPeriod.continueLoading(new LoadingInfo.Builder().setPlaybackPositionUs(this.periodStartPositionUs).build());
                }
            }
        }

        @Override // androidx.media3.exoplayer.source.SequenceableLoader.Callback
        public void onContinueLoadingRequested(final MediaPeriod mediaPeriod) {
            PreloadMediaSource.this.preloadHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.source.preload.PreloadMediaSource$PreloadMediaPeriodCallback$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m133xba37e565(mediaPeriod);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$onContinueLoadingRequested$1$androidx-media3-exoplayer-source-preload-PreloadMediaSource$PreloadMediaPeriodCallback, reason: not valid java name */
        /* synthetic */ void m133xba37e565(MediaPeriod mediaPeriod) {
            if (PreloadMediaSource.this.isUsedByPlayer()) {
                return;
            }
            PreloadMediaPeriod preloadMediaPeriod = (PreloadMediaPeriod) mediaPeriod;
            if (this.prepared && mediaPeriod.getBufferedPositionUs() == Long.MIN_VALUE) {
                PreloadMediaSource.this.preloadControl.onLoadedToTheEndOfSource(PreloadMediaSource.this);
            } else if (!this.prepared || PreloadMediaSource.this.preloadControl.onContinueLoadingRequested(PreloadMediaSource.this, preloadMediaPeriod.getBufferedPositionUs())) {
                preloadMediaPeriod.continueLoading(new LoadingInfo.Builder().setPlaybackPositionUs(this.periodStartPositionUs).build());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isUsedByPlayer() {
        return prepareSourceCalled();
    }

    private void notifyOnUsedByPlayer() {
        this.preloadControl.onUsedByPlayer(this);
        this.onUsedByPlayerNotified = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean mediaPeriodIdEqualsWithoutWindowSequenceNumber(MediaSource.MediaPeriodId firstPeriodId, MediaSource.MediaPeriodId secondPeriodId) {
        return firstPeriodId.periodUid.equals(secondPeriodId.periodUid) && firstPeriodId.adGroupIndex == secondPeriodId.adGroupIndex && firstPeriodId.adIndexInAdGroup == secondPeriodId.adIndexInAdGroup && firstPeriodId.nextAdGroupIndex == secondPeriodId.nextAdGroupIndex;
    }

    private static class MediaPeriodKey {
        public final MediaSource.MediaPeriodId mediaPeriodId;
        private final Long startPositionUs;

        public MediaPeriodKey(MediaSource.MediaPeriodId mediaPeriodId, long startPositionUs) {
            this.mediaPeriodId = mediaPeriodId;
            this.startPositionUs = Long.valueOf(startPositionUs);
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof MediaPeriodKey)) {
                return false;
            }
            MediaPeriodKey mediaPeriodKey = (MediaPeriodKey) other;
            return PreloadMediaSource.mediaPeriodIdEqualsWithoutWindowSequenceNumber(this.mediaPeriodId, mediaPeriodKey.mediaPeriodId) && this.startPositionUs.equals(mediaPeriodKey.startPositionUs);
        }

        public int hashCode() {
            int result = (17 * 31) + this.mediaPeriodId.periodUid.hashCode();
            return (((((((result * 31) + this.mediaPeriodId.adGroupIndex) * 31) + this.mediaPeriodId.adIndexInAdGroup) * 31) + this.mediaPeriodId.nextAdGroupIndex) * 31) + this.startPositionUs.intValue();
        }
    }
}
