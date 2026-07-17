package androidx.media3.common.audio;

import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.LongArray;
import androidx.media3.common.util.LongArrayQueue;
import androidx.media3.common.util.SpeedProviderUtil;
import androidx.media3.common.util.TimestampConsumer;
import androidx.media3.common.util.Util;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class SpeedChangingAudioProcessor extends BaseAudioProcessor {
    private long bytesRead;
    private float currentSpeed;
    private boolean endOfStreamQueuedToSonic;
    private LongArray inputSegmentStartTimesUs;
    private long lastProcessedInputTimeUs;
    private long lastSpeedAdjustedInputTimeUs;
    private long lastSpeedAdjustedOutputTimeUs;
    private LongArray outputSegmentStartTimesUs;
    private final SpeedProvider speedProvider;
    private final Object lock = new Object();
    private final SynchronizedSonicAudioProcessor sonicAudioProcessor = new SynchronizedSonicAudioProcessor(this.lock);
    private final LongArrayQueue pendingCallbackInputTimesUs = new LongArrayQueue();
    private final Queue<TimestampConsumer> pendingCallbacks = new ArrayDeque();
    private long speedAdjustedTimeAsyncInputTimeUs = C.TIME_UNSET;

    public SpeedChangingAudioProcessor(SpeedProvider speedProvider) {
        this.speedProvider = speedProvider;
        resetState();
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor, androidx.media3.common.audio.AudioProcessor
    public long getDurationAfterProcessorApplied(long durationUs) {
        return SpeedProviderUtil.getDurationAfterSpeedProviderApplied(this.speedProvider, durationUs);
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    public AudioProcessor.AudioFormat onConfigure(AudioProcessor.AudioFormat inputAudioFormat) throws AudioProcessor.UnhandledAudioFormatException {
        return this.sonicAudioProcessor.configure(inputAudioFormat);
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public void queueInput(ByteBuffer inputBuffer) {
        int bytesToNextSpeedChange;
        long timeUs = Util.scaleLargeTimestamp(this.bytesRead, 1000000L, ((long) this.inputAudioFormat.bytesPerFrame) * ((long) this.inputAudioFormat.sampleRate));
        float newSpeed = this.speedProvider.getSpeed(timeUs);
        updateSpeed(newSpeed, timeUs);
        int inputBufferLimit = inputBuffer.limit();
        long nextSpeedChangeTimeUs = this.speedProvider.getNextSpeedChangeTimeUs(timeUs);
        if (nextSpeedChangeTimeUs != C.TIME_UNSET) {
            bytesToNextSpeedChange = (int) Util.scaleLargeValue(nextSpeedChangeTimeUs - timeUs, ((long) this.inputAudioFormat.sampleRate) * ((long) this.inputAudioFormat.bytesPerFrame), 1000000L, RoundingMode.CEILING);
            int bytesToNextFrame = this.inputAudioFormat.bytesPerFrame - (bytesToNextSpeedChange % this.inputAudioFormat.bytesPerFrame);
            if (bytesToNextFrame != this.inputAudioFormat.bytesPerFrame) {
                bytesToNextSpeedChange += bytesToNextFrame;
            }
            inputBuffer.limit(Math.min(inputBufferLimit, inputBuffer.position() + bytesToNextSpeedChange));
        } else {
            bytesToNextSpeedChange = -1;
        }
        long startPosition = inputBuffer.position();
        if (isUsingSonic()) {
            this.sonicAudioProcessor.queueInput(inputBuffer);
            if (bytesToNextSpeedChange != -1 && ((long) inputBuffer.position()) - startPosition == bytesToNextSpeedChange) {
                this.sonicAudioProcessor.queueEndOfStream();
                this.endOfStreamQueuedToSonic = true;
            }
        } else {
            ByteBuffer buffer = replaceOutputBuffer(inputBuffer.remaining());
            if (inputBuffer.hasRemaining()) {
                buffer.put(inputBuffer);
            }
            buffer.flip();
        }
        this.bytesRead += ((long) inputBuffer.position()) - startPosition;
        updateLastProcessedInputTime();
        inputBuffer.limit(inputBufferLimit);
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    protected void onQueueEndOfStream() {
        if (!this.endOfStreamQueuedToSonic) {
            this.sonicAudioProcessor.queueEndOfStream();
            this.endOfStreamQueuedToSonic = true;
        }
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor, androidx.media3.common.audio.AudioProcessor
    public ByteBuffer getOutput() {
        ByteBuffer output = isUsingSonic() ? this.sonicAudioProcessor.getOutput() : super.getOutput();
        processPendingCallbacks();
        return output;
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor, androidx.media3.common.audio.AudioProcessor
    public boolean isEnded() {
        return super.isEnded() && this.sonicAudioProcessor.isEnded();
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    protected void onFlush() {
        resetState();
        this.sonicAudioProcessor.flush();
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    protected void onReset() {
        resetState();
        this.sonicAudioProcessor.reset();
    }

    public void getSpeedAdjustedTimeAsync(long inputTimeUs, TimestampConsumer callback) {
        synchronized (this.lock) {
            Assertions.checkArgument(this.speedAdjustedTimeAsyncInputTimeUs < inputTimeUs);
            this.speedAdjustedTimeAsyncInputTimeUs = inputTimeUs;
            if ((inputTimeUs <= this.lastProcessedInputTimeUs && this.pendingCallbackInputTimesUs.isEmpty()) || isEnded()) {
                callback.onTimestamp(calculateSpeedAdjustedTime(inputTimeUs));
            } else {
                this.pendingCallbackInputTimesUs.add(inputTimeUs);
                this.pendingCallbacks.add(callback);
            }
        }
    }

    public long getMediaDurationUs(long playoutDurationUs) {
        long lastSegmentInputDurationUs;
        long j;
        synchronized (this.lock) {
            int floorIndex = this.outputSegmentStartTimesUs.size() - 1;
            while (floorIndex > 0 && this.outputSegmentStartTimesUs.get(floorIndex) > playoutDurationUs) {
                floorIndex--;
            }
            long lastSegmentOutputDurationUs = playoutDurationUs - this.outputSegmentStartTimesUs.get(floorIndex);
            if (floorIndex == this.outputSegmentStartTimesUs.size() - 1) {
                lastSegmentInputDurationUs = getMediaDurationUsAtCurrentSpeed(lastSegmentOutputDurationUs);
            } else {
                lastSegmentInputDurationUs = Math.round(lastSegmentOutputDurationUs * divide(this.inputSegmentStartTimesUs.get(floorIndex + 1) - this.inputSegmentStartTimesUs.get(floorIndex), this.outputSegmentStartTimesUs.get(floorIndex + 1) - this.outputSegmentStartTimesUs.get(floorIndex)));
            }
            j = this.inputSegmentStartTimesUs.get(floorIndex) + lastSegmentInputDurationUs;
        }
        return j;
    }

    private long calculateSpeedAdjustedTime(long inputTimeUs) {
        long lastSegmentInputDurationUs;
        int floorIndex = this.inputSegmentStartTimesUs.size() - 1;
        while (floorIndex > 0 && this.inputSegmentStartTimesUs.get(floorIndex) > inputTimeUs) {
            floorIndex--;
        }
        int size = this.inputSegmentStartTimesUs.size() - 1;
        long j = this.lastSpeedAdjustedInputTimeUs;
        if (floorIndex == size) {
            if (j < this.inputSegmentStartTimesUs.get(floorIndex)) {
                this.lastSpeedAdjustedInputTimeUs = this.inputSegmentStartTimesUs.get(floorIndex);
                this.lastSpeedAdjustedOutputTimeUs = this.outputSegmentStartTimesUs.get(floorIndex);
            }
            long lastSegmentInputDurationUs2 = inputTimeUs - this.lastSpeedAdjustedInputTimeUs;
            lastSegmentInputDurationUs = getPlayoutDurationUsAtCurrentSpeed(lastSegmentInputDurationUs2);
        } else {
            long lastSegmentInputDurationUs3 = inputTimeUs - j;
            lastSegmentInputDurationUs = Math.round(lastSegmentInputDurationUs3 * divide(this.outputSegmentStartTimesUs.get(floorIndex + 1) - this.outputSegmentStartTimesUs.get(floorIndex), this.inputSegmentStartTimesUs.get(floorIndex + 1) - this.inputSegmentStartTimesUs.get(floorIndex)));
        }
        this.lastSpeedAdjustedInputTimeUs = inputTimeUs;
        this.lastSpeedAdjustedOutputTimeUs += lastSegmentInputDurationUs;
        return this.lastSpeedAdjustedOutputTimeUs;
    }

    private static double divide(long dividend, long divisor) {
        return dividend / divisor;
    }

    private void processPendingCallbacks() {
        synchronized (this.lock) {
            while (!this.pendingCallbacks.isEmpty() && (this.pendingCallbackInputTimesUs.element() <= this.lastProcessedInputTimeUs || isEnded())) {
                this.pendingCallbacks.remove().onTimestamp(calculateSpeedAdjustedTime(this.pendingCallbackInputTimesUs.remove()));
            }
        }
    }

    private void updateSpeed(float newSpeed, long timeUs) {
        synchronized (this.lock) {
            if (newSpeed != this.currentSpeed) {
                updateSpeedChangeArrays(timeUs);
                this.currentSpeed = newSpeed;
                if (isUsingSonic()) {
                    this.sonicAudioProcessor.setSpeed(newSpeed);
                    this.sonicAudioProcessor.setPitch(newSpeed);
                }
                this.sonicAudioProcessor.flush();
                this.endOfStreamQueuedToSonic = false;
                super.getOutput();
            }
        }
    }

    private void updateSpeedChangeArrays(long currentSpeedChangeInputTimeUs) {
        long lastSpeedChangeOutputTimeUs = this.outputSegmentStartTimesUs.get(this.outputSegmentStartTimesUs.size() - 1);
        long lastSpeedChangeInputTimeUs = this.inputSegmentStartTimesUs.get(this.inputSegmentStartTimesUs.size() - 1);
        long lastSpeedSegmentMediaDurationUs = currentSpeedChangeInputTimeUs - lastSpeedChangeInputTimeUs;
        this.inputSegmentStartTimesUs.add(currentSpeedChangeInputTimeUs);
        this.outputSegmentStartTimesUs.add(getPlayoutDurationUsAtCurrentSpeed(lastSpeedSegmentMediaDurationUs) + lastSpeedChangeOutputTimeUs);
    }

    private long getPlayoutDurationUsAtCurrentSpeed(long mediaDurationUs) {
        if (isUsingSonic()) {
            return this.sonicAudioProcessor.getPlayoutDuration(mediaDurationUs);
        }
        return mediaDurationUs;
    }

    private long getMediaDurationUsAtCurrentSpeed(long playoutDurationUs) {
        if (isUsingSonic()) {
            return this.sonicAudioProcessor.getMediaDuration(playoutDurationUs);
        }
        return playoutDurationUs;
    }

    private void updateLastProcessedInputTime() {
        synchronized (this.lock) {
            if (isUsingSonic()) {
                long currentProcessedInputDurationUs = Util.scaleLargeTimestamp(this.sonicAudioProcessor.getProcessedInputBytes(), 1000000L, ((long) this.inputAudioFormat.bytesPerFrame) * ((long) this.inputAudioFormat.sampleRate));
                this.lastProcessedInputTimeUs = this.inputSegmentStartTimesUs.get(this.inputSegmentStartTimesUs.size() - 1) + currentProcessedInputDurationUs;
            } else {
                this.lastProcessedInputTimeUs = Util.scaleLargeTimestamp(this.bytesRead, 1000000L, ((long) this.inputAudioFormat.bytesPerFrame) * ((long) this.inputAudioFormat.sampleRate));
            }
        }
    }

    private boolean isUsingSonic() {
        boolean z;
        synchronized (this.lock) {
            z = this.currentSpeed != 1.0f;
        }
        return z;
    }

    @EnsuresNonNull({"inputSegmentStartTimesUs", "outputSegmentStartTimesUs"})
    @RequiresNonNull({"lock"})
    private void resetState() {
        synchronized (this.lock) {
            this.inputSegmentStartTimesUs = new LongArray();
            this.outputSegmentStartTimesUs = new LongArray();
            this.inputSegmentStartTimesUs.add(0L);
            this.outputSegmentStartTimesUs.add(0L);
            this.lastProcessedInputTimeUs = 0L;
            this.lastSpeedAdjustedInputTimeUs = 0L;
            this.lastSpeedAdjustedOutputTimeUs = 0L;
            this.currentSpeed = 1.0f;
        }
        this.bytesRead = 0L;
        this.endOfStreamQueuedToSonic = false;
    }
}
