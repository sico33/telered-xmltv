package androidx.media3.exoplayer.audio;

import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
public final class SilenceSkippingAudioProcessor extends androidx.media3.common.audio.BaseAudioProcessor {
    private static final int AVOID_TRUNCATION_FACTOR = 1000;
    public static final long DEFAULT_MAX_SILENCE_TO_KEEP_DURATION_US = 2000000;
    public static final long DEFAULT_MINIMUM_SILENCE_DURATION_US = 100000;
    public static final int DEFAULT_MIN_VOLUME_TO_KEEP_PERCENTAGE = 10;

    @Deprecated
    public static final long DEFAULT_PADDING_SILENCE_US = 20000;
    public static final float DEFAULT_SILENCE_RETENTION_RATIO = 0.2f;
    public static final short DEFAULT_SILENCE_THRESHOLD_LEVEL = 1024;
    private static final int DO_NOT_CHANGE_VOLUME = 3;
    private static final int FADE_IN = 2;
    private static final int FADE_OUT = 0;
    private static final int MUTE = 1;
    private static final int STATE_NOISY = 0;
    private static final int STATE_SHORTENING_SILENCE = 1;
    private int bytesPerFrame;
    private byte[] contiguousOutputBuffer;
    private boolean enabled;
    private final long maxSilenceToKeepDurationUs;
    private byte[] maybeSilenceBuffer;
    private int maybeSilenceBufferContentsSize;
    private int maybeSilenceBufferStartIndex;
    private final int minVolumeToKeepPercentageWhenMuting;
    private final long minimumSilenceDurationUs;
    private int outputSilenceFramesSinceNoise;
    private final float silenceRetentionRatio;
    private final short silenceThresholdLevel;
    private long skippedFrames;
    private int state;

    public SilenceSkippingAudioProcessor() {
        this(DEFAULT_MINIMUM_SILENCE_DURATION_US, 0.2f, DEFAULT_MAX_SILENCE_TO_KEEP_DURATION_US, 10, DEFAULT_SILENCE_THRESHOLD_LEVEL);
    }

    @Deprecated
    public SilenceSkippingAudioProcessor(long minimumSilenceDurationUs, long paddingSilenceUs, short silenceThresholdLevel) {
        this(minimumSilenceDurationUs, paddingSilenceUs / minimumSilenceDurationUs, minimumSilenceDurationUs, 0, silenceThresholdLevel);
    }

