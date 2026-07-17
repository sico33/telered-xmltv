package androidx.media3.exoplayer.source;

import android.os.Looper;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.TransferListener;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider;
import androidx.media3.exoplayer.drm.DrmSessionManager;
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider;
import androidx.media3.exoplayer.upstream.Allocator;
import androidx.media3.exoplayer.upstream.CmcdConfiguration;
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.ExtractorsFactory;
import androidx.media3.extractor.text.SubtitleParser;

/* JADX INFO: loaded from: classes.dex */
public final class ProgressiveMediaSource extends BaseMediaSource implements ProgressiveMediaPeriod.Listener {
    public static final int DEFAULT_LOADING_CHECK_INTERVAL_BYTES = 1048576;
    private final int continueLoadingCheckIntervalBytes;
    private final DataSource.Factory dataSourceFactory;
    private final DrmSessionManager drmSessionManager;
    private final LoadErrorHandlingPolicy loadableLoadErrorHandlingPolicy;
    private MediaItem mediaItem;
    private final ProgressiveMediaExtractor.Factory progressiveMediaExtractorFactory;
    private long timelineDurationUs;
    private boolean timelineIsLive;
    private boolean timelineIsPlaceholder;
    private boolean timelineIsSeekable;
    private TransferListener transferListener;

