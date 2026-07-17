package androidx.media3.common;

import android.os.Bundle;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public final class VideoSize {
    private static final int DEFAULT_HEIGHT = 0;
    private static final float DEFAULT_PIXEL_WIDTH_HEIGHT_RATIO = 1.0f;
    private static final int DEFAULT_UNAPPLIED_ROTATION_DEGREES = 0;
    private static final int DEFAULT_WIDTH = 0;
    public final int height;
    public final float pixelWidthHeightRatio;
    public final int unappliedRotationDegrees;
    public final int width;
    public static final VideoSize UNKNOWN = new VideoSize(0, 0);
    private static final String FIELD_WIDTH = Util.intToStringMaxRadix(0);
    private static final String FIELD_HEIGHT = Util.intToStringMaxRadix(1);
    private static final String FIELD_UNAPPLIED_ROTATION_DEGREES = Util.intToStringMaxRadix(2);
    private static final String FIELD_PIXEL_WIDTH_HEIGHT_RATIO = Util.intToStringMaxRadix(3);

    public VideoSize(int width, int height) {
        this(width, height, 0, 1.0f);
    }

    public VideoSize(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        this.width = width;
        this.height = height;
        this.unappliedRotationDegrees = unappliedRotationDegrees;
        this.pixelWidthHeightRatio = pixelWidthHeightRatio;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VideoSize)) {
            return false;
        }
        VideoSize other = (VideoSize) obj;
        return this.width == other.width && this.height == other.height && this.unappliedRotationDegrees == other.unappliedRotationDegrees && this.pixelWidthHeightRatio == other.pixelWidthHeightRatio;
    }

    public int hashCode() {
        int result = (7 * 31) + this.width;
        return (((((result * 31) + this.height) * 31) + this.unappliedRotationDegrees) * 31) + Float.floatToRawIntBits(this.pixelWidthHeightRatio);
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(FIELD_WIDTH, this.width);
        bundle.putInt(FIELD_HEIGHT, this.height);
        bundle.putInt(FIELD_UNAPPLIED_ROTATION_DEGREES, this.unappliedRotationDegrees);
        bundle.putFloat(FIELD_PIXEL_WIDTH_HEIGHT_RATIO, this.pixelWidthHeightRatio);
        return bundle;
    }

    public static VideoSize fromBundle(Bundle bundle) {
        int width = bundle.getInt(FIELD_WIDTH, 0);
        int height = bundle.getInt(FIELD_HEIGHT, 0);
        int unappliedRotationDegrees = bundle.getInt(FIELD_UNAPPLIED_ROTATION_DEGREES, 0);
        float pixelWidthHeightRatio = bundle.getFloat(FIELD_PIXEL_WIDTH_HEIGHT_RATIO, 1.0f);
        return new VideoSize(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
    }
}
