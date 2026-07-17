package androidx.media3.extractor.text;

import android.os.Bundle;
import android.os.Parcel;
import androidx.media3.common.text.Cue;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.BundleCollectionUtil;
import com.google.common.base.Function;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public final class CueDecoder {
    static final String BUNDLE_FIELD_CUES = "c";
    static final String BUNDLE_FIELD_DURATION_US = "d";

    public CuesWithTiming decode(long startTimeUs, byte[] bytes) {
        return decode(startTimeUs, bytes, 0, bytes.length);
    }

    public CuesWithTiming decode(long startTimeUs, byte[] bytes, int offset, int length) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, offset, length);
        parcel.setDataPosition(0);
        Bundle bundle = parcel.readBundle(Bundle.class.getClassLoader());
        parcel.recycle();
        ArrayList<Bundle> bundledCues = (ArrayList) Assertions.checkNotNull(bundle.getParcelableArrayList(BUNDLE_FIELD_CUES));
        return new CuesWithTiming(BundleCollectionUtil.fromBundleList(new Function() { // from class: androidx.media3.extractor.text.CueDecoder$$ExternalSyntheticLambda0
            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                return Cue.fromBundle((Bundle) obj);
            }
        }, bundledCues), startTimeUs, bundle.getLong("d"));
    }
}
