package com.bumptech.glide.gifdecoder;

import android.graphics.Bitmap;
import android.util.Log;
import androidx.fragment.app.FragmentTransaction;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Iterator;

/* JADX INFO: loaded from: classes.dex */
public class StandardGifDecoder implements GifDecoder {
    private static final int BYTES_PER_INTEGER = 4;
    private static final int COLOR_TRANSPARENT_BLACK = 0;
    private static final int INITIAL_FRAME_POINTER = -1;
    private static final int MASK_INT_LOWEST_BYTE = 255;
    private static final int MAX_STACK_SIZE = 4096;
    private static final int NULL_CODE = -1;
    private static final String TAG = StandardGifDecoder.class.getSimpleName();
    private int[] act;
    private Bitmap.Config bitmapConfig;
    private final GifDecoder.BitmapProvider bitmapProvider;
    private byte[] block;
    private int downsampledHeight;
    private int downsampledWidth;
    private int framePointer;
    private GifHeader header;
    private Boolean isFirstFrameTransparent;
    private byte[] mainPixels;
    private int[] mainScratch;
    private GifHeaderParser parser;
    private final int[] pct;
    private byte[] pixelStack;
    private short[] prefix;
    private Bitmap previousImage;
    private ByteBuffer rawData;
    private int sampleSize;
    private boolean savePrevious;
    private int status;
    private byte[] suffix;

    public StandardGifDecoder(GifDecoder.BitmapProvider bitmapProvider) {
        this.pct = new int[256];
        this.bitmapConfig = Bitmap.Config.ARGB_8888;
        this.bitmapProvider = bitmapProvider;
        this.header = new GifHeader();
    }

    public StandardGifDecoder(GifDecoder.BitmapProvider bitmapProvider, GifHeader gifHeader, ByteBuffer byteBuffer) {
        this(bitmapProvider, gifHeader, byteBuffer, 1);
    }

    public StandardGifDecoder(GifDecoder.BitmapProvider bitmapProvider, GifHeader gifHeader, ByteBuffer byteBuffer, int i) {
        this(bitmapProvider);
        setData(gifHeader, byteBuffer, i);
    }

    private int averageColorsNear(int i, int i2, int i3) {
        int i4 = 0;
        int i5 = 0;
        int i6 = 0;
        int i7 = 0;
        int i8 = 0;
        for (int i9 = i; i9 < this.sampleSize + i && i9 < this.mainPixels.length && i9 < i2; i9++) {
            int i10 = this.act[this.mainPixels[i9] & 255];
            if (i10 != 0) {
                i8 += (i10 >> 24) & 255;
                i7 += (i10 >> 16) & 255;
                i6 += (i10 >> 8) & 255;
                i5 += i10 & 255;
                i4++;
            }
        }
        for (int i11 = i + i3; i11 < i + i3 + this.sampleSize && i11 < this.mainPixels.length && i11 < i2; i11++) {
            int i12 = this.act[this.mainPixels[i11] & 255];
            if (i12 != 0) {
                i8 += (i12 >> 24) & 255;
                i7 += (i12 >> 16) & 255;
                i6 += (i12 >> 8) & 255;
                i5 += i12 & 255;
                i4++;
            }
        }
        if (i4 == 0) {
            return 0;
        }
        return ((i8 / i4) << 24) | ((i7 / i4) << 16) | ((i6 / i4) << 8) | (i5 / i4);
    }

