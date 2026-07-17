package androidx.media3.exoplayer.trackselection;

import androidx.media3.common.Format;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.Log;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.chunk.Chunk;
import androidx.media3.exoplayer.source.chunk.MediaChunk;
import androidx.media3.exoplayer.source.chunk.MediaChunkIterator;
import androidx.media3.exoplayer.upstream.BandwidthMeter;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public interface ExoTrackSelection extends TrackSelection {

    public interface Factory {
        ExoTrackSelection[] createTrackSelections(Definition[] definitionArr, BandwidthMeter bandwidthMeter, MediaSource.MediaPeriodId mediaPeriodId, Timeline timeline);
    }

    void disable();

    void enable();

    int evaluateQueueSize(long j, List<? extends MediaChunk> list);

    boolean excludeTrack(int i, long j);

    long getLatestBitrateEstimate();

    Format getSelectedFormat();

    int getSelectedIndex();

    int getSelectedIndexInTrackGroup();

    Object getSelectionData();

    int getSelectionReason();

    boolean isTrackExcluded(int i, long j);

    void onDiscontinuity();

    void onPlayWhenReadyChanged(boolean z);

    void onPlaybackSpeed(float f);

    void onRebuffer();

    boolean shouldCancelChunkLoad(long j, Chunk chunk, List<? extends MediaChunk> list);

    void updateSelectedTrack(long j, long j2, long j3, List<? extends MediaChunk> list, MediaChunkIterator[] mediaChunkIteratorArr);

    public static final class Definition {
        private static final String TAG = "ETSDefinition";
        public final TrackGroup group;
        public final int[] tracks;
        public final int type;

        public Definition(TrackGroup group, int... tracks) {
            this(group, tracks, 0);
        }

        public Definition(TrackGroup group, int[] tracks, int type) {
            if (tracks.length == 0) {
                Log.e(TAG, "Empty tracks are not allowed", new IllegalArgumentException());
            }
            this.group = group;
            this.tracks = tracks;
            this.type = type;
        }
    }

    /* JADX INFO: renamed from: androidx.media3.exoplayer.trackselection.ExoTrackSelection$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
        public static void $default$onDiscontinuity(ExoTrackSelection _this) {
        }

        public static void $default$onRebuffer(ExoTrackSelection _this) {
        }

        public static void $default$onPlayWhenReadyChanged(ExoTrackSelection _this, boolean playWhenReady) {
        }

        public static boolean $default$shouldCancelChunkLoad(ExoTrackSelection _this, long playbackPositionUs, Chunk loadingChunk, List list) {
            return false;
        }

        public static long $default$getLatestBitrateEstimate(ExoTrackSelection _this) {
            return -2147483647L;
        }
    }
}
