package androidx.media3.exoplayer;

import android.util.Pair;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.HandlerWrapper;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.TransferListener;
import androidx.media3.exoplayer.analytics.AnalyticsCollector;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.drm.DrmSessionEventListener;
import androidx.media3.exoplayer.source.LoadEventInfo;
import androidx.media3.exoplayer.source.MaskingMediaPeriod;
import androidx.media3.exoplayer.source.MaskingMediaSource;
import androidx.media3.exoplayer.source.MediaLoadData;
import androidx.media3.exoplayer.source.MediaPeriod;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.MediaSourceEventListener;
import androidx.media3.exoplayer.source.ShuffleOrder;
import androidx.media3.exoplayer.upstream.Allocator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
final class MediaSourceList {
    private static final String TAG = "MediaSourceList";
    private final HandlerWrapper eventHandler;
    private final AnalyticsCollector eventListener;
    private boolean isPrepared;
    private final MediaSourceListInfoRefreshListener mediaSourceListInfoListener;
    private TransferListener mediaTransferListener;
    private final PlayerId playerId;
    private ShuffleOrder shuffleOrder = new ShuffleOrder.DefaultShuffleOrder(0);
    private final IdentityHashMap<MediaPeriod, MediaSourceHolder> mediaSourceByMediaPeriod = new IdentityHashMap<>();
    private final Map<Object, MediaSourceHolder> mediaSourceByUid = new HashMap();
    private final List<MediaSourceHolder> mediaSourceHolders = new ArrayList();
    private final HashMap<MediaSourceHolder, MediaSourceAndListener> childSources = new HashMap<>();
    private final Set<MediaSourceHolder> enabledMediaSourceHolders = new HashSet();

    public interface MediaSourceListInfoRefreshListener {
        void onPlaylistUpdateRequested();
    }

    public MediaSourceList(MediaSourceListInfoRefreshListener listener, AnalyticsCollector analyticsCollector, HandlerWrapper analyticsCollectorHandler, PlayerId playerId) {
        this.playerId = playerId;
        this.mediaSourceListInfoListener = listener;
        this.eventListener = analyticsCollector;
        this.eventHandler = analyticsCollectorHandler;
    }

    public Timeline setMediaSources(List<MediaSourceHolder> holders, ShuffleOrder shuffleOrder) {
        removeMediaSourcesInternal(0, this.mediaSourceHolders.size());
        return addMediaSources(this.mediaSourceHolders.size(), holders, shuffleOrder);
    }

    public Timeline addMediaSources(int index, List<MediaSourceHolder> holders, ShuffleOrder shuffleOrder) {
        if (!holders.isEmpty()) {
            this.shuffleOrder = shuffleOrder;
            for (int insertionIndex = index; insertionIndex < holders.size() + index; insertionIndex++) {
                MediaSourceHolder holder = holders.get(insertionIndex - index);
                if (insertionIndex > 0) {
                    MediaSourceHolder previousHolder = this.mediaSourceHolders.get(insertionIndex - 1);
                    Timeline previousTimeline = previousHolder.mediaSource.getTimeline();
                    holder.reset(previousHolder.firstWindowIndexInChild + previousTimeline.getWindowCount());
                } else {
                    holder.reset(0);
                }
                Timeline newTimeline = holder.mediaSource.getTimeline();
                correctOffsets(insertionIndex, newTimeline.getWindowCount());
                this.mediaSourceHolders.add(insertionIndex, holder);
                this.mediaSourceByUid.put(holder.uid, holder);
                if (this.isPrepared) {
                    prepareChildSource(holder);
                    if (this.mediaSourceByMediaPeriod.isEmpty()) {
                        this.enabledMediaSourceHolders.add(holder);
                    } else {
                        disableChildSource(holder);
                    }
                }
            }
        }
        return createTimeline();
    }

    public Timeline removeMediaSourceRange(int fromIndex, int toIndex, ShuffleOrder shuffleOrder) {
        Assertions.checkArgument(fromIndex >= 0 && fromIndex <= toIndex && toIndex <= getSize());
        this.shuffleOrder = shuffleOrder;
        removeMediaSourcesInternal(fromIndex, toIndex);
        return createTimeline();
    }

