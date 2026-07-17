package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.Log;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.PreferredColorSpace;
import com.bumptech.glide.load.data.ParcelFileDescriptorRewinder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Util;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public final class Downsampler {
    static final String TAG = "Downsampler";
    private final BitmapPool bitmapPool;
    private final ArrayPool byteArrayPool;
    private final DisplayMetrics displayMetrics;
    private final HardwareConfigState hardwareConfigState = HardwareConfigState.getInstance();
    private final List<ImageHeaderParser> parsers;
    public static final Option<DecodeFormat> DECODE_FORMAT = Option.memory("com.bumptech.glide.load.resource.bitmap.Downsampler.DecodeFormat", DecodeFormat.DEFAULT);
    public static final Option<PreferredColorSpace> PREFERRED_COLOR_SPACE = Option.memory("com.bumptech.glide.load.resource.bitmap.Downsampler.PreferredColorSpace");

    @Deprecated
    public static final Option<DownsampleStrategy> DOWNSAMPLE_STRATEGY = DownsampleStrategy.OPTION;
    public static final Option<Boolean> FIX_BITMAP_SIZE_TO_REQUESTED_DIMENSIONS = Option.memory("com.bumptech.glide.load.resource.bitmap.Downsampler.FixBitmapSize", false);
    public static final Option<Boolean> ALLOW_HARDWARE_CONFIG = Option.memory("com.bumptech.glide.load.resource.bitmap.Downsampler.AllowHardwareDecode", false);
    private static final String WBMP_MIME_TYPE = "image/vnd.wap.wbmp";
    private static final String ICO_MIME_TYPE = "image/x-ico";
    private static final Set<String> NO_DOWNSAMPLE_PRE_N_MIME_TYPES = Collections.unmodifiableSet(new HashSet(Arrays.asList(WBMP_MIME_TYPE, ICO_MIME_TYPE)));
    private static final DecodeCallbacks EMPTY_CALLBACKS = new DecodeCallbacks() { // from class: com.bumptech.glide.load.resource.bitmap.Downsampler.1
        @Override // com.bumptech.glide.load.resource.bitmap.Downsampler.DecodeCallbacks
        public void onDecodeComplete(BitmapPool bitmapPool, Bitmap bitmap) {
        }

        @Override // com.bumptech.glide.load.resource.bitmap.Downsampler.DecodeCallbacks
        public void onObtainBounds() {
        }
    };
    private static final Set<ImageHeaderParser.ImageType> TYPES_THAT_USE_POOL_PRE_KITKAT = Collections.unmodifiableSet(EnumSet.of(ImageHeaderParser.ImageType.JPEG, ImageHeaderParser.ImageType.PNG_A, ImageHeaderParser.ImageType.PNG));
    private static final Queue<BitmapFactory.Options> OPTIONS_QUEUE = Util.createQueue(0);

    public interface DecodeCallbacks {
        void onDecodeComplete(BitmapPool bitmapPool, Bitmap bitmap) throws IOException;

        void onObtainBounds();
    }

    public Downsampler(List<ImageHeaderParser> list, DisplayMetrics displayMetrics, BitmapPool bitmapPool, ArrayPool arrayPool) {
        this.parsers = list;
        this.displayMetrics = (DisplayMetrics) Preconditions.checkNotNull(displayMetrics);
        this.bitmapPool = (BitmapPool) Preconditions.checkNotNull(bitmapPool);
        this.byteArrayPool = (ArrayPool) Preconditions.checkNotNull(arrayPool);
    }

    private static int adjustTargetDensityForError(double d) {
        int densityMultiplier = getDensityMultiplier(d);
        int iRound = round(((double) densityMultiplier) * d);
        return round(((double) iRound) * (d / ((double) (iRound / densityMultiplier))));
    }

    private void calculateConfig(ImageReader imageReader, DecodeFormat decodeFormat, boolean z, boolean z2, BitmapFactory.Options options, int i, int i2) {
        boolean zHasAlpha;
        if (this.hardwareConfigState.setHardwareConfigIfAllowed(i, i2, options, z, z2)) {
            return;
        }
        if (decodeFormat == DecodeFormat.PREFER_ARGB_8888) {
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return;
        }
        try {
            zHasAlpha = imageReader.getImageType().hasAlpha();
        } catch (IOException e) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "Cannot determine whether the image has alpha or not from header, format " + decodeFormat, e);
                zHasAlpha = false;
            } else {
                zHasAlpha = false;
            }
        }
        options.inPreferredConfig = zHasAlpha ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        if (options.inPreferredConfig == Bitmap.Config.RGB_565) {
            options.inDither = true;
        }
    }

    private static void calculateScaling(ImageHeaderParser.ImageType imageType, ImageReader imageReader, DecodeCallbacks decodeCallbacks, BitmapPool bitmapPool, DownsampleStrategy downsampleStrategy, int i, int i2, int i3, int i4, int i5, BitmapFactory.Options options) throws IOException {
        int i6;
        int i7;
        int iMax;
        int iFloor;
        int i8;
        if (i2 <= 0 || i3 <= 0) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "Unable to determine dimensions for: " + imageType + " with target [" + i4 + "x" + i5 + "]");
                return;
            }
            return;
        }
        if (isRotationRequired(i)) {
            i6 = i2;
            i7 = i3;
        } else {
            i6 = i3;
            i7 = i2;
        }
        float scaleFactor = downsampleStrategy.getScaleFactor(i7, i6, i4, i5);
        if (scaleFactor <= 0.0f) {
            throw new IllegalArgumentException("Cannot scale with factor: " + scaleFactor + " from: " + downsampleStrategy + ", source: [" + i2 + "x" + i3 + "], target: [" + i4 + "x" + i5 + "]");
        }
        DownsampleStrategy.SampleSizeRounding sampleSizeRounding = downsampleStrategy.getSampleSizeRounding(i7, i6, i4, i5);
        if (sampleSizeRounding == null) {
            throw new IllegalArgumentException("Cannot round with null rounding");
        }
        int iRound = i7 / round(i7 * scaleFactor);
        int iRound2 = i6 / round(i6 * scaleFactor);
        int iMax2 = sampleSizeRounding == DownsampleStrategy.SampleSizeRounding.MEMORY ? Math.max(iRound, iRound2) : Math.min(iRound, iRound2);
        if (Build.VERSION.SDK_INT > 23 || !NO_DOWNSAMPLE_PRE_N_MIME_TYPES.contains(options.outMimeType)) {
            iMax = Math.max(1, Integer.highestOneBit(iMax2));
            if (sampleSizeRounding == DownsampleStrategy.SampleSizeRounding.MEMORY && iMax < 1.0f / scaleFactor) {
                iMax <<= 1;
            }
        } else {
            iMax = 1;
        }
        options.inSampleSize = iMax;
        if (imageType == ImageHeaderParser.ImageType.JPEG) {
            int iMin = Math.min(iMax, 8);
            int iCeil = (int) Math.ceil(i7 / iMin);
            int iCeil2 = (int) Math.ceil(i6 / iMin);
            int i9 = iMax / 8;
            if (i9 > 0) {
                i8 = iCeil / i9;
                iFloor = iCeil2 / i9;
            } else {
                i8 = iCeil;
                iFloor = iCeil2;
            }
        } else if (imageType == ImageHeaderParser.ImageType.PNG || imageType == ImageHeaderParser.ImageType.PNG_A) {
            int iFloor2 = (int) Math.floor(i7 / iMax);
            iFloor = (int) Math.floor(i6 / iMax);
            i8 = iFloor2;
        } else if (imageType.isWebp()) {
            if (Build.VERSION.SDK_INT >= 24) {
                int iRound3 = Math.round(i7 / iMax);
                iFloor = Math.round(i6 / iMax);
                i8 = iRound3;
            } else {
                int iFloor3 = (int) Math.floor(i7 / iMax);
                iFloor = (int) Math.floor(i6 / iMax);
                i8 = iFloor3;
            }
        } else if (i7 % iMax == 0 && i6 % iMax == 0) {
            int i10 = i7 / iMax;
            iFloor = i6 / iMax;
            i8 = i10;
        } else {
            int[] dimensions = getDimensions(imageReader, options, decodeCallbacks, bitmapPool);
            i8 = dimensions[0];
            iFloor = dimensions[1];
        }
        double scaleFactor2 = downsampleStrategy.getScaleFactor(i8, iFloor, i4, i5);
        options.inTargetDensity = adjustTargetDensityForError(scaleFactor2);
        options.inDensity = getDensityMultiplier(scaleFactor2);
        if (isScaling(options)) {
            options.inScaled = true;
        } else {
            options.inTargetDensity = 0;
            options.inDensity = 0;
        }
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "Calculate scaling, source: [" + i2 + "x" + i3 + "], degreesToRotate: " + i + ", target: [" + i4 + "x" + i5 + "], power of two scaled: [" + i8 + "x" + iFloor + "], exact scale factor: " + scaleFactor + ", power of 2 sample size: " + iMax + ", adjusted scale factor: " + scaleFactor2 + ", target density: " + options.inTargetDensity + ", density: " + options.inDensity);
        }
    }

    private Resource<Bitmap> decode(ImageReader imageReader, int i, int i2, Options options, DecodeCallbacks decodeCallbacks) throws IOException {
        byte[] bArr = (byte[]) this.byteArrayPool.get(65536, byte[].class);
        BitmapFactory.Options defaultOptions = getDefaultOptions();
        defaultOptions.inTempStorage = bArr;
        DecodeFormat decodeFormat = (DecodeFormat) options.get(DECODE_FORMAT);
        PreferredColorSpace preferredColorSpace = (PreferredColorSpace) options.get(PREFERRED_COLOR_SPACE);
        try {
            return BitmapResource.obtain(decodeFromWrappedStreams(imageReader, defaultOptions, (DownsampleStrategy) options.get(DownsampleStrategy.OPTION), decodeFormat, preferredColorSpace, options.get(ALLOW_HARDWARE_CONFIG) != null && ((Boolean) options.get(ALLOW_HARDWARE_CONFIG)).booleanValue(), i, i2, ((Boolean) options.get(FIX_BITMAP_SIZE_TO_REQUESTED_DIMENSIONS)).booleanValue(), decodeCallbacks), this.bitmapPool);
        } finally {
            releaseOptions(defaultOptions);
            this.byteArrayPool.put(bArr);
        }
    }

    private Bitmap decodeFromWrappedStreams(ImageReader imageReader, BitmapFactory.Options options, DownsampleStrategy downsampleStrategy, DecodeFormat decodeFormat, PreferredColorSpace preferredColorSpace, boolean z, int i, int i2, boolean z2, DecodeCallbacks decodeCallbacks) throws IOException {
        int iRound;
        int iRound2;
        long logTime = LogTime.getLogTime();
        int[] dimensions = getDimensions(imageReader, options, decodeCallbacks, this.bitmapPool);
        int i3 = dimensions[0];
        int i4 = dimensions[1];
        String str = options.outMimeType;
        boolean z3 = (i3 == -1 || i4 == -1) ? false : z;
        int imageOrientation = imageReader.getImageOrientation();
        int exifOrientationDegrees = TransformationUtils.getExifOrientationDegrees(imageOrientation);
        boolean zIsExifOrientationRequired = TransformationUtils.isExifOrientationRequired(imageOrientation);
        if (i == Integer.MIN_VALUE) {
            iRound = isRotationRequired(exifOrientationDegrees) ? i4 : i3;
        } else {
            iRound = i;
        }
        if (i2 == Integer.MIN_VALUE) {
            iRound2 = isRotationRequired(exifOrientationDegrees) ? i3 : i4;
        } else {
            iRound2 = i2;
        }
        ImageHeaderParser.ImageType imageType = imageReader.getImageType();
        calculateScaling(imageType, imageReader, decodeCallbacks, this.bitmapPool, downsampleStrategy, exifOrientationDegrees, i3, i4, iRound, iRound2, options);
        calculateConfig(imageReader, decodeFormat, z3, zIsExifOrientationRequired, options, iRound, iRound2);
        if (options.inSampleSize != 1) {
        }
        if (shouldUsePool(imageType)) {
            if (i3 < 0 || i4 < 0 || !z2) {
                float f = isScaling(options) ? options.inTargetDensity / options.inDensity : 1.0f;
                int i5 = options.inSampleSize;
                int iCeil = (int) Math.ceil(i3 / i5);
                int iCeil2 = (int) Math.ceil(i4 / i5);
                iRound = Math.round(iCeil * f);
                iRound2 = Math.round(iCeil2 * f);
                if (Log.isLoggable(TAG, 2)) {
                    Log.v(TAG, "Calculated target [" + iRound + "x" + iRound2 + "] for source [" + i3 + "x" + i4 + "], sampleSize: " + i5 + ", targetDensity: " + options.inTargetDensity + ", density: " + options.inDensity + ", density multiplier: " + f);
                }
            }
            if (iRound > 0 && iRound2 > 0) {
                setInBitmap(options, this.bitmapPool, iRound, iRound2);
            }
        }
        if (preferredColorSpace != null) {
            if (Build.VERSION.SDK_INT >= 28) {
                options.inPreferredColorSpace = ColorSpace.get(preferredColorSpace == PreferredColorSpace.DISPLAY_P3 && options.outColorSpace != null && options.outColorSpace.isWideGamut() ? ColorSpace.Named.DISPLAY_P3 : ColorSpace.Named.SRGB);
            } else if (Build.VERSION.SDK_INT >= 26) {
                options.inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
            }
        }
        Bitmap bitmapDecodeStream = decodeStream(imageReader, options, decodeCallbacks, this.bitmapPool);
        decodeCallbacks.onDecodeComplete(this.bitmapPool, bitmapDecodeStream);
        if (Log.isLoggable(TAG, 2)) {
            logDecode(i3, i4, str, options, bitmapDecodeStream, i, i2, logTime);
        }
        Bitmap bitmapRotateImageExif = null;
        if (bitmapDecodeStream != null) {
            bitmapDecodeStream.setDensity(this.displayMetrics.densityDpi);
            bitmapRotateImageExif = TransformationUtils.rotateImageExif(this.bitmapPool, bitmapDecodeStream, imageOrientation);
            if (!bitmapDecodeStream.equals(bitmapRotateImageExif)) {
                this.bitmapPool.put(bitmapDecodeStream);
            }
        }
        return bitmapRotateImageExif;
    }

    private static Bitmap decodeStream(ImageReader imageReader, BitmapFactory.Options options, DecodeCallbacks decodeCallbacks, BitmapPool bitmapPool) throws IOException {
        Bitmap bitmapDecodeStream;
        if (!options.inJustDecodeBounds) {
            decodeCallbacks.onObtainBounds();
            imageReader.stopGrowingBuffers();
        }
        int i = options.outWidth;
        int i2 = options.outHeight;
        String str = options.outMimeType;
        TransformationUtils.getBitmapDrawableLock().lock();
        try {
            try {
                bitmapDecodeStream = imageReader.decodeBitmap(options);
                TransformationUtils.getBitmapDrawableLock().unlock();
            } catch (IllegalArgumentException e) {
                IOException iOExceptionNewIoExceptionForInBitmapAssertion = newIoExceptionForInBitmapAssertion(e, i, i2, str, options);
                if (Log.isLoggable(TAG, 3)) {
                    Log.d(TAG, "Failed to decode with inBitmap, trying again without Bitmap re-use", iOExceptionNewIoExceptionForInBitmapAssertion);
                }
                if (options.inBitmap == null) {
                    throw iOExceptionNewIoExceptionForInBitmapAssertion;
                }
                try {
                    bitmapPool.put(options.inBitmap);
                    options.inBitmap = null;
                    bitmapDecodeStream = decodeStream(imageReader, options, decodeCallbacks, bitmapPool);
                    TransformationUtils.getBitmapDrawableLock().unlock();
                } catch (IOException e2) {
                    throw iOExceptionNewIoExceptionForInBitmapAssertion;
                }
            }
            return bitmapDecodeStream;
        } catch (Throwable th) {
            TransformationUtils.getBitmapDrawableLock().unlock();
            throw th;
        }
    }

    private static String getBitmapString(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        return "[" + bitmap.getWidth() + "x" + bitmap.getHeight() + "] " + bitmap.getConfig() + (" (" + bitmap.getAllocationByteCount() + ")");
    }

    private static BitmapFactory.Options getDefaultOptions() {
        BitmapFactory.Options optionsPoll;
        synchronized (Downsampler.class) {
            try {
                synchronized (OPTIONS_QUEUE) {
                    optionsPoll = OPTIONS_QUEUE.poll();
                }
                if (optionsPoll == null) {
                    optionsPoll = new BitmapFactory.Options();
                    resetOptions(optionsPoll);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return optionsPoll;
    }

    private static int getDensityMultiplier(double d) {
        if (d > 1.0d) {
            d = 1.0d / d;
        }
        return (int) Math.round(2.147483647E9d * d);
    }

    private static int[] getDimensions(ImageReader imageReader, BitmapFactory.Options options, DecodeCallbacks decodeCallbacks, BitmapPool bitmapPool) throws IOException {
        options.inJustDecodeBounds = true;
        decodeStream(imageReader, options, decodeCallbacks, bitmapPool);
        options.inJustDecodeBounds = false;
        return new int[]{options.outWidth, options.outHeight};
    }

    private static String getInBitmapString(BitmapFactory.Options options) {
        return getBitmapString(options.inBitmap);
    }

    private static boolean isRotationRequired(int i) {
        return i == 90 || i == 270;
    }

    private static boolean isScaling(BitmapFactory.Options options) {
        return options.inTargetDensity > 0 && options.inDensity > 0 && options.inTargetDensity != options.inDensity;
    }

    private static void logDecode(int i, int i2, String str, BitmapFactory.Options options, Bitmap bitmap, int i3, int i4, long j) {
        Log.v(TAG, "Decoded " + getBitmapString(bitmap) + " from [" + i + "x" + i2 + "] " + str + " with inBitmap " + getInBitmapString(options) + " for [" + i3 + "x" + i4 + "], sample size: " + options.inSampleSize + ", density: " + options.inDensity + ", target density: " + options.inTargetDensity + ", thread: " + Thread.currentThread().getName() + ", duration: " + LogTime.getElapsedMillis(j));
    }

    private static IOException newIoExceptionForInBitmapAssertion(IllegalArgumentException illegalArgumentException, int i, int i2, String str, BitmapFactory.Options options) {
        return new IOException("Exception decoding bitmap, outWidth: " + i + ", outHeight: " + i2 + ", outMimeType: " + str + ", inBitmap: " + getInBitmapString(options), illegalArgumentException);
    }

    private static void releaseOptions(BitmapFactory.Options options) {
        resetOptions(options);
        synchronized (OPTIONS_QUEUE) {
            OPTIONS_QUEUE.offer(options);
        }
    }

    private static void resetOptions(BitmapFactory.Options options) {
        options.inTempStorage = null;
        options.inDither = false;
        options.inScaled = false;
        options.inSampleSize = 1;
        options.inPreferredConfig = null;
        options.inJustDecodeBounds = false;
        options.inDensity = 0;
        options.inTargetDensity = 0;
        if (Build.VERSION.SDK_INT >= 26) {
            options.inPreferredColorSpace = null;
            options.outColorSpace = null;
            options.outConfig = null;
        }
        options.outWidth = 0;
        options.outHeight = 0;
        options.outMimeType = null;
        options.inBitmap = null;
        options.inMutable = true;
    }

    private static int round(double d) {
        return (int) (0.5d + d);
    }

    private static void setInBitmap(BitmapFactory.Options options, BitmapPool bitmapPool, int i, int i2) {
        Bitmap.Config config = null;
        if (Build.VERSION.SDK_INT >= 26) {
            if (options.inPreferredConfig == Bitmap.Config.HARDWARE) {
                return;
            } else {
                config = options.outConfig;
            }
        }
        if (config == null) {
            config = options.inPreferredConfig;
        }
        options.inBitmap = bitmapPool.getDirty(i, i2, config);
    }

    private boolean shouldUsePool(ImageHeaderParser.ImageType imageType) {
        return true;
    }

    public Resource<Bitmap> decode(ParcelFileDescriptor parcelFileDescriptor, int i, int i2, Options options) throws IOException {
        return decode(new ImageReader.ParcelFileDescriptorImageReader(parcelFileDescriptor, this.parsers, this.byteArrayPool), i, i2, options, EMPTY_CALLBACKS);
    }

    public Resource<Bitmap> decode(InputStream inputStream, int i, int i2, Options options) throws IOException {
        return decode(inputStream, i, i2, options, EMPTY_CALLBACKS);
    }

    public Resource<Bitmap> decode(InputStream inputStream, int i, int i2, Options options, DecodeCallbacks decodeCallbacks) throws IOException {
        return decode(new ImageReader.InputStreamImageReader(inputStream, this.parsers, this.byteArrayPool), i, i2, options, decodeCallbacks);
    }

    public Resource<Bitmap> decode(ByteBuffer byteBuffer, int i, int i2, Options options) throws IOException {
        return decode(new ImageReader.ByteBufferReader(byteBuffer, this.parsers, this.byteArrayPool), i, i2, options, EMPTY_CALLBACKS);
    }

    void decode(File file, int i, int i2, Options options) throws IOException {
        decode(new ImageReader.FileReader(file, this.parsers, this.byteArrayPool), i, i2, options, EMPTY_CALLBACKS);
    }

    void decode(byte[] bArr, int i, int i2, Options options) throws IOException {
        decode(new ImageReader.ByteArrayReader(bArr, this.parsers, this.byteArrayPool), i, i2, options, EMPTY_CALLBACKS);
    }

    public boolean handles(ParcelFileDescriptor parcelFileDescriptor) {
        return ParcelFileDescriptorRewinder.isSupported();
    }

    public boolean handles(InputStream inputStream) {
        return true;
    }

    public boolean handles(ByteBuffer byteBuffer) {
        return true;
    }
}
