package androidx.media3.exoplayer.source;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSourceUtil;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.StatsDataSource;
import androidx.media3.datasource.TransferListener;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.exoplayer.FormatHolder;
import androidx.media3.exoplayer.LoadingInfo;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import androidx.media3.exoplayer.upstream.Loader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class SingleSampleMediaPeriod implements MediaPeriod, Loader.Callback<SourceLoadable> {
    private static final int INITIAL_SAMPLE_SIZE = 1024;
    private static final String TAG = "SingleSampleMediaPeriod";
    private final DataSource.Factory dataSourceFactory;
    private final DataSpec dataSpec;
    private final long durationUs;
    private final MediaSourceEventListener.EventDispatcher eventDispatcher;
    final Format format;
    private final LoadErrorHandlingPolicy loadErrorHandlingPolicy;
    boolean loadingFinished;
    byte[] sampleData;
    int sampleSize;
    private final TrackGroupArray tracks;
    private final TransferListener transferListener;
    final boolean treatLoadErrorsAsEndOfStream;
    private final ArrayList<SampleStreamImpl> sampleStreams = new ArrayList<>();
    final Loader loader = new Loader(TAG);

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public /* synthetic */ List getStreamKeys(List list) {
        return Collections.emptyList();
    }

    public SingleSampleMediaPeriod(DataSpec dataSpec, DataSource.Factory dataSourceFactory, TransferListener transferListener, Format format, long durationUs, LoadErrorHandlingPolicy loadErrorHandlingPolicy, MediaSourceEventListener.EventDispatcher eventDispatcher, boolean treatLoadErrorsAsEndOfStream) {
        this.dataSpec = dataSpec;
        this.dataSourceFactory = dataSourceFactory;
        this.transferListener = transferListener;
        this.format = format;
        this.durationUs = durationUs;
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy;
        this.eventDispatcher = eventDispatcher;
        this.treatLoadErrorsAsEndOfStream = treatLoadErrorsAsEndOfStream;
        this.tracks = new TrackGroupArray(new TrackGroup(format));
    }

    public void release() {
        this.loader.release();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void prepare(MediaPeriod.Callback callback, long positionUs) {
        callback.onPrepared(this);
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
                this.sampleStreams.remove(streams[i]);
                streams[i] = null;
            }
            if (streams[i] == null && selections[i] != null) {
                SampleStreamImpl stream = new SampleStreamImpl();
                this.sampleStreams.add(stream);
                streams[i] = stream;
                streamResetFlags[i] = true;
            }
        }
        return positionUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void discardBuffer(long positionUs, boolean toKeyframe) {
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public void reevaluateBuffer(long positionUs) {
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public boolean continueLoading(LoadingInfo loadingInfo) {
        if (this.loadingFinished || this.loader.isLoading() || this.loader.hasFatalError()) {
            return false;
        }
        DataSource dataSource = this.dataSourceFactory.createDataSource();
        if (this.transferListener != null) {
            dataSource.addTransferListener(this.transferListener);
        }
        SourceLoadable loadable = new SourceLoadable(this.dataSpec, dataSource);
        long elapsedRealtimeMs = this.loader.startLoading(loadable, this, this.loadErrorHandlingPolicy.getMinimumLoadableRetryCount(1));
        this.eventDispatcher.loadStarted(new LoadEventInfo(loadable.loadTaskId, this.dataSpec, elapsedRealtimeMs), 1, -1, this.format, 0, null, 0L, this.durationUs);
        return true;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public boolean isLoading() {
        return this.loader.isLoading();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long readDiscontinuity() {
        return C.TIME_UNSET;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public long getNextLoadPositionUs() {
        return (this.loadingFinished || this.loader.isLoading()) ? Long.MIN_VALUE : 0L;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public long getBufferedPositionUs() {
        return this.loadingFinished ? Long.MIN_VALUE : 0L;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long seekToUs(long positionUs) {
        for (int i = 0; i < this.sampleStreams.size(); i++) {
            this.sampleStreams.get(i).reset();
        }
        return positionUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
        return positionUs;
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Callback
    public void onLoadCompleted(SourceLoadable loadable, long elapsedRealtimeMs, long loadDurationMs) {
        this.sampleSize = (int) loadable.dataSource.getBytesRead();
        this.sampleData = (byte[]) Assertions.checkNotNull(loadable.sampleData);
        this.loadingFinished = true;
        StatsDataSource dataSource = loadable.dataSource;
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, dataSource.getLastOpenedUri(), dataSource.getLastResponseHeaders(), elapsedRealtimeMs, loadDurationMs, this.sampleSize);
        this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        this.eventDispatcher.loadCompleted(loadEventInfo, 1, -1, this.format, 0, null, 0L, this.durationUs);
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Callback
    public void onLoadCanceled(SourceLoadable loadable, long elapsedRealtimeMs, long loadDurationMs, boolean released) {
        StatsDataSource dataSource = loadable.dataSource;
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, dataSource.getLastOpenedUri(), dataSource.getLastResponseHeaders(), elapsedRealtimeMs, loadDurationMs, dataSource.getBytesRead());
        this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        this.eventDispatcher.loadCanceled(loadEventInfo, 1, -1, null, 0, null, 0L, this.durationUs);
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Callback
    public Loader.LoadErrorAction onLoadError(SourceLoadable loadable, long elapsedRealtimeMs, long loadDurationMs, IOException error, int errorCount) {
        Loader.LoadErrorAction loadErrorActionCreateRetryAction;
        Loader.LoadErrorAction action;
        StatsDataSource dataSource = loadable.dataSource;
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, dataSource.getLastOpenedUri(), dataSource.getLastResponseHeaders(), elapsedRealtimeMs, loadDurationMs, dataSource.getBytesRead());
        MediaLoadData mediaLoadData = new MediaLoadData(1, -1, this.format, 0, null, 0L, Util.usToMs(this.durationUs));
        long retryDelay = this.loadErrorHandlingPolicy.getRetryDelayMsFor(new LoadErrorHandlingPolicy.LoadErrorInfo(loadEventInfo, mediaLoadData, error, errorCount));
        boolean errorCanBePropagated = retryDelay == C.TIME_UNSET || errorCount >= this.loadErrorHandlingPolicy.getMinimumLoadableRetryCount(1);
        if (this.treatLoadErrorsAsEndOfStream && errorCanBePropagated) {
            Log.w(TAG, "Loading failed, treating as end-of-stream.", error);
            this.loadingFinished = true;
            action = Loader.DONT_RETRY;
        } else {
            if (retryDelay != C.TIME_UNSET) {
                loadErrorActionCreateRetryAction = Loader.createRetryAction(false, retryDelay);
            } else {
                loadErrorActionCreateRetryAction = Loader.DONT_RETRY_FATAL;
            }
            action = loadErrorActionCreateRetryAction;
        }
        boolean wasCanceled = !action.isRetry();
        this.eventDispatcher.loadError(loadEventInfo, 1, -1, this.format, 0, null, 0L, this.durationUs, error, wasCanceled);
        if (wasCanceled) {
            this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        }
        return action;
    }

    private final class SampleStreamImpl implements SampleStream {
        private static final int STREAM_STATE_END_OF_STREAM = 2;
        private static final int STREAM_STATE_SEND_FORMAT = 0;
        private static final int STREAM_STATE_SEND_SAMPLE = 1;
        private boolean notifiedDownstreamFormat;
        private int streamState;

        private SampleStreamImpl() {
        }

        public void reset() {
            if (this.streamState == 2) {
                this.streamState = 1;
            }
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public boolean isReady() {
            return SingleSampleMediaPeriod.this.loadingFinished;
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public void maybeThrowError() throws IOException {
            if (!SingleSampleMediaPeriod.this.treatLoadErrorsAsEndOfStream) {
                SingleSampleMediaPeriod.this.loader.maybeThrowError();
            }
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, int readFlags) {
            maybeNotifyDownstreamFormat();
            if (SingleSampleMediaPeriod.this.loadingFinished && SingleSampleMediaPeriod.this.sampleData == null) {
                this.streamState = 2;
            }
            if (this.streamState == 2) {
                buffer.addFlag(4);
                return -4;
            }
            if ((readFlags & 2) != 0 || this.streamState == 0) {
                formatHolder.format = SingleSampleMediaPeriod.this.format;
                this.streamState = 1;
                return -5;
            }
            if (!SingleSampleMediaPeriod.this.loadingFinished) {
                return -3;
            }
            Assertions.checkNotNull(SingleSampleMediaPeriod.this.sampleData);
            buffer.addFlag(1);
            buffer.timeUs = 0L;
            if ((readFlags & 4) == 0) {
                buffer.ensureSpaceForWrite(SingleSampleMediaPeriod.this.sampleSize);
                buffer.data.put(SingleSampleMediaPeriod.this.sampleData, 0, SingleSampleMediaPeriod.this.sampleSize);
            }
            if ((readFlags & 1) == 0) {
                this.streamState = 2;
            }
            return -4;
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public int skipData(long positionUs) {
            maybeNotifyDownstreamFormat();
            if (positionUs > 0 && this.streamState != 2) {
                this.streamState = 2;
                return 1;
            }
            return 0;
        }

        private void maybeNotifyDownstreamFormat() {
            if (!this.notifiedDownstreamFormat) {
                SingleSampleMediaPeriod.this.eventDispatcher.downstreamFormatChanged(MimeTypes.getTrackType(SingleSampleMediaPeriod.this.format.sampleMimeType), SingleSampleMediaPeriod.this.format, 0, null, 0L);
                this.notifiedDownstreamFormat = true;
            }
        }
    }

    static final class SourceLoadable implements Loader.Loadable {
        private final StatsDataSource dataSource;
        public final DataSpec dataSpec;
        public final long loadTaskId = LoadEventInfo.getNewId();
        private byte[] sampleData;

        public SourceLoadable(DataSpec dataSpec, DataSource dataSource) {
            this.dataSpec = dataSpec;
            this.dataSource = new StatsDataSource(dataSource);
        }

        @Override // androidx.media3.exoplayer.upstream.Loader.Loadable
        public void cancelLoad() {
        }

        @Override // androidx.media3.exoplayer.upstream.Loader.Loadable
        public void load() throws IOException {
            this.dataSource.resetBytesRead();
            try {
                this.dataSource.open(this.dataSpec);
                int result = 0;
                while (true) {
                    StatsDataSource statsDataSource = this.dataSource;
                    if (result != -1) {
                        int sampleSize = (int) statsDataSource.getBytesRead();
                        if (this.sampleData == null) {
                            this.sampleData = new byte[1024];
                        } else if (sampleSize == this.sampleData.length) {
                            this.sampleData = Arrays.copyOf(this.sampleData, this.sampleData.length * 2);
                        }
                        result = this.dataSource.read(this.sampleData, sampleSize, this.sampleData.length - sampleSize);
                    } else {
                        return;
                    }
                }
            } finally {
                DataSourceUtil.closeQuietly(this.dataSource);
            }
        }
    }
}
