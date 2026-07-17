package androidx.media3.exoplayer.hls;

import android.net.Uri;
import android.os.Handler;
import android.util.SparseIntArray;
import androidx.media3.common.C;
import androidx.media3.common.DataReader;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.Format;
import androidx.media3.common.Metadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.ParserException;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.HttpDataSource;
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
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.source.chunk.Chunk;
import androidx.media3.exoplayer.source.chunk.MediaChunkIterator;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.trackselection.TrackSelectionUtil;
import androidx.media3.exoplayer.upstream.Allocator;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import androidx.media3.exoplayer.upstream.Loader;
import androidx.media3.extractor.DiscardingTrackOutput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.TrackOutput;
import androidx.media3.extractor.metadata.emsg.EventMessage;
import androidx.media3.extractor.metadata.emsg.EventMessageDecoder;
import androidx.media3.extractor.metadata.id3.PrivFrame;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
final class HlsSampleStreamWrapper implements Loader.Callback<Chunk>, Loader.ReleaseCallback, SequenceableLoader, ExtractorOutput, SampleQueue.UpstreamFormatChangedListener {
    private static final Set<Integer> MAPPABLE_TYPES = Collections.unmodifiableSet(new HashSet(Arrays.asList(1, 2, 5)));
    public static final int SAMPLE_QUEUE_INDEX_NO_MAPPING_FATAL = -2;
    public static final int SAMPLE_QUEUE_INDEX_NO_MAPPING_NON_FATAL = -3;
    public static final int SAMPLE_QUEUE_INDEX_PENDING = -1;
    private static final String TAG = "HlsSampleStreamWrapper";
    private final Allocator allocator;
    private final Callback callback;
    private final HlsChunkSource chunkSource;
    private Format downstreamTrackFormat;
    private final DrmSessionEventListener.EventDispatcher drmEventDispatcher;
    private DrmInitData drmInitData;
    private final DrmSessionManager drmSessionManager;
    private TrackOutput emsgUnwrappingTrackOutput;
    private int enabledTrackGroupCount;
    private final Handler handler;
    private boolean haveAudioVideoSampleQueues;
    private long lastSeekPositionUs;
    private final LoadErrorHandlingPolicy loadErrorHandlingPolicy;
    private Chunk loadingChunk;
    private boolean loadingFinished;
    private final Runnable maybeFinishPrepareRunnable;
    private final MediaSourceEventListener.EventDispatcher mediaSourceEventDispatcher;
    private final int metadataType;
    private final Format muxedAudioFormat;
    private final Runnable onTracksEndedRunnable;
    private Set<TrackGroup> optionalTrackGroups;
    private final Map<String, DrmInitData> overridingDrmInitData;
    private long pendingResetPositionUs;
    private boolean pendingResetUpstreamFormats;
    private boolean prepared;
    private int primarySampleQueueIndex;
    private int primarySampleQueueType;
    private int primaryTrackGroupIndex;
    private boolean released;
    private long sampleOffsetUs;
    private boolean sampleQueuesBuilt;
    private boolean seenFirstTrackSelection;
    private HlsMediaChunk sourceChunk;
    private int[] trackGroupToSampleQueueIndex;
    private TrackGroupArray trackGroups;
    private final int trackType;
    private boolean tracksEnded;
    private final String uid;
    private Format upstreamTrackFormat;
    private final Loader loader = new Loader("Loader:HlsSampleStreamWrapper");
    private final HlsChunkSource.HlsChunkHolder nextChunkHolder = new HlsChunkSource.HlsChunkHolder();
    private int[] sampleQueueTrackIds = new int[0];
    private Set<Integer> sampleQueueMappingDoneByType = new HashSet(MAPPABLE_TYPES.size());
    private SparseIntArray sampleQueueIndicesByType = new SparseIntArray(MAPPABLE_TYPES.size());
    private HlsSampleQueue[] sampleQueues = new HlsSampleQueue[0];
    private boolean[] sampleQueueIsAudioVideoFlags = new boolean[0];
    private boolean[] sampleQueuesEnabledStates = new boolean[0];
    private final ArrayList<HlsMediaChunk> mediaChunks = new ArrayList<>();
    private final List<HlsMediaChunk> readOnlyMediaChunks = Collections.unmodifiableList(this.mediaChunks);
    private final ArrayList<HlsSampleStream> hlsSampleStreams = new ArrayList<>();

    public interface Callback extends SequenceableLoader.Callback<HlsSampleStreamWrapper> {
        void onPlaylistRefreshRequired(Uri uri);

        void onPrepared();
    }

