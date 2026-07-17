package androidx.media3.exoplayer;

import android.os.SystemClock;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import com.google.common.primitives.Longs;

/* JADX INFO: loaded from: classes.dex */
public final class DefaultLivePlaybackSpeedControl implements LivePlaybackSpeedControl {
    public static final float DEFAULT_FALLBACK_MAX_PLAYBACK_SPEED = 1.03f;
    public static final float DEFAULT_FALLBACK_MIN_PLAYBACK_SPEED = 0.97f;
    public static final long DEFAULT_MAX_LIVE_OFFSET_ERROR_MS_FOR_UNIT_SPEED = 20;
    public static final float DEFAULT_MIN_POSSIBLE_LIVE_OFFSET_SMOOTHING_FACTOR = 0.999f;
    public static final long DEFAULT_MIN_UPDATE_INTERVAL_MS = 1000;
    public static final float DEFAULT_PROPORTIONAL_CONTROL_FACTOR = 0.1f;
    public static final long DEFAULT_TARGET_LIVE_OFFSET_INCREMENT_ON_REBUFFER_MS = 500;
    private float adjustedPlaybackSpeed;
    private long currentTargetLiveOffsetUs;
    private final float fallbackMaxPlaybackSpeed;
    private final float fallbackMinPlaybackSpeed;
    private long idealTargetLiveOffsetUs;
    private long lastPlaybackSpeedUpdateMs;
    private final long maxLiveOffsetErrorUsForUnitSpeed;
    private float maxPlaybackSpeed;
    private long maxTargetLiveOffsetUs;
    private long mediaConfigurationTargetLiveOffsetUs;
    private float minPlaybackSpeed;
    private final float minPossibleLiveOffsetSmoothingFactor;
    private long minTargetLiveOffsetUs;
    private final long minUpdateIntervalMs;
    private final float proportionalControlFactor;
    private long smoothedMinPossibleLiveOffsetDeviationUs;
    private long smoothedMinPossibleLiveOffsetUs;
    private long targetLiveOffsetOverrideUs;
    private final long targetLiveOffsetRebufferDeltaUs;

    public static final class Builder {
        private float fallbackMinPlaybackSpeed = 0.97f;
        private float fallbackMaxPlaybackSpeed = 1.03f;
        private long minUpdateIntervalMs = 1000;
        private float proportionalControlFactorUs = 1.0E-7f;
        private long maxLiveOffsetErrorUsForUnitSpeed = Util.msToUs(20);
        private long targetLiveOffsetIncrementOnRebufferUs = Util.msToUs(500);
        private float minPossibleLiveOffsetSmoothingFactor = 0.999f;

        public Builder setFallbackMinPlaybackSpeed(float fallbackMinPlaybackSpeed) {
            Assertions.checkArgument(0.0f < fallbackMinPlaybackSpeed && fallbackMinPlaybackSpeed <= 1.0f);
            this.fallbackMinPlaybackSpeed = fallbackMinPlaybackSpeed;
            return this;
        }

        public Builder setFallbackMaxPlaybackSpeed(float fallbackMaxPlaybackSpeed) {
            Assertions.checkArgument(fallbackMaxPlaybackSpeed >= 1.0f);
            this.fallbackMaxPlaybackSpeed = fallbackMaxPlaybackSpeed;
            return this;
        }

        public Builder setMinUpdateIntervalMs(long minUpdateIntervalMs) {
            Assertions.checkArgument(minUpdateIntervalMs > 0);
            this.minUpdateIntervalMs = minUpdateIntervalMs;
            return this;
        }

        public Builder setProportionalControlFactor(float proportionalControlFactor) {
            Assertions.checkArgument(proportionalControlFactor > 0.0f);
            this.proportionalControlFactorUs = proportionalControlFactor / 1000000.0f;
            return this;
        }

        public Builder setMaxLiveOffsetErrorMsForUnitSpeed(long maxLiveOffsetErrorMsForUnitSpeed) {
            Assertions.checkArgument(maxLiveOffsetErrorMsForUnitSpeed > 0);
            this.maxLiveOffsetErrorUsForUnitSpeed = Util.msToUs(maxLiveOffsetErrorMsForUnitSpeed);
            return this;
        }

