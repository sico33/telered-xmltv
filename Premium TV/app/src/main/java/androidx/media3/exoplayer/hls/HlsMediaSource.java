package androidx.media3.exoplayer.hls;

import android.os.Looper;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaLibraryInfo;
import androidx.media3.common.StreamKey;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.TransferListener;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider;
import androidx.media3.exoplayer.drm.DrmSessionEventListener;
import androidx.media3.exoplayer.drm.DrmSessionManager;
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider;
import androidx.media3.exoplayer.hls.playlist.DefaultHlsPlaylistParserFactory;
import androidx.media3.exoplayer.hls.playlist.DefaultHlsPlaylistTracker;
import androidx.media3.exoplayer.hls.playlist.FilteringHlsPlaylistParserFactory;
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist;
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist;
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParserFactory;
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker;
import androidx.media3.exoplayer.source.BaseMediaSource;
import androidx.media3.exoplayer.source.CompositeSequenceableLoaderFactory;
import androidx.media3.exoplayer.source.DefaultCompositeSequenceableLoaderFactory;
import androidx.media3.exoplayer.source.MediaPeriod;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.MediaSourceEventListener;
import androidx.media3.exoplayer.source.MediaSourceFactory;
import androidx.media3.exoplayer.source.SinglePeriodTimeline;
import androidx.media3.exoplayer.upstream.Allocator;
import androidx.media3.exoplayer.upstream.CmcdConfiguration;
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import androidx.media3.extractor.text.SubtitleParser;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class HlsMediaSource extends BaseMediaSource implements HlsPlaylistTracker.PrimaryPlaylistListener {
    public static final int METADATA_TYPE_EMSG = 3;
    public static final int METADATA_TYPE_ID3 = 1;
    private final boolean allowChunklessPreparation;
    private final CmcdConfiguration cmcdConfiguration;
    private final CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory;
    private final HlsDataSourceFactory dataSourceFactory;
    private final DrmSessionManager drmSessionManager;
    private final long elapsedRealTimeOffsetMs;
    private final HlsExtractorFactory extractorFactory;
    private MediaItem.LiveConfiguration liveConfiguration;
    private final LoadErrorHandlingPolicy loadErrorHandlingPolicy;
    private MediaItem mediaItem;
    private TransferListener mediaTransferListener;
    private final int metadataType;
    private final HlsPlaylistTracker playlistTracker;
    private final long timestampAdjusterInitializationTimeoutMs;
    private final boolean useSessionKeys;

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface MetadataType {
    }

    static {
        MediaLibraryInfo.registerModule("media3.exoplayer.hls");
    }

    public static final class Factory implements MediaSourceFactory {
        private boolean allowChunklessPreparation;
        private CmcdConfiguration.Factory cmcdConfigurationFactory;
        private CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory;
        private DrmSessionManagerProvider drmSessionManagerProvider;
        private long elapsedRealTimeOffsetMs;
        private HlsExtractorFactory extractorFactory;
        private final HlsDataSourceFactory hlsDataSourceFactory;
        private LoadErrorHandlingPolicy loadErrorHandlingPolicy;
        private int metadataType;
        private HlsPlaylistParserFactory playlistParserFactory;
        private HlsPlaylistTracker.Factory playlistTrackerFactory;
        private long timestampAdjusterInitializationTimeoutMs;
        private boolean useSessionKeys;

        public Factory(DataSource.Factory dataSourceFactory) {
            this(new DefaultHlsDataSourceFactory(dataSourceFactory));
        }

        public Factory(HlsDataSourceFactory hlsDataSourceFactory) {
            this.hlsDataSourceFactory = (HlsDataSourceFactory) Assertions.checkNotNull(hlsDataSourceFactory);
            this.drmSessionManagerProvider = new DefaultDrmSessionManagerProvider();
            this.playlistParserFactory = new DefaultHlsPlaylistParserFactory();
            this.playlistTrackerFactory = DefaultHlsPlaylistTracker.FACTORY;
            this.extractorFactory = HlsExtractorFactory.DEFAULT;
            this.loadErrorHandlingPolicy = new DefaultLoadErrorHandlingPolicy();
            this.compositeSequenceableLoaderFactory = new DefaultCompositeSequenceableLoaderFactory();
            this.metadataType = 1;
            this.elapsedRealTimeOffsetMs = C.TIME_UNSET;
            this.allowChunklessPreparation = true;
            experimentalParseSubtitlesDuringExtraction(true);
        }

        public Factory setExtractorFactory(HlsExtractorFactory extractorFactory) {
            this.extractorFactory = extractorFactory != null ? extractorFactory : HlsExtractorFactory.DEFAULT;
            return this;
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public Factory setLoadErrorHandlingPolicy(LoadErrorHandlingPolicy loadErrorHandlingPolicy) {
            this.loadErrorHandlingPolicy = (LoadErrorHandlingPolicy) Assertions.checkNotNull(loadErrorHandlingPolicy, "MediaSource.Factory#setLoadErrorHandlingPolicy no longer handles null by instantiating a new DefaultLoadErrorHandlingPolicy. Explicitly construct and pass an instance in order to retain the old behavior.");
            return this;
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public Factory setSubtitleParserFactory(SubtitleParser.Factory subtitleParserFactory) {
            this.extractorFactory.setSubtitleParserFactory((SubtitleParser.Factory) Assertions.checkNotNull(subtitleParserFactory));
            return this;
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        @Deprecated
        public Factory experimentalParseSubtitlesDuringExtraction(boolean parseSubtitlesDuringExtraction) {
            this.extractorFactory.experimentalParseSubtitlesDuringExtraction(parseSubtitlesDuringExtraction);
            return this;
        }

        public Factory setPlaylistParserFactory(HlsPlaylistParserFactory playlistParserFactory) {
            this.playlistParserFactory = (HlsPlaylistParserFactory) Assertions.checkNotNull(playlistParserFactory, "HlsMediaSource.Factory#setPlaylistParserFactory no longer handles null by instantiating a new DefaultHlsPlaylistParserFactory. Explicitly construct and pass an instance in order to retain the old behavior.");
            return this;
        }

        public Factory setPlaylistTrackerFactory(HlsPlaylistTracker.Factory playlistTrackerFactory) {
            this.playlistTrackerFactory = (HlsPlaylistTracker.Factory) Assertions.checkNotNull(playlistTrackerFactory, "HlsMediaSource.Factory#setPlaylistTrackerFactory no longer handles null by defaulting to DefaultHlsPlaylistTracker.FACTORY. Explicitly pass a reference to this instance in order to retain the old behavior.");
            return this;
        }

        public Factory setCompositeSequenceableLoaderFactory(CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory) {
            this.compositeSequenceableLoaderFactory = (CompositeSequenceableLoaderFactory) Assertions.checkNotNull(compositeSequenceableLoaderFactory, "HlsMediaSource.Factory#setCompositeSequenceableLoaderFactory no longer handles null by instantiating a new DefaultCompositeSequenceableLoaderFactory. Explicitly construct and pass an instance in order to retain the old behavior.");
            return this;
        }

        public Factory setAllowChunklessPreparation(boolean allowChunklessPreparation) {
            this.allowChunklessPreparation = allowChunklessPreparation;
            return this;
        }

        public Factory setMetadataType(int metadataType) {
            this.metadataType = metadataType;
            return this;
        }

        public Factory setUseSessionKeys(boolean useSessionKeys) {
            this.useSessionKeys = useSessionKeys;
            return this;
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public Factory setCmcdConfigurationFactory(CmcdConfiguration.Factory cmcdConfigurationFactory) {
            this.cmcdConfigurationFactory = (CmcdConfiguration.Factory) Assertions.checkNotNull(cmcdConfigurationFactory);
            return this;
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public Factory setDrmSessionManagerProvider(DrmSessionManagerProvider drmSessionManagerProvider) {
            this.drmSessionManagerProvider = (DrmSessionManagerProvider) Assertions.checkNotNull(drmSessionManagerProvider, "MediaSource.Factory#setDrmSessionManagerProvider no longer handles null by instantiating a new DefaultDrmSessionManagerProvider. Explicitly construct and pass an instance in order to retain the old behavior.");
            return this;
        }

        public Factory setTimestampAdjusterInitializationTimeoutMs(long timestampAdjusterInitializationTimeoutMs) {
            this.timestampAdjusterInitializationTimeoutMs = timestampAdjusterInitializationTimeoutMs;
            return this;
        }

        Factory setElapsedRealTimeOffsetMs(long elapsedRealTimeOffsetMs) {
            this.elapsedRealTimeOffsetMs = elapsedRealTimeOffsetMs;
            return this;
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public HlsMediaSource createMediaSource(MediaItem mediaItem) {
            CmcdConfiguration cmcdConfiguration;
            Assertions.checkNotNull(mediaItem.localConfiguration);
            HlsPlaylistParserFactory playlistParserFactory = this.playlistParserFactory;
            List<StreamKey> streamKeys = mediaItem.localConfiguration.streamKeys;
            if (!streamKeys.isEmpty()) {
                playlistParserFactory = new FilteringHlsPlaylistParserFactory(playlistParserFactory, streamKeys);
            }
            if (this.cmcdConfigurationFactory == null) {
                cmcdConfiguration = null;
            } else {
                cmcdConfiguration = this.cmcdConfigurationFactory.createCmcdConfiguration(mediaItem);
            }
            return new HlsMediaSource(mediaItem, this.hlsDataSourceFactory, this.extractorFactory, this.compositeSequenceableLoaderFactory, cmcdConfiguration, this.drmSessionManagerProvider.get(mediaItem), this.loadErrorHandlingPolicy, this.playlistTrackerFactory.createTracker(this.hlsDataSourceFactory, this.loadErrorHandlingPolicy, playlistParserFactory), this.elapsedRealTimeOffsetMs, this.allowChunklessPreparation, this.metadataType, this.useSessionKeys, this.timestampAdjusterInitializationTimeoutMs);
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public int[] getSupportedTypes() {
            return new int[]{2};
        }
    }

    private HlsMediaSource(MediaItem mediaItem, HlsDataSourceFactory dataSourceFactory, HlsExtractorFactory extractorFactory, CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory, CmcdConfiguration cmcdConfiguration, DrmSessionManager drmSessionManager, LoadErrorHandlingPolicy loadErrorHandlingPolicy, HlsPlaylistTracker playlistTracker, long elapsedRealTimeOffsetMs, boolean allowChunklessPreparation, int metadataType, boolean useSessionKeys, long timestampAdjusterInitializationTimeoutMs) {
        this.mediaItem = mediaItem;
        this.liveConfiguration = mediaItem.liveConfiguration;
        this.dataSourceFactory = dataSourceFactory;
        this.extractorFactory = extractorFactory;
        this.compositeSequenceableLoaderFactory = compositeSequenceableLoaderFactory;
        this.cmcdConfiguration = cmcdConfiguration;
        this.drmSessionManager = drmSessionManager;
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy;
        this.playlistTracker = playlistTracker;
        this.elapsedRealTimeOffsetMs = elapsedRealTimeOffsetMs;
        this.allowChunklessPreparation = allowChunklessPreparation;
        this.metadataType = metadataType;
        this.useSessionKeys = useSessionKeys;
        this.timestampAdjusterInitializationTimeoutMs = timestampAdjusterInitializationTimeoutMs;
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public synchronized MediaItem getMediaItem() {
        return this.mediaItem;
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public boolean canUpdateMediaItem(MediaItem mediaItem) {
        MediaItem existingMediaItem = getMediaItem();
        MediaItem.LocalConfiguration existingConfiguration = (MediaItem.LocalConfiguration) Assertions.checkNotNull(existingMediaItem.localConfiguration);
        MediaItem.LocalConfiguration newConfiguration = mediaItem.localConfiguration;
        return newConfiguration != null && newConfiguration.uri.equals(existingConfiguration.uri) && newConfiguration.streamKeys.equals(existingConfiguration.streamKeys) && Util.areEqual(newConfiguration.drmConfiguration, existingConfiguration.drmConfiguration) && existingMediaItem.liveConfiguration.equals(mediaItem.liveConfiguration);
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public synchronized void updateMediaItem(MediaItem mediaItem) {
        this.mediaItem = mediaItem;
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource
    protected void prepareSourceInternal(TransferListener mediaTransferListener) {
        this.mediaTransferListener = mediaTransferListener;
        this.drmSessionManager.setPlayer((Looper) Assertions.checkNotNull(Looper.myLooper()), getPlayerId());
        this.drmSessionManager.prepare();
        MediaSourceEventListener.EventDispatcher eventDispatcher = createEventDispatcher(null);
        this.playlistTracker.start(((MediaItem.LocalConfiguration) Assertions.checkNotNull(getMediaItem().localConfiguration)).uri, eventDispatcher, this);
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public void maybeThrowSourceInfoRefreshError() throws IOException {
        this.playlistTracker.maybeThrowPrimaryPlaylistRefreshError();
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator, long startPositionUs) {
        MediaSourceEventListener.EventDispatcher mediaSourceEventDispatcher = createEventDispatcher(id);
        DrmSessionEventListener.EventDispatcher drmEventDispatcher = createDrmEventDispatcher(id);
        return new HlsMediaPeriod(this.extractorFactory, this.playlistTracker, this.dataSourceFactory, this.mediaTransferListener, this.cmcdConfiguration, this.drmSessionManager, drmEventDispatcher, this.loadErrorHandlingPolicy, mediaSourceEventDispatcher, allocator, this.compositeSequenceableLoaderFactory, this.allowChunklessPreparation, this.metadataType, this.useSessionKeys, getPlayerId(), this.timestampAdjusterInitializationTimeoutMs);
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public void releasePeriod(MediaPeriod mediaPeriod) {
        ((HlsMediaPeriod) mediaPeriod).release();
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource
    protected void releaseSourceInternal() {
        this.playlistTracker.stop();
        this.drmSessionManager.release();
    }

    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker.PrimaryPlaylistListener
    public void onPrimaryPlaylistRefreshed(HlsMediaPlaylist mediaPlaylist) {
        long presentationStartTimeMs;
        SinglePeriodTimeline timeline;
        long windowStartTimeMs = mediaPlaylist.hasProgramDateTime ? Util.usToMs(mediaPlaylist.startTimeUs) : -9223372036854775807L;
        if (mediaPlaylist.playlistType == 2 || mediaPlaylist.playlistType == 1) {
            presentationStartTimeMs = windowStartTimeMs;
        } else {
            presentationStartTimeMs = -9223372036854775807L;
        }
        HlsManifest manifest = new HlsManifest((HlsMultivariantPlaylist) Assertions.checkNotNull(this.playlistTracker.getMultivariantPlaylist()), mediaPlaylist);
        if (this.playlistTracker.isLive()) {
            timeline = createTimelineForLive(mediaPlaylist, presentationStartTimeMs, windowStartTimeMs, manifest);
        } else {
            timeline = createTimelineForOnDemand(mediaPlaylist, presentationStartTimeMs, windowStartTimeMs, manifest);
        }
        refreshSourceInfo(timeline);
    }

    private SinglePeriodTimeline createTimelineForLive(HlsMediaPlaylist playlist, long presentationStartTimeMs, long windowStartTimeMs, HlsManifest manifest) {
        long targetLiveOffsetUs;
        long offsetFromInitialStartTimeUs = playlist.startTimeUs - this.playlistTracker.getInitialStartTimeUs();
        long periodDurationUs = playlist.hasEndTag ? offsetFromInitialStartTimeUs + playlist.durationUs : -9223372036854775807L;
        long liveEdgeOffsetUs = getLiveEdgeOffsetUs(playlist);
        if (this.liveConfiguration.targetOffsetMs != C.TIME_UNSET) {
            targetLiveOffsetUs = Util.msToUs(this.liveConfiguration.targetOffsetMs);
        } else {
            long targetLiveOffsetUs2 = getTargetLiveOffsetUs(playlist, liveEdgeOffsetUs);
            targetLiveOffsetUs = targetLiveOffsetUs2;
        }
        updateLiveConfiguration(playlist, Util.constrainValue(targetLiveOffsetUs, liveEdgeOffsetUs, playlist.durationUs + liveEdgeOffsetUs));
        long windowDefaultStartPositionUs = getLiveWindowDefaultStartPositionUs(playlist, liveEdgeOffsetUs);
        boolean suppressPositionProjection = playlist.playlistType == 2 && playlist.hasPositiveStartOffset;
        return new SinglePeriodTimeline(presentationStartTimeMs, windowStartTimeMs, C.TIME_UNSET, periodDurationUs, playlist.durationUs, offsetFromInitialStartTimeUs, windowDefaultStartPositionUs, true, !playlist.hasEndTag, suppressPositionProjection, manifest, getMediaItem(), this.liveConfiguration);
    }

    private SinglePeriodTimeline createTimelineForOnDemand(HlsMediaPlaylist playlist, long presentationStartTimeMs, long windowStartTimeMs, HlsManifest manifest) {
        long windowDefaultStartPositionUs;
        if (playlist.startOffsetUs == C.TIME_UNSET || playlist.segments.isEmpty()) {
            windowDefaultStartPositionUs = 0;
        } else if (playlist.preciseStart || playlist.startOffsetUs == playlist.durationUs) {
            long windowDefaultStartPositionUs2 = playlist.startOffsetUs;
            windowDefaultStartPositionUs = windowDefaultStartPositionUs2;
        } else {
            windowDefaultStartPositionUs = findClosestPrecedingSegment(playlist.segments, playlist.startOffsetUs).relativeStartTimeUs;
        }
        return new SinglePeriodTimeline(presentationStartTimeMs, windowStartTimeMs, C.TIME_UNSET, playlist.durationUs, playlist.durationUs, 0L, windowDefaultStartPositionUs, true, false, true, manifest, getMediaItem(), null);
    }

    private long getLiveEdgeOffsetUs(HlsMediaPlaylist playlist) {
        if (playlist.hasProgramDateTime) {
            return Util.msToUs(Util.getNowUnixTimeMs(this.elapsedRealTimeOffsetMs)) - playlist.getEndTimeUs();
        }
        return 0L;
    }

    private long getLiveWindowDefaultStartPositionUs(HlsMediaPlaylist playlist, long liveEdgeOffsetUs) {
        long startPositionUs;
        if (playlist.startOffsetUs != C.TIME_UNSET) {
            startPositionUs = playlist.startOffsetUs;
        } else {
            startPositionUs = (playlist.durationUs + liveEdgeOffsetUs) - Util.msToUs(this.liveConfiguration.targetOffsetMs);
        }
        if (playlist.preciseStart) {
            return startPositionUs;
        }
        HlsMediaPlaylist.Part part = findClosestPrecedingIndependentPart(playlist.trailingParts, startPositionUs);
        if (part != null) {
            return part.relativeStartTimeUs;
        }
        if (playlist.segments.isEmpty()) {
            return 0L;
        }
        HlsMediaPlaylist.Segment segment = findClosestPrecedingSegment(playlist.segments, startPositionUs);
        HlsMediaPlaylist.Part part2 = findClosestPrecedingIndependentPart(segment.parts, startPositionUs);
        if (part2 != null) {
            return part2.relativeStartTimeUs;
        }
        return segment.relativeStartTimeUs;
    }

    private void updateLiveConfiguration(HlsMediaPlaylist playlist, long targetLiveOffsetUs) {
        MediaItem.LiveConfiguration mediaItemLiveConfiguration = getMediaItem().liveConfiguration;
        boolean disableSpeedAdjustment = mediaItemLiveConfiguration.minPlaybackSpeed == -3.4028235E38f && mediaItemLiveConfiguration.maxPlaybackSpeed == -3.4028235E38f && playlist.serverControl.holdBackUs == C.TIME_UNSET && playlist.serverControl.partHoldBackUs == C.TIME_UNSET;
        this.liveConfiguration = new MediaItem.LiveConfiguration.Builder().setTargetOffsetMs(Util.usToMs(targetLiveOffsetUs)).setMinPlaybackSpeed(disableSpeedAdjustment ? 1.0f : this.liveConfiguration.minPlaybackSpeed).setMaxPlaybackSpeed(disableSpeedAdjustment ? 1.0f : this.liveConfiguration.maxPlaybackSpeed).build();
    }

    private static long getTargetLiveOffsetUs(HlsMediaPlaylist playlist, long liveEdgeOffsetUs) {
        long targetOffsetUs;
        HlsMediaPlaylist.ServerControl serverControl = playlist.serverControl;
        if (playlist.startOffsetUs != C.TIME_UNSET) {
            targetOffsetUs = playlist.durationUs - playlist.startOffsetUs;
        } else {
            long targetOffsetUs2 = serverControl.partHoldBackUs;
            if (targetOffsetUs2 != C.TIME_UNSET && playlist.partTargetDurationUs != C.TIME_UNSET) {
                targetOffsetUs = serverControl.partHoldBackUs;
            } else {
                long targetOffsetUs3 = serverControl.holdBackUs;
                if (targetOffsetUs3 != C.TIME_UNSET) {
                    targetOffsetUs = serverControl.holdBackUs;
                } else {
                    targetOffsetUs = 3 * playlist.targetDurationUs;
                }
            }
        }
        return targetOffsetUs + liveEdgeOffsetUs;
    }

    private static HlsMediaPlaylist.Part findClosestPrecedingIndependentPart(List<HlsMediaPlaylist.Part> parts, long positionUs) {
        HlsMediaPlaylist.Part closestPart = null;
        for (int i = 0; i < parts.size(); i++) {
            HlsMediaPlaylist.Part part = parts.get(i);
            if (part.relativeStartTimeUs > positionUs || !part.isIndependent) {
                if (part.relativeStartTimeUs > positionUs) {
                    break;
                }
            } else {
                closestPart = part;
            }
        }
        return closestPart;
    }

    private static HlsMediaPlaylist.Segment findClosestPrecedingSegment(List<HlsMediaPlaylist.Segment> segments, long positionUs) {
        int segmentIndex = Util.binarySearchFloor((List<? extends Comparable<? super Long>>) segments, Long.valueOf(positionUs), true, true);
        return segments.get(segmentIndex);
    }
}
