package androidx.media3.common;

import android.os.Bundle;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public abstract class Rating {
    static final String FIELD_RATING_TYPE = Util.intToStringMaxRadix(0);
    static final int RATING_TYPE_HEART = 0;
    static final int RATING_TYPE_PERCENTAGE = 1;
    static final int RATING_TYPE_STAR = 2;
    static final int RATING_TYPE_THUMB = 3;
    static final int RATING_TYPE_UNSET = -1;
    static final float RATING_UNSET = -1.0f;

    public abstract boolean isRated();

    public abstract Bundle toBundle();

    Rating() {
    }

    public static Rating fromBundle(Bundle bundle) {
        int ratingType = bundle.getInt(FIELD_RATING_TYPE, -1);
        switch (ratingType) {
            case 0:
                return HeartRating.fromBundle(bundle);
            case 1:
                return PercentageRating.fromBundle(bundle);
            case 2:
                return StarRating.fromBundle(bundle);
            case 3:
                return ThumbRating.fromBundle(bundle);
            default:
                throw new IllegalArgumentException("Unknown RatingType: " + ratingType);
        }
    }
}
