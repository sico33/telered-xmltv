package androidx.media3.exoplayer;

import androidx.media3.common.C;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.exoplayer.source.ClippingMediaPeriod;
import androidx.media3.exoplayer.source.EmptySampleStream;
import androidx.media3.exoplayer.source.MediaPeriod;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.SampleStream;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.trackselection.TrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelectorResult;
import androidx.media3.exoplayer.upstream.Allocator;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
final class MediaPeriodHolder {
    private static final String TAG = "MediaPeriodHolder";
    public boolean allRenderersInCorrectState;
    public boolean hasEnabledTracks;
    public MediaPeriodInfo info;
    private final boolean[] mayRetainStreamFlags;
    public final MediaPeriod mediaPeriod;
    private final MediaSourceList mediaSourceList;
    private MediaPeriodHolder next;
    public boolean prepared;
    private final RendererCapabilities[] rendererCapabilities;
    private long rendererPositionOffsetUs;
    public final SampleStream[] sampleStreams;
    private TrackGroupArray trackGroups = TrackGroupArray.EMPTY;
    private final TrackSelector trackSelector;
    private TrackSelectorResult trackSelectorResult;
    public final Object uid;

    interface Factory {
        MediaPeriodHolder create(MediaPeriodInfo mediaPeriodInfo, long j);
    }

    public MediaPeriodHolder(RendererCapabilities[] rendererCapabilities, long rendererPositionOffsetUs, TrackSelector trackSelector, Allocator allocator, MediaSourceList mediaSourceList, MediaPeriodInfo info, TrackSelectorResult emptyTrackSelectorResult) {
        this.rendererCapabilities = rendererCapabilities;
        this.rendererPositionOffsetUs = rendererPositionOffsetUs;
        this.trackSelector = trackSelector;
        this.mediaSourceList = mediaSourceList;
        this.uid = info.id.periodUid;
        this.info = info;
        this.trackSelectorResult = emptyTrackSelectorResult;
        this.sampleStreams = new SampleStream[rendererCapabilities.length];
        this.mayRetainStreamFlags = new boolean[rendererCapabilities.length];
        this.mediaPeriod = createMediaPeriod(info.id, mediaSourceList, allocator, info.startPositionUs, info.endPositionUs);
    }

    public long toRendererTime(long periodTimeUs) {
        return getRendererOffset() + periodTimeUs;
    }

    public long toPeriodTime(long rendererTimeUs) {
        return rendererTimeUs - getRendererOffset();
    }

    public long getRendererOffset() {
        return this.rendererPositionOffsetUs;
    }

    public void setRendererOffset(long rendererPositionOffsetUs) {
        this.rendererPositionOffsetUs = rendererPositionOffsetUs;
    }

    public long getStartPositionRendererTime() {
        return this.info.startPositionUs + this.rendererPositionOffsetUs;
    }

    public boolean isFullyBuffered() {
        return this.prepared && (!this.hasEnabledTracks || this.mediaPeriod.getBufferedPositionUs() == Long.MIN_VALUE);
    }

    public long getBufferedPositionUs() {
        if (!this.prepared) {
            return this.info.startPositionUs;
        }
        long bufferedPositionUs = this.hasEnabledTracks ? this.mediaPeriod.getBufferedPositionUs() : Long.MIN_VALUE;
        return bufferedPositionUs == Long.MIN_VALUE ? this.info.durationUs : bufferedPositionUs;
    }

    public long getNextLoadPositionUs() {
        if (this.prepared) {
            return this.mediaPeriod.getNextLoadPositionUs();
        }
        return 0L;
    }

    public void handlePrepared(float playbackSpeed, Timeline timeline) throws ExoPlaybackException {
        this.prepared = true;
        this.trackGroups = this.mediaPeriod.getTrackGroups();
        TrackSelectorResult selectorResult = selectTracks(playbackSpeed, timeline);
        long requestedStartPositionUs = this.info.startPositionUs;
        if (this.info.durationUs != C.TIME_UNSET && requestedStartPositionUs >= this.info.durationUs) {
            requestedStartPositionUs = Math.max(0L, this.info.durationUs - 1);
        }
        long newStartPositionUs = applyTrackSelection(selectorResult, requestedStartPositionUs, false);
        this.rendererPositionOffsetUs += this.info.startPositionUs - newStartPositionUs;
        this.info = this.info.copyWithStartPositionUs(newStartPositionUs);
    }

