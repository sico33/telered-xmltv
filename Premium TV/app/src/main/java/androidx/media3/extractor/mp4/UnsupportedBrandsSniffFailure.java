package androidx.media3.extractor.mp4;

import androidx.media3.extractor.SniffFailure;
import com.google.common.primitives.ImmutableIntArray;

/* JADX INFO: loaded from: classes.dex */
public final class UnsupportedBrandsSniffFailure implements SniffFailure {
    public final ImmutableIntArray compatibleBrands;
    public final int majorBrand;

    public UnsupportedBrandsSniffFailure(int majorBrand, int[] compatibleBrands) {
        ImmutableIntArray immutableIntArrayOf;
        this.majorBrand = majorBrand;
        if (compatibleBrands != null) {
            immutableIntArrayOf = ImmutableIntArray.copyOf(compatibleBrands);
        } else {
            immutableIntArrayOf = ImmutableIntArray.of();
        }
        this.compatibleBrands = immutableIntArrayOf;
    }
}
