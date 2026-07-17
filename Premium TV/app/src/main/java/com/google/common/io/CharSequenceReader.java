package com.google.common.io;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.Objects;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class CharSequenceReader extends Reader {
    private int mark;
    private int pos;

    @CheckForNull
    private CharSequence seq;

    public CharSequenceReader(CharSequence charSequence) {
        this.seq = (CharSequence) Preconditions.checkNotNull(charSequence);
    }

    private void checkOpen() throws IOException {
        if (this.seq == null) {
            throw new IOException("reader closed");
        }
    }

    private boolean hasRemaining() {
        return remaining() > 0;
    }

    private int remaining() {
        Objects.requireNonNull(this.seq);
        return this.seq.length() - this.pos;
    }

    @Override // java.io.Reader, java.io.Closeable, java.lang.AutoCloseable
    public void close() throws IOException {
        synchronized (this) {
            this.seq = null;
        }
    }

    @Override // java.io.Reader
    public void mark(int i) throws IOException {
        synchronized (this) {
            Preconditions.checkArgument(i >= 0, "readAheadLimit (%s) may not be negative", i);
            checkOpen();
            this.mark = this.pos;
        }
    }

    @Override // java.io.Reader
    public boolean markSupported() {
        return true;
    }

    @Override // java.io.Reader
    public int read() throws IOException {
        int iCharAt;
        synchronized (this) {
            checkOpen();
            Objects.requireNonNull(this.seq);
            if (hasRemaining()) {
                CharSequence charSequence = this.seq;
                int i = this.pos;
                this.pos = i + 1;
                iCharAt = charSequence.charAt(i);
            } else {
                iCharAt = -1;
            }
        }
        return iCharAt;
    }

    @Override // java.io.Reader, java.lang.Readable
    public int read(CharBuffer charBuffer) throws IOException {
        synchronized (this) {
            Preconditions.checkNotNull(charBuffer);
            checkOpen();
            Objects.requireNonNull(this.seq);
            if (!hasRemaining()) {
                return -1;
            }
            int iMin = Math.min(charBuffer.remaining(), remaining());
            for (int i = 0; i < iMin; i++) {
                CharSequence charSequence = this.seq;
                int i2 = this.pos;
                this.pos = i2 + 1;
                charBuffer.put(charSequence.charAt(i2));
            }
            return iMin;
        }
    }

    @Override // java.io.Reader
    public int read(char[] cArr, int i, int i2) throws IOException {
        synchronized (this) {
            Preconditions.checkPositionIndexes(i, i + i2, cArr.length);
            checkOpen();
            Objects.requireNonNull(this.seq);
            if (!hasRemaining()) {
                return -1;
            }
            int iMin = Math.min(i2, remaining());
            for (int i3 = 0; i3 < iMin; i3++) {
                CharSequence charSequence = this.seq;
                int i4 = this.pos;
                this.pos = i4 + 1;
                cArr[i + i3] = charSequence.charAt(i4);
            }
            return iMin;
        }
    }

    @Override // java.io.Reader
    public boolean ready() throws IOException {
        synchronized (this) {
            checkOpen();
        }
        return true;
    }

    @Override // java.io.Reader
    public void reset() throws IOException {
        synchronized (this) {
            checkOpen();
            this.pos = this.mark;
        }
    }

    @Override // java.io.Reader
    public long skip(long j) throws IOException {
        long j2;
        synchronized (this) {
            Preconditions.checkArgument(j >= 0, "n (%s) may not be negative", j);
            checkOpen();
            int iMin = (int) Math.min(remaining(), j);
            this.pos += iMin;
            j2 = iMin;
        }
        return j2;
    }
}
