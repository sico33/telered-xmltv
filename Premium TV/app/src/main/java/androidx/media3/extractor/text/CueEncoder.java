package androidx.media3.extractor.text;

import android.os.Bundle;
import android.os.Parcel;
import androidx.media3.common.text.Cue;
import androidx.media3.common.util.BundleCollectionUtil;
import com.google.common.base.Function;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class CueEncoder {
    public byte[] encode(List<Cue> cues, long durationUs) {
        ArrayList<Bundle> bundledCues = BundleCollectionUtil.toBundleArrayList(cues, new Function() { // from class: androidx.media3.extractor.text.CueEncoder$$ExternalSyntheticLambda0
            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                return ((Cue) obj).toSerializableBundle();
            }
        });
        Bundle allCuesBundle = new Bundle();
        allCuesBundle.putParcelableArrayList("c", bundledCues);
        allCuesBundle.putLong("d", durationUs);
        Parcel parcel = Parcel.obtain();
        parcel.writeBundle(allCuesBundle);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes;
    }
}
