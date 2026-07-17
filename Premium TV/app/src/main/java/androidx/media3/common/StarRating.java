package androidx.media3.common;

import android.os.Bundle;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import com.google.common.base.Objects;

/* JADX INFO: loaded from: classes.dex */
public final class StarRating extends Rating {
    private static final String FIELD_MAX_STARS = Util.intToStringMaxRadix(1);
    private static final String FIELD_STAR_RATING = Util.intToStringMaxRadix(2);
    private static final int MAX_STARS_DEFAULT = 5;
    private static final int TYPE = 2;
    private final int maxStars;
    private final float starRating;

    public StarRating(int maxStars) {
        Assertions.checkArgument(maxStars > 0, "maxStars must be a positive integer");
        this.maxStars = maxStars;
        this.starRating = -1.0f;
    }

    public StarRating(int maxStars, float starRating) {
        Assertions.checkArgument(maxStars > 0, "maxStars must be a positive integer");
        Assertions.checkArgument(starRating >= 0.0f && starRating <= ((float) maxStars), "starRating is out of range [0, maxStars]");
        this.maxStars = maxStars;
        this.starRating = starRating;
    }

    @Override // androidx.media3.common.Rating
    public boolean isRated() {
        return this.starRating != -1.0f;
    }

    public int getMaxStars() {
        return this.maxStars;
    }

    public float getStarRating() {
        return this.starRating;
    }

    public int hashCode() {
        return Objects.hashCode(Integer.valueOf(this.maxStars), Float.valueOf(this.starRating));
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof StarRating)) {
            return false;
        }
        StarRating other = (StarRating) obj;
        return this.maxStars == other.maxStars && this.starRating == other.starRating;
    }

    @Override // androidx.media3.common.Rating
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(FIELD_RATING_TYPE, 2);
        bundle.putInt(FIELD_MAX_STARS, this.maxStars);
        bundle.putFloat(FIELD_STAR_RATING, this.starRating);
        return bundle;
    }

    public static StarRating fromBundle(Bundle bundle) {
        Assertions.checkArgument(bundle.getInt(FIELD_RATING_TYPE, -1) == 2);
        int maxStars = bundle.getInt(FIELD_MAX_STARS, 5);
        float starRating = bundle.getFloat(FIELD_STAR_RATING, -1.0f);
        if (starRating == -1.0f) {
            return new StarRating(maxStars);
        }
        return new StarRating(maxStars, starRating);
    }
}
