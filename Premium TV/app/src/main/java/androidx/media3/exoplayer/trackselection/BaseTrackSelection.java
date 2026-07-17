package androidx.media3.exoplayer.trackselection;

import android.os.SystemClock;
import androidx.media3.common.Format;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.source.chunk.Chunk;
import androidx.media3.exoplayer.source.chunk.MediaChunk;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public abstract class BaseTrackSelection implements ExoTrackSelection {
    private final long[] excludeUntilTimes;
    private final Format[] formats;
    protected final TrackGroup group;
    private int hashCode;
    protected final int length;
    protected final int[] tracks;
    private final int type;

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public /* synthetic */ long getLatestBitrateEstimate() {
        return ExoTrackSelection.CC.$default$getLatestBitrateEstimate(this);
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public /* synthetic */ void onDiscontinuity() {
        ExoTrackSelection.CC.$default$onDiscontinuity(this);
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public /* synthetic */ void onPlayWhenReadyChanged(boolean z) {
        ExoTrackSelection.CC.$default$onPlayWhenReadyChanged(this, z);
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public /* synthetic */ void onRebuffer() {
        ExoTrackSelection.CC.$default$onRebuffer(this);
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public /* synthetic */ boolean shouldCancelChunkLoad(long j, Chunk chunk, List list) {
        return ExoTrackSelection.CC.$default$shouldCancelChunkLoad(this, j, chunk, list);
    }

    public BaseTrackSelection(TrackGroup group, int... tracks) {
        this(group, tracks, 0);
    }

    public BaseTrackSelection(TrackGroup group, int[] tracks, int type) {
        Format[] formatArr;
        Assertions.checkState(tracks.length > 0);
        this.type = type;
        this.group = (TrackGroup) Assertions.checkNotNull(group);
        this.length = tracks.length;
        this.formats = new Format[this.length];
        int i = 0;
        while (true) {
            int length = tracks.length;
            formatArr = this.formats;
            if (i >= length) {
                break;
            }
            formatArr[i] = group.getFormat(tracks[i]);
            i++;
        }
        Arrays.sort(formatArr, new Comparator() { // from class: androidx.media3.exoplayer.trackselection.BaseTrackSelection$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return BaseTrackSelection.lambda$new$0((Format) obj, (Format) obj2);
            }
        });
        this.tracks = new int[this.length];
        for (int i2 = 0; i2 < this.length; i2++) {
            this.tracks[i2] = group.indexOf(this.formats[i2]);
        }
        int i3 = this.length;
        this.excludeUntilTimes = new long[i3];
    }

    static /* synthetic */ int lambda$new$0(Format a, Format b) {
        return b.bitrate - a.bitrate;
    }

    @Override // androidx.media3.exoplayer.trackselection.TrackSelection
    public final int getType() {
        return this.type;
    }

    @Override // androidx.media3.exoplayer.trackselection.TrackSelection
    public final TrackGroup getTrackGroup() {
        return this.group;
    }

    @Override // androidx.media3.exoplayer.trackselection.TrackSelection
    public final int length() {
        return this.tracks.length;
    }

    @Override // androidx.media3.exoplayer.trackselection.TrackSelection
    public final Format getFormat(int index) {
        return this.formats[index];
    }

    @Override // androidx.media3.exoplayer.trackselection.TrackSelection
    public final int getIndexInTrackGroup(int index) {
        return this.tracks[index];
    }

    @Override // androidx.media3.exoplayer.trackselection.TrackSelection
    public final int indexOf(Format format) {
        for (int i = 0; i < this.length; i++) {
            if (this.formats[i] == format) {
                return i;
            }
        }
        return -1;
    }

    @Override // androidx.media3.exoplayer.trackselection.TrackSelection
    public final int indexOf(int indexInTrackGroup) {
        for (int i = 0; i < this.length; i++) {
            if (this.tracks[i] == indexInTrackGroup) {
                return i;
            }
        }
        return -1;
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public final Format getSelectedFormat() {
        return this.formats[getSelectedIndex()];
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public final int getSelectedIndexInTrackGroup() {
        return this.tracks[getSelectedIndex()];
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public void enable() {
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public void disable() {
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public void onPlaybackSpeed(float playbackSpeed) {
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public int evaluateQueueSize(long playbackPositionUs, List<? extends MediaChunk> queue) {
        return queue.size();
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public boolean excludeTrack(int index, long exclusionDurationMs) {
        long nowMs = SystemClock.elapsedRealtime();
        boolean canExclude = isTrackExcluded(index, nowMs);
        int i = 0;
        boolean canExclude2 = canExclude;
        while (true) {
            boolean z = false;
            if (i >= this.length || canExclude2) {
                break;
            }
            if (i != index && !isTrackExcluded(i, nowMs)) {
                z = true;
            }
            canExclude2 = z;
            i++;
        }
        if (!canExclude2) {
            return false;
        }
        this.excludeUntilTimes[index] = Math.max(this.excludeUntilTimes[index], Util.addWithOverflowDefault(nowMs, exclusionDurationMs, Long.MAX_VALUE));
        return true;
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public boolean isTrackExcluded(int index, long nowMs) {
        return this.excludeUntilTimes[index] > nowMs;
    }

    public int hashCode() {
        if (this.hashCode == 0) {
            this.hashCode = (System.identityHashCode(this.group) * 31) + Arrays.hashCode(this.tracks);
        }
        return this.hashCode;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BaseTrackSelection other = (BaseTrackSelection) obj;
        if (this.group.equals(other.group) && Arrays.equals(this.tracks, other.tracks)) {
            return true;
        }
        return false;
    }
}
