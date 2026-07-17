package androidx.media3.exoplayer.audio;

import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.util.Assertions;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
final class ChannelMappingAudioProcessor extends androidx.media3.common.audio.BaseAudioProcessor {
    private int[] outputChannels;
    private int[] pendingOutputChannels;

    ChannelMappingAudioProcessor() {
    }

    public void setChannelMap(int[] outputChannels) {
        this.pendingOutputChannels = outputChannels;
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    public AudioProcessor.AudioFormat onConfigure(AudioProcessor.AudioFormat inputAudioFormat) throws AudioProcessor.UnhandledAudioFormatException {
        int[] outputChannels = this.pendingOutputChannels;
        if (outputChannels == null) {
            return AudioProcessor.AudioFormat.NOT_SET;
        }
        if (inputAudioFormat.encoding != 2) {
            throw new AudioProcessor.UnhandledAudioFormatException(inputAudioFormat);
        }
        boolean active = inputAudioFormat.channelCount != outputChannels.length;
        int i = 0;
        while (i < outputChannels.length) {
            int channelIndex = outputChannels[i];
            if (channelIndex >= inputAudioFormat.channelCount) {
                throw new AudioProcessor.UnhandledAudioFormatException(inputAudioFormat);
            }
            active |= channelIndex != i;
            i++;
        }
        if (active) {
            return new AudioProcessor.AudioFormat(inputAudioFormat.sampleRate, outputChannels.length, 2);
        }
        return AudioProcessor.AudioFormat.NOT_SET;
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public void queueInput(ByteBuffer inputBuffer) {
        int[] outputChannels = (int[]) Assertions.checkNotNull(this.outputChannels);
        int position = inputBuffer.position();
        int limit = inputBuffer.limit();
        int frameCount = (limit - position) / this.inputAudioFormat.bytesPerFrame;
        int outputSize = this.outputAudioFormat.bytesPerFrame * frameCount;
        ByteBuffer buffer = replaceOutputBuffer(outputSize);
        while (position < limit) {
            for (int channelIndex : outputChannels) {
                buffer.putShort(inputBuffer.getShort((channelIndex * 2) + position));
            }
            position += this.inputAudioFormat.bytesPerFrame;
        }
        inputBuffer.position(limit);
        buffer.flip();
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    protected void onFlush() {
        this.outputChannels = this.pendingOutputChannels;
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    protected void onReset() {
        this.outputChannels = null;
        this.pendingOutputChannels = null;
    }
}
