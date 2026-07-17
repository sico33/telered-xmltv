package androidx.media3.exoplayer.trackselection;

import android.os.SystemClock;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackGroup;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.chunk.MediaChunk;
import androidx.media3.exoplayer.source.chunk.MediaChunkIterator;
import androidx.media3.exoplayer.upstream.BandwidthMeter;
import java.util.List;
import java.util.Random;

/* JADX INFO: loaded from: classes.dex */
public final class RandomTrackSelection extends BaseTrackSelection {
    private final Random random;
    private int selectedIndex;

    public static final class Factory implements ExoTrackSelection.Factory {
        private final Random random;

        public Factory() {
            this.random = new Random();
        }

        public Factory(int seed) {
            this.random = new Random(seed);
        }

        @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection.Factory
        public ExoTrackSelection[] createTrackSelections(ExoTrackSelection.Definition[] definitions, BandwidthMeter bandwidthMeter, MediaSource.MediaPeriodId mediaPeriodId, Timeline timeline) {
            return TrackSelectionUtil.createTrackSelectionsForDefinitions(definitions, new TrackSelectionUtil.AdaptiveTrackSelectionFactory() { // from class: androidx.media3.exoplayer.trackselection.RandomTrackSelection$Factory$$ExternalSyntheticLambda0
                @Override // androidx.media3.exoplayer.trackselection.TrackSelectionUtil.AdaptiveTrackSelectionFactory
                public final ExoTrackSelection createAdaptiveTrackSelection(ExoTrackSelection.Definition definition) {
                    return this.f$0.m137xa167648d(definition);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$createTrackSelections$0$androidx-media3-exoplayer-trackselection-RandomTrackSelection$Factory, reason: not valid java name */
        /* synthetic */ ExoTrackSelection m137xa167648d(ExoTrackSelection.Definition definition) {
            return new RandomTrackSelection(definition.group, definition.tracks, definition.type, this.random);
        }
    }

    public RandomTrackSelection(TrackGroup group, int[] tracks, int type, Random random) {
        super(group, tracks, type);
        this.random = random;
        this.selectedIndex = random.nextInt(this.length);
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public void updateSelectedTrack(long playbackPositionUs, long bufferedDurationUs, long availableDurationUs, List<? extends MediaChunk> queue, MediaChunkIterator[] mediaChunkIterators) {
        long nowMs = SystemClock.elapsedRealtime();
        int allowedFormatCount = 0;
        for (int i = 0; i < this.length; i++) {
            if (!isTrackExcluded(i, nowMs)) {
                allowedFormatCount++;
            }
        }
        this.selectedIndex = this.random.nextInt(allowedFormatCount);
        if (allowedFormatCount != this.length) {
            int allowedFormatCount2 = 0;
            for (int i2 = 0; i2 < this.length; i2++) {
                if (!isTrackExcluded(i2, nowMs)) {
                    int allowedFormatCount3 = allowedFormatCount2 + 1;
                    if (this.selectedIndex != allowedFormatCount2) {
                        allowedFormatCount2 = allowedFormatCount3;
                    } else {
                        this.selectedIndex = i2;
                        return;
                    }
                }
            }
        }
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public int getSelectedIndex() {
        return this.selectedIndex;
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public int getSelectionReason() {
        return 3;
    }

    @Override // androidx.media3.exoplayer.trackselection.ExoTrackSelection
    public Object getSelectionData() {
        return null;
    }
}