    public HlsSampleStreamWrapper(String uid, int trackType, Callback callback, HlsChunkSource chunkSource, Map<String, DrmInitData> overridingDrmInitData, Allocator allocator, long positionUs, Format muxedAudioFormat, DrmSessionManager drmSessionManager, DrmSessionEventListener.EventDispatcher drmEventDispatcher, LoadErrorHandlingPolicy loadErrorHandlingPolicy, MediaSourceEventListener.EventDispatcher mediaSourceEventDispatcher, int metadataType) {
        this.uid = uid;
        this.trackType = trackType;
        this.callback = callback;
        this.chunkSource = chunkSource;
        this.overridingDrmInitData = overridingDrmInitData;
        this.allocator = allocator;
        this.muxedAudioFormat = muxedAudioFormat;
        this.drmSessionManager = drmSessionManager;
        this.drmEventDispatcher = drmEventDispatcher;
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy;
        this.mediaSourceEventDispatcher = mediaSourceEventDispatcher;
        this.metadataType = metadataType;
        Runnable maybeFinishPrepareRunnable = new Runnable() { // from class: androidx.media3.exoplayer.hls.HlsSampleStreamWrapper$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.maybeFinishPrepare();
            }
        };
        this.maybeFinishPrepareRunnable = maybeFinishPrepareRunnable;
        Runnable onTracksEndedRunnable = new Runnable() { // from class: androidx.media3.exoplayer.hls.HlsSampleStreamWrapper$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.onTracksEnded();
            }
        };
        this.onTracksEndedRunnable = onTracksEndedRunnable;
        this.handler = Util.createHandlerForCurrentLooper();
        this.lastSeekPositionUs = positionUs;
        this.pendingResetPositionUs = positionUs;
    }

    public void continuePreparing() {
        if (!this.prepared) {
            continueLoading(new LoadingInfo.Builder().setPlaybackPositionUs(this.lastSeekPositionUs).build());
        }
    }

    public void prepareWithMultivariantPlaylistInfo(TrackGroup[] trackGroups, int primaryTrackGroupIndex, int... optionalTrackGroupsIndices) {
        this.trackGroups = createTrackGroupArrayWithDrmInfo(trackGroups);
        this.optionalTrackGroups = new HashSet();
        for (int optionalTrackGroupIndex : optionalTrackGroupsIndices) {
            this.optionalTrackGroups.add(this.trackGroups.get(optionalTrackGroupIndex));
        }
        this.primaryTrackGroupIndex = primaryTrackGroupIndex;
        Handler handler = this.handler;
        final Callback callback = this.callback;
        Objects.requireNonNull(callback);
        handler.post(new Runnable() { // from class: androidx.media3.exoplayer.hls.HlsSampleStreamWrapper$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                callback.onPrepared();
            }
        });
        setIsPrepared();
    }

    public void maybeThrowPrepareError() throws IOException {
        maybeThrowError();
        if (this.loadingFinished && !this.prepared) {
            throw ParserException.createForMalformedContainer("Loading finished before preparation is complete.", null);
        }
    }

    public TrackGroupArray getTrackGroups() {
        assertIsPrepared();
        return this.trackGroups;
    }

    public int getPrimaryTrackGroupIndex() {
        return this.primaryTrackGroupIndex;
    }

    public int bindSampleQueueToSampleStream(int trackGroupIndex) {
        assertIsPrepared();
        Assertions.checkNotNull(this.trackGroupToSampleQueueIndex);
        int sampleQueueIndex = this.trackGroupToSampleQueueIndex[trackGroupIndex];
        if (sampleQueueIndex == -1) {
            return this.optionalTrackGroups.contains(this.trackGroups.get(trackGroupIndex)) ? -3 : -2;
        }
        if (this.sampleQueuesEnabledStates[sampleQueueIndex]) {
            return -2;
        }
        this.sampleQueuesEnabledStates[sampleQueueIndex] = true;
        return sampleQueueIndex;
    }

    public void unbindSampleQueue(int trackGroupIndex) {
        assertIsPrepared();
        Assertions.checkNotNull(this.trackGroupToSampleQueueIndex);
        int sampleQueueIndex = this.trackGroupToSampleQueueIndex[trackGroupIndex];
        Assertions.checkState(this.sampleQueuesEnabledStates[sampleQueueIndex]);
        this.sampleQueuesEnabledStates[sampleQueueIndex] = false;
    }

    /* JADX WARN: Code duplicated, block: B:100:0x0147 A[SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:78:0x0138  */
    /* JADX WARN: Code duplicated, block: B:81:0x013f  */
    /* JADX WARN: Code duplicated, block: B:83:0x0143  */
    public boolean selectTracks(ExoTrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs, boolean forceReset) {
        boolean forceReset2;
        int i;
        assertIsPrepared();
        int oldEnabledTrackGroupCount = this.enabledTrackGroupCount;
        for (int i2 = 0; i2 < selections.length; i2++) {
            HlsSampleStream stream = (HlsSampleStream) streams[i2];
            if (stream != null && (selections[i2] == null || !mayRetainStreamFlags[i2])) {
                this.enabledTrackGroupCount--;
                stream.unbindSampleQueue();
                streams[i2] = null;
            }
        }
        boolean seekRequired = forceReset || (!this.seenFirstTrackSelection ? positionUs == this.lastSeekPositionUs : oldEnabledTrackGroupCount != 0);
        ExoTrackSelection oldPrimaryTrackSelection = this.chunkSource.getTrackSelection();
        ExoTrackSelection primaryTrackSelection = oldPrimaryTrackSelection;
        boolean seekRequired2 = seekRequired;
        for (int i3 = 0; i3 < selections.length; i3++) {
            ExoTrackSelection selection = selections[i3];
            if (selection != null) {
                int trackGroupIndex = this.trackGroups.indexOf(selection.getTrackGroup());
                if (trackGroupIndex == this.primaryTrackGroupIndex) {
                    primaryTrackSelection = selection;
                    this.chunkSource.setTrackSelection(selection);
                }
                if (streams[i3] == null) {
                    this.enabledTrackGroupCount++;
                    streams[i3] = new HlsSampleStream(this, trackGroupIndex);
                    streamResetFlags[i3] = true;
                    if (this.trackGroupToSampleQueueIndex != null) {
                        ((HlsSampleStream) streams[i3]).bindSampleQueue();
                        if (!seekRequired2) {
                            SampleQueue sampleQueue = this.sampleQueues[this.trackGroupToSampleQueueIndex[trackGroupIndex]];
                            seekRequired2 = (sampleQueue.getReadIndex() == 0 || sampleQueue.seekTo(positionUs, true)) ? false : true;
                        }
                    }
                }
            }
        }
        if (this.enabledTrackGroupCount == 0) {
            this.chunkSource.reset();
            this.downstreamTrackFormat = null;
            this.pendingResetUpstreamFormats = true;
            this.mediaChunks.clear();
            if (this.loader.isLoading()) {
                if (this.sampleQueuesBuilt) {
                    for (SampleQueue sampleQueue2 : this.sampleQueues) {
                        sampleQueue2.discardToEnd();
                    }
                }
                this.loader.cancelLoading();
            } else {
                resetSampleQueues();
            }
        } else {
            if (!this.mediaChunks.isEmpty() && !Util.areEqual(primaryTrackSelection, oldPrimaryTrackSelection)) {
                boolean primarySampleQueueDirty = false;
                if (this.seenFirstTrackSelection) {
                    primarySampleQueueDirty = true;
                } else {
                    long bufferedDurationUs = positionUs < 0 ? -positionUs : 0L;
                    HlsMediaChunk lastMediaChunk = getLastMediaChunk();
                    MediaChunkIterator[] mediaChunkIterators = this.chunkSource.createMediaChunkIterators(lastMediaChunk, positionUs);
                    ExoTrackSelection primaryTrackSelection2 = primaryTrackSelection;
                    primaryTrackSelection2.updateSelectedTrack(positionUs, bufferedDurationUs, C.TIME_UNSET, this.readOnlyMediaChunks, mediaChunkIterators);
                    int chunkIndex = this.chunkSource.getTrackGroup().indexOf(lastMediaChunk.trackFormat);
                    if (primaryTrackSelection2.getSelectedIndexInTrackGroup() != chunkIndex) {
                        primarySampleQueueDirty = true;
                    }
                }
                if (primarySampleQueueDirty) {
                    forceReset2 = true;
                    this.pendingResetUpstreamFormats = true;
                    seekRequired2 = true;
                }
                if (seekRequired2) {
                    seekToUs(positionUs, forceReset2);
                    for (i = 0; i < streams.length; i++) {
                        if (streams[i] != null) {
                            streamResetFlags[i] = true;
                        }
                    }
                }
            }
            forceReset2 = forceReset;
            if (seekRequired2) {
                seekToUs(positionUs, forceReset2);
                while (i < streams.length) {
                    if (streams[i] != null) {
                        streamResetFlags[i] = true;
                    }
                }
            }
        }
        updateSampleStreams(streams);
        this.seenFirstTrackSelection = true;
        return seekRequired2;
    }

    public void discardBuffer(long positionUs, boolean toKeyframe) {
        if (!this.sampleQueuesBuilt || isPendingReset()) {
            return;
        }
        int sampleQueueCount = this.sampleQueues.length;
        for (int i = 0; i < sampleQueueCount; i++) {
            this.sampleQueues[i].discardTo(positionUs, toKeyframe, this.sampleQueuesEnabledStates[i]);
        }
    }

    public boolean seekToUs(long positionUs, boolean forceReset) {
        this.lastSeekPositionUs = positionUs;
        if (isPendingReset()) {
            this.pendingResetPositionUs = positionUs;
            return true;
        }
        HlsMediaChunk seekToMediaChunk = null;
        if (this.chunkSource.hasIndependentSegments()) {
            for (int i = 0; i < this.mediaChunks.size(); i++) {
                HlsMediaChunk mediaChunk = this.mediaChunks.get(i);
                long mediaChunkStartTimeUs = mediaChunk.startTimeUs;
                if (mediaChunkStartTimeUs == positionUs) {
                    seekToMediaChunk = mediaChunk;
                    break;
                }
            }
        }
        if (this.sampleQueuesBuilt && !forceReset && seekInsideBufferUs(positionUs, seekToMediaChunk)) {
            return false;
        }
        this.pendingResetPositionUs = positionUs;
        this.loadingFinished = false;
        this.mediaChunks.clear();
        if (this.loader.isLoading()) {
            if (this.sampleQueuesBuilt) {
                for (SampleQueue sampleQueue : this.sampleQueues) {
                    sampleQueue.discardToEnd();
                }
            }
            this.loader.cancelLoading();
        } else {
            this.loader.clearFatalError();
            resetSampleQueues();
        }
        return true;
    }

    public void onPlaylistUpdated() {
        if (this.mediaChunks.isEmpty()) {
            return;
        }
        final HlsMediaChunk lastMediaChunk = (HlsMediaChunk) Iterables.getLast(this.mediaChunks);
        int chunkState = this.chunkSource.getChunkPublicationState(lastMediaChunk);
        if (chunkState == 1) {
            lastMediaChunk.publish();
            return;
        }
        if (chunkState == 0) {
            this.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.hls.HlsSampleStreamWrapper$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m94x332ef84d(lastMediaChunk);
                }
            });
        } else if (chunkState == 2 && !this.loadingFinished && this.loader.isLoading()) {
            this.loader.cancelLoading();
        }
    }

    /* JADX INFO: renamed from: lambda$onPlaylistUpdated$0$androidx-media3-exoplayer-hls-HlsSampleStreamWrapper, reason: not valid java name */
    /* synthetic */ void m94x332ef84d(HlsMediaChunk lastMediaChunk) {
        this.callback.onPlaylistRefreshRequired(lastMediaChunk.playlistUrl);
    }

    public void release() {
        if (this.prepared) {
            for (SampleQueue sampleQueue : this.sampleQueues) {
                sampleQueue.preRelease();
            }
        }
        this.chunkSource.reset();
        this.loader.release(this);
        this.handler.removeCallbacksAndMessages(null);
        this.released = true;
        this.hlsSampleStreams.clear();
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.ReleaseCallback
    public void onLoaderReleased() {
        for (SampleQueue sampleQueue : this.sampleQueues) {
            sampleQueue.release();
        }
    }

    public void setIsPrimaryTimestampSource(boolean isPrimaryTimestampSource) {
        this.chunkSource.setIsPrimaryTimestampSource(isPrimaryTimestampSource);
    }

    public boolean onPlaylistError(Uri playlistUrl, LoadErrorHandlingPolicy.LoadErrorInfo loadErrorInfo, boolean forceRetry) {
        LoadErrorHandlingPolicy.FallbackSelection fallbackSelection;
        if (!this.chunkSource.obtainsChunksForPlaylist(playlistUrl)) {
            return true;
        }
        long exclusionDurationMs = C.TIME_UNSET;
        if (!forceRetry && (fallbackSelection = this.loadErrorHandlingPolicy.getFallbackSelectionFor(TrackSelectionUtil.createFallbackOptions(this.chunkSource.getTrackSelection()), loadErrorInfo)) != null && fallbackSelection.type == 2) {
            exclusionDurationMs = fallbackSelection.exclusionDurationMs;
        }
        return this.chunkSource.onPlaylistError(playlistUrl, exclusionDurationMs) && exclusionDurationMs != C.TIME_UNSET;
    }

    public boolean isVideoSampleStream() {
        return this.primarySampleQueueType == 2;
    }

    public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
        return this.chunkSource.getAdjustedSeekPositionUs(positionUs, seekParameters);
    }

    public boolean isReady(int sampleQueueIndex) {
        return !isPendingReset() && this.sampleQueues[sampleQueueIndex].isReady(this.loadingFinished);
    }

    public void maybeThrowError(int sampleQueueIndex) throws IOException {
        maybeThrowError();
        this.sampleQueues[sampleQueueIndex].maybeThrowError();
    }

    public void maybeThrowError() throws IOException {
        this.loader.maybeThrowError();
        this.chunkSource.maybeThrowError();
    }

    public int readData(int sampleQueueIndex, FormatHolder formatHolder, DecoderInputBuffer buffer, int readFlags) {
        Format trackFormat;
        if (isPendingReset()) {
            return -3;
        }
        if (!this.mediaChunks.isEmpty()) {
            int discardToMediaChunkIndex = 0;
            while (discardToMediaChunkIndex < this.mediaChunks.size() - 1 && finishedReadingChunk(this.mediaChunks.get(discardToMediaChunkIndex))) {
                discardToMediaChunkIndex++;
            }
            Util.removeRange(this.mediaChunks, 0, discardToMediaChunkIndex);
            HlsMediaChunk currentChunk = this.mediaChunks.get(0);
            Format trackFormat2 = currentChunk.trackFormat;
            if (!trackFormat2.equals(this.downstreamTrackFormat)) {
                this.mediaSourceEventDispatcher.downstreamFormatChanged(this.trackType, trackFormat2, currentChunk.trackSelectionReason, currentChunk.trackSelectionData, currentChunk.startTimeUs);
            }
            this.downstreamTrackFormat = trackFormat2;
        }
        if (!this.mediaChunks.isEmpty() && !this.mediaChunks.get(0).isPublished()) {
            return -3;
        }
        int result = this.sampleQueues[sampleQueueIndex].read(formatHolder, buffer, readFlags, this.loadingFinished);
        if (result == -5) {
            Format format = (Format) Assertions.checkNotNull(formatHolder.format);
            if (sampleQueueIndex == this.primarySampleQueueIndex) {
                int chunkUid = Ints.checkedCast(this.sampleQueues[sampleQueueIndex].peekSourceId());
                int chunkIndex = 0;
                while (chunkIndex < this.mediaChunks.size() && this.mediaChunks.get(chunkIndex).uid != chunkUid) {
                    chunkIndex++;
                }
                if (chunkIndex < this.mediaChunks.size()) {
                    trackFormat = this.mediaChunks.get(chunkIndex).trackFormat;
                } else {
                    trackFormat = (Format) Assertions.checkNotNull(this.upstreamTrackFormat);
                }
                format = format.withManifestFormatInfo(trackFormat);
            }
            formatHolder.format = format;
        }
        return result;
    }

    public int skipData(int sampleQueueIndex, long positionUs) throws Throwable {
        if (isPendingReset()) {
            return 0;
        }
        SampleQueue sampleQueue = this.sampleQueues[sampleQueueIndex];
        int skipCount = sampleQueue.getSkipCount(positionUs, this.loadingFinished);
        HlsMediaChunk lastChunk = (HlsMediaChunk) Iterables.getLast(this.mediaChunks, null);
        if (lastChunk != null && !lastChunk.isPublished()) {
            int readIndex = sampleQueue.getReadIndex();
            int firstSampleIndex = lastChunk.getFirstSampleIndex(sampleQueueIndex);
            skipCount = Math.min(skipCount, firstSampleIndex - readIndex);
        }
        sampleQueue.skip(skipCount);
        return skipCount;
    }

    @Override // androidx.media3.exoplayer.source.SequenceableLoader
    public long getBufferedPositionUs() {
        HlsMediaChunk lastCompletedMediaChunk;
        if (this.loadingFinished) {
            return Long.MIN_VALUE;
        }
        if (isPendingReset()) {
            return this.pendingResetPositionUs;
        }
        long bufferedPositionUs = this.lastSeekPositionUs;
        HlsMediaChunk lastMediaChunk = getLastMediaChunk();
        if (lastMediaChunk.isLoadCompleted()) {
            lastCompletedMediaChunk = lastMediaChunk;
        } else {
            lastCompletedMediaChunk = this.mediaChunks.size() > 1 ? this.mediaChunks.get(this.mediaChunks.size() - 2) : null;
        }
        if (lastCompletedMediaChunk != null) {
            bufferedPositionUs = Math.max(bufferedPositionUs, lastCompletedMediaChunk.endTimeUs);
        }
        if (this.sampleQueuesBuilt) {
            for (SampleQueue sampleQueue : this.sampleQueues) {
                bufferedPositionUs = Math.max(bufferedPositionUs, sampleQueue.getLargestQueuedTimestampUs());
            }
        }
        return bufferedPositionUs;
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
    public boolean continueLoading(LoadingInfo loadingInfo) {
        long jMax;
        long loadPositionUs;
        List<HlsMediaChunk> chunkQueue;
        if (this.loadingFinished || this.loader.isLoading() || this.loader.hasFatalError()) {
            return false;
        }
        if (isPendingReset()) {
            List<HlsMediaChunk> chunkQueue2 = Collections.emptyList();
            long loadPositionUs2 = this.pendingResetPositionUs;
            for (SampleQueue sampleQueue : this.sampleQueues) {
                sampleQueue.setStartTimeUs(this.pendingResetPositionUs);
            }
            loadPositionUs = loadPositionUs2;
            chunkQueue = chunkQueue2;
        } else {
            List<HlsMediaChunk> chunkQueue3 = this.readOnlyMediaChunks;
            HlsMediaChunk lastMediaChunk = getLastMediaChunk();
            if (lastMediaChunk.isLoadCompleted()) {
                jMax = lastMediaChunk.endTimeUs;
            } else {
                jMax = Math.max(this.lastSeekPositionUs, lastMediaChunk.startTimeUs);
            }
            loadPositionUs = jMax;
            chunkQueue = chunkQueue3;
        }
        this.nextChunkHolder.clear();
        this.chunkSource.getNextChunk(loadingInfo, loadPositionUs, chunkQueue, this.prepared || !chunkQueue.isEmpty(), this.nextChunkHolder);
        boolean endOfStream = this.nextChunkHolder.endOfStream;
        Chunk loadable = this.nextChunkHolder.chunk;
        Uri playlistUrlToLoad = this.nextChunkHolder.playlistUrl;
        if (endOfStream) {
            this.pendingResetPositionUs = C.TIME_UNSET;
            this.loadingFinished = true;
            return true;
        }
        if (loadable == null) {
            if (playlistUrlToLoad != null) {
                this.callback.onPlaylistRefreshRequired(playlistUrlToLoad);
            }
            return false;
        }
        if (isMediaChunk(loadable)) {
            initMediaChunkLoad((HlsMediaChunk) loadable);
        }
        this.loadingChunk = loadable;
        long elapsedRealtimeMs = this.loader.startLoading(loadable, this, this.loadErrorHandlingPolicy.getMinimumLoadableRetryCount(loadable.type));
        this.mediaSourceEventDispatcher.loadStarted(new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, elapsedRealtimeMs), loadable.type, this.trackType, loadable.trackFormat, loadable.trackSelectionReason, loadable.trackSelectionData, loadable.startTimeUs, loadable.endTimeUs);
        return true;
    }

    @Override // androidx.media3.exoplayer.source.SequenceableLoader
    public boolean isLoading() {
        return this.loader.isLoading();
    }

    @Override // androidx.media3.exoplayer.source.SequenceableLoader
    public void reevaluateBuffer(long positionUs) {
        if (this.loader.hasFatalError() || isPendingReset()) {
            return;
        }
        if (this.loader.isLoading()) {
            Assertions.checkNotNull(this.loadingChunk);
            if (this.chunkSource.shouldCancelLoad(positionUs, this.loadingChunk, this.readOnlyMediaChunks)) {
                this.loader.cancelLoading();
                return;
            }
            return;
        }
        int newQueueSize = this.readOnlyMediaChunks.size();
        while (newQueueSize > 0 && this.chunkSource.getChunkPublicationState(this.readOnlyMediaChunks.get(newQueueSize - 1)) == 2) {
            newQueueSize--;
        }
        if (newQueueSize < this.readOnlyMediaChunks.size()) {
            discardUpstream(newQueueSize);
        }
        int preferredQueueSize = this.chunkSource.getPreferredQueueSize(positionUs, this.readOnlyMediaChunks);
        if (preferredQueueSize < this.mediaChunks.size()) {
            discardUpstream(preferredQueueSize);
        }
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Callback
    public void onLoadCompleted(Chunk loadable, long elapsedRealtimeMs, long loadDurationMs) {
        this.loadingChunk = null;
        this.chunkSource.onChunkLoadCompleted(loadable);
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
        this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        this.mediaSourceEventDispatcher.loadCompleted(loadEventInfo, loadable.type, this.trackType, loadable.trackFormat, loadable.trackSelectionReason, loadable.trackSelectionData, loadable.startTimeUs, loadable.endTimeUs);
        if (!this.prepared) {
            continueLoading(new LoadingInfo.Builder().setPlaybackPositionUs(this.lastSeekPositionUs).build());
        } else {
            this.callback.onContinueLoadingRequested(this);
        }
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Callback
    public void onLoadCanceled(Chunk loadable, long elapsedRealtimeMs, long loadDurationMs, boolean released) {
        this.loadingChunk = null;
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
        this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        this.mediaSourceEventDispatcher.loadCanceled(loadEventInfo, loadable.type, this.trackType, loadable.trackFormat, loadable.trackSelectionReason, loadable.trackSelectionData, loadable.startTimeUs, loadable.endTimeUs);
        if (!released) {
            if (isPendingReset() || this.enabledTrackGroupCount == 0) {
                resetSampleQueues();
            }
            if (this.enabledTrackGroupCount > 0) {
                this.callback.onContinueLoadingRequested(this);
            }
        }
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Callback
    public Loader.LoadErrorAction onLoadError(Chunk loadable, long elapsedRealtimeMs, long loadDurationMs, IOException error, int errorCount) {
        boolean exclusionSucceeded;
        Loader.LoadErrorAction loadErrorActionCreateRetryAction;
        Loader.LoadErrorAction loadErrorAction;
        int responseCode;
        boolean isMediaChunk = isMediaChunk(loadable);
        if (isMediaChunk && !((HlsMediaChunk) loadable).isPublished() && (error instanceof HttpDataSource.InvalidResponseCodeException) && ((responseCode = ((HttpDataSource.InvalidResponseCodeException) error).responseCode) == 410 || responseCode == 404)) {
            return Loader.RETRY;
        }
        long bytesLoaded = loadable.bytesLoaded();
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, bytesLoaded);
        MediaLoadData mediaLoadData = new MediaLoadData(loadable.type, this.trackType, loadable.trackFormat, loadable.trackSelectionReason, loadable.trackSelectionData, Util.usToMs(loadable.startTimeUs), Util.usToMs(loadable.endTimeUs));
        LoadErrorHandlingPolicy.LoadErrorInfo loadErrorInfo = new LoadErrorHandlingPolicy.LoadErrorInfo(loadEventInfo, mediaLoadData, error, errorCount);
        LoadErrorHandlingPolicy.FallbackSelection fallbackSelection = this.loadErrorHandlingPolicy.getFallbackSelectionFor(TrackSelectionUtil.createFallbackOptions(this.chunkSource.getTrackSelection()), loadErrorInfo);
        if (fallbackSelection != null && fallbackSelection.type == 2) {
            boolean exclusionSucceeded2 = this.chunkSource.maybeExcludeTrack(loadable, fallbackSelection.exclusionDurationMs);
            exclusionSucceeded = exclusionSucceeded2;
        } else {
            exclusionSucceeded = false;
        }
        if (exclusionSucceeded) {
            if (isMediaChunk && bytesLoaded == 0) {
                HlsMediaChunk removed = this.mediaChunks.remove(this.mediaChunks.size() - 1);
                boolean exclusionSucceeded3 = removed == loadable;
                Assertions.checkState(exclusionSucceeded3);
                if (this.mediaChunks.isEmpty()) {
                    this.pendingResetPositionUs = this.lastSeekPositionUs;
                } else {
                    ((HlsMediaChunk) Iterables.getLast(this.mediaChunks)).invalidateExtractor();
                }
            }
            loadErrorAction = Loader.DONT_RETRY;
        } else {
            long retryDelayMs = this.loadErrorHandlingPolicy.getRetryDelayMsFor(loadErrorInfo);
            if (retryDelayMs != C.TIME_UNSET) {
                loadErrorActionCreateRetryAction = Loader.createRetryAction(false, retryDelayMs);
            } else {
                loadErrorActionCreateRetryAction = Loader.DONT_RETRY_FATAL;
            }
            loadErrorAction = loadErrorActionCreateRetryAction;
        }
        boolean wasCanceled = !loadErrorAction.isRetry();
        this.mediaSourceEventDispatcher.loadError(loadEventInfo, loadable.type, this.trackType, loadable.trackFormat, loadable.trackSelectionReason, loadable.trackSelectionData, loadable.startTimeUs, loadable.endTimeUs, error, wasCanceled);
        if (wasCanceled) {
            this.loadingChunk = null;
            this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        }
        if (exclusionSucceeded) {
            if (this.prepared) {
                this.callback.onContinueLoadingRequested(this);
            } else {
                continueLoading(new LoadingInfo.Builder().setPlaybackPositionUs(this.lastSeekPositionUs).build());
            }
        }
        return loadErrorAction;
    }

    private void initMediaChunkLoad(HlsMediaChunk chunk) {
        this.sourceChunk = chunk;
        this.upstreamTrackFormat = chunk.trackFormat;
        this.pendingResetPositionUs = C.TIME_UNSET;
        this.mediaChunks.add(chunk);
        ImmutableList.Builder<Integer> sampleQueueWriteIndicesBuilder = ImmutableList.builder();
        for (SampleQueue sampleQueue : this.sampleQueues) {
            sampleQueueWriteIndicesBuilder.add(Integer.valueOf(sampleQueue.getWriteIndex()));
        }
        chunk.init(this, sampleQueueWriteIndicesBuilder.build());
        for (HlsSampleQueue sampleQueue2 : this.sampleQueues) {
            sampleQueue2.setSourceChunk(chunk);
            if (chunk.shouldSpliceIn) {
                sampleQueue2.splice();
            }
        }
    }

    private void discardUpstream(int preferredQueueSize) {
        Assertions.checkState(!this.loader.isLoading());
        int newQueueSize = -1;
        for (int i = preferredQueueSize; i < this.mediaChunks.size(); i++) {
            if (canDiscardUpstreamMediaChunksFromIndex(i)) {
                newQueueSize = i;
                break;
            }
        }
        if (newQueueSize == -1) {
            return;
        }
        long endTimeUs = getLastMediaChunk().endTimeUs;
        HlsMediaChunk firstRemovedChunk = discardUpstreamMediaChunksFromIndex(newQueueSize);
        if (this.mediaChunks.isEmpty()) {
            this.pendingResetPositionUs = this.lastSeekPositionUs;
        } else {
            ((HlsMediaChunk) Iterables.getLast(this.mediaChunks)).invalidateExtractor();
        }
        this.loadingFinished = false;
        this.mediaSourceEventDispatcher.upstreamDiscarded(this.primarySampleQueueType, firstRemovedChunk.startTimeUs, endTimeUs);
    }

    @Override // androidx.media3.extractor.ExtractorOutput
    public TrackOutput track(int id, int type) {
        TrackOutput trackOutput = null;
        if (MAPPABLE_TYPES.contains(Integer.valueOf(type))) {
            trackOutput = getMappedTrackOutput(id, type);
        } else {
            for (int i = 0; i < this.sampleQueues.length; i++) {
                if (this.sampleQueueTrackIds[i] == id) {
                    trackOutput = this.sampleQueues[i];
                    break;
                }
            }
        }
        if (trackOutput == null) {
            if (this.tracksEnded) {
                return createDiscardingTrackOutput(id, type);
            }
            trackOutput = createSampleQueue(id, type);
        }
        if (type == 5) {
            if (this.emsgUnwrappingTrackOutput == null) {
                this.emsgUnwrappingTrackOutput = new EmsgUnwrappingTrackOutput(trackOutput, this.metadataType);
            }
            return this.emsgUnwrappingTrackOutput;
        }
        return trackOutput;
    }

    private TrackOutput getMappedTrackOutput(int id, int type) {
        Assertions.checkArgument(MAPPABLE_TYPES.contains(Integer.valueOf(type)));
        int sampleQueueIndex = this.sampleQueueIndicesByType.get(type, -1);
        if (sampleQueueIndex == -1) {
            return null;
        }
        if (this.sampleQueueMappingDoneByType.add(Integer.valueOf(type))) {
            this.sampleQueueTrackIds[sampleQueueIndex] = id;
        }
        if (this.sampleQueueTrackIds[sampleQueueIndex] == id) {
            return this.sampleQueues[sampleQueueIndex];
        }
        return createDiscardingTrackOutput(id, type);
    }

    private SampleQueue createSampleQueue(int id, int type) {
        int trackCount = this.sampleQueues.length;
        boolean isAudioVideo = true;
        if (type != 1 && type != 2) {
            isAudioVideo = false;
        }
        HlsSampleQueue sampleQueue = new HlsSampleQueue(this.allocator, this.drmSessionManager, this.drmEventDispatcher, this.overridingDrmInitData);
        sampleQueue.setStartTimeUs(this.lastSeekPositionUs);
        if (isAudioVideo) {
            sampleQueue.setDrmInitData(this.drmInitData);
        }
        sampleQueue.setSampleOffsetUs(this.sampleOffsetUs);
        if (this.sourceChunk != null) {
            sampleQueue.setSourceChunk(this.sourceChunk);
        }
        sampleQueue.setUpstreamFormatChangeListener(this);
        this.sampleQueueTrackIds = Arrays.copyOf(this.sampleQueueTrackIds, trackCount + 1);
        this.sampleQueueTrackIds[trackCount] = id;
        this.sampleQueues = (HlsSampleQueue[]) Util.nullSafeArrayAppend(this.sampleQueues, sampleQueue);
        this.sampleQueueIsAudioVideoFlags = Arrays.copyOf(this.sampleQueueIsAudioVideoFlags, trackCount + 1);
        this.sampleQueueIsAudioVideoFlags[trackCount] = isAudioVideo;
        this.haveAudioVideoSampleQueues |= this.sampleQueueIsAudioVideoFlags[trackCount];
        this.sampleQueueMappingDoneByType.add(Integer.valueOf(type));
        this.sampleQueueIndicesByType.append(type, trackCount);
        if (getTrackTypeScore(type) > getTrackTypeScore(this.primarySampleQueueType)) {
            this.primarySampleQueueIndex = trackCount;
            this.primarySampleQueueType = type;
        }
        this.sampleQueuesEnabledStates = Arrays.copyOf(this.sampleQueuesEnabledStates, trackCount + 1);
        return sampleQueue;
    }

    @Override // androidx.media3.extractor.ExtractorOutput
    public void endTracks() {
        this.tracksEnded = true;
        this.handler.post(this.onTracksEndedRunnable);
    }

    @Override // androidx.media3.extractor.ExtractorOutput
    public void seekMap(SeekMap seekMap) {
    }

    @Override // androidx.media3.exoplayer.source.SampleQueue.UpstreamFormatChangedListener
    public void onUpstreamFormatChanged(Format format) {
        this.handler.post(this.maybeFinishPrepareRunnable);
    }

    public void onNewExtractor() {
        this.sampleQueueMappingDoneByType.clear();
    }

    public void setSampleOffsetUs(long sampleOffsetUs) {
        if (this.sampleOffsetUs != sampleOffsetUs) {
            this.sampleOffsetUs = sampleOffsetUs;
            for (SampleQueue sampleQueue : this.sampleQueues) {
                sampleQueue.setSampleOffsetUs(sampleOffsetUs);
            }
        }
    }

    public void setDrmInitData(DrmInitData drmInitData) {
        if (!Util.areEqual(this.drmInitData, drmInitData)) {
            this.drmInitData = drmInitData;
            for (int i = 0; i < this.sampleQueues.length; i++) {
                if (this.sampleQueueIsAudioVideoFlags[i]) {
                    this.sampleQueues[i].setDrmInitData(drmInitData);
                }
            }
        }
    }

    private void updateSampleStreams(SampleStream[] streams) {
        this.hlsSampleStreams.clear();
        for (SampleStream stream : streams) {
            if (stream != null) {
                this.hlsSampleStreams.add((HlsSampleStream) stream);
            }
        }
    }

    private boolean finishedReadingChunk(HlsMediaChunk chunk) {
        int chunkUid = chunk.uid;
        int sampleQueueCount = this.sampleQueues.length;
        for (int i = 0; i < sampleQueueCount; i++) {
            if (this.sampleQueuesEnabledStates[i] && this.sampleQueues[i].peekSourceId() == chunkUid) {
                return false;
            }
        }
        return true;
    }

    private boolean canDiscardUpstreamMediaChunksFromIndex(int mediaChunkIndex) {
        int i = mediaChunkIndex;
        while (true) {
            int size = this.mediaChunks.size();
            ArrayList<HlsMediaChunk> arrayList = this.mediaChunks;
            if (i < size) {
                if (arrayList.get(i).shouldSpliceIn) {
                    return false;
                }
                i++;
            } else {
                HlsMediaChunk mediaChunk = arrayList.get(mediaChunkIndex);
                for (int i2 = 0; i2 < this.sampleQueues.length; i2++) {
                    int discardFromIndex = mediaChunk.getFirstSampleIndex(i2);
                    if (this.sampleQueues[i2].getReadIndex() > discardFromIndex) {
                        return false;
                    }
                }
                return true;
            }
        }
    }

    private HlsMediaChunk discardUpstreamMediaChunksFromIndex(int chunkIndex) {
        HlsMediaChunk firstRemovedChunk = this.mediaChunks.get(chunkIndex);
        Util.removeRange(this.mediaChunks, chunkIndex, this.mediaChunks.size());
        for (int i = 0; i < this.sampleQueues.length; i++) {
            int discardFromIndex = firstRemovedChunk.getFirstSampleIndex(i);
            this.sampleQueues[i].discardUpstreamSamples(discardFromIndex);
        }
        return firstRemovedChunk;
    }

    private void resetSampleQueues() {
        for (SampleQueue sampleQueue : this.sampleQueues) {
            sampleQueue.reset(this.pendingResetUpstreamFormats);
        }
        this.pendingResetUpstreamFormats = false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onTracksEnded() {
        this.sampleQueuesBuilt = true;
        maybeFinishPrepare();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeFinishPrepare() {
        if (this.released || this.trackGroupToSampleQueueIndex != null || !this.sampleQueuesBuilt) {
            return;
        }
        for (SampleQueue sampleQueue : this.sampleQueues) {
            if (sampleQueue.getUpstreamFormat() == null) {
                return;
            }
        }
        if (this.trackGroups != null) {
            mapSampleQueuesToMatchTrackGroups();
            return;
        }
        buildTracksFromSampleStreams();
        setIsPrepared();
        this.callback.onPrepared();
    }

    @EnsuresNonNull({"trackGroupToSampleQueueIndex"})
    @RequiresNonNull({"trackGroups"})
    private void mapSampleQueuesToMatchTrackGroups() {
        int trackGroupCount = this.trackGroups.length;
        this.trackGroupToSampleQueueIndex = new int[trackGroupCount];
        Arrays.fill(this.trackGroupToSampleQueueIndex, -1);
        for (int i = 0; i < trackGroupCount; i++) {
            for (int queueIndex = 0; queueIndex < this.sampleQueues.length; queueIndex++) {
                SampleQueue sampleQueue = this.sampleQueues[queueIndex];
                Format upstreamFormat = (Format) Assertions.checkStateNotNull(sampleQueue.getUpstreamFormat());
                if (formatsMatch(upstreamFormat, this.trackGroups.get(i).getFormat(0))) {
                    this.trackGroupToSampleQueueIndex[i] = queueIndex;
                    break;
                }
            }
        }
        for (HlsSampleStream sampleStream : this.hlsSampleStreams) {
            sampleStream.bindSampleQueue();
        }
    }

    @EnsuresNonNull({"trackGroups", "optionalTrackGroups", "trackGroupToSampleQueueIndex"})
    private void buildTracksFromSampleStreams() {
        Format playlistFormat;
        Format formatDeriveFormat;
        int trackType;
        int primaryExtractorTrackType = -2;
        int primaryExtractorTrackIndex = -1;
        int extractorTrackCount = this.sampleQueues.length;
        for (int i = 0; i < extractorTrackCount; i++) {
            String sampleMimeType = ((Format) Assertions.checkStateNotNull(this.sampleQueues[i].getUpstreamFormat())).sampleMimeType;
            if (MimeTypes.isVideo(sampleMimeType)) {
                trackType = 2;
            } else if (MimeTypes.isAudio(sampleMimeType)) {
                trackType = 1;
            } else if (MimeTypes.isText(sampleMimeType)) {
                trackType = 3;
            } else {
                trackType = -2;
            }
            if (getTrackTypeScore(trackType) > getTrackTypeScore(primaryExtractorTrackType)) {
                primaryExtractorTrackType = trackType;
                primaryExtractorTrackIndex = i;
            } else if (trackType == primaryExtractorTrackType && primaryExtractorTrackIndex != -1) {
                primaryExtractorTrackIndex = -1;
            }
        }
        TrackGroup chunkSourceTrackGroup = this.chunkSource.getTrackGroup();
        int chunkSourceTrackCount = chunkSourceTrackGroup.length;
        this.primaryTrackGroupIndex = -1;
        this.trackGroupToSampleQueueIndex = new int[extractorTrackCount];
        for (int i2 = 0; i2 < extractorTrackCount; i2++) {
            this.trackGroupToSampleQueueIndex[i2] = i2;
        }
        TrackGroup[] trackGroups = new TrackGroup[extractorTrackCount];
        int i3 = 0;
        while (true) {
            if (i3 >= extractorTrackCount) {
                break;
            }
            Format sampleFormat = (Format) Assertions.checkStateNotNull(this.sampleQueues[i3].getUpstreamFormat());
            if (i3 == primaryExtractorTrackIndex) {
                Format[] formats = new Format[chunkSourceTrackCount];
                for (int j = 0; j < chunkSourceTrackCount; j++) {
                    Format playlistFormat2 = chunkSourceTrackGroup.getFormat(j);
                    if (primaryExtractorTrackType == 1 && this.muxedAudioFormat != null) {
                        playlistFormat2 = playlistFormat2.withManifestFormatInfo(this.muxedAudioFormat);
                    }
                    if (chunkSourceTrackCount == 1) {
                        formatDeriveFormat = sampleFormat.withManifestFormatInfo(playlistFormat2);
                    } else {
                        formatDeriveFormat = deriveFormat(playlistFormat2, sampleFormat, true);
                    }
                    formats[j] = formatDeriveFormat;
                }
                trackGroups[i3] = new TrackGroup(this.uid, formats);
                this.primaryTrackGroupIndex = i3;
            } else {
                if (primaryExtractorTrackType == 2 && MimeTypes.isAudio(sampleFormat.sampleMimeType)) {
                    playlistFormat = this.muxedAudioFormat;
                } else {
                    playlistFormat = null;
                }
                String muxedTrackGroupId = this.uid + ":muxed:" + (i3 < primaryExtractorTrackIndex ? i3 : i3 - 1);
                trackGroups[i3] = new TrackGroup(muxedTrackGroupId, deriveFormat(playlistFormat, sampleFormat, false));
            }
            i3++;
        }
        this.trackGroups = createTrackGroupArrayWithDrmInfo(trackGroups);
        Assertions.checkState(this.optionalTrackGroups == null);
        this.optionalTrackGroups = Collections.emptySet();
    }

    private TrackGroupArray createTrackGroupArrayWithDrmInfo(TrackGroup[] trackGroups) {
        for (int i = 0; i < trackGroups.length; i++) {
            TrackGroup trackGroup = trackGroups[i];
            Format[] exposedFormats = new Format[trackGroup.length];
            for (int j = 0; j < trackGroup.length; j++) {
                Format format = trackGroup.getFormat(j);
                exposedFormats[j] = format.copyWithCryptoType(this.drmSessionManager.getCryptoType(format));
            }
            trackGroups[i] = new TrackGroup(trackGroup.id, exposedFormats);
        }
        return new TrackGroupArray(trackGroups);
    }

    private HlsMediaChunk getLastMediaChunk() {
        return this.mediaChunks.get(this.mediaChunks.size() - 1);
    }

    private boolean isPendingReset() {
        return this.pendingResetPositionUs != C.TIME_UNSET;
    }

    private boolean seekInsideBufferUs(long positionUs, HlsMediaChunk chunk) {
        boolean seekInsideQueue;
        int sampleQueueCount = this.sampleQueues.length;
        for (int i = 0; i < sampleQueueCount; i++) {
            SampleQueue sampleQueue = this.sampleQueues[i];
            if (chunk != null) {
                seekInsideQueue = sampleQueue.seekTo(chunk.getFirstSampleIndex(i));
            } else {
                seekInsideQueue = sampleQueue.seekTo(positionUs, false);
            }
            if (!seekInsideQueue && (this.sampleQueueIsAudioVideoFlags[i] || !this.haveAudioVideoSampleQueues)) {
                return false;
            }
        }
        return true;
    }

    @RequiresNonNull({"trackGroups", "optionalTrackGroups"})
    private void setIsPrepared() {
        this.prepared = true;
    }

    @EnsuresNonNull({"trackGroups", "optionalTrackGroups"})
    private void assertIsPrepared() {
        Assertions.checkState(this.prepared);
        Assertions.checkNotNull(this.trackGroups);
        Assertions.checkNotNull(this.optionalTrackGroups);
    }

    private static int getTrackTypeScore(int trackType) {
        switch (trackType) {
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 1;
            default:
                return 0;
        }
    }

    private static Format deriveFormat(Format playlistFormat, Format sampleFormat, boolean propagateBitrates) {
        String codecs;
        String sampleMimeType;
        if (playlistFormat == null) {
            return sampleFormat;
        }
        int sampleTrackType = MimeTypes.getTrackType(sampleFormat.sampleMimeType);
        if (Util.getCodecCountOfType(playlistFormat.codecs, sampleTrackType) == 1) {
            codecs = Util.getCodecsOfType(playlistFormat.codecs, sampleTrackType);
            sampleMimeType = MimeTypes.getMediaMimeType(codecs);
        } else {
            String codecs2 = playlistFormat.codecs;
            codecs = MimeTypes.getCodecsCorrespondingToMimeType(codecs2, sampleFormat.sampleMimeType);
            sampleMimeType = sampleFormat.sampleMimeType;
        }
        Format.Builder formatBuilder = sampleFormat.buildUpon().setId(playlistFormat.id).setLabel(playlistFormat.label).setLabels(playlistFormat.labels).setLanguage(playlistFormat.language).setSelectionFlags(playlistFormat.selectionFlags).setRoleFlags(playlistFormat.roleFlags).setAverageBitrate(propagateBitrates ? playlistFormat.averageBitrate : -1).setPeakBitrate(propagateBitrates ? playlistFormat.peakBitrate : -1).setCodecs(codecs);
        if (sampleTrackType == 2) {
            formatBuilder.setWidth(playlistFormat.width).setHeight(playlistFormat.height).setFrameRate(playlistFormat.frameRate);
        }
        if (sampleMimeType != null) {
            formatBuilder.setSampleMimeType(sampleMimeType);
        }
        if (playlistFormat.channelCount != -1 && sampleTrackType == 1) {
            formatBuilder.setChannelCount(playlistFormat.channelCount);
        }
        if (playlistFormat.metadata != null) {
            Metadata metadata = playlistFormat.metadata;
            if (sampleFormat.metadata != null) {
                metadata = sampleFormat.metadata.copyWithAppendedEntriesFrom(metadata);
            }
            formatBuilder.setMetadata(metadata);
        }
        return formatBuilder.build();
    }

    private static boolean isMediaChunk(Chunk chunk) {
        return chunk instanceof HlsMediaChunk;
    }

    private static boolean formatsMatch(Format manifestFormat, Format sampleFormat) {
        String manifestFormatMimeType = manifestFormat.sampleMimeType;
        String sampleFormatMimeType = sampleFormat.sampleMimeType;
        int manifestFormatTrackType = MimeTypes.getTrackType(manifestFormatMimeType);
        if (manifestFormatTrackType != 3) {
            return manifestFormatTrackType == MimeTypes.getTrackType(sampleFormatMimeType);
        }
        if (Util.areEqual(manifestFormatMimeType, sampleFormatMimeType)) {
            return !(MimeTypes.APPLICATION_CEA608.equals(manifestFormatMimeType) || MimeTypes.APPLICATION_CEA708.equals(manifestFormatMimeType)) || manifestFormat.accessibilityChannel == sampleFormat.accessibilityChannel;
        }
        return false;
    }

    private static DiscardingTrackOutput createDiscardingTrackOutput(int id, int type) {
        Log.w(TAG, "Unmapped track with id " + id + " of type " + type);
        return new DiscardingTrackOutput();
    }

    private static final class HlsSampleQueue extends SampleQueue {
        private DrmInitData drmInitData;
        private final Map<String, DrmInitData> overridingDrmInitData;

        private HlsSampleQueue(Allocator allocator, DrmSessionManager drmSessionManager, DrmSessionEventListener.EventDispatcher eventDispatcher, Map<String, DrmInitData> overridingDrmInitData) {
            super(allocator, drmSessionManager, eventDispatcher);
            this.overridingDrmInitData = overridingDrmInitData;
        }

        public void setSourceChunk(HlsMediaChunk chunk) {
            sourceId(chunk.uid);
        }

        public void setDrmInitData(DrmInitData drmInitData) {
            this.drmInitData = drmInitData;
            invalidateUpstreamFormatAdjustment();
        }

        @Override // androidx.media3.exoplayer.source.SampleQueue
        public Format getAdjustedUpstreamFormat(Format format) {
            DrmInitData overridingDrmInitData;
            DrmInitData drmInitData = this.drmInitData != null ? this.drmInitData : format.drmInitData;
            if (drmInitData != null && (overridingDrmInitData = this.overridingDrmInitData.get(drmInitData.schemeType)) != null) {
                drmInitData = overridingDrmInitData;
            }
            Metadata metadata = getAdjustedMetadata(format.metadata);
            if (drmInitData != format.drmInitData || metadata != format.metadata) {
                format = format.buildUpon().setDrmInitData(drmInitData).setMetadata(metadata).build();
            }
            return super.getAdjustedUpstreamFormat(format);
        }

        private Metadata getAdjustedMetadata(Metadata metadata) {
            if (metadata == null) {
                return null;
            }
            int length = metadata.length();
            int transportStreamTimestampMetadataIndex = -1;
            for (int i = 0; i < length; i++) {
                Metadata.Entry metadataEntry = metadata.get(i);
                if (metadataEntry instanceof PrivFrame) {
                    PrivFrame privFrame = (PrivFrame) metadataEntry;
                    if (HlsMediaChunk.PRIV_TIMESTAMP_FRAME_OWNER.equals(privFrame.owner)) {
                        transportStreamTimestampMetadataIndex = i;
                        break;
                    }
                }
            }
            if (transportStreamTimestampMetadataIndex == -1) {
                return metadata;
            }
            if (length == 1) {
                return null;
            }
            Metadata.Entry[] newMetadataEntries = new Metadata.Entry[length - 1];
            int i2 = 0;
            while (i2 < length) {
                if (i2 != transportStreamTimestampMetadataIndex) {
                    int newIndex = i2 < transportStreamTimestampMetadataIndex ? i2 : i2 - 1;
                    newMetadataEntries[newIndex] = metadata.get(i2);
                }
                i2++;
            }
            return new Metadata(newMetadataEntries);
        }

        @Override // androidx.media3.exoplayer.source.SampleQueue, androidx.media3.extractor.TrackOutput
        public void sampleMetadata(long timeUs, int flags, int size, int offset, TrackOutput.CryptoData cryptoData) {
            super.sampleMetadata(timeUs, flags, size, offset, cryptoData);
        }
    }

    private static class EmsgUnwrappingTrackOutput implements TrackOutput {
        private byte[] buffer;
        private int bufferPosition;
        private final TrackOutput delegate;
        private final Format delegateFormat;
        private final EventMessageDecoder emsgDecoder = new EventMessageDecoder();
        private Format format;
        private static final Format ID3_FORMAT = new Format.Builder().setSampleMimeType(MimeTypes.APPLICATION_ID3).build();
        private static final Format EMSG_FORMAT = new Format.Builder().setSampleMimeType(MimeTypes.APPLICATION_EMSG).build();

        @Override // androidx.media3.extractor.TrackOutput
        public /* synthetic */ int sampleData(DataReader dataReader, int i, boolean z) {
            return sampleData(dataReader, i, z, 0);
        }

        @Override // androidx.media3.extractor.TrackOutput
        public /* synthetic */ void sampleData(ParsableByteArray parsableByteArray, int i) {
            sampleData(parsableByteArray, i, 0);
        }

        public EmsgUnwrappingTrackOutput(TrackOutput delegate, int metadataType) {
            this.delegate = delegate;
            switch (metadataType) {
                case 1:
                    this.delegateFormat = ID3_FORMAT;
                    break;
                case 2:
                default:
                    throw new IllegalArgumentException("Unknown metadataType: " + metadataType);
                case 3:
                    this.delegateFormat = EMSG_FORMAT;
                    break;
            }
            this.buffer = new byte[0];
            this.bufferPosition = 0;
        }

        @Override // androidx.media3.extractor.TrackOutput
        public void format(Format format) {
            this.format = format;
            this.delegate.format(this.delegateFormat);
        }

        @Override // androidx.media3.extractor.TrackOutput
        public int sampleData(DataReader input, int length, boolean allowEndOfInput, int sampleDataPart) throws IOException {
            ensureBufferCapacity(this.bufferPosition + length);
            int numBytesRead = input.read(this.buffer, this.bufferPosition, length);
            if (numBytesRead == -1) {
                if (allowEndOfInput) {
                    return -1;
                }
                throw new EOFException();
            }
            this.bufferPosition += numBytesRead;
            return numBytesRead;
        }

        @Override // androidx.media3.extractor.TrackOutput
        public void sampleData(ParsableByteArray data, int length, int sampleDataPart) {
            ensureBufferCapacity(this.bufferPosition + length);
            data.readBytes(this.buffer, this.bufferPosition, length);
            this.bufferPosition += length;
        }

        @Override // androidx.media3.extractor.TrackOutput
        public void sampleMetadata(long timeUs, int flags, int size, int offset, TrackOutput.CryptoData cryptoData) {
            ParsableByteArray sampleForDelegate;
            Assertions.checkNotNull(this.format);
            ParsableByteArray sample = getSampleAndTrimBuffer(size, offset);
            if (Util.areEqual(this.format.sampleMimeType, this.delegateFormat.sampleMimeType)) {
                sampleForDelegate = sample;
            } else if (MimeTypes.APPLICATION_EMSG.equals(this.format.sampleMimeType)) {
                EventMessage emsg = this.emsgDecoder.decode(sample);
                if (!emsgContainsExpectedWrappedFormat(emsg)) {
                    Log.w(HlsSampleStreamWrapper.TAG, String.format("Ignoring EMSG. Expected it to contain wrapped %s but actual wrapped format: %s", this.delegateFormat.sampleMimeType, emsg.getWrappedMetadataFormat()));
                    return;
                }
                sampleForDelegate = new ParsableByteArray((byte[]) Assertions.checkNotNull(emsg.getWrappedMetadataBytes()));
            } else {
                Log.w(HlsSampleStreamWrapper.TAG, "Ignoring sample for unsupported format: " + this.format.sampleMimeType);
                return;
            }
            int sampleSize = sampleForDelegate.bytesLeft();
            this.delegate.sampleData(sampleForDelegate, sampleSize);
            this.delegate.sampleMetadata(timeUs, flags, sampleSize, 0, cryptoData);
        }

        private boolean emsgContainsExpectedWrappedFormat(EventMessage emsg) {
            Format wrappedMetadataFormat = emsg.getWrappedMetadataFormat();
            return wrappedMetadataFormat != null && Util.areEqual(this.delegateFormat.sampleMimeType, wrappedMetadataFormat.sampleMimeType);
        }

        private void ensureBufferCapacity(int requiredLength) {
            if (this.buffer.length < requiredLength) {
                this.buffer = Arrays.copyOf(this.buffer, (requiredLength / 2) + requiredLength);
            }
        }

        private ParsableByteArray getSampleAndTrimBuffer(int size, int offset) {
            int sampleEnd = this.bufferPosition - offset;
            int sampleStart = sampleEnd - size;
            byte[] sampleBytes = Arrays.copyOfRange(this.buffer, sampleStart, sampleEnd);
            ParsableByteArray sample = new ParsableByteArray(sampleBytes);
            System.arraycopy(this.buffer, sampleEnd, this.buffer, 0, offset);
            this.bufferPosition = offset;
            return sample;
        }
    }
}
