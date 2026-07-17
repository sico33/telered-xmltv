package androidx.media3.extractor;

import androidx.media3.common.util.Assertions;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class TrueHdSampleRechunker {
    private int chunkFlags;
    private int chunkOffset;
    private int chunkSampleCount;
    private int chunkSize;
    private long chunkTimeUs;
    private boolean foundSyncframe;
    private final byte[] syncframePrefix = new byte[10];

    public void reset() {
        this.foundSyncframe = false;
        this.chunkSampleCount = 0;
    }

    public void startSample(ExtractorInput input) throws IOException {
        if (this.foundSyncframe) {
            return;
        }
        input.peekFully(this.syncframePrefix, 0, 10);
        input.resetPeekPosition();
        if (Ac3Util.parseTrueHdSyncframeAudioSampleCount(this.syncframePrefix) == 0) {
            return;
        }
        this.foundSyncframe = true;
    }

    public void sampleMetadata(TrackOutput trackOutput, long timeUs, int flags, int size, int offset, TrackOutput.CryptoData cryptoData) {
        Assertions.checkState(this.chunkOffset <= size + offset, "TrueHD chunk samples must be contiguous in the sample queue.");
        if (!this.foundSyncframe) {
            return;
        }
        int i = this.chunkSampleCount;
        this.chunkSampleCount = i + 1;
        if (i == 0) {
            this.chunkTimeUs = timeUs;
            this.chunkFlags = flags;
            this.chunkSize = 0;
        }
        this.chunkSize += size;
        this.chunkOffset = offset;
        if (this.chunkSampleCount >= 16) {
            outputPendingSampleMetadata(trackOutput, cryptoData);
        }
    }

    public void outputPendingSampleMetadata(TrackOutput trackOutput, TrackOutput.CryptoData cryptoData) {
        if (this.chunkSampleCount > 0) {
            trackOutput.sampleMetadata(this.chunkTimeUs, this.chunkFlags, this.chunkSize, this.chunkOffset, cryptoData);
            this.chunkSampleCount = 0;
        }
    }
}
