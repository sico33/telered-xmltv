package androidx.media3.exoplayer.audio;

import android.media.AudioTimestamp;
import android.media.AudioTrack;
import androidx.media3.common.C;
import androidx.media3.exoplayer.Renderer;

/* JADX INFO: loaded from: classes.dex */
final class AudioTimestampPoller {
    private static final int ERROR_POLL_INTERVAL_US = 500000;
    private static final int FAST_POLL_INTERVAL_US = 10000;
    private static final int INITIALIZING_DURATION_US = 500000;
    private static final int SLOW_POLL_INTERVAL_US = 10000000;
    private static final int STATE_ERROR = 4;
    private static final int STATE_INITIALIZING = 0;
    private static final int STATE_NO_TIMESTAMP = 3;
    private static final int STATE_TIMESTAMP = 1;
    private static final int STATE_TIMESTAMP_ADVANCING = 2;
    private final AudioTimestampWrapper audioTimestamp;
    private long initialTimestampPositionFrames;
    private long initializeSystemTimeUs;
    private long lastTimestampSampleTimeUs;
    private long sampleIntervalUs;
    private int state;

    public AudioTimestampPoller(AudioTrack audioTrack) {
        this.audioTimestamp = new AudioTimestampWrapper(audioTrack);
        reset();
    }

    public boolean maybePollTimestamp(long systemTimeUs) {
        if (this.audioTimestamp == null || systemTimeUs - this.lastTimestampSampleTimeUs < this.sampleIntervalUs) {
            return false;
        }
        this.lastTimestampSampleTimeUs = systemTimeUs;
        boolean updatedTimestamp = this.audioTimestamp.maybeUpdateTimestamp();
        switch (this.state) {
            case 0:
                if (!updatedTimestamp) {
                    if (systemTimeUs - this.initializeSystemTimeUs > 500000) {
                        updateState(3);
                        return updatedTimestamp;
                    }
                    return updatedTimestamp;
                }
                if (this.audioTimestamp.getTimestampSystemTimeUs() >= this.initializeSystemTimeUs) {
                    this.initialTimestampPositionFrames = this.audioTimestamp.getTimestampPositionFrames();
                    updateState(1);
                    return updatedTimestamp;
                }
                return false;
            case 1:
                if (updatedTimestamp) {
                    long timestampPositionFrames = this.audioTimestamp.getTimestampPositionFrames();
                    if (timestampPositionFrames > this.initialTimestampPositionFrames) {
                        updateState(2);
                        return updatedTimestamp;
                    }
                    return updatedTimestamp;
                }
                reset();
                return updatedTimestamp;
            case 2:
                if (!updatedTimestamp) {
                    reset();
                    return updatedTimestamp;
                }
                return updatedTimestamp;
            case 3:
                if (updatedTimestamp) {
                    reset();
                    return updatedTimestamp;
                }
                return updatedTimestamp;
            case 4:
                return updatedTimestamp;
            default:
                throw new IllegalStateException();
        }
    }

    public void rejectTimestamp() {
        updateState(4);
    }

    public void acceptTimestamp() {
        if (this.state == 4) {
            reset();
        }
    }

    public boolean hasTimestamp() {
        return this.state == 1 || this.state == 2;
    }

    public boolean hasAdvancingTimestamp() {
        return this.state == 2;
    }

    public void reset() {
        if (this.audioTimestamp != null) {
            updateState(0);
        }
    }

    public long getTimestampSystemTimeUs() {
        return this.audioTimestamp != null ? this.audioTimestamp.getTimestampSystemTimeUs() : C.TIME_UNSET;
    }

    public long getTimestampPositionFrames() {
        if (this.audioTimestamp != null) {
            return this.audioTimestamp.getTimestampPositionFrames();
        }
        return -1L;
    }

    public void expectTimestampFramePositionReset() {
        if (this.audioTimestamp != null) {
            this.audioTimestamp.expectTimestampFramePositionReset();
        }
    }

    private void updateState(int state) {
        this.state = state;
        switch (state) {
            case 0:
                this.lastTimestampSampleTimeUs = 0L;
                this.initialTimestampPositionFrames = -1L;
                this.initializeSystemTimeUs = System.nanoTime() / 1000;
                this.sampleIntervalUs = Renderer.DEFAULT_DURATION_TO_PROGRESS_US;
                return;
            case 1:
                this.sampleIntervalUs = Renderer.DEFAULT_DURATION_TO_PROGRESS_US;
                return;
            case 2:
            case 3:
                this.sampleIntervalUs = 10000000L;
                return;
            case 4:
                this.sampleIntervalUs = 500000L;
                return;
            default:
                throw new IllegalStateException();
        }
    }

    private static final class AudioTimestampWrapper {
        private long accumulatedRawTimestampFramePosition;
        private final AudioTimestamp audioTimestamp = new AudioTimestamp();
        private final AudioTrack audioTrack;
        private boolean expectTimestampFramePositionReset;
        private long lastTimestampPositionFrames;
        private long lastTimestampRawPositionFrames;
        private long rawTimestampFramePositionWrapCount;

        public AudioTimestampWrapper(AudioTrack audioTrack) {
            this.audioTrack = audioTrack;
        }

        public boolean maybeUpdateTimestamp() {
            boolean updated = this.audioTrack.getTimestamp(this.audioTimestamp);
            if (updated) {
                long rawPositionFrames = this.audioTimestamp.framePosition;
                if (this.lastTimestampRawPositionFrames > rawPositionFrames) {
                    if (this.expectTimestampFramePositionReset) {
                        this.accumulatedRawTimestampFramePosition += this.lastTimestampRawPositionFrames;
                        this.expectTimestampFramePositionReset = false;
                    } else {
                        this.rawTimestampFramePositionWrapCount++;
                    }
                }
                this.lastTimestampRawPositionFrames = rawPositionFrames;
                this.lastTimestampPositionFrames = this.accumulatedRawTimestampFramePosition + rawPositionFrames + (this.rawTimestampFramePositionWrapCount << 32);
            }
            return updated;
        }

        public long getTimestampSystemTimeUs() {
            return this.audioTimestamp.nanoTime / 1000;
        }

        public long getTimestampPositionFrames() {
            return this.lastTimestampPositionFrames;
        }

        public void expectTimestampFramePositionReset() {
            this.expectTimestampFramePositionReset = true;
        }
    }
}
