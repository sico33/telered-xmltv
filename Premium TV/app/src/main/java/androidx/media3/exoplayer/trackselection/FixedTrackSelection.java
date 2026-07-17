package androidx.media3.exoplayer.trackselection;

import androidx.media3.common.TrackGroup;
import androidx.media3.exoplayer.source.chunk.MediaChunk;
import androidx.media3.exoplayer.source.chunk.MediaChunkIterator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class FixedTrackSelection extends BaseTrackSelection {
    private final Object data;
    private final int reason;

    public FixedTrackSelection(TrackGroup group, int track) {
        this(group, track, 0);
    }

    public FixedTrackSelection(TrackGroup group, int track, int type) {
        this(group, track, type, 0, null);
    }

    public FixedTrackSelection(TrackGroup group, int track, int type, int reason, Object data) {
        super(group, new int[]{track}, type);
        this.reason = reason;
        this.data = data;
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public void updateSelectedTrack(long playbackPositionUs, long bufferedDurationUs, long availableDurationUs, List<? extends MediaChunk> queue, MediaChunkIterator[] mediaChunkIterators) {
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public int getSelectedIndex() {
        return 0;
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public int getSelectionReason() {
        return this.reason;
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public Object getSelectionData() {
        return this.data;
    }
}
