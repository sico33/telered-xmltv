package androidx.media3.exoplayer.trackselection;

import android.util.Pair;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.RendererCapabilities;
import androidx.media3.exoplayer.RendererConfiguration;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.TrackGroupArray;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public abstract class MappingTrackSelector extends TrackSelector {
    private MappedTrackInfo currentMappedTrackInfo;

    protected abstract Pair<RendererConfiguration[], ExoTrackSelection[]> selectTracks(MappedTrackInfo mappedTrackInfo, int[][][] iArr, int[] iArr2, MediaSource.MediaPeriodId mediaPeriodId, Timeline timeline) throws ExoPlaybackException;

    public static final class MappedTrackInfo {
        public static final int RENDERER_SUPPORT_EXCEEDS_CAPABILITIES_TRACKS = 2;
        public static final int RENDERER_SUPPORT_NO_TRACKS = 0;
        public static final int RENDERER_SUPPORT_PLAYABLE_TRACKS = 3;
        public static final int RENDERER_SUPPORT_UNSUPPORTED_TRACKS = 1;
        private final int rendererCount;
        private final int[][][] rendererFormatSupports;
        private final int[] rendererMixedMimeTypeAdaptiveSupports;
        private final String[] rendererNames;
        private final TrackGroupArray[] rendererTrackGroups;
        private final int[] rendererTrackTypes;
        private final TrackGroupArray unmappedTrackGroups;

        @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
        @Documented
        @Retention(RetentionPolicy.SOURCE)
        public @interface RendererSupport {
        }

        MappedTrackInfo(String[] rendererNames, int[] rendererTrackTypes, TrackGroupArray[] rendererTrackGroups, int[] rendererMixedMimeTypeAdaptiveSupports, int[][][] rendererFormatSupports, TrackGroupArray unmappedTrackGroups) {
            this.rendererNames = rendererNames;
            this.rendererTrackTypes = rendererTrackTypes;
            this.rendererTrackGroups = rendererTrackGroups;
            this.rendererFormatSupports = rendererFormatSupports;
            this.rendererMixedMimeTypeAdaptiveSupports = rendererMixedMimeTypeAdaptiveSupports;
            this.unmappedTrackGroups = unmappedTrackGroups;
            this.rendererCount = rendererTrackTypes.length;
        }

        public int getRendererCount() {
            return this.rendererCount;
        }

        public String getRendererName(int rendererIndex) {
            return this.rendererNames[rendererIndex];
        }

        public int getRendererType(int rendererIndex) {
            return this.rendererTrackTypes[rendererIndex];
        }

        public TrackGroupArray getTrackGroups(int rendererIndex) {
            return this.rendererTrackGroups[rendererIndex];
        }

        public int getRendererSupport(int rendererIndex) {
            int trackRendererSupport;
            int bestRendererSupport = 0;
            int[][] rendererFormatSupport = this.rendererFormatSupports[rendererIndex];
            for (int[] trackGroupFormatSupport : rendererFormatSupport) {
                for (int trackFormatSupport : trackGroupFormatSupport) {
                    switch (RendererCapabilities.CC.getFormatSupport(trackFormatSupport)) {
                        case 0:
                        case 1:
                        case 2:
                            trackRendererSupport = 1;
                            break;
                        case 3:
                            trackRendererSupport = 2;
                            break;
                        case 4:
                            return 3;
                        default:
                            throw new IllegalStateException();
                    }
                    bestRendererSupport = Math.max(bestRendererSupport, trackRendererSupport);
                }
            }
            return bestRendererSupport;
        }

        public int getTypeSupport(int trackType) {
            int bestRendererSupport = 0;
            for (int i = 0; i < this.rendererCount; i++) {
                if (this.rendererTrackTypes[i] == trackType) {
                    bestRendererSupport = Math.max(bestRendererSupport, getRendererSupport(i));
                }
            }
            return bestRendererSupport;
        }

        public int getCapabilities(int rendererIndex, int groupIndex, int trackIndex) {
            return this.rendererFormatSupports[rendererIndex][groupIndex][trackIndex];
        }

        public int getTrackSupport(int rendererIndex, int groupIndex, int trackIndex) {
            return RendererCapabilities.CC.getFormatSupport(getCapabilities(rendererIndex, groupIndex, trackIndex));
        }

        public int getAdaptiveSupport(int rendererIndex, int groupIndex, boolean includeCapabilitiesExceededTracks) {
            int trackCount = this.rendererTrackGroups[rendererIndex].get(groupIndex).length;
            int[] trackIndices = new int[trackCount];
            int trackIndexCount = 0;
            for (int i = 0; i < trackCount; i++) {
                int fixedSupport = getTrackSupport(rendererIndex, groupIndex, i);
                if (fixedSupport == 4 || (includeCapabilitiesExceededTracks && fixedSupport == 3)) {
                    trackIndices[trackIndexCount] = i;
                    trackIndexCount++;
                }
            }
            return getAdaptiveSupport(rendererIndex, groupIndex, Arrays.copyOf(trackIndices, trackIndexCount));
        }

        public int getAdaptiveSupport(int rendererIndex, int groupIndex, int[] trackIndices) {
            int handledTrackCount = 0;
            int adaptiveSupport = 16;
            boolean multipleMimeTypes = false;
            String firstSampleMimeType = null;
            int i = 0;
            while (i < trackIndices.length) {
                int trackIndex = trackIndices[i];
                String sampleMimeType = this.rendererTrackGroups[rendererIndex].get(groupIndex).getFormat(trackIndex).sampleMimeType;
                int handledTrackCount2 = handledTrackCount + 1;
                if (handledTrackCount == 0) {
                    firstSampleMimeType = sampleMimeType;
                } else {
                    multipleMimeTypes = (!Util.areEqual(firstSampleMimeType, sampleMimeType)) | multipleMimeTypes;
                }
                adaptiveSupport = Math.min(adaptiveSupport, RendererCapabilities.CC.getAdaptiveSupport(this.rendererFormatSupports[rendererIndex][groupIndex][i]));
                i++;
                handledTrackCount = handledTrackCount2;
            }
            if (multipleMimeTypes) {
                return Math.min(adaptiveSupport, this.rendererMixedMimeTypeAdaptiveSupports[rendererIndex]);
            }
            return adaptiveSupport;
        }

        public TrackGroupArray getUnmappedTrackGroups() {
            return this.unmappedTrackGroups;
        }
    }

    public final MappedTrackInfo getCurrentMappedTrackInfo() {
        return this.currentMappedTrackInfo;
    }

    @Override // androidx.media3.exoplayer.trackselection.TrackSelector
    public final void onSelectionActivated(Object info) {
        this.currentMappedTrackInfo = (MappedTrackInfo) info;
    }

    @Override // androidx.media3.exoplayer.trackselection.TrackSelector
    public final TrackSelectorResult selectTracks(RendererCapabilities[] rendererCapabilities, TrackGroupArray trackGroups, MediaSource.MediaPeriodId periodId, Timeline timeline) throws ExoPlaybackException {
        int[] rendererFormatSupport;
        int[] rendererTrackGroupCounts = new int[rendererCapabilities.length + 1];
        TrackGroup[][] rendererTrackGroups = new TrackGroup[rendererCapabilities.length + 1][];
        int[][][] rendererFormatSupports = new int[rendererCapabilities.length + 1][][];
        for (int i = 0; i < rendererTrackGroups.length; i++) {
            rendererTrackGroups[i] = new TrackGroup[trackGroups.length];
            rendererFormatSupports[i] = new int[trackGroups.length][];
        }
        int[] rendererMixedMimeTypeAdaptationSupports = getMixedMimeTypeAdaptationSupports(rendererCapabilities);
        for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
            TrackGroup group = trackGroups.get(groupIndex);
            boolean preferUnassociatedRenderer = group.type == 5;
            int rendererIndex = findRenderer(rendererCapabilities, group, rendererTrackGroupCounts, preferUnassociatedRenderer);
            if (rendererIndex == rendererCapabilities.length) {
                rendererFormatSupport = new int[group.length];
            } else {
                rendererFormatSupport = getFormatSupport(rendererCapabilities[rendererIndex], group);
            }
            int rendererTrackGroupCount = rendererTrackGroupCounts[rendererIndex];
            rendererTrackGroups[rendererIndex][rendererTrackGroupCount] = group;
            rendererFormatSupports[rendererIndex][rendererTrackGroupCount] = rendererFormatSupport;
            rendererTrackGroupCounts[rendererIndex] = rendererTrackGroupCounts[rendererIndex] + 1;
        }
        TrackGroupArray[] rendererTrackGroupArrays = new TrackGroupArray[rendererCapabilities.length];
        String[] rendererNames = new String[rendererCapabilities.length];
        int[] rendererTrackTypes = new int[rendererCapabilities.length];
        for (int i2 = 0; i2 < rendererCapabilities.length; i2++) {
            int rendererTrackGroupCount2 = rendererTrackGroupCounts[i2];
            rendererTrackGroupArrays[i2] = new TrackGroupArray((TrackGroup[]) Util.nullSafeArrayCopy(rendererTrackGroups[i2], rendererTrackGroupCount2));
            rendererFormatSupports[i2] = (int[][]) Util.nullSafeArrayCopy(rendererFormatSupports[i2], rendererTrackGroupCount2);
            rendererNames[i2] = rendererCapabilities[i2].getName();
            rendererTrackTypes[i2] = rendererCapabilities[i2].getTrackType();
        }
        int i3 = rendererCapabilities.length;
        int unmappedTrackGroupCount = rendererTrackGroupCounts[i3];
        TrackGroupArray unmappedTrackGroupArray = new TrackGroupArray((TrackGroup[]) Util.nullSafeArrayCopy(rendererTrackGroups[rendererCapabilities.length], unmappedTrackGroupCount));
        MappedTrackInfo mappedTrackInfo = new MappedTrackInfo(rendererNames, rendererTrackTypes, rendererTrackGroupArrays, rendererMixedMimeTypeAdaptationSupports, rendererFormatSupports, unmappedTrackGroupArray);
        Pair<RendererConfiguration[], ExoTrackSelection[]> result = selectTracks(mappedTrackInfo, rendererFormatSupports, rendererMixedMimeTypeAdaptationSupports, periodId, timeline);
        Tracks tracks = TrackSelectionUtil.buildTracks(mappedTrackInfo, (TrackSelection[]) result.second);
        return new TrackSelectorResult((RendererConfiguration[]) result.first, (ExoTrackSelection[]) result.second, tracks, mappedTrackInfo);
    }

    private static int findRenderer(RendererCapabilities[] rendererCapabilities, TrackGroup group, int[] rendererTrackGroupCounts, boolean preferUnassociatedRenderer) throws ExoPlaybackException {
        int bestRendererIndex = rendererCapabilities.length;
        int bestFormatSupportLevel = 0;
        boolean bestRendererIsUnassociated = true;
        for (int rendererIndex = 0; rendererIndex < rendererCapabilities.length; rendererIndex++) {
            RendererCapabilities rendererCapability = rendererCapabilities[rendererIndex];
            int formatSupportLevel = 0;
            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                int trackFormatSupportLevel = RendererCapabilities.CC.getFormatSupport(rendererCapability.supportsFormat(group.getFormat(trackIndex)));
                formatSupportLevel = Math.max(formatSupportLevel, trackFormatSupportLevel);
            }
            int trackIndex2 = rendererTrackGroupCounts[rendererIndex];
            boolean rendererIsUnassociated = trackIndex2 == 0;
            if (formatSupportLevel > bestFormatSupportLevel || (formatSupportLevel == bestFormatSupportLevel && preferUnassociatedRenderer && !bestRendererIsUnassociated && rendererIsUnassociated)) {
                bestRendererIndex = rendererIndex;
                bestFormatSupportLevel = formatSupportLevel;
                bestRendererIsUnassociated = rendererIsUnassociated;
            }
        }
        return bestRendererIndex;
    }

    private static int[] getFormatSupport(RendererCapabilities rendererCapabilities, TrackGroup group) throws ExoPlaybackException {
        int[] formatSupport = new int[group.length];
        for (int i = 0; i < group.length; i++) {
            formatSupport[i] = rendererCapabilities.supportsFormat(group.getFormat(i));
        }
        return formatSupport;
    }

    private static int[] getMixedMimeTypeAdaptationSupports(RendererCapabilities[] rendererCapabilities) throws ExoPlaybackException {
        int[] mixedMimeTypeAdaptationSupport = new int[rendererCapabilities.length];
        for (int i = 0; i < mixedMimeTypeAdaptationSupport.length; i++) {
            mixedMimeTypeAdaptationSupport[i] = rendererCapabilities[i].supportsMixedMimeTypeAdaptation();
        }
        return mixedMimeTypeAdaptationSupport;
    }
}
