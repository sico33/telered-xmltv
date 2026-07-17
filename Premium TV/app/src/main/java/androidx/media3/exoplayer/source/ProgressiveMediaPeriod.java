package androidx.media3.exoplayer.source;

import android.net.Uri;
import android.os.Handler;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.Metadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.ParserException;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ConditionVariable;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSourceUtil;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.StatsDataSource;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.exoplayer.FormatHolder;
import androidx.media3.exoplayer.LoadingInfo;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.drm.DrmSessionEventListener;
import androidx.media3.exoplayer.drm.DrmSessionManager;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.upstream.Allocator;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import androidx.media3.exoplayer.upstream.Loader;
import androidx.media3.extractor.DiscardingTrackOutput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.ForwardingSeekMap;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.TrackOutput;
import androidx.media3.extractor.metadata.icy.IcyHeaders;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

/* JADX INFO: loaded from: classes.dex */
final class ProgressiveMediaPeriod implements MediaPeriod, ExtractorOutput, Loader.Callback<ExtractingLoadable>, Loader.ReleaseCallback, SampleQueue.UpstreamFormatChangedListener {
    private static final long DEFAULT_LAST_SAMPLE_DURATION_US = 10000;
    private static final String TAG = "ProgressiveMediaPeriod";
    private final Allocator allocator;
    private MediaPeriod.Callback callback;
    private final long continueLoadingCheckIntervalBytes;
    private final String customCacheKey;
    private final DataSource dataSource;
    private final DrmSessionEventListener.EventDispatcher drmEventDispatcher;
    private final DrmSessionManager drmSessionManager;
    private long durationUs;
    private int enabledTrackCount;
    private int extractedSamplesCountAtStartOfLoad;
    private boolean haveAudioVideoTracks;
    private IcyHeaders icyHeaders;
    private boolean isLengthKnown;
    private boolean isLive;
    private boolean isSingleSample;
    private long lastSeekPositionUs;
    private final Listener listener;
    private final LoadErrorHandlingPolicy loadErrorHandlingPolicy;
    private boolean loadingFinished;
    private final MediaSourceEventListener.EventDispatcher mediaSourceEventDispatcher;
    private boolean notifyDiscontinuity;
    private boolean pendingDeferredRetry;
    private boolean prepared;
    private final ProgressiveMediaExtractor progressiveMediaExtractor;
    private boolean released;
    private boolean sampleQueuesBuilt;
    private SeekMap seekMap;
    private boolean seenFirstTrackSelection;
    private final long singleSampleDurationUs;
    private TrackState trackState;
    private final Uri uri;
    private static final Map<String, String> ICY_METADATA_HEADERS = createIcyMetadataHeaders();
    private static final Format ICY_FORMAT = new Format.Builder().setId("icy").setSampleMimeType(MimeTypes.APPLICATION_ICY).build();
    private final Loader loader = new Loader(TAG);
    private final ConditionVariable loadCondition = new ConditionVariable();
    private final Runnable maybeFinishPrepareRunnable = new Runnable() { // from class: androidx.media3.exoplayer.source.ProgressiveMediaPeriod$$ExternalSyntheticLambda1
        @Override // java.lang.Runnable
        public final void run() {
            this.f$0.maybeFinishPrepare();
        }
    };
    private final Runnable onContinueLoadingRequestedRunnable = new Runnable() { // from class: androidx.media3.exoplayer.source.ProgressiveMediaPeriod$$ExternalSyntheticLambda2
        @Override // java.lang.Runnable
        public final void run() {
            this.f$0.m119x97cae34d();
        }
    };
    private final Handler handler = Util.createHandlerForCurrentLooper();
    private TrackId[] sampleQueueTrackIds = new TrackId[0];
    private SampleQueue[] sampleQueues = new SampleQueue[0];
    private long pendingResetPositionUs = C.TIME_UNSET;
    private int dataType = 1;

    interface Listener {
        void onSourceInfoRefreshed(long j, boolean z, boolean z2);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public /* synthetic */ List getStreamKeys(List list) {
        return Collections.emptyList();
    }

    public ProgressiveMediaPeriod(Uri uri, DataSource dataSource, ProgressiveMediaExtractor progressiveMediaExtractor, DrmSessionManager drmSessionManager, DrmSessionEventListener.EventDispatcher drmEventDispatcher, LoadErrorHandlingPolicy loadErrorHandlingPolicy, MediaSourceEventListener.EventDispatcher mediaSourceEventDispatcher, Listener listener, Allocator allocator, String customCacheKey, int continueLoadingCheckIntervalBytes, long singleSampleDurationUs) {
        this.uri = uri;
        this.dataSource = dataSource;
        this.drmSessionManager = drmSessionManager;
        this.drmEventDispatcher = drmEventDispatcher;
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy;
        this.mediaSourceEventDispatcher = mediaSourceEventDispatcher;
        this.listener = listener;
        this.allocator = allocator;
        this.customCacheKey = customCacheKey;
        this.continueLoadingCheckIntervalBytes = continueLoadingCheckIntervalBytes;
        this.progressiveMediaExtractor = progressiveMediaExtractor;
        this.singleSampleDurationUs = singleSampleDurationUs;
    }

