package androidx.media3.common.util;

/* JADX INFO: loaded from: classes.dex */
public final class Size {
    public static final Size UNKNOWN = new Size(-1, -1);
    public static final Size ZERO = new Size(0, 0);
    private final int height;
    private final int width;

    public Size(int width, int height) {
        Assertions.checkArgument((width == -1 || width >= 0) && (height == -1 || height >= 0));
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Size)) {
            return false;
        }
        Size other = (Size) obj;
        if (this.width != other.width || this.height != other.height) {
            return false;
        }
        return true;
    }

    public String toString() {
        return this.width + "x" + this.height;
    }

    public int hashCode() {
        return this.height ^ ((this.width << 16) | (this.width >>> 16));
    }
}
