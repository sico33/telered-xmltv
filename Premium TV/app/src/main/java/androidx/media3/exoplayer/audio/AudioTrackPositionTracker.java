package androidx.media3.exoplayer.audio;

import android.media.AudioTrack;
import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Clock;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.dash.DashMediaSource;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
final class AudioTrackPositionTracker {
    private static final long FORCE_RESET_WORKAROUND_TIMEOUT_MS = 200;
    private static final long MAX_AUDIO_TIMESTAMP_OFFSET_US = 5000000;
    private static final long MAX_LATENCY_US = 5000000;
    private static final int MAX_PLAYHEAD_OFFSET_COUNT = 10;
    private static final int MIN_LATENCY_SAMPLE_INTERVAL_US = 500000;
    private static final int MIN_PLAYHEAD_OFFSET_SAMPLE_INTERVAL_US = 30000;
    private static final long MODE_SWITCH_SMOOTHING_DURATION_US = 1000000;
    private static final int PLAYSTATE_PAUSED = 2;
    private static final int PLAYSTATE_PLAYING = 3;
    private static final int PLAYSTATE_STOPPED = 1;
    private static final long RAW_PLAYBACK_HEAD_POSITION_UPDATE_INTERVAL_MS = 5;
    private AudioTimestampPoller audioTimestampPoller;
    private AudioTrack audioTrack;
    private float audioTrackPlaybackSpeed;
    private int bufferSize;
    private long bufferSizeUs;
    private Clock clock;
    private long endPlaybackHeadPosition;
    private boolean expectRawPlaybackHeadReset;
    private long forceResetWorkaroundTimeMs;
    private Method getLatencyMethod;
    private boolean hasData;
    private boolean isOutputPcm;
    private long lastLatencySampleTimeUs;
    private long lastPlayheadSampleTimeUs;
    private long lastPositionUs;
    private long lastRawPlaybackHeadPositionSampleTimeMs;
    private boolean lastSampleUsedGetTimestampMode;
    private long lastSystemTimeUs;
    private long latencyUs;
    private final Listener listener;
    private boolean needsPassthroughWorkarounds;
    private int nextPlayheadOffsetIndex;
    private boolean notifiedPositionIncreasing;
    private int outputPcmFrameSize;
    private int outputSampleRate;
    private long passthroughWorkaroundPauseOffset;
    private int playheadOffsetCount;
    private final long[] playheadOffsets;
    private long previousModePositionUs;
    private long previousModeSystemTimeUs;
    private long rawPlaybackHeadPosition;
    private long rawPlaybackHeadWrapCount;
    private long smoothedPlayheadOffsetUs;
    private long stopPlaybackHeadPosition;
    private long stopTimestampUs;
    private long sumRawPlaybackHeadPosition;

    public interface Listener {
        void onInvalidLatency(long j);

        void onPositionAdvancing(long j);

        void onPositionFramesMismatch(long j, long j2, long j3, long j4);

        void onSystemTimeUsMismatch(long j, long j2, long j3, long j4);

        void onUnderrun(int i, long j);
    }

    public AudioTrackPositionTracker(Listener listener) {
        this.listener = (Listener) Assertions.checkNotNull(listener);
        try {
            this.getLatencyMethod = AudioTrack.class.getMethod("getLatency", null);
        } catch (NoSuchMethodException e) {
        }
        this.playheadOffsets = new long[10];
        this.clock = Clock.DEFAULT;
    }