    /* JADX INFO: renamed from: lambda$new$0$androidx-media3-exoplayer-source-ProgressiveMediaPeriod, reason: not valid java name */
    /* synthetic */ void m119x97cae34d() {
        if (!this.released) {
            ((MediaPeriod.Callback) Assertions.checkNotNull(this.callback)).onContinueLoadingRequested(this);
        }
    }

    public void release() {
        if (this.prepared) {
            for (SampleQueue sampleQueue : this.sampleQueues) {
                sampleQueue.preRelease();
            }
        }
        this.loader.release(this);
        this.handler.removeCallbacksAndMessages(null);
        this.callback = null;
        this.released = true;
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.ReleaseCallback
    public void onLoaderReleased() {
        for (SampleQueue sampleQueue : this.sampleQueues) {
            sampleQueue.release();
        }
        this.progressiveMediaExtractor.release();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void prepare(MediaPeriod.Callback callback, long positionUs) {
        this.callback = callback;
        this.loadCondition.open();
        startLoading();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void maybeThrowPrepareError() throws IOException {
        maybeThrowError();
        if (this.loadingFinished && !this.prepared) {
            throw ParserException.createForMalformedContainer("Loading finished before preparation is complete.", null);
        }
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public TrackGroupArray getTrackGroups() {
        assertPrepared();
        return this.trackState.tracks;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long selectTracks(ExoTrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
        int i;
        long positionUs2 = positionUs;
        assertPrepared();
        TrackGroupArray tracks = this.trackState.tracks;
        boolean[] trackEnabledStates = this.trackState.trackEnabledStates;
        int oldEnabledTrackCount = this.enabledTrackCount;
        int i2 = 0;
        while (true) {
            i = 0;
            if (i2 >= selections.length) {
                break;
            }
            if (streams[i2] != null && (selections[i2] == null || !mayRetainStreamFlags[i2])) {
                int track = ((SampleStreamImpl) streams[i2]).track;
                Assertions.checkState(trackEnabledStates[track]);
                this.enabledTrackCount--;
                trackEnabledStates[track] = false;
                streams[i2] = null;
            }
            i2++;
        }
        boolean seekRequired = !this.seenFirstTrackSelection ? positionUs2 == 0 || this.isSingleSample : oldEnabledTrackCount != 0;
        for (int i3 = 0; i3 < selections.length; i3++) {
            if (streams[i3] == null && selections[i3] != null) {
                ExoTrackSelection selection = selections[i3];
                Assertions.checkState(selection.length() == 1);
                Assertions.checkState(selection.getIndexInTrackGroup(0) == 0);
                int track2 = tracks.indexOf(selection.getTrackGroup());
                Assertions.checkState(!trackEnabledStates[track2]);
                this.enabledTrackCount++;
                trackEnabledStates[track2] = true;
                streams[i3] = new SampleStreamImpl(track2);
                streamResetFlags[i3] = true;
                if (!seekRequired) {
                    SampleQueue sampleQueue = this.sampleQueues[track2];
                    seekRequired = (sampleQueue.getReadIndex() == 0 || sampleQueue.seekTo(positionUs2, true)) ? false : true;
                }
            }
        }
        int i4 = this.enabledTrackCount;
        if (i4 == 0) {
            this.pendingDeferredRetry = false;
            this.notifyDiscontinuity = false;
            if (this.loader.isLoading()) {
                SampleQueue[] sampleQueueArr = this.sampleQueues;
                int length = sampleQueueArr.length;
                while (i < length) {
                    SampleQueue sampleQueue2 = sampleQueueArr[i];
                    sampleQueue2.discardToEnd();
                    i++;
                }
                this.loader.cancelLoading();
            } else {
                this.loadingFinished = false;
                SampleQueue[] sampleQueueArr2 = this.sampleQueues;
                int length2 = sampleQueueArr2.length;
                while (i < length2) {
                    SampleQueue sampleQueue3 = sampleQueueArr2[i];
                    sampleQueue3.reset();
                    i++;
                }
            }
        } else if (seekRequired) {
            positionUs2 = seekToUs(positionUs2);
            for (int i5 = 0; i5 < streams.length; i5++) {
                if (streams[i5] != null) {
                    streamResetFlags[i5] = true;
                }
            }
        }
        this.seenFirstTrackSelection = true;
        return positionUs2;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void discardBuffer(long positionUs, boolean toKeyframe) {
        if (this.isSingleSample) {
            return;
        }
        assertPrepared();
        if (isPendingReset()) {
            return;
        }
        boolean[] trackEnabledStates = this.trackState.trackEnabledStates;
        int trackCount = this.sampleQueues.length;
        for (int i = 0; i < trackCount; i++) {
            this.sampleQueues[i].discardTo(positionUs, toKeyframe, trackEnabledStates[i]);
        }
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public void reevaluateBuffer(long positionUs) {
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public boolean continueLoading(LoadingInfo loadingInfo) {
        if (this.loadingFinished || this.loader.hasFatalError() || this.pendingDeferredRetry) {
            return false;
        }
        if (this.prepared && this.enabledTrackCount == 0) {
            return false;
        }
        boolean continuedLoading = this.loadCondition.open();
        if (!this.loader.isLoading()) {
            startLoading();
            return true;
        }
        return continuedLoading;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public boolean isLoading() {
        return this.loader.isLoading() && this.loadCondition.isOpen();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public long getNextLoadPositionUs() {
        return getBufferedPositionUs();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long readDiscontinuity() {
        if (this.notifyDiscontinuity) {
            if (this.loadingFinished || getExtractedSamplesCount() > this.extractedSamplesCountAtStartOfLoad) {
                this.notifyDiscontinuity = false;
                return this.lastSeekPositionUs;
            }
            return C.TIME_UNSET;
        }
        return C.TIME_UNSET;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public long getBufferedPositionUs() {
        assertPrepared();
        if (this.loadingFinished || this.enabledTrackCount == 0) {
            return Long.MIN_VALUE;
        }
        if (isPendingReset()) {
            return this.pendingResetPositionUs;
        }
        long largestQueuedTimestampUs = Long.MAX_VALUE;
        if (this.haveAudioVideoTracks) {
            int trackCount = this.sampleQueues.length;
            for (int i = 0; i < trackCount; i++) {
                if (this.trackState.trackIsAudioVideoFlags[i] && this.trackState.trackEnabledStates[i] && !this.sampleQueues[i].isLastSampleQueued()) {
                    largestQueuedTimestampUs = Math.min(largestQueuedTimestampUs, this.sampleQueues[i].getLargestQueuedTimestampUs());
                }
            }
        }
        if (largestQueuedTimestampUs == Long.MAX_VALUE) {
            largestQueuedTimestampUs = getLargestQueuedTimestampUs(false);
        }
        if (largestQueuedTimestampUs == Long.MIN_VALUE) {
            return this.lastSeekPositionUs;
        }
        return largestQueuedTimestampUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long seekToUs(long positionUs) {
        assertPrepared();
        boolean[] trackIsAudioVideoFlags = this.trackState.trackIsAudioVideoFlags;
        long positionUs2 = this.seekMap.isSeekable() ? positionUs : 0L;
        int i = 0;
        this.notifyDiscontinuity = false;
        this.lastSeekPositionUs = positionUs2;
        if (isPendingReset()) {
            this.pendingResetPositionUs = positionUs2;
            return positionUs2;
        }
        if (this.dataType != 7 && ((this.loadingFinished || this.loader.isLoading()) && seekInsideBufferUs(trackIsAudioVideoFlags, positionUs2))) {
            return positionUs2;
        }
        this.pendingDeferredRetry = false;
        this.pendingResetPositionUs = positionUs2;
        this.loadingFinished = false;
        if (this.loader.isLoading()) {
            SampleQueue[] sampleQueueArr = this.sampleQueues;
            int length = sampleQueueArr.length;
            while (i < length) {
                SampleQueue sampleQueue = sampleQueueArr[i];
                sampleQueue.discardToEnd();
                i++;
            }
            this.loader.cancelLoading();
        } else {
            this.loader.clearFatalError();
            SampleQueue[] sampleQueueArr2 = this.sampleQueues;
            int length2 = sampleQueueArr2.length;
            while (i < length2) {
                SampleQueue sampleQueue2 = sampleQueueArr2[i];
                sampleQueue2.reset();
                i++;
            }
        }
        return positionUs2;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
        assertPrepared();
        if (!this.seekMap.isSeekable()) {
            return 0L;
        }
        SeekMap.SeekPoints seekPoints = this.seekMap.getSeekPoints(positionUs);
        return seekParameters.resolveSeekPositionUs(positionUs, seekPoints.first.timeUs, seekPoints.second.timeUs);
    }

    boolean isReady(int track) {
        return !suppressRead() && this.sampleQueues[track].isReady(this.loadingFinished);
    }

    void maybeThrowError(int sampleQueueIndex) throws IOException {
        this.sampleQueues[sampleQueueIndex].maybeThrowError();
        maybeThrowError();
    }

    void maybeThrowError() throws IOException {
        this.loader.maybeThrowError(this.loadErrorHandlingPolicy.getMinimumLoadableRetryCount(this.dataType));
    }

    int readData(int sampleQueueIndex, FormatHolder formatHolder, DecoderInputBuffer buffer, int readFlags) {
        if (suppressRead()) {
            return -3;
        }
        maybeNotifyDownstreamFormat(sampleQueueIndex);
        int result = this.sampleQueues[sampleQueueIndex].read(formatHolder, buffer, readFlags, this.loadingFinished);
        if (result == -3) {
            maybeStartDeferredRetry(sampleQueueIndex);
        }
        return result;
    }

    int skipData(int track, long positionUs) throws Throwable {
        if (suppressRead()) {
            return 0;
        }
        maybeNotifyDownstreamFormat(track);
        SampleQueue sampleQueue = this.sampleQueues[track];
        int skipCount = sampleQueue.getSkipCount(positionUs, this.loadingFinished);
        sampleQueue.skip(skipCount);
        if (skipCount == 0) {
            maybeStartDeferredRetry(track);
        }
        return skipCount;
    }

    private void maybeNotifyDownstreamFormat(int track) {
        assertPrepared();
        boolean[] trackNotifiedDownstreamFormats = this.trackState.trackNotifiedDownstreamFormats;
        if (!trackNotifiedDownstreamFormats[track]) {
            Format trackFormat = this.trackState.tracks.get(track).getFormat(0);
            this.mediaSourceEventDispatcher.downstreamFormatChanged(MimeTypes.getTrackType(trackFormat.sampleMimeType), trackFormat, 0, null, this.lastSeekPositionUs);
            trackNotifiedDownstreamFormats[track] = true;
        }
    }

    private void maybeStartDeferredRetry(int track) {
        assertPrepared();
        boolean[] trackIsAudioVideoFlags = this.trackState.trackIsAudioVideoFlags;
        if (this.pendingDeferredRetry && trackIsAudioVideoFlags[track]) {
            if (this.sampleQueues[track].isReady(false)) {
                return;
            }
            this.pendingResetPositionUs = 0L;
            this.pendingDeferredRetry = false;
            this.notifyDiscontinuity = true;
            this.lastSeekPositionUs = 0L;
            this.extractedSamplesCountAtStartOfLoad = 0;
            for (SampleQueue sampleQueue : this.sampleQueues) {
                sampleQueue.reset();
            }
            ((MediaPeriod.Callback) Assertions.checkNotNull(this.callback)).onContinueLoadingRequested(this);
        }
    }

    private boolean suppressRead() {
        return this.notifyDiscontinuity || isPendingReset();
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Callback
    public void onLoadCompleted(ExtractingLoadable loadable, long elapsedRealtimeMs, long loadDurationMs) {
        long j;
        if (this.durationUs == C.TIME_UNSET && this.seekMap != null) {
            boolean isSeekable = this.seekMap.isSeekable();
            long largestQueuedTimestampUs = getLargestQueuedTimestampUs(true);
            if (largestQueuedTimestampUs == Long.MIN_VALUE) {
                j = 0;
            } else {
                j = 10000 + largestQueuedTimestampUs;
            }
            this.durationUs = j;
            this.listener.onSourceInfoRefreshed(this.durationUs, isSeekable, this.isLive);
        }
        StatsDataSource dataSource = loadable.dataSource;
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, dataSource.getLastOpenedUri(), dataSource.getLastResponseHeaders(), elapsedRealtimeMs, loadDurationMs, dataSource.getBytesRead());
        this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        this.mediaSourceEventDispatcher.loadCompleted(loadEventInfo, 1, -1, null, 0, null, loadable.seekTimeUs, this.durationUs);
        this.loadingFinished = true;
        ((MediaPeriod.Callback) Assertions.checkNotNull(this.callback)).onContinueLoadingRequested(this);
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Callback
    public void onLoadCanceled(ExtractingLoadable loadable, long elapsedRealtimeMs, long loadDurationMs, boolean released) {
        StatsDataSource dataSource = loadable.dataSource;
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, dataSource.getLastOpenedUri(), dataSource.getLastResponseHeaders(), elapsedRealtimeMs, loadDurationMs, dataSource.getBytesRead());
        this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        this.mediaSourceEventDispatcher.loadCanceled(loadEventInfo, 1, -1, null, 0, null, loadable.seekTimeUs, this.durationUs);
        if (!released) {
            for (SampleQueue sampleQueue : this.sampleQueues) {
                sampleQueue.reset();
            }
            if (this.enabledTrackCount > 0) {
                ((MediaPeriod.Callback) Assertions.checkNotNull(this.callback)).onContinueLoadingRequested(this);
            }
        }
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Callback
    public Loader.LoadErrorAction onLoadError(ExtractingLoadable loadable, long elapsedRealtimeMs, long loadDurationMs, IOException error, int errorCount) {
        Loader.LoadErrorAction loadErrorActionCreateRetryAction;
        Loader.LoadErrorAction loadErrorAction;
        StatsDataSource dataSource = loadable.dataSource;
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, dataSource.getLastOpenedUri(), dataSource.getLastResponseHeaders(), elapsedRealtimeMs, loadDurationMs, dataSource.getBytesRead());
        MediaLoadData mediaLoadData = new MediaLoadData(1, -1, null, 0, null, Util.usToMs(loadable.seekTimeUs), Util.usToMs(this.durationUs));
        long retryDelayMs = this.loadErrorHandlingPolicy.getRetryDelayMsFor(new LoadErrorHandlingPolicy.LoadErrorInfo(loadEventInfo, mediaLoadData, error, errorCount));
        if (retryDelayMs == C.TIME_UNSET) {
            loadErrorAction = Loader.DONT_RETRY_FATAL;
        } else {
            int extractedSamplesCount = getExtractedSamplesCount();
            boolean madeProgress = extractedSamplesCount > this.extractedSamplesCountAtStartOfLoad;
            if (configureRetry(loadable, extractedSamplesCount)) {
                loadErrorActionCreateRetryAction = Loader.createRetryAction(madeProgress, retryDelayMs);
            } else {
                loadErrorActionCreateRetryAction = Loader.DONT_RETRY;
            }
            loadErrorAction = loadErrorActionCreateRetryAction;
        }
        boolean wasCanceled = !loadErrorAction.isRetry();
        this.mediaSourceEventDispatcher.loadError(loadEventInfo, 1, -1, null, 0, null, loadable.seekTimeUs, this.durationUs, error, wasCanceled);
        if (wasCanceled) {
            this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        }
        return loadErrorAction;
    }

    @Override // androidx.media3.extractor.ExtractorOutput
    public TrackOutput track(int id, int type) {
        return prepareTrackOutput(new TrackId(id, false));
    }

    @Override // androidx.media3.extractor.ExtractorOutput
    public void endTracks() {
        this.sampleQueuesBuilt = true;
        this.handler.post(this.maybeFinishPrepareRunnable);
    }

    @Override // androidx.media3.extractor.ExtractorOutput
    public void seekMap(final SeekMap seekMap) {
        this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.source.ProgressiveMediaPeriod$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m121x33ac0ff2(seekMap);
            }
        });
    }

    TrackOutput icyTrack() {
        return prepareTrackOutput(new TrackId(0, true));
    }

    @Override // androidx.media3.exoplayer.source.SampleQueue.UpstreamFormatChangedListener
    public void onUpstreamFormatChanged(Format format) {
        this.handler.post(this.maybeFinishPrepareRunnable);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLengthKnown() {
        this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.source.ProgressiveMediaPeriod$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m120xa2bd730d();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$onLengthKnown$2$androidx-media3-exoplayer-source-ProgressiveMediaPeriod, reason: not valid java name */
    /* synthetic */ void m120xa2bd730d() {
        this.isLengthKnown = true;
    }

    private TrackOutput prepareTrackOutput(TrackId id) {
        int trackCount = this.sampleQueues.length;
        for (int i = 0; i < trackCount; i++) {
            if (id.equals(this.sampleQueueTrackIds[i])) {
                return this.sampleQueues[i];
            }
        }
        if (this.sampleQueuesBuilt) {
            Log.w(TAG, "Extractor added new track (id=" + id.id + ") after finishing tracks.");
            return new DiscardingTrackOutput();
        }
        SampleQueue trackOutput = SampleQueue.createWithDrm(this.allocator, this.drmSessionManager, this.drmEventDispatcher);
        trackOutput.setUpstreamFormatChangeListener(this);
        TrackId[] sampleQueueTrackIds = (TrackId[]) Arrays.copyOf(this.sampleQueueTrackIds, trackCount + 1);
        sampleQueueTrackIds[trackCount] = id;
        this.sampleQueueTrackIds = (TrackId[]) Util.castNonNullTypeArray(sampleQueueTrackIds);
        SampleQueue[] sampleQueues = (SampleQueue[]) Arrays.copyOf(this.sampleQueues, trackCount + 1);
        sampleQueues[trackCount] = trackOutput;
        this.sampleQueues = (SampleQueue[]) Util.castNonNullTypeArray(sampleQueues);
        return trackOutput;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: setSeekMap, reason: merged with bridge method [inline-methods] */
    public void m121x33ac0ff2(SeekMap seekMap) {
        this.seekMap = this.icyHeaders == null ? seekMap : new SeekMap.Unseekable(C.TIME_UNSET);
        this.durationUs = seekMap.getDurationUs();
        this.isLive = !this.isLengthKnown && seekMap.getDurationUs() == C.TIME_UNSET;
        this.dataType = this.isLive ? 7 : 1;
        if (this.prepared) {
            this.listener.onSourceInfoRefreshed(this.durationUs, seekMap.isSeekable(), this.isLive);
        } else {
            maybeFinishPrepare();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeFinishPrepare() {
        Metadata metadata;
        if (this.released || this.prepared || !this.sampleQueuesBuilt || this.seekMap == null) {
            return;
        }
        for (SampleQueue sampleQueue : this.sampleQueues) {
            if (sampleQueue.getUpstreamFormat() == null) {
                return;
            }
        }
        this.loadCondition.close();
        int trackCount = this.sampleQueues.length;
        TrackGroup[] trackArray = new TrackGroup[trackCount];
        boolean[] trackIsAudioVideoFlags = new boolean[trackCount];
        for (int i = 0; i < trackCount; i++) {
            Format trackFormat = (Format) Assertions.checkNotNull(this.sampleQueues[i].getUpstreamFormat());
            String mimeType = trackFormat.sampleMimeType;
            boolean isAudio = MimeTypes.isAudio(mimeType);
            boolean isAudioVideo = isAudio || MimeTypes.isVideo(mimeType);
            trackIsAudioVideoFlags[i] = isAudioVideo;
            this.haveAudioVideoTracks |= isAudioVideo;
            boolean isImage = MimeTypes.isImage(mimeType);
            this.isSingleSample = this.singleSampleDurationUs != C.TIME_UNSET && trackCount == 1 && isImage;
            IcyHeaders icyHeaders = this.icyHeaders;
            if (icyHeaders != null) {
                if (isAudio || this.sampleQueueTrackIds[i].isIcyTrack) {
                    Metadata metadata2 = trackFormat.metadata;
                    if (metadata2 == null) {
                        metadata = new Metadata(icyHeaders);
                    } else {
                        metadata = metadata2.copyWithAppendedEntries(icyHeaders);
                    }
                    trackFormat = trackFormat.buildUpon().setMetadata(metadata).build();
                }
                if (isAudio && trackFormat.averageBitrate == -1 && trackFormat.peakBitrate == -1 && icyHeaders.bitrate != -1) {
                    trackFormat = trackFormat.buildUpon().setAverageBitrate(icyHeaders.bitrate).build();
                }
            }
            trackArray[i] = new TrackGroup(Integer.toString(i), trackFormat.copyWithCryptoType(this.drmSessionManager.getCryptoType(trackFormat)));
        }
        this.trackState = new TrackState(new TrackGroupArray(trackArray), trackIsAudioVideoFlags);
        if (this.isSingleSample && this.durationUs == C.TIME_UNSET) {
            this.durationUs = this.singleSampleDurationUs;
            this.seekMap = new ForwardingSeekMap(this.seekMap) { // from class: androidx.media3.exoplayer.source.ProgressiveMediaPeriod.1
                @Override // androidx.media3.extractor.ForwardingSeekMap, androidx.media3.extractor.SeekMap
                public long getDurationUs() {
                    return ProgressiveMediaPeriod.this.durationUs;
                }
            };
        }
        this.listener.onSourceInfoRefreshed(this.durationUs, this.seekMap.isSeekable(), this.isLive);
        this.prepared = true;
        ((MediaPeriod.Callback) Assertions.checkNotNull(this.callback)).onPrepared(this);
    }

    private void startLoading() {
        ExtractingLoadable loadable = new ExtractingLoadable(this.uri, this.dataSource, this.progressiveMediaExtractor, this, this.loadCondition);
        if (this.prepared) {
            Assertions.checkState(isPendingReset());
            if (this.durationUs != C.TIME_UNSET && this.pendingResetPositionUs > this.durationUs) {
                this.loadingFinished = true;
                this.pendingResetPositionUs = C.TIME_UNSET;
                return;
            }
            loadable.setLoadPosition(((SeekMap) Assertions.checkNotNull(this.seekMap)).getSeekPoints(this.pendingResetPositionUs).first.position, this.pendingResetPositionUs);
            for (SampleQueue sampleQueue : this.sampleQueues) {
                sampleQueue.setStartTimeUs(this.pendingResetPositionUs);
            }
            this.pendingResetPositionUs = C.TIME_UNSET;
        }
        this.extractedSamplesCountAtStartOfLoad = getExtractedSamplesCount();
        long elapsedRealtimeMs = this.loader.startLoading(loadable, this, this.loadErrorHandlingPolicy.getMinimumLoadableRetryCount(this.dataType));
        DataSpec dataSpec = loadable.dataSpec;
        this.mediaSourceEventDispatcher.loadStarted(new LoadEventInfo(loadable.loadTaskId, dataSpec, elapsedRealtimeMs), 1, -1, null, 0, null, loadable.seekTimeUs, this.durationUs);
    }

    private boolean configureRetry(ExtractingLoadable loadable, int currentExtractedSampleCount) {
        if (this.isLengthKnown || (this.seekMap != null && this.seekMap.getDurationUs() != C.TIME_UNSET)) {
            this.extractedSamplesCountAtStartOfLoad = currentExtractedSampleCount;
            return true;
        }
        if (this.prepared && !suppressRead()) {
            this.pendingDeferredRetry = true;
            return false;
        }
        this.notifyDiscontinuity = this.prepared;
        this.lastSeekPositionUs = 0L;
        this.extractedSamplesCountAtStartOfLoad = 0;
        for (SampleQueue sampleQueue : this.sampleQueues) {
            sampleQueue.reset();
        }
        loadable.setLoadPosition(0L, 0L);
        return true;
    }

    private boolean seekInsideBufferUs(boolean[] trackIsAudioVideoFlags, long positionUs) {
        boolean seekInsideQueue;
        int trackCount = this.sampleQueues.length;
        for (int i = 0; i < trackCount; i++) {
            SampleQueue sampleQueue = this.sampleQueues[i];
            if (this.isSingleSample) {
                seekInsideQueue = sampleQueue.seekTo(sampleQueue.getFirstIndex());
            } else {
                seekInsideQueue = sampleQueue.seekTo(positionUs, false);
            }
            if (!seekInsideQueue && (trackIsAudioVideoFlags[i] || !this.haveAudioVideoTracks)) {
                return false;
            }
        }
        return true;
    }

    private int getExtractedSamplesCount() {
        int extractedSamplesCount = 0;
        for (SampleQueue sampleQueue : this.sampleQueues) {
            extractedSamplesCount += sampleQueue.getWriteIndex();
        }
        return extractedSamplesCount;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public long getLargestQueuedTimestampUs(boolean includeDisabledTracks) {
        long largestQueuedTimestampUs = Long.MIN_VALUE;
        for (int i = 0; i < this.sampleQueues.length; i++) {
            if (includeDisabledTracks || ((TrackState) Assertions.checkNotNull(this.trackState)).trackEnabledStates[i]) {
                largestQueuedTimestampUs = Math.max(largestQueuedTimestampUs, this.sampleQueues[i].getLargestQueuedTimestampUs());
            }
        }
        return largestQueuedTimestampUs;
    }

    private boolean isPendingReset() {
        return this.pendingResetPositionUs != C.TIME_UNSET;
    }

    @EnsuresNonNull({"trackState", "seekMap"})
    private void assertPrepared() {
        Assertions.checkState(this.prepared);
        Assertions.checkNotNull(this.trackState);
        Assertions.checkNotNull(this.seekMap);
    }

    private final class SampleStreamImpl implements SampleStream {
        private final int track;

        public SampleStreamImpl(int track) {
            this.track = track;
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public boolean isReady() {
            return ProgressiveMediaPeriod.this.isReady(this.track);
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public void maybeThrowError() throws IOException {
            ProgressiveMediaPeriod.this.maybeThrowError(this.track);
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, int readFlags) {
            return ProgressiveMediaPeriod.this.readData(this.track, formatHolder, buffer, readFlags);
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public int skipData(long positionUs) {
            return ProgressiveMediaPeriod.this.skipData(this.track, positionUs);
        }
    }

    final class ExtractingLoadable implements Loader.Loadable, IcyDataSource.Listener {
        private final StatsDataSource dataSource;
        private final ExtractorOutput extractorOutput;
        private TrackOutput icyTrackOutput;
        private volatile boolean loadCanceled;
        private final ConditionVariable loadCondition;
        private final ProgressiveMediaExtractor progressiveMediaExtractor;
        private long seekTimeUs;
        private boolean seenIcyMetadata;
        private final Uri uri;
        private final PositionHolder positionHolder = new PositionHolder();
        private boolean pendingExtractorSeek = true;
        private final long loadTaskId = LoadEventInfo.getNewId();
        private DataSpec dataSpec = buildDataSpec(0);

        public ExtractingLoadable(Uri uri, DataSource dataSource, ProgressiveMediaExtractor progressiveMediaExtractor, ExtractorOutput extractorOutput, ConditionVariable loadCondition) {
            this.uri = uri;
            this.dataSource = new StatsDataSource(dataSource);
            this.progressiveMediaExtractor = progressiveMediaExtractor;
            this.extractorOutput = extractorOutput;
            this.loadCondition = loadCondition;
        }

        @Override // androidx.media3.exoplayer.upstream.Loader.Loadable
        public void cancelLoad() {
            this.loadCanceled = true;
        }

        @Override // androidx.media3.exoplayer.upstream.Loader.Loadable
        public void load() throws IOException {
            long length;
            DataSource extractorDataSource;
            int result = 0;
            while (result == 0 && !this.loadCanceled) {
                try {
                    long position = this.positionHolder.position;
                    this.dataSpec = buildDataSpec(position);
                    long length2 = this.dataSource.open(this.dataSpec);
                    if (!this.loadCanceled) {
                        if (length2 == -1) {
                            length = length2;
                        } else {
                            long length3 = length2 + position;
                            ProgressiveMediaPeriod.this.onLengthKnown();
                            length = length3;
                        }
                        ProgressiveMediaPeriod.this.icyHeaders = IcyHeaders.parse(this.dataSource.getResponseHeaders());
                        DataSource extractorDataSource2 = this.dataSource;
                        if (ProgressiveMediaPeriod.this.icyHeaders != null && ProgressiveMediaPeriod.this.icyHeaders.metadataInterval != -1) {
                            DataSource extractorDataSource3 = new IcyDataSource(this.dataSource, ProgressiveMediaPeriod.this.icyHeaders.metadataInterval, this);
                            this.icyTrackOutput = ProgressiveMediaPeriod.this.icyTrack();
                            this.icyTrackOutput.format(ProgressiveMediaPeriod.ICY_FORMAT);
                            extractorDataSource = extractorDataSource3;
                        } else {
                            extractorDataSource = extractorDataSource2;
                        }
                        this.progressiveMediaExtractor.init(extractorDataSource, this.uri, this.dataSource.getResponseHeaders(), position, length, this.extractorOutput);
                        if (ProgressiveMediaPeriod.this.icyHeaders != null) {
                            this.progressiveMediaExtractor.disableSeekingOnMp3Streams();
                        }
                        if (this.pendingExtractorSeek) {
                            this.progressiveMediaExtractor.seek(position, this.seekTimeUs);
                            this.pendingExtractorSeek = false;
                        }
                        while (result == 0 && !this.loadCanceled) {
                            try {
                                this.loadCondition.block();
                                result = this.progressiveMediaExtractor.read(this.positionHolder);
                                long currentInputPosition = this.progressiveMediaExtractor.getCurrentInputPosition();
                                if (currentInputPosition > ProgressiveMediaPeriod.this.continueLoadingCheckIntervalBytes + position) {
                                    this.loadCondition.close();
                                    ProgressiveMediaPeriod.this.handler.post(ProgressiveMediaPeriod.this.onContinueLoadingRequestedRunnable);
                                    position = currentInputPosition;
                                }
                            } catch (InterruptedException e) {
                                throw new InterruptedIOException();
                            }
                        }
                        if (result == 1) {
                            result = 0;
                        } else if (this.progressiveMediaExtractor.getCurrentInputPosition() != -1) {
                            this.positionHolder.position = this.progressiveMediaExtractor.getCurrentInputPosition();
                        }
                        DataSourceUtil.closeQuietly(this.dataSource);
                    } else {
                        if (result != 1 && this.progressiveMediaExtractor.getCurrentInputPosition() != -1) {
                            this.positionHolder.position = this.progressiveMediaExtractor.getCurrentInputPosition();
                        }
                        DataSourceUtil.closeQuietly(this.dataSource);
                        return;
                    }
                } catch (Throwable th) {
                    if (result != 1 && this.progressiveMediaExtractor.getCurrentInputPosition() != -1) {
                        this.positionHolder.position = this.progressiveMediaExtractor.getCurrentInputPosition();
                    }
                    DataSourceUtil.closeQuietly(this.dataSource);
                    throw th;
                }
            }
        }

        @Override // androidx.media3.exoplayer.source.IcyDataSource.Listener
        public void onIcyMetadata(ParsableByteArray metadata) {
            long jMax;
            if (this.seenIcyMetadata) {
                jMax = Math.max(ProgressiveMediaPeriod.this.getLargestQueuedTimestampUs(true), this.seekTimeUs);
            } else {
                jMax = this.seekTimeUs;
            }
            long timeUs = jMax;
            int length = metadata.bytesLeft();
            TrackOutput icyTrackOutput = (TrackOutput) Assertions.checkNotNull(this.icyTrackOutput);
            icyTrackOutput.sampleData(metadata, length);
            icyTrackOutput.sampleMetadata(timeUs, 1, length, 0, null);
            this.seenIcyMetadata = true;
        }

        private DataSpec buildDataSpec(long position) {
            return new DataSpec.Builder().setUri(this.uri).setPosition(position).setKey(ProgressiveMediaPeriod.this.customCacheKey).setFlags(6).setHttpRequestHeaders(ProgressiveMediaPeriod.ICY_METADATA_HEADERS).build();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setLoadPosition(long position, long timeUs) {
            this.positionHolder.position = position;
            this.seekTimeUs = timeUs;
            this.pendingExtractorSeek = true;
            this.seenIcyMetadata = false;
        }
    }

    private static final class TrackState {
        public final boolean[] trackEnabledStates;
        public final boolean[] trackIsAudioVideoFlags;
        public final boolean[] trackNotifiedDownstreamFormats;
        public final TrackGroupArray tracks;

        public TrackState(TrackGroupArray tracks, boolean[] trackIsAudioVideoFlags) {
            this.tracks = tracks;
            this.trackIsAudioVideoFlags = trackIsAudioVideoFlags;
            this.trackEnabledStates = new boolean[tracks.length];
            this.trackNotifiedDownstreamFormats = new boolean[tracks.length];
        }
    }

    private static final class TrackId {
        public final int id;
        public final boolean isIcyTrack;

        public TrackId(int id, boolean isIcyTrack) {
            this.id = id;
            this.isIcyTrack = isIcyTrack;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            TrackId other = (TrackId) obj;
            if (this.id == other.id && this.isIcyTrack == other.isIcyTrack) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (this.id * 31) + (this.isIcyTrack ? 1 : 0);
        }
    }

    private static Map<String, String> createIcyMetadataHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(IcyHeaders.REQUEST_HEADER_ENABLE_METADATA_NAME, IcyHeaders.REQUEST_HEADER_ENABLE_METADATA_VALUE);
        return Collections.unmodifiableMap(headers);
    }
}