    public Timeline moveMediaSource(int currentIndex, int newIndex, ShuffleOrder shuffleOrder) {
        return moveMediaSourceRange(currentIndex, currentIndex + 1, newIndex, shuffleOrder);
    }

    public Timeline moveMediaSourceRange(int fromIndex, int toIndex, int newFromIndex, ShuffleOrder shuffleOrder) {
        Assertions.checkArgument(fromIndex >= 0 && fromIndex <= toIndex && toIndex <= getSize() && newFromIndex >= 0);
        this.shuffleOrder = shuffleOrder;
        if (fromIndex == toIndex || fromIndex == newFromIndex) {
            return createTimeline();
        }
        int startIndex = Math.min(fromIndex, newFromIndex);
        int newEndIndex = ((toIndex - fromIndex) + newFromIndex) - 1;
        int endIndex = Math.max(newEndIndex, toIndex - 1);
        int windowOffset = this.mediaSourceHolders.get(startIndex).firstWindowIndexInChild;
        Util.moveItems(this.mediaSourceHolders, fromIndex, toIndex, newFromIndex);
        for (int i = startIndex; i <= endIndex; i++) {
            MediaSourceHolder holder = this.mediaSourceHolders.get(i);
            holder.firstWindowIndexInChild = windowOffset;
            windowOffset += holder.mediaSource.getTimeline().getWindowCount();
        }
        return createTimeline();
    }

    public Timeline updateMediaSourcesWithMediaItems(int fromIndex, int toIndex, List<MediaItem> mediaItems) {
        Assertions.checkArgument(fromIndex >= 0 && fromIndex <= toIndex && toIndex <= getSize());
        Assertions.checkArgument(mediaItems.size() == toIndex - fromIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            this.mediaSourceHolders.get(i).mediaSource.updateMediaItem(mediaItems.get(i - fromIndex));
        }
        return createTimeline();
    }

    public Timeline clear(ShuffleOrder shuffleOrder) {
        this.shuffleOrder = shuffleOrder != null ? shuffleOrder : this.shuffleOrder.cloneAndClear();
        removeMediaSourcesInternal(0, getSize());
        return createTimeline();
    }

    public boolean isPrepared() {
        return this.isPrepared;
    }

    public int getSize() {
        return this.mediaSourceHolders.size();
    }

    public Timeline setShuffleOrder(ShuffleOrder shuffleOrder) {
        int size = getSize();
        if (shuffleOrder.getLength() != size) {
            shuffleOrder = shuffleOrder.cloneAndClear().cloneAndInsert(0, size);
        }
        this.shuffleOrder = shuffleOrder;
        return createTimeline();
    }

