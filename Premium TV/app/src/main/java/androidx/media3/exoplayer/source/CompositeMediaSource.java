package androidx.media3.exoplayer.source;

import android.os.Handler;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.TransferListener;
import androidx.media3.exoplayer.drm.DrmSessionEventListener;
import java.io.IOException;
import java.util.HashMap;

/* JADX INFO: loaded from: classes.dex */
public abstract class CompositeMediaSource<T> extends BaseMediaSource {
    private final HashMap<T, MediaSourceAndListener<T>> childSources = new HashMap<>();
    private Handler eventHandler;
    private TransferListener mediaTransferListener;

    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX INFO: renamed from: onChildSourceInfoRefreshed, reason: merged with bridge method [inline-methods] */
    public abstract void m108x28f9175(T t, MediaSource mediaSource, Timeline timeline);

    protected CompositeMediaSource() {
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource
    protected void prepareSourceInternal(TransferListener mediaTransferListener) {
        this.mediaTransferListener = mediaTransferListener;
        this.eventHandler = Util.createHandlerForCurrentLooper();
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public void maybeThrowSourceInfoRefreshError() throws IOException {
        for (MediaSourceAndListener<T> childSource : this.childSources.values()) {
            childSource.mediaSource.maybeThrowSourceInfoRefreshError();
        }
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource
    protected void enableInternal() {
        for (MediaSourceAndListener<T> childSource : this.childSources.values()) {
            childSource.mediaSource.enable(childSource.caller);
        }
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource
    protected void disableInternal() {
        for (MediaSourceAndListener<T> childSource : this.childSources.values()) {
            childSource.mediaSource.disable(childSource.caller);
        }
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource
    protected void releaseSourceInternal() {
        for (MediaSourceAndListener<T> childSource : this.childSources.values()) {
            childSource.mediaSource.releaseSource(childSource.caller);
            childSource.mediaSource.removeEventListener(childSource.eventListener);
            childSource.mediaSource.removeDrmEventListener(childSource.eventListener);
        }
        this.childSources.clear();
    }

    protected final void prepareChildSource(final T id, MediaSource mediaSource) {
        Assertions.checkArgument(!this.childSources.containsKey(id));
        MediaSource.MediaSourceCaller caller = new MediaSource.MediaSourceCaller() { // from class: androidx.media3.exoplayer.source.CompositeMediaSource$$ExternalSyntheticLambda0
            @Override // androidx.media3.exoplayer.source.MediaSource.MediaSourceCaller
            public final void onSourceInfoRefreshed(MediaSource mediaSource2, Timeline timeline) {
                this.f$0.m108x28f9175(id, mediaSource2, timeline);
            }
        };
        CompositeMediaSource<T>.ForwardingEventListener eventListener = new ForwardingEventListener(id);
        this.childSources.put(id, new MediaSourceAndListener<>(mediaSource, caller, eventListener));
        mediaSource.addEventListener((Handler) Assertions.checkNotNull(this.eventHandler), eventListener);
        mediaSource.addDrmEventListener((Handler) Assertions.checkNotNull(this.eventHandler), eventListener);
        mediaSource.prepareSource(caller, this.mediaTransferListener, getPlayerId());
        if (!isEnabled()) {
            mediaSource.disable(caller);
        }
    }

    protected final void enableChildSource(T id) {
        MediaSourceAndListener<T> enabledChild = (MediaSourceAndListener) Assertions.checkNotNull(this.childSources.get(id));
        enabledChild.mediaSource.enable(enabledChild.caller);
    }

    protected final void disableChildSource(T id) {
        MediaSourceAndListener<T> disabledChild = (MediaSourceAndListener) Assertions.checkNotNull(this.childSources.get(id));
        disabledChild.mediaSource.disable(disabledChild.caller);
    }

    protected final void releaseChildSource(T id) {
        MediaSourceAndListener<T> removedChild = (MediaSourceAndListener) Assertions.checkNotNull(this.childSources.remove(id));
        removedChild.mediaSource.releaseSource(removedChild.caller);
        removedChild.mediaSource.removeEventListener(removedChild.eventListener);
        removedChild.mediaSource.removeDrmEventListener(removedChild.eventListener);
    }

    protected int getWindowIndexForChildWindowIndex(T childSourceId, int windowIndex) {
        return windowIndex;
    }

    protected MediaSource.MediaPeriodId getMediaPeriodIdForChildMediaPeriodId(T childSourceId, MediaSource.MediaPeriodId mediaPeriodId) {
        return mediaPeriodId;
    }

    protected long getMediaTimeForChildMediaTime(T childSourceId, long mediaTimeMs, MediaSource.MediaPeriodId mediaPeriodId) {
        return mediaTimeMs;
    }

    private static final class MediaSourceAndListener<T> {
        public final MediaSource.MediaSourceCaller caller;
        public final CompositeMediaSource<T>.ForwardingEventListener eventListener;
        public final MediaSource mediaSource;

        public MediaSourceAndListener(MediaSource mediaSource, MediaSource.MediaSourceCaller caller, CompositeMediaSource<T>.ForwardingEventListener eventListener) {
            this.mediaSource = mediaSource;
            this.caller = caller;
            this.eventListener = eventListener;
        }
    }

    private final class ForwardingEventListener implements MediaSourceEventListener, DrmSessionEventListener {
        private DrmSessionEventListener.EventDispatcher drmEventDispatcher;
        private final T id;
        private MediaSourceEventListener.EventDispatcher mediaSourceEventDispatcher;

        @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
        public /* synthetic */ void onDrmSessionAcquired(int i, MediaSource.MediaPeriodId mediaPeriodId) {
            DrmSessionEventListener.CC.$default$onDrmSessionAcquired(this, i, mediaPeriodId);
        }

        public ForwardingEventListener(T id) {
            this.mediaSourceEventDispatcher = CompositeMediaSource.this.createEventDispatcher(null);
            this.drmEventDispatcher = CompositeMediaSource.this.createDrmEventDispatcher(null);
            this.id = id;
        }

        @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
        public void onLoadStarted(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventData, MediaLoadData mediaLoadData) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.mediaSourceEventDispatcher.loadStarted(loadEventData, maybeUpdateMediaLoadData(mediaLoadData, mediaPeriodId));
            }
        }

        @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
        public void onLoadCompleted(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventData, MediaLoadData mediaLoadData) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.mediaSourceEventDispatcher.loadCompleted(loadEventData, maybeUpdateMediaLoadData(mediaLoadData, mediaPeriodId));
            }
        }

        @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
        public void onLoadCanceled(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventData, MediaLoadData mediaLoadData) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.mediaSourceEventDispatcher.loadCanceled(loadEventData, maybeUpdateMediaLoadData(mediaLoadData, mediaPeriodId));
            }
        }

