package androidx.media3.exoplayer.source.preload;

import android.os.Handler;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.source.MediaSource;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/* JADX INFO: loaded from: classes.dex */
public abstract class BasePreloadManager<T> {
    private final MediaSource.Factory mediaSourceFactory;
    protected final Comparator<T> rankingDataComparator;
    private final TargetPreloadStatusControl<T> targetPreloadStatusControl;
    private TargetPreloadStatusControl.PreloadStatus targetPreloadStatusOfCurrentPreloadingSource;
    private final Object lock = new Object();
    private final Map<MediaItem, BasePreloadManager<T>.MediaSourceHolder> mediaItemMediaSourceHolderMap = new HashMap();
    private final Handler startPreloadingHandler = Util.createHandlerForCurrentOrMainLooper();
    private final PriorityQueue<BasePreloadManager<T>.MediaSourceHolder> sourceHolderPriorityQueue = new PriorityQueue<>();

    protected abstract void clearSourceInternal(MediaSource mediaSource);

    protected abstract void preloadSourceInternal(MediaSource mediaSource, long j);

    protected abstract void releaseSourceInternal(MediaSource mediaSource);

    protected static abstract class BuilderBase<T> {
        protected final MediaSource.Factory mediaSourceFactory;
        protected final Comparator<T> rankingDataComparator;
        protected final TargetPreloadStatusControl<T> targetPreloadStatusControl;

        public abstract BasePreloadManager<T> build();

        public BuilderBase(Comparator<T> rankingDataComparator, TargetPreloadStatusControl<T> targetPreloadStatusControl, MediaSource.Factory mediaSourceFactory) {
            this.rankingDataComparator = rankingDataComparator;
            this.targetPreloadStatusControl = targetPreloadStatusControl;
            this.mediaSourceFactory = mediaSourceFactory;
        }
    }

    protected BasePreloadManager(Comparator<T> rankingDataComparator, TargetPreloadStatusControl<T> targetPreloadStatusControl, MediaSource.Factory mediaSourceFactory) {
        this.rankingDataComparator = rankingDataComparator;
        this.targetPreloadStatusControl = targetPreloadStatusControl;
        this.mediaSourceFactory = mediaSourceFactory;
    }

    public final int getSourceCount() {
        return this.mediaItemMediaSourceHolderMap.size();
    }

    public final void add(MediaItem mediaItem, T rankingData) {
        add(this.mediaSourceFactory.createMediaSource(mediaItem), rankingData);
    }

    public final void add(MediaSource mediaSource, T rankingData) {
        MediaSource mediaSourceForPreloading = createMediaSourceForPreloading(mediaSource);
        BasePreloadManager<T>.MediaSourceHolder mediaSourceHolder = new MediaSourceHolder(this, mediaSourceForPreloading, rankingData);
        this.mediaItemMediaSourceHolderMap.put(mediaSourceForPreloading.getMediaItem(), mediaSourceHolder);
    }

    public final void invalidate() {
        synchronized (this.lock) {
            this.sourceHolderPriorityQueue.clear();
            this.sourceHolderPriorityQueue.addAll(this.mediaItemMediaSourceHolderMap.values());
            while (!this.sourceHolderPriorityQueue.isEmpty() && !maybeStartPreloadNextSource()) {
                this.sourceHolderPriorityQueue.poll();
            }
        }
    }

    public final MediaSource getMediaSource(MediaItem mediaItem) {
        if (!this.mediaItemMediaSourceHolderMap.containsKey(mediaItem)) {
            return null;
        }
        return this.mediaItemMediaSourceHolderMap.get(mediaItem).mediaSource;
    }

    public final boolean remove(MediaItem mediaItem) {
        if (this.mediaItemMediaSourceHolderMap.containsKey(mediaItem)) {
            MediaSource mediaSource = this.mediaItemMediaSourceHolderMap.get(mediaItem).mediaSource;
            this.mediaItemMediaSourceHolderMap.remove(mediaItem);
            releaseSourceInternal(mediaSource);
            return true;
        }
        return false;
    }

