package androidx.media3.exoplayer.source;

import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import androidx.media3.exoplayer.LoadingInfo;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class CompositeSequenceableLoader implements SequenceableLoader {
    private long lastAudioVideoBufferedPositionUs;
    private final ImmutableList<SequenceableLoaderWithTrackTypes> loadersWithTrackTypes;

    @Deprecated
    public CompositeSequenceableLoader(SequenceableLoader[] loaders) {
        this(ImmutableList.copyOf(loaders), Collections.nCopies(loaders.length, ImmutableList.of(-1)));
    }

    public CompositeSequenceableLoader(List<? extends SequenceableLoader> loaders, List<List<Integer>> loaderTrackTypes) {
        ImmutableList.Builder<SequenceableLoaderWithTrackTypes> loaderAndTrackTypes = ImmutableList.builder();
        Assertions.checkArgument(loaders.size() == loaderTrackTypes.size());
        for (int i = 0; i < loaders.size(); i++) {
            loaderAndTrackTypes.add(new SequenceableLoaderWithTrackTypes(loaders.get(i), loaderTrackTypes.get(i)));
        }
        this.loadersWithTrackTypes = loaderAndTrackTypes.build();
        this.lastAudioVideoBufferedPositionUs = C.TIME_UNSET;
    }

    @Override // androidx.media3.exoplayer.source.SequenceableLoader
    public long getBufferedPositionUs() {
        long bufferedPositionUs = Long.MAX_VALUE;
        long bufferedPositionAudioVideoUs = Long.MAX_VALUE;
        for (int i = 0; i < this.loadersWithTrackTypes.size(); i++) {
            SequenceableLoaderWithTrackTypes loader = this.loadersWithTrackTypes.get(i);
            long loaderBufferedPositionUs = loader.getBufferedPositionUs();
            if ((loader.getTrackTypes().contains(1) || loader.getTrackTypes().contains(2) || loader.getTrackTypes().contains(4)) && loaderBufferedPositionUs != Long.MIN_VALUE) {
                bufferedPositionAudioVideoUs = Math.min(bufferedPositionAudioVideoUs, loaderBufferedPositionUs);
            }
            if (loaderBufferedPositionUs != Long.MIN_VALUE) {
                bufferedPositionUs = Math.min(bufferedPositionUs, loaderBufferedPositionUs);
            }
        }
        if (bufferedPositionAudioVideoUs != Long.MAX_VALUE) {
            this.lastAudioVideoBufferedPositionUs = bufferedPositionAudioVideoUs;
            return bufferedPositionAudioVideoUs;
        }
        if (bufferedPositionUs == Long.MAX_VALUE) {
            return Long.MIN_VALUE;
        }
        if (this.lastAudioVideoBufferedPositionUs != C.TIME_UNSET) {
            return this.lastAudioVideoBufferedPositionUs;
        }
        return bufferedPositionUs;
    }

    @Override // androidx.media3.exoplayer.source.SequenceableLoader
    public long getNextLoadPositionUs() {
        long nextLoadPositionUs = Long.MAX_VALUE;
        for (int i = 0; i < this.loadersWithTrackTypes.size(); i++) {
            SequenceableLoaderWithTrackTypes loader = this.loadersWithTrackTypes.get(i);
            long loaderNextLoadPositionUs = loader.getNextLoadPositionUs();
            if (loaderNextLoadPositionUs != Long.MIN_VALUE) {
                nextLoadPositionUs = Math.min(nextLoadPositionUs, loaderNextLoadPositionUs);
            }
        }
        if (nextLoadPositionUs == Long.MAX_VALUE) {
            return Long.MIN_VALUE;
        }
        return nextLoadPositionUs;
    }

    @Override // androidx.media3.exoplayer.source.SequenceableLoader
    public void reevaluateBuffer(long positionUs) {
        for (int i = 0; i < this.loadersWithTrackTypes.size(); i++) {
            this.loadersWithTrackTypes.get(i).reevaluateBuffer(positionUs);
        }
    }

    @Override // androidx.media3.exoplayer.source.SequenceableLoader
    public boolean continueLoading(LoadingInfo loadingInfo) {
        boolean madeProgressThisIteration;
        boolean madeProgress = false;
        do {
            madeProgressThisIteration = false;
            long nextLoadPositionUs = getNextLoadPositionUs();
            if (nextLoadPositionUs == Long.MIN_VALUE) {
                break;
            }
            for (int i = 0; i < this.loadersWithTrackTypes.size(); i++) {
                long loaderNextLoadPositionUs = this.loadersWithTrackTypes.get(i).getNextLoadPositionUs();
                boolean isLoaderBehind = loaderNextLoadPositionUs != Long.MIN_VALUE && loaderNextLoadPositionUs <= loadingInfo.playbackPositionUs;
                if (loaderNextLoadPositionUs == nextLoadPositionUs || isLoaderBehind) {
                    madeProgressThisIteration |= this.loadersWithTrackTypes.get(i).continueLoading(loadingInfo);
                }
            }
            madeProgress |= madeProgressThisIteration;
        } while (madeProgressThisIteration);
        return madeProgress;
    }

    @Override // androidx.media3.exoplayer.source.SequenceableLoader
    public boolean isLoading() {
        for (int i = 0; i < this.loadersWithTrackTypes.size(); i++) {
            if (this.loadersWithTrackTypes.get(i).isLoading()) {
                return true;
            }
        }
        return false;
    }

    private static final class SequenceableLoaderWithTrackTypes implements SequenceableLoader {
        private final SequenceableLoader loader;
        private final ImmutableList<Integer> trackTypes;

        public SequenceableLoaderWithTrackTypes(SequenceableLoader loader, List<Integer> trackTypes) {
            this.loader = loader;
            this.trackTypes = ImmutableList.copyOf((Collection) trackTypes);
        }

        public ImmutableList<Integer> getTrackTypes() {
            return this.trackTypes;
        }

        @Override // androidx.media3.exoplayer.source.SequenceableLoader
        public long getBufferedPositionUs() {
            return this.loader.getBufferedPositionUs();
        }

        @Override // androidx.media3.exoplayer.source.SequenceableLoader
        public long getNextLoadPositionUs() {
            return this.loader.getNextLoadPositionUs();
        }

        @Override // androidx.media3.exoplayer.source.SequenceableLoader
        public boolean continueLoading(LoadingInfo loadingInfo) {
            return this.loader.continueLoading(loadingInfo);
        }

        @Override // androidx.media3.exoplayer.source.SequenceableLoader
        public boolean isLoading() {
            return this.loader.isLoading();
        }

        @Override // androidx.media3.exoplayer.source.SequenceableLoader
        public void reevaluateBuffer(long positionUs) {
            this.loader.reevaluateBuffer(positionUs);
        }
    }
}
