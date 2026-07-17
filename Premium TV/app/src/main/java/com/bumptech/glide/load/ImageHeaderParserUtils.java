package com.bumptech.glide.load;

import com.bumptech.glide.load.data.ParcelFileDescriptorRewinder;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.resource.bitmap.RecyclableBufferedInputStream;
import com.bumptech.glide.util.ByteBufferUtil;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class ImageHeaderParserUtils {
    private static final int MARK_READ_LIMIT = 5242880;

    private interface OrientationReader {
        int getOrientationAndRewind(ImageHeaderParser imageHeaderParser) throws IOException;
    }

    private interface TypeReader {
        ImageHeaderParser.ImageType getTypeAndRewind(ImageHeaderParser imageHeaderParser) throws IOException;
    }

    private ImageHeaderParserUtils() {
    }

    public static int getOrientation(List<ImageHeaderParser> list, ParcelFileDescriptorRewinder parcelFileDescriptorRewinder, ArrayPool arrayPool) throws IOException {
        return getOrientationInternal(list, new OrientationReader(parcelFileDescriptorRewinder, arrayPool) { // from class: com.bumptech.glide.load.ImageHeaderParserUtils.6
            final ArrayPool val$byteArrayPool;
            final ParcelFileDescriptorRewinder val$parcelFileDescriptorRewinder;

            {
                this.val$parcelFileDescriptorRewinder = parcelFileDescriptorRewinder;
                this.val$byteArrayPool = arrayPool;
            }

            @Override // com.bumptech.glide.load.ImageHeaderParserUtils.OrientationReader
            public int getOrientationAndRewind(ImageHeaderParser imageHeaderParser) throws Throwable {
                RecyclableBufferedInputStream recyclableBufferedInputStream;
                try {
                    recyclableBufferedInputStream = new RecyclableBufferedInputStream(new FileInputStream(this.val$parcelFileDescriptorRewinder.rewindAndGet().getFileDescriptor()), this.val$byteArrayPool);
                    try {
                        int orientation = imageHeaderParser.getOrientation(recyclableBufferedInputStream, this.val$byteArrayPool);
                        recyclableBufferedInputStream.release();
                        this.val$parcelFileDescriptorRewinder.rewindAndGet();
                        return orientation;
                    } catch (Throwable th) {
                        th = th;
                        if (recyclableBufferedInputStream != null) {
                            recyclableBufferedInputStream.release();
                        }
                        this.val$parcelFileDescriptorRewinder.rewindAndGet();
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    recyclableBufferedInputStream = null;
                }
            }
        });
    }

    public static int getOrientation(List<ImageHeaderParser> list, InputStream inputStream, ArrayPool arrayPool) throws IOException {
        if (inputStream == null) {
            return -1;
        }
        if (!inputStream.markSupported()) {
            inputStream = new RecyclableBufferedInputStream(inputStream, arrayPool);
        }
        inputStream.mark(MARK_READ_LIMIT);
        return getOrientationInternal(list, new OrientationReader(inputStream, arrayPool) { // from class: com.bumptech.glide.load.ImageHeaderParserUtils.5
            final ArrayPool val$byteArrayPool;
            final InputStream val$finalIs;

            {
                this.val$finalIs = inputStream;
                this.val$byteArrayPool = arrayPool;
            }

            @Override // com.bumptech.glide.load.ImageHeaderParserUtils.OrientationReader
            public int getOrientationAndRewind(ImageHeaderParser imageHeaderParser) throws IOException {
                try {
                    return imageHeaderParser.getOrientation(this.val$finalIs, this.val$byteArrayPool);
                } finally {
                    this.val$finalIs.reset();
                }
            }
        });
    }

    public static int getOrientation(List<ImageHeaderParser> list, ByteBuffer byteBuffer, ArrayPool arrayPool) throws IOException {
        if (byteBuffer == null) {
            return -1;
        }
        return getOrientationInternal(list, new OrientationReader(byteBuffer, arrayPool) { // from class: com.bumptech.glide.load.ImageHeaderParserUtils.4
            final ArrayPool val$arrayPool;
            final ByteBuffer val$buffer;

            {
                this.val$buffer = byteBuffer;
                this.val$arrayPool = arrayPool;
            }

            @Override // com.bumptech.glide.load.ImageHeaderParserUtils.OrientationReader
            public int getOrientationAndRewind(ImageHeaderParser imageHeaderParser) throws IOException {
                try {
                    return imageHeaderParser.getOrientation(this.val$buffer, this.val$arrayPool);
                } finally {
                    ByteBufferUtil.rewind(this.val$buffer);
                }
            }
        });
    }

    private static int getOrientationInternal(List<ImageHeaderParser> list, OrientationReader orientationReader) throws IOException {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            int orientationAndRewind = orientationReader.getOrientationAndRewind(list.get(i));
            if (orientationAndRewind != -1) {
                return orientationAndRewind;
            }
        }
        return -1;
    }

    public static ImageHeaderParser.ImageType getType(List<ImageHeaderParser> list, ParcelFileDescriptorRewinder parcelFileDescriptorRewinder, ArrayPool arrayPool) throws IOException {
        return getTypeInternal(list, new TypeReader(parcelFileDescriptorRewinder, arrayPool) { // from class: com.bumptech.glide.load.ImageHeaderParserUtils.3
            final ArrayPool val$byteArrayPool;
            final ParcelFileDescriptorRewinder val$parcelFileDescriptorRewinder;

            {
                this.val$parcelFileDescriptorRewinder = parcelFileDescriptorRewinder;
                this.val$byteArrayPool = arrayPool;
            }

            @Override // com.bumptech.glide.load.ImageHeaderParserUtils.TypeReader
            public ImageHeaderParser.ImageType getTypeAndRewind(ImageHeaderParser imageHeaderParser) throws Throwable {
                RecyclableBufferedInputStream recyclableBufferedInputStream;
                try {
                    recyclableBufferedInputStream = new RecyclableBufferedInputStream(new FileInputStream(this.val$parcelFileDescriptorRewinder.rewindAndGet().getFileDescriptor()), this.val$byteArrayPool);
                    try {
                        ImageHeaderParser.ImageType type = imageHeaderParser.getType(recyclableBufferedInputStream);
                        recyclableBufferedInputStream.release();
                        this.val$parcelFileDescriptorRewinder.rewindAndGet();
                        return type;
                    } catch (Throwable th) {
                        th = th;
                        if (recyclableBufferedInputStream != null) {
                            recyclableBufferedInputStream.release();
                        }
                        this.val$parcelFileDescriptorRewinder.rewindAndGet();
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    recyclableBufferedInputStream = null;
                }
            }
        });
    }

    public static ImageHeaderParser.ImageType getType(List<ImageHeaderParser> list, InputStream inputStream, ArrayPool arrayPool) throws IOException {
        if (inputStream == null) {
            return ImageHeaderParser.ImageType.UNKNOWN;
        }
        if (!inputStream.markSupported()) {
            inputStream = new RecyclableBufferedInputStream(inputStream, arrayPool);
        }
        inputStream.mark(MARK_READ_LIMIT);
        return getTypeInternal(list, new TypeReader(inputStream) { // from class: com.bumptech.glide.load.ImageHeaderParserUtils.1
            final InputStream val$finalIs;

            {
                this.val$finalIs = inputStream;
            }

            @Override // com.bumptech.glide.load.ImageHeaderParserUtils.TypeReader
            public ImageHeaderParser.ImageType getTypeAndRewind(ImageHeaderParser imageHeaderParser) throws IOException {
                try {
                    return imageHeaderParser.getType(this.val$finalIs);
                } finally {
                    this.val$finalIs.reset();
                }
            }
        });
    }

    public static ImageHeaderParser.ImageType getType(List<ImageHeaderParser> list, ByteBuffer byteBuffer) throws IOException {
        return byteBuffer == null ? ImageHeaderParser.ImageType.UNKNOWN : getTypeInternal(list, new TypeReader(byteBuffer) { // from class: com.bumptech.glide.load.ImageHeaderParserUtils.2
            final ByteBuffer val$buffer;

            {
                this.val$buffer = byteBuffer;
            }

            @Override // com.bumptech.glide.load.ImageHeaderParserUtils.TypeReader
            public ImageHeaderParser.ImageType getTypeAndRewind(ImageHeaderParser imageHeaderParser) throws IOException {
                try {
                    return imageHeaderParser.getType(this.val$buffer);
                } finally {
                    ByteBufferUtil.rewind(this.val$buffer);
                }
            }
        });
    }

    private static ImageHeaderParser.ImageType getTypeInternal(List<ImageHeaderParser> list, TypeReader typeReader) throws IOException {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            ImageHeaderParser.ImageType typeAndRewind = typeReader.getTypeAndRewind(list.get(i));
            if (typeAndRewind != ImageHeaderParser.ImageType.UNKNOWN) {
                return typeAndRewind;
            }
        }
        return ImageHeaderParser.ImageType.UNKNOWN;
    }
}