        public Builder setTargetLiveOffsetIncrementOnRebufferMs(long targetLiveOffsetIncrementOnRebufferMs) {
            Assertions.checkArgument(targetLiveOffsetIncrementOnRebufferMs >= 0);
            this.targetLiveOffsetIncrementOnRebufferUs = Util.msToUs(targetLiveOffsetIncrementOnRebufferMs);
            return this;
        }

        public Builder setMinPossibleLiveOffsetSmoothingFactor(float minPossibleLiveOffsetSmoothingFactor) {
            Assertions.checkArgument(minPossibleLiveOffsetSmoothingFactor >= 0.0f && minPossibleLiveOffsetSmoothingFactor < 1.0f);
            this.minPossibleLiveOffsetSmoothingFactor = minPossibleLiveOffsetSmoothingFactor;
            return this;
        }

        public DefaultLivePlaybackSpeedControl build() {
            return new DefaultLivePlaybackSpeedControl(this.fallbackMinPlaybackSpeed, this.fallbackMaxPlaybackSpeed, this.minUpdateIntervalMs, this.proportionalControlFactorUs, this.maxLiveOffsetErrorUsForUnitSpeed, this.targetLiveOffsetIncrementOnRebufferUs, this.minPossibleLiveOffsetSmoothingFactor);
        }
    }

    private DefaultLivePlaybackSpeedControl(float fallbackMinPlaybackSpeed, float fallbackMaxPlaybackSpeed, long minUpdateIntervalMs, float proportionalControlFactor, long maxLiveOffsetErrorUsForUnitSpeed, long targetLiveOffsetRebufferDeltaUs, float minPossibleLiveOffsetSmoothingFactor) {
        this.fallbackMinPlaybackSpeed = fallbackMinPlaybackSpeed;
        this.fallbackMaxPlaybackSpeed = fallbackMaxPlaybackSpeed;
        this.minUpdateIntervalMs = minUpdateIntervalMs;
        this.proportionalControlFactor = proportionalControlFactor;
        this.maxLiveOffsetErrorUsForUnitSpeed = maxLiveOffsetErrorUsForUnitSpeed;
        this.targetLiveOffsetRebufferDeltaUs = targetLiveOffsetRebufferDeltaUs;
        this.minPossibleLiveOffsetSmoothingFactor = minPossibleLiveOffsetSmoothingFactor;
        this.mediaConfigurationTargetLiveOffsetUs = C.TIME_UNSET;
        this.targetLiveOffsetOverrideUs = C.TIME_UNSET;
        this.minTargetLiveOffsetUs = C.TIME_UNSET;
        this.maxTargetLiveOffsetUs = C.TIME_UNSET;
        this.minPlaybackSpeed = fallbackMinPlaybackSpeed;
        this.maxPlaybackSpeed = fallbackMaxPlaybackSpeed;
        this.adjustedPlaybackSpeed = 1.0f;
        this.lastPlaybackSpeedUpdateMs = C.TIME_UNSET;
        this.idealTargetLiveOffsetUs = C.TIME_UNSET;
        this.currentTargetLiveOffsetUs = C.TIME_UNSET;
        this.smoothedMinPossibleLiveOffsetUs = C.TIME_UNSET;
        this.smoothedMinPossibleLiveOffsetDeviationUs = C.TIME_UNSET;
    }