    public void setAudioTrack(AudioTrack audioTrack, boolean isPassthrough, int outputEncoding, int outputPcmFrameSize, int bufferSize) {
        long jSampleCountToDurationUs;
        this.audioTrack = audioTrack;
        this.outputPcmFrameSize = outputPcmFrameSize;
        this.bufferSize = bufferSize;
        this.audioTimestampPoller = new AudioTimestampPoller(audioTrack);
        this.outputSampleRate = audioTrack.getSampleRate();
        this.needsPassthroughWorkarounds = isPassthrough && needsPassthroughWorkarounds(outputEncoding);
        this.isOutputPcm = Util.isEncodingLinearPcm(outputEncoding);
        if (this.isOutputPcm) {
            jSampleCountToDurationUs = Util.sampleCountToDurationUs(bufferSize / outputPcmFrameSize, this.outputSampleRate);
        } else {
            jSampleCountToDurationUs = -9223372036854775807L;
        }
        this.bufferSizeUs = jSampleCountToDurationUs;
        this.rawPlaybackHeadPosition = 0L;
        this.rawPlaybackHeadWrapCount = 0L;
        this.expectRawPlaybackHeadReset = false;
        this.sumRawPlaybackHeadPosition = 0L;
        this.passthroughWorkaroundPauseOffset = 0L;
        this.hasData = false;
        this.stopTimestampUs = C.TIME_UNSET;
        this.forceResetWorkaroundTimeMs = C.TIME_UNSET;
        this.lastLatencySampleTimeUs = 0L;
        this.latencyUs = 0L;
        this.audioTrackPlaybackSpeed = 1.0f;
    }

    public void setAudioTrackPlaybackSpeed(float audioTrackPlaybackSpeed) {
        this.audioTrackPlaybackSpeed = audioTrackPlaybackSpeed;
        if (this.audioTimestampPoller != null) {
            this.audioTimestampPoller.reset();
        }
        resetSyncParams();
    }

    public long getCurrentPositionUs(boolean sourceEnded) {
        long timestampPositionUs;
        if (((AudioTrack) Assertions.checkNotNull(this.audioTrack)).getPlayState() == 3) {
            maybeSampleSyncParams();
        }
        long systemTimeUs = this.clock.nanoTime() / 1000;
        AudioTimestampPoller audioTimestampPoller = (AudioTimestampPoller) Assertions.checkNotNull(this.audioTimestampPoller);
        boolean useGetTimestampMode = audioTimestampPoller.hasAdvancingTimestamp();
        if (useGetTimestampMode) {
            long timestampPositionFrames = audioTimestampPoller.getTimestampPositionFrames();
            long timestampPositionUs2 = Util.sampleCountToDurationUs(timestampPositionFrames, this.outputSampleRate);
            long elapsedSinceTimestampUs = systemTimeUs - audioTimestampPoller.getTimestampSystemTimeUs();
            timestampPositionUs = timestampPositionUs2 + Util.getMediaDurationForPlayoutDuration(elapsedSinceTimestampUs, this.audioTrackPlaybackSpeed);
        } else {
            if (this.playheadOffsetCount == 0) {
                timestampPositionUs = getPlaybackHeadPositionUs();
            } else {
                long positionUs = this.smoothedPlayheadOffsetUs;
                timestampPositionUs = Util.getMediaDurationForPlayoutDuration(positionUs + systemTimeUs, this.audioTrackPlaybackSpeed);
            }
            if (!sourceEnded) {
                timestampPositionUs = Math.max(0L, timestampPositionUs - this.latencyUs);
            }
        }
        if (this.lastSampleUsedGetTimestampMode != useGetTimestampMode) {
            this.previousModeSystemTimeUs = this.lastSystemTimeUs;
            this.previousModePositionUs = this.lastPositionUs;
        }
        long elapsedSincePreviousModeUs = systemTimeUs - this.previousModeSystemTimeUs;
        if (elapsedSincePreviousModeUs < 1000000) {
            long previousModeProjectedPositionUs = this.previousModePositionUs + Util.getMediaDurationForPlayoutDuration(elapsedSincePreviousModeUs, this.audioTrackPlaybackSpeed);
            long rampPoint = (elapsedSincePreviousModeUs * 1000) / 1000000;
            long positionUs2 = timestampPositionUs * rampPoint;
            timestampPositionUs = (positionUs2 + ((1000 - rampPoint) * previousModeProjectedPositionUs)) / 1000;
        }
        if (!this.notifiedPositionIncreasing && timestampPositionUs > this.lastPositionUs) {
            this.notifiedPositionIncreasing = true;
            long mediaDurationSinceLastPositionUs = Util.usToMs(timestampPositionUs - this.lastPositionUs);
            long playoutDurationSinceLastPositionUs = Util.getPlayoutDurationForMediaDuration(mediaDurationSinceLastPositionUs, this.audioTrackPlaybackSpeed);
            long playoutStartSystemTimeMs = this.clock.currentTimeMillis() - Util.usToMs(playoutDurationSinceLastPositionUs);
            this.listener.onPositionAdvancing(playoutStartSystemTimeMs);
        }
        this.lastSystemTimeUs = systemTimeUs;
        this.lastPositionUs = timestampPositionUs;
        this.lastSampleUsedGetTimestampMode = useGetTimestampMode;
        return timestampPositionUs;
    }

