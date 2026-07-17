package androidx.media3.exoplayer.source;

import androidx.media3.common.C;
import androidx.media3.common.StreamKey;
import androidx.media3.common.util.Assertions;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.exoplayer.FormatHolder;
import androidx.media3.exoplayer.LoadingInfo;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import java.io.IOException;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class TimeOffsetMediaPeriod implements MediaPeriod, MediaPeriod.Callback {
    private MediaPeriod.Callback callback;
    private final MediaPeriod mediaPeriod;
    private final long timeOffsetUs;

    public TimeOffsetMediaPeriod(MediaPeriod mediaPeriod, long timeOffsetUs) {
        this.mediaPeriod = mediaPeriod;
        this.timeOffsetUs = timeOffsetUs;
    }

    public MediaPeriod getWrappedMediaPeriod() {
        return this.mediaPeriod;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void prepare(MediaPeriod.Callback callback, long positionUs) {
        this.callback = callback;
        this.mediaPeriod.prepare(this, positionUs - this.timeOffsetUs);
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
    public List<StreamKey> getStreamKeys(List<ExoTrackSelection> trackSelections) {
        return this.mediaPeriod.getStreamKeys(trackSelections);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long selectTracks(ExoTrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
        SampleStream[] childStreams = new SampleStream[streams.length];
        int i = 0;
        while (true) {
            SampleStream childStream = null;
            if (i >= streams.length) {
                break;
            }
            TimeOffsetSampleStream sampleStream = (TimeOffsetSampleStream) streams[i];
            if (sampleStream != null) {
                childStream = sampleStream.getChildStream();
            }
            childStreams[i] = childStream;
            i++;
        }
        long startPositionUs = this.mediaPeriod.selectTracks(selections, mayRetainStreamFlags, childStreams, streamResetFlags, positionUs - this.timeOffsetUs);
        for (int i2 = 0; i2 < streams.length; i2++) {
            SampleStream childStream2 = childStreams[i2];
            if (childStream2 == null) {
                streams[i2] = null;
            } else if (streams[i2] == null || ((TimeOffsetSampleStream) streams[i2]).getChildStream() != childStream2) {
                streams[i2] = new TimeOffsetSampleStream(childStream2, this.timeOffsetUs);
            }
        }
        return this.timeOffsetUs + startPositionUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void discardBuffer(long positionUs, boolean toKeyframe) {
        this.mediaPeriod.discardBuffer(positionUs - this.timeOffsetUs, toKeyframe);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long readDiscontinuity() {
        long discontinuityPositionUs = this.mediaPeriod.readDiscontinuity();
        if (discontinuityPositionUs == C.TIME_UNSET) {
            return C.TIME_UNSET;
        }
        return this.timeOffsetUs + discontinuityPositionUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long seekToUs(long positionUs) {
        return this.mediaPeriod.seekToUs(positionUs - this.timeOffsetUs) + this.timeOffsetUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
        return this.mediaPeriod.getAdjustedSeekPositionUs(positionUs - this.timeOffsetUs, seekParameters) + this.timeOffsetUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public long getBufferedPositionUs() {
        long bufferedPositionUs = this.mediaPeriod.getBufferedPositionUs();
        if (bufferedPositionUs == Long.MIN_VALUE) {
            return Long.MIN_VALUE;
        }
        return this.timeOffsetUs + bufferedPositionUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public long getNextLoadPositionUs() {
        long nextLoadPositionUs = this.mediaPeriod.getNextLoadPositionUs();
        if (nextLoadPositionUs == Long.MIN_VALUE) {
            return Long.MIN_VALUE;
        }
        return this.timeOffsetUs + nextLoadPositionUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public boolean continueLoading(LoadingInfo loadingInfo) {
        return this.mediaPeriod.continueLoading(loadingInfo.buildUpon().setPlaybackPositionUs(loadingInfo.playbackPositionUs - this.timeOffsetUs).build());
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public boolean isLoading() {
        return this.mediaPeriod.isLoading();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public void reevaluateBuffer(long positionUs) {
        this.mediaPeriod.reevaluateBuffer(positionUs - this.timeOffsetUs);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod.Callback
    public void onPrepared(MediaPeriod mediaPeriod) {
        ((MediaPeriod.Callback) Assertions.checkNotNull(this.callback)).onPrepared(this);
    }

    @Override // androidx.media3.exoplayer.source.SequenceableLoader.Callback
    public void onContinueLoadingRequested(MediaPeriod source) {
        ((MediaPeriod.Callback) Assertions.checkNotNull(this.callback)).onContinueLoadingRequested(this);
    }

    private static final class TimeOffsetSampleStream implements SampleStream {
        private final SampleStream sampleStream;
        private final long timeOffsetUs;

        public TimeOffsetSampleStream(SampleStream sampleStream, long timeOffsetUs) {
            this.sampleStream = sampleStream;
            this.timeOffsetUs = timeOffsetUs;
        }

        public SampleStream getChildStream() {
            return this.sampleStream;
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public boolean isReady() {
            return this.sampleStream.isReady();
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public void maybeThrowError() throws IOException {
            this.sampleStream.maybeThrowError();
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, int readFlags) {
            int readResult = this.sampleStream.readData(formatHolder, buffer, readFlags);
            if (readResult == -4) {
                buffer.timeUs += this.timeOffsetUs;
            }
            return readResult;
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public int skipData(long positionUs) {
            return this.sampleStream.skipData(positionUs - this.timeOffsetUs);
        }
    }
}
