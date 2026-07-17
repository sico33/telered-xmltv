package androidx.media3.exoplayer.mediacodec;

import androidx.media3.common.util.Assertions;
import androidx.media3.decoder.DecoderInputBuffer;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
final class BatchBuffer extends DecoderInputBuffer {
    public static final int DEFAULT_MAX_SAMPLE_COUNT = 32;
    static final int MAX_SIZE_BYTES = 3072000;
    private long lastSampleTimeUs;
    private int maxSampleCount;
    private int sampleCount;

    public BatchBuffer() {
        super(2);
        this.maxSampleCount = 32;
    }

    @Override // androidx.media3.decoder.DecoderInputBuffer, androidx.media3.decoder.Buffer
    public void clear() {
        super.clear();
        this.sampleCount = 0;
    }

    public void setMaxSampleCount(int maxSampleCount) {
        Assertions.checkArgument(maxSampleCount > 0);
        this.maxSampleCount = maxSampleCount;
    }

    public long getFirstSampleTimeUs() {
        return this.timeUs;
    }

    public long getLastSampleTimeUs() {
        return this.lastSampleTimeUs;
    }

    public int getSampleCount() {
        return this.sampleCount;
    }

    public boolean hasSamples() {
        return this.sampleCount > 0;
    }

    public boolean append(DecoderInputBuffer buffer) {
        Assertions.checkArgument(!buffer.isEncrypted());
        Assertions.checkArgument(!buffer.hasSupplementalData());
        Assertions.checkArgument(!buffer.isEndOfStream());
        if (!canAppendSampleBuffer(buffer)) {
            return false;
        }
        int i = this.sampleCount;
        this.sampleCount = i + 1;
        if (i == 0) {
            this.timeUs = buffer.timeUs;
            if (buffer.isKeyFrame()) {
                setFlags(1);
            }
        }
        ByteBuffer bufferData = buffer.data;
        if (bufferData != null) {
            ensureSpaceForWrite(bufferData.remaining());
            this.data.put(bufferData);
        }
        this.lastSampleTimeUs = buffer.timeUs;
        return true;
    }

    private boolean canAppendSampleBuffer(DecoderInputBuffer buffer) {
        if (!hasSamples()) {
            return true;
        }
        if (this.sampleCount >= this.maxSampleCount) {
            return false;
        }
        ByteBuffer bufferData = buffer.data;
        return bufferData == null || this.data == null || this.data.position() + bufferData.remaining() <= MAX_SIZE_BYTES;
    }
}