    public SilenceSkippingAudioProcessor(long minimumSilenceDurationUs, float silenceRetentionRatio, long maxSilenceToKeepDurationUs, int minVolumeToKeepPercentageWhenMuting, short silenceThresholdLevel) {
        boolean z = false;
        this.outputSilenceFramesSinceNoise = 0;
        this.maybeSilenceBufferStartIndex = 0;
        this.maybeSilenceBufferContentsSize = 0;
        if (silenceRetentionRatio >= 0.0f && silenceRetentionRatio <= 1.0f) {
            z = true;
        }
        Assertions.checkArgument(z);
        this.minimumSilenceDurationUs = minimumSilenceDurationUs;
        this.silenceRetentionRatio = silenceRetentionRatio;
        this.maxSilenceToKeepDurationUs = maxSilenceToKeepDurationUs;
        this.minVolumeToKeepPercentageWhenMuting = minVolumeToKeepPercentageWhenMuting;
        this.silenceThresholdLevel = silenceThresholdLevel;
        this.maybeSilenceBuffer = Util.EMPTY_BYTE_ARRAY;
        this.contiguousOutputBuffer = Util.EMPTY_BYTE_ARRAY;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getSkippedFrames() {
        return this.skippedFrames;
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    protected AudioProcessor.AudioFormat onConfigure(AudioProcessor.AudioFormat inputAudioFormat) throws AudioProcessor.UnhandledAudioFormatException {
        if (inputAudioFormat.encoding != 2) {
            throw new AudioProcessor.UnhandledAudioFormatException(inputAudioFormat);
        }
        if (inputAudioFormat.sampleRate == -1) {
            return AudioProcessor.AudioFormat.NOT_SET;
        }
        return inputAudioFormat;
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor, androidx.media3.common.audio.AudioProcessor
    public boolean isActive() {
        return super.isActive() && this.enabled;
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public void queueInput(ByteBuffer inputBuffer) {
        while (inputBuffer.hasRemaining() && !hasPendingOutput()) {
            switch (this.state) {
                case 0:
                    processNoisy(inputBuffer);
                    break;
                case 1:
                    shortenSilenceSilenceUntilNoise(inputBuffer);
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    public void onQueueEndOfStream() {
        if (this.maybeSilenceBufferContentsSize > 0) {
            outputShortenedSilenceBuffer(true);
            this.outputSilenceFramesSinceNoise = 0;
        }
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    public void onFlush() {
        if (isActive()) {
            this.bytesPerFrame = this.inputAudioFormat.channelCount * 2;
            int maybeSilenceBufferSize = alignToBytePerFrameBoundary(durationUsToFrames(this.minimumSilenceDurationUs) / 2) * 2;
            if (this.maybeSilenceBuffer.length != maybeSilenceBufferSize) {
                this.maybeSilenceBuffer = new byte[maybeSilenceBufferSize];
                this.contiguousOutputBuffer = new byte[maybeSilenceBufferSize];
            }
        }
        this.state = 0;
        this.skippedFrames = 0L;
        this.outputSilenceFramesSinceNoise = 0;
        this.maybeSilenceBufferStartIndex = 0;
        this.maybeSilenceBufferContentsSize = 0;
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    public void onReset() {
        this.enabled = false;
        this.maybeSilenceBuffer = Util.EMPTY_BYTE_ARRAY;
        this.contiguousOutputBuffer = Util.EMPTY_BYTE_ARRAY;
    }

    private void processNoisy(ByteBuffer inputBuffer) {
        int limit = inputBuffer.limit();
        inputBuffer.limit(Math.min(limit, inputBuffer.position() + this.maybeSilenceBuffer.length));
        int noiseLimit = findNoiseLimit(inputBuffer);
        if (noiseLimit == inputBuffer.position()) {
            this.state = 1;
        } else {
            inputBuffer.limit(Math.min(noiseLimit, inputBuffer.capacity()));
            output(inputBuffer);
        }
        inputBuffer.limit(limit);
    }

    private void shortenSilenceSilenceUntilNoise(ByteBuffer inputBuffer) {
        int indexToWriteTo;
        int amountInUpperPartOfBuffer;
        Assertions.checkState(this.maybeSilenceBufferStartIndex < this.maybeSilenceBuffer.length);
        int limit = inputBuffer.limit();
        int noisePosition = findNoisePosition(inputBuffer);
        int silenceInputSize = noisePosition - inputBuffer.position();
        int i = this.maybeSilenceBufferStartIndex + this.maybeSilenceBufferContentsSize;
        int length = this.maybeSilenceBuffer.length;
        byte[] bArr = this.maybeSilenceBuffer;
        if (i < length) {
            amountInUpperPartOfBuffer = bArr.length - (this.maybeSilenceBufferContentsSize + this.maybeSilenceBufferStartIndex);
            indexToWriteTo = this.maybeSilenceBufferStartIndex + this.maybeSilenceBufferContentsSize;
        } else {
            int contiguousBufferRemaining = bArr.length;
            int amountInUpperPartOfBuffer2 = contiguousBufferRemaining - this.maybeSilenceBufferStartIndex;
            indexToWriteTo = this.maybeSilenceBufferContentsSize - amountInUpperPartOfBuffer2;
            amountInUpperPartOfBuffer = this.maybeSilenceBufferStartIndex - indexToWriteTo;
        }
        boolean noiseFound = noisePosition < limit;
        int bytesOfInput = Math.min(silenceInputSize, amountInUpperPartOfBuffer);
        inputBuffer.limit(inputBuffer.position() + bytesOfInput);
        inputBuffer.get(this.maybeSilenceBuffer, indexToWriteTo, bytesOfInput);
        this.maybeSilenceBufferContentsSize += bytesOfInput;
        Assertions.checkState(this.maybeSilenceBufferContentsSize <= this.maybeSilenceBuffer.length);
        boolean shouldTransitionToNoisyState = noiseFound && silenceInputSize < amountInUpperPartOfBuffer;
        outputShortenedSilenceBuffer(shouldTransitionToNoisyState);
        if (shouldTransitionToNoisyState) {
            this.state = 0;
            this.outputSilenceFramesSinceNoise = 0;
        }
        inputBuffer.limit(limit);
    }

    private void outputShortenedSilenceBuffer(boolean shouldTransitionToNoisyState) {
        int bytesConsumed;
        int bytesToOutput;
        int sizeBeforeOutput = this.maybeSilenceBufferContentsSize;
        if (this.maybeSilenceBufferContentsSize == this.maybeSilenceBuffer.length || shouldTransitionToNoisyState) {
            if (this.outputSilenceFramesSinceNoise == 0) {
                if (shouldTransitionToNoisyState) {
                    bytesToOutput = this.maybeSilenceBufferContentsSize;
                    outputSilence(bytesToOutput, 3);
                    bytesConsumed = bytesToOutput;
                } else {
                    int volumeChangeType = this.maybeSilenceBufferContentsSize;
                    Assertions.checkState(volumeChangeType >= this.maybeSilenceBuffer.length / 2);
                    bytesToOutput = this.maybeSilenceBuffer.length / 2;
                    outputSilence(bytesToOutput, 0);
                    bytesConsumed = bytesToOutput;
                }
            } else {
                int i = this.maybeSilenceBufferContentsSize;
                if (shouldTransitionToNoisyState) {
                    int bytesRemainingAfterOutputtingHalfMin = i - (this.maybeSilenceBuffer.length / 2);
                    bytesConsumed = (this.maybeSilenceBuffer.length / 2) + bytesRemainingAfterOutputtingHalfMin;
                    int shortenedSilenceLength = calculateShortenedSilenceLength(bytesRemainingAfterOutputtingHalfMin);
                    int bytesToOutput2 = (this.maybeSilenceBuffer.length / 2) + shortenedSilenceLength;
                    outputSilence(bytesToOutput2, 2);
                    bytesToOutput = bytesToOutput2;
                } else {
                    bytesConsumed = i - (this.maybeSilenceBuffer.length / 2);
                    int bytesToOutput3 = calculateShortenedSilenceLength(bytesConsumed);
                    outputSilence(bytesToOutput3, 1);
                    bytesToOutput = bytesToOutput3;
                }
            }
            Assertions.checkState(bytesConsumed % this.bytesPerFrame == 0, "bytesConsumed is not aligned to frame size: %s" + bytesConsumed);
            Assertions.checkState(sizeBeforeOutput >= bytesToOutput);
            this.maybeSilenceBufferContentsSize -= bytesConsumed;
            this.maybeSilenceBufferStartIndex += bytesConsumed;
            this.maybeSilenceBufferStartIndex %= this.maybeSilenceBuffer.length;
            this.outputSilenceFramesSinceNoise += bytesToOutput / this.bytesPerFrame;
            this.skippedFrames += (long) ((bytesConsumed - bytesToOutput) / this.bytesPerFrame);
        }
    }

    private int calculateShortenedSilenceLength(int silenceToShortenBytes) {
        int bytesNeededToReachMax = ((durationUsToFrames(this.maxSilenceToKeepDurationUs) - this.outputSilenceFramesSinceNoise) * this.bytesPerFrame) - (this.maybeSilenceBuffer.length / 2);
        Assertions.checkState(bytesNeededToReachMax >= 0);
        return alignToBytePerFrameBoundary(Math.min((silenceToShortenBytes * this.silenceRetentionRatio) + 0.5f, bytesNeededToReachMax));
    }

    private int alignToBytePerFrameBoundary(int value) {
        return (value / this.bytesPerFrame) * this.bytesPerFrame;
    }

    private int alignToBytePerFrameBoundary(float value) {
        return alignToBytePerFrameBoundary((int) value);
    }

    private void outputRange(byte[] data, int size, int rampType) {
        Assertions.checkArgument(size % this.bytesPerFrame == 0, "byteOutput size is not aligned to frame size " + size);
        modifyVolume(data, size, rampType);
        replaceOutputBuffer(size).put(data, 0, size).flip();
    }

    private void outputSilence(int sizeToOutput, int rampType) {
        if (sizeToOutput == 0) {
            return;
        }
        Assertions.checkArgument(this.maybeSilenceBufferContentsSize >= sizeToOutput);
        int i = this.maybeSilenceBufferStartIndex;
        if (rampType == 2) {
            int i2 = i + this.maybeSilenceBufferContentsSize;
            int length = this.maybeSilenceBuffer.length;
            byte[] bArr = this.maybeSilenceBuffer;
            if (i2 <= length) {
                System.arraycopy(bArr, (this.maybeSilenceBufferStartIndex + this.maybeSilenceBufferContentsSize) - sizeToOutput, this.contiguousOutputBuffer, 0, sizeToOutput);
            } else {
                int sizeInUpperPartOfArray = bArr.length - this.maybeSilenceBufferStartIndex;
                int sizeInLowerPartOfArray = this.maybeSilenceBufferContentsSize - sizeInUpperPartOfArray;
                byte[] bArr2 = this.maybeSilenceBuffer;
                if (sizeInLowerPartOfArray >= sizeToOutput) {
                    System.arraycopy(bArr2, sizeInLowerPartOfArray - sizeToOutput, this.contiguousOutputBuffer, 0, sizeToOutput);
                } else {
                    int sizeToOutputInUpperPart = sizeToOutput - sizeInLowerPartOfArray;
                    System.arraycopy(bArr2, this.maybeSilenceBuffer.length - sizeToOutputInUpperPart, this.contiguousOutputBuffer, 0, sizeToOutputInUpperPart);
                    System.arraycopy(this.maybeSilenceBuffer, 0, this.contiguousOutputBuffer, sizeToOutputInUpperPart, sizeInLowerPartOfArray);
                }
            }
        } else {
            int i3 = i + sizeToOutput;
            int length2 = this.maybeSilenceBuffer.length;
            byte[] bArr3 = this.maybeSilenceBuffer;
            if (i3 <= length2) {
                System.arraycopy(bArr3, this.maybeSilenceBufferStartIndex, this.contiguousOutputBuffer, 0, sizeToOutput);
            } else {
                int sizeToCopyInUpperPartOfArray = bArr3.length - this.maybeSilenceBufferStartIndex;
                System.arraycopy(this.maybeSilenceBuffer, this.maybeSilenceBufferStartIndex, this.contiguousOutputBuffer, 0, sizeToCopyInUpperPartOfArray);
                int amountToCopyFromLowerPartOfArray = sizeToOutput - sizeToCopyInUpperPartOfArray;
                System.arraycopy(this.maybeSilenceBuffer, 0, this.contiguousOutputBuffer, sizeToCopyInUpperPartOfArray, amountToCopyFromLowerPartOfArray);
            }
        }
        Assertions.checkArgument(sizeToOutput % this.bytesPerFrame == 0, "sizeToOutput is not aligned to frame size: " + sizeToOutput);
        Assertions.checkState(this.maybeSilenceBufferStartIndex < this.maybeSilenceBuffer.length);
        outputRange(this.contiguousOutputBuffer, sizeToOutput, rampType);
    }

    private void modifyVolume(byte[] sampleBuffer, int size, int volumeChangeType) {
        int volumeModificationPercentage;
        if (volumeChangeType == 3) {
            return;
        }
        for (int idx = 0; idx < size; idx += 2) {
            byte mostSignificantByte = sampleBuffer[idx + 1];
            byte leastSignificantByte = sampleBuffer[idx];
            int sample = twoByteSampleToInt(mostSignificantByte, leastSignificantByte);
            if (volumeChangeType == 0) {
                volumeModificationPercentage = calculateFadeOutPercentage(idx, size - 1);
            } else if (volumeChangeType == 2) {
                volumeModificationPercentage = calculateFadeInPercentage(idx, size - 1);
            } else {
                volumeModificationPercentage = this.minVolumeToKeepPercentageWhenMuting;
            }
            sampleIntToTwoBigEndianBytes(sampleBuffer, idx, (sample * volumeModificationPercentage) / 100);
        }
    }

    private int calculateFadeOutPercentage(int value, int max) {
        return (((this.minVolumeToKeepPercentageWhenMuting - 100) * ((value * 1000) / max)) / 1000) + 100;
    }

    private int calculateFadeInPercentage(int value, int max) {
        return this.minVolumeToKeepPercentageWhenMuting + ((((100 - this.minVolumeToKeepPercentageWhenMuting) * (value * 1000)) / max) / 1000);
    }

    private static int twoByteSampleToInt(byte mostSignificantByte, byte leastSignificantByte) {
        return (leastSignificantByte & 255) | (mostSignificantByte << 8);
    }

    private static void sampleIntToTwoBigEndianBytes(byte[] byteArray, int startIndex, int sample) {
        if (sample >= 32767) {
            byteArray[startIndex] = -1;
            byteArray[startIndex + 1] = 127;
        } else if (sample <= -32768) {
            byteArray[startIndex] = 0;
            byteArray[startIndex + 1] = -128;
        } else {
            byteArray[startIndex] = (byte) (sample & 255);
            byteArray[startIndex + 1] = (byte) (sample >> 8);
        }
    }

    private void output(ByteBuffer data) {
        replaceOutputBuffer(data.remaining()).put(data).flip();
    }

    private int durationUsToFrames(long durationUs) {
        return (int) ((((long) this.inputAudioFormat.sampleRate) * durationUs) / 1000000);
    }

    private int findNoisePosition(ByteBuffer buffer) {
        for (int i = buffer.position() + 1; i < buffer.limit(); i += 2) {
            if (isNoise(buffer.get(i), buffer.get(i - 1))) {
                return this.bytesPerFrame * (i / this.bytesPerFrame);
            }
        }
        int i2 = buffer.limit();
        return i2;
    }

    private int findNoiseLimit(ByteBuffer buffer) {
        for (int i = buffer.limit() - 1; i >= buffer.position(); i -= 2) {
            if (isNoise(buffer.get(i), buffer.get(i - 1))) {
                return (this.bytesPerFrame * (i / this.bytesPerFrame)) + this.bytesPerFrame;
            }
        }
        int i2 = buffer.position();
        return i2;
    }

    private boolean isNoise(byte mostSignificantByte, byte leastSignificantByte) {
        return Math.abs(twoByteSampleToInt(mostSignificantByte, leastSignificantByte)) > this.silenceThresholdLevel;
    }
}
