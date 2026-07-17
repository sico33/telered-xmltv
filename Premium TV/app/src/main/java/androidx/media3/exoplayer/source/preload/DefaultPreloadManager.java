package androidx.media3.exoplayer.source.preload;

import android.os.Looper;
import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.RendererCapabilitiesList;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.trackselection.TrackSelector;
import androidx.media3.exoplayer.upstream.Allocator;
import androidx.media3.exoplayer.upstream.BandwidthMeter;
import com.google.common.base.Predicate;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Comparator;

/* JADX INFO: loaded from: classes.dex */
public final class DefaultPreloadManager extends BasePreloadManager<Integer> {
    private final PreloadMediaSource.Factory preloadMediaSourceFactory;
    private final RendererCapabilitiesList rendererCapabilitiesList;

    public static class Status implements TargetPreloadStatusControl.PreloadStatus {
        public static final int STAGE_LOADED_TO_POSITION_MS = 2;
        public static final int STAGE_SOURCE_PREPARED = 0;
        public static final int STAGE_TRACKS_SELECTED = 1;
        private final int stage;
        private final long value;

        @Target({ElementType.TYPE_USE})
        @Documented
        @Retention(RetentionPolicy.SOURCE)
        public @interface Stage {
        }

        public Status(int stage, long value) {
            this.stage = stage;
            this.value = value;
        }

        public Status(int stage) {
            this(stage, C.TIME_UNSET);
        }

        @Override // androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl.PreloadStatus
        public int getStage() {
            return this.stage;
        }

        @Override // androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl.PreloadStatus
        public long getValue() {
            return this.value;
        }
    }

    public DefaultPreloadManager(TargetPreloadStatusControl<Integer> targetPreloadStatusControl, MediaSource.Factory mediaSourceFactory, TrackSelector trackSelector, BandwidthMeter bandwidthMeter, RendererCapabilitiesList.Factory rendererCapabilitiesListFactory, Allocator allocator, Looper preloadLooper) {
        super(new RankingDataComparator(), targetPreloadStatusControl, mediaSourceFactory);
        this.rendererCapabilitiesList = rendererCapabilitiesListFactory.createRendererCapabilitiesList();
        this.preloadMediaSourceFactory = new PreloadMediaSource.Factory(mediaSourceFactory, new SourcePreloadControl(), trackSelector, bandwidthMeter, this.rendererCapabilitiesList.getRendererCapabilities(), allocator, preloadLooper);
    }

    public void setCurrentPlayingIndex(int currentPlayingIndex) {
        RankingDataComparator rankingDataComparator = (RankingDataComparator) this.rankingDataComparator;
        rankingDataComparator.currentPlayingIndex = currentPlayingIndex;
    }

    @Override // androidx.media3.exoplayer.source.preload.BasePreloadManager
    public MediaSource createMediaSourceForPreloading(MediaSource mediaSource) {
        return this.preloadMediaSourceFactory.createMediaSource(mediaSource);
    }

    @Override // androidx.media3.exoplayer.source.preload.BasePreloadManager
    protected void preloadSourceInternal(MediaSource mediaSource, long startPositionsUs) {
        Assertions.checkArgument(mediaSource instanceof PreloadMediaSource);
        ((PreloadMediaSource) mediaSource).preload(startPositionsUs);
    }

    @Override // androidx.media3.exoplayer.source.preload.BasePreloadManager
    protected void clearSourceInternal(MediaSource mediaSource) {
        Assertions.checkArgument(mediaSource instanceof PreloadMediaSource);
        ((PreloadMediaSource) mediaSource).clear();
    }

    @Override // androidx.media3.exoplayer.source.preload.BasePreloadManager
    protected void releaseSourceInternal(MediaSource mediaSource) {
        Assertions.checkArgument(mediaSource instanceof PreloadMediaSource);
        ((PreloadMediaSource) mediaSource).releasePreloadMediaSource();
    }

    @Override // androidx.media3.exoplayer.source.preload.BasePreloadManager
    protected void releaseInternal() {
        this.rendererCapabilitiesList.release();
    }

    private static final class RankingDataComparator implements Comparator<Integer> {
        public int currentPlayingIndex = -1;

