package androidx.media3.exoplayer.dash;

import android.util.Pair;
import android.util.SparseArray;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.StreamKey;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.TransferListener;
import androidx.media3.exoplayer.LoadingInfo;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.dash.manifest.AdaptationSet;
import androidx.media3.exoplayer.dash.manifest.DashManifest;
import androidx.media3.exoplayer.dash.manifest.Descriptor;
import androidx.media3.exoplayer.dash.manifest.EventStream;
import androidx.media3.exoplayer.dash.manifest.Period;
import androidx.media3.exoplayer.dash.manifest.Representation;
import androidx.media3.exoplayer.drm.DrmSessionEventListener;
import androidx.media3.exoplayer.drm.DrmSessionManager;
import androidx.media3.exoplayer.source.CompositeSequenceableLoaderFactory;
import androidx.media3.exoplayer.source.EmptySampleStream;
import androidx.media3.exoplayer.source.MediaPeriod;
import androidx.media3.exoplayer.source.MediaSourceEventListener;
import androidx.media3.exoplayer.source.SampleStream;
import androidx.media3.exoplayer.source.SequenceableLoader;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.source.chunk.ChunkSampleStream;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.upstream.Allocator;
import androidx.media3.exoplayer.upstream.CmcdConfiguration;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import androidx.media3.exoplayer.upstream.LoaderErrorThrower;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
final class DashMediaPeriod implements MediaPeriod, SequenceableLoader.Callback<ChunkSampleStream<DashChunkSource>>, ChunkSampleStream.ReleaseCallback<DashChunkSource> {
    private static final Pattern CEA608_SERVICE_DESCRIPTOR_REGEX = Pattern.compile("CC([1-4])=(.+)");
    private static final Pattern CEA708_SERVICE_DESCRIPTOR_REGEX = Pattern.compile("([1-4])=lang:(\\w+)(,.+)?");
    private final Allocator allocator;
    private final BaseUrlExclusionList baseUrlExclusionList;
    private MediaPeriod.Callback callback;
    private final DashChunkSource.Factory chunkSourceFactory;
    private final CmcdConfiguration cmcdConfiguration;
    private SequenceableLoader compositeSequenceableLoader;
    private final CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory;
    private final DrmSessionEventListener.EventDispatcher drmEventDispatcher;
    private final DrmSessionManager drmSessionManager;
    private final long elapsedRealtimeOffsetMs;
    private List<EventStream> eventStreams;
    final int id;
    private final LoadErrorHandlingPolicy loadErrorHandlingPolicy;
    private DashManifest manifest;
    private final LoaderErrorThrower manifestLoaderErrorThrower;
    private final MediaSourceEventListener.EventDispatcher mediaSourceEventDispatcher;
    private int periodIndex;
    private final PlayerEmsgHandler playerEmsgHandler;
    private final PlayerId playerId;
    private final TrackGroupInfo[] trackGroupInfos;
    private final TrackGroupArray trackGroups;
    private final TransferListener transferListener;
    private ChunkSampleStream<DashChunkSource>[] sampleStreams = newSampleStreamArray(0);
    private EventSampleStream[] eventSampleStreams = new EventSampleStream[0];
    private final IdentityHashMap<ChunkSampleStream<DashChunkSource>, PlayerEmsgHandler.PlayerTrackEmsgHandler> trackEmsgHandlerBySampleStream = new IdentityHashMap<>();

    public DashMediaPeriod(int id, DashManifest manifest, BaseUrlExclusionList baseUrlExclusionList, int periodIndex, DashChunkSource.Factory chunkSourceFactory, TransferListener transferListener, CmcdConfiguration cmcdConfiguration, DrmSessionManager drmSessionManager, DrmSessionEventListener.EventDispatcher drmEventDispatcher, LoadErrorHandlingPolicy loadErrorHandlingPolicy, MediaSourceEventListener.EventDispatcher mediaSourceEventDispatcher, long elapsedRealtimeOffsetMs, LoaderErrorThrower manifestLoaderErrorThrower, Allocator allocator, CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory, PlayerEmsgHandler.PlayerEmsgCallback playerEmsgCallback, PlayerId playerId) {
        this.id = id;
        this.manifest = manifest;
        this.baseUrlExclusionList = baseUrlExclusionList;
        this.periodIndex = periodIndex;
        this.chunkSourceFactory = chunkSourceFactory;
        this.transferListener = transferListener;
        this.cmcdConfiguration = cmcdConfiguration;
        this.drmSessionManager = drmSessionManager;
        this.drmEventDispatcher = drmEventDispatcher;
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy;
        this.mediaSourceEventDispatcher = mediaSourceEventDispatcher;
        this.elapsedRealtimeOffsetMs = elapsedRealtimeOffsetMs;
        this.manifestLoaderErrorThrower = manifestLoaderErrorThrower;
        this.allocator = allocator;
        this.compositeSequenceableLoaderFactory = compositeSequenceableLoaderFactory;
        this.playerId = playerId;
        this.playerEmsgHandler = new PlayerEmsgHandler(manifest, playerEmsgCallback, allocator);
        this.compositeSequenceableLoader = compositeSequenceableLoaderFactory.empty();
        Period period = manifest.getPeriod(periodIndex);
        this.eventStreams = period.eventStreams;
        Pair<TrackGroupArray, TrackGroupInfo[]> result = buildTrackGroups(drmSessionManager, chunkSourceFactory, period.adaptationSets, this.eventStreams);
        this.trackGroups = (TrackGroupArray) result.first;
        this.trackGroupInfos = (TrackGroupInfo[]) result.second;
    }

