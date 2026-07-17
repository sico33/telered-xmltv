package androidx.media3.common.audio;

import android.util.SparseArray;
import androidx.media3.common.util.Assertions;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
public final class ChannelMixingAudioProcessor extends BaseAudioProcessor {
    private final SparseArray<ChannelMixingMatrix> matrixByInputChannelCount = new SparseArray<>();

    public void putChannelMixingMatrix(ChannelMixingMatrix matrix) {
        int inputChannelCount = matrix.getInputChannelCount();
        this.matrixByInputChannelCount.put(inputChannelCount, matrix);
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    protected AudioProcessor.AudioFormat onConfigure(AudioProcessor.AudioFormat inputAudioFormat) throws AudioProcessor.UnhandledAudioFormatException {
        if (inputAudioFormat.encoding != 2) {
            throw new AudioProcessor.UnhandledAudioFormatException(inputAudioFormat);
        }
        ChannelMixingMatrix channelMixingMatrix = this.matrixByInputChannelCount.get(inputAudioFormat.channelCount);
        if (channelMixingMatrix == null) {
            throw new AudioProcessor.UnhandledAudioFormatException("No mixing matrix for input channel count", inputAudioFormat);
        }
        if (channelMixingMatrix.isIdentity()) {
            return AudioProcessor.AudioFormat.NOT_SET;
        }
        return new AudioProcessor.AudioFormat(inputAudioFormat.sampleRate, channelMixingMatrix.getOutputChannelCount(), 2);
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public void queueInput(ByteBuffer inputBuffer) {
        ChannelMixingMatrix channelMixingMatrix = (ChannelMixingMatrix) Assertions.checkStateNotNull(this.matrixByInputChannelCount.get(this.inputAudioFormat.channelCount));
        int framesToMix = inputBuffer.remaining() / this.inputAudioFormat.bytesPerFrame;
        ByteBuffer outputBuffer = replaceOutputBuffer(this.outputAudioFormat.bytesPerFrame * framesToMix);
        AudioMixingUtil.mix(inputBuffer, this.inputAudioFormat, outputBuffer, this.outputAudioFormat, channelMixingMatrix, framesToMix, false, true);
        outputBuffer.flip();
    }
}
