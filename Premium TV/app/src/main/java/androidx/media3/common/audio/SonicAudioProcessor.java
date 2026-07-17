package androidx.media3.common.audio;

import android.support.v4.media.session.PlaybackStateCompat;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/* JADX INFO: loaded from: classes.dex */
public class SonicAudioProcessor implements AudioProcessor {
    private static final float CLOSE_THRESHOLD = 1.0E-4f;
    private static final int MIN_BYTES_FOR_DURATION_SCALING_CALCULATION = 1024;
    public static final int SAMPLE_RATE_NO_CHANGE = -1;
    private long inputBytes;
    private boolean inputEnded;
    private long outputBytes;
    private boolean pendingSonicRecreation;
    private Sonic sonic;
    private float speed = 1.0f;
    private float pitch = 1.0f;
    private AudioProcessor.AudioFormat pendingInputAudioFormat = AudioProcessor.AudioFormat.NOT_SET;
    private AudioProcessor.AudioFormat pendingOutputAudioFormat = AudioProcessor.AudioFormat.NOT_SET;
    private AudioProcessor.AudioFormat inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET;
    private AudioProcessor.AudioFormat outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET;
    private ByteBuffer buffer = EMPTY_BUFFER;
    private ShortBuffer shortBuffer = this.buffer.asShortBuffer();
    private ByteBuffer outputBuffer = EMPTY_BUFFER;
    private int pendingOutputSampleRate = -1;

    public final void setSpeed(float speed) {
        if (this.speed != speed) {
            this.speed = speed;
            this.pendingSonicRecreation = true;
        }
    }

    public final void setPitch(float pitch) {
        if (this.pitch != pitch) {
            this.pitch = pitch;
            this.pendingSonicRecreation = true;
        }
    }

    public final void setOutputSampleRateHz(int sampleRateHz) {
        this.pendingOutputSampleRate = sampleRateHz;
    }

    public final long getMediaDuration(long playoutDuration) {
        if (this.outputBytes >= PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID) {
            long processedInputBytes = this.inputBytes - ((long) ((Sonic) Assertions.checkNotNull(this.sonic)).getPendingInputBytes());
            if (this.outputAudioFormat.sampleRate == this.inputAudioFormat.sampleRate) {
                return Util.scaleLargeTimestamp(playoutDuration, processedInputBytes, this.outputBytes);
            }
            return Util.scaleLargeTimestamp(playoutDuration, processedInputBytes * ((long) this.outputAudioFormat.sampleRate), ((long) this.inputAudioFormat.sampleRate) * this.outputBytes);
        }
        return (long) (((double) this.speed) * playoutDuration);
    }

    public final long getPlayoutDuration(long mediaDuration) {
        if (this.outputBytes >= PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID) {
            long processedInputBytes = this.inputBytes - ((long) ((Sonic) Assertions.checkNotNull(this.sonic)).getPendingInputBytes());
            int i = this.outputAudioFormat.sampleRate;
            int i2 = this.inputAudioFormat.sampleRate;
            long j = this.outputBytes;
            if (i == i2) {
                return Util.scaleLargeTimestamp(mediaDuration, j, processedInputBytes);
            }
            return Util.scaleLargeTimestamp(mediaDuration, j * ((long) this.inputAudioFormat.sampleRate), processedInputBytes * ((long) this.outputAudioFormat.sampleRate));
        }
        return (long) (mediaDuration / ((double) this.speed));
    }