    public void start() {
        if (this.stopTimestampUs != C.TIME_UNSET) {
            this.stopTimestampUs = Util.msToUs(this.clock.elapsedRealtime());
        }
        ((AudioTimestampPoller) Assertions.checkNotNull(this.audioTimestampPoller)).reset();
    }

    public boolean isPlaying() {
        return ((AudioTrack) Assertions.checkNotNull(this.audioTrack)).getPlayState() == 3;
    }

    public boolean mayHandleBuffer(long writtenFrames) {
        int playState = ((AudioTrack) Assertions.checkNotNull(this.audioTrack)).getPlayState();
        if (this.needsPassthroughWorkarounds) {
            if (playState == 2) {
                this.hasData = false;
                return false;
            }
            if (playState == 1 && getPlaybackHeadPosition() == 0) {
                return false;
            }
        }
        boolean hadData = this.hasData;
        this.hasData = hasPendingData(writtenFrames);
        if (hadData && !this.hasData && playState != 1) {
            this.listener.onUnderrun(this.bufferSize, Util.usToMs(this.bufferSizeUs));
        }
        return true;
    }

    public int getAvailableBufferSize(long writtenBytes) {
        int bytesPending = (int) (writtenBytes - (getPlaybackHeadPosition() * ((long) this.outputPcmFrameSize)));
        return this.bufferSize - bytesPending;
    }

    public boolean isStalled(long writtenFrames) {
        return this.forceResetWorkaroundTimeMs != C.TIME_UNSET && writtenFrames > 0 && this.clock.elapsedRealtime() - this.forceResetWorkaroundTimeMs >= FORCE_RESET_WORKAROUND_TIMEOUT_MS;
    }

    public void handleEndOfStream(long writtenFrames) {
        this.stopPlaybackHeadPosition = getPlaybackHeadPosition();
        this.stopTimestampUs = Util.msToUs(this.clock.elapsedRealtime());
        this.endPlaybackHeadPosition = writtenFrames;
    }

    public boolean hasPendingData(long writtenFrames) {
        long currentPositionUs = getCurrentPositionUs(false);
        return writtenFrames > Util.durationUsToSampleCount(currentPositionUs, this.outputSampleRate) || forceHasPendingData();
    }

    public boolean pause() {
        resetSyncParams();
        if (this.stopTimestampUs == C.TIME_UNSET) {
            ((AudioTimestampPoller) Assertions.checkNotNull(this.audioTimestampPoller)).reset();
            return true;
        }
        this.stopPlaybackHeadPosition = getPlaybackHeadPosition();
        return false;
    }

    public void expectRawPlaybackHeadReset() {
        this.expectRawPlaybackHeadReset = true;
        if (this.audioTimestampPoller != null) {
            this.audioTimestampPoller.expectTimestampFramePositionReset();
        }
    }

