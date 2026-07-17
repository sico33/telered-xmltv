package androidx.media3.exoplayer.source;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.StreamKey;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.LoadingInfo;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import java.io.IOException;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class ClippingMediaPeriod implements MediaPeriod, MediaPeriod.Callback {
    private MediaPeriod.Callback callback;
    private ClippingMediaSource.IllegalClippingException clippingError;
    long endUs;
    public final MediaPeriod mediaPeriod;
    private long pendingInitialDiscontinuityPositionUs;
    private ClippingSampleStream[] sampleStreams = new ClippingSampleStream[0];
    long startUs;

    public ClippingMediaPeriod(MediaPeriod mediaPeriod, boolean enableInitialDiscontinuity, long startUs, long endUs) {
        this.mediaPeriod = mediaPeriod;
        this.pendingInitialDiscontinuityPositionUs = enableInitialDiscontinuity ? startUs : C.TIME_UNSET;
        this.startUs = startUs;
        this.endUs = endUs;
    }

    public void updateClipping(long startUs, long endUs) {
        this.startUs = startUs;
        this.endUs = endUs;
    }

    public void setClippingError(ClippingMediaSource.IllegalClippingException clippingError) {
        this.clippingError = clippingError;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void prepare(MediaPeriod.Callback callback, long positionUs) {
        this.callback = callback;
        this.mediaPeriod.prepare(this, positionUs);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void maybeThrowPrepareError() throws IOException {
        if (this.clippingError != null) {
            throw this.clippingError;
        }
        this.mediaPeriod.maybeThrowPrepareError();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public List<StreamKey> getStreamKeys(List<ExoTrackSelection> trackSelections) {
        return this.mediaPeriod.getStreamKeys(trackSelections);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public TrackGroupArray getTrackGroups() {
        return this.mediaPeriod.getTrackGroups();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long selectTracks(ExoTrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
        long j;
        this.sampleStreams = new ClippingSampleStream[streams.length];
        SampleStream[] childStreams = new SampleStream[streams.length];
        int i = 0;
        while (true) {
            SampleStream sampleStream = null;
            if (i >= streams.length) {
                break;
            }
            this.sampleStreams[i] = (ClippingSampleStream) streams[i];
            if (this.sampleStreams[i] != null) {
                sampleStream = this.sampleStreams[i].childStream;
            }
            childStreams[i] = sampleStream;
            i++;
        }
        long enablePositionUs = this.mediaPeriod.selectTracks(selections, mayRetainStreamFlags, childStreams, streamResetFlags, positionUs);
        if (isPendingInitialDiscontinuity() && positionUs == this.startUs && shouldKeepInitialDiscontinuity(this.startUs, selections)) {
            j = enablePositionUs;
        } else {
            j = C.TIME_UNSET;
        }
        this.pendingInitialDiscontinuityPositionUs = j;
        Assertions.checkState(enablePositionUs == positionUs || (enablePositionUs >= this.startUs && (this.endUs == Long.MIN_VALUE || enablePositionUs <= this.endUs)));
        for (int i2 = 0; i2 < streams.length; i2++) {
            SampleStream sampleStream2 = childStreams[i2];
            ClippingSampleStream[] clippingSampleStreamArr = this.sampleStreams;
            if (sampleStream2 == null) {
                clippingSampleStreamArr[i2] = null;
            } else if (clippingSampleStreamArr[i2] == null || this.sampleStreams[i2].childStream != childStreams[i2]) {
                this.sampleStreams[i2] = new ClippingSampleStream(childStreams[i2]);
            }
            streams[i2] = this.sampleStreams[i2];
        }
        return enablePositionUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void discardBuffer(long positionUs, boolean toKeyframe) {
        this.mediaPeriod.discardBuffer(positionUs, toKeyframe);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public void reevaluateBuffer(long positionUs) {
        this.mediaPeriod.reevaluateBuffer(positionUs);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long readDiscontinuity() {
        if (isPendingInitialDiscontinuity()) {
            long initialDiscontinuityUs = this.pendingInitialDiscontinuityPositionUs;
            this.pendingInitialDiscontinuityPositionUs = C.TIME_UNSET;
            long childDiscontinuityUs = readDiscontinuity();
            return childDiscontinuityUs != C.TIME_UNSET ? childDiscontinuityUs : initialDiscontinuityUs;
        }
        long discontinuityUs = this.mediaPeriod.readDiscontinuity();
        if (discontinuityUs == C.TIME_UNSET) {
            return C.TIME_UNSET;
        }
        boolean z = true;
        Assertions.checkState(discontinuityUs >= this.startUs);
        if (this.endUs != Long.MIN_VALUE && discontinuityUs > this.endUs) {
            z = false;
        }
        Assertions.checkState(z);
        return discontinuityUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public long getBufferedPositionUs() {
        long bufferedPositionUs = this.mediaPeriod.getBufferedPositionUs();
        if (bufferedPositionUs == Long.MIN_VALUE || (this.endUs != Long.MIN_VALUE && bufferedPositionUs >= this.endUs)) {
            return Long.MIN_VALUE;
        }
        return bufferedPositionUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long seekToUs(long positionUs) {
        this.pendingInitialDiscontinuityPositionUs = C.TIME_UNSET;
        boolean z = false;
        for (ClippingSampleStream sampleStream : this.sampleStreams) {
            if (sampleStream != null) {
                sampleStream.clearSentEos();
            }
        }
        long seekUs = this.mediaPeriod.seekToUs(positionUs);
        if (seekUs == positionUs || (seekUs >= this.startUs && (this.endUs == Long.MIN_VALUE || seekUs <= this.endUs))) {
            z = true;
        }
        Assertions.checkState(z);
        return seekUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
        if (positionUs == this.startUs) {
            return this.startUs;
        }
        SeekParameters clippedSeekParameters = clipSeekParameters(positionUs, seekParameters);
        return this.mediaPeriod.getAdjustedSeekPositionUs(positionUs, clippedSeekParameters);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public long getNextLoadPositionUs() {
        long nextLoadPositionUs = this.mediaPeriod.getNextLoadPositionUs();
        if (nextLoadPositionUs == Long.MIN_VALUE || (this.endUs != Long.MIN_VALUE && nextLoadPositionUs >= this.endUs)) {
            return Long.MIN_VALUE;
        }
        return nextLoadPositionUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public boolean continueLoading(LoadingInfo loadingInfo) {
        return this.mediaPeriod.continueLoading(loadingInfo);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public boolean isLoading() {
        return this.mediaPeriod.isLoading();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod.Callback
    public void onPrepared(MediaPeriod mediaPeriod) {
        if (this.clippingError != null) {
            return;
        }
        ((MediaPeriod.Callback) Assertions.checkNotNull(this.callback)).onPrepared(this);
    }

    @Override // androidx.media3.exoplayer.source.SequenceableLoader.Callback
    public void onContinueLoadingRequested(MediaPeriod source) {
        ((MediaPeriod.Callback) Assertions.checkNotNull(this.callback)).onContinueLoadingRequested(this);
    }

    boolean isPendingInitialDiscontinuity() {
        return this.pendingInitialDiscontinuityPositionUs != C.TIME_UNSET;
    }

    private SeekParameters clipSeekParameters(long positionUs, SeekParameters seekParameters) {
        long toleranceBeforeUs = Util.constrainValue(seekParameters.toleranceBeforeUs, 0L, positionUs - this.startUs);
        long toleranceAfterUs = Util.constrainValue(seekParameters.toleranceAfterUs, 0L, this.endUs == Long.MIN_VALUE ? Long.MAX_VALUE : this.endUs - positionUs);
        if (toleranceBeforeUs == seekParameters.toleranceBeforeUs && toleranceAfterUs == seekParameters.toleranceAfterUs) {
            return seekParameters;
        }
        return new SeekParameters(toleranceBeforeUs, toleranceAfterUs);
    }

    private static boolean shouldKeepInitialDiscontinuity(long startUs, ExoTrackSelection[] selections) {
        if (startUs != 0) {
            for (ExoTrackSelection trackSelection : selections) {
                if (trackSelection != null) {
                    Format selectedFormat = trackSelection.getSelectedFormat();
                    if (!MimeTypes.allSamplesAreSyncSamples(selectedFormat.sampleMimeType, selectedFormat.codecs)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private final class ClippingSampleStream implements SampleStream {
        public final SampleStream childStream;
        private boolean sentEos;

        public ClippingSampleStream(SampleStream childStream) {
            this.childStream = childStream;
        }

        public void clearSentEos() {
            this.sentEos = false;
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public boolean isReady() {
            return !ClippingMediaPeriod.this.isPendingInitialDiscontinuity() && this.childStream.isReady();
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public void maybeThrowError() throws IOException {
            this.childStream.maybeThrowError();
        }

        /* JADX WARN: Code restructure failed: missing block: B:36:0x008c, code lost:
        
            if (r19.waitingForKeys == false) goto L37;
         */
        @Override // androidx.media3.exoplayer.source.SampleStream
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct code enable 'Show inconsistent code' option in preferences
        */
        public int readData(androidx.media3.exoplayer.FormatHolder r18, androidx.media3.decoder.DecoderInputBuffer r19, int r20) {
            /*
                r17 = this;
                r0 = r17
                r1 = r18
                r2 = r19
                androidx.media3.exoplayer.source.ClippingMediaPeriod r3 = androidx.media3.exoplayer.source.ClippingMediaPeriod.this
                boolean r3 = r3.isPendingInitialDiscontinuity()
                r4 = -3
                if (r3 == 0) goto L10
                return r4
            L10:
                boolean r3 = r0.sentEos
                r5 = 4
                r6 = -4
                if (r3 == 0) goto L1a
                r2.setFlags(r5)
                return r6
            L1a:
                androidx.media3.exoplayer.source.ClippingMediaPeriod r3 = androidx.media3.exoplayer.source.ClippingMediaPeriod.this
                long r7 = r3.getBufferedPositionUs()
                androidx.media3.exoplayer.source.SampleStream r3 = r0.childStream
                r9 = r20
                int r3 = r3.readData(r1, r2, r9)
                r10 = -5
                r11 = -9223372036854775808
                if (r3 != r10) goto L6b
                androidx.media3.common.Format r4 = r1.format
                java.lang.Object r4 = androidx.media3.common.util.Assertions.checkNotNull(r4)
                androidx.media3.common.Format r4 = (androidx.media3.common.Format) r4
                int r5 = r4.encoderDelay
                if (r5 != 0) goto L3d
                int r5 = r4.encoderPadding
                if (r5 == 0) goto L6a
            L3d:
                androidx.media3.exoplayer.source.ClippingMediaPeriod r5 = androidx.media3.exoplayer.source.ClippingMediaPeriod.this
                long r5 = r5.startUs
                r13 = 0
                int r5 = (r5 > r13 ? 1 : (r5 == r13 ? 0 : -1))
                r6 = 0
                if (r5 == 0) goto L4a
                r5 = r6
                goto L4c
            L4a:
                int r5 = r4.encoderDelay
            L4c:
                androidx.media3.exoplayer.source.ClippingMediaPeriod r13 = androidx.media3.exoplayer.source.ClippingMediaPeriod.this
                long r13 = r13.endUs
                int r11 = (r13 > r11 ? 1 : (r13 == r11 ? 0 : -1))
                if (r11 == 0) goto L55
                goto L57
            L55:
                int r6 = r4.encoderPadding
            L57:
                androidx.media3.common.Format$Builder r11 = r4.buildUpon()
                androidx.media3.common.Format$Builder r11 = r11.setEncoderDelay(r5)
                androidx.media3.common.Format$Builder r11 = r11.setEncoderPadding(r6)
                androidx.media3.common.Format r11 = r11.build()
                r1.format = r11
            L6a:
                return r10
            L6b:
                androidx.media3.exoplayer.source.ClippingMediaPeriod r10 = androidx.media3.exoplayer.source.ClippingMediaPeriod.this
                long r13 = r10.endUs
                int r10 = (r13 > r11 ? 1 : (r13 == r11 ? 0 : -1))
                if (r10 == 0) goto L98
                if (r3 != r6) goto L82
                long r13 = r2.timeUs
                androidx.media3.exoplayer.source.ClippingMediaPeriod r10 = androidx.media3.exoplayer.source.ClippingMediaPeriod.this
                r15 = r7
                r8 = r6
                long r6 = r10.endUs
                int r6 = (r13 > r6 ? 1 : (r13 == r6 ? 0 : -1))
                if (r6 >= 0) goto L8e
                goto L84
            L82:
                r15 = r7
                r8 = r6
            L84:
                if (r3 != r4) goto L99
                int r4 = (r15 > r11 ? 1 : (r15 == r11 ? 0 : -1))
                if (r4 != 0) goto L99
                boolean r4 = r2.waitingForKeys
                if (r4 != 0) goto L99
            L8e:
                r2.clear()
                r2.setFlags(r5)
                r4 = 1
                r0.sentEos = r4
                return r8
            L98:
                r15 = r7
            L99:
                return r3
            */
            throw new UnsupportedOperationException("Method not decompiled: androidx.media3.exoplayer.source.ClippingMediaPeriod.ClippingSampleStream.readData(androidx.media3.exoplayer.FormatHolder, androidx.media3.decoder.DecoderInputBuffer, int):int");
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public int skipData(long positionUs) {
            if (ClippingMediaPeriod.this.isPendingInitialDiscontinuity()) {
                return -3;
            }
            return this.childStream.skipData(positionUs);
        }
    }
}
