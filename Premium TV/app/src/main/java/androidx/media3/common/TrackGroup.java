package androidx.media3.common;

import android.os.Bundle;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.BundleCollectionUtil;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class TrackGroup {
    private static final String FIELD_FORMATS = Util.intToStringMaxRadix(0);
    private static final String FIELD_ID = Util.intToStringMaxRadix(1);
    private static final String TAG = "TrackGroup";
    private final Format[] formats;
    private int hashCode;
    public final String id;
    public final int length;
    public final int type;

    public TrackGroup(Format... formats) {
        this("", formats);
    }

    public TrackGroup(String id, Format... formats) {
        Assertions.checkArgument(formats.length > 0);
        this.id = id;
        this.formats = formats;
        this.length = formats.length;
        int type = MimeTypes.getTrackType(formats[0].sampleMimeType);
        this.type = type == -1 ? MimeTypes.getTrackType(formats[0].containerMimeType) : type;
        verifyCorrectness();
    }

    public TrackGroup copyWithId(String id) {
        return new TrackGroup(id, this.formats);
    }

    public Format getFormat(int index) {
        return this.formats[index];
    }

    public int indexOf(Format format) {
        for (int i = 0; i < this.formats.length; i++) {
            if (format == this.formats[i]) {
                return i;
            }
        }
        return -1;
    }

    public int hashCode() {
        if (this.hashCode == 0) {
            int result = (17 * 31) + this.id.hashCode();
            this.hashCode = (result * 31) + Arrays.hashCode(this.formats);
        }
        int result2 = this.hashCode;
        return result2;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TrackGroup other = (TrackGroup) obj;
        if (this.id.equals(other.id) && Arrays.equals(this.formats, other.formats)) {
            return true;
        }
        return false;
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        ArrayList<Bundle> arrayList = new ArrayList<>(this.formats.length);
        for (Format format : this.formats) {
            arrayList.add(format.toBundle(true));
        }
        bundle.putParcelableArrayList(FIELD_FORMATS, arrayList);
        bundle.putString(FIELD_ID, this.id);
        return bundle;
    }

    public static TrackGroup fromBundle(Bundle bundle) {
        List<Format> formats;
        List<Bundle> formatBundles = bundle.getParcelableArrayList(FIELD_FORMATS);
        if (formatBundles == null) {
            formats = ImmutableList.of();
        } else {
            formats = BundleCollectionUtil.fromBundleList(new Function() { // from class: androidx.media3.common.TrackGroup$$ExternalSyntheticLambda0
                @Override // com.google.common.base.Function
                public final Object apply(Object obj) {
                    return Format.fromBundle((Bundle) obj);
                }
            }, formatBundles);
        }
        String id = bundle.getString(FIELD_ID, "");
        return new TrackGroup(id, (Format[]) formats.toArray(new Format[0]));
    }

    private void verifyCorrectness() {
        String language = normalizeLanguage(this.formats[0].language);
        int roleFlags = normalizeRoleFlags(this.formats[0].roleFlags);
        for (int i = 1; i < this.formats.length; i++) {
            boolean zEquals = language.equals(normalizeLanguage(this.formats[i].language));
            Format[] formatArr = this.formats;
            if (!zEquals) {
                logErrorMessage("languages", formatArr[0].language, this.formats[i].language, i);
                return;
            } else {
                if (roleFlags != normalizeRoleFlags(formatArr[i].roleFlags)) {
                    logErrorMessage("role flags", Integer.toBinaryString(this.formats[0].roleFlags), Integer.toBinaryString(this.formats[i].roleFlags), i);
                    return;
                }
            }
        }
    }

    private static String normalizeLanguage(String language) {
        return (language == null || language.equals(C.LANGUAGE_UNDETERMINED)) ? "" : language;
    }

    private static int normalizeRoleFlags(int roleFlags) {
        return roleFlags | 16384;
    }

    private static void logErrorMessage(String mismatchField, String valueIndex0, String otherValue, int otherIndex) {
        Log.e(TAG, "", new IllegalStateException("Different " + mismatchField + " combined in one TrackGroup: '" + valueIndex0 + "' (track 0) and '" + otherValue + "' (track " + otherIndex + ")"));
    }
}
