package com.bumptech.glide.load;

import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
public interface ImageHeaderParser {
    public static final int UNKNOWN_ORIENTATION = -1;

    /* JADX INFO: renamed from: com.bumptech.glide.load.ImageHeaderParser$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final int[] $SwitchMap$com$bumptech$glide$load$ImageHeaderParser$ImageType = new int[ImageType.values().length];

        static {
            try {
                $SwitchMap$com$bumptech$glide$load$ImageHeaderParser$ImageType[ImageType.WEBP.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$bumptech$glide$load$ImageHeaderParser$ImageType[ImageType.WEBP_A.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$bumptech$glide$load$ImageHeaderParser$ImageType[ImageType.ANIMATED_WEBP.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    public enum ImageType {
        GIF(true),
        JPEG(false),
        RAW(false),
        PNG_A(true),
        PNG(false),
        WEBP_A(true),
        WEBP(false),
        ANIMATED_WEBP(true),
        AVIF(true),
        ANIMATED_AVIF(true),
        UNKNOWN(false);

        private final boolean hasAlpha;

        ImageType(boolean z) {
            this.hasAlpha = z;
        }

        public boolean hasAlpha() {
            return this.hasAlpha;
        }

        public boolean isWebp() {
            switch (AnonymousClass1.$SwitchMap$com$bumptech$glide$load$ImageHeaderParser$ImageType[ordinal()]) {
                case 1:
                case 2:
                case 3:
                    return true;
                default:
                    return false;
            }
        }
    }

    int getOrientation(InputStream inputStream, ArrayPool arrayPool) throws IOException;

    int getOrientation(ByteBuffer byteBuffer, ArrayPool arrayPool) throws IOException;

    ImageType getType(InputStream inputStream) throws IOException;

    ImageType getType(ByteBuffer byteBuffer) throws IOException;
}
