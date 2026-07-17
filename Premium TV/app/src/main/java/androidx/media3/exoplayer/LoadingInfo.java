package androidx.media3.exoplayer;

import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import com.google.common.base.Objects;

/* JADX INFO: loaded from: classes.dex */
public final class LoadingInfo {
    public final long lastRebufferRealtimeMs;
    public final long playbackPositionUs;
    public final float playbackSpeed;

    public static final class Builder {
        private long lastRebufferRealtimeMs;
        private long playbackPositionUs;
        private float playbackSpeed;

        public Builder() {
            this.playbackPositionUs = C.TIME_UNSET;
            this.playbackSpeed = -3.4028235E38f;
            this.lastRebufferRealtimeMs = C.TIME_UNSET;
        }

        private Builder(LoadingInfo loadingInfo) {
            this.playbackPositionUs = loadingInfo.playbackPositionUs;
            this.playbackSpeed = loadingInfo.playbackSpeed;
            this.lastRebufferRealtimeMs = loadingInfo.lastRebufferRealtimeMs;
        }

        public Builder setPlaybackPositionUs(long playbackPositionUs) {
            this.playbackPositionUs = playbackPositionUs;
            return this;
        }

        public Builder setPlaybackSpeed(float playbackSpeed) {
            Assertions.checkArgument(playbackSpeed > 0.0f || playbackSpeed == -3.4028235E38f);
            this.playbackSpeed = playbackSpeed;
            return this;
        }

        public Builder setLastRebufferRealtimeMs(long lastRebufferRealtimeMs) {
            Assertions.checkArgument(lastRebufferRealtimeMs >= 0 || lastRebufferRealtimeMs == C.TIME_UNSET);
            this.lastRebufferRealtimeMs = lastRebufferRealtimeMs;
            return this;
        }

        public LoadingInfo build() {
            return new LoadingInfo(this);
        }
    }

    private LoadingInfo(Builder builder) {
        this.playbackPositionUs = builder.playbackPositionUs;
        this.playbackSpeed = builder.playbackSpeed;
        this.lastRebufferRealtimeMs = builder.lastRebufferRealtimeMs;
    }

    public Builder buildUpon() {
        return new Builder();
    }

    public boolean rebufferedSince(long realtimeMs) {
        return (this.lastRebufferRealtimeMs == C.TIME_UNSET || realtimeMs == C.TIME_UNSET || this.lastRebufferRealtimeMs < realtimeMs) ? false : true;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LoadingInfo)) {
            return false;
        }
        LoadingInfo that = (LoadingInfo) o;
        return this.playbackPositionUs == that.playbackPositionUs && this.playbackSpeed == that.playbackSpeed && this.lastRebufferRealtimeMs == that.lastRebufferRealtimeMs;
    }

    public int hashCode() {
        return Objects.hashCode(Long.valueOf(this.playbackPositionUs), Float.valueOf(this.playbackSpeed), Long.valueOf(this.lastRebufferRealtimeMs));
    }
}