    public final boolean remove(MediaSource mediaSource) {
        MediaItem mediaItem = mediaSource.getMediaItem();
        if (this.mediaItemMediaSourceHolderMap.containsKey(mediaItem)) {
            MediaSource heldMediaSource = this.mediaItemMediaSourceHolderMap.get(mediaItem).mediaSource;
            if (mediaSource == heldMediaSource) {
                this.mediaItemMediaSourceHolderMap.remove(mediaItem);
                releaseSourceInternal(mediaSource);
                return true;
            }
            return false;
        }
        return false;
    }

    public final void reset() {
        for (BasePreloadManager<T>.MediaSourceHolder sourceHolder : this.mediaItemMediaSourceHolderMap.values()) {
            releaseSourceInternal(sourceHolder.mediaSource);
        }
        this.mediaItemMediaSourceHolderMap.clear();
        synchronized (this.lock) {
            this.sourceHolderPriorityQueue.clear();
            this.targetPreloadStatusOfCurrentPreloadingSource = null;
        }
    }

    public final void release() {
        reset();
        releaseInternal();
    }

    protected final void onPreloadCompleted(final MediaSource source) {
        this.startPreloadingHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.source.preload.BasePreloadManager$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m128x846d5e27(source);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$onPreloadCompleted$0$androidx-media3-exoplayer-source-preload-BasePreloadManager, reason: not valid java name */
    /* synthetic */ void m128x846d5e27(MediaSource source) {
        synchronized (this.lock) {
            if (!this.sourceHolderPriorityQueue.isEmpty() && ((MediaSourceHolder) Assertions.checkNotNull(this.sourceHolderPriorityQueue.peek())).mediaSource == source) {
                do {
                    this.sourceHolderPriorityQueue.poll();
                    if (this.sourceHolderPriorityQueue.isEmpty()) {
                        break;
                    }
                } while (!maybeStartPreloadNextSource());
            }
        }
    }

    protected final TargetPreloadStatusControl.PreloadStatus getTargetPreloadStatus(MediaSource source) {
        synchronized (this.lock) {
            if (!this.sourceHolderPriorityQueue.isEmpty() && ((MediaSourceHolder) Assertions.checkNotNull(this.sourceHolderPriorityQueue.peek())).mediaSource == source) {
                return this.targetPreloadStatusOfCurrentPreloadingSource;
            }
            return null;
        }
    }

    protected MediaSource createMediaSourceForPreloading(MediaSource mediaSource) {
        return mediaSource;
    }

    protected boolean shouldStartPreloadingNextSource() {
        return true;
    }

    protected void releaseInternal() {
    }

    private boolean maybeStartPreloadNextSource() {
        if (shouldStartPreloadingNextSource()) {
            BasePreloadManager<T>.MediaSourceHolder preloadingHolder = (MediaSourceHolder) Assertions.checkNotNull(this.sourceHolderPriorityQueue.peek());
            this.targetPreloadStatusOfCurrentPreloadingSource = this.targetPreloadStatusControl.getTargetPreloadStatus(preloadingHolder.rankingData);
            if (this.targetPreloadStatusOfCurrentPreloadingSource != null) {
                preloadSourceInternal(preloadingHolder.mediaSource, preloadingHolder.startPositionUs);
                return true;
            }
            clearSourceInternal(preloadingHolder.mediaSource);
            return false;
        }
        return false;
    }

    private final class MediaSourceHolder implements Comparable<BasePreloadManager<T>.MediaSourceHolder> {
        public final MediaSource mediaSource;
        public final T rankingData;
        public final long startPositionUs;

        public MediaSourceHolder(BasePreloadManager basePreloadManager, MediaSource mediaSource, T rankingData) {
            this(mediaSource, rankingData, C.TIME_UNSET);
        }

        public MediaSourceHolder(MediaSource mediaSource, T rankingData, long startPositionUs) {
            this.mediaSource = mediaSource;
            this.rankingData = rankingData;
            this.startPositionUs = startPositionUs;
        }

        @Override // java.lang.Comparable
        public int compareTo(BasePreloadManager<T>.MediaSourceHolder o) {
            return BasePreloadManager.this.rankingDataComparator.compare(this.rankingData, o.rankingData);
        }
    }
}
