package androidx.media3.exoplayer.source;

import androidx.media3.common.StreamKey;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.Assertions;
import androidx.media3.exoplayer.LoadingInfo;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.upstream.Allocator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public class FilteringMediaSource extends WrappingMediaSource {
    private final ImmutableSet<Integer> trackTypes;

    public FilteringMediaSource(MediaSource mediaSource, int trackType) {
        this(mediaSource, ImmutableSet.of(Integer.valueOf(trackType)));
    }

    public FilteringMediaSource(MediaSource mediaSource, Set<Integer> trackTypes) {
        super(mediaSource);
        this.trackTypes = ImmutableSet.copyOf((Collection) trackTypes);
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource, androidx.media3.exoplayer.source.MediaSource
    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator, long startPositionUs) {
        MediaPeriod wrappedPeriod = super.createPeriod(id, allocator, startPositionUs);
        return new FilteringMediaPeriod(wrappedPeriod, this.trackTypes);
    }

    @Override // androidx.media3.exoplayer.source.WrappingMediaSource, androidx.media3.exoplayer.source.MediaSource
    public void releasePeriod(MediaPeriod mediaPeriod) {
        MediaPeriod wrappedPeriod = ((FilteringMediaPeriod) mediaPeriod).mediaPeriod;
        super.releasePeriod(wrappedPeriod);
    }

    private static final class FilteringMediaPeriod implements MediaPeriod, MediaPeriod.Callback {
        private MediaPeriod.Callback callback;
        private TrackGroupArray filteredTrackGroups;
        public final MediaPeriod mediaPeriod;
        private final ImmutableSet<Integer> trackTypes;

        public FilteringMediaPeriod(MediaPeriod mediaPeriod, ImmutableSet<Integer> trackTypes) {
            this.mediaPeriod = mediaPeriod;
            this.trackTypes = trackTypes;
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public void prepare(MediaPeriod.Callback callback, long positionUs) {
            this.callback = callback;
            this.mediaPeriod.prepare(this, positionUs);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public void maybeThrowPrepareError() throws IOException {
            this.mediaPeriod.maybeThrowPrepareError();
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public TrackGroupArray getTrackGroups() {
            return (TrackGroupArray) Assertions.checkNotNull(this.filteredTrackGroups);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public List<StreamKey> getStreamKeys(List<ExoTrackSelection> trackSelections) {
            return this.mediaPeriod.getStreamKeys(trackSelections);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public long selectTracks(ExoTrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
            return this.mediaPeriod.selectTracks(selections, mayRetainStreamFlags, streams, streamResetFlags, positionUs);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public void discardBuffer(long positionUs, boolean toKeyframe) {
            this.mediaPeriod.discardBuffer(positionUs, toKeyframe);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public long readDiscontinuity() {
            return this.mediaPeriod.readDiscontinuity();
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public long seekToUs(long positionUs) {
            return this.mediaPeriod.seekToUs(positionUs);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod
        public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
            return this.mediaPeriod.getAdjustedSeekPositionUs(positionUs, seekParameters);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
        public long getBufferedPositionUs() {
            return this.mediaPeriod.getBufferedPositionUs();
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
        public long getNextLoadPositionUs() {
            return this.mediaPeriod.getNextLoadPositionUs();
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
        public boolean continueLoading(LoadingInfo loadingInfo) {
            return this.mediaPeriod.continueLoading(loadingInfo);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
        public boolean isLoading() {
            return this.mediaPeriod.isLoading();
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
        public void reevaluateBuffer(long positionUs) {
            this.mediaPeriod.reevaluateBuffer(positionUs);
        }

        @Override // androidx.media3.exoplayer.source.MediaPeriod.Callback
        public void onPrepared(MediaPeriod mediaPeriod) {
            TrackGroupArray trackGroups = mediaPeriod.getTrackGroups();
            ImmutableList.Builder<TrackGroup> trackGroupsBuilder = ImmutableList.builder();
            for (int i = 0; i < trackGroups.length; i++) {
                TrackGroup trackGroup = trackGroups.get(i);
                if (this.trackTypes.contains(Integer.valueOf(trackGroup.type))) {
                    trackGroupsBuilder.add(trackGroup);
                }
            }
            this.filteredTrackGroups = new TrackGroupArray((TrackGroup[]) trackGroupsBuilder.build().toArray(new TrackGroup[0]));
            ((MediaPeriod.Callback) Assertions.checkNotNull(this.callback)).onPrepared(this);
        }

        @Override // androidx.media3.exoplayer.source.SequenceableLoader.Callback
        public void onContinueLoadingRequested(MediaPeriod source) {
            ((MediaPeriod.Callback) Assertions.checkNotNull(this.callback)).onContinueLoadingRequested(this);
        }
    }
}