    private void copyCopyIntoScratchRobust(GifFrame gifFrame) {
        int i;
        int i2;
        int i3;
        Boolean bool;
        int[] iArr = this.mainScratch;
        int i4 = gifFrame.ih / this.sampleSize;
        int i5 = gifFrame.iy / this.sampleSize;
        int i6 = gifFrame.iw / this.sampleSize;
        int i7 = gifFrame.ix / this.sampleSize;
        int i8 = 1;
        int i9 = 8;
        int i10 = 0;
        boolean z = this.framePointer == 0;
        int i11 = this.sampleSize;
        int i12 = this.downsampledWidth;
        int i13 = this.downsampledHeight;
        byte[] bArr = this.mainPixels;
        int[] iArr2 = this.act;
        Boolean bool2 = this.isFirstFrameTransparent;
        int i14 = 0;
        while (i14 < i4) {
            if (gifFrame.interlace) {
                if (i10 >= i4) {
                    i8++;
                    switch (i8) {
                        case 2:
                            i10 = 4;
                            break;
                        case 3:
                            i10 = 2;
                            i9 = 4;
                            break;
                        case 4:
                            i10 = 1;
                            i9 = 2;
                            break;
                    }
                }
                i = i10 + i9;
                i2 = i9;
                i3 = i8;
            } else {
                i = i10;
                i2 = i9;
                i3 = i8;
                i10 = i14;
            }
            int i15 = i10 + i5;
            boolean z2 = i11 == 1;
            if (i15 < i13) {
                int i16 = i15 * i12;
                int i17 = i16 + i7;
                int i18 = i17 + i6;
                if (i16 + i12 < i18) {
                    i18 = i16 + i12;
                }
                int i19 = i14 * i11 * gifFrame.iw;
                if (z2) {
                    bool = bool2;
                    while (i17 < i18) {
                        int i20 = iArr2[bArr[i19] & 255];
                        if (i20 != 0) {
                            iArr[i17] = i20;
                        } else if (z && bool == null) {
                            bool = true;
                        }
                        i19 += i11;
                        i17++;
                    }
                } else {
                    int i21 = i19;
                    bool = bool2;
                    for (int i22 = i17; i22 < i18; i22++) {
                        int iAverageColorsNear = averageColorsNear(i21, ((i18 - i17) * i11) + i19, gifFrame.iw);
                        if (iAverageColorsNear != 0) {
                            iArr[i22] = iAverageColorsNear;
                        } else if (z && bool == null) {
                            bool = true;
                        }
                        i21 += i11;
                    }
                }
            } else {
                bool = bool2;
            }
            i14++;
            bool2 = bool;
            i10 = i;
            i9 = i2;
            i8 = i3;
        }
        if (this.isFirstFrameTransparent == null) {
            this.isFirstFrameTransparent = Boolean.valueOf(bool2 == null ? false : bool2.booleanValue());
        }
    }

