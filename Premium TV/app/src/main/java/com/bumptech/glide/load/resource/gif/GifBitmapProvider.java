package com.bumptech.glide.load.resource.gif;

import android.graphics.Bitmap;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

/* JADX INFO: loaded from: classes.dex */
public final class GifBitmapProvider implements GifDecoder.BitmapProvider {
    private final ArrayPool arrayPool;
    private final BitmapPool bitmapPool;

    public GifBitmapProvider(BitmapPool bitmapPool) {
        this(bitmapPool, null);
    }

    public GifBitmapProvider(BitmapPool bitmapPool, ArrayPool arrayPool) {
        this.bitmapPool = bitmapPool;
        this.arrayPool = arrayPool;
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder.BitmapProvider
    public Bitmap obtain(int i, int i2, Bitmap.Config config) {
        return this.bitmapPool.getDirty(i, i2, config);
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder.BitmapProvider
    public byte[] obtainByteArray(int i) {
        return this.arrayPool == null ? new byte[i] : (byte[]) this.arrayPool.get(i, byte[].class);
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder.BitmapProvider
    public int[] obtainIntArray(int i) {
        return this.arrayPool == null ? new int[i] : (int[]) this.arrayPool.get(i, int[].class);
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder.BitmapProvider
    public void release(Bitmap bitmap) {
        this.bitmapPool.put(bitmap);
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder.BitmapProvider
    public void release(byte[] bArr) {
        if (this.arrayPool == null) {
            return;
        }
        this.arrayPool.put(bArr);
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder.BitmapProvider
    public void release(int[] iArr) {
        if (this.arrayPool == null) {
            return;
        }
        this.arrayPool.put(iArr);
    }
}
