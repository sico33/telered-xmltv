package androidx.media3.exoplayer.trackselection;

import androidx.media3.common.Tracks;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.RendererConfiguration;

/* JADX INFO: loaded from: classes.dex */
public final class TrackSelectorResult {
    public final Object info;
    public final int length;
    public final RendererConfiguration[] rendererConfigurations;
    public final ExoTrackSelection[] selections;
    public final Tracks tracks;

    @Deprecated
    public TrackSelectorResult(RendererConfiguration[] rendererConfigurations, ExoTrackSelection[] selections, Object info) {
        this(rendererConfigurations, selections, Tracks.EMPTY, info);
    }

    public TrackSelectorResult(RendererConfiguration[] rendererConfigurations, ExoTrackSelection[] selections, Tracks tracks, Object info) {
        Assertions.checkArgument(rendererConfigurations.length == selections.length);
        this.rendererConfigurations = rendererConfigurations;
        this.selections = (ExoTrackSelection[]) selections.clone();
        this.tracks = tracks;
        this.info = info;
        this.length = rendererConfigurations.length;
    }

    public boolean isRendererEnabled(int index) {
        return this.rendererConfigurations[index] != null;
    }

    public boolean isEquivalent(TrackSelectorResult other) {
        if (other == null || other.selections.length != this.selections.length) {
            return false;
        }
        for (int i = 0; i < this.selections.length; i++) {
            if (!isEquivalent(other, i)) {
                return false;
            }
        }
        return true;
    }

    public boolean isEquivalent(TrackSelectorResult other, int index) {
        return other != null && Util.areEqual(this.rendererConfigurations[index], other.rendererConfigurations[index]) && Util.areEqual(this.selections[index], other.selections[index]);
    }
}
