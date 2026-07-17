package androidx.media3.exoplayer.trackselection;

import android.os.SystemClock;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class TrackSelectionUtil {

    public interface AdaptiveTrackSelectionFactory {
        ExoTrackSelection createAdaptiveTrackSelection(ExoTrackSelection.Definition definition);
    }

    private TrackSelectionUtil() {
    }

    public static ExoTrackSelection[] createTrackSelectionsForDefinitions(ExoTrackSelection.Definition[] definitions, AdaptiveTrackSelectionFactory adaptiveTrackSelectionFactory) {
        ExoTrackSelection[] selections = new ExoTrackSelection[definitions.length];
        boolean createdAdaptiveTrackSelection = false;
        for (int i = 0; i < definitions.length; i++) {
            ExoTrackSelection.Definition definition = definitions[i];
            if (definition != null) {
                if (definition.tracks.length > 1 && !createdAdaptiveTrackSelection) {
                    createdAdaptiveTrackSelection = true;
                    selections[i] = adaptiveTrackSelectionFactory.createAdaptiveTrackSelection(definition);
                } else {
                    selections[i] = new FixedTrackSelection(definition.group, definition.tracks[0], definition.type);
                }
            }
        }
        return selections;
    }

    @Deprecated
    public static DefaultTrackSelector.Parameters updateParametersWithOverride(DefaultTrackSelector.Parameters parameters, int rendererIndex, TrackGroupArray trackGroupArray, boolean isDisabled, DefaultTrackSelector.SelectionOverride override) {
        DefaultTrackSelector.Parameters.Builder builder = parameters.buildUpon().clearSelectionOverrides(rendererIndex).setRendererDisabled(rendererIndex, isDisabled);
        if (override != null) {
            builder.setSelectionOverride(rendererIndex, trackGroupArray, override);
        }
        return builder.build();
    }

    public static LoadErrorHandlingPolicy.FallbackOptions createFallbackOptions(ExoTrackSelection trackSelection) {
        long nowMs = SystemClock.elapsedRealtime();
        int numberOfTracks = trackSelection.length();
        int numberOfExcludedTracks = 0;
        for (int i = 0; i < numberOfTracks; i++) {
            if (trackSelection.isTrackExcluded(i, nowMs)) {
                numberOfExcludedTracks++;
            }
        }
        return new LoadErrorHandlingPolicy.FallbackOptions(1, 0, numberOfTracks, numberOfExcludedTracks);
    }

    public static Tracks buildTracks(MappingTrackSelector.MappedTrackInfo mappedTrackInfo, TrackSelection[] selections) {
        List<? extends TrackSelection>[] listSelections = new List[selections.length];
        for (int i = 0; i < selections.length; i++) {
            TrackSelection selection = selections[i];
            listSelections[i] = selection != null ? ImmutableList.of(selection) : ImmutableList.of();
        }
        return buildTracks(mappedTrackInfo, listSelections);
    }

    public static Tracks buildTracks(MappingTrackSelector.MappedTrackInfo mappedTrackInfo, List<? extends TrackSelection>[] selections) {
        ImmutableList.Builder<Tracks.Group> trackGroups = new ImmutableList.Builder<>();
        int rendererIndex = 0;
        while (true) {
            boolean z = false;
            if (rendererIndex >= mappedTrackInfo.getRendererCount()) {
                break;
            }
            TrackGroupArray trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex);
            List<? extends TrackSelection> rendererTrackSelections = selections[rendererIndex];
            int groupIndex = 0;
            while (groupIndex < trackGroupArray.length) {
                TrackGroup trackGroup = trackGroupArray.get(groupIndex);
                boolean adaptiveSupported = mappedTrackInfo.getAdaptiveSupport(rendererIndex, groupIndex, z) != 0 ? true : z;
                int[] trackSupport = new int[trackGroup.length];
                boolean[] selected = new boolean[trackGroup.length];
                for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                    trackSupport[trackIndex] = mappedTrackInfo.getTrackSupport(rendererIndex, groupIndex, trackIndex);
                    boolean isTrackSelected = false;
                    for (int i = 0; i < rendererTrackSelections.size(); i++) {
                        TrackSelection trackSelection = rendererTrackSelections.get(i);
                        if (trackSelection.getTrackGroup().equals(trackGroup) && trackSelection.indexOf(trackIndex) != -1) {
                            isTrackSelected = true;
                            break;
                        }
                    }
                    selected[trackIndex] = isTrackSelected;
                }
                trackGroups.add(new Tracks.Group(trackGroup, adaptiveSupported, trackSupport, selected));
                groupIndex++;
                z = false;
            }
            rendererIndex++;
        }
        TrackGroupArray unmappedTrackGroups = mappedTrackInfo.getUnmappedTrackGroups();
        for (int groupIndex2 = 0; groupIndex2 < unmappedTrackGroups.length; groupIndex2++) {
            TrackGroup trackGroup2 = unmappedTrackGroups.get(groupIndex2);
            int[] trackSupport2 = new int[trackGroup2.length];
            Arrays.fill(trackSupport2, 0);
            trackGroups.add(new Tracks.Group(trackGroup2, false, trackSupport2, new boolean[trackGroup2.length]));
        }
        return new Tracks(trackGroups.build());
    }
}
