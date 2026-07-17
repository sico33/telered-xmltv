package androidx.media3.exoplayer;

import androidx.media3.common.PlaybackParameters;

/* JADX INFO: loaded from: classes.dex */
public interface MediaClock {
    PlaybackParameters getPlaybackParameters();

    long getPositionUs();

    boolean hasSkippedSilenceSinceLastCall();

    void setPlaybackParameters(PlaybackParameters playbackParameters);

    /* JADX INFO: renamed from: androidx.media3.exoplayer.MediaClock$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
        public static boolean $default$hasSkippedSilenceSinceLastCall(MediaClock _this) {
            return false;
        }
    }
}
