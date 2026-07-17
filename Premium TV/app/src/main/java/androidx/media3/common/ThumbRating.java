package androidx.media3.common;

import android.os.Bundle;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import com.google.common.base.Objects;

/* JADX INFO: loaded from: classes.dex */
public final class ThumbRating extends Rating {
    private static final int TYPE = 3;
    private final boolean isThumbsUp;
    private final boolean rated;
    private static final String FIELD_RATED = Util.intToStringMaxRadix(1);
    private static final String FIELD_IS_THUMBS_UP = Util.intToStringMaxRadix(2);

    public ThumbRating() {
        this.rated = false;
        this.isThumbsUp = false;
    }

    public ThumbRating(boolean isThumbsUp) {
        this.rated = true;
        this.isThumbsUp = isThumbsUp;
    }

    @Override // androidx.media3.common.Rating
    public boolean isRated() {
        return this.rated;
    }

    public boolean isThumbsUp() {
        return this.isThumbsUp;
    }

    public int hashCode() {
        return Objects.hashCode(Boolean.valueOf(this.rated), Boolean.valueOf(this.isThumbsUp));
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ThumbRating)) {
            return false;
        }
        ThumbRating other = (ThumbRating) obj;
        return this.isThumbsUp == other.isThumbsUp && this.rated == other.rated;
    }

    @Override // androidx.media3.common.Rating
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(FIELD_RATING_TYPE, 3);
        bundle.putBoolean(FIELD_RATED, this.rated);
        bundle.putBoolean(FIELD_IS_THUMBS_UP, this.isThumbsUp);
        return bundle;
    }

    public static ThumbRating fromBundle(Bundle bundle) {
        Assertions.checkArgument(bundle.getInt(FIELD_RATING_TYPE, -1) == 3);
        boolean rated = bundle.getBoolean(FIELD_RATED, false);
        if (rated) {
            return new ThumbRating(bundle.getBoolean(FIELD_IS_THUMBS_UP, false));
        }
        return new ThumbRating();
    }
}
