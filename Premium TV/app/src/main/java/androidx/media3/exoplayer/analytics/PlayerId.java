package androidx.media3.exoplayer.analytics;

import android.media.metrics.LogSessionId;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import java.util.Objects;

/* JADX INFO: loaded from: classes.dex */
public final class PlayerId {
    public static final PlayerId UNSET;
    private final Object equalityToken;
    private final LogSessionIdApi31 logSessionIdApi31;
    public final String name;

    static {
        PlayerId playerId;
        if (Util.SDK_INT < 31) {
            playerId = new PlayerId("");
        } else {
            playerId = new PlayerId(LogSessionIdApi31.UNSET, "");
        }
        UNSET = playerId;
    }

    public PlayerId(String playerName) {
        Assertions.checkState(Util.SDK_INT < 31);
        this.name = playerName;
        this.logSessionIdApi31 = null;
        this.equalityToken = new Object();
    }

    public PlayerId(LogSessionId logSessionId, String playerName) {
        this(new LogSessionIdApi31(logSessionId), playerName);
    }

    private PlayerId(LogSessionIdApi31 logSessionIdApi31, String playerName) {
        this.logSessionIdApi31 = logSessionIdApi31;
        this.name = playerName;
        this.equalityToken = new Object();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlayerId)) {
            return false;
        }
        PlayerId playerId = (PlayerId) o;
        return Objects.equals(this.name, playerId.name) && Objects.equals(this.logSessionIdApi31, playerId.logSessionIdApi31) && Objects.equals(this.equalityToken, playerId.equalityToken);
    }

    public int hashCode() {
        return Objects.hash(this.name, this.logSessionIdApi31, this.equalityToken);
    }

    public LogSessionId getLogSessionId() {
        return ((LogSessionIdApi31) Assertions.checkNotNull(this.logSessionIdApi31)).logSessionId;
    }

    private static final class LogSessionIdApi31 {
        public static final LogSessionIdApi31 UNSET = new LogSessionIdApi31(LogSessionId.LOG_SESSION_ID_NONE);
        public final LogSessionId logSessionId;

        public LogSessionIdApi31(LogSessionId logSessionId) {
            this.logSessionId = logSessionId;
        }
    }
}
