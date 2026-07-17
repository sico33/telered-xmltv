package androidx.media3.exoplayer;

import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Clock;

/* JADX INFO: loaded from: classes.dex */
final class DefaultMediaClock implements MediaClock {
    private boolean isUsingStandaloneClock = true;
    private final PlaybackParametersListener listener;
    private MediaClock rendererClock;
    private Renderer rendererClockSource;
    private final StandaloneMediaClock standaloneClock;
    private boolean standaloneClockIsStarted;

    public interface PlaybackParametersListener {
        void onPlaybackParametersChanged(PlaybackParameters playbackParameters);
    }

    public DefaultMediaClock(PlaybackParametersListener listener, Clock clock) {
        this.listener = listener;
        this.standaloneClock = new StandaloneMediaClock(clock);
    }

    public void start() {
        this.standaloneClockIsStarted = true;
        this.standaloneClock.start();
    }

    public void stop() {
        this.standaloneClockIsStarted = false;
        this.standaloneClock.stop();
    }

    public void resetPosition(long positionUs) {
        this.standaloneClock.resetPosition(positionUs);
    }

    public void onRendererEnabled(Renderer renderer) throws ExoPlaybackException {
        MediaClock rendererMediaClock = renderer.getMediaClock();
        if (rendererMediaClock != null && rendererMediaClock != this.rendererClock) {
            if (this.rendererClock != null) {
                throw ExoPlaybackException.createForUnexpected(new IllegalStateException("Multiple renderer media clocks enabled."), 1000);
            }
            this.rendererClock = rendererMediaClock;
            this.rendererClockSource = renderer;
            this.rendererClock.setPlaybackParameters(this.standaloneClock.getPlaybackParameters());
        }
    }

    public void onRendererDisabled(Renderer renderer) {
        if (renderer == this.rendererClockSource) {
            this.rendererClock = null;
            this.rendererClockSource = null;
            this.isUsingStandaloneClock = true;
        }
    }

    public long syncAndGetPositionUs(boolean isReadingAhead) {
        syncClocks(isReadingAhead);
        return getPositionUs();
    }

    @Override // androidx.media3.exoplayer.MediaClock
    public long getPositionUs() {
        if (this.isUsingStandaloneClock) {
            return this.standaloneClock.getPositionUs();
        }
        return ((MediaClock) Assertions.checkNotNull(this.rendererClock)).getPositionUs();
    }

    @Override // androidx.media3.exoplayer.MediaClock
    public boolean hasSkippedSilenceSinceLastCall() {
        if (this.isUsingStandaloneClock) {
            return this.standaloneClock.hasSkippedSilenceSinceLastCall();
        }
        return ((MediaClock) Assertions.checkNotNull(this.rendererClock)).hasSkippedSilenceSinceLastCall();
    }

    @Override // androidx.media3.exoplayer.MediaClock
    public void setPlaybackParameters(PlaybackParameters playbackParameters) {
        if (this.rendererClock != null) {
            this.rendererClock.setPlaybackParameters(playbackParameters);
            playbackParameters = this.rendererClock.getPlaybackParameters();
        }
        this.standaloneClock.setPlaybackParameters(playbackParameters);
    }

    @Override // androidx.media3.exoplayer.MediaClock
    public PlaybackParameters getPlaybackParameters() {
        if (this.rendererClock != null) {
            return this.rendererClock.getPlaybackParameters();
        }
        return this.standaloneClock.getPlaybackParameters();
    }

    private void syncClocks(boolean isReadingAhead) {
        if (shouldUseStandaloneClock(isReadingAhead)) {
            this.isUsingStandaloneClock = true;
            if (this.standaloneClockIsStarted) {
                this.standaloneClock.start();
                return;
            }
            return;
        }
        MediaClock rendererClock = (MediaClock) Assertions.checkNotNull(this.rendererClock);
        long rendererClockPositionUs = rendererClock.getPositionUs();
        if (this.isUsingStandaloneClock) {
            if (rendererClockPositionUs < this.standaloneClock.getPositionUs()) {
                this.standaloneClock.stop();
                return;
            } else {
                this.isUsingStandaloneClock = false;
                if (this.standaloneClockIsStarted) {
                    this.standaloneClock.start();
                }
            }
        }
        this.standaloneClock.resetPosition(rendererClockPositionUs);
        PlaybackParameters playbackParameters = rendererClock.getPlaybackParameters();
        if (!playbackParameters.equals(this.standaloneClock.getPlaybackParameters())) {
            this.standaloneClock.setPlaybackParameters(playbackParameters);
            this.listener.onPlaybackParametersChanged(playbackParameters);
        }
    }

    private boolean shouldUseStandaloneClock(boolean isReadingAhead) {
        return this.rendererClockSource == null || this.rendererClockSource.isEnded() || (isReadingAhead && this.rendererClockSource.getState() != 2) || (!this.rendererClockSource.isReady() && (isReadingAhead || this.rendererClockSource.hasReadStreamToEnd()));
    }
}
