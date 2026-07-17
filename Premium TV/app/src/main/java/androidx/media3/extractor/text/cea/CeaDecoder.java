package androidx.media3.extractor.text.cea;

import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.decoder.DecoderOutputBuffer;
import androidx.media3.extractor.text.Subtitle;
import androidx.media3.extractor.text.SubtitleDecoder;
import androidx.media3.extractor.text.SubtitleDecoderException;
import androidx.media3.extractor.text.SubtitleInputBuffer;
import androidx.media3.extractor.text.SubtitleOutputBuffer;
import java.util.ArrayDeque;
import java.util.PriorityQueue;

/* JADX INFO: loaded from: classes.dex */
abstract class CeaDecoder implements SubtitleDecoder {
    private static final int NUM_INPUT_BUFFERS = 10;
    private static final int NUM_OUTPUT_BUFFERS = 2;
    private final ArrayDeque<CeaInputBuffer> availableInputBuffers = new ArrayDeque<>();
    private final ArrayDeque<SubtitleOutputBuffer> availableOutputBuffers;
    private CeaInputBuffer dequeuedInputBuffer;
    private long outputStartTimeUs;
    private long playbackPositionUs;
    private long queuedInputBufferCount;
    private final PriorityQueue<CeaInputBuffer> queuedInputBuffers;

    protected abstract Subtitle createSubtitle();

    protected abstract void decode(SubtitleInputBuffer subtitleInputBuffer);

    @Override // androidx.media3.decoder.Decoder
    public abstract String getName();

    protected abstract boolean isNewSubtitleDataAvailable();

    public CeaDecoder() {
        for (int i = 0; i < 10; i++) {
            this.availableInputBuffers.add(new CeaInputBuffer());
        }
        this.availableOutputBuffers = new ArrayDeque<>();
        for (int i2 = 0; i2 < 2; i2++) {
            this.availableOutputBuffers.add(new CeaOutputBuffer(new DecoderOutputBuffer.Owner() { // from class: androidx.media3.extractor.text.cea.CeaDecoder$$ExternalSyntheticLambda0
                @Override // androidx.media3.decoder.DecoderOutputBuffer.Owner
                public final void releaseOutputBuffer(DecoderOutputBuffer decoderOutputBuffer) {
                    this.f$0.releaseOutputBuffer((CeaDecoder.CeaOutputBuffer) decoderOutputBuffer);
                }
            }));
        }
        this.queuedInputBuffers = new PriorityQueue<>();
        this.outputStartTimeUs = C.TIME_UNSET;
    }

    @Override // androidx.media3.decoder.Decoder
    public final void setOutputStartTimeUs(long outputStartTimeUs) {
        this.outputStartTimeUs = outputStartTimeUs;
    }

    @Override // androidx.media3.extractor.text.SubtitleDecoder
    public void setPositionUs(long positionUs) {
        this.playbackPositionUs = positionUs;
    }

    /* JADX WARN: Can't rename method to resolve collision */
    @Override // androidx.media3.decoder.Decoder
    public SubtitleInputBuffer dequeueInputBuffer() throws SubtitleDecoderException {
        Assertions.checkState(this.dequeuedInputBuffer == null);
        if (this.availableInputBuffers.isEmpty()) {
            return null;
        }
        this.dequeuedInputBuffer = this.availableInputBuffers.pollFirst();
        return this.dequeuedInputBuffer;
    }

    /* JADX WARN: Can't rename method to resolve collision */
    @Override // androidx.media3.decoder.Decoder
    public void queueInputBuffer(SubtitleInputBuffer inputBuffer) throws SubtitleDecoderException {
        Assertions.checkArgument(inputBuffer == this.dequeuedInputBuffer);
        CeaInputBuffer ceaInputBuffer = (CeaInputBuffer) inputBuffer;
        if (this.outputStartTimeUs != C.TIME_UNSET && ceaInputBuffer.timeUs < this.outputStartTimeUs) {
            releaseInputBuffer(ceaInputBuffer);
        } else {
            long j = this.queuedInputBufferCount;
            this.queuedInputBufferCount = 1 + j;
            ceaInputBuffer.queuedInputBufferCount = j;
            this.queuedInputBuffers.add(ceaInputBuffer);
        }
        this.dequeuedInputBuffer = null;
    }