    private void copyIntoScratchFast(GifFrame gifFrame) {
        int[] iArr = this.mainScratch;
        int i = gifFrame.ih;
        int i2 = gifFrame.iy;
        int i3 = gifFrame.iw;
        int i4 = gifFrame.ix;
        boolean z = this.framePointer == 0;
        int i5 = this.downsampledWidth;
        byte[] bArr = this.mainPixels;
        int[] iArr2 = this.act;
        byte b = -1;
        int i6 = 0;
        while (i6 < i) {
            int i7 = (i6 + i2) * i5;
            int i8 = i7 + i4;
            int i9 = i8 + i3;
            if (i7 + i5 < i9) {
                i9 = i7 + i5;
            }
            int i10 = i8;
            byte b2 = b;
            int i11 = gifFrame.iw * i6;
            while (i10 < i9) {
                byte b3 = bArr[i11];
                int i12 = b3 & 255;
                if (i12 != b2) {
                    int i13 = iArr2[i12];
                    if (i13 != 0) {
                        iArr[i10] = i13;
                    } else {
                        b2 = b3;
                    }
                }
                i10++;
                i11++;
            }
            i6++;
            b = b2;
        }
        this.isFirstFrameTransparent = Boolean.valueOf((this.isFirstFrameTransparent != null && this.isFirstFrameTransparent.booleanValue()) || (this.isFirstFrameTransparent == null && z && b != -1));
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r5v16 */
    /* JADX WARN: Type inference failed for: r5v17 */
    /* JADX WARN: Type inference failed for: r5v18 */
    /* JADX WARN: Type inference failed for: r5v26, types: [short] */
    /* JADX WARN: Type inference failed for: r5v29 */
    private void decodeBitmapData(GifFrame gifFrame) {
        int i;
        int i2;
        short s;
        if (gifFrame != null) {
            this.rawData.position(gifFrame.bufferFrameStart);
        }
        if (gifFrame == null) {
            i = this.header.width;
            i2 = this.header.height;
        } else {
            i = gifFrame.iw;
            i2 = gifFrame.ih;
        }
        int i3 = i * i2;
        if (this.mainPixels == null || this.mainPixels.length < i3) {
            this.mainPixels = this.bitmapProvider.obtainByteArray(i3);
        }
        byte[] bArr = this.mainPixels;
        if (this.prefix == null) {
            this.prefix = new short[4096];
        }
        short[] sArr = this.prefix;
        if (this.suffix == null) {
            this.suffix = new byte[4096];
        }
        byte[] bArr2 = this.suffix;
        if (this.pixelStack == null) {
            this.pixelStack = new byte[FragmentTransaction.TRANSIT_FRAGMENT_OPEN];
        }
        byte[] bArr3 = this.pixelStack;
        int i4 = readByte();
        int i5 = 1 << i4;
        int i6 = i5 + 2;
        int i7 = i4 + 1;
        int i8 = (1 << i7) - 1;
        for (int i9 = 0; i9 < i5; i9++) {
            sArr[i9] = (short) 0;
            bArr2[i9] = (byte) i9;
        }
        byte[] bArr4 = this.block;
        int i10 = 0;
        int i11 = 0;
        int i12 = 0;
        int block = 0;
        int i13 = -1;
        int i14 = i6;
        int i15 = 0;
        int i16 = 0;
        int i17 = 0;
        StandardGifDecoder standardGifDecoder = this;
        int i18 = 0;
        int i19 = i7;
        while (i15 < i3) {
            if (block == 0) {
                block = standardGifDecoder.readBlock();
                if (block <= 0) {
                    standardGifDecoder.status = 3;
                    break;
                }
                i10 = 0;
            }
            int i20 = i18 + ((bArr4[i10] & 255) << i17);
            i10++;
            block--;
            int i21 = i17 + 8;
            i14 = i14;
            i16 = i16;
            StandardGifDecoder standardGifDecoder2 = standardGifDecoder;
            i15 = i15;
            i13 = i13;
            int i22 = i21;
            int i23 = i20;
            int i24 = i19;
            i8 = i8;
            i12 = i12;
            while (true) {
                if (i22 < i24) {
                    i19 = i24;
                    standardGifDecoder = this;
                    i18 = i23;
                    i17 = i22;
                    break;
                }
                int i25 = i23 & i8;
                i23 >>= i24;
                i22 -= i24;
                if (i25 == i5) {
                    i24 = i4 + 1;
                    i8 = (1 << i24) - 1;
                    i14 = i5 + 2;
                    i13 = -1;
                } else {
                    if (i25 == i5 + 1) {
                        i17 = i22;
                        standardGifDecoder = standardGifDecoder2;
                        int i26 = i24;
                        i18 = i23;
                        i19 = i26;
                        break;
                    }
                    if (i13 == -1) {
                        bArr[i16] = bArr2[i25];
                        i15++;
                        i16++;
                        i13 = i25;
                        standardGifDecoder2 = this;
                        i12 = i25;
                    } else {
                        if (i25 >= i14) {
                            bArr3[i11] = (byte) i12;
                            i11++;
                            s = i13;
                        } else {
                            s = i25;
                        }
                        while (s >= i5) {
                            bArr3[i11] = bArr2[s];
                            i11++;
                            s = sArr[s];
                        }
                        int i27 = bArr2[s] & 255;
                        bArr[i16] = (byte) i27;
                        int i28 = i16 + 1;
                        int i29 = i15 + 1;
                        while (i11 > 0) {
                            i11--;
                            bArr[i28] = bArr3[i11];
                            i28++;
                            i29++;
                        }
                        if (i14 < 4096) {
                            sArr[i14] = (short) i13;
                            bArr2[i14] = (byte) i27;
                            i14++;
                            if ((i14 & i8) == 0 && i14 < 4096) {
                                i24++;
                                i8 += i14;
                            }
                        }
                        i15 = i29;
                        i16 = i28;
                        i13 = i25;
                        standardGifDecoder2 = this;
                        i12 = i27;
                    }
                }
            }
        }
        Arrays.fill(bArr, i16, i3, (byte) 0);
    }

    private GifHeaderParser getHeaderParser() {
        if (this.parser == null) {
            this.parser = new GifHeaderParser();
        }
        return this.parser;
    }

    private Bitmap getNextBitmap() {
        Bitmap bitmapObtain = this.bitmapProvider.obtain(this.downsampledWidth, this.downsampledHeight, (this.isFirstFrameTransparent == null || this.isFirstFrameTransparent.booleanValue()) ? Bitmap.Config.ARGB_8888 : this.bitmapConfig);
        bitmapObtain.setHasAlpha(true);
        return bitmapObtain;
    }

    private int readBlock() {
        int i = readByte();
        if (i > 0) {
            this.rawData.get(this.block, 0, Math.min(i, this.rawData.remaining()));
        }
        return i;
    }

    private int readByte() {
        return this.rawData.get() & 255;
    }

    private Bitmap setPixels(GifFrame gifFrame, GifFrame gifFrame2) {
        int i;
        int[] iArr = this.mainScratch;
        if (gifFrame2 == null) {
            if (this.previousImage != null) {
                this.bitmapProvider.release(this.previousImage);
            }
            this.previousImage = null;
            Arrays.fill(iArr, 0);
        }
        if (gifFrame2 != null && gifFrame2.dispose == 3 && this.previousImage == null) {
            Arrays.fill(iArr, 0);
        }
        if (gifFrame2 != null && gifFrame2.dispose > 0) {
            if (gifFrame2.dispose == 2) {
                if (gifFrame.transparency) {
                    i = 0;
                } else {
                    i = this.header.bgColor;
                    if (gifFrame.lct != null && this.header.bgIndex == gifFrame.transIndex) {
                        i = 0;
                    }
                }
                int i2 = gifFrame2.ih / this.sampleSize;
                int i3 = gifFrame2.iy / this.sampleSize;
                int i4 = gifFrame2.iw / this.sampleSize;
                int i5 = (i3 * this.downsampledWidth) + (gifFrame2.ix / this.sampleSize);
                int i6 = this.downsampledWidth;
                int i7 = i5;
                while (i7 < (i6 * i2) + i5) {
                    for (int i8 = i7; i8 < i7 + i4; i8++) {
                        iArr[i8] = i;
                    }
                    i7 += this.downsampledWidth;
                }
            } else if (gifFrame2.dispose == 3 && this.previousImage != null) {
                this.previousImage.getPixels(iArr, 0, this.downsampledWidth, 0, 0, this.downsampledWidth, this.downsampledHeight);
            }
        }
        decodeBitmapData(gifFrame);
        if (gifFrame.interlace || this.sampleSize != 1) {
            copyCopyIntoScratchRobust(gifFrame);
        } else {
            copyIntoScratchFast(gifFrame);
        }
        if (this.savePrevious && (gifFrame.dispose == 0 || gifFrame.dispose == 1)) {
            if (this.previousImage == null) {
                this.previousImage = getNextBitmap();
            }
            this.previousImage.setPixels(iArr, 0, this.downsampledWidth, 0, 0, this.downsampledWidth, this.downsampledHeight);
        }
        Bitmap nextBitmap = getNextBitmap();
        nextBitmap.setPixels(iArr, 0, this.downsampledWidth, 0, 0, this.downsampledWidth, this.downsampledHeight);
        return nextBitmap;
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public void advance() {
        this.framePointer = (this.framePointer + 1) % this.header.frameCount;
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public void clear() {
        this.header = null;
        if (this.mainPixels != null) {
            this.bitmapProvider.release(this.mainPixels);
        }
        if (this.mainScratch != null) {
            this.bitmapProvider.release(this.mainScratch);
        }
        if (this.previousImage != null) {
            this.bitmapProvider.release(this.previousImage);
        }
        this.previousImage = null;
        this.rawData = null;
        this.isFirstFrameTransparent = null;
        if (this.block != null) {
            this.bitmapProvider.release(this.block);
        }
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public int getByteSize() {
        return this.rawData.limit() + this.mainPixels.length + (this.mainScratch.length * 4);
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public int getCurrentFrameIndex() {
        return this.framePointer;
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public ByteBuffer getData() {
        return this.rawData;
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public int getDelay(int i) {
        if (i < 0 || i >= this.header.frameCount) {
            return -1;
        }
        return this.header.frames.get(i).delay;
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public int getFrameCount() {
        return this.header.frameCount;
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public int getHeight() {
        return this.header.height;
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    @Deprecated
    public int getLoopCount() {
        if (this.header.loopCount == -1) {
            return 1;
        }
        return this.header.loopCount;
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public int getNetscapeLoopCount() {
        return this.header.loopCount;
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public int getNextDelay() {
        if (this.header.frameCount <= 0 || this.framePointer < 0) {
            return 0;
        }
        return getDelay(this.framePointer);
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public Bitmap getNextFrame() {
        synchronized (this) {
            if (this.header.frameCount <= 0 || this.framePointer < 0) {
                if (Log.isLoggable(TAG, 3)) {
                    Log.d(TAG, "Unable to decode frame, frameCount=" + this.header.frameCount + ", framePointer=" + this.framePointer);
                }
                this.status = 1;
            }
            if (this.status == 1 || this.status == 2) {
                if (Log.isLoggable(TAG, 3)) {
                    Log.d(TAG, "Unable to decode frame, status=" + this.status);
                }
                return null;
            }
            this.status = 0;
            if (this.block == null) {
                this.block = this.bitmapProvider.obtainByteArray(255);
            }
            GifFrame gifFrame = this.header.frames.get(this.framePointer);
            int i = this.framePointer - 1;
            GifFrame gifFrame2 = i >= 0 ? this.header.frames.get(i) : null;
            this.act = gifFrame.lct != null ? gifFrame.lct : this.header.gct;
            if (this.act == null) {
                if (Log.isLoggable(TAG, 3)) {
                    Log.d(TAG, "No valid color table found for frame #" + this.framePointer);
                }
                this.status = 1;
                return null;
            }
            if (gifFrame.transparency) {
                System.arraycopy(this.act, 0, this.pct, 0, this.act.length);
                this.act = this.pct;
                this.act[gifFrame.transIndex] = 0;
                if (gifFrame.dispose == 2 && this.framePointer == 0) {
                    this.isFirstFrameTransparent = true;
                }
            }
            return setPixels(gifFrame, gifFrame2);
        }
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public int getStatus() {
        return this.status;
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public int getTotalIterationCount() {
        if (this.header.loopCount == -1) {
            return 1;
        }
        if (this.header.loopCount == 0) {
            return 0;
        }
        return this.header.loopCount + 1;
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public int getWidth() {
        return this.header.width;
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public int read(InputStream inputStream, int i) {
        if (inputStream != null) {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(i > 0 ? i + 4096 : 16384);
                byte[] bArr = new byte[16384];
                while (true) {
                    int i2 = inputStream.read(bArr, 0, bArr.length);
                    if (i2 == -1) {
                        break;
                    }
                    byteArrayOutputStream.write(bArr, 0, i2);
                }
                byteArrayOutputStream.flush();
                read(byteArrayOutputStream.toByteArray());
            } catch (IOException e) {
                Log.w(TAG, "Error reading data from stream", e);
            }
        } else {
            this.status = 2;
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e2) {
                Log.w(TAG, "Error closing stream", e2);
            }
        }
        return this.status;
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public int read(byte[] bArr) {
        int i;
        synchronized (this) {
            this.header = getHeaderParser().setData(bArr).parseHeader();
            if (bArr != null) {
                setData(this.header, bArr);
            }
            i = this.status;
        }
        return i;
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public void resetFrameIndex() {
        this.framePointer = -1;
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public void setData(GifHeader gifHeader, ByteBuffer byteBuffer) {
        synchronized (this) {
            setData(gifHeader, byteBuffer, 1);
        }
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public void setData(GifHeader gifHeader, ByteBuffer byteBuffer, int i) {
        synchronized (this) {
            try {
                if (i <= 0) {
                    throw new IllegalArgumentException("Sample size must be >=0, not: " + i);
                }
                int iHighestOneBit = Integer.highestOneBit(i);
                this.status = 0;
                this.header = gifHeader;
                this.framePointer = -1;
                this.rawData = byteBuffer.asReadOnlyBuffer();
                this.rawData.position(0);
                this.rawData.order(ByteOrder.LITTLE_ENDIAN);
                this.savePrevious = false;
                Iterator<GifFrame> it = gifHeader.frames.iterator();
                while (it.hasNext()) {
                    if (it.next().dispose == 3) {
                        this.savePrevious = true;
                        break;
                    }
                }
                this.sampleSize = iHighestOneBit;
                this.downsampledWidth = gifHeader.width / iHighestOneBit;
                this.downsampledHeight = gifHeader.height / iHighestOneBit;
                this.mainPixels = this.bitmapProvider.obtainByteArray(gifHeader.width * gifHeader.height);
                this.mainScratch = this.bitmapProvider.obtainIntArray(this.downsampledWidth * this.downsampledHeight);
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public void setData(GifHeader gifHeader, byte[] bArr) {
        synchronized (this) {
            setData(gifHeader, ByteBuffer.wrap(bArr));
        }
    }

    @Override // com.bumptech.glide.gifdecoder.GifDecoder
    public void setDefaultBitmapConfig(Bitmap.Config config) {
        if (config != Bitmap.Config.ARGB_8888 && config != Bitmap.Config.RGB_565) {
            throw new IllegalArgumentException("Unsupported format: " + config + ", must be one of " + Bitmap.Config.ARGB_8888 + " or " + Bitmap.Config.RGB_565);
        }
        this.bitmapConfig = config;
    }
}