    public void reevaluateBuffer(long rendererPositionUs) {
        Assertions.checkState(isLoadingMediaPeriod());
        if (this.prepared) {
            this.mediaPeriod.reevaluateBuffer(toPeriodTime(rendererPositionUs));
        }
    }

    public void continueLoading(long rendererPositionUs, float playbackSpeed, long lastRebufferRealtimeMs) {
        Assertions.checkState(isLoadingMediaPeriod());
        long loadingPeriodPositionUs = toPeriodTime(rendererPositionUs);
        this.mediaPeriod.continueLoading(new LoadingInfo.Builder().setPlaybackPositionUs(loadingPeriodPositionUs).setPlaybackSpeed(playbackSpeed).setLastRebufferRealtimeMs(lastRebufferRealtimeMs).build());
    }

    public TrackSelectorResult selectTracks(float f, Timeline timeline) throws ExoPlaybackException {
        int i;
        TrackSelectorResult trackSelectorResultSelectTracks = this.trackSelector.selectTracks(this.rendererCapabilities, getTrackGroups(), this.info.id, timeline);
        int i2 = 0;
        while (true) {
            if (i2 >= trackSelectorResultSelectTracks.length) {
                break;
            }
            if (trackSelectorResultSelectTracks.isRendererEnabled(i2)) {
                Assertions.checkState(trackSelectorResultSelectTracks.selections[i2] != null || this.rendererCapabilities[i2].getTrackType() == -2);
            } else {
                Assertions.checkState(trackSelectorResultSelectTracks.selections[i2] == null);
            }
            i2++;
        }
        for (ExoTrackSelection exoTrackSelection : trackSelectorResultSelectTracks.selections) {
            if (exoTrackSelection != null) {
                exoTrackSelection.onPlaybackSpeed(f);
            }
        }
        return trackSelectorResultSelectTracks;
    }

    public long applyTrackSelection(TrackSelectorResult trackSelectorResult, long positionUs, boolean forceRecreateStreams) {
        return applyTrackSelection(trackSelectorResult, positionUs, forceRecreateStreams, new boolean[this.rendererCapabilities.length]);
    }

    public long applyTrackSelection(TrackSelectorResult newTrackSelectorResult, long positionUs, boolean forceRecreateStreams, boolean[] streamResetFlags) {
        int i = 0;
        while (true) {
            boolean z = false;
            if (i >= newTrackSelectorResult.length) {
                break;
            }
            boolean[] zArr = this.mayRetainStreamFlags;
            if (!forceRecreateStreams && newTrackSelectorResult.isEquivalent(this.trackSelectorResult, i)) {
                z = true;
            }
            zArr[i] = z;
            i++;
        }
        disassociateNoSampleRenderersWithEmptySampleStream(this.sampleStreams);
        disableTrackSelectionsInResult();
        this.trackSelectorResult = newTrackSelectorResult;
        enableTrackSelectionsInResult();
        long positionUs2 = this.mediaPeriod.selectTracks(newTrackSelectorResult.selections, this.mayRetainStreamFlags, this.sampleStreams, streamResetFlags, positionUs);
        associateNoSampleRenderersWithEmptySampleStream(this.sampleStreams);
        this.hasEnabledTracks = false;
        for (int i2 = 0; i2 < this.sampleStreams.length; i2++) {
            if (this.sampleStreams[i2] != null) {
                Assertions.checkState(newTrackSelectorResult.isRendererEnabled(i2));
                if (this.rendererCapabilities[i2].getTrackType() != -2) {
                    this.hasEnabledTracks = true;
                }
            } else {
                Assertions.checkState(newTrackSelectorResult.selections[i2] == null);
            }
        }
        return positionUs2;
    }

    public void release() {
        disableTrackSelectionsInResult();
        releaseMediaPeriod(this.mediaSourceList, this.mediaPeriod);
    }