    /* JADX WARN: Can't rename method to resolve collision */
    @Override // androidx.media3.decoder.Decoder
    public SubtitleOutputBuffer dequeueOutputBuffer() throws SubtitleDecoderException {
        if (this.availableOutputBuffers.isEmpty()) {
            return null;
        }
        while (!this.queuedInputBuffers.isEmpty() && ((CeaInputBuffer) Util.castNonNull(this.queuedInputBuffers.peek())).timeUs <= this.playbackPositionUs) {
            CeaInputBuffer inputBuffer = (CeaInputBuffer) Util.castNonNull(this.queuedInputBuffers.poll());
            if (inputBuffer.isEndOfStream()) {
                SubtitleOutputBuffer outputBuffer = (SubtitleOutputBuffer) Util.castNonNull(this.availableOutputBuffers.pollFirst());
                outputBuffer.addFlag(4);
                releaseInputBuffer(inputBuffer);
                return outputBuffer;
            }
            decode(inputBuffer);
            if (isNewSubtitleDataAvailable()) {
                Subtitle subtitle = createSubtitle();
                SubtitleOutputBuffer outputBuffer2 = (SubtitleOutputBuffer) Util.castNonNull(this.availableOutputBuffers.pollFirst());
                outputBuffer2.setContent(inputBuffer.timeUs, subtitle, Long.MAX_VALUE);
                releaseInputBuffer(inputBuffer);
                return outputBuffer2;
            }
            releaseInputBuffer(inputBuffer);
        }
        return null;
    }

    private void releaseInputBuffer(CeaInputBuffer inputBuffer) {
        inputBuffer.clear();
        this.availableInputBuffers.add(inputBuffer);
    }

    protected void releaseOutputBuffer(SubtitleOutputBuffer outputBuffer) {
        outputBuffer.clear();
        this.availableOutputBuffers.add(outputBuffer);
    }

    @Override // androidx.media3.decoder.Decoder
    public void flush() {
        this.queuedInputBufferCount = 0L;
        this.playbackPositionUs = 0L;
        while (!this.queuedInputBuffers.isEmpty()) {
            releaseInputBuffer((CeaInputBuffer) Util.castNonNull(this.queuedInputBuffers.poll()));
        }
        if (this.dequeuedInputBuffer != null) {
            releaseInputBuffer(this.dequeuedInputBuffer);
            this.dequeuedInputBuffer = null;
        }
    }

    @Override // androidx.media3.decoder.Decoder
    public void release() {
    }

    protected final SubtitleOutputBuffer getAvailableOutputBuffer() {
        return this.availableOutputBuffers.pollFirst();
    }

    protected final long getPositionUs() {
        return this.playbackPositionUs;
    }

    private static final class CeaInputBuffer extends SubtitleInputBuffer implements Comparable<CeaInputBuffer> {
        private long queuedInputBufferCount;

        private CeaInputBuffer() {
        }

        @Override // java.lang.Comparable
        public int compareTo(CeaInputBuffer other) {
            if (isEndOfStream() != other.isEndOfStream()) {
                return isEndOfStream() ? 1 : -1;
            }
            long delta = this.timeUs - other.timeUs;
            if (delta == 0) {
                delta = this.queuedInputBufferCount - other.queuedInputBufferCount;
                if (delta == 0) {
                    return 0;
                }
            }
            return delta > 0 ? 1 : -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class CeaOutputBuffer extends SubtitleOutputBuffer {
        private DecoderOutputBuffer.Owner<CeaOutputBuffer> owner;

        public CeaOutputBuffer(DecoderOutputBuffer.Owner<CeaOutputBuffer> owner) {
            this.owner = owner;
        }

        @Override // androidx.media3.decoder.DecoderOutputBuffer
        public final void release() {
            this.owner.releaseOutputBuffer(this);
        }
    }
}