    @Override // androidx.media3.exoplayer.LivePlaybackSpeedControl
    public void setLiveConfiguration(MediaItem.LiveConfiguration liveConfiguration) {
        float f;
        float f2;
        this.mediaConfigurationTargetLiveOffsetUs = Util.msToUs(liveConfiguration.targetOffsetMs);
        this.minTargetLiveOffsetUs = Util.msToUs(liveConfiguration.minOffsetMs);
        this.maxTargetLiveOffsetUs = Util.msToUs(liveConfiguration.maxOffsetMs);
        if (liveConfiguration.minPlaybackSpeed != -3.4028235E38f) {
            f = liveConfiguration.minPlaybackSpeed;
        } else {
            f = this.fallbackMinPlaybackSpeed;
        }
        this.minPlaybackSpeed = f;
        if (liveConfiguration.maxPlaybackSpeed != -3.4028235E38f) {
            f2 = liveConfiguration.maxPlaybackSpeed;
        } else {
            f2 = this.fallbackMaxPlaybackSpeed;
        }
        this.maxPlaybackSpeed = f2;
        if (this.minPlaybackSpeed == 1.0f && this.maxPlaybackSpeed == 1.0f) {
            this.mediaConfigurationTargetLiveOffsetUs = C.TIME_UNSET;
        }
        maybeResetTargetLiveOffsetUs();
    }

    @Override // androidx.media3.exoplayer.LivePlaybackSpeedControl
    public void setTargetLiveOffsetOverrideUs(long liveOffsetUs) {
        this.targetLiveOffsetOverrideUs = liveOffsetUs;
        maybeResetTargetLiveOffsetUs();
    }

    @Override // androidx.media3.exoplayer.LivePlaybackSpeedControl
    public void notifyRebuffer() {
        if (this.currentTargetLiveOffsetUs == C.TIME_UNSET) {
            return;
        }
        this.currentTargetLiveOffsetUs += this.targetLiveOffsetRebufferDeltaUs;
        if (this.maxTargetLiveOffsetUs != C.TIME_UNSET && this.currentTargetLiveOffsetUs > this.maxTargetLiveOffsetUs) {
            this.currentTargetLiveOffsetUs = this.maxTargetLiveOffsetUs;
        }
        this.lastPlaybackSpeedUpdateMs = C.TIME_UNSET;
    }

    @Override // androidx.media3.exoplayer.LivePlaybackSpeedControl
    public float getAdjustedPlaybackSpeed(long liveOffsetUs, long bufferedDurationUs) {
        if (this.mediaConfigurationTargetLiveOffsetUs == C.TIME_UNSET) {
            return 1.0f;
        }
        updateSmoothedMinPossibleLiveOffsetUs(liveOffsetUs, bufferedDurationUs);
        if (this.lastPlaybackSpeedUpdateMs != C.TIME_UNSET && SystemClock.elapsedRealtime() - this.lastPlaybackSpeedUpdateMs < this.minUpdateIntervalMs) {
            return this.adjustedPlaybackSpeed;
        }
        this.lastPlaybackSpeedUpdateMs = SystemClock.elapsedRealtime();
        adjustTargetLiveOffsetUs(liveOffsetUs);
        long liveOffsetErrorUs = liveOffsetUs - this.currentTargetLiveOffsetUs;
        if (Math.abs(liveOffsetErrorUs) < this.maxLiveOffsetErrorUsForUnitSpeed) {
            this.adjustedPlaybackSpeed = 1.0f;
        } else {
            float calculatedSpeed = (this.proportionalControlFactor * liveOffsetErrorUs) + 1.0f;
            this.adjustedPlaybackSpeed = Util.constrainValue(calculatedSpeed, this.minPlaybackSpeed, this.maxPlaybackSpeed);
        }
        float calculatedSpeed2 = this.adjustedPlaybackSpeed;
        return calculatedSpeed2;
    }

    @Override // androidx.media3.exoplayer.LivePlaybackSpeedControl
    public long getTargetLiveOffsetUs() {
        return this.currentTargetLiveOffsetUs;
    }

