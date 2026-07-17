package com.bumptech.glide.load.model;

import android.content.res.AssetFileDescriptor;
import android.util.Log;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/* JADX INFO: loaded from: classes.dex */
public class StreamEncoder implements Encoder<InputStream> {
    private static final String TAG = "StreamEncoder";
    private final ArrayPool byteArrayPool;

    public StreamEncoder(ArrayPool arrayPool) {
        this.byteArrayPool = arrayPool;
    }

    /* JADX WARN: Code duplicated, block: B:37:0x004d A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.bumptech.glide.load.Encoder
    public boolean encode(InputStream inputStream, File file, Options options) throws Throwable {
        AssetFileDescriptor.AutoCloseOutputStream autoCloseOutputStream;
        FileOutputStream fileOutputStream;
        boolean z = true;
        byte[] bArr = (byte[]) this.byteArrayPool.get(65536, byte[].class);
        try {
            try {
                fileOutputStream = new FileOutputStream(file);
                while (true) {
                    try {
                        int i = inputStream.read(bArr);
                        if (i == -1) {
                            break;
                        }
                        fileOutputStream.write(bArr, 0, i);
                    } catch (IOException e) {
                        e = e;
                        if (Log.isLoggable(TAG, 3)) {
                            Log.d(TAG, "Failed to encode data onto the OutputStream", e);
                        }
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                                z = false;
                            } catch (IOException e2) {
                                z = false;
                            }
                        } else {
                            z = false;
                        }
                    }
                }
                fileOutputStream.close();
                try {
                    fileOutputStream.close();
                } catch (IOException e3) {
                }
            } catch (IOException e4) {
                e = e4;
                fileOutputStream = null;
            } catch (Throwable th) {
                th = th;
                autoCloseOutputStream = 0;
                if (autoCloseOutputStream != 0) {
                    try {
                        autoCloseOutputStream.close();
                    } catch (IOException e5) {
                    }
                }
                this.byteArrayPool.put(bArr);
                throw th;
            }
            this.byteArrayPool.put(bArr);
            return z;
        } catch (Throwable th2) {
            th = th2;
            autoCloseOutputStream = 65536;
            if (autoCloseOutputStream != 0) {
                autoCloseOutputStream.close();
            }
            this.byteArrayPool.put(bArr);
            throw th;
        }
    }
}