    public final long getProcessedInputBytes() {
        return this.inputBytes - ((long) ((Sonic) Assertions.checkNotNull(this.sonic)).getPendingInputBytes());
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public long getDurationAfterProcessorApplied(long durationUs) {
        return getPlayoutDuration(durationUs);
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public final AudioProcessor.AudioFormat configure(AudioProcessor.AudioFormat inputAudioFormat) throws AudioProcessor.UnhandledAudioFormatException {
        int outputSampleRateHz;
        if (inputAudioFormat.encoding != 2) {
            throw new AudioProcessor.UnhandledAudioFormatException(inputAudioFormat);
        }
        if (this.pendingOutputSampleRate == -1) {
            outputSampleRateHz = inputAudioFormat.sampleRate;
        } else {
            outputSampleRateHz = this.pendingOutputSampleRate;
        }
        this.pendingInputAudioFormat = inputAudioFormat;
        this.pendingOutputAudioFormat = new AudioProcessor.AudioFormat(outputSampleRateHz, inputAudioFormat.channelCount, 2);
        this.pendingSonicRecreation = true;
        return this.pendingOutputAudioFormat;
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public final boolean isActive() {
        return this.pendingOutputAudioFormat.sampleRate != -1 && (Math.abs(this.speed - 1.0f) >= CLOSE_THRESHOLD || Math.abs(this.pitch - 1.0f) >= CLOSE_THRESHOLD || this.pendingOutputAudioFormat.sampleRate != this.pendingInputAudioFormat.sampleRate);
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public final void queueInput(ByteBuffer inputBuffer) {
        if (!inputBuffer.hasRemaining()) {
            return;
        }
        Sonic sonic = (Sonic) Assertions.checkNotNull(this.sonic);
        ShortBuffer shortBuffer = inputBuffer.asShortBuffer();
        int inputSize = inputBuffer.remaining();
        this.inputBytes += (long) inputSize;
        sonic.queueInput(shortBuffer);
        inputBuffer.position(inputBuffer.position() + inputSize);
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public final void queueEndOfStream() {
        if (this.sonic != null) {
            this.sonic.queueEndOfStream();
        }
        this.inputEnded = true;
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public final ByteBuffer getOutput() {
        int outputSize;
        Sonic sonic = this.sonic;
        if (sonic != null && (outputSize = sonic.getOutputSize()) > 0) {
            if (this.buffer.capacity() < outputSize) {
                this.buffer = ByteBuffer.allocateDirect(outputSize).order(ByteOrder.nativeOrder());
                this.shortBuffer = this.buffer.asShortBuffer();
            } else {
                this.buffer.clear();
                this.shortBuffer.clear();
            }
            sonic.getOutput(this.shortBuffer);
            this.outputBytes += (long) outputSize;
            this.buffer.limit(outputSize);
            this.outputBuffer = this.buffer;
        }
        ByteBuffer outputBuffer = this.outputBuffer;
        this.outputBuffer = EMPTY_BUFFER;
        return outputBuffer;
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public final boolean isEnded() {
        return this.inputEnded && (this.sonic == null || this.sonic.getOutputSize() == 0);
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public final void flush() {
        if (isActive()) {
            this.inputAudioFormat = this.pendingInputAudioFormat;
            this.outputAudioFormat = this.pendingOutputAudioFormat;
            if (this.pendingSonicRecreation) {
                this.sonic = new Sonic(this.inputAudioFormat.sampleRate, this.inputAudioFormat.channelCount, this.speed, this.pitch, this.outputAudioFormat.sampleRate);
            } else if (this.sonic != null) {
                this.sonic.flush();
            }
        }
        this.outputBuffer = EMPTY_BUFFER;
        this.inputBytes = 0L;
        this.outputBytes = 0L;
        this.inputEnded = false;
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public final void reset() {
        this.speed = 1.0f;
        this.pitch = 1.0f;
        this.pendingInputAudioFormat = AudioProcessor.AudioFormat.NOT_SET;
        this.pendingOutputAudioFormat = AudioProcessor.AudioFormat.NOT_SET;
        this.inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET;
        this.outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET;
        this.buffer = EMPTY_BUFFER;
        this.shortBuffer = this.buffer.asShortBuffer();
        this.outputBuffer = EMPTY_BUFFER;
        this.pendingOutputSampleRate = -1;
        this.pendingSonicRecreation = false;
        this.sonic = null;
        this.inputBytes = 0L;
        this.outputBytes = 0L;
        this.inputEnded = false;
    }
}
