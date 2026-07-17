package com.bumptech.glide.load.resource.bitmap;

import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/* JADX INFO: loaded from: classes.dex */
public class RecyclableBufferedInputStream extends FilterInputStream {
    private volatile byte[] buf;
    private final ArrayPool byteArrayPool;
    private int count;
    private int marklimit;
    private int markpos;
    private int pos;

    static class InvalidMarkException extends IOException {
        private static final long serialVersionUID = -4338378848813561757L;

        InvalidMarkException(String str) {
            super(str);
        }
    }

    public RecyclableBufferedInputStream(InputStream inputStream, ArrayPool arrayPool) {
        this(inputStream, arrayPool, 65536);
    }

    RecyclableBufferedInputStream(InputStream inputStream, ArrayPool arrayPool, int i) {
        super(inputStream);
        this.markpos = -1;
        this.byteArrayPool = arrayPool;
        this.buf = (byte[]) arrayPool.get(i, byte[].class);
    }

    /* JADX WARN: Code duplicated, block: B:27:0x0074  */
    private int fillbuf(InputStream inputStream, byte[] bArr) throws IOException {
        byte[] bArr2;
        int i;
        int i2;
        if (this.markpos == -1 || this.pos - this.markpos >= this.marklimit) {
            int i3 = inputStream.read(bArr);
            if (i3 <= 0) {
                return i3;
            }
            this.markpos = -1;
            this.pos = 0;
            this.count = i3;
            return i3;
        }
        if (this.markpos != 0 || this.marklimit <= bArr.length || this.count != bArr.length) {
            if (this.markpos > 0) {
                System.arraycopy(bArr, this.markpos, bArr, 0, bArr.length - this.markpos);
            } else {
                bArr2 = bArr;
            }
            this.pos -= this.markpos;
            this.markpos = 0;
            this.count = 0;
            i = inputStream.read(bArr, this.pos, bArr.length - this.pos);
            i2 = this.pos;
            if (i > 0) {
                i2 += i;
            }
            this.count = i2;
            return i;
        }
        int length = bArr.length * 2;
        if (length > this.marklimit) {
            length = this.marklimit;
        }
        bArr2 = (byte[]) this.byteArrayPool.get(length, byte[].class);
        System.arraycopy(bArr, 0, bArr2, 0, bArr.length);
        this.buf = bArr2;
        this.byteArrayPool.put(bArr);
        bArr = bArr2;
        this.pos -= this.markpos;
        this.markpos = 0;
        this.count = 0;
        i = inputStream.read(bArr, this.pos, bArr.length - this.pos);
        i2 = this.pos;
        if (i > 0) {
            i2 += i;
        }
        this.count = i2;
        return i;
    }

    private static IOException streamClosed() throws IOException {
        throw new IOException("BufferedInputStream is closed");
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int available() throws IOException {
        int i;
        int i2;
        int iAvailable;
        synchronized (this) {
            InputStream inputStream = this.in;
            if (this.buf == null || inputStream == null) {
                throw streamClosed();
            }
            i = this.count;
            i2 = this.pos;
            iAvailable = inputStream.available();
        }
        return iAvailable + (i - i2);
    }

    @Override // java.io.FilterInputStream, java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
    public void close() throws IOException {
        if (this.buf != null) {
            this.byteArrayPool.put(this.buf);
            this.buf = null;
        }
        InputStream inputStream = this.in;
        this.in = null;
        if (inputStream != null) {
            inputStream.close();
        }
    }

    public void fixMarkLimit() {
        synchronized (this) {
            this.marklimit = this.buf.length;
        }
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public void mark(int i) {
        synchronized (this) {
            this.marklimit = Math.max(this.marklimit, i);
            this.markpos = this.pos;
        }
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public boolean markSupported() {
        return true;
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int read() throws IOException {
        synchronized (this) {
            byte[] bArr = this.buf;
            InputStream inputStream = this.in;
            if (bArr == null || inputStream == null) {
                throw streamClosed();
            }
            if (this.pos >= this.count && fillbuf(inputStream, bArr) == -1) {
                return -1;
            }
            if (bArr != this.buf && (bArr = this.buf) == null) {
                throw streamClosed();
            }
            if (this.count - this.pos <= 0) {
                return -1;
            }
            int i = this.pos;
            this.pos = i + 1;
            return bArr[i] & 255;
        }
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int read(byte[] bArr, int i, int i2) throws IOException {
        int i3;
        int i4;
        synchronized (this) {
            byte[] bArr2 = this.buf;
            if (bArr2 == null) {
                throw streamClosed();
            }
            if (i2 == 0) {
                return 0;
            }
            InputStream inputStream = this.in;
            if (inputStream == null) {
                throw streamClosed();
            }
            if (this.pos < this.count) {
                int i5 = this.count - this.pos >= i2 ? i2 : this.count - this.pos;
                System.arraycopy(bArr2, this.pos, bArr, i, i5);
                this.pos += i5;
                if (i5 == i2 || inputStream.available() == 0) {
                    return i5;
                }
                i += i5;
                i3 = i2 - i5;
            } else {
                i3 = i2;
            }
            while (true) {
                if (this.markpos == -1 && i3 >= bArr2.length) {
                    i4 = inputStream.read(bArr, i, i3);
                    if (i4 == -1) {
                        return i3 != i2 ? i2 - i3 : -1;
                    }
                } else {
                    if (fillbuf(inputStream, bArr2) == -1) {
                        return i3 != i2 ? i2 - i3 : -1;
                    }
                    if (bArr2 != this.buf && (bArr2 = this.buf) == null) {
                        throw streamClosed();
                    }
                    i4 = this.count - this.pos >= i3 ? i3 : this.count - this.pos;
                    System.arraycopy(bArr2, this.pos, bArr, i, i4);
                    this.pos += i4;
                }
                i3 -= i4;
                if (i3 == 0) {
                    return i2;
                }
                if (inputStream.available() == 0) {
                    return i2 - i3;
                }
                i += i4;
            }
        }
    }

    public void release() {
        synchronized (this) {
            if (this.buf != null) {
                this.byteArrayPool.put(this.buf);
                this.buf = null;
            }
        }
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public void reset() throws IOException {
        synchronized (this) {
            if (this.buf == null) {
                throw new IOException("Stream is closed");
            }
            if (-1 == this.markpos) {
                throw new InvalidMarkException("Mark has been invalidated, pos: " + this.pos + " markLimit: " + this.marklimit);
            }
            this.pos = this.markpos;
        }
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public long skip(long j) throws IOException {
        synchronized (this) {
            if (j < 1) {
                return 0L;
            }
            byte[] bArr = this.buf;
            if (bArr == null) {
                throw streamClosed();
            }
            InputStream inputStream = this.in;
            if (inputStream == null) {
                throw streamClosed();
            }
            if (this.count - this.pos >= j) {
                this.pos = (int) (((long) this.pos) + j);
                return j;
            }
            long j2 = ((long) this.count) - ((long) this.pos);
            this.pos = this.count;
            if (this.markpos == -1 || j > this.marklimit) {
                long jSkip = inputStream.skip(j - j2);
                if (jSkip > 0) {
                    this.markpos = -1;
                }
                return j2 + jSkip;
            }
            if (fillbuf(inputStream, bArr) == -1) {
                return j2;
            }
            if (this.count - this.pos >= j - j2) {
                this.pos = (int) ((((long) this.pos) + j) - j2);
                return j;
            }
            long j3 = this.count;
            long j4 = this.pos;
            this.pos = this.count;
            return (j3 + j2) - j4;
        }
    }
}
