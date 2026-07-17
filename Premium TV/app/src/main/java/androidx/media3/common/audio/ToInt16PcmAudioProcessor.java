package androidx.media3.common.audio;

import androidx.media3.common.C;
import androidx.media3.common.util.Util;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
public final class ToInt16PcmAudioProcessor extends BaseAudioProcessor {
    @Override // androidx.media3.common.audio.BaseAudioProcessor
    public AudioProcessor.AudioFormat onConfigure(AudioProcessor.AudioFormat inputAudioFormat) throws AudioProcessor.UnhandledAudioFormatException {
        int encoding = inputAudioFormat.encoding;
        if (encoding != 3 && encoding != 2 && encoding != 268435456 && encoding != 21 && encoding != 1342177280 && encoding != 22 && encoding != 1610612736 && encoding != 4) {
            throw new AudioProcessor.UnhandledAudioFormatException(inputAudioFormat);
        }
        if (encoding != 2) {
            return new AudioProcessor.AudioFormat(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, 2);
        }
        return AudioProcessor.AudioFormat.NOT_SET;
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public void queueInput(ByteBuffer inputBuffer) {
        int resampledSize;
        int position = inputBuffer.position();
        int limit = inputBuffer.limit();
        int size = limit - position;
        switch (this.inputAudioFormat.encoding) {
            case 3:
                resampledSize = size * 2;
                break;
            case 4:
            case 22:
            case C.ENCODING_PCM_32BIT_BIG_ENDIAN /* 1610612736 */:
                resampledSize = size / 2;
                break;
            case 21:
            case C.ENCODING_PCM_24BIT_BIG_ENDIAN /* 1342177280 */:
                int resampledSize2 = size / 3;
                resampledSize = resampledSize2 * 2;
                break;
            case 268435456:
                resampledSize = size;
                break;
            default:
                throw new IllegalStateException();
        }
        ByteBuffer buffer = replaceOutputBuffer(resampledSize);
        switch (this.inputAudioFormat.encoding) {
            case 3:
                for (int i = position; i < limit; i++) {
                    buffer.put((byte) 0);
                    buffer.put((byte) ((inputBuffer.get(i) & 255) - 128));
                }
                break;
            case 4:
                for (int i2 = position; i2 < limit; i2 += 4) {
                    float floatValue = Util.constrainValue(inputBuffer.getFloat(i2), -1.0f, 1.0f);
                    short shortValue = (short) (32767.0f * floatValue);
                    buffer.put((byte) (shortValue & 255));
                    buffer.put((byte) ((shortValue >> 8) & 255));
                }
                break;
            case 21:
                for (int i3 = position; i3 < limit; i3 += 3) {
                    buffer.put(inputBuffer.get(i3 + 1));
                    buffer.put(inputBuffer.get(i3 + 2));
                }
                break;
            case 22:
                for (int i4 = position; i4 < limit; i4 += 4) {
                    buffer.put(inputBuffer.get(i4 + 2));
                    buffer.put(inputBuffer.get(i4 + 3));
                }
                break;
            case 268435456:
                for (int i5 = position; i5 < limit; i5 += 2) {
                    buffer.put(inputBuffer.get(i5 + 1));
                    buffer.put(inputBuffer.get(i5));
                }
                break;
            case C.ENCODING_PCM_24BIT_BIG_ENDIAN /* 1342177280 */:
                for (int i6 = position; i6 < limit; i6 += 3) {
                    buffer.put(inputBuffer.get(i6 + 1));
                    buffer.put(inputBuffer.get(i6));
                }
                break;
            case C.ENCODING_PCM_32BIT_BIG_ENDIAN /* 1610612736 */:
                for (int i7 = position; i7 < limit; i7 += 4) {
                    buffer.put(inputBuffer.get(i7 + 1));
                    buffer.put(inputBuffer.get(i7));
                }
                break;
            default:
                throw new IllegalStateException();
        }
        inputBuffer.position(inputBuffer.limit());
        buffer.flip();
    }
}
