package androidx.media3.exoplayer;

import androidx.media3.common.Timeline;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.upstream.Allocator;

/* JADX INFO: loaded from: classes.dex */
public interface LoadControl {

    @Deprecated
    public static final MediaSource.MediaPeriodId EMPTY_MEDIA_PERIOD_ID = new MediaSource.MediaPeriodId(new Object());

    Allocator getAllocator();

    @Deprecated
    long getBackBufferDurationUs();

    long getBackBufferDurationUs(PlayerId playerId);

    @Deprecated
    void onPrepared();

    void onPrepared(PlayerId playerId);

    @Deprecated
    void onReleased();

    void onReleased(PlayerId playerId);

    @Deprecated
    void onStopped();

    void onStopped(PlayerId playerId);

    @Deprecated
    void onTracksSelected(Timeline timeline, MediaSource.MediaPeriodId mediaPeriodId, Renderer[] rendererArr, TrackGroupArray trackGroupArray, ExoTrackSelection[] exoTrackSelectionArr);

    void onTracksSelected(PlayerId playerId, Timeline timeline, MediaSource.MediaPeriodId mediaPeriodId, Renderer[] rendererArr, TrackGroupArray trackGroupArray, ExoTrackSelection[] exoTrackSelectionArr);

    @Deprecated
    void onTracksSelected(Renderer[] rendererArr, TrackGroupArray trackGroupArray, ExoTrackSelection[] exoTrackSelectionArr);

    @Deprecated
    boolean retainBackBufferFromKeyframe();

    boolean retainBackBufferFromKeyframe(PlayerId playerId);

    @Deprecated
    boolean shouldContinueLoading(long j, long j2, float f);

    boolean shouldContinueLoading(Parameters parameters);

    @Deprecated
    boolean shouldStartPlayback(long j, float f, boolean z, long j2);

    @Deprecated
    boolean shouldStartPlayback(Timeline timeline, MediaSource.MediaPeriodId mediaPeriodId, long j, float f, boolean z, long j2);

    boolean shouldStartPlayback(Parameters parameters);

    public static final class Parameters {
        public final long bufferedDurationUs;
        public final MediaSource.MediaPeriodId mediaPeriodId;
        public final boolean playWhenReady;
        public final long playbackPositionUs;
        public final float playbackSpeed;
        public final PlayerId playerId;
        public final boolean rebuffering;
        public final long targetLiveOffsetUs;
        public final Timeline timeline;

        public Parameters(PlayerId playerId, Timeline timeline, MediaSource.MediaPeriodId mediaPeriodId, long playbackPositionUs, long bufferedDurationUs, float playbackSpeed, boolean playWhenReady, boolean rebuffering, long targetLiveOffsetUs) {
            this.playerId = playerId;
            this.timeline = timeline;
            this.mediaPeriodId = mediaPeriodId;
            this.playbackPositionUs = playbackPositionUs;
            this.bufferedDurationUs = bufferedDurationUs;
            this.playbackSpeed = playbackSpeed;
            this.playWhenReady = playWhenReady;
            this.rebuffering = rebuffering;
            this.targetLiveOffsetUs = targetLiveOffsetUs;
        }
    }

    /* JADX INFO: renamed from: androidx.media3.exoplayer.LoadControl$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
        @Deprecated
        public static void $default$onPrepared(LoadControl _this) {
            throw new IllegalStateException("onPrepared not implemented");
        }

        @Deprecated
        public static void $default$onTracksSelected(LoadControl _this, Renderer[] renderers, TrackGroupArray trackGroups, ExoTrackSelection[] trackSelections) {
            throw new IllegalStateException("onTracksSelected not implemented");
        }

        @Deprecated
        public static void $default$onStopped(LoadControl _this) {
            throw new IllegalStateException("onStopped not implemented");
        }

        @Deprecated
        public static void $default$onReleased(LoadControl _this) {
            throw new IllegalStateException("onReleased not implemented");
        }

        @Deprecated
        public static long $default$getBackBufferDurationUs(LoadControl _this) {
            throw new IllegalStateException("getBackBufferDurationUs not implemented");
        }

        @Deprecated
        public static boolean $default$retainBackBufferFromKeyframe(LoadControl _this) {
            throw new IllegalStateException("retainBackBufferFromKeyframe not implemented");
        }

        @Deprecated
        public static boolean $default$shouldContinueLoading(LoadControl _this, long playbackPositionUs, long bufferedDurationUs, float playbackSpeed) {
            throw new IllegalStateException("shouldContinueLoading not implemented");
        }

        @Deprecated
        public static boolean $default$shouldStartPlayback(LoadControl _this, long bufferedDurationUs, float playbackSpeed, boolean rebuffering, long targetLiveOffsetUs) {
            throw new IllegalStateException("shouldStartPlayback not implemented");
        }
    }
}
