package androidx.media3.exoplayer.source;

import android.net.Uri;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.TrackGroup;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.exoplayer.FormatHolder;
import androidx.media3.exoplayer.LoadingInfo;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import com.google.common.base.Charsets;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/* JADX INFO: loaded from: classes.dex */
final class ExternallyLoadedMediaPeriod implements MediaPeriod {
    private final ExternalLoader externalLoader;
    private final AtomicBoolean loadingFinished;
    private ListenableFuture<?> loadingFuture;
    private final AtomicReference<Throwable> loadingThrowable;
    private final byte[] sampleData;
    private final TrackGroupArray tracks;
    private final Uri uri;

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public /* synthetic */ List getStreamKeys(List list) {
        return Collections.emptyList();
    }

    public ExternallyLoadedMediaPeriod(Uri uri, String mimeType, ExternalLoader externalLoader) {
        this.uri = uri;
        Format format = new Format.Builder().setSampleMimeType(mimeType).build();
        this.externalLoader = externalLoader;
        this.tracks = new TrackGroupArray(new TrackGroup(format));
        this.sampleData = uri.toString().getBytes(Charsets.UTF_8);
        this.loadingFinished = new AtomicBoolean();
        this.loadingThrowable = new AtomicReference<>();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void prepare(MediaPeriod.Callback callback, long positionUs) {
        callback.onPrepared(this);
        this.loadingFuture = this.externalLoader.load(new ExternalLoader.LoadRequest(this.uri));
        Futures.addCallback(this.loadingFuture, new FutureCallback<Object>() { // from class: androidx.media3.exoplayer.source.ExternallyLoadedMediaPeriod.1
            @Override // com.google.common.util.concurrent.FutureCallback
            public void onSuccess(Object result) {
                ExternallyLoadedMediaPeriod.this.loadingFinished.set(true);
            }

            @Override // com.google.common.util.concurrent.FutureCallback
            public void onFailure(Throwable t) {
                ExternallyLoadedMediaPeriod.this.loadingThrowable.set(t);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void maybeThrowPrepareError() {
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public TrackGroupArray getTrackGroups() {
        return this.tracks;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long selectTracks(ExoTrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
        for (int i = 0; i < selections.length; i++) {
            if (streams[i] != null && (selections[i] == null || !mayRetainStreamFlags[i])) {
                streams[i] = null;
            }
            if (streams[i] == null && selections[i] != null) {
                SampleStreamImpl stream = new SampleStreamImpl();
                streams[i] = stream;
                streamResetFlags[i] = true;
            }
        }
        return positionUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void discardBuffer(long positionUs, boolean toKeyframe) {
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long readDiscontinuity() {
        return C.TIME_UNSET;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long seekToUs(long positionUs) {
        return positionUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
        return positionUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public long getBufferedPositionUs() {
        return this.loadingFinished.get() ? Long.MIN_VALUE : 0L;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public long getNextLoadPositionUs() {
        return this.loadingFinished.get() ? Long.MIN_VALUE : 0L;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public boolean continueLoading(LoadingInfo loadingInfo) {
        return !this.loadingFinished.get();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public boolean isLoading() {
        return !this.loadingFinished.get();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public void reevaluateBuffer(long positionUs) {
    }

    public void releasePeriod() {
        if (this.loadingFuture != null) {
            this.loadingFuture.cancel(false);
        }
    }

    private final class SampleStreamImpl implements SampleStream {
        private static final int STREAM_STATE_END_OF_STREAM = 2;
        private static final int STREAM_STATE_SEND_FORMAT = 0;
        private static final int STREAM_STATE_SEND_SAMPLE = 1;
        private int streamState = 0;

        public SampleStreamImpl() {
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public boolean isReady() {
            return ExternallyLoadedMediaPeriod.this.loadingFinished.get();
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public void maybeThrowError() throws IOException {
            Throwable loadingThrowable = (Throwable) ExternallyLoadedMediaPeriod.this.loadingThrowable.get();
            if (loadingThrowable != null) {
                throw new IOException(loadingThrowable);
            }
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, int readFlags) {
            if (this.streamState == 2) {
                buffer.addFlag(4);
                return -4;
            }
            if ((readFlags & 2) != 0 || this.streamState == 0) {
                formatHolder.format = ExternallyLoadedMediaPeriod.this.tracks.get(0).getFormat(0);
                this.streamState = 1;
                return -5;
            }
            if (ExternallyLoadedMediaPeriod.this.loadingFinished.get()) {
                int sampleSize = ExternallyLoadedMediaPeriod.this.sampleData.length;
                buffer.addFlag(1);
                buffer.timeUs = 0L;
                if ((readFlags & 4) == 0) {
                    buffer.ensureSpaceForWrite(sampleSize);
                    buffer.data.put(ExternallyLoadedMediaPeriod.this.sampleData, 0, sampleSize);
                }
                if ((readFlags & 1) == 0) {
                    this.streamState = 2;
                }
                return -4;
            }
            return -3;
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public int skipData(long positionUs) {
            return 0;
        }
    }
}