    private void maybeResetTargetLiveOffsetUs() {
        long idealOffsetUs = C.TIME_UNSET;
        if (this.mediaConfigurationTargetLiveOffsetUs != C.TIME_UNSET) {
            if (this.targetLiveOffsetOverrideUs != C.TIME_UNSET) {
                idealOffsetUs = this.targetLiveOffsetOverrideUs;
            } else {
                idealOffsetUs = this.mediaConfigurationTargetLiveOffsetUs;
                if (this.minTargetLiveOffsetUs != C.TIME_UNSET && idealOffsetUs < this.minTargetLiveOffsetUs) {
                    idealOffsetUs = this.minTargetLiveOffsetUs;
                }
                if (this.maxTargetLiveOffsetUs != C.TIME_UNSET && idealOffsetUs > this.maxTargetLiveOffsetUs) {
                    idealOffsetUs = this.maxTargetLiveOffsetUs;
                }
            }
        }
        if (this.idealTargetLiveOffsetUs == idealOffsetUs) {
            return;
        }
        this.idealTargetLiveOffsetUs = idealOffsetUs;
        this.currentTargetLiveOffsetUs = idealOffsetUs;
        this.smoothedMinPossibleLiveOffsetUs = C.TIME_UNSET;
        this.smoothedMinPossibleLiveOffsetDeviationUs = C.TIME_UNSET;
        this.lastPlaybackSpeedUpdateMs = C.TIME_UNSET;
    }

    private void updateSmoothedMinPossibleLiveOffsetUs(long liveOffsetUs, long bufferedDurationUs) {
        long minPossibleLiveOffsetUs = liveOffsetUs - bufferedDurationUs;
        if (this.smoothedMinPossibleLiveOffsetUs == C.TIME_UNSET) {
            this.smoothedMinPossibleLiveOffsetUs = minPossibleLiveOffsetUs;
            this.smoothedMinPossibleLiveOffsetDeviationUs = 0L;
        } else {
            this.smoothedMinPossibleLiveOffsetUs = Math.max(minPossibleLiveOffsetUs, smooth(this.smoothedMinPossibleLiveOffsetUs, minPossibleLiveOffsetUs, this.minPossibleLiveOffsetSmoothingFactor));
            long minPossibleLiveOffsetDeviationUs = Math.abs(minPossibleLiveOffsetUs - this.smoothedMinPossibleLiveOffsetUs);
            this.smoothedMinPossibleLiveOffsetDeviationUs = smooth(this.smoothedMinPossibleLiveOffsetDeviationUs, minPossibleLiveOffsetDeviationUs, this.minPossibleLiveOffsetSmoothingFactor);
        }
    }

    private void adjustTargetLiveOffsetUs(long liveOffsetUs) {
        long safeOffsetUs = this.smoothedMinPossibleLiveOffsetUs + (this.smoothedMinPossibleLiveOffsetDeviationUs * 3);
        if (this.currentTargetLiveOffsetUs <= safeOffsetUs) {
            long offsetWhenSlowingDownNowUs = liveOffsetUs - ((long) (Math.max(0.0f, this.adjustedPlaybackSpeed - 1.0f) / this.proportionalControlFactor));
            this.currentTargetLiveOffsetUs = Util.constrainValue(offsetWhenSlowingDownNowUs, this.currentTargetLiveOffsetUs, safeOffsetUs);
            if (this.maxTargetLiveOffsetUs != C.TIME_UNSET && this.currentTargetLiveOffsetUs > this.maxTargetLiveOffsetUs) {
                this.currentTargetLiveOffsetUs = this.maxTargetLiveOffsetUs;
                return;
            }
            return;
        }
        long minUpdateIntervalUs = Util.msToUs(this.minUpdateIntervalMs);
        long decrementToOffsetCurrentSpeedUs = (long) ((this.adjustedPlaybackSpeed - 1.0f) * minUpdateIntervalUs);
        long decrementToIncreaseSpeedUs = (long) ((this.maxPlaybackSpeed - 1.0f) * minUpdateIntervalUs);
        long maxDecrementUs = decrementToOffsetCurrentSpeedUs + decrementToIncreaseSpeedUs;
        this.currentTargetLiveOffsetUs = Longs.max(safeOffsetUs, this.idealTargetLiveOffsetUs, this.currentTargetLiveOffsetUs - maxDecrementUs);
    }

    private static long smooth(long smoothedValue, long newValue, float smoothingFactor) {
        return (long) ((smoothedValue * smoothingFactor) + ((1.0f - smoothingFactor) * newValue));
    }
}
