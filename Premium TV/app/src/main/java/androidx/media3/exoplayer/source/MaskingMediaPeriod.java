package androidx.media3.exoplayer.source;

import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.LoadingInfo;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.upstream.Allocator;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class MaskingMediaPeriod implements MediaPeriod, MediaPeriod.Callback {
    private final Allocator allocator;
    private MediaPeriod.Callback callback;
    public final MediaSource.MediaPeriodId id;
    private PrepareListener listener;
    private MediaPeriod mediaPeriod;
    private MediaSource mediaSource;
    private boolean notifiedPrepareError;
    private long preparePositionOverrideUs = C.TIME_UNSET;
    private final long preparePositionUs;

    public interface PrepareListener {
        void onPrepareComplete(MediaSource.MediaPeriodId mediaPeriodId);

        void onPrepareError(MediaSource.MediaPeriodId mediaPeriodId, IOException iOException);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public /* synthetic */ List getStreamKeys(List list) {
        return Collections.emptyList();
    }

    public MaskingMediaPeriod(MediaSource.MediaPeriodId id, Allocator allocator, long preparePositionUs) {
        this.id = id;
        this.allocator = allocator;
        this.preparePositionUs = preparePositionUs;
    }

    public void setPrepareListener(PrepareListener listener) {
        this.listener = listener;
    }

    public long getPreparePositionUs() {
        return this.preparePositionUs;
    }

    public void overridePreparePositionUs(long preparePositionUs) {
        this.preparePositionOverrideUs = preparePositionUs;
    }

    public long getPreparePositionOverrideUs() {
        return this.preparePositionOverrideUs;
    }

    public void setMediaSource(MediaSource mediaSource) {
        Assertions.checkState(this.mediaSource == null);
        this.mediaSource = mediaSource;
    }

    public void createPeriod(MediaSource.MediaPeriodId id) {
        long preparePositionUs = getPreparePositionWithOverride(this.preparePositionUs);
        this.mediaPeriod = ((MediaSource) Assertions.checkNotNull(this.mediaSource)).createPeriod(id, this.allocator, preparePositionUs);
        if (this.callback != null) {
            this.mediaPeriod.prepare(this, preparePositionUs);
        }
    }

    public void releasePeriod() {
        if (this.mediaPeriod != null) {
            ((MediaSource) Assertions.checkNotNull(this.mediaSource)).releasePeriod(this.mediaPeriod);
        }
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void prepare(MediaPeriod.Callback callback, long positionUs) {
        this.callback = callback;
        if (this.mediaPeriod != null) {
            this.mediaPeriod.prepare(this, getPreparePositionWithOverride(this.preparePositionUs));
        }
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void maybeThrowPrepareError() throws IOException {
        try {
            if (this.mediaPeriod != null) {
                this.mediaPeriod.maybeThrowPrepareError();
            } else if (this.mediaSource != null) {
                this.mediaSource.maybeThrowSourceInfoRefreshError();
            }
        } catch (IOException e) {
            if (this.listener == null) {
                throw e;
            }
            if (!this.notifiedPrepareError) {
                this.notifiedPrepareError = true;
                this.listener.onPrepareError(this.id, e);
            }
        }
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public TrackGroupArray getTrackGroups() {
        return ((MediaPeriod) Util.castNonNull(this.mediaPeriod)).getTrackGroups();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long selectTracks(ExoTrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
        long positionUs2;
        if (this.preparePositionOverrideUs != C.TIME_UNSET && positionUs == this.preparePositionUs) {
            positionUs2 = this.preparePositionOverrideUs;
        } else {
            positionUs2 = positionUs;
        }
        this.preparePositionOverrideUs = C.TIME_UNSET;
        return ((MediaPeriod) Util.castNonNull(this.mediaPeriod)).selectTracks(selections, mayRetainStreamFlags, streams, streamResetFlags, positionUs2);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void discardBuffer(long positionUs, boolean toKeyframe) {
        ((MediaPeriod) Util.castNonNull(this.mediaPeriod)).discardBuffer(positionUs, toKeyframe);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long readDiscontinuity() {
        return ((MediaPeriod) Util.castNonNull(this.mediaPeriod)).readDiscontinuity();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public long getBufferedPositionUs() {
        return ((MediaPeriod) Util.castNonNull(this.mediaPeriod)).getBufferedPositionUs();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long seekToUs(long positionUs) {
        return ((MediaPeriod) Util.castNonNull(this.mediaPeriod)).seekToUs(positionUs);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
        return ((MediaPeriod) Util.castNonNull(this.mediaPeriod)).getAdjustedSeekPositionUs(positionUs, seekParameters);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public long getNextLoadPositionUs() {
        return ((MediaPeriod) Util.castNonNull(this.mediaPeriod)).getNextLoadPositionUs();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public void reevaluateBuffer(long positionUs) {
        ((MediaPeriod) Util.castNonNull(this.mediaPeriod)).reevaluateBuffer(positionUs);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public boolean continueLoading(LoadingInfo loadingInfo) {
        return this.mediaPeriod != null && this.mediaPeriod.continueLoading(loadingInfo);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public boolean isLoading() {
        return this.mediaPeriod != null && this.mediaPeriod.isLoading();
    }

    @Override // androidx.media3.exoplayer.source.SequenceableLoader.Callback
    public void onContinueLoadingRequested(MediaPeriod source) {
        ((MediaPeriod.Callback) Util.castNonNull(this.callback)).onContinueLoadingRequested(this);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod.Callback
    public void onPrepared(MediaPeriod mediaPeriod) {
        ((MediaPeriod.Callback) Util.castNonNull(this.callback)).onPrepared(this);
        if (this.listener != null) {
            this.listener.onPrepareComplete(this.id);
        }
    }

    private long getPreparePositionWithOverride(long preparePositionUs) {
        if (this.preparePositionOverrideUs != C.TIME_UNSET) {
            return this.preparePositionOverrideUs;
        }
        return preparePositionUs;
    }
}