    public void setNext(MediaPeriodHolder nextMediaPeriodHolder) {
        if (nextMediaPeriodHolder == this.next) {
            return;
        }
        disableTrackSelectionsInResult();
        this.next = nextMediaPeriodHolder;
        enableTrackSelectionsInResult();
    }

    public MediaPeriodHolder getNext() {
        return this.next;
    }

    public TrackGroupArray getTrackGroups() {
        return this.trackGroups;
    }

    public TrackSelectorResult getTrackSelectorResult() {
        return this.trackSelectorResult;
    }

    public void updateClipping() {
        if (this.mediaPeriod instanceof ClippingMediaPeriod) {
            long endPositionUs = this.info.endPositionUs == C.TIME_UNSET ? Long.MIN_VALUE : this.info.endPositionUs;
            ((ClippingMediaPeriod) this.mediaPeriod).updateClipping(0L, endPositionUs);
        }
    }

    public boolean hasLoadingError() {
        try {
            if (!this.prepared) {
                this.mediaPeriod.maybeThrowPrepareError();
            } else {
                for (SampleStream sampleStream : this.sampleStreams) {
                    if (sampleStream != null) {
                        sampleStream.maybeThrowError();
                    }
                }
            }
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    private void enableTrackSelectionsInResult() {
        if (!isLoadingMediaPeriod()) {
            return;
        }
        for (int i = 0; i < this.trackSelectorResult.length; i++) {
            boolean rendererEnabled = this.trackSelectorResult.isRendererEnabled(i);
            ExoTrackSelection trackSelection = this.trackSelectorResult.selections[i];
            if (rendererEnabled && trackSelection != null) {
                trackSelection.enable();
            }
        }
    }

    private void disableTrackSelectionsInResult() {
        if (!isLoadingMediaPeriod()) {
            return;
        }
        for (int i = 0; i < this.trackSelectorResult.length; i++) {
            boolean rendererEnabled = this.trackSelectorResult.isRendererEnabled(i);
            ExoTrackSelection trackSelection = this.trackSelectorResult.selections[i];
            if (rendererEnabled && trackSelection != null) {
                trackSelection.disable();
            }
        }
    }

    private void disassociateNoSampleRenderersWithEmptySampleStream(SampleStream[] sampleStreams) {
        for (int i = 0; i < this.rendererCapabilities.length; i++) {
            if (this.rendererCapabilities[i].getTrackType() == -2) {
                sampleStreams[i] = null;
            }
        }
    }

    private void associateNoSampleRenderersWithEmptySampleStream(SampleStream[] sampleStreams) {
        for (int i = 0; i < this.rendererCapabilities.length; i++) {
            if (this.rendererCapabilities[i].getTrackType() == -2 && this.trackSelectorResult.isRendererEnabled(i)) {
                sampleStreams[i] = new EmptySampleStream();
            }
        }
    }

    private boolean isLoadingMediaPeriod() {
        return this.next == null;
    }

    private static MediaPeriod createMediaPeriod(MediaSource.MediaPeriodId id, MediaSourceList mediaSourceList, Allocator allocator, long startPositionUs, long endPositionUs) {
        MediaPeriod mediaPeriod = mediaSourceList.createPeriod(id, allocator, startPositionUs);
        if (endPositionUs != C.TIME_UNSET) {
            return new ClippingMediaPeriod(mediaPeriod, true, 0L, endPositionUs);
        }
        return mediaPeriod;
    }

    private static void releaseMediaPeriod(MediaSourceList mediaSourceList, MediaPeriod mediaPeriod) {
        try {
            if (mediaPeriod instanceof ClippingMediaPeriod) {
                mediaSourceList.releasePeriod(((ClippingMediaPeriod) mediaPeriod).mediaPeriod);
            } else {
                mediaSourceList.releasePeriod(mediaPeriod);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Period release failed.", e);
        }
    }

    public boolean canBeUsedForMediaPeriodInfo(MediaPeriodInfo info) {
        return MediaPeriodQueue.areDurationsCompatible(this.info.durationUs, info.durationUs) && this.info.startPositionUs == info.startPositionUs && this.info.id.equals(info.id);
    }
}
