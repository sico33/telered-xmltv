package com.google.common.hash;

import com.google.common.primitives.UnsignedBytes;
import com.google.errorprone.annotations.Immutable;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@Immutable
@ElementTypesAreNonnullByDefault
final class Murmur3_128HashFunction extends AbstractHashFunction implements Serializable {
    private static final long serialVersionUID = 0;
    private final int seed;
    static final HashFunction MURMUR3_128 = new Murmur3_128HashFunction(0);
    static final HashFunction GOOD_FAST_HASH_128 = new Murmur3_128HashFunction(Hashing.GOOD_FAST_HASH_SEED);

    private static final class Murmur3_128Hasher extends AbstractStreamingHasher {
        private static final long C1 = -8663945395140668459L;
        private static final long C2 = 5545529020109919103L;
        private static final int CHUNK_SIZE = 16;
        private long h1;
        private long h2;
        private int length;

        Murmur3_128Hasher(int i) {
            super(16);
            this.h1 = i;
            this.h2 = i;
            this.length = 0;
        }

        private void bmix64(long j, long j2) {
            this.h1 ^= mixK1(j);
            this.h1 = Long.rotateLeft(this.h1, 27);
            this.h1 += this.h2;
            this.h1 = (this.h1 * 5) + 1390208809;
            this.h2 ^= mixK2(j2);
            this.h2 = Long.rotateLeft(this.h2, 31);
            this.h2 += this.h1;
            this.h2 = (this.h2 * 5) + 944331445;
        }

        private static long fmix64(long j) {
            long j2 = ((j >>> 33) ^ j) * (-49064778989728563L);
            long j3 = (j2 ^ (j2 >>> 33)) * (-4265267296055464877L);
            return j3 ^ (j3 >>> 33);
        }

        private static long mixK1(long j) {
            return Long.rotateLeft(C1 * j, 31) * C2;
        }

        private static long mixK2(long j) {
            return Long.rotateLeft(C2 * j, 33) * C1;
        }

        @Override // com.google.common.hash.AbstractStreamingHasher
        protected HashCode makeHash() {
            this.h1 ^= (long) this.length;
            this.h2 ^= (long) this.length;
            this.h1 += this.h2;
            this.h2 += this.h1;
            this.h1 = fmix64(this.h1);
            this.h2 = fmix64(this.h2);
            this.h1 += this.h2;
            this.h2 += this.h1;
            return HashCode.fromBytesNoCopy(ByteBuffer.wrap(new byte[16]).order(ByteOrder.LITTLE_ENDIAN).putLong(this.h1).putLong(this.h2).array());
        }

        @Override // com.google.common.hash.AbstractStreamingHasher
        protected void process(ByteBuffer byteBuffer) {
            bmix64(byteBuffer.getLong(), byteBuffer.getLong());
            this.length += 16;
        }

