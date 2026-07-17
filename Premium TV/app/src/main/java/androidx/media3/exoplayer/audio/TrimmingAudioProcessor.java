package androidx.media3.exoplayer.audio;

import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.util.Util;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
final class TrimmingAudioProcessor extends androidx.media3.common.audio.BaseAudioProcessor {
    private static final int OUTPUT_ENCODING = 2;
    private byte[] endBuffer = Util.EMPTY_BYTE_ARRAY;
    private int endBufferSize;
    private int pendingTrimStartBytes;
    private boolean reconfigurationPending;
    private int trimEndFrames;
    private int trimStartFrames;
    private long trimmedFrameCount;

    public void setTrimFrameCount(int trimStartFrames, int trimEndFrames) {
        this.trimStartFrames = trimStartFrames;
        this.trimEndFrames = trimEndFrames;
    }

    public void resetTrimmedFrameCount() {
        this.trimmedFrameCount = 0L;
    }

    public long getTrimmedFrameCount() {
        return this.trimmedFrameCount;
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor, androidx.media3.common.audio.AudioProcessor
    public long getDurationAfterProcessorApplied(long durationUs) {
        return durationUs - Util.sampleCountToDurationUs(this.trimEndFrames + this.trimStartFrames, this.inputAudioFormat.sampleRate);
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    public AudioProcessor.AudioFormat onConfigure(AudioProcessor.AudioFormat inputAudioFormat) throws AudioProcessor.UnhandledAudioFormatException {
        if (inputAudioFormat.encoding != 2) {
            throw new AudioProcessor.UnhandledAudioFormatException(inputAudioFormat);
        }
        this.reconfigurationPending = true;
        return (this.trimStartFrames == 0 && this.trimEndFrames == 0) ? AudioProcessor.AudioFormat.NOT_SET : inputAudioFormat;
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public void queueInput(ByteBuffer inputBuffer) {
        int position = inputBuffer.position();
        int limit = inputBuffer.limit();
        int remaining = limit - position;
        if (remaining == 0) {
            return;
        }
        int trimBytes = Math.min(remaining, this.pendingTrimStartBytes);
        this.trimmedFrameCount += (long) (trimBytes / this.inputAudioFormat.bytesPerFrame);
        this.pendingTrimStartBytes -= trimBytes;
        inputBuffer.position(position + trimBytes);
        if (this.pendingTrimStartBytes > 0) {
            return;
        }
        int remaining2 = remaining - trimBytes;
        int remainingBytesToOutput = (this.endBufferSize + remaining2) - this.endBuffer.length;
        ByteBuffer buffer = replaceOutputBuffer(remainingBytesToOutput);
        int endBufferBytesToOutput = Util.constrainValue(remainingBytesToOutput, 0, this.endBufferSize);
        buffer.put(this.endBuffer, 0, endBufferBytesToOutput);
        int inputBufferBytesToOutput = Util.constrainValue(remainingBytesToOutput - endBufferBytesToOutput, 0, remaining2);
        inputBuffer.limit(inputBuffer.position() + inputBufferBytesToOutput);
        buffer.put(inputBuffer);
        inputBuffer.limit(limit);
        int remaining3 = remaining2 - inputBufferBytesToOutput;
        this.endBufferSize -= endBufferBytesToOutput;
        System.arraycopy(this.endBuffer, endBufferBytesToOutput, this.endBuffer, 0, this.endBufferSize);
        inputBuffer.get(this.endBuffer, this.endBufferSize, remaining3);
        this.endBufferSize += remaining3;
        buffer.flip();
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor, androidx.media3.common.audio.AudioProcessor
    public ByteBuffer getOutput() {
        if (super.isEnded() && this.endBufferSize > 0) {
            replaceOutputBuffer(this.endBufferSize).put(this.endBuffer, 0, this.endBufferSize).flip();
            this.endBufferSize = 0;
        }
        return super.getOutput();
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor, androidx.media3.common.audio.AudioProcessor
    public boolean isEnded() {
        return super.isEnded() && this.endBufferSize == 0;
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    protected void onQueueEndOfStream() {
        if (this.reconfigurationPending) {
            if (this.endBufferSize > 0) {
                this.trimmedFrameCount += (long) (this.endBufferSize / this.inputAudioFormat.bytesPerFrame);
            }
            this.endBufferSize = 0;
        }
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    protected void onFlush() {
        if (this.reconfigurationPending) {
            this.reconfigurationPending = false;
            this.endBuffer = new byte[this.trimEndFrames * this.inputAudioFormat.bytesPerFrame];
            this.pendingTrimStartBytes = this.trimStartFrames * this.inputAudioFormat.bytesPerFrame;
        }
        this.endBufferSize = 0;
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    protected void onReset() {
        this.endBuffer = Util.EMPTY_BYTE_ARRAY;
    }
}
