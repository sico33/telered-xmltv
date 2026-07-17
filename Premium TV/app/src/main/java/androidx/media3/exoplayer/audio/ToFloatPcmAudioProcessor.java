package androidx.media3.exoplayer.audio;

import androidx.media3.common.C;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.util.Util;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
final class ToFloatPcmAudioProcessor extends androidx.media3.common.audio.BaseAudioProcessor {
    private static final int FLOAT_NAN_AS_INT = Float.floatToIntBits(Float.NaN);
    private static final double PCM_32_BIT_INT_TO_PCM_32_BIT_FLOAT_FACTOR = 4.656612875245797E-10d;

    ToFloatPcmAudioProcessor() {
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    public AudioProcessor.AudioFormat onConfigure(AudioProcessor.AudioFormat inputAudioFormat) throws AudioProcessor.UnhandledAudioFormatException {
        int encoding = inputAudioFormat.encoding;
        if (!Util.isEncodingHighResolutionPcm(encoding)) {
            throw new AudioProcessor.UnhandledAudioFormatException(inputAudioFormat);
        }
        if (encoding != 4) {
            return new AudioProcessor.AudioFormat(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, 4);
        }
        return AudioProcessor.AudioFormat.NOT_SET;
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public void queueInput(ByteBuffer inputBuffer) {
        ByteBuffer buffer;
        int position = inputBuffer.position();
        int limit = inputBuffer.limit();
        int size = limit - position;
        switch (this.inputAudioFormat.encoding) {
            case 21:
                buffer = replaceOutputBuffer((size / 3) * 4);
                for (int i = position; i < limit; i += 3) {
                    int pcm32BitInteger = ((inputBuffer.get(i) & 255) << 8) | ((inputBuffer.get(i + 1) & 255) << 16) | ((inputBuffer.get(i + 2) & 255) << 24);
                    writePcm32BitFloat(pcm32BitInteger, buffer);
                }
                break;
            case 22:
                buffer = replaceOutputBuffer(size);
                for (int i2 = position; i2 < limit; i2 += 4) {
                    int pcm32BitInteger2 = (inputBuffer.get(i2) & 255) | ((inputBuffer.get(i2 + 1) & 255) << 8) | ((inputBuffer.get(i2 + 2) & 255) << 16) | ((inputBuffer.get(i2 + 3) & 255) << 24);
                    writePcm32BitFloat(pcm32BitInteger2, buffer);
                }
                break;
            case C.ENCODING_PCM_24BIT_BIG_ENDIAN /* 1342177280 */:
                buffer = replaceOutputBuffer((size / 3) * 4);
                for (int i3 = position; i3 < limit; i3 += 3) {
                    int pcm32BitInteger3 = ((inputBuffer.get(i3 + 2) & 255) << 8) | ((inputBuffer.get(i3 + 1) & 255) << 16) | ((inputBuffer.get(i3) & 255) << 24);
                    writePcm32BitFloat(pcm32BitInteger3, buffer);
                }
                break;
            case C.ENCODING_PCM_32BIT_BIG_ENDIAN /* 1610612736 */:
                buffer = replaceOutputBuffer(size);
                for (int i4 = position; i4 < limit; i4 += 4) {
                    int pcm32BitInteger4 = (inputBuffer.get(i4 + 3) & 255) | ((inputBuffer.get(i4 + 2) & 255) << 8) | ((inputBuffer.get(i4 + 1) & 255) << 16) | ((inputBuffer.get(i4) & 255) << 24);
                    writePcm32BitFloat(pcm32BitInteger4, buffer);
                }
                break;
            default:
                throw new IllegalStateException();
        }
        inputBuffer.position(inputBuffer.limit());
        buffer.flip();
    }

    private static void writePcm32BitFloat(int pcm32BitInt, ByteBuffer buffer) {
        float pcm32BitFloat = (float) (((double) pcm32BitInt) * PCM_32_BIT_INT_TO_PCM_32_BIT_FLOAT_FACTOR);
        int floatBits = Float.floatToIntBits(pcm32BitFloat);
        if (floatBits == FLOAT_NAN_AS_INT) {
            floatBits = Float.floatToIntBits(0.0f);
        }
        buffer.putInt(floatBits);
    }
}