    public void prepare(TransferListener mediaTransferListener) {
        Assertions.checkState(!this.isPrepared);
        this.mediaTransferListener = mediaTransferListener;
        for (int i = 0; i < this.mediaSourceHolders.size(); i++) {
            MediaSourceHolder mediaSourceHolder = this.mediaSourceHolders.get(i);
            prepareChildSource(mediaSourceHolder);
            this.enabledMediaSourceHolders.add(mediaSourceHolder);
        }
        this.isPrepared = true;
    }

    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator, long startPositionUs) {
        Object mediaSourceHolderUid = getMediaSourceHolderUid(id.periodUid);
        MediaSource.MediaPeriodId childMediaPeriodId = id.copyWithPeriodUid(getChildPeriodUid(id.periodUid));
        MediaSourceHolder holder = (MediaSourceHolder) Assertions.checkNotNull(this.mediaSourceByUid.get(mediaSourceHolderUid));
        enableMediaSource(holder);
        holder.activeMediaPeriodIds.add(childMediaPeriodId);
        MediaPeriod mediaPeriod = holder.mediaSource.createPeriod(childMediaPeriodId, allocator, startPositionUs);
        this.mediaSourceByMediaPeriod.put(mediaPeriod, holder);
        disableUnusedMediaSources();
        return mediaPeriod;
    }

    public void releasePeriod(MediaPeriod mediaPeriod) {
        MediaSourceHolder holder = (MediaSourceHolder) Assertions.checkNotNull(this.mediaSourceByMediaPeriod.remove(mediaPeriod));
        holder.mediaSource.releasePeriod(mediaPeriod);
        holder.activeMediaPeriodIds.remove(((MaskingMediaPeriod) mediaPeriod).id);
        if (!this.mediaSourceByMediaPeriod.isEmpty()) {
            disableUnusedMediaSources();
        }
        maybeReleaseChildSource(holder);
    }

    public void release() {
        for (MediaSourceAndListener childSource : this.childSources.values()) {
            try {
                childSource.mediaSource.releaseSource(childSource.caller);
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to release child source.", e);
            }
            childSource.mediaSource.removeEventListener(childSource.eventListener);
            childSource.mediaSource.removeDrmEventListener(childSource.eventListener);
        }
        this.childSources.clear();
        this.enabledMediaSourceHolders.clear();
        this.isPrepared = false;
    }

    public Timeline createTimeline() {
        if (this.mediaSourceHolders.isEmpty()) {
            return Timeline.EMPTY;
        }
        int windowOffset = 0;
        for (int i = 0; i < this.mediaSourceHolders.size(); i++) {
            MediaSourceHolder mediaSourceHolder = this.mediaSourceHolders.get(i);
            mediaSourceHolder.firstWindowIndexInChild = windowOffset;
            windowOffset += mediaSourceHolder.mediaSource.getTimeline().getWindowCount();
        }
        return new PlaylistTimeline(this.mediaSourceHolders, this.shuffleOrder);
    }

    public ShuffleOrder getShuffleOrder() {
        return this.shuffleOrder;
    }

    private void enableMediaSource(MediaSourceHolder mediaSourceHolder) {
        this.enabledMediaSourceHolders.add(mediaSourceHolder);
        MediaSourceAndListener enabledChild = this.childSources.get(mediaSourceHolder);
        if (enabledChild != null) {
            enabledChild.mediaSource.enable(enabledChild.caller);
        }
    }

    private void disableUnusedMediaSources() {
        Iterator<MediaSourceHolder> iterator = this.enabledMediaSourceHolders.iterator();
        while (iterator.hasNext()) {
            MediaSourceHolder holder = iterator.next();
            if (holder.activeMediaPeriodIds.isEmpty()) {
                disableChildSource(holder);
                iterator.remove();
            }
        }
    }

    private void disableChildSource(MediaSourceHolder holder) {
        MediaSourceAndListener disabledChild = this.childSources.get(holder);
        if (disabledChild != null) {
            disabledChild.mediaSource.disable(disabledChild.caller);
        }
    }

    private void removeMediaSourcesInternal(int fromIndex, int toIndex) {
        for (int index = toIndex - 1; index >= fromIndex; index--) {
            MediaSourceHolder holder = this.mediaSourceHolders.remove(index);
            this.mediaSourceByUid.remove(holder.uid);
            Timeline oldTimeline = holder.mediaSource.getTimeline();
            correctOffsets(index, -oldTimeline.getWindowCount());
            holder.isRemoved = true;
            if (this.isPrepared) {
                maybeReleaseChildSource(holder);
            }
        }
    }

    private void correctOffsets(int startIndex, int windowOffsetUpdate) {
        for (int i = startIndex; i < this.mediaSourceHolders.size(); i++) {
            MediaSourceHolder mediaSourceHolder = this.mediaSourceHolders.get(i);
            mediaSourceHolder.firstWindowIndexInChild += windowOffsetUpdate;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static MediaSource.MediaPeriodId getMediaPeriodIdForChildMediaPeriodId(MediaSourceHolder mediaSourceHolder, MediaSource.MediaPeriodId mediaPeriodId) {
        for (int i = 0; i < mediaSourceHolder.activeMediaPeriodIds.size(); i++) {
            if (mediaSourceHolder.activeMediaPeriodIds.get(i).windowSequenceNumber == mediaPeriodId.windowSequenceNumber) {
                Object periodUid = getPeriodUid(mediaSourceHolder, mediaPeriodId.periodUid);
                return mediaPeriodId.copyWithPeriodUid(periodUid);
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int getWindowIndexForChildWindowIndex(MediaSourceHolder mediaSourceHolder, int windowIndex) {
        return mediaSourceHolder.firstWindowIndexInChild + windowIndex;
    }

    private void prepareChildSource(MediaSourceHolder holder) {
        MediaSource mediaSource = holder.mediaSource;
        MediaSource.MediaSourceCaller caller = new MediaSource.MediaSourceCaller() { // from class: androidx.media3.exoplayer.MediaSourceList$$ExternalSyntheticLambda0
            @Override // androidx.media3.exoplayer.source.MediaSource.MediaSourceCaller
            public final void onSourceInfoRefreshed(MediaSource mediaSource2, Timeline timeline) {
                this.f$0.m50x10419188(mediaSource2, timeline);
            }
        };
        ForwardingEventListener eventListener = new ForwardingEventListener(holder);
        this.childSources.put(holder, new MediaSourceAndListener(mediaSource, caller, eventListener));
        mediaSource.addEventListener(Util.createHandlerForCurrentOrMainLooper(), eventListener);
        mediaSource.addDrmEventListener(Util.createHandlerForCurrentOrMainLooper(), eventListener);
        mediaSource.prepareSource(caller, this.mediaTransferListener, this.playerId);
    }

    /* JADX INFO: renamed from: lambda$prepareChildSource$0$androidx-media3-exoplayer-MediaSourceList, reason: not valid java name */
    /* synthetic */ void m50x10419188(MediaSource source, Timeline timeline) {
        this.mediaSourceListInfoListener.onPlaylistUpdateRequested();
    }

    private void maybeReleaseChildSource(MediaSourceHolder mediaSourceHolder) {
        if (mediaSourceHolder.isRemoved && mediaSourceHolder.activeMediaPeriodIds.isEmpty()) {
            MediaSourceAndListener removedChild = (MediaSourceAndListener) Assertions.checkNotNull(this.childSources.remove(mediaSourceHolder));
            removedChild.mediaSource.releaseSource(removedChild.caller);
            removedChild.mediaSource.removeEventListener(removedChild.eventListener);
            removedChild.mediaSource.removeDrmEventListener(removedChild.eventListener);
            this.enabledMediaSourceHolders.remove(mediaSourceHolder);
        }
    }

    private static Object getMediaSourceHolderUid(Object periodUid) {
        return PlaylistTimeline.getChildTimelineUidFromConcatenatedUid(periodUid);
    }

    private static Object getChildPeriodUid(Object periodUid) {
        return PlaylistTimeline.getChildPeriodUidFromConcatenatedUid(periodUid);
    }

    private static Object getPeriodUid(MediaSourceHolder holder, Object childPeriodUid) {
        return PlaylistTimeline.getConcatenatedUid(holder.uid, childPeriodUid);
    }

    static final class MediaSourceHolder implements MediaSourceInfoHolder {
        public int firstWindowIndexInChild;
        public boolean isRemoved;
        public final MaskingMediaSource mediaSource;
        public final List<MediaSource.MediaPeriodId> activeMediaPeriodIds = new ArrayList();
        public final Object uid = new Object();

        public MediaSourceHolder(MediaSource mediaSource, boolean useLazyPreparation) {
            this.mediaSource = new MaskingMediaSource(mediaSource, useLazyPreparation);
        }

        public void reset(int firstWindowIndexInChild) {
            this.firstWindowIndexInChild = firstWindowIndexInChild;
            this.isRemoved = false;
            this.activeMediaPeriodIds.clear();
        }

        @Override // androidx.media3.exoplayer.MediaSourceInfoHolder
        public Object getUid() {
            return this.uid;
        }

        @Override // androidx.media3.exoplayer.MediaSourceInfoHolder
        public Timeline getTimeline() {
            return this.mediaSource.getTimeline();
        }
    }

    private static final class MediaSourceAndListener {
        public final MediaSource.MediaSourceCaller caller;
        public final ForwardingEventListener eventListener;
        public final MediaSource mediaSource;

        public MediaSourceAndListener(MediaSource mediaSource, MediaSource.MediaSourceCaller caller, ForwardingEventListener eventListener) {
            this.mediaSource = mediaSource;
            this.caller = caller;
            this.eventListener = eventListener;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class ForwardingEventListener implements MediaSourceEventListener, DrmSessionEventListener {
        private final MediaSourceHolder id;

        @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
        public /* synthetic */ void onDrmSessionAcquired(int i, MediaSource.MediaPeriodId mediaPeriodId) {
            DrmSessionEventListener.CC.$default$onDrmSessionAcquired(this, i, mediaPeriodId);
        }

        public ForwardingEventListener(MediaSourceHolder id) {
            this.id = id;
        }

        @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
        public void onLoadStarted(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventData, final MediaLoadData mediaLoadData) {
            final Pair<Integer, MediaSource.MediaPeriodId> eventParameters = getEventParameters(windowIndex, mediaPeriodId);
            if (eventParameters != null) {
                MediaSourceList.this.eventHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.MediaSourceList$ForwardingEventListener$$ExternalSyntheticLambda4
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m61x11454fa7(eventParameters, loadEventData, mediaLoadData);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$onLoadStarted$0$androidx-media3-exoplayer-MediaSourceList$ForwardingEventListener, reason: not valid java name */
        /* synthetic */ void m61x11454fa7(Pair eventParameters, LoadEventInfo loadEventData, MediaLoadData mediaLoadData) {
            MediaSourceList.this.eventListener.onLoadStarted(((Integer) eventParameters.first).intValue(), (MediaSource.MediaPeriodId) eventParameters.second, loadEventData, mediaLoadData);
        }

        @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
        public void onLoadCompleted(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventData, final MediaLoadData mediaLoadData) {
            final Pair<Integer, MediaSource.MediaPeriodId> eventParameters = getEventParameters(windowIndex, mediaPeriodId);
            if (eventParameters != null) {
                MediaSourceList.this.eventHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.MediaSourceList$ForwardingEventListener$$ExternalSyntheticLambda8
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m59x562f44b2(eventParameters, loadEventData, mediaLoadData);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$onLoadCompleted$1$androidx-media3-exoplayer-MediaSourceList$ForwardingEventListener, reason: not valid java name */
        /* synthetic */ void m59x562f44b2(Pair eventParameters, LoadEventInfo loadEventData, MediaLoadData mediaLoadData) {
            MediaSourceList.this.eventListener.onLoadCompleted(((Integer) eventParameters.first).intValue(), (MediaSource.MediaPeriodId) eventParameters.second, loadEventData, mediaLoadData);
        }

        @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
        public void onLoadCanceled(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventData, final MediaLoadData mediaLoadData) {
            final Pair<Integer, MediaSource.MediaPeriodId> eventParameters = getEventParameters(windowIndex, mediaPeriodId);
            if (eventParameters != null) {
                MediaSourceList.this.eventHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.MediaSourceList$ForwardingEventListener$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m58x36d40f85(eventParameters, loadEventData, mediaLoadData);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$onLoadCanceled$2$androidx-media3-exoplayer-MediaSourceList$ForwardingEventListener, reason: not valid java name */
        /* synthetic */ void m58x36d40f85(Pair eventParameters, LoadEventInfo loadEventData, MediaLoadData mediaLoadData) {
            MediaSourceList.this.eventListener.onLoadCanceled(((Integer) eventParameters.first).intValue(), (MediaSource.MediaPeriodId) eventParameters.second, loadEventData, mediaLoadData);
        }

        @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
        public void onLoadError(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventData, final MediaLoadData mediaLoadData, final IOException error, final boolean wasCanceled) {
            final Pair<Integer, MediaSource.MediaPeriodId> eventParameters = getEventParameters(windowIndex, mediaPeriodId);
            if (eventParameters != null) {
                MediaSourceList.this.eventHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.MediaSourceList$ForwardingEventListener$$ExternalSyntheticLambda11
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m60x67e35871(eventParameters, loadEventData, mediaLoadData, error, wasCanceled);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$onLoadError$3$androidx-media3-exoplayer-MediaSourceList$ForwardingEventListener, reason: not valid java name */
        /* synthetic */ void m60x67e35871(Pair eventParameters, LoadEventInfo loadEventData, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
            MediaSourceList.this.eventListener.onLoadError(((Integer) eventParameters.first).intValue(), (MediaSource.MediaPeriodId) eventParameters.second, loadEventData, mediaLoadData, error, wasCanceled);
        }

        @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
        public void onUpstreamDiscarded(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, final MediaLoadData mediaLoadData) {
            final Pair<Integer, MediaSource.MediaPeriodId> eventParameters = getEventParameters(windowIndex, mediaPeriodId);
            if (eventParameters != null) {
                MediaSourceList.this.eventHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.MediaSourceList$ForwardingEventListener$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m62xcc5b5192(eventParameters, mediaLoadData);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$onUpstreamDiscarded$4$androidx-media3-exoplayer-MediaSourceList$ForwardingEventListener, reason: not valid java name */
        /* synthetic */ void m62xcc5b5192(Pair eventParameters, MediaLoadData mediaLoadData) {
            MediaSourceList.this.eventListener.onUpstreamDiscarded(((Integer) eventParameters.first).intValue(), (MediaSource.MediaPeriodId) Assertions.checkNotNull((MediaSource.MediaPeriodId) eventParameters.second), mediaLoadData);
        }

        @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
        public void onDownstreamFormatChanged(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, final MediaLoadData mediaLoadData) {
            final Pair<Integer, MediaSource.MediaPeriodId> eventParameters = getEventParameters(windowIndex, mediaPeriodId);
            if (eventParameters != null) {
                MediaSourceList.this.eventHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.MediaSourceList$ForwardingEventListener$$ExternalSyntheticLambda5
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m51xf34172ec(eventParameters, mediaLoadData);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$onDownstreamFormatChanged$5$androidx-media3-exoplayer-MediaSourceList$ForwardingEventListener, reason: not valid java name */
        /* synthetic */ void m51xf34172ec(Pair eventParameters, MediaLoadData mediaLoadData) {
            MediaSourceList.this.eventListener.onDownstreamFormatChanged(((Integer) eventParameters.first).intValue(), (MediaSource.MediaPeriodId) eventParameters.second, mediaLoadData);
        }

        @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
        public void onDrmSessionAcquired(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, final int state) {
            final Pair<Integer, MediaSource.MediaPeriodId> eventParameters = getEventParameters(windowIndex, mediaPeriodId);
            if (eventParameters != null) {
                MediaSourceList.this.eventHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.MediaSourceList$ForwardingEventListener$$ExternalSyntheticLambda9
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m55xe036324f(eventParameters, state);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$onDrmSessionAcquired$6$androidx-media3-exoplayer-MediaSourceList$ForwardingEventListener, reason: not valid java name */
        /* synthetic */ void m55xe036324f(Pair eventParameters, int state) {
            MediaSourceList.this.eventListener.onDrmSessionAcquired(((Integer) eventParameters.first).intValue(), (MediaSource.MediaPeriodId) eventParameters.second, state);
        }

        @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
        public void onDrmKeysLoaded(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
            final Pair<Integer, MediaSource.MediaPeriodId> eventParameters = getEventParameters(windowIndex, mediaPeriodId);
            if (eventParameters != null) {
                MediaSourceList.this.eventHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.MediaSourceList$ForwardingEventListener$$ExternalSyntheticLambda10
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m52x3f5587cb(eventParameters);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$onDrmKeysLoaded$7$androidx-media3-exoplayer-MediaSourceList$ForwardingEventListener, reason: not valid java name */
        /* synthetic */ void m52x3f5587cb(Pair eventParameters) {
            MediaSourceList.this.eventListener.onDrmKeysLoaded(((Integer) eventParameters.first).intValue(), (MediaSource.MediaPeriodId) eventParameters.second);
        }

        @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
        public void onDrmSessionManagerError(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, final Exception error) {
            final Pair<Integer, MediaSource.MediaPeriodId> eventParameters = getEventParameters(windowIndex, mediaPeriodId);
            if (eventParameters != null) {
                MediaSourceList.this.eventHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.MediaSourceList$ForwardingEventListener$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m56x6070cdde(eventParameters, error);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$onDrmSessionManagerError$8$androidx-media3-exoplayer-MediaSourceList$ForwardingEventListener, reason: not valid java name */
        /* synthetic */ void m56x6070cdde(Pair eventParameters, Exception error) {
            MediaSourceList.this.eventListener.onDrmSessionManagerError(((Integer) eventParameters.first).intValue(), (MediaSource.MediaPeriodId) eventParameters.second, error);
        }

        @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
        public void onDrmKeysRestored(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
            final Pair<Integer, MediaSource.MediaPeriodId> eventParameters = getEventParameters(windowIndex, mediaPeriodId);
            if (eventParameters != null) {
                MediaSourceList.this.eventHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.MediaSourceList$ForwardingEventListener$$ExternalSyntheticLambda6
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m54x356ec9e(eventParameters);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$onDrmKeysRestored$9$androidx-media3-exoplayer-MediaSourceList$ForwardingEventListener, reason: not valid java name */
        /* synthetic */ void m54x356ec9e(Pair eventParameters) {
            MediaSourceList.this.eventListener.onDrmKeysRestored(((Integer) eventParameters.first).intValue(), (MediaSource.MediaPeriodId) eventParameters.second);
        }

        @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
        public void onDrmKeysRemoved(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
            final Pair<Integer, MediaSource.MediaPeriodId> eventParameters = getEventParameters(windowIndex, mediaPeriodId);
            if (eventParameters != null) {
                MediaSourceList.this.eventHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.MediaSourceList$ForwardingEventListener$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m53x32cca0d6(eventParameters);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$onDrmKeysRemoved$10$androidx-media3-exoplayer-MediaSourceList$ForwardingEventListener, reason: not valid java name */
        /* synthetic */ void m53x32cca0d6(Pair eventParameters) {
            MediaSourceList.this.eventListener.onDrmKeysRemoved(((Integer) eventParameters.first).intValue(), (MediaSource.MediaPeriodId) eventParameters.second);
        }

        @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
        public void onDrmSessionReleased(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
            final Pair<Integer, MediaSource.MediaPeriodId> eventParameters = getEventParameters(windowIndex, mediaPeriodId);
            if (eventParameters != null) {
                MediaSourceList.this.eventHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.MediaSourceList$ForwardingEventListener$$ExternalSyntheticLambda7
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m57xbda1950(eventParameters);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$onDrmSessionReleased$11$androidx-media3-exoplayer-MediaSourceList$ForwardingEventListener, reason: not valid java name */
        /* synthetic */ void m57xbda1950(Pair eventParameters) {
            MediaSourceList.this.eventListener.onDrmSessionReleased(((Integer) eventParameters.first).intValue(), (MediaSource.MediaPeriodId) eventParameters.second);
        }

        private Pair<Integer, MediaSource.MediaPeriodId> getEventParameters(int childWindowIndex, MediaSource.MediaPeriodId childMediaPeriodId) {
            MediaSource.MediaPeriodId mediaPeriodId = null;
            if (childMediaPeriodId == null || (mediaPeriodId = MediaSourceList.getMediaPeriodIdForChildMediaPeriodId(this.id, childMediaPeriodId)) != null) {
                int windowIndex = MediaSourceList.getWindowIndexForChildWindowIndex(this.id, childWindowIndex);
                return Pair.create(Integer.valueOf(windowIndex), mediaPeriodId);
            }
            return null;
        }
    }
}
