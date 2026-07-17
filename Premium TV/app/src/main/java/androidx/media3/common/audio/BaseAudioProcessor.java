package androidx.media3.common.audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* JADX INFO: loaded from: classes.dex */
public abstract class BaseAudioProcessor implements AudioProcessor {
    private boolean inputEnded;
    private ByteBuffer buffer = EMPTY_BUFFER;
    private ByteBuffer outputBuffer = EMPTY_BUFFER;
    private AudioProcessor.AudioFormat pendingInputAudioFormat = AudioProcessor.AudioFormat.NOT_SET;
    private AudioProcessor.AudioFormat pendingOutputAudioFormat = AudioProcessor.AudioFormat.NOT_SET;
    protected AudioProcessor.AudioFormat inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET;
    protected AudioProcessor.AudioFormat outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET;

    @Override // androidx.media3.common.audio.AudioProcessor
    public /* synthetic */ long getDurationAfterProcessorApplied(long j) {
        return AudioProcessor.CC.$default$getDurationAfterProcessorApplied(this, j);
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public final AudioProcessor.AudioFormat configure(AudioProcessor.AudioFormat inputAudioFormat) throws AudioProcessor.UnhandledAudioFormatException {
        this.pendingInputAudioFormat = inputAudioFormat;
        this.pendingOutputAudioFormat = onConfigure(inputAudioFormat);
        return isActive() ? this.pendingOutputAudioFormat : AudioProcessor.AudioFormat.NOT_SET;
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public boolean isActive() {
        return this.pendingOutputAudioFormat != AudioProcessor.AudioFormat.NOT_SET;
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public final void queueEndOfStream() {
        this.inputEnded = true;
        onQueueEndOfStream();
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public ByteBuffer getOutput() {
        ByteBuffer outputBuffer = this.outputBuffer;
        this.outputBuffer = EMPTY_BUFFER;
        return outputBuffer;
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public boolean isEnded() {
        return this.inputEnded && this.outputBuffer == EMPTY_BUFFER;
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public final void flush() {
        this.outputBuffer = EMPTY_BUFFER;
        this.inputEnded = false;
        this.inputAudioFormat = this.pendingInputAudioFormat;
        this.outputAudioFormat = this.pendingOutputAudioFormat;
        onFlush();
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public final void reset() {
        flush();
        this.buffer = EMPTY_BUFFER;
        this.pendingInputAudioFormat = AudioProcessor.AudioFormat.NOT_SET;
        this.pendingOutputAudioFormat = AudioProcessor.AudioFormat.NOT_SET;
        this.inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET;
        this.outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET;
        onReset();
    }

    protected final ByteBuffer replaceOutputBuffer(int size) {
        if (this.buffer.capacity() < size) {
            this.buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        } else {
            this.buffer.clear();
        }
        this.outputBuffer = this.buffer;
        return this.buffer;
    }

    protected final boolean hasPendingOutput() {
        return this.outputBuffer.hasRemaining();
    }

    protected AudioProcessor.AudioFormat onConfigure(AudioProcessor.AudioFormat inputAudioFormat) throws AudioProcessor.UnhandledAudioFormatException {
        return AudioProcessor.AudioFormat.NOT_SET;
    }

    protected void onQueueEndOfStream() {
    }

    protected void onFlush() {
    }

    protected void onReset() {
    }
}
