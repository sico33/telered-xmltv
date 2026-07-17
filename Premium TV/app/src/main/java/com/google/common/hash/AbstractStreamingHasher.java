package com.google.common.hash;

import com.google.common.base.Preconditions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
abstract class AbstractStreamingHasher extends AbstractHasher {
    private final ByteBuffer buffer;
    private final int bufferSize;
    private final int chunkSize;

    protected AbstractStreamingHasher(int i) {
        this(i, i);
    }

    protected AbstractStreamingHasher(int i, int i2) {
        Preconditions.checkArgument(i2 % i == 0);
        this.buffer = ByteBuffer.allocate(i2 + 7).order(ByteOrder.LITTLE_ENDIAN);
        this.bufferSize = i2;
        this.chunkSize = i;
    }

    private void munch() {
        Java8Compatibility.flip(this.buffer);
        while (true) {
            int iRemaining = this.buffer.remaining();
            int i = this.chunkSize;
            ByteBuffer byteBuffer = this.buffer;
            if (iRemaining < i) {
                byteBuffer.compact();
                return;
            }
            process(byteBuffer);
        }
    }

    private void munchIfFull() {
        if (this.buffer.remaining() < 8) {
            munch();
        }
    }

    private Hasher putBytesInternal(ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() <= this.buffer.remaining()) {
            this.buffer.put(byteBuffer);
            munchIfFull();
        } else {
            int i = this.bufferSize;
            int iPosition = this.buffer.position();
            for (int i2 = 0; i2 < i - iPosition; i2++) {
                this.buffer.put(byteBuffer.get());
            }
            munch();
            while (byteBuffer.remaining() >= this.chunkSize) {
                process(byteBuffer);
            }
            this.buffer.put(byteBuffer);
        }
        return this;
    }

    @Override // com.google.common.hash.Hasher
    public final HashCode hash() {
        munch();
        Java8Compatibility.flip(this.buffer);
        if (this.buffer.remaining() > 0) {
            processRemaining(this.buffer);
            Java8Compatibility.position(this.buffer, this.buffer.limit());
        }
        return makeHash();
    }

    protected abstract HashCode makeHash();

    protected abstract void process(ByteBuffer byteBuffer);

    protected void processRemaining(ByteBuffer byteBuffer) {
        Java8Compatibility.position(byteBuffer, byteBuffer.limit());
        Java8Compatibility.limit(byteBuffer, this.chunkSize + 7);
        while (byteBuffer.position() < this.chunkSize) {
            byteBuffer.putLong(0L);
        }
        Java8Compatibility.limit(byteBuffer, this.chunkSize);
        Java8Compatibility.flip(byteBuffer);
        process(byteBuffer);
    }

    @Override // com.google.common.hash.AbstractHasher, com.google.common.hash.Hasher, com.google.common.hash.PrimitiveSink
    public final Hasher putByte(byte b) {
        this.buffer.put(b);
        munchIfFull();
        return this;
    }

    @Override // com.google.common.hash.AbstractHasher, com.google.common.hash.PrimitiveSink
    public final Hasher putBytes(ByteBuffer byteBuffer) {
        ByteOrder byteOrderOrder = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            return putBytesInternal(byteBuffer);
        } finally {
            byteBuffer.order(byteOrderOrder);
        }
    }

    @Override // com.google.common.hash.AbstractHasher, com.google.common.hash.PrimitiveSink
    public final Hasher putBytes(byte[] bArr, int i, int i2) {
        return putBytesInternal(ByteBuffer.wrap(bArr, i, i2).order(ByteOrder.LITTLE_ENDIAN));
    }

    @Override // com.google.common.hash.AbstractHasher, com.google.common.hash.PrimitiveSink
    public final Hasher putChar(char c) {
        this.buffer.putChar(c);
        munchIfFull();
        return this;
    }

    @Override // com.google.common.hash.AbstractHasher, com.google.common.hash.PrimitiveSink
    public final Hasher putInt(int i) {
        this.buffer.putInt(i);
        munchIfFull();
        return this;
    }

    @Override // com.google.common.hash.AbstractHasher, com.google.common.hash.PrimitiveSink
    public final Hasher putLong(long j) {
        this.buffer.putLong(j);
        munchIfFull();
        return this;
    }

    @Override // com.google.common.hash.AbstractHasher, com.google.common.hash.PrimitiveSink
    public final Hasher putShort(short s) {
        this.buffer.putShort(s);
        munchIfFull();
        return this;
    }
}
