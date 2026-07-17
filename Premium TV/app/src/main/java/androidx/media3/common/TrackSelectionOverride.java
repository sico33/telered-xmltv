package androidx.media3.common;

import android.os.Bundle;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class TrackSelectionOverride {
    public final TrackGroup mediaTrackGroup;
    public final ImmutableList<Integer> trackIndices;
    private static final String FIELD_TRACK_GROUP = Util.intToStringMaxRadix(0);
    private static final String FIELD_TRACKS = Util.intToStringMaxRadix(1);

    public TrackSelectionOverride(TrackGroup mediaTrackGroup, int trackIndex) {
        this(mediaTrackGroup, ImmutableList.of(Integer.valueOf(trackIndex)));
    }

    public TrackSelectionOverride(TrackGroup mediaTrackGroup, List<Integer> trackIndices) {
        if (!trackIndices.isEmpty() && (((Integer) Collections.min(trackIndices)).intValue() < 0 || ((Integer) Collections.max(trackIndices)).intValue() >= mediaTrackGroup.length)) {
            throw new IndexOutOfBoundsException();
        }
        this.mediaTrackGroup = mediaTrackGroup;
        this.trackIndices = ImmutableList.copyOf((Collection) trackIndices);
    }

    public int getType() {
        return this.mediaTrackGroup.type;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TrackSelectionOverride that = (TrackSelectionOverride) obj;
        if (this.mediaTrackGroup.equals(that.mediaTrackGroup) && this.trackIndices.equals(that.trackIndices)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return this.mediaTrackGroup.hashCode() + (this.trackIndices.hashCode() * 31);
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putBundle(FIELD_TRACK_GROUP, this.mediaTrackGroup.toBundle());
        bundle.putIntArray(FIELD_TRACKS, Ints.toArray(this.trackIndices));
        return bundle;
    }

    public static TrackSelectionOverride fromBundle(Bundle bundle) {
        Bundle trackGroupBundle = (Bundle) Assertions.checkNotNull(bundle.getBundle(FIELD_TRACK_GROUP));
        TrackGroup mediaTrackGroup = TrackGroup.fromBundle(trackGroupBundle);
        int[] tracks = (int[]) Assertions.checkNotNull(bundle.getIntArray(FIELD_TRACKS));
        return new TrackSelectionOverride(mediaTrackGroup, Ints.asList(tracks));
    }
}
