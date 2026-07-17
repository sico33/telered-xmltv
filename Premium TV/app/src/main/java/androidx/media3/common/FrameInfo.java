package androidx.media3.common;

import androidx.media3.common.util.Assertions;

/* JADX INFO: loaded from: classes.dex */
public class FrameInfo {
    public final ColorInfo colorInfo;
    public final int height;
    public final long offsetToAddUs;
    public final float pixelWidthHeightRatio;
    public final int width;

    public static final class Builder {
        private ColorInfo colorInfo;
        private int height;
        private long offsetToAddUs;
        private float pixelWidthHeightRatio;
        private int width;

        public Builder(ColorInfo colorInfo, int width, int height) {
            this.colorInfo = colorInfo;
            this.width = width;
            this.height = height;
            this.pixelWidthHeightRatio = 1.0f;
        }

        public Builder(FrameInfo frameInfo) {
            this.colorInfo = frameInfo.colorInfo;
            this.width = frameInfo.width;
            this.height = frameInfo.height;
            this.pixelWidthHeightRatio = frameInfo.pixelWidthHeightRatio;
            this.offsetToAddUs = frameInfo.offsetToAddUs;
        }

        public Builder setColorInfo(ColorInfo colorInfo) {
            this.colorInfo = colorInfo;
            return this;
        }

        public Builder setWidth(int width) {
            this.width = width;
            return this;
        }

        public Builder setHeight(int height) {
            this.height = height;
            return this;
        }

        public Builder setPixelWidthHeightRatio(float pixelWidthHeightRatio) {
            this.pixelWidthHeightRatio = pixelWidthHeightRatio;
            return this;
        }

        public Builder setOffsetToAddUs(long offsetToAddUs) {
            this.offsetToAddUs = offsetToAddUs;
            return this;
        }

        public FrameInfo build() {
            return new FrameInfo(this.colorInfo, this.width, this.height, this.pixelWidthHeightRatio, this.offsetToAddUs);
        }
    }

    private FrameInfo(ColorInfo colorInfo, int width, int height, float pixelWidthHeightRatio, long offsetToAddUs) {
        Assertions.checkArgument(width > 0, "width must be positive, but is: " + width);
        Assertions.checkArgument(height > 0, "height must be positive, but is: " + height);
        this.colorInfo = colorInfo;
        this.width = width;
        this.height = height;
        this.pixelWidthHeightRatio = pixelWidthHeightRatio;
        this.offsetToAddUs = offsetToAddUs;
    }
}
