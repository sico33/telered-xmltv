package androidx.media3.exoplayer;

import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.util.Clock;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public final class StandaloneMediaClock implements MediaClock {
    private long baseElapsedMs;
    private long baseUs;
    private final Clock clock;
    private PlaybackParameters playbackParameters = PlaybackParameters.DEFAULT;
    private boolean started;

    @Override // androidx.media3.exoplayer.MediaClock
    public /* synthetic */ boolean hasSkippedSilenceSinceLastCall() {
        return MediaClock.CC.$default$hasSkippedSilenceSinceLastCall(this);
    }

    public StandaloneMediaClock(Clock clock) {
        this.clock = clock;
    }

    public void start() {
        if (!this.started) {
            this.baseElapsedMs = this.clock.elapsedRealtime();
            this.started = true;
        }
    }

    public void stop() {
        if (this.started) {
            resetPosition(getPositionUs());
            this.started = false;
        }
    }

    public void resetPosition(long positionUs) {
        this.baseUs = positionUs;
        if (this.started) {
            this.baseElapsedMs = this.clock.elapsedRealtime();
        }
    }

    @Override // androidx.media3.exoplayer.MediaClock
    public long getPositionUs() {
        long positionUs = this.baseUs;
        if (this.started) {
            long elapsedSinceBaseMs = this.clock.elapsedRealtime() - this.baseElapsedMs;
            if (this.playbackParameters.speed == 1.0f) {
                return positionUs + Util.msToUs(elapsedSinceBaseMs);
            }
            return positionUs + this.playbackParameters.getMediaTimeUsForPlayoutTimeMs(elapsedSinceBaseMs);
        }
        return positionUs;
    }

    @Override // androidx.media3.exoplayer.MediaClock
    public void setPlaybackParameters(PlaybackParameters playbackParameters) {
        if (this.started) {
            resetPosition(getPositionUs());
        }
        this.playbackParameters = playbackParameters;
    }

    @Override // androidx.media3.exoplayer.MediaClock
    public PlaybackParameters getPlaybackParameters() {
        return this.playbackParameters;
    }
}
