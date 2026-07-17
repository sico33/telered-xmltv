package androidx.media3.exoplayer.dash;

import android.net.Uri;
import android.os.SystemClock;
import android.util.Pair;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.UriUtil;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.datasource.TransferListener;
import androidx.media3.exoplayer.LoadingInfo;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.dash.manifest.AdaptationSet;
import androidx.media3.exoplayer.dash.manifest.BaseUrl;
import androidx.media3.exoplayer.dash.manifest.DashManifest;
import androidx.media3.exoplayer.dash.manifest.RangedUri;
import androidx.media3.exoplayer.dash.manifest.Representation;
import androidx.media3.exoplayer.source.BehindLiveWindowException;
import androidx.media3.exoplayer.source.chunk.BaseMediaChunkIterator;
import androidx.media3.exoplayer.source.chunk.BundledChunkExtractor;
import androidx.media3.exoplayer.source.chunk.Chunk;
import androidx.media3.exoplayer.source.chunk.ChunkExtractor;
import androidx.media3.exoplayer.source.chunk.ChunkHolder;
import androidx.media3.exoplayer.source.chunk.ContainerMediaChunk;
import androidx.media3.exoplayer.source.chunk.InitializationChunk;
import androidx.media3.exoplayer.source.chunk.MediaChunk;
import androidx.media3.exoplayer.source.chunk.MediaChunkIterator;
import androidx.media3.exoplayer.source.chunk.SingleSampleMediaChunk;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.upstream.CmcdConfiguration;
import androidx.media3.exoplayer.upstream.CmcdData;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import androidx.media3.exoplayer.upstream.LoaderErrorThrower;
import androidx.media3.extractor.ChunkIndex;
import androidx.media3.extractor.text.SubtitleParser;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public class DefaultDashChunkSource implements DashChunkSource {
    private final int[] adaptationSetIndices;
    private final BaseUrlExclusionList baseUrlExclusionList;
    private final CmcdConfiguration cmcdConfiguration;
    private final DataSource dataSource;
    private final long elapsedRealtimeOffsetMs;
    private IOException fatalError;
    private long lastChunkRequestRealtimeMs;
    private DashManifest manifest;
    private final LoaderErrorThrower manifestLoaderErrorThrower;
    private final int maxSegmentsPerLoad;
    private boolean missingLastSegment;
    private int periodIndex;
    private final PlayerEmsgHandler.PlayerTrackEmsgHandler playerTrackEmsgHandler;
    protected final RepresentationHolder[] representationHolders;
    private ExoTrackSelection trackSelection;
    private final int trackType;

    public static final class Factory implements DashChunkSource.Factory {
        private final ChunkExtractor.Factory chunkExtractorFactory;
        private final DataSource.Factory dataSourceFactory;
        private final int maxSegmentsPerLoad;

        public Factory(DataSource.Factory dataSourceFactory) {
            this(dataSourceFactory, 1);
        }

        public Factory(DataSource.Factory dataSourceFactory, int maxSegmentsPerLoad) {
            this(BundledChunkExtractor.FACTORY, dataSourceFactory, maxSegmentsPerLoad);
        }

        public Factory(ChunkExtractor.Factory chunkExtractorFactory, DataSource.Factory dataSourceFactory, int maxSegmentsPerLoad) {
            this.chunkExtractorFactory = chunkExtractorFactory;
            this.dataSourceFactory = dataSourceFactory;
            this.maxSegmentsPerLoad = maxSegmentsPerLoad;
        }

        @Override // androidx.media3.exoplayer.dash.DashChunkSource.Factory
        public Factory setSubtitleParserFactory(SubtitleParser.Factory subtitleParserFactory) {
            this.chunkExtractorFactory.setSubtitleParserFactory(subtitleParserFactory);
            return this;
        }

        @Override // androidx.media3.exoplayer.dash.DashChunkSource.Factory
        public Factory experimentalParseSubtitlesDuringExtraction(boolean parseSubtitlesDuringExtraction) {
            this.chunkExtractorFactory.experimentalParseSubtitlesDuringExtraction(parseSubtitlesDuringExtraction);
            return this;
        }

        @Override // androidx.media3.exoplayer.dash.DashChunkSource.Factory
        public DashChunkSource createDashChunkSource(LoaderErrorThrower manifestLoaderErrorThrower, DashManifest manifest, BaseUrlExclusionList baseUrlExclusionList, int periodIndex, int[] adaptationSetIndices, ExoTrackSelection trackSelection, int trackType, long elapsedRealtimeOffsetMs, boolean enableEventMessageTrack, List<Format> closedCaptionFormats, PlayerEmsgHandler.PlayerTrackEmsgHandler playerEmsgHandler, TransferListener transferListener, PlayerId playerId, CmcdConfiguration cmcdConfiguration) {
            DataSource dataSource = this.dataSourceFactory.createDataSource();
            if (transferListener != null) {
                dataSource.addTransferListener(transferListener);
            }
            return new DefaultDashChunkSource(this.chunkExtractorFactory, manifestLoaderErrorThrower, manifest, baseUrlExclusionList, periodIndex, adaptationSetIndices, trackSelection, trackType, dataSource, elapsedRealtimeOffsetMs, this.maxSegmentsPerLoad, enableEventMessageTrack, closedCaptionFormats, playerEmsgHandler, playerId, cmcdConfiguration);
        }

        @Override // androidx.media3.exoplayer.dash.DashChunkSource.Factory
        public Format getOutputTextFormat(Format sourceFormat) {
            return this.chunkExtractorFactory.getOutputTextFormat(sourceFormat);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v0, types: [androidx.media3.exoplayer.dash.DefaultDashChunkSource, java.lang.Object] */
    public DefaultDashChunkSource(ChunkExtractor.Factory factory, LoaderErrorThrower loaderErrorThrower, DashManifest dashManifest, BaseUrlExclusionList baseUrlExclusionList, int i, int[] iArr, ExoTrackSelection exoTrackSelection, int i2, DataSource dataSource, long j, int i3, boolean z, List<Format> list, PlayerEmsgHandler.PlayerTrackEmsgHandler playerTrackEmsgHandler, PlayerId playerId, CmcdConfiguration cmcdConfiguration) {
        ?? obj = new Object();
        obj.manifestLoaderErrorThrower = loaderErrorThrower;
        obj.manifest = dashManifest;
        obj.baseUrlExclusionList = baseUrlExclusionList;
        obj.adaptationSetIndices = iArr;
        obj.trackSelection = exoTrackSelection;
        int i4 = i2;
        obj.trackType = i4;
        obj.dataSource = dataSource;
        obj.periodIndex = i;
        obj.elapsedRealtimeOffsetMs = j;
        obj.maxSegmentsPerLoad = i3;
        PlayerEmsgHandler.PlayerTrackEmsgHandler playerTrackEmsgHandler2 = playerTrackEmsgHandler;
        obj.playerTrackEmsgHandler = playerTrackEmsgHandler2;
        obj.cmcdConfiguration = cmcdConfiguration;
        obj.lastChunkRequestRealtimeMs = C.TIME_UNSET;
        long periodDurationUs = dashManifest.getPeriodDurationUs(i);
        ArrayList<Representation> representations = obj.getRepresentations();
        obj.representationHolders = new RepresentationHolder[exoTrackSelection.length()];
        int i5 = 0;
        DefaultDashChunkSource defaultDashChunkSource = obj;
        while (i5 < defaultDashChunkSource.representationHolders.length) {
            Representation representation = representations.get(exoTrackSelection.getIndexInTrackGroup(i5));
            BaseUrl baseUrlSelectBaseUrl = baseUrlExclusionList.selectBaseUrl(representation.baseUrls);
            defaultDashChunkSource.representationHolders[i5] = new RepresentationHolder(periodDurationUs, representation, baseUrlSelectBaseUrl != null ? baseUrlSelectBaseUrl : representation.baseUrls.get(0), factory.createProgressiveMediaExtractor(i4, representation.format, z, list, playerTrackEmsgHandler2, playerId), 0L, representation.getIndex());
            i5++;
            defaultDashChunkSource = this;
            i4 = i2;
            playerTrackEmsgHandler2 = playerTrackEmsgHandler;
        }
    }

    @Override // androidx.media3.exoplayer.source.chunk.ChunkSource
    public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
        long secondSyncUs;
        for (RepresentationHolder representationHolder : this.representationHolders) {
            if (representationHolder.segmentIndex != null) {
                long segmentCount = representationHolder.getSegmentCount();
                if (segmentCount != 0) {
                    long segmentNum = representationHolder.getSegmentNum(positionUs);
                    long firstSyncUs = representationHolder.getSegmentStartTimeUs(segmentNum);
                    if (firstSyncUs < positionUs && (segmentCount == -1 || segmentNum < (representationHolder.getFirstSegmentNum() + segmentCount) - 1)) {
                        secondSyncUs = representationHolder.getSegmentStartTimeUs(1 + segmentNum);
                    } else {
                        secondSyncUs = firstSyncUs;
                    }
                    return seekParameters.resolveSeekPositionUs(positionUs, firstSyncUs, secondSyncUs);
                }
            }
        }
        return positionUs;
    }

    @Override // androidx.media3.exoplayer.dash.DashChunkSource
    public void updateManifest(DashManifest newManifest, int newPeriodIndex) {
        try {
            this.manifest = newManifest;
            this.periodIndex = newPeriodIndex;
            long periodDurationUs = this.manifest.getPeriodDurationUs(this.periodIndex);
            List<Representation> representations = getRepresentations();
            for (int i = 0; i < this.representationHolders.length; i++) {
                Representation representation = representations.get(this.trackSelection.getIndexInTrackGroup(i));
                this.representationHolders[i] = this.representationHolders[i].copyWithNewRepresentation(periodDurationUs, representation);
            }
        } catch (BehindLiveWindowException e) {
            this.fatalError = e;
        }
    }

    @Override // androidx.media3.exoplayer.dash.DashChunkSource
    public void updateTrackSelection(ExoTrackSelection trackSelection) {
        this.trackSelection = trackSelection;
    }

    @Override // androidx.media3.exoplayer.source.chunk.ChunkSource
    public void maybeThrowError() throws IOException {
        if (this.fatalError != null) {
            throw this.fatalError;
        }
        this.manifestLoaderErrorThrower.maybeThrowError();
    }

    @Override // androidx.media3.exoplayer.source.chunk.ChunkSource
    public int getPreferredQueueSize(long playbackPositionUs, List<? extends MediaChunk> queue) {
        if (this.fatalError != null || this.trackSelection.length() < 2) {
            return queue.size();
        }
        return this.trackSelection.evaluateQueueSize(playbackPositionUs, queue);
    }

    @Override // androidx.media3.exoplayer.source.chunk.ChunkSource
    public boolean shouldCancelLoad(long playbackPositionUs, Chunk loadingChunk, List<? extends MediaChunk> queue) {
        if (this.fatalError != null) {
            return false;
        }
        return this.trackSelection.shouldCancelChunkLoad(playbackPositionUs, loadingChunk, queue);
    }

    @Override // androidx.media3.exoplayer.source.chunk.ChunkSource
    public void getNextChunk(LoadingInfo loadingInfo, long loadPositionUs, List<? extends MediaChunk> queue, ChunkHolder out) {
        RangedUri pendingInitializationUri;
        RangedUri pendingIndexUri;
        long nowUnixTimeUs;
        long presentationPositionUs;
        MediaChunkIterator[] chunkIterators;
        boolean z;
        int i;
        MediaChunk previous;
        if (this.fatalError != null) {
            return;
        }
        long playbackPositionUs = loadingInfo.playbackPositionUs;
        long bufferedDurationUs = loadPositionUs - playbackPositionUs;
        long presentationPositionUs2 = Util.msToUs(this.manifest.availabilityStartTimeMs) + Util.msToUs(this.manifest.getPeriod(this.periodIndex).startMs) + loadPositionUs;
        if (this.playerTrackEmsgHandler != null && this.playerTrackEmsgHandler.maybeRefreshManifestBeforeLoadingNextChunk(presentationPositionUs2)) {
            return;
        }
        long nowUnixTimeUs2 = Util.msToUs(Util.getNowUnixTimeMs(this.elapsedRealtimeOffsetMs));
        long nowPeriodTimeUs = getNowPeriodTimeUs(nowUnixTimeUs2);
        boolean z2 = true;
        MediaChunk previous2 = queue.isEmpty() ? null : queue.get(queue.size() - 1);
        MediaChunkIterator[] chunkIterators2 = new MediaChunkIterator[this.trackSelection.length()];
        int i2 = 0;
        while (i2 < chunkIterators2.length) {
            RepresentationHolder representationHolder = this.representationHolders[i2];
            if (representationHolder.segmentIndex == null) {
                chunkIterators2[i2] = MediaChunkIterator.EMPTY;
                nowUnixTimeUs = nowUnixTimeUs2;
                chunkIterators = chunkIterators2;
                presentationPositionUs = presentationPositionUs2;
                z = true;
                previous = previous2;
                i = i2;
            } else {
                long firstAvailableSegmentNum = representationHolder.getFirstAvailableSegmentNum(nowUnixTimeUs2);
                long lastAvailableSegmentNum = representationHolder.getLastAvailableSegmentNum(nowUnixTimeUs2);
                nowUnixTimeUs = nowUnixTimeUs2;
                MediaChunk previous3 = previous2;
                presentationPositionUs = presentationPositionUs2;
                chunkIterators = chunkIterators2;
                z = true;
                i = i2;
                long segmentNum = getSegmentNum(representationHolder, previous3, loadPositionUs, firstAvailableSegmentNum, lastAvailableSegmentNum);
                previous = previous3;
                if (segmentNum < firstAvailableSegmentNum) {
                    chunkIterators[i] = MediaChunkIterator.EMPTY;
                } else {
                    chunkIterators[i] = new RepresentationSegmentIterator(updateSelectedBaseUrl(i), segmentNum, lastAvailableSegmentNum, nowPeriodTimeUs);
                }
            }
            i2 = i + 1;
            previous2 = previous;
            nowUnixTimeUs2 = nowUnixTimeUs;
            chunkIterators2 = chunkIterators;
            z2 = z;
            presentationPositionUs2 = presentationPositionUs;
        }
        long nowUnixTimeUs3 = nowUnixTimeUs2;
        boolean z3 = z2;
        MediaChunk previous4 = previous2;
        long availableLiveDurationUs = getAvailableLiveDurationUs(nowUnixTimeUs3, playbackPositionUs);
        this.trackSelection.updateSelectedTrack(playbackPositionUs, bufferedDurationUs, availableLiveDurationUs, queue, chunkIterators2);
        int selectedTrackIndex = this.trackSelection.getSelectedIndex();
        CmcdData.Factory cmcdDataFactory = this.cmcdConfiguration == null ? null : new CmcdData.Factory(this.cmcdConfiguration, this.trackSelection, Math.max(0L, bufferedDurationUs), loadingInfo.playbackSpeed, "d", this.manifest.dynamic, loadingInfo.rebufferedSince(this.lastChunkRequestRealtimeMs), queue.isEmpty());
        this.lastChunkRequestRealtimeMs = SystemClock.elapsedRealtime();
        RepresentationHolder representationHolder2 = updateSelectedBaseUrl(selectedTrackIndex);
        if (representationHolder2.chunkExtractor != null) {
            Representation selectedRepresentation = representationHolder2.representation;
            if (representationHolder2.chunkExtractor.getSampleFormats() != null) {
                pendingInitializationUri = null;
            } else {
                RangedUri pendingInitializationUri2 = selectedRepresentation.getInitializationUri();
                pendingInitializationUri = pendingInitializationUri2;
            }
            if (representationHolder2.segmentIndex != null) {
                pendingIndexUri = null;
            } else {
                RangedUri pendingIndexUri2 = selectedRepresentation.getIndexUri();
                pendingIndexUri = pendingIndexUri2;
            }
            if (pendingInitializationUri != null || pendingIndexUri != null) {
                out.chunk = newInitializationChunk(representationHolder2, this.dataSource, this.trackSelection.getSelectedFormat(), this.trackSelection.getSelectionReason(), this.trackSelection.getSelectionData(), pendingInitializationUri, pendingIndexUri, cmcdDataFactory);
                return;
            }
        }
        long periodDurationUs = representationHolder2.periodDurationUs;
        boolean isLastPeriodInDynamicManifest = (this.manifest.dynamic && this.periodIndex == this.manifest.getPeriodCount() + (-1)) ? z3 : false;
        long seekTimeUs = C.TIME_UNSET;
        boolean periodEnded = (isLastPeriodInDynamicManifest && periodDurationUs == C.TIME_UNSET) ? false : z3;
        if (representationHolder2.getSegmentCount() == 0) {
            out.endOfStream = periodEnded;
            return;
        }
        long firstAvailableSegmentNum2 = representationHolder2.getFirstAvailableSegmentNum(nowUnixTimeUs3);
        long lastAvailableSegmentNum2 = representationHolder2.getLastAvailableSegmentNum(nowUnixTimeUs3);
        if (isLastPeriodInDynamicManifest) {
            long lastAvailableSegmentEndTimeUs = representationHolder2.getSegmentEndTimeUs(lastAvailableSegmentNum2);
            long lastSegmentDurationUs = lastAvailableSegmentEndTimeUs - representationHolder2.getSegmentStartTimeUs(lastAvailableSegmentNum2);
            periodEnded &= lastAvailableSegmentEndTimeUs + lastSegmentDurationUs >= periodDurationUs ? z3 : false;
        }
        boolean periodEnded2 = periodEnded;
        long segmentNum2 = getSegmentNum(representationHolder2, previous4, loadPositionUs, firstAvailableSegmentNum2, lastAvailableSegmentNum2);
        if (segmentNum2 < firstAvailableSegmentNum2) {
            this.fatalError = new BehindLiveWindowException();
            return;
        }
        if (segmentNum2 > lastAvailableSegmentNum2 || (this.missingLastSegment && segmentNum2 >= lastAvailableSegmentNum2)) {
            out.endOfStream = periodEnded2;
            return;
        }
        if (periodEnded2 && representationHolder2.getSegmentStartTimeUs(segmentNum2) >= periodDurationUs) {
            out.endOfStream = z3;
            return;
        }
        int maxSegmentCount = (int) Math.min(this.maxSegmentsPerLoad, (lastAvailableSegmentNum2 - segmentNum2) + 1);
        if (periodDurationUs != C.TIME_UNSET) {
            while (maxSegmentCount > 1 && representationHolder2.getSegmentStartTimeUs((((long) maxSegmentCount) + segmentNum2) - 1) >= periodDurationUs) {
                maxSegmentCount--;
            }
        }
        if (queue.isEmpty()) {
            seekTimeUs = loadPositionUs;
        }
        out.chunk = newMediaChunk(representationHolder2, this.dataSource, this.trackType, this.trackSelection.getSelectedFormat(), this.trackSelection.getSelectionReason(), this.trackSelection.getSelectionData(), segmentNum2, maxSegmentCount, seekTimeUs, nowPeriodTimeUs, cmcdDataFactory);
    }

    @Override // androidx.media3.exoplayer.source.chunk.ChunkSource
    public void onChunkLoadCompleted(Chunk chunk) {
        ChunkIndex chunkIndex;
        if (chunk instanceof InitializationChunk) {
            InitializationChunk initializationChunk = (InitializationChunk) chunk;
            int trackIndex = this.trackSelection.indexOf(initializationChunk.trackFormat);
            RepresentationHolder representationHolder = this.representationHolders[trackIndex];
            if (representationHolder.segmentIndex == null && (chunkIndex = ((ChunkExtractor) Assertions.checkStateNotNull(representationHolder.chunkExtractor)).getChunkIndex()) != null) {
                this.representationHolders[trackIndex] = representationHolder.copyWithNewSegmentIndex(new DashWrappingSegmentIndex(chunkIndex, representationHolder.representation.presentationTimeOffsetUs));
            }
        }
        if (this.playerTrackEmsgHandler != null) {
            this.playerTrackEmsgHandler.onChunkLoadCompleted(chunk);
        }
    }

    @Override // androidx.media3.exoplayer.source.chunk.ChunkSource
    public boolean onChunkLoadError(Chunk chunk, boolean cancelable, LoadErrorHandlingPolicy.LoadErrorInfo loadErrorInfo, LoadErrorHandlingPolicy loadErrorHandlingPolicy) {
        LoadErrorHandlingPolicy.FallbackSelection fallbackSelection;
        if (!cancelable) {
            return false;
        }
        if (this.playerTrackEmsgHandler != null && this.playerTrackEmsgHandler.onChunkLoadError(chunk)) {
            return true;
        }
        if (!this.manifest.dynamic && (chunk instanceof MediaChunk) && (loadErrorInfo.exception instanceof HttpDataSource.InvalidResponseCodeException) && ((HttpDataSource.InvalidResponseCodeException) loadErrorInfo.exception).responseCode == 404) {
            RepresentationHolder representationHolder = this.representationHolders[this.trackSelection.indexOf(chunk.trackFormat)];
            long segmentCount = representationHolder.getSegmentCount();
            if (segmentCount != -1 && segmentCount != 0) {
                long lastAvailableSegmentNum = (representationHolder.getFirstSegmentNum() + segmentCount) - 1;
                if (((MediaChunk) chunk).getNextChunkIndex() > lastAvailableSegmentNum) {
                    this.missingLastSegment = true;
                    return true;
                }
            }
        }
        int trackIndex = this.trackSelection.indexOf(chunk.trackFormat);
        RepresentationHolder representationHolder2 = this.representationHolders[trackIndex];
        BaseUrl newBaseUrl = this.baseUrlExclusionList.selectBaseUrl(representationHolder2.representation.baseUrls);
        if (newBaseUrl != null && !representationHolder2.selectedBaseUrl.equals(newBaseUrl)) {
            return true;
        }
        LoadErrorHandlingPolicy.FallbackOptions fallbackOptions = createFallbackOptions(this.trackSelection, representationHolder2.representation.baseUrls);
        if ((!fallbackOptions.isFallbackAvailable(2) && !fallbackOptions.isFallbackAvailable(1)) || (fallbackSelection = loadErrorHandlingPolicy.getFallbackSelectionFor(fallbackOptions, loadErrorInfo)) == null || !fallbackOptions.isFallbackAvailable(fallbackSelection.type)) {
            return false;
        }
        if (fallbackSelection.type == 2) {
            boolean cancelLoad = this.trackSelection.excludeTrack(this.trackSelection.indexOf(chunk.trackFormat), fallbackSelection.exclusionDurationMs);
            return cancelLoad;
        }
        if (fallbackSelection.type != 1) {
            return false;
        }
        this.baseUrlExclusionList.exclude(representationHolder2.selectedBaseUrl, fallbackSelection.exclusionDurationMs);
        return true;
    }

    @Override // androidx.media3.exoplayer.source.chunk.ChunkSource
    public void release() {
        for (RepresentationHolder representationHolder : this.representationHolders) {
            ChunkExtractor chunkExtractor = representationHolder.chunkExtractor;
            if (chunkExtractor != null) {
                chunkExtractor.release();
            }
        }
    }

    private LoadErrorHandlingPolicy.FallbackOptions createFallbackOptions(ExoTrackSelection trackSelection, List<BaseUrl> baseUrls) {
        long nowMs = SystemClock.elapsedRealtime();
        int numberOfTracks = trackSelection.length();
        int numberOfExcludedTracks = 0;
        for (int i = 0; i < numberOfTracks; i++) {
            if (trackSelection.isTrackExcluded(i, nowMs)) {
                numberOfExcludedTracks++;
            }
        }
        int priorityCount = BaseUrlExclusionList.getPriorityCount(baseUrls);
        return new LoadErrorHandlingPolicy.FallbackOptions(priorityCount, priorityCount - this.baseUrlExclusionList.getPriorityCountAfterExclusion(baseUrls), numberOfTracks, numberOfExcludedTracks);
    }

    private long getSegmentNum(RepresentationHolder representationHolder, MediaChunk previousChunk, long loadPositionUs, long firstAvailableSegmentNum, long lastAvailableSegmentNum) {
        if (previousChunk != null) {
            return previousChunk.getNextChunkIndex();
        }
        return Util.constrainValue(representationHolder.getSegmentNum(loadPositionUs), firstAvailableSegmentNum, lastAvailableSegmentNum);
    }

    @RequiresNonNull({"manifest", "adaptationSetIndices"})
    private ArrayList<Representation> getRepresentations() {
        List<AdaptationSet> manifestAdaptationSets = this.manifest.getPeriod(this.periodIndex).adaptationSets;
        ArrayList<Representation> representations = new ArrayList<>();
        for (int adaptationSetIndex : this.adaptationSetIndices) {
            representations.addAll(manifestAdaptationSets.get(adaptationSetIndex).representations);
        }
        return representations;
    }

    private long getAvailableLiveDurationUs(long nowUnixTimeUs, long playbackPositionUs) {
        if (this.manifest.dynamic && this.representationHolders[0].getSegmentCount() != 0) {
            long lastSegmentNum = this.representationHolders[0].getLastAvailableSegmentNum(nowUnixTimeUs);
            long lastSegmentEndTimeUs = this.representationHolders[0].getSegmentEndTimeUs(lastSegmentNum);
            long nowPeriodTimeUs = getNowPeriodTimeUs(nowUnixTimeUs);
            long availabilityEndTimeUs = Math.min(nowPeriodTimeUs, lastSegmentEndTimeUs);
            return Math.max(0L, availabilityEndTimeUs - playbackPositionUs);
        }
        return C.TIME_UNSET;
    }

    private long getNowPeriodTimeUs(long nowUnixTimeUs) {
        return this.manifest.availabilityStartTimeMs == C.TIME_UNSET ? C.TIME_UNSET : nowUnixTimeUs - Util.msToUs(this.manifest.availabilityStartTimeMs + this.manifest.getPeriod(this.periodIndex).startMs);
    }

    @RequiresNonNull({"#1.chunkExtractor"})
    protected Chunk newInitializationChunk(RepresentationHolder representationHolder, DataSource dataSource, Format trackFormat, int trackSelectionReason, Object trackSelectionData, RangedUri initializationUri, RangedUri indexUri, CmcdData.Factory cmcdDataFactory) {
        RangedUri requestUri;
        DataSpec dataSpec;
        Representation representation = representationHolder.representation;
        if (initializationUri != null) {
            requestUri = initializationUri.attemptMerge(indexUri, representationHolder.selectedBaseUrl.url);
            if (requestUri == null) {
                requestUri = initializationUri;
            }
        } else {
            requestUri = (RangedUri) Assertions.checkNotNull(indexUri);
        }
        DataSpec dataSpec2 = DashUtil.buildDataSpec(representation, representationHolder.selectedBaseUrl.url, requestUri, 0, ImmutableMap.of());
        if (cmcdDataFactory == null) {
            dataSpec = dataSpec2;
        } else {
            CmcdData cmcdData = cmcdDataFactory.setObjectType(CmcdData.Factory.OBJECT_TYPE_INIT_SEGMENT).createCmcdData();
            dataSpec = cmcdData.addToDataSpec(dataSpec2);
        }
        return new InitializationChunk(dataSource, dataSpec, trackFormat, trackSelectionReason, trackSelectionData, representationHolder.chunkExtractor);
    }

    protected Chunk newMediaChunk(RepresentationHolder representationHolder, DataSource dataSource, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long firstSegmentNum, int maxSegmentCount, long seekTimeUs, long nowPeriodTimeUs, CmcdData.Factory cmcdDataFactory) {
        int segmentCount;
        DataSpec dataSpec;
        Representation representation = representationHolder.representation;
        long startTimeUs = representationHolder.getSegmentStartTimeUs(firstSegmentNum);
        RangedUri segmentUri = representationHolder.getSegmentUrl(firstSegmentNum);
        if (representationHolder.chunkExtractor == null) {
            int flags = 0;
            long endTimeUs = representationHolder.getSegmentEndTimeUs(firstSegmentNum);
            if (!representationHolder.isSegmentAvailableAtFullNetworkSpeed(firstSegmentNum, nowPeriodTimeUs)) {
                flags = 8;
            }
            DataSpec dataSpec2 = DashUtil.buildDataSpec(representation, representationHolder.selectedBaseUrl.url, segmentUri, flags, ImmutableMap.of());
            if (cmcdDataFactory != null) {
                cmcdDataFactory.setChunkDurationUs(endTimeUs - startTimeUs).setObjectType(CmcdData.Factory.getObjectType(this.trackSelection));
                Pair<String, String> nextObjectAndRangeRequest = getNextObjectAndRangeRequest(firstSegmentNum, segmentUri, representationHolder);
                if (nextObjectAndRangeRequest != null) {
                    cmcdDataFactory.setNextObjectRequest((String) nextObjectAndRangeRequest.first).setNextRangeRequest((String) nextObjectAndRangeRequest.second);
                }
                CmcdData cmcdData = cmcdDataFactory.createCmcdData();
                dataSpec2 = cmcdData.addToDataSpec(dataSpec2);
            }
            return new SingleSampleMediaChunk(dataSource, dataSpec2, trackFormat, trackSelectionReason, trackSelectionData, startTimeUs, endTimeUs, firstSegmentNum, trackType, trackFormat);
        }
        int segmentCount2 = 1;
        RangedUri segmentUri2 = segmentUri;
        for (int i = 1; i < maxSegmentCount; i++) {
            RangedUri nextSegmentUri = representationHolder.getSegmentUrl(((long) i) + firstSegmentNum);
            RangedUri mergedSegmentUri = segmentUri2.attemptMerge(nextSegmentUri, representationHolder.selectedBaseUrl.url);
            if (mergedSegmentUri == null) {
                break;
            }
            segmentUri2 = mergedSegmentUri;
            segmentCount2++;
        }
        long segmentNum = (((long) segmentCount2) + firstSegmentNum) - 1;
        long endTimeUs2 = representationHolder.getSegmentEndTimeUs(segmentNum);
        long periodDurationUs = representationHolder.periodDurationUs;
        long clippedEndTimeUs = C.TIME_UNSET;
        if (periodDurationUs != C.TIME_UNSET && periodDurationUs <= endTimeUs2) {
            clippedEndTimeUs = periodDurationUs;
        }
        int segmentCount3 = segmentCount2;
        int flags2 = representationHolder.isSegmentAvailableAtFullNetworkSpeed(segmentNum, nowPeriodTimeUs) ? 0 : 8;
        DataSpec dataSpec3 = DashUtil.buildDataSpec(representation, representationHolder.selectedBaseUrl.url, segmentUri2, flags2, ImmutableMap.of());
        if (cmcdDataFactory != null) {
            segmentCount = segmentCount3;
            cmcdDataFactory.setChunkDurationUs(endTimeUs2 - startTimeUs).setObjectType(CmcdData.Factory.getObjectType(this.trackSelection));
            Pair<String, String> nextObjectAndRangeRequest2 = getNextObjectAndRangeRequest(firstSegmentNum, segmentUri2, representationHolder);
            if (nextObjectAndRangeRequest2 != null) {
                cmcdDataFactory.setNextObjectRequest((String) nextObjectAndRangeRequest2.first).setNextRangeRequest((String) nextObjectAndRangeRequest2.second);
            }
            CmcdData cmcdData2 = cmcdDataFactory.createCmcdData();
            dataSpec = cmcdData2.addToDataSpec(dataSpec3);
        } else {
            segmentUri2 = segmentUri2;
            segmentCount = segmentCount3;
            dataSpec = dataSpec3;
        }
        long sampleOffsetUs = -representation.presentationTimeOffsetUs;
        if (MimeTypes.isImage(trackFormat.sampleMimeType)) {
            sampleOffsetUs += startTimeUs;
        }
        return new ContainerMediaChunk(dataSource, dataSpec, trackFormat, trackSelectionReason, trackSelectionData, startTimeUs, endTimeUs2, seekTimeUs, clippedEndTimeUs, firstSegmentNum, segmentCount, sampleOffsetUs, representationHolder.chunkExtractor);
    }

    private Pair<String, String> getNextObjectAndRangeRequest(long segmentNum, RangedUri segmentUri, RepresentationHolder representationHolder) {
        if (segmentNum + 1 >= representationHolder.getSegmentCount()) {
            return null;
        }
        RangedUri nextSegmentUri = representationHolder.getSegmentUrl(1 + segmentNum);
        Uri uri = segmentUri.resolveUri(representationHolder.selectedBaseUrl.url);
        Uri nextUri = nextSegmentUri.resolveUri(representationHolder.selectedBaseUrl.url);
        String nextObjectRequest = UriUtil.getRelativePath(uri, nextUri);
        String nextRangeRequest = nextSegmentUri.start + "-";
        if (nextSegmentUri.length != -1) {
            nextRangeRequest = nextRangeRequest + (nextSegmentUri.start + nextSegmentUri.length);
        }
        return new Pair<>(nextObjectRequest, nextRangeRequest);
    }

    private RepresentationHolder updateSelectedBaseUrl(int trackIndex) {
        RepresentationHolder representationHolder = this.representationHolders[trackIndex];
        BaseUrl selectedBaseUrl = this.baseUrlExclusionList.selectBaseUrl(representationHolder.representation.baseUrls);
        if (selectedBaseUrl != null && !selectedBaseUrl.equals(representationHolder.selectedBaseUrl)) {
            RepresentationHolder representationHolder2 = representationHolder.copyWithNewSelectedBaseUrl(selectedBaseUrl);
            this.representationHolders[trackIndex] = representationHolder2;
            return representationHolder2;
        }
        return representationHolder;
    }

    protected static final class RepresentationSegmentIterator extends BaseMediaChunkIterator {
        private final long nowPeriodTimeUs;
        private final RepresentationHolder representationHolder;

        public RepresentationSegmentIterator(RepresentationHolder representation, long firstAvailableSegmentNum, long lastAvailableSegmentNum, long nowPeriodTimeUs) {
            super(firstAvailableSegmentNum, lastAvailableSegmentNum);
            this.representationHolder = representation;
            this.nowPeriodTimeUs = nowPeriodTimeUs;
        }

        @Override // androidx.media3.exoplayer.source.chunk.MediaChunkIterator
        public DataSpec getDataSpec() {
            int flags;
            checkInBounds();
            long currentIndex = getCurrentIndex();
            RangedUri segmentUri = this.representationHolder.getSegmentUrl(currentIndex);
            if (this.representationHolder.isSegmentAvailableAtFullNetworkSpeed(currentIndex, this.nowPeriodTimeUs)) {
                flags = 0;
            } else {
                flags = 8;
            }
            return DashUtil.buildDataSpec(this.representationHolder.representation, this.representationHolder.selectedBaseUrl.url, segmentUri, flags, ImmutableMap.of());
        }

        @Override // androidx.media3.exoplayer.source.chunk.MediaChunkIterator
        public long getChunkStartTimeUs() {
            checkInBounds();
            return this.representationHolder.getSegmentStartTimeUs(getCurrentIndex());
        }

        @Override // androidx.media3.exoplayer.source.chunk.MediaChunkIterator
        public long getChunkEndTimeUs() {
            checkInBounds();
            return this.representationHolder.getSegmentEndTimeUs(getCurrentIndex());
        }
    }

    protected static final class RepresentationHolder {
        final ChunkExtractor chunkExtractor;
        private final long periodDurationUs;
        public final Representation representation;
        public final DashSegmentIndex segmentIndex;
        private final long segmentNumShift;
        public final BaseUrl selectedBaseUrl;

        RepresentationHolder(long periodDurationUs, Representation representation, BaseUrl selectedBaseUrl, ChunkExtractor chunkExtractor, long segmentNumShift, DashSegmentIndex segmentIndex) {
            this.periodDurationUs = periodDurationUs;
            this.representation = representation;
            this.selectedBaseUrl = selectedBaseUrl;
            this.segmentNumShift = segmentNumShift;
            this.chunkExtractor = chunkExtractor;
            this.segmentIndex = segmentIndex;
        }

        RepresentationHolder copyWithNewRepresentation(long newPeriodDurationUs, Representation newRepresentation) throws BehindLiveWindowException {
            long newSegmentNumShift;
            DashSegmentIndex oldIndex = this.representation.getIndex();
            DashSegmentIndex newIndex = newRepresentation.getIndex();
            if (oldIndex == null) {
                return new RepresentationHolder(newPeriodDurationUs, newRepresentation, this.selectedBaseUrl, this.chunkExtractor, this.segmentNumShift, oldIndex);
            }
            if (oldIndex.isExplicit()) {
                long oldIndexSegmentCount = oldIndex.getSegmentCount(newPeriodDurationUs);
                if (oldIndexSegmentCount == 0) {
                    return new RepresentationHolder(newPeriodDurationUs, newRepresentation, this.selectedBaseUrl, this.chunkExtractor, this.segmentNumShift, newIndex);
                }
                Assertions.checkStateNotNull(newIndex);
                long oldIndexFirstSegmentNum = oldIndex.getFirstSegmentNum();
                long oldIndexStartTimeUs = oldIndex.getTimeUs(oldIndexFirstSegmentNum);
                long oldIndexLastSegmentNum = (oldIndexFirstSegmentNum + oldIndexSegmentCount) - 1;
                long oldIndexEndTimeUs = oldIndex.getTimeUs(oldIndexLastSegmentNum) + oldIndex.getDurationUs(oldIndexLastSegmentNum, newPeriodDurationUs);
                long newIndexFirstSegmentNum = newIndex.getFirstSegmentNum();
                long newIndexStartTimeUs = newIndex.getTimeUs(newIndexFirstSegmentNum);
                long oldIndexLastSegmentNum2 = this.segmentNumShift;
                if (oldIndexEndTimeUs == newIndexStartTimeUs) {
                    long newSegmentNumShift2 = oldIndexLastSegmentNum2 + ((oldIndexLastSegmentNum + 1) - newIndexFirstSegmentNum);
                    newSegmentNumShift = newSegmentNumShift2;
                } else {
                    if (oldIndexEndTimeUs < newIndexStartTimeUs) {
                        throw new BehindLiveWindowException();
                    }
                    if (newIndexStartTimeUs < oldIndexStartTimeUs) {
                        long newSegmentNumShift3 = oldIndexLastSegmentNum2 - (newIndex.getSegmentNum(oldIndexStartTimeUs, newPeriodDurationUs) - oldIndexFirstSegmentNum);
                        newSegmentNumShift = newSegmentNumShift3;
                    } else {
                        long newSegmentNumShift4 = oldIndexLastSegmentNum2 + (oldIndex.getSegmentNum(newIndexStartTimeUs, newPeriodDurationUs) - newIndexFirstSegmentNum);
                        newSegmentNumShift = newSegmentNumShift4;
                    }
                }
                return new RepresentationHolder(newPeriodDurationUs, newRepresentation, this.selectedBaseUrl, this.chunkExtractor, newSegmentNumShift, newIndex);
            }
            return new RepresentationHolder(newPeriodDurationUs, newRepresentation, this.selectedBaseUrl, this.chunkExtractor, this.segmentNumShift, newIndex);
        }

        RepresentationHolder copyWithNewSegmentIndex(DashSegmentIndex segmentIndex) {
            return new RepresentationHolder(this.periodDurationUs, this.representation, this.selectedBaseUrl, this.chunkExtractor, this.segmentNumShift, segmentIndex);
        }

        RepresentationHolder copyWithNewSelectedBaseUrl(BaseUrl selectedBaseUrl) {
            return new RepresentationHolder(this.periodDurationUs, this.representation, selectedBaseUrl, this.chunkExtractor, this.segmentNumShift, this.segmentIndex);
        }

        public long getFirstSegmentNum() {
            return ((DashSegmentIndex) Assertions.checkStateNotNull(this.segmentIndex)).getFirstSegmentNum() + this.segmentNumShift;
        }

        public long getFirstAvailableSegmentNum(long nowUnixTimeUs) {
            return ((DashSegmentIndex) Assertions.checkStateNotNull(this.segmentIndex)).getFirstAvailableSegmentNum(this.periodDurationUs, nowUnixTimeUs) + this.segmentNumShift;
        }

        public long getSegmentCount() {
            return ((DashSegmentIndex) Assertions.checkStateNotNull(this.segmentIndex)).getSegmentCount(this.periodDurationUs);
        }

        public long getSegmentStartTimeUs(long segmentNum) {
            return ((DashSegmentIndex) Assertions.checkStateNotNull(this.segmentIndex)).getTimeUs(segmentNum - this.segmentNumShift);
        }

        public long getSegmentEndTimeUs(long segmentNum) {
            return getSegmentStartTimeUs(segmentNum) + ((DashSegmentIndex) Assertions.checkStateNotNull(this.segmentIndex)).getDurationUs(segmentNum - this.segmentNumShift, this.periodDurationUs);
        }

        public long getSegmentNum(long positionUs) {
            return ((DashSegmentIndex) Assertions.checkStateNotNull(this.segmentIndex)).getSegmentNum(positionUs, this.periodDurationUs) + this.segmentNumShift;
        }

        public RangedUri getSegmentUrl(long segmentNum) {
            return ((DashSegmentIndex) Assertions.checkStateNotNull(this.segmentIndex)).getSegmentUrl(segmentNum - this.segmentNumShift);
        }

        public long getLastAvailableSegmentNum(long nowUnixTimeUs) {
            return (getFirstAvailableSegmentNum(nowUnixTimeUs) + ((DashSegmentIndex) Assertions.checkStateNotNull(this.segmentIndex)).getAvailableSegmentCount(this.periodDurationUs, nowUnixTimeUs)) - 1;
        }

        public boolean isSegmentAvailableAtFullNetworkSpeed(long segmentNum, long nowPeriodTimeUs) {
            return ((DashSegmentIndex) Assertions.checkStateNotNull(this.segmentIndex)).isExplicit() || nowPeriodTimeUs == C.TIME_UNSET || getSegmentEndTimeUs(segmentNum) <= nowPeriodTimeUs;
        }
    }
}
