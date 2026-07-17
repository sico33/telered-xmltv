package androidx.media3.exoplayer.source;

import android.os.Bundle;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.BundleCollectionUtil;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class TrackGroupArray {
    public static final TrackGroupArray EMPTY = new TrackGroupArray(new TrackGroup[0]);
    private static final String FIELD_TRACK_GROUPS = Util.intToStringMaxRadix(0);
    private static final String TAG = "TrackGroupArray";
    private int hashCode;
    public final int length;
    private final ImmutableList<TrackGroup> trackGroups;

    public TrackGroupArray(TrackGroup... trackGroups) {
        this.trackGroups = ImmutableList.copyOf(trackGroups);
        this.length = trackGroups.length;
        verifyCorrectness();
    }

    public TrackGroup get(int index) {
        return this.trackGroups.get(index);
    }

    public int indexOf(TrackGroup group) {
        int index = this.trackGroups.indexOf(group);
        if (index >= 0) {
            return index;
        }
        return -1;
    }

    public boolean isEmpty() {
        return this.length == 0;
    }

    public ImmutableList<Integer> getTrackTypes() {
        return ImmutableList.copyOf((Collection) Lists.transform(this.trackGroups, new Function() { // from class: androidx.media3.exoplayer.source.TrackGroupArray$$ExternalSyntheticLambda0
            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                return Integer.valueOf(((TrackGroup) obj).type);
            }
        }));
    }

    public int hashCode() {
        if (this.hashCode == 0) {
            this.hashCode = this.trackGroups.hashCode();
        }
        return this.hashCode;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TrackGroupArray other = (TrackGroupArray) obj;
        if (this.length == other.length && this.trackGroups.equals(other.trackGroups)) {
            return true;
        }
        return false;
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(FIELD_TRACK_GROUPS, BundleCollectionUtil.toBundleArrayList(this.trackGroups, new Function() { // from class: androidx.media3.exoplayer.source.TrackGroupArray$$ExternalSyntheticLambda1
            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                return ((TrackGroup) obj).toBundle();
            }
        }));
        return bundle;
    }

    public static TrackGroupArray fromBundle(Bundle bundle) {
        List<Bundle> trackGroupBundles = bundle.getParcelableArrayList(FIELD_TRACK_GROUPS);
        if (trackGroupBundles == null) {
            return new TrackGroupArray(new TrackGroup[0]);
        }
        return new TrackGroupArray((TrackGroup[]) BundleCollectionUtil.fromBundleList(new Function() { // from class: androidx.media3.exoplayer.source.TrackGroupArray$$ExternalSyntheticLambda2
            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                return TrackGroup.fromBundle((Bundle) obj);
            }
        }, trackGroupBundles).toArray(new TrackGroup[0]));
    }

    private void verifyCorrectness() {
        for (int i = 0; i < this.trackGroups.size(); i++) {
            for (int j = i + 1; j < this.trackGroups.size(); j++) {
                if (this.trackGroups.get(i).equals(this.trackGroups.get(j))) {
                    Log.e(TAG, "", new IllegalArgumentException("Multiple identical TrackGroups added to one TrackGroupArray."));
                }
            }
        }
    }
}
