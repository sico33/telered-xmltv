package androidx.media3.exoplayer.hls;

import androidx.media3.common.util.Assertions;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.exoplayer.FormatHolder;
import androidx.media3.exoplayer.source.SampleStream;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
final class HlsSampleStream implements SampleStream {
    private int sampleQueueIndex = -1;
    private final HlsSampleStreamWrapper sampleStreamWrapper;
    private final int trackGroupIndex;

    public HlsSampleStream(HlsSampleStreamWrapper sampleStreamWrapper, int trackGroupIndex) {
        this.sampleStreamWrapper = sampleStreamWrapper;
        this.trackGroupIndex = trackGroupIndex;
    }

    public void bindSampleQueue() {
        Assertions.checkArgument(this.sampleQueueIndex == -1);
        this.sampleQueueIndex = this.sampleStreamWrapper.bindSampleQueueToSampleStream(this.trackGroupIndex);
    }

    public void unbindSampleQueue() {
        if (this.sampleQueueIndex != -1) {
            this.sampleStreamWrapper.unbindSampleQueue(this.trackGroupIndex);
            this.sampleQueueIndex = -1;
        }
    }

    @Override // androidx.media3.exoplayer.source.SampleStream
    public boolean isReady() {
        return this.sampleQueueIndex == -3 || (hasValidSampleQueueIndex() && this.sampleStreamWrapper.isReady(this.sampleQueueIndex));
    }

    @Override // androidx.media3.exoplayer.source.SampleStream
    public void maybeThrowError() throws IOException {
        if (this.sampleQueueIndex == -2) {
            throw new SampleQueueMappingException(this.sampleStreamWrapper.getTrackGroups().get(this.trackGroupIndex).getFormat(0).sampleMimeType);
        }
        if (this.sampleQueueIndex == -1) {
            this.sampleStreamWrapper.maybeThrowError();
        } else if (this.sampleQueueIndex != -3) {
            this.sampleStreamWrapper.maybeThrowError(this.sampleQueueIndex);
        }
    }

    @Override // androidx.media3.exoplayer.source.SampleStream
    public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, int readFlags) {
        if (this.sampleQueueIndex == -3) {
            buffer.addFlag(4);
            return -4;
        }
        if (hasValidSampleQueueIndex()) {
            return this.sampleStreamWrapper.readData(this.sampleQueueIndex, formatHolder, buffer, readFlags);
        }
        return -3;
    }

    @Override // androidx.media3.exoplayer.source.SampleStream
    public int skipData(long positionUs) {
        if (hasValidSampleQueueIndex()) {
            return this.sampleStreamWrapper.skipData(this.sampleQueueIndex, positionUs);
        }
        return 0;
    }

    private boolean hasValidSampleQueueIndex() {
        return (this.sampleQueueIndex == -1 || this.sampleQueueIndex == -3 || this.sampleQueueIndex == -2) ? false : true;
    }
}
