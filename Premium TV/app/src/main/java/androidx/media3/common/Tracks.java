package androidx.media3.common;

import android.os.Bundle;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.BundleCollectionUtil;
import androidx.media3.common.util.Util;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Booleans;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class Tracks {
    public static final Tracks EMPTY = new Tracks(ImmutableList.of());
    private static final String FIELD_TRACK_GROUPS = Util.intToStringMaxRadix(0);
    private final ImmutableList<Group> groups;

    public static final class Group {
        private final boolean adaptiveSupported;
        public final int length;
        private final TrackGroup mediaTrackGroup;
        private final boolean[] trackSelected;
        private final int[] trackSupport;
        private static final String FIELD_TRACK_GROUP = Util.intToStringMaxRadix(0);
        private static final String FIELD_TRACK_SUPPORT = Util.intToStringMaxRadix(1);
        private static final String FIELD_TRACK_SELECTED = Util.intToStringMaxRadix(3);
        private static final String FIELD_ADAPTIVE_SUPPORTED = Util.intToStringMaxRadix(4);

        public Group(TrackGroup mediaTrackGroup, boolean adaptiveSupported, int[] trackSupport, boolean[] trackSelected) {
            this.length = mediaTrackGroup.length;
            boolean z = false;
            Assertions.checkArgument(this.length == trackSupport.length && this.length == trackSelected.length);
            this.mediaTrackGroup = mediaTrackGroup;
            if (adaptiveSupported && this.length > 1) {
                z = true;
            }
            this.adaptiveSupported = z;
            this.trackSupport = (int[]) trackSupport.clone();
            this.trackSelected = (boolean[]) trackSelected.clone();
        }

        public TrackGroup getMediaTrackGroup() {
            return this.mediaTrackGroup;
        }

        public Format getTrackFormat(int trackIndex) {
            return this.mediaTrackGroup.getFormat(trackIndex);
        }

        public int getTrackSupport(int trackIndex) {
            return this.trackSupport[trackIndex];
        }

        public boolean isTrackSupported(int trackIndex) {
            return isTrackSupported(trackIndex, false);
        }

        public boolean isTrackSupported(int trackIndex, boolean allowExceedsCapabilities) {
            return this.trackSupport[trackIndex] == 4 || (allowExceedsCapabilities && this.trackSupport[trackIndex] == 3);
        }

        public boolean isSelected() {
            return Booleans.contains(this.trackSelected, true);
        }

        public boolean isAdaptiveSupported() {
            return this.adaptiveSupported;
        }

        public boolean isSupported() {
            return isSupported(false);
        }

        public boolean isSupported(boolean allowExceedsCapabilities) {
            for (int i = 0; i < this.trackSupport.length; i++) {
                if (isTrackSupported(i, allowExceedsCapabilities)) {
                    return true;
                }
            }
            return false;
        }

        public boolean isTrackSelected(int trackIndex) {
            return this.trackSelected[trackIndex];
        }

        public int getType() {
            return this.mediaTrackGroup.type;
        }

        public Group copyWithId(String groupId) {
            return new Group(this.mediaTrackGroup.copyWithId(groupId), this.adaptiveSupported, this.trackSupport, this.trackSelected);
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }
            Group that = (Group) other;
            if (this.adaptiveSupported == that.adaptiveSupported && this.mediaTrackGroup.equals(that.mediaTrackGroup) && Arrays.equals(this.trackSupport, that.trackSupport) && Arrays.equals(this.trackSelected, that.trackSelected)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (((((this.mediaTrackGroup.hashCode() * 31) + (this.adaptiveSupported ? 1 : 0)) * 31) + Arrays.hashCode(this.trackSupport)) * 31) + Arrays.hashCode(this.trackSelected);
        }

        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putBundle(FIELD_TRACK_GROUP, this.mediaTrackGroup.toBundle());
            bundle.putIntArray(FIELD_TRACK_SUPPORT, this.trackSupport);
            bundle.putBooleanArray(FIELD_TRACK_SELECTED, this.trackSelected);
            bundle.putBoolean(FIELD_ADAPTIVE_SUPPORTED, this.adaptiveSupported);
            return bundle;
        }

        public static Group fromBundle(Bundle bundle) {
            TrackGroup trackGroup = TrackGroup.fromBundle((Bundle) Assertions.checkNotNull(bundle.getBundle(FIELD_TRACK_GROUP)));
            int[] trackSupport = (int[]) MoreObjects.firstNonNull(bundle.getIntArray(FIELD_TRACK_SUPPORT), new int[trackGroup.length]);
            boolean[] selected = (boolean[]) MoreObjects.firstNonNull(bundle.getBooleanArray(FIELD_TRACK_SELECTED), new boolean[trackGroup.length]);
            boolean adaptiveSupported = bundle.getBoolean(FIELD_ADAPTIVE_SUPPORTED, false);
            return new Group(trackGroup, adaptiveSupported, trackSupport, selected);
        }
    }

    public Tracks(List<Group> groups) {
        this.groups = ImmutableList.copyOf((Collection) groups);
    }

    public ImmutableList<Group> getGroups() {
        return this.groups;
    }

    public boolean isEmpty() {
        return this.groups.isEmpty();
    }

    public boolean containsType(int trackType) {
        for (int i = 0; i < this.groups.size(); i++) {
            if (this.groups.get(i).getType() == trackType) {
                return true;
            }
        }
        return false;
    }

    public boolean isTypeSupported(int trackType) {
        return isTypeSupported(trackType, false);
    }

    public boolean isTypeSupported(int trackType, boolean allowExceedsCapabilities) {
        for (int i = 0; i < this.groups.size(); i++) {
            if (this.groups.get(i).getType() == trackType && this.groups.get(i).isSupported(allowExceedsCapabilities)) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    public boolean isTypeSupportedOrEmpty(int trackType) {
        return isTypeSupportedOrEmpty(trackType, false);
    }

    @Deprecated
    public boolean isTypeSupportedOrEmpty(int trackType, boolean allowExceedsCapabilities) {
        return !containsType(trackType) || isTypeSupported(trackType, allowExceedsCapabilities);
    }

    public boolean isTypeSelected(int trackType) {
        for (int i = 0; i < this.groups.size(); i++) {
            Group group = this.groups.get(i);
            if (group.isSelected() && group.getType() == trackType) {
                return true;
            }
        }
        return false;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Tracks that = (Tracks) other;
        return this.groups.equals(that.groups);
    }

    public int hashCode() {
        return this.groups.hashCode();
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(FIELD_TRACK_GROUPS, BundleCollectionUtil.toBundleArrayList(this.groups, new Function() { // from class: androidx.media3.common.Tracks$$ExternalSyntheticLambda0
            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                return ((Tracks.Group) obj).toBundle();
            }
        }));
        return bundle;
    }

    public static Tracks fromBundle(Bundle bundle) {
        List<Group> groups;
        List<Bundle> groupBundles = bundle.getParcelableArrayList(FIELD_TRACK_GROUPS);
        if (groupBundles == null) {
            groups = ImmutableList.of();
        } else {
            groups = BundleCollectionUtil.fromBundleList(new Function() { // from class: androidx.media3.common.Tracks$$ExternalSyntheticLambda1
                @Override // com.google.common.base.Function
                public final Object apply(Object obj) {
                    return Tracks.Group.fromBundle((Bundle) obj);
                }
            }, groupBundles);
        }
        return new Tracks(groups);
    }
}
