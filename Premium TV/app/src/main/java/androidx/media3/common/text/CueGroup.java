package androidx.media3.common.text;

import android.os.Bundle;
import androidx.media3.common.util.BundleCollectionUtil;
import androidx.media3.common.util.Util;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class CueGroup {
    public static final CueGroup EMPTY_TIME_ZERO = new CueGroup(ImmutableList.of(), 0);
    private static final String FIELD_CUES = Util.intToStringMaxRadix(0);
    private static final String FIELD_PRESENTATION_TIME_US = Util.intToStringMaxRadix(1);
    public final ImmutableList<Cue> cues;
    public final long presentationTimeUs;

    public CueGroup(List<Cue> cues, long presentationTimeUs) {
        this.cues = ImmutableList.copyOf((Collection) cues);
        this.presentationTimeUs = presentationTimeUs;
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(FIELD_CUES, BundleCollectionUtil.toBundleArrayList(filterOutBitmapCues(this.cues), new Function() { // from class: androidx.media3.common.text.CueGroup$$ExternalSyntheticLambda1
            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                return ((Cue) obj).toBinderBasedBundle();
            }
        }));
        bundle.putLong(FIELD_PRESENTATION_TIME_US, this.presentationTimeUs);
        return bundle;
    }

    public static CueGroup fromBundle(Bundle bundle) {
        List<Cue> cues;
        ArrayList<Bundle> cueBundles = bundle.getParcelableArrayList(FIELD_CUES);
        if (cueBundles == null) {
            cues = ImmutableList.of();
        } else {
            cues = BundleCollectionUtil.fromBundleList(new Function() { // from class: androidx.media3.common.text.CueGroup$$ExternalSyntheticLambda0
                @Override // com.google.common.base.Function
                public final Object apply(Object obj) {
                    return Cue.fromBundle((Bundle) obj);
                }
            }, cueBundles);
        }
        long presentationTimeUs = bundle.getLong(FIELD_PRESENTATION_TIME_US);
        return new CueGroup(cues, presentationTimeUs);
    }

    private static ImmutableList<Cue> filterOutBitmapCues(List<Cue> cues) {
        ImmutableList.Builder<Cue> builder = ImmutableList.builder();
        for (int i = 0; i < cues.size(); i++) {
            if (cues.get(i).bitmap == null) {
                builder.add(cues.get(i));
            }
        }
        return builder.build();
    }
}
