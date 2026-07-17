package androidx.media3.exoplayer.hls;

import android.net.Uri;
import android.text.TextUtils;
import androidx.media3.common.C;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.Format;
import androidx.media3.common.Label;
import androidx.media3.common.Metadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.StreamKey;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.TransferListener;
import androidx.media3.exoplayer.LoadingInfo;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.drm.DrmSessionEventListener;
import androidx.media3.exoplayer.drm.DrmSessionManager;
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist;
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker;
import androidx.media3.exoplayer.source.CompositeSequenceableLoaderFactory;
import androidx.media3.exoplayer.source.MediaPeriod;
import androidx.media3.exoplayer.source.MediaSourceEventListener;
import androidx.media3.exoplayer.source.SampleStream;
import androidx.media3.exoplayer.source.SequenceableLoader;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.upstream.Allocator;
import androidx.media3.exoplayer.upstream.CmcdConfiguration;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class HlsMediaPeriod implements MediaPeriod, HlsPlaylistTracker.PlaylistEventListener {
    private final Allocator allocator;
    private final boolean allowChunklessPreparation;
    private int audioVideoSampleStreamWrapperCount;
    private final CmcdConfiguration cmcdConfiguration;
    private SequenceableLoader compositeSequenceableLoader;
    private final CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory;
    private final HlsDataSourceFactory dataSourceFactory;
    private final DrmSessionEventListener.EventDispatcher drmEventDispatcher;
    private final DrmSessionManager drmSessionManager;
    private final MediaSourceEventListener.EventDispatcher eventDispatcher;
    private final HlsExtractorFactory extractorFactory;
    private final LoadErrorHandlingPolicy loadErrorHandlingPolicy;
    private MediaPeriod.Callback mediaPeriodCallback;
    private final TransferListener mediaTransferListener;
    private final int metadataType;
    private int pendingPrepareCount;
    private final PlayerId playerId;
    private final HlsPlaylistTracker playlistTracker;
    private final long timestampAdjusterInitializationTimeoutMs;
    private TrackGroupArray trackGroups;
    private final boolean useSessionKeys;
    private final HlsSampleStreamWrapper.Callback sampleStreamWrapperCallback = new SampleStreamWrapperCallback();
    private final IdentityHashMap<SampleStream, Integer> streamWrapperIndices = new IdentityHashMap<>();
    private final TimestampAdjusterProvider timestampAdjusterProvider = new TimestampAdjusterProvider();
    private HlsSampleStreamWrapper[] sampleStreamWrappers = new HlsSampleStreamWrapper[0];
    private HlsSampleStreamWrapper[] enabledSampleStreamWrappers = new HlsSampleStreamWrapper[0];
    private int[][] manifestUrlIndicesPerWrapper = new int[0][];

    static /* synthetic */ int access$106(HlsMediaPeriod x0) {
        int i = x0.pendingPrepareCount - 1;
        x0.pendingPrepareCount = i;
        return i;
    }

    public HlsMediaPeriod(HlsExtractorFactory extractorFactory, HlsPlaylistTracker playlistTracker, HlsDataSourceFactory dataSourceFactory, TransferListener mediaTransferListener, CmcdConfiguration cmcdConfiguration, DrmSessionManager drmSessionManager, DrmSessionEventListener.EventDispatcher drmEventDispatcher, LoadErrorHandlingPolicy loadErrorHandlingPolicy, MediaSourceEventListener.EventDispatcher eventDispatcher, Allocator allocator, CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory, boolean allowChunklessPreparation, int metadataType, boolean useSessionKeys, PlayerId playerId, long timestampAdjusterInitializationTimeoutMs) {
        this.extractorFactory = extractorFactory;
        this.playlistTracker = playlistTracker;
        this.dataSourceFactory = dataSourceFactory;
        this.mediaTransferListener = mediaTransferListener;
        this.cmcdConfiguration = cmcdConfiguration;
        this.drmSessionManager = drmSessionManager;
        this.drmEventDispatcher = drmEventDispatcher;
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy;
        this.eventDispatcher = eventDispatcher;
        this.allocator = allocator;
        this.compositeSequenceableLoaderFactory = compositeSequenceableLoaderFactory;
        this.allowChunklessPreparation = allowChunklessPreparation;
        this.metadataType = metadataType;
        this.useSessionKeys = useSessionKeys;
        this.playerId = playerId;
        this.timestampAdjusterInitializationTimeoutMs = timestampAdjusterInitializationTimeoutMs;
        this.compositeSequenceableLoader = compositeSequenceableLoaderFactory.empty();
    }

    public void release() {
        this.playlistTracker.removeListener(this);
        for (HlsSampleStreamWrapper sampleStreamWrapper : this.sampleStreamWrappers) {
            sampleStreamWrapper.release();
        }
        this.mediaPeriodCallback = null;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void prepare(MediaPeriod.Callback callback, long positionUs) {
        this.mediaPeriodCallback = callback;
        this.playlistTracker.addListener(this);
        buildAndPrepareSampleStreamWrappers(positionUs);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void maybeThrowPrepareError() throws IOException {
        for (HlsSampleStreamWrapper sampleStreamWrapper : this.sampleStreamWrappers) {
            sampleStreamWrapper.maybeThrowPrepareError();
        }
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public TrackGroupArray getTrackGroups() {
        return (TrackGroupArray) Assertions.checkNotNull(this.trackGroups);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public List<StreamKey> getStreamKeys(List<ExoTrackSelection> trackSelections) {
        int[] mainWrapperVariantIndices;
        TrackGroupArray mainWrapperTrackGroups;
        int mainWrapperPrimaryGroupIndex;
        boolean hasVariants;
        int audioWrapperOffset;
        int groupIndexType;
        HlsMediaPeriod hlsMediaPeriod = this;
        HlsMultivariantPlaylist multivariantPlaylist = (HlsMultivariantPlaylist) Assertions.checkNotNull(hlsMediaPeriod.playlistTracker.getMultivariantPlaylist());
        boolean hasVariants2 = !multivariantPlaylist.variants.isEmpty();
        int audioWrapperOffset2 = hasVariants2 ? 1 : 0;
        int subtitleWrapperOffset = hlsMediaPeriod.sampleStreamWrappers.length - multivariantPlaylist.subtitles.size();
        if (hasVariants2) {
            HlsSampleStreamWrapper mainWrapper = hlsMediaPeriod.sampleStreamWrappers[0];
            mainWrapperVariantIndices = hlsMediaPeriod.manifestUrlIndicesPerWrapper[0];
            mainWrapperTrackGroups = mainWrapper.getTrackGroups();
            mainWrapperPrimaryGroupIndex = mainWrapper.getPrimaryTrackGroupIndex();
        } else {
            mainWrapperVariantIndices = new int[0];
            mainWrapperTrackGroups = TrackGroupArray.EMPTY;
            mainWrapperPrimaryGroupIndex = 0;
        }
        List<StreamKey> streamKeys = new ArrayList<>();
        boolean needsPrimaryTrackGroupSelection = false;
        boolean hasPrimaryTrackGroupSelection = false;
        for (ExoTrackSelection trackSelection : trackSelections) {
            TrackGroup trackSelectionGroup = trackSelection.getTrackGroup();
            int mainWrapperTrackGroupIndex = mainWrapperTrackGroups.indexOf(trackSelectionGroup);
            if (mainWrapperTrackGroupIndex != -1) {
                if (mainWrapperTrackGroupIndex == mainWrapperPrimaryGroupIndex) {
                    boolean hasPrimaryTrackGroupSelection2 = true;
                    int i = 0;
                    while (true) {
                        hasVariants = hasVariants2;
                        if (i >= trackSelection.length()) {
                            break;
                        }
                        int variantIndex = mainWrapperVariantIndices[trackSelection.getIndexInTrackGroup(i)];
                        streamKeys.add(new StreamKey(0, variantIndex));
                        i++;
                        hasVariants2 = hasVariants;
                        mainWrapperTrackGroupIndex = mainWrapperTrackGroupIndex;
                        hasPrimaryTrackGroupSelection2 = hasPrimaryTrackGroupSelection2;
                    }
                    hasPrimaryTrackGroupSelection = hasPrimaryTrackGroupSelection2;
                    audioWrapperOffset = audioWrapperOffset2;
                } else {
                    hasVariants = hasVariants2;
                    needsPrimaryTrackGroupSelection = true;
                    audioWrapperOffset = audioWrapperOffset2;
                }
            } else {
                hasVariants = hasVariants2;
                int i2 = audioWrapperOffset2;
                while (true) {
                    if (i2 >= hlsMediaPeriod.sampleStreamWrappers.length) {
                        audioWrapperOffset = audioWrapperOffset2;
                        break;
                    }
                    TrackGroupArray wrapperTrackGroups = hlsMediaPeriod.sampleStreamWrappers[i2].getTrackGroups();
                    audioWrapperOffset = audioWrapperOffset2;
                    int selectedTrackGroupIndex = wrapperTrackGroups.indexOf(trackSelectionGroup);
                    if (selectedTrackGroupIndex == -1) {
                        i2++;
                        hlsMediaPeriod = this;
                        audioWrapperOffset2 = audioWrapperOffset;
                    } else {
                        if (i2 < subtitleWrapperOffset) {
                            groupIndexType = 1;
                        } else {
                            groupIndexType = 2;
                        }
                        int[] selectedWrapperUrlIndices = hlsMediaPeriod.manifestUrlIndicesPerWrapper[i2];
                        int trackIndex = 0;
                        while (true) {
                            int[] selectedWrapperUrlIndices2 = selectedWrapperUrlIndices;
                            if (trackIndex < trackSelection.length()) {
                                int renditionIndex = selectedWrapperUrlIndices2[trackSelection.getIndexInTrackGroup(trackIndex)];
                                streamKeys.add(new StreamKey(groupIndexType, renditionIndex));
                                trackIndex++;
                                selectedWrapperUrlIndices = selectedWrapperUrlIndices2;
                            }
                        }
                        break;
                    }
                }
            }
            hlsMediaPeriod = this;
            hasVariants2 = hasVariants;
            audioWrapperOffset2 = audioWrapperOffset;
        }
        if (needsPrimaryTrackGroupSelection && !hasPrimaryTrackGroupSelection) {
            int lowestBitrateIndex = mainWrapperVariantIndices[0];
            int lowestBitrate = multivariantPlaylist.variants.get(mainWrapperVariantIndices[0]).format.bitrate;
            for (int i3 = 1; i3 < mainWrapperVariantIndices.length; i3++) {
                int variantBitrate = multivariantPlaylist.variants.get(mainWrapperVariantIndices[i3]).format.bitrate;
                if (variantBitrate < lowestBitrate) {
                    lowestBitrate = variantBitrate;
                    lowestBitrateIndex = mainWrapperVariantIndices[i3];
                }
            }
            streamKeys.add(new StreamKey(0, lowestBitrateIndex));
        }
        return streamKeys;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long selectTracks(ExoTrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
        ExoTrackSelection[] exoTrackSelectionArr = selections;
        int[] streamChildIndices = new int[exoTrackSelectionArr.length];
        int[] selectionChildIndices = new int[exoTrackSelectionArr.length];
        for (int i = 0; i < exoTrackSelectionArr.length; i++) {
            streamChildIndices[i] = streams[i] == null ? -1 : this.streamWrapperIndices.get(streams[i]).intValue();
            selectionChildIndices[i] = -1;
            if (exoTrackSelectionArr[i] != null) {
                TrackGroup trackGroup = exoTrackSelectionArr[i].getTrackGroup();
                for (int j = 0; j < this.sampleStreamWrappers.length; j++) {
                    if (this.sampleStreamWrappers[j].getTrackGroups().indexOf(trackGroup) != -1) {
                        selectionChildIndices[i] = j;
                        break;
                    }
                }
            }
        }
        this.streamWrapperIndices.clear();
        SampleStream[] newStreams = new SampleStream[exoTrackSelectionArr.length];
        SampleStream[] childStreams = new SampleStream[exoTrackSelectionArr.length];
        ExoTrackSelection[] childSelections = new ExoTrackSelection[exoTrackSelectionArr.length];
        int newEnabledSampleStreamWrapperCount = 0;
        HlsSampleStreamWrapper[] newEnabledSampleStreamWrappers = new HlsSampleStreamWrapper[this.sampleStreamWrappers.length];
        boolean forceReset = false;
        int i2 = 0;
        while (i2 < this.sampleStreamWrappers.length) {
            for (int j2 = 0; j2 < exoTrackSelectionArr.length; j2++) {
                ExoTrackSelection exoTrackSelection = null;
                childStreams[j2] = streamChildIndices[j2] == i2 ? streams[j2] : null;
                if (selectionChildIndices[j2] == i2) {
                    exoTrackSelection = exoTrackSelectionArr[j2];
                }
                childSelections[j2] = exoTrackSelection;
            }
            HlsSampleStreamWrapper sampleStreamWrapper = this.sampleStreamWrappers[i2];
            int[] streamChildIndices2 = streamChildIndices;
            HlsSampleStreamWrapper[] newEnabledSampleStreamWrappers2 = newEnabledSampleStreamWrappers;
            boolean wasReset = sampleStreamWrapper.selectTracks(childSelections, mayRetainStreamFlags, childStreams, streamResetFlags, positionUs, forceReset);
            boolean wrapperEnabled = false;
            int j3 = 0;
            while (j3 < exoTrackSelectionArr.length) {
                SampleStream childStream = childStreams[j3];
                if (selectionChildIndices[j3] == i2) {
                    Assertions.checkNotNull(childStream);
                    newStreams[j3] = childStream;
                    wrapperEnabled = true;
                    this.streamWrapperIndices.put(childStream, Integer.valueOf(i2));
                } else if (streamChildIndices2[j3] == i2) {
                    Assertions.checkState(childStream == null);
                }
                j3++;
                exoTrackSelectionArr = selections;
            }
            if (wrapperEnabled) {
                newEnabledSampleStreamWrappers2[newEnabledSampleStreamWrapperCount] = sampleStreamWrapper;
                int newEnabledSampleStreamWrapperCount2 = newEnabledSampleStreamWrapperCount + 1;
                if (newEnabledSampleStreamWrapperCount == 0) {
                    sampleStreamWrapper.setIsPrimaryTimestampSource(true);
                    if (wasReset || this.enabledSampleStreamWrappers.length == 0 || sampleStreamWrapper != this.enabledSampleStreamWrappers[0]) {
                        this.timestampAdjusterProvider.reset();
                        forceReset = true;
                        newEnabledSampleStreamWrapperCount = newEnabledSampleStreamWrapperCount2;
                    }
                } else {
                    sampleStreamWrapper.setIsPrimaryTimestampSource(i2 < this.audioVideoSampleStreamWrapperCount);
                }
                newEnabledSampleStreamWrapperCount = newEnabledSampleStreamWrapperCount2;
            }
            i2++;
            exoTrackSelectionArr = selections;
            newEnabledSampleStreamWrappers = newEnabledSampleStreamWrappers2;
            streamChildIndices = streamChildIndices2;
        }
        System.arraycopy(newStreams, 0, streams, 0, newStreams.length);
        this.enabledSampleStreamWrappers = (HlsSampleStreamWrapper[]) Util.nullSafeArrayCopy(newEnabledSampleStreamWrappers, newEnabledSampleStreamWrapperCount);
        ImmutableList<HlsSampleStreamWrapper> enabledSampleStreamWrappersList = ImmutableList.copyOf(this.enabledSampleStreamWrappers);
        this.compositeSequenceableLoader = this.compositeSequenceableLoaderFactory.create(enabledSampleStreamWrappersList, Lists.transform(enabledSampleStreamWrappersList, new Function() { // from class: androidx.media3.exoplayer.hls.HlsMediaPeriod$$ExternalSyntheticLambda0
            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                return ((HlsSampleStreamWrapper) obj).getTrackGroups().getTrackTypes();
            }
        }));
        return positionUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public void discardBuffer(long positionUs, boolean toKeyframe) {
        for (HlsSampleStreamWrapper sampleStreamWrapper : this.enabledSampleStreamWrappers) {
            sampleStreamWrapper.discardBuffer(positionUs, toKeyframe);
        }
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public void reevaluateBuffer(long positionUs) {
        this.compositeSequenceableLoader.reevaluateBuffer(positionUs);
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod, androidx.media3.exoplayer.source.SequenceableLoader
    public boolean continueLoading(LoadingInfo loadingInfo) {
        if (this.trackGroups == null) {
            for (HlsSampleStreamWrapper wrapper : this.sampleStreamWrappers) {
                wrapper.continuePreparing();
            }
            return false;
        }
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
        if (this.enabledSampleStreamWrappers.length > 0) {
            boolean forceReset = this.enabledSampleStreamWrappers[0].seekToUs(positionUs, false);
            for (int i = 1; i < this.enabledSampleStreamWrappers.length; i++) {
                this.enabledSampleStreamWrappers[i].seekToUs(positionUs, forceReset);
            }
            if (forceReset) {
                this.timestampAdjusterProvider.reset();
            }
        }
        return positionUs;
    }

    @Override // androidx.media3.exoplayer.source.MediaPeriod
    public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
        for (HlsSampleStreamWrapper sampleStreamWrapper : this.enabledSampleStreamWrappers) {
            if (sampleStreamWrapper.isVideoSampleStream()) {
                long seekTargetUs = sampleStreamWrapper.getAdjustedSeekPositionUs(positionUs, seekParameters);
                return seekTargetUs;
            }
        }
        return positionUs;
    }

    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker.PlaylistEventListener
    public void onPlaylistChanged() {
        for (HlsSampleStreamWrapper streamWrapper : this.sampleStreamWrappers) {
            streamWrapper.onPlaylistUpdated();
        }
        this.mediaPeriodCallback.onContinueLoadingRequested(this);
    }

    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker.PlaylistEventListener
    public boolean onPlaylistError(Uri url, LoadErrorHandlingPolicy.LoadErrorInfo loadErrorInfo, boolean forceRetry) {
        boolean exclusionSucceeded = true;
        for (HlsSampleStreamWrapper streamWrapper : this.sampleStreamWrappers) {
            exclusionSucceeded &= streamWrapper.onPlaylistError(url, loadErrorInfo, forceRetry);
        }
        this.mediaPeriodCallback.onContinueLoadingRequested(this);
        return exclusionSucceeded;
    }

    private void buildAndPrepareSampleStreamWrappers(long positionUs) {
        Map<String, DrmInitData> overridingDrmInitData;
        HlsSampleStreamWrapper[] hlsSampleStreamWrapperArr;
        HlsMultivariantPlaylist multivariantPlaylist = (HlsMultivariantPlaylist) Assertions.checkNotNull(this.playlistTracker.getMultivariantPlaylist());
        if (this.useSessionKeys) {
            overridingDrmInitData = deriveOverridingDrmInitData(multivariantPlaylist.sessionKeyDrmInitData);
        } else {
            overridingDrmInitData = Collections.emptyMap();
        }
        int i = 1;
        boolean hasVariants = !multivariantPlaylist.variants.isEmpty();
        List<HlsMultivariantPlaylist.Rendition> audioRenditions = multivariantPlaylist.audios;
        List<HlsMultivariantPlaylist.Rendition> subtitleRenditions = multivariantPlaylist.subtitles;
        int i2 = 0;
        this.pendingPrepareCount = 0;
        ArrayList<HlsSampleStreamWrapper> sampleStreamWrappers = new ArrayList<>();
        ArrayList<int[]> manifestUrlIndicesPerWrapper = new ArrayList<>();
        if (hasVariants) {
            buildAndPrepareMainSampleStreamWrapper(multivariantPlaylist, positionUs, sampleStreamWrappers, manifestUrlIndicesPerWrapper, overridingDrmInitData);
        }
        buildAndPrepareAudioSampleStreamWrappers(positionUs, audioRenditions, sampleStreamWrappers, manifestUrlIndicesPerWrapper, overridingDrmInitData);
        this.audioVideoSampleStreamWrapperCount = sampleStreamWrappers.size();
        int i3 = 0;
        while (i3 < subtitleRenditions.size()) {
            HlsMultivariantPlaylist.Rendition subtitleRendition = subtitleRenditions.get(i3);
            String sampleStreamWrapperUid = "subtitle:" + i3 + ":" + subtitleRendition.name;
            Format originalSubtitleFormat = subtitleRendition.format;
            int i4 = i3;
            Uri[] uriArr = new Uri[i];
            uriArr[i2] = subtitleRendition.url;
            ArrayList<HlsSampleStreamWrapper> sampleStreamWrappers2 = sampleStreamWrappers;
            Format[] formatArr = new Format[i];
            formatArr[i2] = originalSubtitleFormat;
            Map<String, DrmInitData> overridingDrmInitData2 = overridingDrmInitData;
            ArrayList<int[]> manifestUrlIndicesPerWrapper2 = manifestUrlIndicesPerWrapper;
            int i5 = i2;
            HlsSampleStreamWrapper sampleStreamWrapper = buildSampleStreamWrapper(sampleStreamWrapperUid, 3, uriArr, formatArr, null, Collections.emptyList(), overridingDrmInitData2, positionUs);
            overridingDrmInitData = overridingDrmInitData2;
            manifestUrlIndicesPerWrapper2.add(new int[]{i4});
            sampleStreamWrappers2.add(sampleStreamWrapper);
            TrackGroup[] trackGroupArr = new TrackGroup[1];
            Format[] formatArr2 = new Format[1];
            formatArr2[i5] = this.extractorFactory.getOutputTextFormat(originalSubtitleFormat);
            trackGroupArr[i5] = new TrackGroup(sampleStreamWrapperUid, formatArr2);
            sampleStreamWrapper.prepareWithMultivariantPlaylistInfo(trackGroupArr, i5, new int[i5]);
            i3 = i4 + 1;
            manifestUrlIndicesPerWrapper = manifestUrlIndicesPerWrapper2;
            sampleStreamWrappers = sampleStreamWrappers2;
            hasVariants = hasVariants;
            i = 1;
            i2 = i5;
        }
        int i6 = i2;
        this.sampleStreamWrappers = (HlsSampleStreamWrapper[]) sampleStreamWrappers.toArray(new HlsSampleStreamWrapper[i6]);
        this.manifestUrlIndicesPerWrapper = (int[][]) manifestUrlIndicesPerWrapper.toArray(new int[i6][]);
        this.pendingPrepareCount = this.sampleStreamWrappers.length;
        int i7 = 0;
        while (true) {
            int i8 = this.audioVideoSampleStreamWrapperCount;
            hlsSampleStreamWrapperArr = this.sampleStreamWrappers;
            if (i7 >= i8) {
                break;
            }
            hlsSampleStreamWrapperArr[i7].setIsPrimaryTimestampSource(true);
            i7++;
        }
        int i9 = hlsSampleStreamWrapperArr.length;
        while (i6 < i9) {
            hlsSampleStreamWrapperArr[i6].continuePreparing();
            i6++;
        }
        this.enabledSampleStreamWrappers = this.sampleStreamWrappers;
    }

    private void buildAndPrepareMainSampleStreamWrapper(HlsMultivariantPlaylist multivariantPlaylist, long positionUs, List<HlsSampleStreamWrapper> sampleStreamWrappers, List<int[]> manifestUrlIndicesPerWrapper, Map<String, DrmInitData> overridingDrmInitData) {
        boolean useVideoVariantsOnly;
        boolean useNonAudioVariantsOnly;
        int selectedVariantsCount;
        int trackType;
        int[] variantTypes = new int[multivariantPlaylist.variants.size()];
        int videoVariantCount = 0;
        int audioVariantCount = 0;
        for (int i = 0; i < multivariantPlaylist.variants.size(); i++) {
            Format format = multivariantPlaylist.variants.get(i).format;
            if (format.height > 0 || Util.getCodecsOfType(format.codecs, 2) != null) {
                variantTypes[i] = 2;
                videoVariantCount++;
            } else if (Util.getCodecsOfType(format.codecs, 1) != null) {
                variantTypes[i] = 1;
                audioVariantCount++;
            } else {
                variantTypes[i] = -1;
            }
        }
        int selectedVariantsCount2 = variantTypes.length;
        if (videoVariantCount > 0) {
            int selectedVariantsCount3 = videoVariantCount;
            useVideoVariantsOnly = true;
            useNonAudioVariantsOnly = false;
            selectedVariantsCount = selectedVariantsCount3;
        } else if (audioVariantCount >= variantTypes.length) {
            useVideoVariantsOnly = false;
            useNonAudioVariantsOnly = false;
            selectedVariantsCount = selectedVariantsCount2;
        } else {
            int selectedVariantsCount4 = variantTypes.length - audioVariantCount;
            useVideoVariantsOnly = false;
            useNonAudioVariantsOnly = true;
            selectedVariantsCount = selectedVariantsCount4;
        }
        Uri[] selectedPlaylistUrls = new Uri[selectedVariantsCount];
        Format[] selectedPlaylistFormats = new Format[selectedVariantsCount];
        int[] selectedVariantIndices = new int[selectedVariantsCount];
        int outIndex = 0;
        for (int i2 = 0; i2 < multivariantPlaylist.variants.size(); i2++) {
            if ((!useVideoVariantsOnly || variantTypes[i2] == 2) && (!useNonAudioVariantsOnly || variantTypes[i2] != 1)) {
                HlsMultivariantPlaylist.Variant variant = multivariantPlaylist.variants.get(i2);
                selectedPlaylistUrls[outIndex] = variant.url;
                selectedPlaylistFormats[outIndex] = variant.format;
                selectedVariantIndices[outIndex] = i2;
                outIndex++;
            }
        }
        String codecs = selectedPlaylistFormats[0].codecs;
        int numberOfVideoCodecs = Util.getCodecCountOfType(codecs, 2);
        int numberOfAudioCodecs = Util.getCodecCountOfType(codecs, 1);
        boolean codecsStringAllowsChunklessPreparation = (numberOfAudioCodecs == 1 || (numberOfAudioCodecs == 0 && multivariantPlaylist.audios.isEmpty())) && numberOfVideoCodecs <= 1 && numberOfAudioCodecs + numberOfVideoCodecs > 0;
        if (!useVideoVariantsOnly && numberOfAudioCodecs > 0) {
            trackType = 1;
        } else {
            trackType = 0;
        }
        int selectedVariantsCount5 = selectedVariantsCount;
        HlsMediaPeriod hlsMediaPeriod = this;
        int trackType2 = trackType;
        HlsSampleStreamWrapper sampleStreamWrapper = hlsMediaPeriod.buildSampleStreamWrapper("main", trackType2, selectedPlaylistUrls, selectedPlaylistFormats, multivariantPlaylist.muxedAudioFormat, multivariantPlaylist.muxedCaptionFormats, overridingDrmInitData, positionUs);
        sampleStreamWrappers.add(sampleStreamWrapper);
        manifestUrlIndicesPerWrapper.add(selectedVariantIndices);
        if (hlsMediaPeriod.allowChunklessPreparation && codecsStringAllowsChunklessPreparation) {
            List<TrackGroup> muxedTrackGroups = new ArrayList<>();
            if (numberOfVideoCodecs > 0) {
                Format[] videoFormats = new Format[selectedVariantsCount5];
                int i3 = 0;
                while (true) {
                    int trackType3 = trackType2;
                    int trackType4 = videoFormats.length;
                    if (i3 >= trackType4) {
                        break;
                    }
                    videoFormats[i3] = deriveVideoFormat(selectedPlaylistFormats[i3]);
                    i3++;
                    trackType2 = trackType3;
                }
                muxedTrackGroups.add(new TrackGroup("main", videoFormats));
                if (numberOfAudioCodecs > 0 && (multivariantPlaylist.muxedAudioFormat != null || multivariantPlaylist.audios.isEmpty())) {
                    muxedTrackGroups.add(new TrackGroup("main:audio", deriveAudioFormat(selectedPlaylistFormats[0], multivariantPlaylist.muxedAudioFormat, false)));
                }
                List<Format> ccFormats = multivariantPlaylist.muxedCaptionFormats;
                if (ccFormats != null) {
                    int i4 = 0;
                    while (i4 < ccFormats.size()) {
                        String ccId = "main:cc:" + i4;
                        muxedTrackGroups.add(new TrackGroup(ccId, hlsMediaPeriod.extractorFactory.getOutputTextFormat(ccFormats.get(i4))));
                        i4++;
                        hlsMediaPeriod = this;
                    }
                }
            } else {
                Format[] audioFormats = new Format[selectedVariantsCount5];
                for (int i5 = 0; i5 < audioFormats.length; i5++) {
                    audioFormats[i5] = deriveAudioFormat(selectedPlaylistFormats[i5], multivariantPlaylist.muxedAudioFormat, true);
                }
                muxedTrackGroups.add(new TrackGroup("main", audioFormats));
            }
            TrackGroup id3TrackGroup = new TrackGroup("main:id3", new Format.Builder().setId("ID3").setSampleMimeType(MimeTypes.APPLICATION_ID3).build());
            muxedTrackGroups.add(id3TrackGroup);
            sampleStreamWrapper.prepareWithMultivariantPlaylistInfo((TrackGroup[]) muxedTrackGroups.toArray(new TrackGroup[0]), 0, muxedTrackGroups.indexOf(id3TrackGroup));
        }
    }

    private void buildAndPrepareAudioSampleStreamWrappers(long positionUs, List<HlsMultivariantPlaylist.Rendition> audioRenditions, List<HlsSampleStreamWrapper> sampleStreamWrappers, List<int[]> manifestUrlsIndicesPerWrapper, Map<String, DrmInitData> overridingDrmInitData) {
        List<HlsMultivariantPlaylist.Rendition> list = audioRenditions;
        ArrayList<Uri> scratchPlaylistUrls = new ArrayList<>(list.size());
        ArrayList<Format> scratchPlaylistFormats = new ArrayList<>(list.size());
        ArrayList<Integer> scratchIndicesList = new ArrayList<>(list.size());
        HashSet<String> alreadyGroupedNames = new HashSet<>();
        int renditionByNameIndex = 0;
        while (renditionByNameIndex < list.size()) {
            String name = list.get(renditionByNameIndex).name;
            if (alreadyGroupedNames.add(name)) {
                boolean codecStringsAllowChunklessPreparation = true;
                scratchPlaylistUrls.clear();
                scratchPlaylistFormats.clear();
                scratchIndicesList.clear();
                int renditionIndex = 0;
                while (true) {
                    if (renditionIndex >= list.size()) {
                        break;
                    }
                    if (Util.areEqual(name, list.get(renditionIndex).name)) {
                        HlsMultivariantPlaylist.Rendition rendition = list.get(renditionIndex);
                        scratchIndicesList.add(Integer.valueOf(renditionIndex));
                        scratchPlaylistUrls.add(rendition.url);
                        scratchPlaylistFormats.add(rendition.format);
                        codecStringsAllowChunklessPreparation &= Util.getCodecCountOfType(rendition.format.codecs, 1) == 1;
                    }
                    renditionIndex++;
                }
                String sampleStreamWrapperUid = "audio:" + name;
                HlsSampleStreamWrapper sampleStreamWrapper = buildSampleStreamWrapper(sampleStreamWrapperUid, 1, (Uri[]) scratchPlaylistUrls.toArray((Uri[]) Util.castNonNullTypeArray(new Uri[0])), (Format[]) scratchPlaylistFormats.toArray(new Format[0]), null, Collections.emptyList(), overridingDrmInitData, positionUs);
                manifestUrlsIndicesPerWrapper.add(Ints.toArray(scratchIndicesList));
                sampleStreamWrappers.add(sampleStreamWrapper);
                if (this.allowChunklessPreparation && codecStringsAllowChunklessPreparation) {
                    Format[] renditionFormats = (Format[]) scratchPlaylistFormats.toArray(new Format[0]);
                    sampleStreamWrapper.prepareWithMultivariantPlaylistInfo(new TrackGroup[]{new TrackGroup(sampleStreamWrapperUid, renditionFormats)}, 0, new int[0]);
                }
            }
            renditionByNameIndex++;
            list = audioRenditions;
        }
    }

    private HlsSampleStreamWrapper buildSampleStreamWrapper(String uid, int trackType, Uri[] playlistUrls, Format[] playlistFormats, Format muxedAudioFormat, List<Format> muxedCaptionFormats, Map<String, DrmInitData> overridingDrmInitData, long positionUs) {
        HlsChunkSource defaultChunkSource = new HlsChunkSource(this.extractorFactory, this.playlistTracker, playlistUrls, playlistFormats, this.dataSourceFactory, this.mediaTransferListener, this.timestampAdjusterProvider, this.timestampAdjusterInitializationTimeoutMs, muxedCaptionFormats, this.playerId, this.cmcdConfiguration);
        return new HlsSampleStreamWrapper(uid, trackType, this.sampleStreamWrapperCallback, defaultChunkSource, overridingDrmInitData, this.allocator, positionUs, muxedAudioFormat, this.drmSessionManager, this.drmEventDispatcher, this.loadErrorHandlingPolicy, this.eventDispatcher, this.metadataType);
    }

    private static Map<String, DrmInitData> deriveOverridingDrmInitData(List<DrmInitData> sessionKeyDrmInitData) {
        ArrayList<DrmInitData> mutableSessionKeyDrmInitData = new ArrayList<>(sessionKeyDrmInitData);
        HashMap<String, DrmInitData> drmInitDataBySchemeType = new HashMap<>();
        for (int i = 0; i < mutableSessionKeyDrmInitData.size(); i++) {
            DrmInitData drmInitData = sessionKeyDrmInitData.get(i);
            String scheme = drmInitData.schemeType;
            int j = i + 1;
            while (j < mutableSessionKeyDrmInitData.size()) {
                DrmInitData nextDrmInitData = mutableSessionKeyDrmInitData.get(j);
                if (TextUtils.equals(nextDrmInitData.schemeType, scheme)) {
                    drmInitData = drmInitData.merge(nextDrmInitData);
                    mutableSessionKeyDrmInitData.remove(j);
                } else {
                    j++;
                }
            }
            drmInitDataBySchemeType.put(scheme, drmInitData);
        }
        return drmInitDataBySchemeType;
    }

    private static Format deriveVideoFormat(Format variantFormat) {
        String codecs = Util.getCodecsOfType(variantFormat.codecs, 2);
        String sampleMimeType = MimeTypes.getMediaMimeType(codecs);
        return new Format.Builder().setId(variantFormat.id).setLabel(variantFormat.label).setLabels(variantFormat.labels).setContainerMimeType(variantFormat.containerMimeType).setSampleMimeType(sampleMimeType).setCodecs(codecs).setMetadata(variantFormat.metadata).setAverageBitrate(variantFormat.averageBitrate).setPeakBitrate(variantFormat.peakBitrate).setWidth(variantFormat.width).setHeight(variantFormat.height).setFrameRate(variantFormat.frameRate).setSelectionFlags(variantFormat.selectionFlags).setRoleFlags(variantFormat.roleFlags).build();
    }

    private static Format deriveAudioFormat(Format variantFormat, Format mediaTagFormat, boolean isPrimaryTrackInVariant) {
        String codecs;
        Metadata metadata;
        int channelCount = -1;
        int selectionFlags = 0;
        int roleFlags = 0;
        String language = null;
        String label = null;
        List<Label> labels = ImmutableList.of();
        if (mediaTagFormat != null) {
            codecs = mediaTagFormat.codecs;
            metadata = mediaTagFormat.metadata;
            channelCount = mediaTagFormat.channelCount;
            selectionFlags = mediaTagFormat.selectionFlags;
            roleFlags = mediaTagFormat.roleFlags;
            language = mediaTagFormat.language;
            label = mediaTagFormat.label;
            labels = mediaTagFormat.labels;
        } else {
            codecs = Util.getCodecsOfType(variantFormat.codecs, 1);
            metadata = variantFormat.metadata;
            if (isPrimaryTrackInVariant) {
                channelCount = variantFormat.channelCount;
                selectionFlags = variantFormat.selectionFlags;
                roleFlags = variantFormat.roleFlags;
                language = variantFormat.language;
                label = variantFormat.label;
                labels = variantFormat.labels;
            }
        }
        String sampleMimeType = MimeTypes.getMediaMimeType(codecs);
        int averageBitrate = isPrimaryTrackInVariant ? variantFormat.averageBitrate : -1;
        int peakBitrate = isPrimaryTrackInVariant ? variantFormat.peakBitrate : -1;
        return new Format.Builder().setId(variantFormat.id).setLabel(label).setLabels(labels).setContainerMimeType(variantFormat.containerMimeType).setSampleMimeType(sampleMimeType).setCodecs(codecs).setMetadata(metadata).setAverageBitrate(averageBitrate).setPeakBitrate(peakBitrate).setChannelCount(channelCount).setSelectionFlags(selectionFlags).setRoleFlags(roleFlags).setLanguage(language).build();
    }

    private class SampleStreamWrapperCallback implements HlsSampleStreamWrapper.Callback {
        private SampleStreamWrapperCallback() {
        }

        @Override // androidx.media3.exoplayer.hls.HlsSampleStreamWrapper.Callback
        public void onPrepared() {
            if (HlsMediaPeriod.access$106(HlsMediaPeriod.this) > 0) {
                return;
            }
            int totalTrackGroupCount = 0;
            for (HlsSampleStreamWrapper hlsSampleStreamWrapper : HlsMediaPeriod.this.sampleStreamWrappers) {
                totalTrackGroupCount += hlsSampleStreamWrapper.getTrackGroups().length;
            }
            TrackGroup[] trackGroupArray = new TrackGroup[totalTrackGroupCount];
            int trackGroupIndex = 0;
            for (HlsSampleStreamWrapper sampleStreamWrapper : HlsMediaPeriod.this.sampleStreamWrappers) {
                int wrapperTrackGroupCount = sampleStreamWrapper.getTrackGroups().length;
                int j = 0;
                while (j < wrapperTrackGroupCount) {
                    trackGroupArray[trackGroupIndex] = sampleStreamWrapper.getTrackGroups().get(j);
                    j++;
                    trackGroupIndex++;
                }
            }
            HlsMediaPeriod.this.trackGroups = new TrackGroupArray(trackGroupArray);
            HlsMediaPeriod.this.mediaPeriodCallback.onPrepared(HlsMediaPeriod.this);
        }

        @Override // androidx.media3.exoplayer.hls.HlsSampleStreamWrapper.Callback
        public void onPlaylistRefreshRequired(Uri url) {
            HlsMediaPeriod.this.playlistTracker.refreshPlaylist(url);
        }

        @Override // androidx.media3.exoplayer.source.SequenceableLoader.Callback
        public void onContinueLoadingRequested(HlsSampleStreamWrapper sampleStreamWrapper) {
            HlsMediaPeriod.this.mediaPeriodCallback.onContinueLoadingRequested(HlsMediaPeriod.this);
        }
    }
}