        @Override // java.util.Comparator
        public int compare(Integer o1, Integer o2) {
            return Integer.compare(Math.abs(o1.intValue() - this.currentPlayingIndex), Math.abs(o2.intValue() - this.currentPlayingIndex));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class SourcePreloadControl implements PreloadMediaSource.PreloadControl {
        private SourcePreloadControl() {
        }

        @Override // androidx.media3.exoplayer.source.preload.PreloadMediaSource.PreloadControl
        public boolean onSourcePrepared(PreloadMediaSource mediaSource) {
            return continueOrCompletePreloading(mediaSource, new Predicate() { // from class: androidx.media3.exoplayer.source.preload.DefaultPreloadManager$SourcePreloadControl$$ExternalSyntheticLambda1
                @Override // com.google.common.base.Predicate
                public final boolean apply(Object obj) {
                    return DefaultPreloadManager.SourcePreloadControl.lambda$onSourcePrepared$0((DefaultPreloadManager.Status) obj);
                }
            }, true);
        }

        static /* synthetic */ boolean lambda$onSourcePrepared$0(Status status) {
            return status.getStage() > 0;
        }

        @Override // androidx.media3.exoplayer.source.preload.PreloadMediaSource.PreloadControl
        public boolean onTracksSelected(PreloadMediaSource mediaSource) {
            return continueOrCompletePreloading(mediaSource, new Predicate() { // from class: androidx.media3.exoplayer.source.preload.DefaultPreloadManager$SourcePreloadControl$$ExternalSyntheticLambda0
                @Override // com.google.common.base.Predicate
                public final boolean apply(Object obj) {
                    return DefaultPreloadManager.SourcePreloadControl.lambda$onTracksSelected$1((DefaultPreloadManager.Status) obj);
                }
            }, false);
        }

        static /* synthetic */ boolean lambda$onTracksSelected$1(Status status) {
            return status.getStage() > 1;
        }

        @Override // androidx.media3.exoplayer.source.preload.PreloadMediaSource.PreloadControl
        public boolean onContinueLoadingRequested(PreloadMediaSource mediaSource, final long bufferedPositionUs) {
            return continueOrCompletePreloading(mediaSource, new Predicate() { // from class: androidx.media3.exoplayer.source.preload.DefaultPreloadManager$SourcePreloadControl$$ExternalSyntheticLambda2
                @Override // com.google.common.base.Predicate
                public final boolean apply(Object obj) {
                    return DefaultPreloadManager.SourcePreloadControl.lambda$onContinueLoadingRequested$2(bufferedPositionUs, (DefaultPreloadManager.Status) obj);
                }
            }, false);
        }

        static /* synthetic */ boolean lambda$onContinueLoadingRequested$2(long bufferedPositionUs, Status status) {
            return status.getStage() == 2 && status.getValue() > Util.usToMs(bufferedPositionUs);
        }

        @Override // androidx.media3.exoplayer.source.preload.PreloadMediaSource.PreloadControl
        public void onUsedByPlayer(PreloadMediaSource mediaSource) {
            DefaultPreloadManager.this.onPreloadCompleted(mediaSource);
        }

        @Override // androidx.media3.exoplayer.source.preload.PreloadMediaSource.PreloadControl
        public void onLoadedToTheEndOfSource(PreloadMediaSource mediaSource) {
            DefaultPreloadManager.this.onPreloadCompleted(mediaSource);
        }

        private boolean continueOrCompletePreloading(PreloadMediaSource mediaSource, Predicate<Status> continueLoadingPredicate, boolean clearExceededDataFromTargetPreloadStatus) {
            TargetPreloadStatusControl.PreloadStatus targetPreloadStatus = DefaultPreloadManager.this.getTargetPreloadStatus(mediaSource);
            if (targetPreloadStatus != null) {
                Status status = (Status) targetPreloadStatus;
                if (continueLoadingPredicate.apply((Status) Assertions.checkNotNull(status))) {
                    return true;
                }
                if (clearExceededDataFromTargetPreloadStatus) {
                    DefaultPreloadManager.this.clearSourceInternal(mediaSource);
                }
            }
            DefaultPreloadManager.this.onPreloadCompleted(mediaSource);
            return false;
        }
    }
}