    public static final class Factory implements MediaSourceFactory {
        private int continueLoadingCheckIntervalBytes;
        private final DataSource.Factory dataSourceFactory;
        private DrmSessionManagerProvider drmSessionManagerProvider;
        private LoadErrorHandlingPolicy loadErrorHandlingPolicy;
        private ProgressiveMediaExtractor.Factory progressiveMediaExtractorFactory;

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public /* synthetic */ MediaSource.Factory experimentalParseSubtitlesDuringExtraction(boolean z) {
            return MediaSource.Factory.CC.$default$experimentalParseSubtitlesDuringExtraction(this, z);
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public /* synthetic */ MediaSource.Factory setCmcdConfigurationFactory(CmcdConfiguration.Factory factory) {
            return MediaSource.Factory.CC.$default$setCmcdConfigurationFactory(this, factory);
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public /* synthetic */ MediaSource.Factory setSubtitleParserFactory(SubtitleParser.Factory factory) {
            return MediaSource.Factory.CC.$default$setSubtitleParserFactory(this, factory);
        }

        public Factory(DataSource.Factory dataSourceFactory) {
            this(dataSourceFactory, new DefaultExtractorsFactory());
        }

        public Factory(DataSource.Factory dataSourceFactory, final ExtractorsFactory extractorsFactory) {
            this(dataSourceFactory, new ProgressiveMediaExtractor.Factory() { // from class: androidx.media3.exoplayer.source.ProgressiveMediaSource$Factory$$ExternalSyntheticLambda0
                @Override // androidx.media3.exoplayer.source.ProgressiveMediaExtractor.Factory
                public final ProgressiveMediaExtractor createProgressiveMediaExtractor(PlayerId playerId) {
                    return ProgressiveMediaSource.Factory.lambda$new$0(extractorsFactory, playerId);
                }
            });
        }

        static /* synthetic */ ProgressiveMediaExtractor lambda$new$0(ExtractorsFactory extractorsFactory, PlayerId playerId) {
            return new BundledExtractorsAdapter(extractorsFactory);
        }

        public Factory(DataSource.Factory dataSourceFactory, ProgressiveMediaExtractor.Factory progressiveMediaExtractorFactory) {
            this(dataSourceFactory, progressiveMediaExtractorFactory, new DefaultDrmSessionManagerProvider(), new DefaultLoadErrorHandlingPolicy(), 1048576);
        }

        public Factory(DataSource.Factory dataSourceFactory, ProgressiveMediaExtractor.Factory progressiveMediaExtractorFactory, DrmSessionManagerProvider drmSessionManagerProvider, LoadErrorHandlingPolicy loadErrorHandlingPolicy, int continueLoadingCheckIntervalBytes) {
            this.dataSourceFactory = dataSourceFactory;
            this.progressiveMediaExtractorFactory = progressiveMediaExtractorFactory;
            this.drmSessionManagerProvider = drmSessionManagerProvider;
            this.loadErrorHandlingPolicy = loadErrorHandlingPolicy;
            this.continueLoadingCheckIntervalBytes = continueLoadingCheckIntervalBytes;
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public Factory setLoadErrorHandlingPolicy(LoadErrorHandlingPolicy loadErrorHandlingPolicy) {
            this.loadErrorHandlingPolicy = (LoadErrorHandlingPolicy) Assertions.checkNotNull(loadErrorHandlingPolicy, "MediaSource.Factory#setLoadErrorHandlingPolicy no longer handles null by instantiating a new DefaultLoadErrorHandlingPolicy. Explicitly construct and pass an instance in order to retain the old behavior.");
            return this;
        }

        public Factory setContinueLoadingCheckIntervalBytes(int continueLoadingCheckIntervalBytes) {
            this.continueLoadingCheckIntervalBytes = continueLoadingCheckIntervalBytes;
            return this;
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public Factory setDrmSessionManagerProvider(DrmSessionManagerProvider drmSessionManagerProvider) {
            this.drmSessionManagerProvider = (DrmSessionManagerProvider) Assertions.checkNotNull(drmSessionManagerProvider, "MediaSource.Factory#setDrmSessionManagerProvider no longer handles null by instantiating a new DefaultDrmSessionManagerProvider. Explicitly construct and pass an instance in order to retain the old behavior.");
            return this;
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public ProgressiveMediaSource createMediaSource(MediaItem mediaItem) {
            Assertions.checkNotNull(mediaItem.localConfiguration);
            return new ProgressiveMediaSource(mediaItem, this.dataSourceFactory, this.progressiveMediaExtractorFactory, this.drmSessionManagerProvider.get(mediaItem), this.loadErrorHandlingPolicy, this.continueLoadingCheckIntervalBytes);
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public int[] getSupportedTypes() {
            return new int[]{4};
        }
    }

    private ProgressiveMediaSource(MediaItem mediaItem, DataSource.Factory dataSourceFactory, ProgressiveMediaExtractor.Factory progressiveMediaExtractorFactory, DrmSessionManager drmSessionManager, LoadErrorHandlingPolicy loadableLoadErrorHandlingPolicy, int continueLoadingCheckIntervalBytes) {
        this.mediaItem = mediaItem;
        this.dataSourceFactory = dataSourceFactory;
        this.progressiveMediaExtractorFactory = progressiveMediaExtractorFactory;
        this.drmSessionManager = drmSessionManager;
        this.loadableLoadErrorHandlingPolicy = loadableLoadErrorHandlingPolicy;
        this.continueLoadingCheckIntervalBytes = continueLoadingCheckIntervalBytes;
        this.timelineIsPlaceholder = true;
        this.timelineDurationUs = C.TIME_UNSET;
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public synchronized MediaItem getMediaItem() {
        return this.mediaItem;
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public boolean canUpdateMediaItem(MediaItem mediaItem) {
        MediaItem.LocalConfiguration existingConfiguration = getLocalConfiguration();
        MediaItem.LocalConfiguration newConfiguration = mediaItem.localConfiguration;
        return newConfiguration != null && newConfiguration.uri.equals(existingConfiguration.uri) && newConfiguration.imageDurationMs == existingConfiguration.imageDurationMs && Util.areEqual(newConfiguration.customCacheKey, existingConfiguration.customCacheKey);
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public synchronized void updateMediaItem(MediaItem mediaItem) {
        this.mediaItem = mediaItem;
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource
    protected void prepareSourceInternal(TransferListener mediaTransferListener) {
        this.transferListener = mediaTransferListener;
        this.drmSessionManager.setPlayer((Looper) Assertions.checkNotNull(Looper.myLooper()), getPlayerId());
        this.drmSessionManager.prepare();
        notifySourceInfoRefreshed();
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public void maybeThrowSourceInfoRefreshError() {
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator, long startPositionUs) {
        DataSource dataSource = this.dataSourceFactory.createDataSource();
        if (this.transferListener != null) {
            dataSource.addTransferListener(this.transferListener);
        }
        MediaItem.LocalConfiguration localConfiguration = getLocalConfiguration();
        return new ProgressiveMediaPeriod(localConfiguration.uri, dataSource, this.progressiveMediaExtractorFactory.createProgressiveMediaExtractor(getPlayerId()), this.drmSessionManager, createDrmEventDispatcher(id), this.loadableLoadErrorHandlingPolicy, createEventDispatcher(id), this, allocator, localConfiguration.customCacheKey, this.continueLoadingCheckIntervalBytes, Util.msToUs(localConfiguration.imageDurationMs));
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public void releasePeriod(MediaPeriod mediaPeriod) {
        ((ProgressiveMediaPeriod) mediaPeriod).release();
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource
    protected void releaseSourceInternal() {
        this.drmSessionManager.release();
    }

    @Override // androidx.media3.exoplayer.source.ProgressiveMediaPeriod.Listener
    public void onSourceInfoRefreshed(long durationUs, boolean isSeekable, boolean isLive) {
        long durationUs2 = durationUs == C.TIME_UNSET ? this.timelineDurationUs : durationUs;
        if (!this.timelineIsPlaceholder && this.timelineDurationUs == durationUs2 && this.timelineIsSeekable == isSeekable && this.timelineIsLive == isLive) {
            return;
        }
        this.timelineDurationUs = durationUs2;
        this.timelineIsSeekable = isSeekable;
        this.timelineIsLive = isLive;
        this.timelineIsPlaceholder = false;
        notifySourceInfoRefreshed();
    }

    private MediaItem.LocalConfiguration getLocalConfiguration() {
        return (MediaItem.LocalConfiguration) Assertions.checkNotNull(getMediaItem().localConfiguration);
    }

    private void notifySourceInfoRefreshed() {
        Timeline timeline = new SinglePeriodTimeline(this.timelineDurationUs, this.timelineIsSeekable, false, this.timelineIsLive, (Object) null, getMediaItem());
        if (this.timelineIsPlaceholder) {
            timeline = new ForwardingTimeline(timeline) { // from class: androidx.media3.exoplayer.source.ProgressiveMediaSource.1
                @Override // androidx.media3.exoplayer.source.ForwardingTimeline, androidx.media3.common.Timeline
                public Timeline.Window getWindow(int windowIndex, Timeline.Window window, long defaultPositionProjectionUs) {
                    super.getWindow(windowIndex, window, defaultPositionProjectionUs);
                    window.isPlaceholder = true;
                    return window;
                }

                @Override // androidx.media3.exoplayer.source.ForwardingTimeline, androidx.media3.common.Timeline
                public Timeline.Period getPeriod(int periodIndex, Timeline.Period period, boolean setIds) {
                    super.getPeriod(periodIndex, period, setIds);
                    period.isPlaceholder = true;
                    return period;
                }
            };
        }
        refreshSourceInfo(timeline);
    }
}