    public void reset() {
        resetSyncParams();
        this.audioTrack = null;
        this.audioTimestampPoller = null;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    private void maybeSampleSyncParams() {
        long systemTimeUs = this.clock.nanoTime() / 1000;
        if (systemTimeUs - this.lastPlayheadSampleTimeUs >= DashMediaSource.DEFAULT_FALLBACK_TARGET_LIVE_OFFSET_MS) {
            long playbackPositionUs = getPlaybackHeadPositionUs();
            if (playbackPositionUs == 0) {
                return;
            }
            this.playheadOffsets[this.nextPlayheadOffsetIndex] = Util.getPlayoutDurationForMediaDuration(playbackPositionUs, this.audioTrackPlaybackSpeed) - systemTimeUs;
            this.nextPlayheadOffsetIndex = (this.nextPlayheadOffsetIndex + 1) % 10;
            if (this.playheadOffsetCount < 10) {
                this.playheadOffsetCount++;
            }
            this.lastPlayheadSampleTimeUs = systemTimeUs;
            this.smoothedPlayheadOffsetUs = 0L;
            for (int i = 0; i < this.playheadOffsetCount; i++) {
                this.smoothedPlayheadOffsetUs += this.playheadOffsets[i] / ((long) this.playheadOffsetCount);
            }
        }
        if (this.needsPassthroughWorkarounds) {
            return;
        }
        maybePollAndCheckTimestamp(systemTimeUs);
        maybeUpdateLatency(systemTimeUs);
    }

    private void maybePollAndCheckTimestamp(long systemTimeUs) {
        AudioTimestampPoller audioTimestampPoller = (AudioTimestampPoller) Assertions.checkNotNull(this.audioTimestampPoller);
        if (!audioTimestampPoller.maybePollTimestamp(systemTimeUs)) {
            return;
        }
        long timestampSystemTimeUs = audioTimestampPoller.getTimestampSystemTimeUs();
        long timestampPositionFrames = audioTimestampPoller.getTimestampPositionFrames();
        long playbackPositionUs = getPlaybackHeadPositionUs();
        if (Math.abs(timestampSystemTimeUs - systemTimeUs) > DashMediaSource.MIN_LIVE_DEFAULT_START_POSITION_US) {
            this.listener.onSystemTimeUsMismatch(timestampPositionFrames, timestampSystemTimeUs, systemTimeUs, playbackPositionUs);
            audioTimestampPoller.rejectTimestamp();
        } else if (Math.abs(Util.sampleCountToDurationUs(timestampPositionFrames, this.outputSampleRate) - playbackPositionUs) > DashMediaSource.MIN_LIVE_DEFAULT_START_POSITION_US) {
            this.listener.onPositionFramesMismatch(timestampPositionFrames, timestampSystemTimeUs, systemTimeUs, playbackPositionUs);
            audioTimestampPoller.rejectTimestamp();
        } else {
            audioTimestampPoller.acceptTimestamp();
        }
    }

    private void maybeUpdateLatency(long systemTimeUs) {
        if (this.isOutputPcm && this.getLatencyMethod != null && systemTimeUs - this.lastLatencySampleTimeUs >= 500000) {
            try {
                this.latencyUs = (((long) ((Integer) Util.castNonNull((Integer) this.getLatencyMethod.invoke(Assertions.checkNotNull(this.audioTrack), new Object[0]))).intValue()) * 1000) - this.bufferSizeUs;
                this.latencyUs = Math.max(this.latencyUs, 0L);
                if (this.latencyUs > DashMediaSource.MIN_LIVE_DEFAULT_START_POSITION_US) {
                    this.listener.onInvalidLatency(this.latencyUs);
                    this.latencyUs = 0L;
                }
            } catch (Exception e) {
                this.getLatencyMethod = null;
            }
            this.lastLatencySampleTimeUs = systemTimeUs;
        }
    }

    private void resetSyncParams() {
        this.smoothedPlayheadOffsetUs = 0L;
        this.playheadOffsetCount = 0;
        this.nextPlayheadOffsetIndex = 0;
        this.lastPlayheadSampleTimeUs = 0L;
        this.lastSystemTimeUs = 0L;
        this.previousModeSystemTimeUs = 0L;
        this.notifiedPositionIncreasing = false;
    }

    private boolean forceHasPendingData() {
        return this.needsPassthroughWorkarounds && ((AudioTrack) Assertions.checkNotNull(this.audioTrack)).getPlayState() == 2 && getPlaybackHeadPosition() == 0;
    }

    private static boolean needsPassthroughWorkarounds(int outputEncoding) {
        return Util.SDK_INT < 23 && (outputEncoding == 5 || outputEncoding == 6);
    }

    private long getPlaybackHeadPositionUs() {
        return Util.sampleCountToDurationUs(getPlaybackHeadPosition(), this.outputSampleRate);
    }

    private long getPlaybackHeadPosition() {
        long currentTimeMs = this.clock.elapsedRealtime();
        if (this.stopTimestampUs != C.TIME_UNSET) {
            if (((AudioTrack) Assertions.checkNotNull(this.audioTrack)).getPlayState() == 2) {
                return this.stopPlaybackHeadPosition;
            }
            long elapsedTimeSinceStopUs = Util.msToUs(currentTimeMs) - this.stopTimestampUs;
            long mediaTimeSinceStopUs = Util.getMediaDurationForPlayoutDuration(elapsedTimeSinceStopUs, this.audioTrackPlaybackSpeed);
            long framesSinceStop = Util.durationUsToSampleCount(mediaTimeSinceStopUs, this.outputSampleRate);
            return Math.min(this.endPlaybackHeadPosition, this.stopPlaybackHeadPosition + framesSinceStop);
        }
        long elapsedTimeSinceStopUs2 = this.lastRawPlaybackHeadPositionSampleTimeMs;
        if (currentTimeMs - elapsedTimeSinceStopUs2 >= 5) {
            updateRawPlaybackHeadPosition(currentTimeMs);
            this.lastRawPlaybackHeadPositionSampleTimeMs = currentTimeMs;
        }
        return this.rawPlaybackHeadPosition + this.sumRawPlaybackHeadPosition + (this.rawPlaybackHeadWrapCount << 32);
    }

    private void updateRawPlaybackHeadPosition(long currentTimeMs) {
        AudioTrack audioTrack = (AudioTrack) Assertions.checkNotNull(this.audioTrack);
        int state = audioTrack.getPlayState();
        if (state == 1) {
            return;
        }
        long rawPlaybackHeadPosition = ((long) audioTrack.getPlaybackHeadPosition()) & 4294967295L;
        if (this.needsPassthroughWorkarounds) {
            if (state == 2 && rawPlaybackHeadPosition == 0) {
                this.passthroughWorkaroundPauseOffset = this.rawPlaybackHeadPosition;
            }
            rawPlaybackHeadPosition += this.passthroughWorkaroundPauseOffset;
        }
        if (Util.SDK_INT <= 29) {
            if (rawPlaybackHeadPosition == 0 && this.rawPlaybackHeadPosition > 0 && state == 3) {
                if (this.forceResetWorkaroundTimeMs == C.TIME_UNSET) {
                    this.forceResetWorkaroundTimeMs = currentTimeMs;
                    return;
                }
                return;
            }
            this.forceResetWorkaroundTimeMs = C.TIME_UNSET;
        }
        if (this.rawPlaybackHeadPosition > rawPlaybackHeadPosition) {
            if (this.expectRawPlaybackHeadReset) {
                this.sumRawPlaybackHeadPosition += this.rawPlaybackHeadPosition;
                this.expectRawPlaybackHeadReset = false;
            } else {
                this.rawPlaybackHeadWrapCount++;
            }
        }
        this.rawPlaybackHeadPosition = rawPlaybackHeadPosition;
    }
}
