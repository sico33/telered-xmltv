package androidx.media3.exoplayer.source;

import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.datasource.TransferListener;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.drm.DrmSessionEventListener;
import java.util.ArrayList;
import java.util.HashSet;

/* JADX INFO: loaded from: classes.dex */
public abstract class BaseMediaSource implements MediaSource {
    private Looper looper;
    private PlayerId playerId;
    private Timeline timeline;
    private final ArrayList<MediaSource.MediaSourceCaller> mediaSourceCallers = new ArrayList<>(1);
    private final HashSet<MediaSource.MediaSourceCaller> enabledMediaSourceCallers = new HashSet<>(1);
    private final MediaSourceEventListener.EventDispatcher eventDispatcher = new MediaSourceEventListener.EventDispatcher();
    private final DrmSessionEventListener.EventDispatcher drmEventDispatcher = new DrmSessionEventListener.EventDispatcher();

    @Override // androidx.media3.exoplayer.source.MediaSource
    public /* synthetic */ boolean canUpdateMediaItem(MediaItem mediaItem) {
        return MediaSource.CC.$default$canUpdateMediaItem(this, mediaItem);
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public /* synthetic */ Timeline getInitialTimeline() {
        return MediaSource.CC.$default$getInitialTimeline(this);
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public /* synthetic */ boolean isSingleWindow() {
        return MediaSource.CC.$default$isSingleWindow(this);
    }

    protected abstract void prepareSourceInternal(TransferListener transferListener);

    protected abstract void releaseSourceInternal();

    @Override // androidx.media3.exoplayer.source.MediaSource
    public /* synthetic */ void updateMediaItem(MediaItem mediaItem) {
        MediaSource.CC.$default$updateMediaItem(this, mediaItem);
    }

    protected void enableInternal() {
    }

    protected void disableInternal() {
    }

    protected final void refreshSourceInfo(Timeline timeline) {
        this.timeline = timeline;
        for (MediaSource.MediaSourceCaller caller : this.mediaSourceCallers) {
            caller.onSourceInfoRefreshed(this, timeline);
        }
    }

    protected final MediaSourceEventListener.EventDispatcher createEventDispatcher(MediaSource.MediaPeriodId mediaPeriodId) {
        return this.eventDispatcher.withParameters(0, mediaPeriodId);
    }

    protected final MediaSourceEventListener.EventDispatcher createEventDispatcher(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        return this.eventDispatcher.withParameters(windowIndex, mediaPeriodId);
    }

    @Deprecated
    protected final MediaSourceEventListener.EventDispatcher createEventDispatcher(MediaSource.MediaPeriodId mediaPeriodId, long mediaTimeOffsetMs) {
        Assertions.checkNotNull(mediaPeriodId);
        return this.eventDispatcher.withParameters(0, mediaPeriodId);
    }

    @Deprecated
    protected final MediaSourceEventListener.EventDispatcher createEventDispatcher(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, long mediaTimeOffsetMs) {
        return this.eventDispatcher.withParameters(windowIndex, mediaPeriodId);
    }

    protected final DrmSessionEventListener.EventDispatcher createDrmEventDispatcher(MediaSource.MediaPeriodId mediaPeriodId) {
        return this.drmEventDispatcher.withParameters(0, mediaPeriodId);
    }

    protected final DrmSessionEventListener.EventDispatcher createDrmEventDispatcher(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        return this.drmEventDispatcher.withParameters(windowIndex, mediaPeriodId);
    }

    protected final boolean isEnabled() {
        return !this.enabledMediaSourceCallers.isEmpty();
    }

    protected final PlayerId getPlayerId() {
        return (PlayerId) Assertions.checkStateNotNull(this.playerId);
    }

    protected final void setPlayerId(PlayerId playerId) {
        this.playerId = playerId;
    }

    protected final boolean prepareSourceCalled() {
        return !this.mediaSourceCallers.isEmpty();
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public final void addEventListener(Handler handler, MediaSourceEventListener eventListener) {
        Assertions.checkNotNull(handler);
        Assertions.checkNotNull(eventListener);
        this.eventDispatcher.addEventListener(handler, eventListener);
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public final void removeEventListener(MediaSourceEventListener eventListener) {
        this.eventDispatcher.removeEventListener(eventListener);
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public final void addDrmEventListener(Handler handler, DrmSessionEventListener eventListener) {
        Assertions.checkNotNull(handler);
        Assertions.checkNotNull(eventListener);
        this.drmEventDispatcher.addEventListener(handler, eventListener);
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public final void removeDrmEventListener(DrmSessionEventListener eventListener) {
        this.drmEventDispatcher.removeEventListener(eventListener);
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public final void prepareSource(MediaSource.MediaSourceCaller caller, TransferListener mediaTransferListener) {
        prepareSource(caller, mediaTransferListener, PlayerId.UNSET);
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public final void prepareSource(MediaSource.MediaSourceCaller caller, TransferListener mediaTransferListener, PlayerId playerId) {
        Looper looper = Looper.myLooper();
        Assertions.checkArgument(this.looper == null || this.looper == looper);
        this.playerId = playerId;
        Timeline timeline = this.timeline;
        this.mediaSourceCallers.add(caller);
        if (this.looper == null) {
            this.looper = looper;
            this.enabledMediaSourceCallers.add(caller);
            prepareSourceInternal(mediaTransferListener);
        } else if (timeline != null) {
            enable(caller);
            caller.onSourceInfoRefreshed(this, timeline);
        }
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public final void enable(MediaSource.MediaSourceCaller caller) {
        Assertions.checkNotNull(this.looper);
        boolean wasDisabled = this.enabledMediaSourceCallers.isEmpty();
        this.enabledMediaSourceCallers.add(caller);
        if (wasDisabled) {
            enableInternal();
        }
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public final void disable(MediaSource.MediaSourceCaller caller) {
        boolean wasEnabled = !this.enabledMediaSourceCallers.isEmpty();
        this.enabledMediaSourceCallers.remove(caller);
        if (wasEnabled && this.enabledMediaSourceCallers.isEmpty()) {
            disableInternal();
        }
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public final void releaseSource(MediaSource.MediaSourceCaller caller) {
        this.mediaSourceCallers.remove(caller);
        if (this.mediaSourceCallers.isEmpty()) {
            this.looper = null;
            this.timeline = null;
            this.playerId = null;
            this.enabledMediaSourceCallers.clear();
            releaseSourceInternal();
            return;
        }
        disable(caller);
    }
}