        @Override // com.google.common.hash.AbstractStreamingHasher
        protected void processRemaining(ByteBuffer byteBuffer) {
            long j;
            long j2;
            long j3;
            long j4;
            long j5;
            long j6;
            long j7;
            long j8;
            long j9;
            long j10;
            long j11;
            long j12;
            long j13;
            long j14;
            this.length += byteBuffer.remaining();
            switch (byteBuffer.remaining()) {
                case 1:
                    j = 0;
                    j14 = j ^ ((long) UnsignedBytes.toInt(byteBuffer.get(0)));
                    j7 = 0;
                    this.h1 = mixK1(j14) ^ this.h1;
                    this.h2 = mixK2(j7) ^ this.h2;
                    return;
                case 2:
                    j2 = 0;
                    j = j2 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(1))) << 8);
                    j14 = j ^ ((long) UnsignedBytes.toInt(byteBuffer.get(0)));
                    j7 = 0;
                    this.h1 = mixK1(j14) ^ this.h1;
                    this.h2 = mixK2(j7) ^ this.h2;
                    return;
                case 3:
                    j3 = 0;
                    j2 = j3 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(2))) << 16);
                    j = j2 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(1))) << 8);
                    j14 = j ^ ((long) UnsignedBytes.toInt(byteBuffer.get(0)));
                    j7 = 0;
                    this.h1 = mixK1(j14) ^ this.h1;
                    this.h2 = mixK2(j7) ^ this.h2;
                    return;
                case 4:
                    j4 = 0;
                    j3 = j4 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(3))) << 24);
                    j2 = j3 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(2))) << 16);
                    j = j2 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(1))) << 8);
                    j14 = j ^ ((long) UnsignedBytes.toInt(byteBuffer.get(0)));
                    j7 = 0;
                    this.h1 = mixK1(j14) ^ this.h1;
                    this.h2 = mixK2(j7) ^ this.h2;
                    return;
                case 5:
                    j5 = 0;
                    j4 = j5 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(4))) << 32);
                    j3 = j4 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(3))) << 24);
                    j2 = j3 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(2))) << 16);
                    j = j2 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(1))) << 8);
                    j14 = j ^ ((long) UnsignedBytes.toInt(byteBuffer.get(0)));
                    j7 = 0;
                    this.h1 = mixK1(j14) ^ this.h1;
                    this.h2 = mixK2(j7) ^ this.h2;
                    return;
                case 6:
                    j6 = 0;
                    j5 = j6 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(5))) << 40);
                    j4 = j5 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(4))) << 32);
                    j3 = j4 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(3))) << 24);
                    j2 = j3 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(2))) << 16);
                    j = j2 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(1))) << 8);
                    j14 = j ^ ((long) UnsignedBytes.toInt(byteBuffer.get(0)));
                    j7 = 0;
                    this.h1 = mixK1(j14) ^ this.h1;
                    this.h2 = mixK2(j7) ^ this.h2;
                    return;
                case 7:
                    j6 = (((long) UnsignedBytes.toInt(byteBuffer.get(6))) << 48) ^ 0;
                    j5 = j6 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(5))) << 40);
                    j4 = j5 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(4))) << 32);
                    j3 = j4 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(3))) << 24);
                    j2 = j3 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(2))) << 16);
                    j = j2 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(1))) << 8);
                    j14 = j ^ ((long) UnsignedBytes.toInt(byteBuffer.get(0)));
                    j7 = 0;
                    this.h1 = mixK1(j14) ^ this.h1;
                    this.h2 = mixK2(j7) ^ this.h2;
                    return;
                case 8:
                    j7 = 0;
                    j14 = 0 ^ byteBuffer.getLong();
                    this.h1 = mixK1(j14) ^ this.h1;
                    this.h2 = mixK2(j7) ^ this.h2;
                    return;
                case 9:
                    j8 = 0;
                    j7 = j8 ^ ((long) UnsignedBytes.toInt(byteBuffer.get(8)));
                    j14 = 0 ^ byteBuffer.getLong();
                    this.h1 = mixK1(j14) ^ this.h1;
                    this.h2 = mixK2(j7) ^ this.h2;
                    return;
                case 10:
                    j9 = 0;
                    j8 = j9 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(9))) << 8);
                    j7 = j8 ^ ((long) UnsignedBytes.toInt(byteBuffer.get(8)));
                    j14 = 0 ^ byteBuffer.getLong();
                    this.h1 = mixK1(j14) ^ this.h1;
                    this.h2 = mixK2(j7) ^ this.h2;
                    return;
                case 11:
                    j10 = 0;
                    j9 = j10 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(10))) << 16);
                    j8 = j9 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(9))) << 8);
                    j7 = j8 ^ ((long) UnsignedBytes.toInt(byteBuffer.get(8)));
                    j14 = 0 ^ byteBuffer.getLong();
                    this.h1 = mixK1(j14) ^ this.h1;
                    this.h2 = mixK2(j7) ^ this.h2;
                    return;
                case 12:
                    j11 = 0;
                    j10 = j11 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(11))) << 24);
                    j9 = j10 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(10))) << 16);
                    j8 = j9 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(9))) << 8);
                    j7 = j8 ^ ((long) UnsignedBytes.toInt(byteBuffer.get(8)));
                    j14 = 0 ^ byteBuffer.getLong();
                    this.h1 = mixK1(j14) ^ this.h1;
                    this.h2 = mixK2(j7) ^ this.h2;
                    return;
                case 13:
                    j12 = 0;
                    j11 = j12 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(12))) << 32);
                    j10 = j11 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(11))) << 24);
                    j9 = j10 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(10))) << 16);
                    j8 = j9 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(9))) << 8);
                    j7 = j8 ^ ((long) UnsignedBytes.toInt(byteBuffer.get(8)));
                    j14 = 0 ^ byteBuffer.getLong();
                    this.h1 = mixK1(j14) ^ this.h1;
                    this.h2 = mixK2(j7) ^ this.h2;
                    return;
                case 14:
                    j13 = 0;
                    j12 = j13 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(13))) << 40);
                    j11 = j12 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(12))) << 32);
                    j10 = j11 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(11))) << 24);
                    j9 = j10 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(10))) << 16);
                    j8 = j9 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(9))) << 8);
                    j7 = j8 ^ ((long) UnsignedBytes.toInt(byteBuffer.get(8)));
                    j14 = 0 ^ byteBuffer.getLong();
                    this.h1 = mixK1(j14) ^ this.h1;
                    this.h2 = mixK2(j7) ^ this.h2;
                    return;
                case 15:
                    j13 = (((long) UnsignedBytes.toInt(byteBuffer.get(14))) << 48) ^ 0;
                    j12 = j13 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(13))) << 40);
                    j11 = j12 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(12))) << 32);
                    j10 = j11 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(11))) << 24);
                    j9 = j10 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(10))) << 16);
                    j8 = j9 ^ (((long) UnsignedBytes.toInt(byteBuffer.get(9))) << 8);
                    j7 = j8 ^ ((long) UnsignedBytes.toInt(byteBuffer.get(8)));
                    j14 = 0 ^ byteBuffer.getLong();
                    this.h1 = mixK1(j14) ^ this.h1;
                    this.h2 = mixK2(j7) ^ this.h2;
                    return;
                default:
                    throw new AssertionError("Should never get here.");
            }
        }
    }

    Murmur3_128HashFunction(int i) {
        this.seed = i;
    }

    @Override // com.google.common.hash.HashFunction
    public int bits() {
        return 128;
    }

    public boolean equals(@CheckForNull Object obj) {
        return (obj instanceof Murmur3_128HashFunction) && this.seed == ((Murmur3_128HashFunction) obj).seed;
    }

    public int hashCode() {
        return getClass().hashCode() ^ this.seed;
    }

    @Override // com.google.common.hash.HashFunction
    public Hasher newHasher() {
        return new Murmur3_128Hasher(this.seed);
    }

    public String toString() {
        return "Hashing.murmur3_128(" + this.seed + ")";
    }
}
