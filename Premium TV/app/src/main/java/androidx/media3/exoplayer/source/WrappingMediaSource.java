package androidx.media3.exoplayer.source;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Timeline;
import androidx.media3.datasource.TransferListener;
import androidx.media3.exoplayer.upstream.Allocator;

/* JADX INFO: loaded from: classes.dex */
public abstract class WrappingMediaSource extends CompositeMediaSource<Void> {
    private static final Void CHILD_SOURCE_ID = null;
    protected final MediaSource mediaSource;

    protected WrappingMediaSource(MediaSource mediaSource) {
        this.mediaSource = mediaSource;
    }

    @Override // androidx.media3.exoplayer.source.CompositeMediaSource, androidx.media3.exoplayer.source.BaseMediaSource
    protected final void prepareSourceInternal(TransferListener mediaTransferListener) {
        super.prepareSourceInternal(mediaTransferListener);
        prepareSourceInternal();
    }

    protected void prepareSourceInternal() {
        prepareChildSource();
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public Timeline getInitialTimeline() {
        return this.mediaSource.getInitialTimeline();
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public boolean isSingleWindow() {
        return this.mediaSource.isSingleWindow();
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public MediaItem getMediaItem() {
        return this.mediaSource.getMediaItem();
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public boolean canUpdateMediaItem(MediaItem mediaItem) {
        return this.mediaSource.canUpdateMediaItem(mediaItem);
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public void updateMediaItem(MediaItem mediaItem) {
        this.mediaSource.updateMediaItem(mediaItem);
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator, long startPositionUs) {
        return this.mediaSource.createPeriod(id, allocator, startPositionUs);
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public void releasePeriod(MediaPeriod mediaPeriod) {
        this.mediaSource.releasePeriod(mediaPeriod);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.exoplayer.source.CompositeMediaSource
    /* JADX INFO: renamed from: onChildSourceInfoRefreshed, reason: avoid collision after fix types in other method and merged with bridge method [inline-methods] */
    public final void m108x28f9175(Void childSourceId, MediaSource mediaSource, Timeline newTimeline) {
        onChildSourceInfoRefreshed(newTimeline);
    }

    protected void onChildSourceInfoRefreshed(Timeline newTimeline) {
        refreshSourceInfo(newTimeline);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.exoplayer.source.CompositeMediaSource
    public final int getWindowIndexForChildWindowIndex(Void childSourceId, int windowIndex) {
        return getWindowIndexForChildWindowIndex(windowIndex);
    }

    protected int getWindowIndexForChildWindowIndex(int windowIndex) {
        return windowIndex;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.exoplayer.source.CompositeMediaSource
    public final MediaSource.MediaPeriodId getMediaPeriodIdForChildMediaPeriodId(Void childSourceId, MediaSource.MediaPeriodId mediaPeriodId) {
        return getMediaPeriodIdForChildMediaPeriodId(mediaPeriodId);
    }

    protected MediaSource.MediaPeriodId getMediaPeriodIdForChildMediaPeriodId(MediaSource.MediaPeriodId mediaPeriodId) {
        return mediaPeriodId;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.exoplayer.source.CompositeMediaSource
    public final long getMediaTimeForChildMediaTime(Void childSourceId, long mediaTimeMs, MediaSource.MediaPeriodId mediaPeriodId) {
        return getMediaTimeForChildMediaTime(mediaTimeMs, mediaPeriodId);
    }

    protected long getMediaTimeForChildMediaTime(long mediaTimeMs, MediaSource.MediaPeriodId mediaPeriodId) {
        return mediaTimeMs;
    }

    protected final void prepareChildSource() {
        prepareChildSource(CHILD_SOURCE_ID, this.mediaSource);
    }

    protected final void enableChildSource() {
        enableChildSource(CHILD_SOURCE_ID);
    }

    protected final void disableChildSource() {
        disableChildSource(CHILD_SOURCE_ID);
    }

    protected final void releaseChildSource() {
        releaseChildSource(CHILD_SOURCE_ID);
    }
}
