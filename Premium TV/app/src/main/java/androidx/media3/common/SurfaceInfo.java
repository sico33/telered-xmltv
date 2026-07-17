package androidx.media3.common;

import android.view.Surface;
import androidx.media3.common.util.Assertions;

/* JADX INFO: loaded from: classes.dex */
public final class SurfaceInfo {
    public final int height;
    public final int orientationDegrees;
    public final Surface surface;
    public final int width;

    public SurfaceInfo(Surface surface, int width, int height) {
        this(surface, width, height, 0);
    }

    public SurfaceInfo(Surface surface, int width, int height, int orientationDegrees) {
        Assertions.checkArgument(orientationDegrees == 0 || orientationDegrees == 90 || orientationDegrees == 180 || orientationDegrees == 270, "orientationDegrees must be 0, 90, 180, or 270");
        this.surface = surface;
        this.width = width;
        this.height = height;
        this.orientationDegrees = orientationDegrees;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SurfaceInfo)) {
            return false;
        }
        SurfaceInfo that = (SurfaceInfo) o;
        return this.width == that.width && this.height == that.height && this.orientationDegrees == that.orientationDegrees && this.surface.equals(that.surface);
    }

    public int hashCode() {
        int result = this.surface.hashCode();
        return (((((result * 31) + this.width) * 31) + this.height) * 31) + this.orientationDegrees;
    }
}