    public void updateManifest(DashManifest manifest, int periodIndex) {
        this.manifest = manifest;
        this.periodIndex = periodIndex;
        this.playerEmsgHandler.updateManifest(manifest);
        if (this.sampleStreams != null) {
            for (ChunkSampleStream<DashChunkSource> sampleStream : this.sampleStreams) {
                ((DashChunkSource) sampleStream.getChunkSource()).updateManifest(manifest, periodIndex);
            }
            this.callback.onContinueLoadingRequested(this);
        }
        this.eventStreams = manifest.getPeriod(periodIndex).eventStreams;
        for (EventSampleStream eventSampleStream : this.eventSampleStreams) {
            for (EventStream eventStream : this.eventStreams) {
                if (eventStream.id().equals(eventSampleStream.eventStreamId())) {
                    int lastPeriodIndex = manifest.getPeriodCount() - 1;
                    eventSampleStream.updateEventStream(eventStream, manifest.dynamic && periodIndex == lastPeriodIndex);
                    break;
                }
            }
        }
    }

    public void release() {
        this.playerEmsgHandler.release();
        for (ChunkSampleStream<DashChunkSource> sampleStream : this.sampleStreams) {
            sampleStream.release(this);
        }
        this.callback = null;
    }

    @Override // androidx.media3.exoplayer.source.chunk.ChunkSampleStream.ReleaseCallback
    public synchronized void onSampleStreamReleased(ChunkSampleStream<DashChunkSource> stream) {
        PlayerEmsgHandler.PlayerTrackEmsgHandler trackEmsgHandler = this.trackEmsgHandlerBySampleStream.remove(stream);
        if (trackEmsgHandler != null) {
            trackEmsgHandler.release();
        }
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void prepare(MediaPeriod.Callback callback, long positionUs) {
        this.callback = callback;
        callback.onPrepared(this);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void maybeThrowPrepareError() throws IOException {
        this.manifestLoaderErrorThrower.maybeThrowError();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public TrackGroupArray getTrackGroups() {
        return this.trackGroups;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public List<StreamKey> getStreamKeys(List<ExoTrackSelection> trackSelections) {
        DashMediaPeriod dashMediaPeriod = this;
        List<AdaptationSet> manifestAdaptationSets = dashMediaPeriod.manifest.getPeriod(dashMediaPeriod.periodIndex).adaptationSets;
        List<StreamKey> streamKeys = new ArrayList<>();
        Iterator<ExoTrackSelection> it = trackSelections.iterator();
        while (it.hasNext()) {
            ExoTrackSelection trackSelection = it.next();
            int trackGroupIndex = dashMediaPeriod.trackGroups.indexOf(trackSelection.getTrackGroup());
            TrackGroupInfo trackGroupInfo = dashMediaPeriod.trackGroupInfos[trackGroupIndex];
            if (trackGroupInfo.trackGroupCategory == 0) {
                int[] adaptationSetIndices = trackGroupInfo.adaptationSetIndices;
                int[] trackIndices = new int[trackSelection.length()];
                for (int i = 0; i < trackSelection.length(); i++) {
                    trackIndices[i] = trackSelection.getIndexInTrackGroup(i);
                }
                Arrays.sort(trackIndices);
                int currentAdaptationSetIndex = 0;
                int totalTracksInPreviousAdaptationSets = 0;
                int i2 = 0;
                int tracksInCurrentAdaptationSet = manifestAdaptationSets.get(adaptationSetIndices[0]).representations.size();
                int length = trackIndices.length;
                while (i2 < length) {
                    int trackIndex = trackIndices[i2];
                    while (trackIndex >= totalTracksInPreviousAdaptationSets + tracksInCurrentAdaptationSet) {
                        currentAdaptationSetIndex++;
                        totalTracksInPreviousAdaptationSets += tracksInCurrentAdaptationSet;
                        tracksInCurrentAdaptationSet = manifestAdaptationSets.get(adaptationSetIndices[currentAdaptationSetIndex]).representations.size();
                    }
                    streamKeys.add(new StreamKey(dashMediaPeriod.periodIndex, adaptationSetIndices[currentAdaptationSetIndex], trackIndex - totalTracksInPreviousAdaptationSets));
                    i2++;
                    dashMediaPeriod = this;
                    manifestAdaptationSets = manifestAdaptationSets;
                    it = it;
                }
                dashMediaPeriod = this;
            }
        }
        return streamKeys;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long selectTracks(ExoTrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
        int[] streamIndexToTrackGroupIndex = getStreamIndexToTrackGroupIndex(selections);
        releaseDisabledStreams(selections, mayRetainStreamFlags, streams);
        releaseOrphanEmbeddedStreams(selections, streams, streamIndexToTrackGroupIndex);
        selectNewStreams(selections, streams, streamResetFlags, positionUs, streamIndexToTrackGroupIndex);
        ArrayList<ChunkSampleStream<DashChunkSource>> sampleStreamList = new ArrayList<>();
        ArrayList<EventSampleStream> eventSampleStreamList = new ArrayList<>();
        for (SampleStream sampleStream : streams) {
            if (sampleStream instanceof ChunkSampleStream) {
                ChunkSampleStream<DashChunkSource> stream = (ChunkSampleStream) sampleStream;
                sampleStreamList.add(stream);
            } else if (sampleStream instanceof EventSampleStream) {
                eventSampleStreamList.add((EventSampleStream) sampleStream);
            }
        }
        this.sampleStreams = newSampleStreamArray(sampleStreamList.size());
        sampleStreamList.toArray(this.sampleStreams);
        this.eventSampleStreams = new EventSampleStream[eventSampleStreamList.size()];
        eventSampleStreamList.toArray(this.eventSampleStreams);
        this.compositeSequenceableLoader = this.compositeSequenceableLoaderFactory.create(sampleStreamList, Lists.transform(sampleStreamList, new Function() { // from class: androidx.media3.exoplayer.dash.DashMediaPeriod$$ExternalSyntheticLambda0
            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                return ImmutableList.of(Integer.valueOf(((ChunkSampleStream) obj).primaryTrackType));
            }
        }));
        return positionUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void discardBuffer(long positionUs, boolean toKeyframe) {
        for (ChunkSampleStream<DashChunkSource> sampleStream : this.sampleStreams) {
            sampleStream.discardBuffer(positionUs, toKeyframe);
        }
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public void reevaluateBuffer(long positionUs) {
        this.compositeSequenceableLoader.reevaluateBuffer(positionUs);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public boolean continueLoading(LoadingInfo loadingInfo) {
        return this.compositeSequenceableLoader.continueLoading(loadingInfo);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public boolean isLoading() {
        return this.compositeSequenceableLoader.isLoading();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public long getNextLoadPositionUs() {
        return this.compositeSequenceableLoader.getNextLoadPositionUs();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long readDiscontinuity() {
        return C.TIME_UNSET;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public long getBufferedPositionUs() {
        return this.compositeSequenceableLoader.getBufferedPositionUs();
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long seekToUs(long positionUs) {
        for (ChunkSampleStream<DashChunkSource> sampleStream : this.sampleStreams) {
            sampleStream.seekToUs(positionUs);
        }
        for (EventSampleStream sampleStream2 : this.eventSampleStreams) {
            sampleStream2.seekToUs(positionUs);
        }
        return positionUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
        for (ChunkSampleStream<DashChunkSource> sampleStream : this.sampleStreams) {
            if (sampleStream.primaryTrackType == 2) {
                return sampleStream.getAdjustedSeekPositionUs(positionUs, seekParameters);
            }
        }
        return positionUs;
    }

    @Override // androidx.media3.exoplayer.source.SequenceableLoader.Callback
    public void onContinueLoadingRequested(ChunkSampleStream<DashChunkSource> sampleStream) {
        this.callback.onContinueLoadingRequested(this);
    }

    private int[] getStreamIndexToTrackGroupIndex(ExoTrackSelection[] selections) {
        int[] streamIndexToTrackGroupIndex = new int[selections.length];
        for (int i = 0; i < selections.length; i++) {
            if (selections[i] != null) {
                streamIndexToTrackGroupIndex[i] = this.trackGroups.indexOf(selections[i].getTrackGroup());
            } else {
                streamIndexToTrackGroupIndex[i] = -1;
            }
        }
        return streamIndexToTrackGroupIndex;
    }

    private void releaseDisabledStreams(ExoTrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams) {
        for (int i = 0; i < selections.length; i++) {
            if (selections[i] == null || !mayRetainStreamFlags[i]) {
                if (streams[i] instanceof ChunkSampleStream) {
                    ChunkSampleStream<DashChunkSource> stream = (ChunkSampleStream) streams[i];
                    stream.release(this);
                } else if (streams[i] instanceof ChunkSampleStream.EmbeddedSampleStream) {
                    ((ChunkSampleStream.EmbeddedSampleStream) streams[i]).release();
                }
                streams[i] = null;
            }
        }
    }

    private void releaseOrphanEmbeddedStreams(ExoTrackSelection[] selections, SampleStream[] streams, int[] streamIndexToTrackGroupIndex) {
        boolean mayRetainStream;
        for (int i = 0; i < selections.length; i++) {
            if ((streams[i] instanceof EmptySampleStream) || (streams[i] instanceof ChunkSampleStream.EmbeddedSampleStream)) {
                int primaryStreamIndex = getPrimaryStreamIndex(i, streamIndexToTrackGroupIndex);
                if (primaryStreamIndex == -1) {
                    mayRetainStream = streams[i] instanceof EmptySampleStream;
                } else {
                    mayRetainStream = (streams[i] instanceof ChunkSampleStream.EmbeddedSampleStream) && ((ChunkSampleStream.EmbeddedSampleStream) streams[i]).parent == streams[primaryStreamIndex];
                }
                if (!mayRetainStream) {
                    if (streams[i] instanceof ChunkSampleStream.EmbeddedSampleStream) {
                        ((ChunkSampleStream.EmbeddedSampleStream) streams[i]).release();
                    }
                    streams[i] = null;
                }
            }
        }
    }

    private void selectNewStreams(ExoTrackSelection[] selections, SampleStream[] streams, boolean[] streamResetFlags, long positionUs, int[] streamIndexToTrackGroupIndex) {
        for (int i = 0; i < selections.length; i++) {
            ExoTrackSelection selection = selections[i];
            if (selection != null) {
                if (streams[i] == null) {
                    streamResetFlags[i] = true;
                    int trackGroupIndex = streamIndexToTrackGroupIndex[i];
                    TrackGroupInfo trackGroupInfo = this.trackGroupInfos[trackGroupIndex];
                    if (trackGroupInfo.trackGroupCategory == 0) {
                        streams[i] = buildSampleStream(trackGroupInfo, selection, positionUs);
                    } else if (trackGroupInfo.trackGroupCategory == 2) {
                        EventStream eventStream = this.eventStreams.get(trackGroupInfo.eventStreamGroupIndex);
                        Format format = selection.getTrackGroup().getFormat(0);
                        streams[i] = new EventSampleStream(eventStream, format, this.manifest.dynamic);
                    }
                } else if (streams[i] instanceof ChunkSampleStream) {
                    ChunkSampleStream<DashChunkSource> stream = (ChunkSampleStream) streams[i];
                    ((DashChunkSource) stream.getChunkSource()).updateTrackSelection(selection);
                }
            }
        }
        for (int i2 = 0; i2 < selections.length; i2++) {
            if (streams[i2] == null && selections[i2] != null) {
                int trackGroupIndex2 = streamIndexToTrackGroupIndex[i2];
                TrackGroupInfo trackGroupInfo2 = this.trackGroupInfos[trackGroupIndex2];
                if (trackGroupInfo2.trackGroupCategory == 1) {
                    int primaryStreamIndex = getPrimaryStreamIndex(i2, streamIndexToTrackGroupIndex);
                    if (primaryStreamIndex == -1) {
                        streams[i2] = new EmptySampleStream();
                    } else {
                        streams[i2] = ((ChunkSampleStream) streams[primaryStreamIndex]).selectEmbeddedTrack(positionUs, trackGroupInfo2.trackType);
                    }
                }
            }
        }
    }

    private int getPrimaryStreamIndex(int embeddedStreamIndex, int[] streamIndexToTrackGroupIndex) {
        int embeddedTrackGroupIndex = streamIndexToTrackGroupIndex[embeddedStreamIndex];
        if (embeddedTrackGroupIndex == -1) {
            return -1;
        }
        int primaryTrackGroupIndex = this.trackGroupInfos[embeddedTrackGroupIndex].primaryTrackGroupIndex;
        for (int i = 0; i < streamIndexToTrackGroupIndex.length; i++) {
            int trackGroupIndex = streamIndexToTrackGroupIndex[i];
            if (trackGroupIndex == primaryTrackGroupIndex && this.trackGroupInfos[trackGroupIndex].trackGroupCategory == 0) {
                return i;
            }
        }
        return -1;
    }

    private static Pair<TrackGroupArray, TrackGroupInfo[]> buildTrackGroups(DrmSessionManager drmSessionManager, DashChunkSource.Factory chunkSourceFactory, List<AdaptationSet> adaptationSets, List<EventStream> eventStreams) {
        int[][] groupedAdaptationSetIndices = getGroupedAdaptationSetIndices(adaptationSets);
        int primaryGroupCount = groupedAdaptationSetIndices.length;
        boolean[] primaryGroupHasEventMessageTrackFlags = new boolean[primaryGroupCount];
        Format[][] primaryGroupClosedCaptionTrackFormats = new Format[primaryGroupCount][];
        int totalEmbeddedTrackGroupCount = identifyEmbeddedTracks(primaryGroupCount, adaptationSets, groupedAdaptationSetIndices, primaryGroupHasEventMessageTrackFlags, primaryGroupClosedCaptionTrackFormats);
        int totalGroupCount = primaryGroupCount + totalEmbeddedTrackGroupCount + eventStreams.size();
        TrackGroup[] trackGroups = new TrackGroup[totalGroupCount];
        TrackGroupInfo[] trackGroupInfos = new TrackGroupInfo[totalGroupCount];
        int trackGroupCount = buildPrimaryAndEmbeddedTrackGroupInfos(drmSessionManager, chunkSourceFactory, adaptationSets, groupedAdaptationSetIndices, primaryGroupCount, primaryGroupHasEventMessageTrackFlags, primaryGroupClosedCaptionTrackFormats, trackGroups, trackGroupInfos);
        buildManifestEventTrackGroupInfos(eventStreams, trackGroups, trackGroupInfos, trackGroupCount);
        return Pair.create(new TrackGroupArray(trackGroups), trackGroupInfos);
    }

    private static int[][] getGroupedAdaptationSetIndices(List<AdaptationSet> adaptationSets) {
        Descriptor adaptationSetSwitchingProperty;
        int adaptationSetCount = adaptationSets.size();
        HashMap<Long, Integer> adaptationSetIdToIndex = Maps.newHashMapWithExpectedSize(adaptationSetCount);
        List<List<Integer>> adaptationSetGroupedIndices = new ArrayList<>(adaptationSetCount);
        SparseArray<List<Integer>> adaptationSetIndexToGroupedIndices = new SparseArray<>(adaptationSetCount);
        for (int i = 0; i < adaptationSetCount; i++) {
            adaptationSetIdToIndex.put(Long.valueOf(adaptationSets.get(i).id), Integer.valueOf(i));
            List<Integer> initialGroup = new ArrayList<>();
            initialGroup.add(Integer.valueOf(i));
            adaptationSetGroupedIndices.add(initialGroup);
            adaptationSetIndexToGroupedIndices.put(i, initialGroup);
        }
        for (int i2 = 0; i2 < adaptationSetCount; i2++) {
            int mergedGroupIndex = i2;
            AdaptationSet adaptationSet = adaptationSets.get(i2);
            Descriptor trickPlayProperty = findTrickPlayProperty(adaptationSet.essentialProperties);
            if (trickPlayProperty == null) {
                trickPlayProperty = findTrickPlayProperty(adaptationSet.supplementalProperties);
            }
            if (trickPlayProperty != null) {
                long mainAdaptationSetId = Long.parseLong(trickPlayProperty.value);
                Integer mainAdaptationSetIndex = adaptationSetIdToIndex.get(Long.valueOf(mainAdaptationSetId));
                if (mainAdaptationSetIndex != null) {
                    mergedGroupIndex = mainAdaptationSetIndex.intValue();
                }
            }
            if (mergedGroupIndex == i2 && (adaptationSetSwitchingProperty = findAdaptationSetSwitchingProperty(adaptationSet.supplementalProperties)) != null) {
                String[] otherAdaptationSetIds = Util.split(adaptationSetSwitchingProperty.value, ",");
                for (String adaptationSetId : otherAdaptationSetIds) {
                    Integer otherAdaptationSetIndex = adaptationSetIdToIndex.get(Long.valueOf(Long.parseLong(adaptationSetId)));
                    if (otherAdaptationSetIndex != null) {
                        mergedGroupIndex = Math.min(mergedGroupIndex, otherAdaptationSetIndex.intValue());
                    }
                }
            }
            if (mergedGroupIndex != i2) {
                List<Integer> thisGroup = adaptationSetIndexToGroupedIndices.get(i2);
                List<Integer> mergedGroup = adaptationSetIndexToGroupedIndices.get(mergedGroupIndex);
                mergedGroup.addAll(thisGroup);
                adaptationSetIndexToGroupedIndices.put(i2, mergedGroup);
                adaptationSetGroupedIndices.remove(thisGroup);
            }
        }
        int i3 = adaptationSetGroupedIndices.size();
        int[][] groupedAdaptationSetIndices = new int[i3][];
        for (int i4 = 0; i4 < groupedAdaptationSetIndices.length; i4++) {
            groupedAdaptationSetIndices[i4] = Ints.toArray(adaptationSetGroupedIndices.get(i4));
            Arrays.sort(groupedAdaptationSetIndices[i4]);
        }
        return groupedAdaptationSetIndices;
    }

    private static int identifyEmbeddedTracks(int primaryGroupCount, List<AdaptationSet> adaptationSets, int[][] groupedAdaptationSetIndices, boolean[] primaryGroupHasEventMessageTrackFlags, Format[][] primaryGroupClosedCaptionTrackFormats) {
        int numEmbeddedTrackGroups = 0;
        for (int i = 0; i < primaryGroupCount; i++) {
            if (hasEventMessageTrack(adaptationSets, groupedAdaptationSetIndices[i])) {
                primaryGroupHasEventMessageTrackFlags[i] = true;
                numEmbeddedTrackGroups++;
            }
            primaryGroupClosedCaptionTrackFormats[i] = getClosedCaptionTrackFormats(adaptationSets, groupedAdaptationSetIndices[i]);
            if (primaryGroupClosedCaptionTrackFormats[i].length != 0) {
                numEmbeddedTrackGroups++;
            }
        }
        return numEmbeddedTrackGroups;
    }

    private static int buildPrimaryAndEmbeddedTrackGroupInfos(DrmSessionManager drmSessionManager, DashChunkSource.Factory chunkSourceFactory, List<AdaptationSet> adaptationSets, int[][] groupedAdaptationSetIndices, int primaryGroupCount, boolean[] primaryGroupHasEventMessageTrackFlags, Format[][] primaryGroupClosedCaptionTrackFormats, TrackGroup[] trackGroups, TrackGroupInfo[] trackGroupInfos) {
        String trackGroupId;
        int trackGroupCount;
        int closedCaptionTrackGroupIndex;
        int trackGroupCount2 = 0;
        int i = 0;
        while (i < primaryGroupCount) {
            int[] adaptationSetIndices = groupedAdaptationSetIndices[i];
            List<Representation> representations = new ArrayList<>();
            for (int adaptationSetIndex : adaptationSetIndices) {
                representations.addAll(adaptationSets.get(adaptationSetIndex).representations);
            }
            Format[] formats = new Format[representations.size()];
            for (int j = 0; j < formats.length; j++) {
                Format originalFormat = representations.get(j).format;
                Format.Builder updatedFormat = originalFormat.buildUpon().setCryptoType(drmSessionManager.getCryptoType(originalFormat));
                formats[j] = updatedFormat.build();
            }
            int j2 = adaptationSetIndices[0];
            AdaptationSet firstAdaptationSet = adaptationSets.get(j2);
            if (firstAdaptationSet.id != -1) {
                trackGroupId = Long.toString(firstAdaptationSet.id);
            } else {
                trackGroupId = "unset:" + i;
            }
            int eventMessageTrackGroupIndex = trackGroupCount2 + 1;
            if (primaryGroupHasEventMessageTrackFlags[i]) {
                trackGroupCount = eventMessageTrackGroupIndex + 1;
            } else {
                trackGroupCount = eventMessageTrackGroupIndex;
                eventMessageTrackGroupIndex = -1;
            }
            if (primaryGroupClosedCaptionTrackFormats[i].length != 0) {
                closedCaptionTrackGroupIndex = trackGroupCount;
                trackGroupCount++;
            } else {
                closedCaptionTrackGroupIndex = -1;
            }
            maybeUpdateFormatsForParsedText(chunkSourceFactory, formats);
            trackGroups[trackGroupCount2] = new TrackGroup(trackGroupId, formats);
            trackGroupInfos[trackGroupCount2] = TrackGroupInfo.primaryTrack(firstAdaptationSet.type, adaptationSetIndices, trackGroupCount2, eventMessageTrackGroupIndex, closedCaptionTrackGroupIndex);
            if (eventMessageTrackGroupIndex != -1) {
                String eventMessageTrackGroupId = trackGroupId + ":emsg";
                Format format = new Format.Builder().setId(eventMessageTrackGroupId).setSampleMimeType(MimeTypes.APPLICATION_EMSG).build();
                trackGroups[eventMessageTrackGroupIndex] = new TrackGroup(eventMessageTrackGroupId, format);
                trackGroupInfos[eventMessageTrackGroupIndex] = TrackGroupInfo.embeddedEmsgTrack(adaptationSetIndices, trackGroupCount2);
            }
            if (closedCaptionTrackGroupIndex != -1) {
                String closedCaptionTrackGroupId = trackGroupId + ":cc";
                trackGroupInfos[closedCaptionTrackGroupIndex] = TrackGroupInfo.embeddedClosedCaptionTrack(adaptationSetIndices, trackGroupCount2, ImmutableList.copyOf(primaryGroupClosedCaptionTrackFormats[i]));
                maybeUpdateFormatsForParsedText(chunkSourceFactory, primaryGroupClosedCaptionTrackFormats[i]);
                trackGroups[closedCaptionTrackGroupIndex] = new TrackGroup(closedCaptionTrackGroupId, primaryGroupClosedCaptionTrackFormats[i]);
            }
            i++;
            trackGroupCount2 = trackGroupCount;
        }
        return trackGroupCount2;
    }

    private static void buildManifestEventTrackGroupInfos(List<EventStream> eventStreams, TrackGroup[] trackGroups, TrackGroupInfo[] trackGroupInfos, int existingTrackGroupCount) {
        int i = 0;
        while (i < eventStreams.size()) {
            EventStream eventStream = eventStreams.get(i);
            Format format = new Format.Builder().setId(eventStream.id()).setSampleMimeType(MimeTypes.APPLICATION_EMSG).build();
            String uniqueTrackGroupId = eventStream.id() + ":" + i;
            trackGroups[existingTrackGroupCount] = new TrackGroup(uniqueTrackGroupId, format);
            trackGroupInfos[existingTrackGroupCount] = TrackGroupInfo.mpdEventTrack(i);
            i++;
            existingTrackGroupCount++;
        }
    }

    private ChunkSampleStream<DashChunkSource> buildSampleStream(TrackGroupInfo trackGroupInfo, ExoTrackSelection selection, long positionUs) {
        ImmutableList<Format> embeddedClosedCaptionOriginalFormats;
        PlayerEmsgHandler.PlayerTrackEmsgHandler trackPlayerEmsgHandler;
        int embeddedTrackCount = 0;
        boolean enableEventMessageTrack = trackGroupInfo.embeddedEventMessageTrackGroupIndex != -1;
        TrackGroup embeddedEventMessageTrackGroup = null;
        if (enableEventMessageTrack) {
            embeddedEventMessageTrackGroup = this.trackGroups.get(trackGroupInfo.embeddedEventMessageTrackGroupIndex);
            embeddedTrackCount = 0 + 1;
        }
        if (trackGroupInfo.embeddedClosedCaptionTrackGroupIndex != -1) {
            embeddedClosedCaptionOriginalFormats = this.trackGroupInfos[trackGroupInfo.embeddedClosedCaptionTrackGroupIndex].embeddedClosedCaptionTrackOriginalFormats;
        } else {
            embeddedClosedCaptionOriginalFormats = ImmutableList.of();
        }
        int embeddedTrackCount2 = embeddedTrackCount + embeddedClosedCaptionOriginalFormats.size();
        Format[] embeddedTrackFormats = new Format[embeddedTrackCount2];
        int[] embeddedTrackTypes = new int[embeddedTrackCount2];
        int embeddedTrackCount3 = 0;
        if (enableEventMessageTrack) {
            embeddedTrackFormats[0] = embeddedEventMessageTrackGroup.getFormat(0);
            embeddedTrackTypes[0] = 5;
            embeddedTrackCount3 = 0 + 1;
        }
        List<Format> embeddedClosedCaptionTrackFormats = new ArrayList<>();
        int embeddedTrackCount4 = embeddedTrackCount3;
        for (int i = 0; i < embeddedTrackCount; i++) {
            embeddedTrackFormats[embeddedTrackCount4] = embeddedClosedCaptionOriginalFormats.get(i);
            embeddedTrackTypes[embeddedTrackCount4] = 3;
            embeddedClosedCaptionTrackFormats.add(embeddedTrackFormats[embeddedTrackCount4]);
            embeddedTrackCount4++;
        }
        if (this.manifest.dynamic && enableEventMessageTrack) {
            trackPlayerEmsgHandler = this.playerEmsgHandler.newPlayerTrackEmsgHandler();
        } else {
            trackPlayerEmsgHandler = null;
        }
        DashChunkSource chunkSource = this.chunkSourceFactory.createDashChunkSource(this.manifestLoaderErrorThrower, this.manifest, this.baseUrlExclusionList, this.periodIndex, trackGroupInfo.adaptationSetIndices, selection, trackGroupInfo.trackType, this.elapsedRealtimeOffsetMs, enableEventMessageTrack, embeddedClosedCaptionTrackFormats, trackPlayerEmsgHandler, this.transferListener, this.playerId, this.cmcdConfiguration);
        PlayerEmsgHandler.PlayerTrackEmsgHandler trackPlayerEmsgHandler2 = trackPlayerEmsgHandler;
        ChunkSampleStream<DashChunkSource> stream = new ChunkSampleStream<>(trackGroupInfo.trackType, embeddedTrackTypes, embeddedTrackFormats, chunkSource, this, this.allocator, positionUs, this.drmSessionManager, this.drmEventDispatcher, this.loadErrorHandlingPolicy, this.mediaSourceEventDispatcher);
        synchronized (this) {
            this.trackEmsgHandlerBySampleStream.put(stream, trackPlayerEmsgHandler2);
        }
        return stream;
    }

    private static Descriptor findAdaptationSetSwitchingProperty(List<Descriptor> descriptors) {
        return findDescriptor(descriptors, "urn:mpeg:dash:adaptation-set-switching:2016");
    }

    private static Descriptor findTrickPlayProperty(List<Descriptor> descriptors) {
        return findDescriptor(descriptors, "http://dashif.org/guidelines/trickmode");
    }

    private static Descriptor findDescriptor(List<Descriptor> descriptors, String schemeIdUri) {
        for (int i = 0; i < descriptors.size(); i++) {
            Descriptor descriptor = descriptors.get(i);
            if (schemeIdUri.equals(descriptor.schemeIdUri)) {
                return descriptor;
            }
        }
        return null;
    }

    private static boolean hasEventMessageTrack(List<AdaptationSet> adaptationSets, int[] adaptationSetIndices) {
        for (int i : adaptationSetIndices) {
            List<Representation> representations = adaptationSets.get(i).representations;
            for (int j = 0; j < representations.size(); j++) {
                Representation representation = representations.get(j);
                if (!representation.inbandEventStreams.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Format[] getClosedCaptionTrackFormats(List<AdaptationSet> adaptationSets, int[] adaptationSetIndices) {
        for (int i : adaptationSetIndices) {
            AdaptationSet adaptationSet = adaptationSets.get(i);
            List<Descriptor> descriptors = adaptationSets.get(i).accessibilityDescriptors;
            for (int j = 0; j < descriptors.size(); j++) {
                Descriptor descriptor = descriptors.get(j);
                if ("urn:scte:dash:cc:cea-608:2015".equals(descriptor.schemeIdUri)) {
                    Format cea608Format = new Format.Builder().setSampleMimeType(MimeTypes.APPLICATION_CEA608).setId(adaptationSet.id + ":cea608").build();
                    return parseClosedCaptionDescriptor(descriptor, CEA608_SERVICE_DESCRIPTOR_REGEX, cea608Format);
                }
                if ("urn:scte:dash:cc:cea-708:2015".equals(descriptor.schemeIdUri)) {
                    Format cea708Format = new Format.Builder().setSampleMimeType(MimeTypes.APPLICATION_CEA708).setId(adaptationSet.id + ":cea708").build();
                    return parseClosedCaptionDescriptor(descriptor, CEA708_SERVICE_DESCRIPTOR_REGEX, cea708Format);
                }
            }
        }
        return new Format[0];
    }

    private static Format[] parseClosedCaptionDescriptor(Descriptor descriptor, Pattern serviceDescriptorRegex, Format baseFormat) {
        String value = descriptor.value;
        if (value == null) {
            return new Format[]{baseFormat};
        }
        String[] services = Util.split(value, ";");
        Format[] formats = new Format[services.length];
        for (int i = 0; i < services.length; i++) {
            Matcher matcher = serviceDescriptorRegex.matcher(services[i]);
            if (!matcher.matches()) {
                return new Format[]{baseFormat};
            }
            int accessibilityChannel = Integer.parseInt(matcher.group(1));
            formats[i] = baseFormat.buildUpon().setId(baseFormat.id + ":" + accessibilityChannel).setAccessibilityChannel(accessibilityChannel).setLanguage(matcher.group(2)).build();
        }
        return formats;
    }

    private static void maybeUpdateFormatsForParsedText(DashChunkSource.Factory chunkSourceFactory, Format[] formats) {
        for (int i = 0; i < formats.length; i++) {
            formats[i] = chunkSourceFactory.getOutputTextFormat(formats[i]);
        }
    }

    private static ChunkSampleStream<DashChunkSource>[] newSampleStreamArray(int length) {
        return new ChunkSampleStream[length];
    }

    private static final class TrackGroupInfo {
        private static final int CATEGORY_EMBEDDED = 1;
        private static final int CATEGORY_MANIFEST_EVENTS = 2;
        private static final int CATEGORY_PRIMARY = 0;
        public final int[] adaptationSetIndices;
        public final int embeddedClosedCaptionTrackGroupIndex;
        public final ImmutableList<Format> embeddedClosedCaptionTrackOriginalFormats;
        public final int embeddedEventMessageTrackGroupIndex;
        public final int eventStreamGroupIndex;
        public final int primaryTrackGroupIndex;
        public final int trackGroupCategory;
        public final int trackType;

        @Target({ElementType.TYPE_USE})
        @Documented
        @Retention(RetentionPolicy.SOURCE)
        public @interface TrackGroupCategory {
        }

        public static TrackGroupInfo primaryTrack(int trackType, int[] adaptationSetIndices, int primaryTrackGroupIndex, int embeddedEventMessageTrackGroupIndex, int embeddedClosedCaptionTrackGroupIndex) {
            return new TrackGroupInfo(trackType, 0, adaptationSetIndices, primaryTrackGroupIndex, embeddedEventMessageTrackGroupIndex, embeddedClosedCaptionTrackGroupIndex, -1, ImmutableList.of());
        }

        public static TrackGroupInfo embeddedEmsgTrack(int[] adaptationSetIndices, int primaryTrackGroupIndex) {
            return new TrackGroupInfo(5, 1, adaptationSetIndices, primaryTrackGroupIndex, -1, -1, -1, ImmutableList.of());
        }

        public static TrackGroupInfo embeddedClosedCaptionTrack(int[] adaptationSetIndices, int primaryTrackGroupIndex, ImmutableList<Format> originalFormats) {
            return new TrackGroupInfo(3, 1, adaptationSetIndices, primaryTrackGroupIndex, -1, -1, -1, originalFormats);
        }

        public static TrackGroupInfo mpdEventTrack(int eventStreamIndex) {
            return new TrackGroupInfo(5, 2, new int[0], -1, -1, -1, eventStreamIndex, ImmutableList.of());
        }

        private TrackGroupInfo(int trackType, int trackGroupCategory, int[] adaptationSetIndices, int primaryTrackGroupIndex, int embeddedEventMessageTrackGroupIndex, int embeddedClosedCaptionTrackGroupIndex, int eventStreamGroupIndex, ImmutableList<Format> embeddedClosedCaptionTrackOriginalFormats) {
            this.trackType = trackType;
            this.adaptationSetIndices = adaptationSetIndices;
            this.trackGroupCategory = trackGroupCategory;
            this.primaryTrackGroupIndex = primaryTrackGroupIndex;
            this.embeddedEventMessageTrackGroupIndex = embeddedEventMessageTrackGroupIndex;
            this.embeddedClosedCaptionTrackGroupIndex = embeddedClosedCaptionTrackGroupIndex;
            this.eventStreamGroupIndex = eventStreamGroupIndex;
            this.embeddedClosedCaptionTrackOriginalFormats = embeddedClosedCaptionTrackOriginalFormats;
        }
    }
}
