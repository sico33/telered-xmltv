package com.bumptech.glide.load.resource.bitmap;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaDataSource;
import android.media.MediaExtractor;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.media3.common.MimeTypes;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class VideoDecoder<T> implements ResourceDecoder<T, Bitmap> {
    public static final long DEFAULT_FRAME = -1;
    static final int DEFAULT_FRAME_OPTION = 2;
    private static final String TAG = "VideoDecoder";
    private static final String WEBM_MIME_TYPE = "video/webm";
    private final BitmapPool bitmapPool;
    private final MediaMetadataRetrieverFactory factory;
    private final MediaInitializer<T> initializer;
    public static final Option<Long> TARGET_FRAME = Option.disk("com.bumptech.glide.load.resource.bitmap.VideoBitmapDecode.TargetFrame", -1L, new Option.CacheKeyUpdater<Long>() { // from class: com.bumptech.glide.load.resource.bitmap.VideoDecoder.1
        private final ByteBuffer buffer = ByteBuffer.allocate(8);

        @Override // com.bumptech.glide.load.Option.CacheKeyUpdater
        public void update(byte[] bArr, Long l, MessageDigest messageDigest) {
            messageDigest.update(bArr);
            synchronized (this.buffer) {
                this.buffer.position(0);
                messageDigest.update(this.buffer.putLong(l.longValue()).array());
            }
        }
    });
    public static final Option<Integer> FRAME_OPTION = Option.disk("com.bumptech.glide.load.resource.bitmap.VideoBitmapDecode.FrameOption", 2, new Option.CacheKeyUpdater<Integer>() { // from class: com.bumptech.glide.load.resource.bitmap.VideoDecoder.2
        private final ByteBuffer buffer = ByteBuffer.allocate(4);

        @Override // com.bumptech.glide.load.Option.CacheKeyUpdater
        public void update(byte[] bArr, Integer num, MessageDigest messageDigest) {
            if (num == null) {
                return;
            }
            messageDigest.update(bArr);
            synchronized (this.buffer) {
                this.buffer.position(0);
                messageDigest.update(this.buffer.putInt(num.intValue()).array());
            }
        }
    });
    private static final MediaMetadataRetrieverFactory DEFAULT_FACTORY = new MediaMetadataRetrieverFactory();
    private static final List<String> PIXEL_T_BUILD_ID_PREFIXES_REQUIRING_HDR_180_ROTATION_FIX = Collections.unmodifiableList(Arrays.asList("TP1A", "TD1A.220804.031"));

    private static final class AssetFileDescriptorInitializer implements MediaInitializer<AssetFileDescriptor> {
        private AssetFileDescriptorInitializer() {
        }

        @Override // com.bumptech.glide.load.resource.bitmap.VideoDecoder.MediaInitializer
        public void initializeExtractor(MediaExtractor mediaExtractor, AssetFileDescriptor assetFileDescriptor) throws IOException {
            mediaExtractor.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
        }

        @Override // com.bumptech.glide.load.resource.bitmap.VideoDecoder.MediaInitializer
        public void initializeRetriever(MediaMetadataRetriever mediaMetadataRetriever, AssetFileDescriptor assetFileDescriptor) {
            mediaMetadataRetriever.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
        }
    }

    static final class ByteBufferInitializer implements MediaInitializer<ByteBuffer> {
        ByteBufferInitializer() {
        }

        private MediaDataSource getMediaDataSource(ByteBuffer byteBuffer) {
            return new MediaDataSource(this, byteBuffer) { // from class: com.bumptech.glide.load.resource.bitmap.VideoDecoder.ByteBufferInitializer.1
                final ByteBufferInitializer this$0;
                final ByteBuffer val$data;

                {
                    this.this$0 = this;
                    this.val$data = byteBuffer;
                }

                @Override // java.io.Closeable, java.lang.AutoCloseable
                public void close() {
                }

                @Override // android.media.MediaDataSource
                public long getSize() {
                    return this.val$data.limit();
                }

                @Override // android.media.MediaDataSource
                public int readAt(long j, byte[] bArr, int i, int i2) {
                    if (j >= this.val$data.limit()) {
                        return -1;
                    }
                    this.val$data.position((int) j);
                    int iMin = Math.min(i2, this.val$data.remaining());
                    this.val$data.get(bArr, i, iMin);
                    return iMin;
                }
            };
        }

        @Override // com.bumptech.glide.load.resource.bitmap.VideoDecoder.MediaInitializer
        public void initializeExtractor(MediaExtractor mediaExtractor, ByteBuffer byteBuffer) throws IOException {
            mediaExtractor.setDataSource(getMediaDataSource(byteBuffer));
        }

        @Override // com.bumptech.glide.load.resource.bitmap.VideoDecoder.MediaInitializer
        public void initializeRetriever(MediaMetadataRetriever mediaMetadataRetriever, ByteBuffer byteBuffer) {
            mediaMetadataRetriever.setDataSource(getMediaDataSource(byteBuffer));
        }
    }

    interface MediaInitializer<T> {
        void initializeExtractor(MediaExtractor mediaExtractor, T t) throws IOException;

        void initializeRetriever(MediaMetadataRetriever mediaMetadataRetriever, T t);
    }

    static class MediaMetadataRetrieverFactory {
        MediaMetadataRetrieverFactory() {
        }

        public MediaMetadataRetriever build() {
            return new MediaMetadataRetriever();
        }
    }

    static final class ParcelFileDescriptorInitializer implements MediaInitializer<ParcelFileDescriptor> {
        ParcelFileDescriptorInitializer() {
        }

        @Override // com.bumptech.glide.load.resource.bitmap.VideoDecoder.MediaInitializer
        public void initializeExtractor(MediaExtractor mediaExtractor, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
            mediaExtractor.setDataSource(parcelFileDescriptor.getFileDescriptor());
        }

        @Override // com.bumptech.glide.load.resource.bitmap.VideoDecoder.MediaInitializer
        public void initializeRetriever(MediaMetadataRetriever mediaMetadataRetriever, ParcelFileDescriptor parcelFileDescriptor) {
            mediaMetadataRetriever.setDataSource(parcelFileDescriptor.getFileDescriptor());
        }
    }

    private static final class VideoDecoderException extends RuntimeException {
        private static final long serialVersionUID = -2556382523004027815L;

        VideoDecoderException() {
            super("MediaMetadataRetriever failed to retrieve a frame without throwing, check the adb logs for .*MetadataRetriever.* prior to this exception for details");
        }
    }

    VideoDecoder(BitmapPool bitmapPool, MediaInitializer<T> mediaInitializer) {
        this(bitmapPool, mediaInitializer, DEFAULT_FACTORY);
    }

    VideoDecoder(BitmapPool bitmapPool, MediaInitializer<T> mediaInitializer, MediaMetadataRetrieverFactory mediaMetadataRetrieverFactory) {
        this.bitmapPool = bitmapPool;
        this.initializer = mediaInitializer;
        this.factory = mediaMetadataRetrieverFactory;
    }

    public static ResourceDecoder<AssetFileDescriptor, Bitmap> asset(BitmapPool bitmapPool) {
        return new VideoDecoder(bitmapPool, new AssetFileDescriptorInitializer());
    }

    public static ResourceDecoder<ByteBuffer, Bitmap> byteBuffer(BitmapPool bitmapPool) {
        return new VideoDecoder(bitmapPool, new ByteBufferInitializer());
    }

    private static Bitmap correctHdr180DegVideoFrameOrientation(MediaMetadataRetriever mediaMetadataRetriever, Bitmap bitmap) {
        boolean z;
        if (!isHdr180RotationFixRequired()) {
            return bitmap;
        }
        try {
            z = isHDR(mediaMetadataRetriever) && Math.abs(Integer.parseInt(mediaMetadataRetriever.extractMetadata(24))) == 180;
        } catch (NumberFormatException e) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "Exception trying to extract HDR transfer function or rotation");
                z = false;
            } else {
                z = false;
            }
        }
        if (!z) {
            return bitmap;
        }
        if (Log.isLoggable(TAG, 3)) {
            Log.d(TAG, "Applying HDR 180 deg thumbnail correction");
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(180.0f, bitmap.getWidth() / 2.0f, bitmap.getHeight() / 2.0f);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private Bitmap decodeFrame(T t, MediaMetadataRetriever mediaMetadataRetriever, long j, int i, int i2, int i3, DownsampleStrategy downsampleStrategy) {
        if (isUnsupportedFormat(t, mediaMetadataRetriever)) {
            throw new IllegalStateException("Cannot decode VP8 video on CrOS.");
        }
        Bitmap bitmapDecodeOriginalFrame = null;
        if (Build.VERSION.SDK_INT >= 27 && i2 != Integer.MIN_VALUE && i3 != Integer.MIN_VALUE && downsampleStrategy != DownsampleStrategy.NONE) {
            bitmapDecodeOriginalFrame = decodeScaledFrame(mediaMetadataRetriever, j, i, i2, i3, downsampleStrategy);
        }
        if (bitmapDecodeOriginalFrame == null) {
            bitmapDecodeOriginalFrame = decodeOriginalFrame(mediaMetadataRetriever, j, i);
        }
        Bitmap bitmapCorrectHdr180DegVideoFrameOrientation = correctHdr180DegVideoFrameOrientation(mediaMetadataRetriever, bitmapDecodeOriginalFrame);
        if (bitmapCorrectHdr180DegVideoFrameOrientation != null) {
            return bitmapCorrectHdr180DegVideoFrameOrientation;
        }
        throw new VideoDecoderException();
    }

    private static Bitmap decodeOriginalFrame(MediaMetadataRetriever mediaMetadataRetriever, long j, int i) {
        return mediaMetadataRetriever.getFrameAtTime(j, i);
    }

    private static Bitmap decodeScaledFrame(MediaMetadataRetriever mediaMetadataRetriever, long j, int i, int i2, int i3, DownsampleStrategy downsampleStrategy) {
        try {
            int i4 = Integer.parseInt(mediaMetadataRetriever.extractMetadata(18));
            int i5 = Integer.parseInt(mediaMetadataRetriever.extractMetadata(19));
            int i6 = Integer.parseInt(mediaMetadataRetriever.extractMetadata(24));
            if (i6 == 90 || i6 == 270) {
                i4 = i5;
                i5 = i4;
            }
            try {
                float scaleFactor = downsampleStrategy.getScaleFactor(i4, i5, i2, i3);
                return mediaMetadataRetriever.getScaledFrameAtTime(j, i, Math.round(i4 * scaleFactor), Math.round(i5 * scaleFactor));
            } catch (Throwable th) {
                th = th;
                if (Log.isLoggable(TAG, 3)) {
                    Log.d(TAG, "Exception trying to decode a scaled frame on oreo+, falling back to a fullsize frame", th);
                }
                return null;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private static boolean isHDR(MediaMetadataRetriever mediaMetadataRetriever) throws NumberFormatException {
        String strExtractMetadata = mediaMetadataRetriever.extractMetadata(36);
        String strExtractMetadata2 = mediaMetadataRetriever.extractMetadata(35);
        int i = Integer.parseInt(strExtractMetadata);
        return (i == 7 || i == 6) && Integer.parseInt(strExtractMetadata2) == 6;
    }

    static boolean isHdr180RotationFixRequired() {
        if (Build.MODEL.startsWith("Pixel") && Build.VERSION.SDK_INT == 33) {
            return isTBuildRequiringRotationFix();
        }
        return Build.VERSION.SDK_INT >= 30 && Build.VERSION.SDK_INT < 33;
    }

    private static boolean isTBuildRequiringRotationFix() {
        Iterator<String> it = PIXEL_T_BUILD_ID_PREFIXES_REQUIRING_HDR_180_ROTATION_FIX.iterator();
        while (it.hasNext()) {
            if (Build.ID.startsWith(it.next())) {
                return true;
            }
        }
        return false;
    }

    /* JADX WARN: Code duplicated, block: B:26:0x0061 A[Catch: all -> 0x006f, TRY_LEAVE, TryCatch #2 {all -> 0x006f, blocks: (B:24:0x0058, B:26:0x0061), top: B:40:0x0058 }] */
    private boolean isUnsupportedFormat(T t, MediaMetadataRetriever mediaMetadataRetriever) {
        Throwable th;
        MediaExtractor mediaExtractor;
        if (!(Build.DEVICE != null && Build.DEVICE.matches(".+_cheets|cheets_.+"))) {
            return false;
        }
        try {
            try {
                if (!"video/webm".equals(mediaMetadataRetriever.extractMetadata(12))) {
                    return false;
                }
                mediaExtractor = new MediaExtractor();
                try {
                    this.initializer.initializeExtractor(mediaExtractor, t);
                    int trackCount = mediaExtractor.getTrackCount();
                    for (int i = 0; i < trackCount; i++) {
                        if (MimeTypes.VIDEO_VP8.equals(mediaExtractor.getTrackFormat(i).getString("mime"))) {
                            mediaExtractor.release();
                            return true;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (Log.isLoggable(TAG, 3)) {
                        Log.d(TAG, "Exception trying to extract track info for a webm video on CrOS.", th);
                    }
                    return false;
                }
                return false;
            } catch (Throwable th3) {
                th = th3;
                mediaExtractor = null;
            }
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "Exception trying to extract track info for a webm video on CrOS.", th);
            }
            return false;
        } finally {
            if (mediaExtractor != null) {
                mediaExtractor.release();
            }
        }
    }

    public static ResourceDecoder<ParcelFileDescriptor, Bitmap> parcel(BitmapPool bitmapPool) {
        return new VideoDecoder(bitmapPool, new ParcelFileDescriptorInitializer());
    }

    @Override // com.bumptech.glide.load.ResourceDecoder
    public Resource<Bitmap> decode(T t, int i, int i2, Options options) throws Exception {
        long jLongValue = ((Long) options.get(TARGET_FRAME)).longValue();
        if (jLongValue < 0 && jLongValue != -1) {
            throw new IllegalArgumentException("Requested frame must be non-negative, or DEFAULT_FRAME, given: " + jLongValue);
        }
        Integer num = (Integer) options.get(FRAME_OPTION);
        Integer num2 = num == null ? 2 : num;
        DownsampleStrategy downsampleStrategy = (DownsampleStrategy) options.get(DownsampleStrategy.OPTION);
        DownsampleStrategy downsampleStrategy2 = downsampleStrategy == null ? DownsampleStrategy.DEFAULT : downsampleStrategy;
        MediaMetadataRetriever mediaMetadataRetrieverBuild = this.factory.build();
        try {
            this.initializer.initializeRetriever(mediaMetadataRetrieverBuild, t);
            return BitmapResource.obtain(decodeFrame(t, mediaMetadataRetrieverBuild, jLongValue, num2.intValue(), i, i2, downsampleStrategy2), this.bitmapPool);
        } finally {
            if (Build.VERSION.SDK_INT >= 29) {
                VideoDecoder$$ExternalSyntheticAutoCloseableDispatcher0.m(mediaMetadataRetrieverBuild);
            } else {
                mediaMetadataRetrieverBuild.release();
            }
        }
    }

    @Override // com.bumptech.glide.load.ResourceDecoder
    public boolean handles(T t, Options options) {
        return true;
    }
}
