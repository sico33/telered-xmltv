package androidx.media3.exoplayer.source;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.TransferListener;
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider;
import androidx.media3.exoplayer.upstream.Allocator;
import androidx.media3.exoplayer.upstream.CmcdConfiguration;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import androidx.media3.extractor.text.SubtitleParser;
import java.util.Objects;

/* JADX INFO: loaded from: classes.dex */
public final class ExternallyLoadedMediaSource extends BaseMediaSource {
    private final ExternalLoader externalLoader;
    private MediaItem mediaItem;
    private final long timelineDurationUs;

    public static final class Factory implements MediaSource.Factory {
        private final ExternalLoader externalLoader;
        private final long timelineDurationUs;

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

        public Factory(long timelineDurationUs, ExternalLoader externalLoader) {
            this.timelineDurationUs = timelineDurationUs;
            this.externalLoader = externalLoader;
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public MediaSource.Factory setDrmSessionManagerProvider(DrmSessionManagerProvider drmSessionManagerProvider) {
            return this;
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public MediaSource.Factory setLoadErrorHandlingPolicy(LoadErrorHandlingPolicy loadErrorHandlingPolicy) {
            return this;
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public int[] getSupportedTypes() {
            return new int[]{4};
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public ExternallyLoadedMediaSource createMediaSource(MediaItem mediaItem) {
            return new ExternallyLoadedMediaSource(mediaItem, this.timelineDurationUs, this.externalLoader);
        }
    }

    private ExternallyLoadedMediaSource(MediaItem mediaItem, long timelineDurationUs, ExternalLoader externalLoader) {
        this.mediaItem = mediaItem;
        this.timelineDurationUs = timelineDurationUs;
        this.externalLoader = externalLoader;
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource
    protected void prepareSourceInternal(TransferListener mediaTransferListener) {
        Timeline timeline = new SinglePeriodTimeline(this.timelineDurationUs, true, false, false, (Object) null, getMediaItem());
        refreshSourceInfo(timeline);
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource
    protected void releaseSourceInternal() {
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public synchronized MediaItem getMediaItem() {
        return this.mediaItem;
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public boolean canUpdateMediaItem(MediaItem mediaItem) {
        MediaItem.LocalConfiguration newConfiguration = mediaItem.localConfiguration;
        MediaItem.LocalConfiguration oldConfiguration = (MediaItem.LocalConfiguration) Assertions.checkNotNull(getMediaItem().localConfiguration);
        return newConfiguration != null && newConfiguration.uri.equals(oldConfiguration.uri) && Objects.equals(newConfiguration.mimeType, oldConfiguration.mimeType) && (newConfiguration.imageDurationMs == C.TIME_UNSET || Util.msToUs(newConfiguration.imageDurationMs) == this.timelineDurationUs);
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public synchronized void updateMediaItem(MediaItem mediaItem) {
        this.mediaItem = mediaItem;
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public void maybeThrowSourceInfoRefreshError() {
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator, long startPositionUs) {
        MediaItem mediaItem = getMediaItem();
        Assertions.checkNotNull(mediaItem.localConfiguration);
        Assertions.checkNotNull(mediaItem.localConfiguration.mimeType, "Externally loaded mediaItems require a MIME type.");
        return new ExternallyLoadedMediaPeriod(mediaItem.localConfiguration.uri, mediaItem.localConfiguration.mimeType, this.externalLoader);
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public void releasePeriod(MediaPeriod mediaPeriod) {
        ((ExternallyLoadedMediaPeriod) mediaPeriod).releasePeriod();
    }
}
