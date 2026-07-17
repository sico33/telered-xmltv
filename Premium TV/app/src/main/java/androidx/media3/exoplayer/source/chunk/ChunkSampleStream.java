package androidx.media3.exoplayer.source.chunk;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.exoplayer.FormatHolder;
import androidx.media3.exoplayer.LoadingInfo;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.drm.DrmSessionEventListener;
import androidx.media3.exoplayer.drm.DrmSessionManager;
import androidx.media3.exoplayer.source.LoadEventInfo;
import androidx.media3.exoplayer.source.MediaLoadData;
import androidx.media3.exoplayer.source.MediaSourceEventListener;
import androidx.media3.exoplayer.source.SampleQueue;
import androidx.media3.exoplayer.source.SampleStream;
import androidx.media3.exoplayer.source.SequenceableLoader;
import androidx.media3.exoplayer.source.chunk.ChunkSource;
import androidx.media3.exoplayer.upstream.Allocator;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import androidx.media3.exoplayer.upstream.Loader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class ChunkSampleStream<T extends ChunkSource> implements SampleStream, SequenceableLoader, Loader.Callback<Chunk>, Loader.ReleaseCallback {
    private static final String TAG = "ChunkSampleStream";
    private final SequenceableLoader.Callback<ChunkSampleStream<T>> callback;
    private BaseMediaChunk canceledMediaChunk;
    private final BaseMediaChunkOutput chunkOutput;
    private final T chunkSource;
    private final SampleQueue[] embeddedSampleQueues;
    private final Format[] embeddedTrackFormats;
    private final int[] embeddedTrackTypes;
    private final boolean[] embeddedTracksSelected;
    private long lastSeekPositionUs;
    private final LoadErrorHandlingPolicy loadErrorHandlingPolicy;
    private final Loader loader;
    private Chunk loadingChunk;
    boolean loadingFinished;
    private final ArrayList<BaseMediaChunk> mediaChunks;
    private final MediaSourceEventListener.EventDispatcher mediaSourceEventDispatcher;
    private final ChunkHolder nextChunkHolder;
    private int nextNotifyPrimaryFormatMediaChunkIndex;
    private long pendingResetPositionUs;
    private Format primaryDownstreamTrackFormat;
    private final SampleQueue primarySampleQueue;
    public final int primaryTrackType;
    private final List<BaseMediaChunk> readOnlyMediaChunks;
    private ReleaseCallback<T> releaseCallback;

    public interface ReleaseCallback<T extends ChunkSource> {
        void onSampleStreamReleased(ChunkSampleStream<T> chunkSampleStream);
    }

    public ChunkSampleStream(int primaryTrackType, int[] embeddedTrackTypes, Format[] embeddedTrackFormats, T chunkSource, SequenceableLoader.Callback<ChunkSampleStream<T>> callback, Allocator allocator, long positionUs, DrmSessionManager drmSessionManager, DrmSessionEventListener.EventDispatcher drmEventDispatcher, LoadErrorHandlingPolicy loadErrorHandlingPolicy, MediaSourceEventListener.EventDispatcher mediaSourceEventDispatcher) {
        this.primaryTrackType = primaryTrackType;
        this.embeddedTrackTypes = embeddedTrackTypes == null ? new int[0] : embeddedTrackTypes;
        this.embeddedTrackFormats = embeddedTrackFormats == null ? new Format[0] : embeddedTrackFormats;
        this.chunkSource = chunkSource;
        this.callback = callback;
        this.mediaSourceEventDispatcher = mediaSourceEventDispatcher;
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy;
        this.loader = new Loader(TAG);
        this.nextChunkHolder = new ChunkHolder();
        this.mediaChunks = new ArrayList<>();
        this.readOnlyMediaChunks = Collections.unmodifiableList(this.mediaChunks);
        int embeddedTrackCount = this.embeddedTrackTypes.length;
        this.embeddedSampleQueues = new SampleQueue[embeddedTrackCount];
        this.embeddedTracksSelected = new boolean[embeddedTrackCount];
        int[] trackTypes = new int[embeddedTrackCount + 1];
        SampleQueue[] sampleQueues = new SampleQueue[embeddedTrackCount + 1];
        this.primarySampleQueue = SampleQueue.createWithDrm(allocator, drmSessionManager, drmEventDispatcher);
        trackTypes[0] = primaryTrackType;
        sampleQueues[0] = this.primarySampleQueue;
        for (int i = 0; i < embeddedTrackCount; i++) {
            SampleQueue sampleQueue = SampleQueue.createWithoutDrm(allocator);
            this.embeddedSampleQueues[i] = sampleQueue;
            sampleQueues[i + 1] = sampleQueue;
            trackTypes[i + 1] = this.embeddedTrackTypes[i];
        }
        this.chunkOutput = new BaseMediaChunkOutput(trackTypes, sampleQueues);
        this.pendingResetPositionUs = positionUs;
        this.lastSeekPositionUs = positionUs;
    }

    public void discardBuffer(long positionUs, boolean toKeyframe) {
        if (isPendingReset()) {
            return;
        }
        int oldFirstSampleIndex = this.primarySampleQueue.getFirstIndex();
        this.primarySampleQueue.discardTo(positionUs, toKeyframe, true);
        int newFirstSampleIndex = this.primarySampleQueue.getFirstIndex();
        if (newFirstSampleIndex > oldFirstSampleIndex) {
            long discardToUs = this.primarySampleQueue.getFirstTimestampUs();
            for (int i = 0; i < this.embeddedSampleQueues.length; i++) {
                this.embeddedSampleQueues[i].discardTo(discardToUs, toKeyframe, this.embeddedTracksSelected[i]);
            }
        }
        discardDownstreamMediaChunks(newFirstSampleIndex);
    }

    public ChunkSampleStream<T>.EmbeddedSampleStream selectEmbeddedTrack(long positionUs, int trackType) {
        for (int i = 0; i < this.embeddedSampleQueues.length; i++) {
            if (this.embeddedTrackTypes[i] == trackType) {
                Assertions.checkState(!this.embeddedTracksSelected[i]);
                this.embeddedTracksSelected[i] = true;
                this.embeddedSampleQueues[i].seekTo(positionUs, true);
                return new EmbeddedSampleStream(this, this.embeddedSampleQueues[i], i);
            }
        }
        throw new IllegalStateException();
    }

    public T getChunkSource() {
        return this.chunkSource;
    }

    @Override // androidx.media3.exoplayer.source.SequenceableLoader
    public long getBufferedPositionUs() {
        BaseMediaChunk lastCompletedMediaChunk;
        if (this.loadingFinished) {
            return Long.MIN_VALUE;
        }
        if (isPendingReset()) {
            return this.pendingResetPositionUs;
        }
        long bufferedPositionUs = this.lastSeekPositionUs;
        BaseMediaChunk lastMediaChunk = getLastMediaChunk();
        if (lastMediaChunk.isLoadCompleted()) {
            lastCompletedMediaChunk = lastMediaChunk;
        } else {
            lastCompletedMediaChunk = this.mediaChunks.size() > 1 ? this.mediaChunks.get(this.mediaChunks.size() - 2) : null;
        }
        if (lastCompletedMediaChunk != null) {
            bufferedPositionUs = Math.max(bufferedPositionUs, lastCompletedMediaChunk.endTimeUs);
        }
        return Math.max(bufferedPositionUs, this.primarySampleQueue.getLargestQueuedTimestampUs());
    }

    public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
        return this.chunkSource.getAdjustedSeekPositionUs(positionUs, seekParameters);
    }

    public void seekToUs(long positionUs) {
        boolean seekInsideBuffer;
        this.lastSeekPositionUs = positionUs;
        if (isPendingReset()) {
            this.pendingResetPositionUs = positionUs;
            return;
        }
        BaseMediaChunk seekToMediaChunk = null;
        for (int i = 0; i < this.mediaChunks.size(); i++) {
            BaseMediaChunk mediaChunk = this.mediaChunks.get(i);
            long mediaChunkStartTimeUs = mediaChunk.startTimeUs;
            if (mediaChunkStartTimeUs == positionUs && mediaChunk.clippedStartTimeUs == C.TIME_UNSET) {
                seekToMediaChunk = mediaChunk;
                break;
            } else {
                if (mediaChunkStartTimeUs > positionUs) {
                    break;
                }
            }
        }
        SampleQueue sampleQueue = this.primarySampleQueue;
        int i2 = 0;
        if (seekToMediaChunk != null) {
            seekInsideBuffer = sampleQueue.seekTo(seekToMediaChunk.getFirstSampleIndex(0));
        } else {
            seekInsideBuffer = sampleQueue.seekTo(positionUs, positionUs < getNextLoadPositionUs());
        }
        if (seekInsideBuffer) {
            this.nextNotifyPrimaryFormatMediaChunkIndex = primarySampleIndexToMediaChunkIndex(this.primarySampleQueue.getReadIndex(), 0);
            SampleQueue[] sampleQueueArr = this.embeddedSampleQueues;
            int length = sampleQueueArr.length;
            while (i2 < length) {
                SampleQueue embeddedSampleQueue = sampleQueueArr[i2];
                embeddedSampleQueue.seekTo(positionUs, true);
                i2++;
            }
            return;
        }
        this.pendingResetPositionUs = positionUs;
        this.loadingFinished = false;
        this.mediaChunks.clear();
        this.nextNotifyPrimaryFormatMediaChunkIndex = 0;
        if (this.loader.isLoading()) {
            this.primarySampleQueue.discardToEnd();
            SampleQueue[] sampleQueueArr2 = this.embeddedSampleQueues;
            int length2 = sampleQueueArr2.length;
            while (i2 < length2) {
                SampleQueue embeddedSampleQueue2 = sampleQueueArr2[i2];
                embeddedSampleQueue2.discardToEnd();
                i2++;
            }
            this.loader.cancelLoading();
            return;
        }
        this.loader.clearFatalError();
        resetSampleQueues();
    }

    public void release() {
        release(null);
    }

    public void release(ReleaseCallback<T> callback) {
        this.releaseCallback = callback;
        this.primarySampleQueue.preRelease();
        for (SampleQueue embeddedSampleQueue : this.embeddedSampleQueues) {
            embeddedSampleQueue.preRelease();
        }
        this.loader.release(this);
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.ReleaseCallback
    public void onLoaderReleased() {
        this.primarySampleQueue.release();
        for (SampleQueue embeddedSampleQueue : this.embeddedSampleQueues) {
            embeddedSampleQueue.release();
        }
        this.chunkSource.release();
        if (this.releaseCallback != null) {
            this.releaseCallback.onSampleStreamReleased(this);
        }
    }

    @Override // androidx.media3.exoplayer.source.SampleStream
    public boolean isReady() {
        return !isPendingReset() && this.primarySampleQueue.isReady(this.loadingFinished);
    }

    @Override // androidx.media3.exoplayer.source.SampleStream
    public void maybeThrowError() throws IOException {
        this.loader.maybeThrowError();
        this.primarySampleQueue.maybeThrowError();
        if (!this.loader.isLoading()) {
            this.chunkSource.maybeThrowError();
        }
    }

    @Override // androidx.media3.exoplayer.source.SampleStream
    public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, int readFlags) {
        if (isPendingReset()) {
            return -3;
        }
        if (this.canceledMediaChunk != null && this.canceledMediaChunk.getFirstSampleIndex(0) <= this.primarySampleQueue.getReadIndex()) {
            return -3;
        }
        maybeNotifyPrimaryTrackFormatChanged();
        return this.primarySampleQueue.read(formatHolder, buffer, readFlags, this.loadingFinished);
    }

    @Override // androidx.media3.exoplayer.source.SampleStream
    public int skipData(long positionUs) throws Throwable {
        if (isPendingReset()) {
            return 0;
        }
        int skipCount = this.primarySampleQueue.getSkipCount(positionUs, this.loadingFinished);
        if (this.canceledMediaChunk != null) {
            int maxSkipCount = this.canceledMediaChunk.getFirstSampleIndex(0) - this.primarySampleQueue.getReadIndex();
            skipCount = Math.min(skipCount, maxSkipCount);
        }
        this.primarySampleQueue.skip(skipCount);
        maybeNotifyPrimaryTrackFormatChanged();
        return skipCount;
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Callback
    public void onLoadCompleted(Chunk loadable, long elapsedRealtimeMs, long loadDurationMs) {
        this.loadingChunk = null;
        this.chunkSource.onChunkLoadCompleted(loadable);
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
        this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        this.mediaSourceEventDispatcher.loadCompleted(loadEventInfo, loadable.type, this.primaryTrackType, loadable.trackFormat, loadable.trackSelectionReason, loadable.trackSelectionData, loadable.startTimeUs, loadable.endTimeUs);
        this.callback.onContinueLoadingRequested(this);
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Callback
    public void onLoadCanceled(Chunk loadable, long elapsedRealtimeMs, long loadDurationMs, boolean released) {
        this.loadingChunk = null;
        this.canceledMediaChunk = null;
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
        this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        this.mediaSourceEventDispatcher.loadCanceled(loadEventInfo, loadable.type, this.primaryTrackType, loadable.trackFormat, loadable.trackSelectionReason, loadable.trackSelectionData, loadable.startTimeUs, loadable.endTimeUs);
        if (!released) {
            if (isPendingReset()) {
                resetSampleQueues();
            } else if (isMediaChunk(loadable)) {
                discardUpstreamMediaChunksFromIndex(this.mediaChunks.size() - 1);
                if (this.mediaChunks.isEmpty()) {
                    this.pendingResetPositionUs = this.lastSeekPositionUs;
                }
            }
            this.callback.onContinueLoadingRequested(this);
        }
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Callback
    public Loader.LoadErrorAction onLoadError(Chunk loadable, long elapsedRealtimeMs, long loadDurationMs, IOException error, int errorCount) {
        Loader.LoadErrorAction loadErrorActionCreateRetryAction;
        long bytesLoaded = loadable.bytesLoaded();
        boolean isMediaChunk = isMediaChunk(loadable);
        int lastChunkIndex = this.mediaChunks.size() - 1;
        boolean cancelable = (bytesLoaded != 0 && isMediaChunk && haveReadFromMediaChunk(lastChunkIndex)) ? false : true;
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, bytesLoaded);
        MediaLoadData mediaLoadData = new MediaLoadData(loadable.type, this.primaryTrackType, loadable.trackFormat, loadable.trackSelectionReason, loadable.trackSelectionData, Util.usToMs(loadable.startTimeUs), Util.usToMs(loadable.endTimeUs));
        LoadErrorHandlingPolicy.LoadErrorInfo loadErrorInfo = new LoadErrorHandlingPolicy.LoadErrorInfo(loadEventInfo, mediaLoadData, error, errorCount);
        Loader.LoadErrorAction loadErrorAction = null;
        if (this.chunkSource.onChunkLoadError(loadable, cancelable, loadErrorInfo, this.loadErrorHandlingPolicy)) {
            if (cancelable) {
                loadErrorAction = Loader.DONT_RETRY;
                if (isMediaChunk) {
                    BaseMediaChunk removed = discardUpstreamMediaChunksFromIndex(lastChunkIndex);
                    Assertions.checkState(removed == loadable);
                    if (this.mediaChunks.isEmpty()) {
                        this.pendingResetPositionUs = this.lastSeekPositionUs;
                    }
                }
            } else {
                Log.w(TAG, "Ignoring attempt to cancel non-cancelable load.");
            }
        }
        if (loadErrorAction == null) {
            long retryDelayMs = this.loadErrorHandlingPolicy.getRetryDelayMsFor(loadErrorInfo);
            if (retryDelayMs != C.TIME_UNSET) {
                loadErrorActionCreateRetryAction = Loader.createRetryAction(false, retryDelayMs);
            } else {
                loadErrorActionCreateRetryAction = Loader.DONT_RETRY_FATAL;
            }
            loadErrorAction = loadErrorActionCreateRetryAction;
        }
        boolean canceled = !loadErrorAction.isRetry();
        this.mediaSourceEventDispatcher.loadError(loadEventInfo, loadable.type, this.primaryTrackType, loadable.trackFormat, loadable.trackSelectionReason, loadable.trackSelectionData, loadable.startTimeUs, loadable.endTimeUs, error, canceled);
        if (canceled) {
            this.loadingChunk = null;
            this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
            this.callback.onContinueLoadingRequested(this);
        }
        return loadErrorAction;
    }

    @Override // androidx.media3.exoplayer.source.SequenceableLoader
    public boolean continueLoading(LoadingInfo loadingInfo) {
        List<BaseMediaChunk> chunkQueue;
        long loadPositionUs;
        boolean z;
        int i = 0;
        if (this.loadingFinished || this.loader.isLoading() || this.loader.hasFatalError()) {
            return false;
        }
        boolean pendingReset = isPendingReset();
        if (pendingReset) {
            List<BaseMediaChunk> chunkQueue2 = Collections.emptyList();
            chunkQueue = chunkQueue2;
            loadPositionUs = this.pendingResetPositionUs;
        } else {
            List<BaseMediaChunk> chunkQueue3 = this.readOnlyMediaChunks;
            chunkQueue = chunkQueue3;
            loadPositionUs = getLastMediaChunk().endTimeUs;
        }
        this.chunkSource.getNextChunk(loadingInfo, loadPositionUs, chunkQueue, this.nextChunkHolder);
        boolean endOfStream = this.nextChunkHolder.endOfStream;
        Chunk loadable = this.nextChunkHolder.chunk;
        this.nextChunkHolder.clear();
        boolean z2 = true;
        if (endOfStream) {
            this.pendingResetPositionUs = C.TIME_UNSET;
            this.loadingFinished = true;
            return true;
        }
        if (loadable == null) {
            return false;
        }
        this.loadingChunk = loadable;
        if (isMediaChunk(loadable)) {
            BaseMediaChunk mediaChunk = (BaseMediaChunk) loadable;
            if (pendingReset) {
                if (mediaChunk.startTimeUs != this.pendingResetPositionUs) {
                    this.primarySampleQueue.setStartTimeUs(this.pendingResetPositionUs);
                    SampleQueue[] sampleQueueArr = this.embeddedSampleQueues;
                    int length = sampleQueueArr.length;
                    while (i < length) {
                        SampleQueue embeddedSampleQueue = sampleQueueArr[i];
                        embeddedSampleQueue.setStartTimeUs(this.pendingResetPositionUs);
                        i++;
                        z2 = z2;
                        loadPositionUs = loadPositionUs;
                    }
                    z = z2;
                } else {
                    z = true;
                }
                this.pendingResetPositionUs = C.TIME_UNSET;
            } else {
                z = true;
            }
            mediaChunk.init(this.chunkOutput);
            this.mediaChunks.add(mediaChunk);
        } else {
            z = true;
            if (loadable instanceof InitializationChunk) {
                ((InitializationChunk) loadable).init(this.chunkOutput);
            }
        }
        long elapsedRealtimeMs = this.loader.startLoading(loadable, this, this.loadErrorHandlingPolicy.getMinimumLoadableRetryCount(loadable.type));
        boolean z3 = z;
        this.mediaSourceEventDispatcher.loadStarted(new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, elapsedRealtimeMs), loadable.type, this.primaryTrackType, loadable.trackFormat, loadable.trackSelectionReason, loadable.trackSelectionData, loadable.startTimeUs, loadable.endTimeUs);
        return z3;
    }

    @Override // androidx.media3.exoplayer.source.SequenceableLoader
    public boolean isLoading() {
        return this.loader.isLoading();
    }

    @Override // androidx.media3.exoplayer.source.SequenceableLoader
    public long getNextLoadPositionUs() {
        if (isPendingReset()) {
            return this.pendingResetPositionUs;
        }
        if (this.loadingFinished) {
            return Long.MIN_VALUE;
        }
        return getLastMediaChunk().endTimeUs;
    }

    @Override // androidx.media3.exoplayer.source.SequenceableLoader
    public void reevaluateBuffer(long positionUs) {
        if (this.loader.hasFatalError() || isPendingReset()) {
            return;
        }
        if (this.loader.isLoading()) {
            Chunk loadingChunk = (Chunk) Assertions.checkNotNull(this.loadingChunk);
            if ((!isMediaChunk(loadingChunk) || !haveReadFromMediaChunk(this.mediaChunks.size() - 1)) && this.chunkSource.shouldCancelLoad(positionUs, loadingChunk, this.readOnlyMediaChunks)) {
                this.loader.cancelLoading();
                if (isMediaChunk(loadingChunk)) {
                    this.canceledMediaChunk = (BaseMediaChunk) loadingChunk;
                    return;
                }
                return;
            }
            return;
        }
        int preferredQueueSize = this.chunkSource.getPreferredQueueSize(positionUs, this.readOnlyMediaChunks);
        if (preferredQueueSize < this.mediaChunks.size()) {
            discardUpstream(preferredQueueSize);
        }
    }

    private void discardUpstream(int preferredQueueSize) {
        Assertions.checkState(!this.loader.isLoading());
        int currentQueueSize = this.mediaChunks.size();
        int newQueueSize = -1;
        for (int i = preferredQueueSize; i < currentQueueSize; i++) {
            if (!haveReadFromMediaChunk(i)) {
                newQueueSize = i;
                break;
            }
        }
        if (newQueueSize == -1) {
            return;
        }
        long endTimeUs = getLastMediaChunk().endTimeUs;
        BaseMediaChunk firstRemovedChunk = discardUpstreamMediaChunksFromIndex(newQueueSize);
        if (this.mediaChunks.isEmpty()) {
            this.pendingResetPositionUs = this.lastSeekPositionUs;
        }
        this.loadingFinished = false;
        this.mediaSourceEventDispatcher.upstreamDiscarded(this.primaryTrackType, firstRemovedChunk.startTimeUs, endTimeUs);
    }

    private boolean isMediaChunk(Chunk chunk) {
        return chunk instanceof BaseMediaChunk;
    }

    private void resetSampleQueues() {
        this.primarySampleQueue.reset();
        for (SampleQueue embeddedSampleQueue : this.embeddedSampleQueues) {
            embeddedSampleQueue.reset();
        }
    }

    private boolean haveReadFromMediaChunk(int mediaChunkIndex) {
        BaseMediaChunk mediaChunk = this.mediaChunks.get(mediaChunkIndex);
        if (this.primarySampleQueue.getReadIndex() > mediaChunk.getFirstSampleIndex(0)) {
            return true;
        }
        for (int i = 0; i < this.embeddedSampleQueues.length; i++) {
            if (this.embeddedSampleQueues[i].getReadIndex() > mediaChunk.getFirstSampleIndex(i + 1)) {
                return true;
            }
        }
        return false;
    }

    boolean isPendingReset() {
        return this.pendingResetPositionUs != C.TIME_UNSET;
    }

    private void discardDownstreamMediaChunks(int discardToSampleIndex) {
        int discardToMediaChunkIndex = Math.min(primarySampleIndexToMediaChunkIndex(discardToSampleIndex, 0), this.nextNotifyPrimaryFormatMediaChunkIndex);
        if (discardToMediaChunkIndex > 0) {
            Util.removeRange(this.mediaChunks, 0, discardToMediaChunkIndex);
            this.nextNotifyPrimaryFormatMediaChunkIndex -= discardToMediaChunkIndex;
        }
    }

    private void maybeNotifyPrimaryTrackFormatChanged() {
        int readSampleIndex = this.primarySampleQueue.getReadIndex();
        int notifyToMediaChunkIndex = primarySampleIndexToMediaChunkIndex(readSampleIndex, this.nextNotifyPrimaryFormatMediaChunkIndex - 1);
        while (this.nextNotifyPrimaryFormatMediaChunkIndex <= notifyToMediaChunkIndex) {
            int i = this.nextNotifyPrimaryFormatMediaChunkIndex;
            this.nextNotifyPrimaryFormatMediaChunkIndex = i + 1;
            maybeNotifyPrimaryTrackFormatChanged(i);
        }
    }

    private void maybeNotifyPrimaryTrackFormatChanged(int mediaChunkReadIndex) {
        BaseMediaChunk currentChunk = this.mediaChunks.get(mediaChunkReadIndex);
        Format trackFormat = currentChunk.trackFormat;
        if (!trackFormat.equals(this.primaryDownstreamTrackFormat)) {
            this.mediaSourceEventDispatcher.downstreamFormatChanged(this.primaryTrackType, trackFormat, currentChunk.trackSelectionReason, currentChunk.trackSelectionData, currentChunk.startTimeUs);
        }
        this.primaryDownstreamTrackFormat = trackFormat;
    }

    private int primarySampleIndexToMediaChunkIndex(int primarySampleIndex, int minChunkIndex) {
        int i = minChunkIndex + 1;
        while (true) {
            int size = this.mediaChunks.size();
            ArrayList<BaseMediaChunk> arrayList = this.mediaChunks;
            if (i < size) {
                if (arrayList.get(i).getFirstSampleIndex(0) <= primarySampleIndex) {
                    i++;
                } else {
                    return i - 1;
                }
            } else {
                int i2 = arrayList.size();
                return i2 - 1;
            }
        }
    }

    private BaseMediaChunk getLastMediaChunk() {
        return this.mediaChunks.get(this.mediaChunks.size() - 1);
    }

    private BaseMediaChunk discardUpstreamMediaChunksFromIndex(int chunkIndex) {
        BaseMediaChunk firstRemovedChunk = this.mediaChunks.get(chunkIndex);
        Util.removeRange(this.mediaChunks, chunkIndex, this.mediaChunks.size());
        this.nextNotifyPrimaryFormatMediaChunkIndex = Math.max(this.nextNotifyPrimaryFormatMediaChunkIndex, this.mediaChunks.size());
        this.primarySampleQueue.discardUpstreamSamples(firstRemovedChunk.getFirstSampleIndex(0));
        for (int i = 0; i < this.embeddedSampleQueues.length; i++) {
            this.embeddedSampleQueues[i].discardUpstreamSamples(firstRemovedChunk.getFirstSampleIndex(i + 1));
        }
        return firstRemovedChunk;
    }

    public final class EmbeddedSampleStream implements SampleStream {
        private final int index;
        private boolean notifiedDownstreamFormat;
        public final ChunkSampleStream<T> parent;
        private final SampleQueue sampleQueue;

        public EmbeddedSampleStream(ChunkSampleStream<T> parent, SampleQueue sampleQueue, int index) {
            this.parent = parent;
            this.sampleQueue = sampleQueue;
            this.index = index;
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public boolean isReady() {
            return !ChunkSampleStream.this.isPendingReset() && this.sampleQueue.isReady(ChunkSampleStream.this.loadingFinished);
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public int skipData(long positionUs) throws Throwable {
            if (ChunkSampleStream.this.isPendingReset()) {
                return 0;
            }
            int skipCount = this.sampleQueue.getSkipCount(positionUs, ChunkSampleStream.this.loadingFinished);
            if (ChunkSampleStream.this.canceledMediaChunk != null) {
                int maxSkipCount = ChunkSampleStream.this.canceledMediaChunk.getFirstSampleIndex(this.index + 1) - this.sampleQueue.getReadIndex();
                skipCount = Math.min(skipCount, maxSkipCount);
            }
            this.sampleQueue.skip(skipCount);
            if (skipCount > 0) {
                maybeNotifyDownstreamFormat();
            }
            return skipCount;
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public void maybeThrowError() {
        }

        @Override // androidx.media3.exoplayer.source.SampleStream
        public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, int readFlags) {
            if (ChunkSampleStream.this.isPendingReset()) {
                return -3;
            }
            if (ChunkSampleStream.this.canceledMediaChunk != null && ChunkSampleStream.this.canceledMediaChunk.getFirstSampleIndex(this.index + 1) <= this.sampleQueue.getReadIndex()) {
                return -3;
            }
            maybeNotifyDownstreamFormat();
            return this.sampleQueue.read(formatHolder, buffer, readFlags, ChunkSampleStream.this.loadingFinished);
        }

        public void release() {
            Assertions.checkState(ChunkSampleStream.this.embeddedTracksSelected[this.index]);
            ChunkSampleStream.this.embeddedTracksSelected[this.index] = false;
        }

        private void maybeNotifyDownstreamFormat() {
            if (!this.notifiedDownstreamFormat) {
                ChunkSampleStream.this.mediaSourceEventDispatcher.downstreamFormatChanged(ChunkSampleStream.this.embeddedTrackTypes[this.index], ChunkSampleStream.this.embeddedTrackFormats[this.index], 0, null, ChunkSampleStream.this.lastSeekPositionUs);
                this.notifiedDownstreamFormat = true;
            }
        }
    }
}
