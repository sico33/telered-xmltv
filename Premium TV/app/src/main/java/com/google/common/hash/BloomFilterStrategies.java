package com.google.common.hash;

import com.google.common.base.Preconditions;
import com.google.common.math.LongMath;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLongArray;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
enum BloomFilterStrategies implements BloomFilter.Strategy {
    MURMUR128_MITZ_32 { // from class: com.google.common.hash.BloomFilterStrategies.1
        @Override // com.google.common.hash.BloomFilter.Strategy
        public <T> boolean mightContain(@ParametricNullness T t, Funnel<? super T> funnel, int i, LockFreeBitArray lockFreeBitArray) {
            long jBitSize = lockFreeBitArray.bitSize();
            long jAsLong = Hashing.murmur3_128().hashObject(t, funnel).asLong();
            int i2 = (int) jAsLong;
            int i3 = (int) (jAsLong >>> 32);
            for (int i4 = 1; i4 <= i; i4++) {
                int i5 = (i4 * i3) + i2;
                if (i5 < 0) {
                    i5 ^= -1;
                }
                if (!lockFreeBitArray.get(((long) i5) % jBitSize)) {
                    return false;
                }
            }
            return true;
        }

        @Override // com.google.common.hash.BloomFilter.Strategy
        public <T> boolean put(@ParametricNullness T t, Funnel<? super T> funnel, int i, LockFreeBitArray lockFreeBitArray) {
            long jBitSize = lockFreeBitArray.bitSize();
            long jAsLong = Hashing.murmur3_128().hashObject(t, funnel).asLong();
            int i2 = (int) jAsLong;
            int i3 = (int) (jAsLong >>> 32);
            boolean z = false;
            for (int i4 = 1; i4 <= i; i4++) {
                int i5 = (i4 * i3) + i2;
                if (i5 < 0) {
                    i5 ^= -1;
                }
                z |= lockFreeBitArray.set(((long) i5) % jBitSize);
            }
            return z;
        }
    },
    MURMUR128_MITZ_64 { // from class: com.google.common.hash.BloomFilterStrategies.2
        private long lowerEight(byte[] bArr) {
            return Longs.fromBytes(bArr[7], bArr[6], bArr[5], bArr[4], bArr[3], bArr[2], bArr[1], bArr[0]);
        }

        private long upperEight(byte[] bArr) {
            return Longs.fromBytes(bArr[15], bArr[14], bArr[13], bArr[12], bArr[11], bArr[10], bArr[9], bArr[8]);
        }

        @Override // com.google.common.hash.BloomFilter.Strategy
        public <T> boolean mightContain(@ParametricNullness T t, Funnel<? super T> funnel, int i, LockFreeBitArray lockFreeBitArray) {
            long jBitSize = lockFreeBitArray.bitSize();
            byte[] bytesInternal = Hashing.murmur3_128().hashObject(t, funnel).getBytesInternal();
            long jLowerEight = lowerEight(bytesInternal);
            long jUpperEight = upperEight(bytesInternal);
            for (int i2 = 0; i2 < i; i2++) {
                if (!lockFreeBitArray.get((Long.MAX_VALUE & jLowerEight) % jBitSize)) {
                    return false;
                }
                jLowerEight += jUpperEight;
            }
            return true;
        }

        @Override // com.google.common.hash.BloomFilter.Strategy
        public <T> boolean put(@ParametricNullness T t, Funnel<? super T> funnel, int i, LockFreeBitArray lockFreeBitArray) {
            long jBitSize = lockFreeBitArray.bitSize();
            byte[] bytesInternal = Hashing.murmur3_128().hashObject(t, funnel).getBytesInternal();
            long jLowerEight = lowerEight(bytesInternal);
            long jUpperEight = upperEight(bytesInternal);
            boolean z = false;
            for (int i2 = 0; i2 < i; i2++) {
                z |= lockFreeBitArray.set((Long.MAX_VALUE & jLowerEight) % jBitSize);
                jLowerEight += jUpperEight;
            }
            return z;
        }
    };

    static final class LockFreeBitArray {
        private static final int LONG_ADDRESSABLE_BITS = 6;
        private final LongAddable bitCount;
        final AtomicLongArray data;

        LockFreeBitArray(long j) {
            Preconditions.checkArgument(j > 0, "data length is zero!");
            this.data = new AtomicLongArray(Ints.checkedCast(LongMath.divide(j, 64L, RoundingMode.CEILING)));
            this.bitCount = LongAddables.create();
        }

        LockFreeBitArray(long[] jArr) {
            Preconditions.checkArgument(jArr.length > 0, "data length is zero!");
            this.data = new AtomicLongArray(jArr);
            this.bitCount = LongAddables.create();
            long jBitCount = 0;
            for (long j : jArr) {
                jBitCount += (long) Long.bitCount(j);
            }
            this.bitCount.add(jBitCount);
        }

        public static long[] toPlainArray(AtomicLongArray atomicLongArray) {
            long[] jArr = new long[atomicLongArray.length()];
            for (int i = 0; i < jArr.length; i++) {
                jArr[i] = atomicLongArray.get(i);
            }
            return jArr;
        }

        long bitCount() {
            return this.bitCount.sum();
        }

        long bitSize() {
            return ((long) this.data.length()) * 64;
        }

        LockFreeBitArray copy() {
            return new LockFreeBitArray(toPlainArray(this.data));
        }

        int dataLength() {
            return this.data.length();
        }

        public boolean equals(@CheckForNull Object obj) {
            if (obj instanceof LockFreeBitArray) {
                return Arrays.equals(toPlainArray(this.data), toPlainArray(((LockFreeBitArray) obj).data));
            }
            return false;
        }

        boolean get(long j) {
            return (this.data.get((int) (j >>> 6)) & (1 << ((int) j))) != 0;
        }

        public int hashCode() {
            return Arrays.hashCode(toPlainArray(this.data));
        }

        void putAll(LockFreeBitArray lockFreeBitArray) {
            Preconditions.checkArgument(this.data.length() == lockFreeBitArray.data.length(), "BitArrays must be of equal length (%s != %s)", this.data.length(), lockFreeBitArray.data.length());
            for (int i = 0; i < this.data.length(); i++) {
                putData(i, lockFreeBitArray.data.get(i));
            }
        }

        void putData(int i, long j) {
            long j2;
            long j3;
            boolean z;
            while (true) {
                j2 = this.data.get(i);
                j3 = j2 | j;
                if (j2 == j3) {
                    z = false;
                    break;
                } else if (this.data.compareAndSet(i, j2, j3)) {
                    z = true;
                    break;
                }
            }
            if (z) {
                this.bitCount.add(Long.bitCount(j3) - Long.bitCount(j2));
            }
        }

        boolean set(long j) {
            long j2;
            long j3;
            if (get(j)) {
                return false;
            }
            int i = (int) (j >>> 6);
            int i2 = (int) j;
            do {
                j2 = this.data.get(i);
                j3 = (1 << i2) | j2;
                if (j2 == j3) {
                    return false;
                }
            } while (!this.data.compareAndSet(i, j2, j3));
            this.bitCount.increment();
            return true;
        }
    }
}
