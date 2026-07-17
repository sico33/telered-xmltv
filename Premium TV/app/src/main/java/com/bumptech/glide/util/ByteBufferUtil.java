package com.bumptech.glide.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicReference;

/* JADX INFO: loaded from: classes.dex */
public final class ByteBufferUtil {
    private static final AtomicReference<byte[]> BUFFER_REF = new AtomicReference<>();
    private static final int BUFFER_SIZE = 16384;

    private static class ByteBufferStream extends InputStream {
        private static final int UNSET = -1;
        private final ByteBuffer byteBuffer;
        private int markPos = -1;

        ByteBufferStream(ByteBuffer byteBuffer) {
            this.byteBuffer = byteBuffer;
        }

        @Override // java.io.InputStream
        public int available() {
            return this.byteBuffer.remaining();
        }

        @Override // java.io.InputStream
        public void mark(int i) {
            synchronized (this) {
                this.markPos = this.byteBuffer.position();
            }
        }

        @Override // java.io.InputStream
        public boolean markSupported() {
            return true;
        }

        @Override // java.io.InputStream
        public int read() {
            if (this.byteBuffer.hasRemaining()) {
                return this.byteBuffer.get() & 255;
            }
            return -1;
        }

        @Override // java.io.InputStream
        public int read(byte[] bArr, int i, int i2) {
            if (!this.byteBuffer.hasRemaining()) {
                return -1;
            }
            int iMin = Math.min(i2, available());
            this.byteBuffer.get(bArr, i, iMin);
            return iMin;
        }

        @Override // java.io.InputStream
        public void reset() throws IOException {
            synchronized (this) {
                if (this.markPos == -1) {
                    throw new IOException("Cannot reset to unset mark position");
                }
                this.byteBuffer.position(this.markPos);
            }
        }

        @Override // java.io.InputStream
        public long skip(long j) {
            if (!this.byteBuffer.hasRemaining()) {
                return -1L;
            }
            long jMin = Math.min(j, available());
            this.byteBuffer.position((int) (((long) this.byteBuffer.position()) + jMin));
            return jMin;
        }
    }

    static final class SafeArray {
        final byte[] data;
        final int limit;
        final int offset;

        SafeArray(byte[] bArr, int i, int i2) {
            this.data = bArr;
            this.offset = i;
            this.limit = i2;
        }
    }

    private ByteBufferUtil() {
    }

    /* JADX WARN: Code duplicated, block: B:38:0x0046 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:40:0x0041 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:51:? A[SYNTHETIC] */
    public static ByteBuffer fromFile(File file) throws Throwable {
        RandomAccessFile randomAccessFile;
        Throwable th;
        FileChannel channel;
        try {
            long length = file.length();
            if (length > 2147483647L) {
                throw new IOException("File too large to map into memory");
            }
            if (length == 0) {
                throw new IOException("File unsuitable for memory mapping");
            }
            RandomAccessFile randomAccessFile2 = new RandomAccessFile(file, "r");
            try {
                channel = randomAccessFile2.getChannel();
                try {
                    MappedByteBuffer mappedByteBufferLoad = channel.map(FileChannel.MapMode.READ_ONLY, 0L, length).load();
                    if (channel != null) {
                        try {
                            channel.close();
                        } catch (IOException e) {
                        }
                    }
                    try {
                        randomAccessFile2.close();
                    } catch (IOException e2) {
                    }
                    return mappedByteBufferLoad;
                } catch (Throwable th2) {
                    th = th2;
                    if (channel != null) {
                        try {
                            channel.close();
                        } catch (IOException e3) {
                        }
                    }
                    if (randomAccessFile2 != null) {
                        throw th;
                    }
                    try {
                        randomAccessFile2.close();
                        throw th;
                    } catch (IOException e4) {
                        throw th;
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                randomAccessFile = randomAccessFile2;
                randomAccessFile2 = randomAccessFile;
                th = th;
                channel = null;
                if (channel != null) {
                    channel.close();
                }
                if (randomAccessFile2 != null) {
                    throw th;
                }
                randomAccessFile2.close();
                throw th;
            }
        } catch (Throwable th4) {
            th = th4;
            randomAccessFile = null;
        }
    }

    public static ByteBuffer fromStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(16384);
        byte[] andSet = BUFFER_REF.getAndSet(null);
        if (andSet == null) {
            andSet = new byte[16384];
        }
        while (true) {
            int i = inputStream.read(andSet);
            if (i < 0) {
                BUFFER_REF.set(andSet);
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                return rewind(ByteBuffer.allocateDirect(byteArray.length).put(byteArray));
            }
            byteArrayOutputStream.write(andSet, 0, i);
        }
    }

    private static SafeArray getSafeArray(ByteBuffer byteBuffer) {
        if (byteBuffer.isReadOnly() || !byteBuffer.hasArray()) {
            return null;
        }
        return new SafeArray(byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.limit());
    }

    public static ByteBuffer rewind(ByteBuffer byteBuffer) {
        return (ByteBuffer) byteBuffer.position(0);
    }

    public static byte[] toBytes(ByteBuffer byteBuffer) {
        SafeArray safeArray = getSafeArray(byteBuffer);
        if (safeArray != null && safeArray.offset == 0 && safeArray.limit == safeArray.data.length) {
            return byteBuffer.array();
        }
        ByteBuffer byteBufferAsReadOnlyBuffer = byteBuffer.asReadOnlyBuffer();
        byte[] bArr = new byte[byteBufferAsReadOnlyBuffer.limit()];
        rewind(byteBufferAsReadOnlyBuffer);
        byteBufferAsReadOnlyBuffer.get(bArr);
        return bArr;
    }

    public static void toFile(ByteBuffer byteBuffer, File file) throws Throwable {
        RandomAccessFile randomAccessFile;
        FileChannel channel = null;
        rewind(byteBuffer);
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            try {
                channel = randomAccessFile.getChannel();
                channel.write(byteBuffer);
                channel.force(false);
                channel.close();
                randomAccessFile.close();
                if (channel != null) {
                    try {
                        channel.close();
                    } catch (IOException e) {
                    }
                }
                try {
                    randomAccessFile.close();
                } catch (IOException e2) {
                }
            } catch (Throwable th) {
                th = th;
                if (channel != null) {
                    try {
                        channel.close();
                    } catch (IOException e3) {
                    }
                }
                if (randomAccessFile == null) {
                    throw th;
                }
                try {
                    randomAccessFile.close();
                    throw th;
                } catch (IOException e4) {
                    throw th;
                }
            }
        } catch (Throwable th2) {
            th = th2;
            randomAccessFile = null;
        }
    }

    public static InputStream toStream(ByteBuffer byteBuffer) {
        return new ByteBufferStream(byteBuffer);
    }

    public static void toStream(ByteBuffer byteBuffer, OutputStream outputStream) throws IOException {
        SafeArray safeArray = getSafeArray(byteBuffer);
        if (safeArray != null) {
            outputStream.write(safeArray.data, safeArray.offset, safeArray.limit + safeArray.offset);
            return;
        }
        byte[] andSet = BUFFER_REF.getAndSet(null);
        if (andSet == null) {
            andSet = new byte[16384];
        }
        while (byteBuffer.remaining() > 0) {
            int iMin = Math.min(byteBuffer.remaining(), andSet.length);
            byteBuffer.get(andSet, 0, iMin);
            outputStream.write(andSet, 0, iMin);
        }
        BUFFER_REF.set(andSet);
    }
}