        @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
        public void onLoadError(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventData, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.mediaSourceEventDispatcher.loadError(loadEventData, maybeUpdateMediaLoadData(mediaLoadData, mediaPeriodId), error, wasCanceled);
            }
        }

        @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
        public void onUpstreamDiscarded(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.mediaSourceEventDispatcher.upstreamDiscarded(maybeUpdateMediaLoadData(mediaLoadData, mediaPeriodId));
            }
        }

        @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
        public void onDownstreamFormatChanged(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.mediaSourceEventDispatcher.downstreamFormatChanged(maybeUpdateMediaLoadData(mediaLoadData, mediaPeriodId));
            }
        }

        @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
        public void onDrmSessionAcquired(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, int state) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.drmEventDispatcher.drmSessionAcquired(state);
            }
        }

        @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
        public void onDrmKeysLoaded(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.drmEventDispatcher.drmKeysLoaded();
            }
        }

        @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
        public void onDrmSessionManagerError(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, Exception error) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.drmEventDispatcher.drmSessionManagerError(error);
            }
        }

        @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
        public void onDrmKeysRestored(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.drmEventDispatcher.drmKeysRestored();
            }
        }

        @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
        public void onDrmKeysRemoved(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.drmEventDispatcher.drmKeysRemoved();
            }
        }

        @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
        public void onDrmSessionReleased(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.drmEventDispatcher.drmSessionReleased();
            }
        }

        private boolean maybeUpdateEventDispatcher(int childWindowIndex, MediaSource.MediaPeriodId childMediaPeriodId) {
            MediaSource.MediaPeriodId mediaPeriodId = null;
            if (childMediaPeriodId != null && (mediaPeriodId = CompositeMediaSource.this.getMediaPeriodIdForChildMediaPeriodId(this.id, childMediaPeriodId)) == null) {
                return false;
            }
            int windowIndex = CompositeMediaSource.this.getWindowIndexForChildWindowIndex(this.id, childWindowIndex);
            if (this.mediaSourceEventDispatcher.windowIndex != windowIndex || !Util.areEqual(this.mediaSourceEventDispatcher.mediaPeriodId, mediaPeriodId)) {
                this.mediaSourceEventDispatcher = CompositeMediaSource.this.createEventDispatcher(windowIndex, mediaPeriodId);
            }
            if (this.drmEventDispatcher.windowIndex != windowIndex || !Util.areEqual(this.drmEventDispatcher.mediaPeriodId, mediaPeriodId)) {
                this.drmEventDispatcher = CompositeMediaSource.this.createDrmEventDispatcher(windowIndex, mediaPeriodId);
                return true;
            }
            return true;
        }

        private MediaLoadData maybeUpdateMediaLoadData(MediaLoadData mediaLoadData, MediaSource.MediaPeriodId childMediaPeriodId) {
            long mediaStartTimeMs = CompositeMediaSource.this.getMediaTimeForChildMediaTime(this.id, mediaLoadData.mediaStartTimeMs, childMediaPeriodId);
            long mediaEndTimeMs = CompositeMediaSource.this.getMediaTimeForChildMediaTime(this.id, mediaLoadData.mediaEndTimeMs, childMediaPeriodId);
            return (mediaStartTimeMs == mediaLoadData.mediaStartTimeMs && mediaEndTimeMs == mediaLoadData.mediaEndTimeMs) ? mediaLoadData : new MediaLoadData(mediaLoadData.dataType, mediaLoadData.trackType, mediaLoadData.trackFormat, mediaLoadData.trackSelectionReason, mediaLoadData.trackSelectionData, mediaStartTimeMs, mediaEndTimeMs);
        }
    }
}
