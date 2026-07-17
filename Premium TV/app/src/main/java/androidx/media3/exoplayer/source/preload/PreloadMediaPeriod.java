package androidx.media3.exoplayer.source.preload;

import androidx.media3.common.util.Assertions;
import androidx.media3.exoplayer.LoadingInfo;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.source.MediaPeriod;
import androidx.media3.exoplayer.source.SampleStream;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/* JADX INFO: loaded from: classes.dex */
final class PreloadMediaPeriod implements MediaPeriod {
    private MediaPeriod.Callback callback;
    public final MediaPeriod mediaPeriod;
    private PreloadTrackSelectionHolder preloadTrackSelectionHolder;
    private boolean prepareInternalCalled;
    private boolean prepared;

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public /* synthetic */ List getStreamKeys(List list) {
        return Collections.emptyList();
    }

    public PreloadMediaPeriod(MediaPeriod mediaPeriod) {
        this.mediaPeriod = mediaPeriod;
    }

    void preload(MediaPeriod.Callback callback, long positionUs) {
        this.callback = callback;
        if (this.prepared) {
            callback.onPrepared(this);
        }
        if (!this.prepareInternalCalled) {
            prepareInternal(positionUs);
        }
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void prepare(MediaPeriod.Callback callback, long positionUs) {
        this.callback = callback;
        if (this.prepared) {
            callback.onPrepared(this);
        } else if (!this.prepareInternalCalled) {
            prepareInternal(positionUs);
        }
    }

    private void prepareInternal(long positionUs) {
        this.prepareInternalCalled = true;
        this.mediaPeriod.prepare(new MediaPeriod.Callback() { // from class: androidx.media3.exoplayer.source.preload.PreloadMediaPeriod.1
            @Override // androidx.media3.exoplayer.source.SequenceableLoader.Callback
            public void onContinueLoadingRequested(MediaPeriod mediaPeriod) {
                ((MediaPeriod.Callback) Assertions.checkNotNull(PreloadMediaPeriod.this.callback)).onContinueLoadingRequested(PreloadMediaPeriod.this);
            }

            @Override // androidx.media3.exoplayer.source.MediaPeriod.Callback
            public void onPrepared(MediaPeriod mediaPeriod) {
                PreloadMediaPeriod.this.prepared = true;
                ((MediaPeriod.Callback) Assertions.checkNotNull(PreloadMediaPeriod.this.callback)).onPrepared(PreloadMediaPeriod.this);
            }
        }, positionUs);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void maybeThrowPrepareError() throws IOException {
        this.mediaPeriod.maybeThrowPrepareError();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public TrackGroupArray getTrackGroups() {
        return this.mediaPeriod.getTrackGroups();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long selectTracks(ExoTrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
        return selectTracksInternal(selections, mayRetainStreamFlags, streams, streamResetFlags, positionUs);
    }

    private long selectTracksInternal(ExoTrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
        if (this.preloadTrackSelectionHolder != null) {
            Assertions.checkState(streams.length == this.preloadTrackSelectionHolder.streams.length);
            if (positionUs != this.preloadTrackSelectionHolder.trackSelectionPositionUs) {
                for (int i = 0; i < this.preloadTrackSelectionHolder.streams.length; i++) {
                    if (this.preloadTrackSelectionHolder.streams[i] != null) {
                        streams[i] = this.preloadTrackSelectionHolder.streams[i];
                        mayRetainStreamFlags[i] = false;
                    }
                }
                this.preloadTrackSelectionHolder = null;
                return this.mediaPeriod.selectTracks(selections, mayRetainStreamFlags, streams, streamResetFlags, positionUs);
            }
            PreloadTrackSelectionHolder holder = (PreloadTrackSelectionHolder) Assertions.checkNotNull(this.preloadTrackSelectionHolder);
            long trackSelectionPositionUs = holder.trackSelectionPositionUs;
            boolean[] preloadStreamResetFlags = holder.streamResetFlags;
            if (maybeUpdatePreloadTrackSelectionHolderForReselection(selections, holder)) {
                boolean[] preloadStreamResetFlags2 = new boolean[preloadStreamResetFlags.length];
                trackSelectionPositionUs = this.mediaPeriod.selectTracks(holder.selections, holder.mayRetainStreamFlags, holder.streams, preloadStreamResetFlags2, holder.trackSelectionPositionUs);
                for (int i2 = 0; i2 < holder.mayRetainStreamFlags.length; i2++) {
                    if (holder.mayRetainStreamFlags[i2]) {
                        preloadStreamResetFlags2[i2] = true;
                    }
                }
                preloadStreamResetFlags = preloadStreamResetFlags2;
            }
            System.arraycopy(holder.streams, 0, streams, 0, holder.streams.length);
            System.arraycopy(preloadStreamResetFlags, 0, streamResetFlags, 0, preloadStreamResetFlags.length);
            this.preloadTrackSelectionHolder = null;
            return trackSelectionPositionUs;
        }
        return this.mediaPeriod.selectTracks(selections, mayRetainStreamFlags, streams, streamResetFlags, positionUs);
    }

    private static boolean maybeUpdatePreloadTrackSelectionHolderForReselection(ExoTrackSelection[] selections, PreloadTrackSelectionHolder preloadTrackSelectionHolder) {
        ExoTrackSelection[] preloadSelections = ((PreloadTrackSelectionHolder) Assertions.checkNotNull(preloadTrackSelectionHolder)).selections;
        boolean needsReselection = false;
        for (int i = 0; i < selections.length; i++) {
            ExoTrackSelection selection = selections[i];
            ExoTrackSelection preloadSelection = preloadSelections[i];
            if (selection != null || preloadSelection != null) {
                preloadTrackSelectionHolder.mayRetainStreamFlags[i] = false;
                if (selection == null) {
                    preloadTrackSelectionHolder.selections[i] = null;
                    needsReselection = true;
                } else if (preloadSelection == null) {
                    preloadTrackSelectionHolder.selections[i] = selection;
                    needsReselection = true;
                } else if (!isSameAdaptionSet(selection, preloadSelection)) {
                    preloadTrackSelectionHolder.selections[i] = selection;
                    needsReselection = true;
                } else if (selection.getTrackGroup().type == 2 || selection.getTrackGroup().type == 1 || selection.getSelectedIndexInTrackGroup() == preloadSelection.getSelectedIndexInTrackGroup()) {
                    preloadTrackSelectionHolder.mayRetainStreamFlags[i] = true;
                } else {
                    preloadTrackSelectionHolder.selections[i] = selection;
                    needsReselection = true;
                }
            }
        }
        return needsReselection;
    }

    private static boolean isSameAdaptionSet(ExoTrackSelection selection, ExoTrackSelection preloadSelection) {
        if (!Objects.equals(selection.getTrackGroup(), preloadSelection.getTrackGroup()) || selection.length() != preloadSelection.length()) {
            return false;
        }
        for (int i = 0; i < selection.length(); i++) {
            if (selection.getIndexInTrackGroup(i) != preloadSelection.getIndexInTrackGroup(i)) {
                return false;
            }
        }
        return true;
    }

    long selectTracksForPreloading(ExoTrackSelection[] selections, long positionUs) {
        SampleStream[] preloadedSampleStreams = new SampleStream[selections.length];
        boolean[] preloadedStreamResetFlags = new boolean[selections.length];
        boolean[] mayRetainStreamFlags = new boolean[selections.length];
        long trackSelectionPositionUs = selectTracksInternal(selections, mayRetainStreamFlags, preloadedSampleStreams, preloadedStreamResetFlags, positionUs);
        this.preloadTrackSelectionHolder = new PreloadTrackSelectionHolder(selections, mayRetainStreamFlags, preloadedSampleStreams, preloadedStreamResetFlags, trackSelectionPositionUs);
        return trackSelectionPositionUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void discardBuffer(long positionUs, boolean toKeyframe) {
        this.mediaPeriod.discardBuffer(positionUs, toKeyframe);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long readDiscontinuity() {
        return this.mediaPeriod.readDiscontinuity();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long seekToUs(long positionUs) {
        return this.mediaPeriod.seekToUs(positionUs);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
        return this.mediaPeriod.getAdjustedSeekPositionUs(positionUs, seekParameters);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public long getBufferedPositionUs() {
        return this.mediaPeriod.getBufferedPositionUs();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public long getNextLoadPositionUs() {
        return this.mediaPeriod.getNextLoadPositionUs();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public boolean continueLoading(LoadingInfo loadingInfo) {
        return this.mediaPeriod.continueLoading(loadingInfo);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public boolean isLoading() {
        return this.mediaPeriod.isLoading();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public void reevaluateBuffer(long positionUs) {
        this.mediaPeriod.reevaluateBuffer(positionUs);
    }

    private static class PreloadTrackSelectionHolder {
        public final boolean[] mayRetainStreamFlags;
        public final ExoTrackSelection[] selections;
        public final boolean[] streamResetFlags;
        public final SampleStream[] streams;
        public final long trackSelectionPositionUs;

        public PreloadTrackSelectionHolder(ExoTrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long trackSelectionPositionUs) {
            this.selections = selections;
            this.mayRetainStreamFlags = mayRetainStreamFlags;
            this.streams = streams;
            this.streamResetFlags = streamResetFlags;
            this.trackSelectionPositionUs = trackSelectionPositionUs;
        }
    }
}
