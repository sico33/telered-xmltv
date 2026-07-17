package androidx.media3.common.audio;

import androidx.media3.common.util.Util;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
public final class AudioMixingUtil {
    private static final float FLOAT_PCM_MAX_VALUE = 1.0f;
    private static final float FLOAT_PCM_MIN_VALUE = -1.0f;

    public static boolean canMix(AudioProcessor.AudioFormat audioFormat) {
        if (audioFormat.sampleRate == -1 || audioFormat.channelCount == -1) {
            return false;
        }
        return audioFormat.encoding == 2 || audioFormat.encoding == 4;
    }

    public static boolean canMix(AudioProcessor.AudioFormat inputAudioFormat, AudioProcessor.AudioFormat outputAudioFormat) {
        return inputAudioFormat.sampleRate == outputAudioFormat.sampleRate && canMix(inputAudioFormat) && canMix(outputAudioFormat);
    }

    public static ByteBuffer mix(ByteBuffer inputBuffer, AudioProcessor.AudioFormat inputAudioFormat, ByteBuffer mixingBuffer, AudioProcessor.AudioFormat mixingAudioFormat, ChannelMixingMatrix matrix, int framesToMix, boolean accumulate, boolean clipFloatOutput) {
        float fConstrainValue;
        boolean int16Input = inputAudioFormat.encoding == 2;
        boolean int16Output = mixingAudioFormat.encoding == 2;
        int inputChannels = matrix.getInputChannelCount();
        int outputChannels = matrix.getOutputChannelCount();
        float[] inputFrame = new float[inputChannels];
        float[] outputFrame = new float[outputChannels];
        for (int i = 0; i < framesToMix; i++) {
            if (accumulate) {
                int position = mixingBuffer.position();
                for (int outputChannel = 0; outputChannel < outputChannels; outputChannel++) {
                    outputFrame[outputChannel] = getPcmSample(mixingBuffer, int16Output, int16Output);
                }
                mixingBuffer.position(position);
            }
            for (int inputChannel = 0; inputChannel < inputChannels; inputChannel++) {
                inputFrame[inputChannel] = getPcmSample(inputBuffer, int16Input, int16Output);
            }
            for (int outputChannel2 = 0; outputChannel2 < outputChannels; outputChannel2++) {
                for (int inputChannel2 = 0; inputChannel2 < inputChannels; inputChannel2++) {
                    outputFrame[outputChannel2] = outputFrame[outputChannel2] + (inputFrame[inputChannel2] * matrix.getMixingCoefficient(inputChannel2, outputChannel2));
                }
                if (int16Output) {
                    mixingBuffer.putShort((short) Util.constrainValue(outputFrame[outputChannel2], -32768.0f, 32767.0f));
                } else {
                    if (clipFloatOutput) {
                        fConstrainValue = Util.constrainValue(outputFrame[outputChannel2], FLOAT_PCM_MIN_VALUE, 1.0f);
                    } else {
                        fConstrainValue = outputFrame[outputChannel2];
                    }
                    mixingBuffer.putFloat(fConstrainValue);
                }
                outputFrame[outputChannel2] = 0.0f;
            }
        }
        return mixingBuffer;
    }

    private static float getPcmSample(ByteBuffer buffer, boolean int16Buffer, boolean int16Output) {
        if (int16Output) {
            return int16Buffer ? buffer.getShort() : floatSampleToInt16Pcm(buffer.getFloat());
        }
        return int16Buffer ? int16SampleToFloatPcm(buffer.getShort()) : buffer.getFloat();
    }

    private static float floatSampleToInt16Pcm(float floatPcmValue) {
        return Util.constrainValue((floatPcmValue < 0.0f ? 32768 : 32767) * floatPcmValue, -32768.0f, 32767.0f);
    }

    private static float int16SampleToFloatPcm(short shortPcmValue) {
        return shortPcmValue / (shortPcmValue < 0 ? 32768 : 32767);
    }

    private AudioMixingUtil() {
    }
}
