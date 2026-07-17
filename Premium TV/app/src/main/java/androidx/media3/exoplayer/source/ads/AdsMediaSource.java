package androidx.media3.exoplayer.source.ads;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.media3.common.AdPlaybackState;
import androidx.media3.common.AdViewProvider;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;
import androidx.media3.exoplayer.source.CompositeMediaSource;
import androidx.media3.exoplayer.source.LoadEventInfo;
import androidx.media3.exoplayer.source.MaskingMediaPeriod;
import androidx.media3.exoplayer.source.MaskingMediaSource;
import androidx.media3.exoplayer.source.MediaPeriod;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.upstream.Allocator;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class AdsMediaSource extends CompositeMediaSource<MediaSource.MediaPeriodId> {
    private static final MediaSource.MediaPeriodId CHILD_SOURCE_MEDIA_PERIOD_ID = new MediaSource.MediaPeriodId(new Object());
    private final MediaSource.Factory adMediaSourceFactory;
    private AdPlaybackState adPlaybackState;
    private final DataSpec adTagDataSpec;
    private final AdViewProvider adViewProvider;
    private final Object adsId;
    private final AdsLoader adsLoader;
    private ComponentListener componentListener;
    final MediaItem.DrmConfiguration contentDrmConfiguration;
    private final MaskingMediaSource contentMediaSource;
    private Timeline contentTimeline;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Timeline.Period period = new Timeline.Period();
    private AdMediaSourceHolder[][] adMediaSourceHolders = new AdMediaSourceHolder[0][];

    public static final class AdLoadException extends IOException {
        public static final int TYPE_AD = 0;
        public static final int TYPE_AD_GROUP = 1;
        public static final int TYPE_ALL_ADS = 2;
        public static final int TYPE_UNEXPECTED = 3;
        public final int type;

        @Target({ElementType.TYPE_USE})
        @Documented
        @Retention(RetentionPolicy.SOURCE)
        public @interface Type {
        }

        public static AdLoadException createForAd(Exception error) {
            return new AdLoadException(0, error);
        }

        public static AdLoadException createForAdGroup(Exception error, int adGroupIndex) {
            return new AdLoadException(1, new IOException("Failed to load ad group " + adGroupIndex, error));
        }

        public static AdLoadException createForAllAds(Exception error) {
            return new AdLoadException(2, error);
        }

        public static AdLoadException createForUnexpected(RuntimeException error) {
            return new AdLoadException(3, error);
        }

        private AdLoadException(int type, Exception cause) {
            super(cause);
            this.type = type;
        }

        public RuntimeException getRuntimeExceptionForUnexpected() {
            Assertions.checkState(this.type == 3);
            return (RuntimeException) Assertions.checkNotNull(getCause());
        }
    }

    public AdsMediaSource(MediaSource contentMediaSource, DataSpec adTagDataSpec, Object adsId, MediaSource.Factory adMediaSourceFactory, AdsLoader adsLoader, AdViewProvider adViewProvider) {
        this.contentMediaSource = new MaskingMediaSource(contentMediaSource, true);
        this.contentDrmConfiguration = ((MediaItem.LocalConfiguration) Assertions.checkNotNull(contentMediaSource.getMediaItem().localConfiguration)).drmConfiguration;
        this.adMediaSourceFactory = adMediaSourceFactory;
        this.adsLoader = adsLoader;
        this.adViewProvider = adViewProvider;
        this.adTagDataSpec = adTagDataSpec;
        this.adsId = adsId;
        adsLoader.setSupportedContentTypes(adMediaSourceFactory.getSupportedTypes());
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public MediaItem getMediaItem() {
        return this.contentMediaSource.getMediaItem();
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public boolean canUpdateMediaItem(MediaItem mediaItem) {
        return Util.areEqual(getAdsConfiguration(getMediaItem()), getAdsConfiguration(mediaItem)) && this.contentMediaSource.canUpdateMediaItem(mediaItem);
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public void updateMediaItem(MediaItem mediaItem) {
        this.contentMediaSource.updateMediaItem(mediaItem);
    }

    @Override // androidx.media3.exoplayer.source.CompositeMediaSource, androidx.media3.exoplayer.source.BaseMediaSource
    protected void prepareSourceInternal(TransferListener mediaTransferListener) {
        super.prepareSourceInternal(mediaTransferListener);
        final ComponentListener componentListener = new ComponentListener();
        this.componentListener = componentListener;
        this.contentTimeline = this.contentMediaSource.getTimeline();
        prepareChildSource(CHILD_SOURCE_MEDIA_PERIOD_ID, this.contentMediaSource);
        this.mainHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.source.ads.AdsMediaSource$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m122x9f9466de(componentListener);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$prepareSourceInternal$0$androidx-media3-exoplayer-source-ads-AdsMediaSource, reason: not valid java name */
    /* synthetic */ void m122x9f9466de(ComponentListener componentListener) {
        this.adsLoader.start(this, this.adTagDataSpec, this.adsId, this.adViewProvider, componentListener);
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator, long startPositionUs) {
        AdPlaybackState adPlaybackState = (AdPlaybackState) Assertions.checkNotNull(this.adPlaybackState);
        if (adPlaybackState.adGroupCount > 0 && id.isAd()) {
            int adGroupIndex = id.adGroupIndex;
            int adIndexInAdGroup = id.adIndexInAdGroup;
            if (this.adMediaSourceHolders[adGroupIndex].length <= adIndexInAdGroup) {
                int adCount = adIndexInAdGroup + 1;
                this.adMediaSourceHolders[adGroupIndex] = (AdMediaSourceHolder[]) Arrays.copyOf(this.adMediaSourceHolders[adGroupIndex], adCount);
            }
            AdMediaSourceHolder adMediaSourceHolder = this.adMediaSourceHolders[adGroupIndex][adIndexInAdGroup];
            if (adMediaSourceHolder == null) {
                adMediaSourceHolder = new AdMediaSourceHolder(id);
                this.adMediaSourceHolders[adGroupIndex][adIndexInAdGroup] = adMediaSourceHolder;
                maybeUpdateAdMediaSources();
            }
            return adMediaSourceHolder.createMediaPeriod(id, allocator, startPositionUs);
        }
        MaskingMediaPeriod mediaPeriod = new MaskingMediaPeriod(id, allocator, startPositionUs);
        mediaPeriod.setMediaSource(this.contentMediaSource);
        mediaPeriod.createPeriod(id);
        return mediaPeriod;
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public void releasePeriod(MediaPeriod mediaPeriod) {
        MaskingMediaPeriod maskingMediaPeriod = (MaskingMediaPeriod) mediaPeriod;
        MediaSource.MediaPeriodId id = maskingMediaPeriod.id;
        if (id.isAd()) {
            AdMediaSourceHolder adMediaSourceHolder = (AdMediaSourceHolder) Assertions.checkNotNull(this.adMediaSourceHolders[id.adGroupIndex][id.adIndexInAdGroup]);
            adMediaSourceHolder.releaseMediaPeriod(maskingMediaPeriod);
            if (adMediaSourceHolder.isInactive()) {
                adMediaSourceHolder.release();
                this.adMediaSourceHolders[id.adGroupIndex][id.adIndexInAdGroup] = null;
                return;
            }
            return;
        }
        maskingMediaPeriod.releasePeriod();
    }

    @Override // androidx.media3.exoplayer.source.CompositeMediaSource, androidx.media3.exoplayer.source.BaseMediaSource
    protected void releaseSourceInternal() {
        super.releaseSourceInternal();
        final ComponentListener componentListener = (ComponentListener) Assertions.checkNotNull(this.componentListener);
        this.componentListener = null;
        componentListener.stop();
        this.contentTimeline = null;
        this.adPlaybackState = null;
        this.adMediaSourceHolders = new AdMediaSourceHolder[0][];
        this.mainHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.source.ads.AdsMediaSource$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m123x4d6cb35f(componentListener);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$releaseSourceInternal$1$androidx-media3-exoplayer-source-ads-AdsMediaSource, reason: not valid java name */
    /* synthetic */ void m123x4d6cb35f(ComponentListener componentListener) {
        this.adsLoader.stop(this, componentListener);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.exoplayer.source.CompositeMediaSource
    /* JADX INFO: renamed from: onChildSourceInfoRefreshed, reason: avoid collision after fix types in other method and merged with bridge method [inline-methods] */
    public void m108x28f9175(MediaSource.MediaPeriodId childSourceId, MediaSource mediaSource, Timeline newTimeline) {
        if (childSourceId.isAd()) {
            int adGroupIndex = childSourceId.adGroupIndex;
            int adIndexInAdGroup = childSourceId.adIndexInAdGroup;
            ((AdMediaSourceHolder) Assertions.checkNotNull(this.adMediaSourceHolders[adGroupIndex][adIndexInAdGroup])).handleSourceInfoRefresh(newTimeline);
        } else {
            Assertions.checkArgument(newTimeline.getPeriodCount() == 1);
            this.contentTimeline = newTimeline;
        }
        maybeUpdateSourceInfo();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.exoplayer.source.CompositeMediaSource
    public MediaSource.MediaPeriodId getMediaPeriodIdForChildMediaPeriodId(MediaSource.MediaPeriodId childSourceId, MediaSource.MediaPeriodId mediaPeriodId) {
        return childSourceId.isAd() ? childSourceId : mediaPeriodId;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAdPlaybackState(AdPlaybackState adPlaybackState) {
        if (this.adPlaybackState == null) {
            this.adMediaSourceHolders = new AdMediaSourceHolder[adPlaybackState.adGroupCount][];
            Arrays.fill(this.adMediaSourceHolders, new AdMediaSourceHolder[0]);
        } else {
            Assertions.checkState(adPlaybackState.adGroupCount == this.adPlaybackState.adGroupCount);
        }
        this.adPlaybackState = adPlaybackState;
        maybeUpdateAdMediaSources();
        maybeUpdateSourceInfo();
    }

    private void maybeUpdateAdMediaSources() {
        MediaItem adMediaItem;
        AdPlaybackState adPlaybackState = this.adPlaybackState;
        if (adPlaybackState == null) {
            return;
        }
        for (int adGroupIndex = 0; adGroupIndex < this.adMediaSourceHolders.length; adGroupIndex++) {
            for (int adIndexInAdGroup = 0; adIndexInAdGroup < this.adMediaSourceHolders[adGroupIndex].length; adIndexInAdGroup++) {
                AdMediaSourceHolder adMediaSourceHolder = this.adMediaSourceHolders[adGroupIndex][adIndexInAdGroup];
                AdPlaybackState.AdGroup adGroup = adPlaybackState.getAdGroup(adGroupIndex);
                if (adMediaSourceHolder != null && !adMediaSourceHolder.hasMediaSource() && adIndexInAdGroup < adGroup.mediaItems.length && (adMediaItem = adGroup.mediaItems[adIndexInAdGroup]) != null) {
                    if (this.contentDrmConfiguration != null) {
                        adMediaItem = adMediaItem.buildUpon().setDrmConfiguration(this.contentDrmConfiguration).build();
                    }
                    MediaSource adMediaSource = this.adMediaSourceFactory.createMediaSource(adMediaItem);
                    adMediaSourceHolder.initializeWithMediaSource(adMediaSource, adMediaItem);
                }
            }
        }
    }

    private void maybeUpdateSourceInfo() {
        Timeline contentTimeline = this.contentTimeline;
        if (this.adPlaybackState != null && contentTimeline != null) {
            if (this.adPlaybackState.adGroupCount == 0) {
                refreshSourceInfo(contentTimeline);
            } else {
                this.adPlaybackState = this.adPlaybackState.withAdDurationsUs(getAdDurationsUs());
                refreshSourceInfo(new SinglePeriodAdTimeline(contentTimeline, this.adPlaybackState));
            }
        }
    }

    private long[][] getAdDurationsUs() {
        long[][] adDurationsUs = new long[this.adMediaSourceHolders.length][];
        for (int i = 0; i < this.adMediaSourceHolders.length; i++) {
            adDurationsUs[i] = new long[this.adMediaSourceHolders[i].length];
            for (int j = 0; j < this.adMediaSourceHolders[i].length; j++) {
                AdMediaSourceHolder holder = this.adMediaSourceHolders[i][j];
                adDurationsUs[i][j] = holder == null ? C.TIME_UNSET : holder.getDurationUs();
            }
        }
        return adDurationsUs;
    }

    private static MediaItem.AdsConfiguration getAdsConfiguration(MediaItem mediaItem) {
        if (mediaItem.localConfiguration == null) {
            return null;
        }
        return mediaItem.localConfiguration.adsConfiguration;
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class ComponentListener implements AdsLoader.EventListener {
        private final Handler playerHandler = Util.createHandlerForCurrentLooper();
        private volatile boolean stopped;

        @Override // androidx.media3.exoplayer.source.ads.AdsLoader.EventListener
        public /* synthetic */ void onAdClicked() {
            AdsLoader.EventListener.CC.$default$onAdClicked(this);
        }

        @Override // androidx.media3.exoplayer.source.ads.AdsLoader.EventListener
        public /* synthetic */ void onAdTapped() {
            AdsLoader.EventListener.CC.$default$onAdTapped(this);
        }

        public ComponentListener() {
        }

        public void stop() {
            this.stopped = true;
            this.playerHandler.removeCallbacksAndMessages(null);
        }

        @Override // androidx.media3.exoplayer.source.ads.AdsLoader.EventListener
        public void onAdPlaybackState(final AdPlaybackState adPlaybackState) {
            if (this.stopped) {
                return;
            }
            this.playerHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.source.ads.AdsMediaSource$ComponentListener$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m126x6396e000(adPlaybackState);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$onAdPlaybackState$0$androidx-media3-exoplayer-source-ads-AdsMediaSource$ComponentListener, reason: not valid java name */
        /* synthetic */ void m126x6396e000(AdPlaybackState adPlaybackState) {
            if (!this.stopped) {
                AdsMediaSource.this.onAdPlaybackState(adPlaybackState);
            }
        }

        @Override // androidx.media3.exoplayer.source.ads.AdsLoader.EventListener
        public void onAdLoadError(AdLoadException error, DataSpec dataSpec) {
            if (this.stopped) {
                return;
            }
            AdsMediaSource.this.createEventDispatcher(null).loadError(new LoadEventInfo(LoadEventInfo.getNewId(), dataSpec, SystemClock.elapsedRealtime()), 6, (IOException) error, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class AdPrepareListener implements MaskingMediaPeriod.PrepareListener {
        private final MediaItem adMediaItem;

        public AdPrepareListener(MediaItem adMediaItem) {
            this.adMediaItem = adMediaItem;
        }

        @Override // androidx.media3.exoplayer.source.MaskingMediaPeriod.PrepareListener
        public void onPrepareComplete(final MediaSource.MediaPeriodId mediaPeriodId) {
            AdsMediaSource.this.mainHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.source.ads.AdsMediaSource$AdPrepareListener$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m124x672fc1f4(mediaPeriodId);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$onPrepareComplete$0$androidx-media3-exoplayer-source-ads-AdsMediaSource$AdPrepareListener, reason: not valid java name */
        /* synthetic */ void m124x672fc1f4(MediaSource.MediaPeriodId mediaPeriodId) {
            AdsMediaSource.this.adsLoader.handlePrepareComplete(AdsMediaSource.this, mediaPeriodId.adGroupIndex, mediaPeriodId.adIndexInAdGroup);
        }

        @Override // androidx.media3.exoplayer.source.MaskingMediaPeriod.PrepareListener
        public void onPrepareError(final MediaSource.MediaPeriodId mediaPeriodId, final IOException exception) {
            AdsMediaSource.this.createEventDispatcher(mediaPeriodId).loadError(new LoadEventInfo(LoadEventInfo.getNewId(), new DataSpec(((MediaItem.LocalConfiguration) Assertions.checkNotNull(this.adMediaItem.localConfiguration)).uri), SystemClock.elapsedRealtime()), 6, (IOException) AdLoadException.createForAd(exception), true);
            AdsMediaSource.this.mainHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.source.ads.AdsMediaSource$AdPrepareListener$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m125xa9898f8e(mediaPeriodId, exception);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$onPrepareError$1$androidx-media3-exoplayer-source-ads-AdsMediaSource$AdPrepareListener, reason: not valid java name */
        /* synthetic */ void m125xa9898f8e(MediaSource.MediaPeriodId mediaPeriodId, IOException exception) {
            AdsMediaSource.this.adsLoader.handlePrepareError(AdsMediaSource.this, mediaPeriodId.adGroupIndex, mediaPeriodId.adIndexInAdGroup, exception);
        }
    }

    private final class AdMediaSourceHolder {
        private final List<MaskingMediaPeriod> activeMediaPeriods = new ArrayList();
        private MediaItem adMediaItem;
        private MediaSource adMediaSource;
        private final MediaSource.MediaPeriodId id;
        private Timeline timeline;

        public AdMediaSourceHolder(MediaSource.MediaPeriodId id) {
            this.id = id;
        }

        public void initializeWithMediaSource(MediaSource adMediaSource, MediaItem adMediaItem) {
            this.adMediaSource = adMediaSource;
            this.adMediaItem = adMediaItem;
            for (int i = 0; i < this.activeMediaPeriods.size(); i++) {
                MaskingMediaPeriod maskingMediaPeriod = this.activeMediaPeriods.get(i);
                maskingMediaPeriod.setMediaSource(adMediaSource);
                maskingMediaPeriod.setPrepareListener(AdsMediaSource.this.new AdPrepareListener(adMediaItem));
            }
            AdsMediaSource.this.prepareChildSource(this.id, adMediaSource);
        }

        public MediaPeriod createMediaPeriod(MediaSource.MediaPeriodId id, Allocator allocator, long startPositionUs) {
            MaskingMediaPeriod maskingMediaPeriod = new MaskingMediaPeriod(id, allocator, startPositionUs);
            this.activeMediaPeriods.add(maskingMediaPeriod);
            if (this.adMediaSource != null) {
                maskingMediaPeriod.setMediaSource(this.adMediaSource);
                maskingMediaPeriod.setPrepareListener(AdsMediaSource.this.new AdPrepareListener((MediaItem) Assertions.checkNotNull(this.adMediaItem)));
            }
            if (this.timeline != null) {
                Object periodUid = this.timeline.getUidOfPeriod(0);
                MediaSource.MediaPeriodId adSourceMediaPeriodId = new MediaSource.MediaPeriodId(periodUid, id.windowSequenceNumber);
                maskingMediaPeriod.createPeriod(adSourceMediaPeriodId);
            }
            return maskingMediaPeriod;
        }

        public void handleSourceInfoRefresh(Timeline timeline) {
            Assertions.checkArgument(timeline.getPeriodCount() == 1);
            if (this.timeline == null) {
                Object periodUid = timeline.getUidOfPeriod(0);
                for (int i = 0; i < this.activeMediaPeriods.size(); i++) {
                    MaskingMediaPeriod mediaPeriod = this.activeMediaPeriods.get(i);
                    MediaSource.MediaPeriodId adSourceMediaPeriodId = new MediaSource.MediaPeriodId(periodUid, mediaPeriod.id.windowSequenceNumber);
                    mediaPeriod.createPeriod(adSourceMediaPeriodId);
                }
            }
            this.timeline = timeline;
        }

        public long getDurationUs() {
            if (this.timeline != null) {
                return this.timeline.getPeriod(0, AdsMediaSource.this.period).getDurationUs();
            }
            return C.TIME_UNSET;
        }

        public void releaseMediaPeriod(MaskingMediaPeriod maskingMediaPeriod) {
            this.activeMediaPeriods.remove(maskingMediaPeriod);
            maskingMediaPeriod.releasePeriod();
        }

        public void release() {
            if (hasMediaSource()) {
                AdsMediaSource.this.releaseChildSource(this.id);
            }
        }

        public boolean hasMediaSource() {
            return this.adMediaSource != null;
        }

        public boolean isInactive() {
            return this.activeMediaPeriods.isEmpty();
        }
    }
}
