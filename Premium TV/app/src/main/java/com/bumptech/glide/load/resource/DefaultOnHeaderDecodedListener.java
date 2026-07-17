package com.bumptech.glide.load.resource;

import android.graphics.ColorSpace;
import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder$DecodeException;
import android.graphics.ImageDecoder$ImageInfo;
import android.graphics.ImageDecoder$OnHeaderDecodedListener;
import android.graphics.ImageDecoder$OnPartialImageListener;
import android.graphics.ImageDecoder$Source;
import android.os.Build;
import android.util.Log;
import android.util.Size;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.PreferredColorSpace;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.load.resource.bitmap.HardwareConfigState;

/* JADX INFO: loaded from: classes.dex */
public final class DefaultOnHeaderDecodedListener implements ImageDecoder$OnHeaderDecodedListener {
    private static final String TAG = "ImageDecoder";
    private final DecodeFormat decodeFormat;
    private final HardwareConfigState hardwareConfigState = HardwareConfigState.getInstance();
    private final boolean isHardwareConfigAllowed;
    private final PreferredColorSpace preferredColorSpace;
    private final int requestedHeight;
    private final int requestedWidth;
    private final DownsampleStrategy strategy;

    public DefaultOnHeaderDecodedListener(int i, int i2, Options options) {
        this.requestedWidth = i;
        this.requestedHeight = i2;
        this.decodeFormat = (DecodeFormat) options.get(Downsampler.DECODE_FORMAT);
        this.strategy = (DownsampleStrategy) options.get(DownsampleStrategy.OPTION);
        this.isHardwareConfigAllowed = options.get(Downsampler.ALLOW_HARDWARE_CONFIG) != null && ((Boolean) options.get(Downsampler.ALLOW_HARDWARE_CONFIG)).booleanValue();
        this.preferredColorSpace = (PreferredColorSpace) options.get(Downsampler.PREFERRED_COLOR_SPACE);
    }

    public void onHeaderDecoded(ImageDecoder imageDecoder, ImageDecoder$ImageInfo imageDecoder$ImageInfo, ImageDecoder$Source imageDecoder$Source) {
        if (this.hardwareConfigState.isHardwareConfigAllowed(this.requestedWidth, this.requestedHeight, this.isHardwareConfigAllowed, false)) {
            imageDecoder.setAllocator(3);
        } else {
            imageDecoder.setAllocator(1);
        }
        if (this.decodeFormat == DecodeFormat.PREFER_RGB_565) {
            imageDecoder.setMemorySizePolicy(0);
        }
        imageDecoder.setOnPartialImageListener(new ImageDecoder$OnPartialImageListener(this) { // from class: com.bumptech.glide.load.resource.DefaultOnHeaderDecodedListener.1
            final DefaultOnHeaderDecodedListener this$0;

            {
                this.this$0 = this;
            }

            public boolean onPartialImage(ImageDecoder$DecodeException imageDecoder$DecodeException) {
                return false;
            }
        });
        Size size = imageDecoder$ImageInfo.getSize();
        int width = this.requestedWidth;
        if (this.requestedWidth == Integer.MIN_VALUE) {
            width = size.getWidth();
        }
        int height = this.requestedHeight;
        if (this.requestedHeight == Integer.MIN_VALUE) {
            height = size.getHeight();
        }
        float scaleFactor = this.strategy.getScaleFactor(size.getWidth(), size.getHeight(), width, height);
        int iRound = Math.round(size.getWidth() * scaleFactor);
        int iRound2 = Math.round(size.getHeight() * scaleFactor);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "Resizing from [" + size.getWidth() + "x" + size.getHeight() + "] to [" + iRound + "x" + iRound2 + "] scaleFactor: " + scaleFactor);
        }
        imageDecoder.setTargetSize(iRound, iRound2);
        if (this.preferredColorSpace != null) {
            if (Build.VERSION.SDK_INT >= 28) {
                imageDecoder.setTargetColorSpace(ColorSpace.get(this.preferredColorSpace == PreferredColorSpace.DISPLAY_P3 && imageDecoder$ImageInfo.getColorSpace() != null && imageDecoder$ImageInfo.getColorSpace().isWideGamut() ? ColorSpace.Named.DISPLAY_P3 : ColorSpace.Named.SRGB));
            } else if (Build.VERSION.SDK_INT >= 26) {
                imageDecoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB));
            }
        }
    }
}
