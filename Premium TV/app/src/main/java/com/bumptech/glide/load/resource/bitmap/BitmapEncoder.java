package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.util.Log;
import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.data.BufferedOutputStream;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Util;
import com.bumptech.glide.util.pool.GlideTrace;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/* JADX INFO: loaded from: classes.dex */
public class BitmapEncoder implements ResourceEncoder<Bitmap> {
    private static final String TAG = "BitmapEncoder";
    private final ArrayPool arrayPool;
    public static final Option<Integer> COMPRESSION_QUALITY = Option.memory("com.bumptech.glide.load.resource.bitmap.BitmapEncoder.CompressionQuality", 90);
    public static final Option<Bitmap.CompressFormat> COMPRESSION_FORMAT = Option.memory("com.bumptech.glide.load.resource.bitmap.BitmapEncoder.CompressionFormat");

    @Deprecated
    public BitmapEncoder() {
        this.arrayPool = null;
    }

    public BitmapEncoder(ArrayPool arrayPool) {
        this.arrayPool = arrayPool;
    }

    private Bitmap.CompressFormat getFormat(Bitmap bitmap, Options options) {
        Bitmap.CompressFormat compressFormat = (Bitmap.CompressFormat) options.get(COMPRESSION_FORMAT);
        if (compressFormat != null) {
            return compressFormat;
        }
        return bitmap.hasAlpha() ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
    }

    /* JADX WARN: Code duplicated, block: B:13:0x0056 A[Catch: all -> 0x00ca, TRY_LEAVE, TryCatch #5 {all -> 0x00ca, blocks: (B:3:0x0021, B:10:0x004a, B:11:0x004d, B:13:0x0056, B:26:0x00c6, B:27:0x00c9), top: B:51:0x0021 }] */
    @Override // com.bumptech.glide.load.Encoder
    public boolean encode(Resource<Bitmap> resource, File file, Options options) {
        OutputStream fileOutputStream;
        boolean z;
        Bitmap bitmap = resource.get();
        Bitmap.CompressFormat format = getFormat(bitmap, options);
        GlideTrace.beginSectionFormat("encode: [%dx%d] %s", Integer.valueOf(bitmap.getWidth()), Integer.valueOf(bitmap.getHeight()), format);
        try {
            long logTime = LogTime.getLogTime();
            int iIntValue = ((Integer) options.get(COMPRESSION_QUALITY)).intValue();
            try {
                fileOutputStream = new FileOutputStream(file);
                try {
                    try {
                        OutputStream bufferedOutputStream = this.arrayPool != null ? new BufferedOutputStream(fileOutputStream, this.arrayPool) : fileOutputStream;
                        try {
                            bitmap.compress(format, iIntValue, bufferedOutputStream);
                            bufferedOutputStream.close();
                            fileOutputStream = bufferedOutputStream;
                            z = true;
                        } catch (IOException e) {
                            fileOutputStream = bufferedOutputStream;
                            e = e;
                            if (Log.isLoggable(TAG, 3)) {
                                Log.d(TAG, "Failed to encode Bitmap", e);
                            }
                            if (fileOutputStream != null) {
                                z = false;
                            } else {
                                z = false;
                            }
                            if (Log.isLoggable(TAG, 2)) {
                                Log.v(TAG, "Compressed with type: " + format + " of size " + Util.getBitmapByteSize(bitmap) + " in " + LogTime.getElapsedMillis(logTime) + ", options format: " + options.get(COMPRESSION_FORMAT) + ", hasAlpha: " + bitmap.hasAlpha());
                            }
                            GlideTrace.endSection();
                            return z;
                        } catch (Throwable th) {
                            th = th;
                            fileOutputStream = bufferedOutputStream;
                            if (fileOutputStream != null) {
                                try {
                                    fileOutputStream.close();
                                } catch (IOException e2) {
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } catch (IOException e3) {
                    e = e3;
                }
            } catch (IOException e4) {
                e = e4;
                fileOutputStream = null;
            } catch (Throwable th3) {
                th = th3;
                fileOutputStream = null;
            }
            try {
                fileOutputStream.close();
            } catch (IOException e5) {
            }
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "Compressed with type: " + format + " of size " + Util.getBitmapByteSize(bitmap) + " in " + LogTime.getElapsedMillis(logTime) + ", options format: " + options.get(COMPRESSION_FORMAT) + ", hasAlpha: " + bitmap.hasAlpha());
            }
            GlideTrace.endSection();
            return z;
        } catch (Throwable th4) {
            GlideTrace.endSection();
            throw th4;
        }
    }

    @Override // com.bumptech.glide.load.ResourceEncoder
    public EncodeStrategy getEncodeStrategy(Options options) {
        return EncodeStrategy.TRANSFORMED;
    }
}
